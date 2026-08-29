package io.github.describeadmin.excel.core;

import io.github.describeadmin.common.api.BizException;
import io.github.describeadmin.excel.api.ExcelImportResult;
import io.github.describeadmin.excel.api.ExcelReadOptions;
import io.github.describeadmin.excel.api.ExcelWriteOptions;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 真 xlsx 往返：写出的字节能被读回、且逐值精确（含中文与 19 位雪花 Long）。 */
@DisplayName("Excel 往返")
class ExcelRoundTripTest {

    private static final long SNOWFLAKE = 1234567890123456789L;

    private final FrameworkExcelProperties properties = new FrameworkExcelProperties();
    private final FesodExcelExporter exporter = new FesodExcelExporter(properties);
    private final FesodExcelImporter importer = new FesodExcelImporter(properties);

    @Test
    @DisplayName("写出再读回，中文与 19 位 Long 逐值精确")
    void writeThenReadBackPreservesConcreteValues() {
        List<DemoRow> input = List.of(
                new DemoRow(SNOWFLAKE, "张三", "研发中心", LocalDate.of(2026, 8, 28), true),
                new DemoRow(9007199254740993L, "李四", "财务部", LocalDate.of(2020, 1, 1), false));

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(write(input, DemoRow.class)), DemoRow.class, ExcelReadOptions.defaults());

        assertThat(read.hasErrors()).isFalse();
        assertThat(read.totalDataRows()).isEqualTo(2);
        assertThat(read.rows()).hasSize(2);

        DemoRow first = read.rows().get(0);
        assertThat(first.getId()).isEqualTo(SNOWFLAKE);
        assertThat(first.getName()).isEqualTo("张三");
        assertThat(first.getDept()).isEqualTo("研发中心");
        assertThat(first.getHireDate()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(first.getActive()).isTrue();

        assertThat(read.rows().get(1).getId()).isEqualTo(9007199254740993L);
        assertThat(read.rows().get(1).getName()).isEqualTo("李四");
    }

    @Test
    @DisplayName("Long 默认写成文本单元格，19 位不丢精度")
    void longWrittenAsTextCellByDefault() throws Exception {
        byte[] xlsx = write(List.of(new DemoRow(SNOWFLAKE, "张三", "研发中心", LocalDate.now(), true)), DemoRow.class);

        assertThat(idCellType(xlsx)).isEqualTo(CellType.STRING);

        ExcelImportResult<StringIdRow> asString = importer.read(
                new ByteArrayInputStream(xlsx), StringIdRow.class, ExcelReadOptions.defaults());
        assertThat(asString.rows().get(0).getId()).isEqualTo("1234567890123456789");
    }

    @Test
    @DisplayName("@ExcelLongNumber 逃生舱：写成数值单元格，小于 2^53 时仍精确往返")
    void longAsNumberWhenOptedOut() throws Exception {
        byte[] xlsx = write(List.of(new DemoRowNumberId(4321L, "王五")), DemoRowNumberId.class);

        assertThat(idCellType(xlsx)).isEqualTo(CellType.NUMERIC);

        ExcelImportResult<DemoRowNumberId> read = importer.read(
                new ByteArrayInputStream(xlsx), DemoRowNumberId.class, ExcelReadOptions.defaults());
        assertThat(read.rows().get(0).getId()).isEqualTo(4321L);
    }

    @Test
    @DisplayName("空的数值单元格读成 null，不产生 RowError")
    void blankNumericCellReadsAsNull() {
        byte[] xlsx = write(java.util.Arrays.asList(new DemoRow(null, "无编号", "行政部", LocalDate.now(), true)),
                DemoRow.class);

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(xlsx), DemoRow.class, ExcelReadOptions.defaults());
        assertThat(read.hasErrors()).isFalse();
        assertThat(read.rows().get(0).getId()).isNull();
        assertThat(read.rows().get(0).getName()).isEqualTo("无编号");
    }

    @Test
    @DisplayName("非数字的编号单元格：该行不入 rows()，产生一条 RowError")
    void nonNumericLongCellBecomesRowError() {
        byte[] xlsx = writeRaw(new String[]{"编号", "姓名"}, new String[][]{{"abc", "张三"}});

        ExcelImportResult<StringNameRow> readNames = importer.read(
                new ByteArrayInputStream(xlsx), StringNameRow.class, ExcelReadOptions.defaults());
        assertThat(readNames.rows().get(0).getName()).isEqualTo("张三");

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(xlsx), DemoRow.class, ExcelReadOptions.defaults());
        assertThat(read.rows()).isEmpty();
        assertThat(read.errors()).hasSize(1);
        assertThat(read.errors().get(0).rowIndex()).isEqualTo(2);
        assertThat(read.errors().get(0).field()).isEqualTo("id");
        assertThat(read.errors().get(0).column()).isEqualTo("编号");
        assertThat(read.errors().get(0).rejectedValue()).isEqualTo("abc");
        assertThat(read.errors().get(0).message()).isNotBlank();
    }

    @Test
    @DisplayName("trim-strings=true 时字符串字段两端空白被裁掉")
    void trimStringsApplied() {
        byte[] xlsx = writeRaw(new String[]{"编号", "姓名"}, new String[][]{{"1", "  张三  "}});

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(xlsx), DemoRow.class, ExcelReadOptions.defaults());
        assertThat(read.rows().get(0).getName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("导入超过行数上限：多出的行不入 rows()，产生一条整行 RowError")
    void maxRowsExceededOnImportCollectsWholeRowError() {
        byte[] xlsx = writeRaw(new String[]{"编号", "姓名"},
                new String[][]{{"1", "a"}, {"2", "b"}, {"3", "c"}});

        ExcelImportResult<DemoRow> read = importer.read(new ByteArrayInputStream(xlsx), DemoRow.class,
                ExcelReadOptions.builder().maxRows(1).build());
        assertThat(read.rows()).hasSize(1);
        assertThat(read.errors()).hasSize(1);
        assertThat(read.errors().get(0).field()).isNull();
        assertThat(read.errors().get(0).message()).contains("上限");
    }

    @Test
    @DisplayName("导出超过行数上限：写第一个字节之前抛 BizException(BAD_REQUEST)")
    void exportRowLimitThrowsBeforeWriting() {
        ExcelWriteOptions capped = ExcelWriteOptions.builder().maxRows(1).build();
        List<DemoRow> two = List.of(
                new DemoRow(1L, "a", "x", LocalDate.now(), true),
                new DemoRow(2L, "b", "y", LocalDate.now(), true));

        assertThatThrownBy(() -> exporter.write(new ByteArrayOutputStream(), two, DemoRow.class, capped))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40000));
    }

    // --- helpers ---

    private <T> byte[] write(List<T> rows, Class<T> model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.write(out, rows, model, ExcelWriteOptions.defaults());
        return out.toByteArray();
    }

    /** 用原始表头 + 字符串矩阵造一个 xlsx（所有单元格都是文本），用于构造"坏数据"场景。 */
    private static byte[] writeRaw(String[] header, String[][] dataRows) {
        try (Workbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Sheet1");
            var head = sheet.createRow(0);
            for (int c = 0; c < header.length; c++) {
                head.createCell(c).setCellValue(header[c]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < dataRows[r].length; c++) {
                    row.createCell(c).setCellValue(dataRows[r][c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static CellType idCellType(byte[] xlsx) throws Exception {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Cell cell = wb.getSheetAt(0).getRow(1).getCell(0);
            return cell.getCellType();
        }
    }

    /** 只读姓名列，用来证明"编号列坏了但姓名列仍读得到"。 */
    public static class StringNameRow {
        @org.apache.fesod.sheet.annotation.ExcelProperty("姓名")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
