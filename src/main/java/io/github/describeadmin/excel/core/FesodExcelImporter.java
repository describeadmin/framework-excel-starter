package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.ExcelImportResult;
import io.github.describeadmin.excel.api.ExcelImporter;
import io.github.describeadmin.excel.api.ExcelParseException;
import io.github.describeadmin.excel.api.ExcelReadOptions;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import org.apache.fesod.sheet.FesodSheet;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/** {@link ExcelImporter} 的 Fesod 实现。 */
public class FesodExcelImporter implements ExcelImporter {

    private final FrameworkExcelProperties properties;

    public FesodExcelImporter(FrameworkExcelProperties properties) {
        this.properties = properties;
    }

    @Override
    public <T> ExcelImportResult<T> read(InputStream in, Class<T> model, ExcelReadOptions options) {
        ExcelReadOptions opts = options == null ? ExcelReadOptions.defaults() : options;
        FrameworkExcelProperties.Import cfg = properties.getImport();

        boolean trim = opts.trim() != null ? opts.trim() : cfg.isTrimStrings();
        boolean failFast = opts.failFast() != null ? opts.failFast() : cfg.isFailFast();
        int maxRows = opts.maxRows() != null ? opts.maxRows() : cfg.getMaxRows();
        int headRow = opts.headRowNumber() != null ? opts.headRowNumber() : cfg.getHeadRowNumber();

        InputStream verified = requireWorkbookMagic(in);

        CollectingReadListener<T> listener = new CollectingReadListener<>(trim, failFast, maxRows);
        try {
            FesodSheet.read(verified, model, listener)
                    .headRowNumber(headRow)
                    .registerConverter(LongCellConverter.forWrapper(true))
                    .registerConverter(LongCellConverter.forPrimitive(true))
                    .sheet()
                    .doRead();
        } catch (ExcelParseException e) {
            throw e;
        } catch (RuntimeException e) {
            // Fesod 的 ExcelRuntimeException 家族、POI 的 NotOfficeXmlFileException 等都在此
            throw new ExcelParseException("文件无法作为 Excel 读取: " + rootMessage(e), e);
        }
        return ExcelImportResult.of(listener.rows(), listener.errors(), listener.totalDataRows());
    }

    @Override
    public <T> ExcelImportResult<T> read(MultipartFile file, Class<T> model, ExcelReadOptions options) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("上传文件为空");
        }
        try (InputStream in = file.getInputStream()) {
            return read(in, model, options);
        } catch (IOException e) {
            throw new ExcelParseException("读取上传文件失败: " + rootMessage(e), e);
        }
    }

    /**
     * Fesod / POI 遇到无法识别的字节时不抛异常，而是当成 CSV 静默解析出 0 行——那会让业务方
     * 拿到一个"空导入"而不是"文件不对"的信号。所以在交给 Fesod 之前先校验文件头：
     * xlsx 是 ZIP（{@code PK..}），xls 是 OLE2（{@code D0CF11E0...}）。
     */
    private static InputStream requireWorkbookMagic(InputStream in) {
        BufferedInputStream buffered = in instanceof BufferedInputStream b ? b : new BufferedInputStream(in);
        try {
            buffered.mark(16);
            byte[] head = buffered.readNBytes(8);
            buffered.reset();
            if (!looksLikeZip(head) && !looksLikeOle2(head)) {
                throw new ExcelParseException("上传内容不是 Excel 文件（缺少 xlsx/xls 文件头）");
            }
            return buffered;
        } catch (IOException e) {
            throw new ExcelParseException("读取文件头失败: " + rootMessage(e), e);
        }
    }

    private static boolean looksLikeZip(byte[] head) {
        return head.length >= 4 && head[0] == 0x50 && head[1] == 0x4B
                && (head[2] == 0x03 || head[2] == 0x05 || head[2] == 0x07)
                && (head[3] == 0x04 || head[3] == 0x06 || head[3] == 0x08);
    }

    private static boolean looksLikeOle2(byte[] head) {
        return head.length >= 8
                && (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0
                && (head[4] & 0xFF) == 0xA1 && (head[5] & 0xFF) == 0xB1
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0xE1;
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage() == null ? cur.getClass().getSimpleName() : cur.getMessage();
    }
}
