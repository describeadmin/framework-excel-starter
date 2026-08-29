package io.github.describeadmin.excel.core;

import io.github.describeadmin.common.api.BizException;
import io.github.describeadmin.common.api.ResultCode;
import io.github.describeadmin.excel.api.ExcelExporter;
import io.github.describeadmin.excel.api.ExcelIoException;
import io.github.describeadmin.excel.api.ExcelWriteOptions;
import io.github.describeadmin.excel.api.LongCellMode;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** {@link ExcelExporter} 的 Fesod 实现。 */
public class FesodExcelExporter implements ExcelExporter {

    private final FrameworkExcelProperties properties;

    public FesodExcelExporter(FrameworkExcelProperties properties) {
        this.properties = properties;
    }

    @Override
    public <T> void write(OutputStream out, List<T> rows, Class<T> model, ExcelWriteOptions options) {
        ExcelWriteOptions opts = options == null ? ExcelWriteOptions.defaults() : options;
        guardRowLimit(rows, opts);

        boolean asText = resolveLongAsText(opts);
        String sheetName = opts.sheetName() != null ? opts.sheetName()
                : properties.getExport().getDefaultSheetName();

        ExcelWriterBuilder builder = FesodSheet.write(out, model)
                .autoCloseStream(Boolean.FALSE)
                .registerConverter(LongCellConverter.forWrapper(asText))
                .registerConverter(LongCellConverter.forPrimitive(asText));
        if (opts.includeFields() != null && !opts.includeFields().isEmpty()) {
            builder.includeColumnFieldNames(opts.includeFields());
        }
        if (opts.excludeFields() != null && !opts.excludeFields().isEmpty()) {
            builder.excludeColumnFieldNames(opts.excludeFields());
        }

        try {
            builder.sheet(sheetName).doWrite(rows == null ? List.of() : rows);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException io) {
                throw new ExcelIoException("Excel 写出失败", io);
            }
            throw ex;
        }
    }

    @Override
    public <T> void writeToResponse(HttpServletResponse response, List<T> rows, Class<T> model,
                                    ExcelWriteOptions options) {
        ExcelWriteOptions opts = options == null ? ExcelWriteOptions.defaults() : options;
        // 行数超限必须在写第一个字节之前发现——一旦响应流提交就无法再回退成 JSON 错误
        guardRowLimit(rows, opts);

        response.setContentType(ExcelMediaType.XLSX_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ExcelContentDisposition.header(opts.fileName()));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        try {
            write(response.getOutputStream(), rows, model, opts);
        } catch (IOException e) {
            throw new ExcelIoException("获取响应输出流失败", e);
        }
    }

    private void guardRowLimit(List<?> rows, ExcelWriteOptions opts) {
        int limit = opts.maxRows() != null ? opts.maxRows() : properties.getExport().getMaxRows();
        int count = rows == null ? 0 : rows.size();
        if (limit > 0 && count > limit) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "导出行数 " + count + " 超过上限 " + limit + "，请缩小查询范围后重试");
        }
    }

    private boolean resolveLongAsText(ExcelWriteOptions opts) {
        LongCellMode mode = opts.longAsText() == null ? LongCellMode.DEFAULT : opts.longAsText();
        return switch (mode) {
            case TEXT -> true;
            case NUMBER -> false;
            case DEFAULT -> properties.getExport().isLongAsText();
        };
    }
}
