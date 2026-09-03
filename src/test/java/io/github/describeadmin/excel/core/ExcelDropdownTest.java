package io.github.describeadmin.excel.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.describeadmin.excel.api.ExcelColumnDropdown;
import io.github.describeadmin.excel.api.ExcelWriteOptions;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@code @ExcelColumnDropdown} / {@code ExcelWriteOptions.dropdown(...)} 生成的 Excel 数据有效性。 */
@DisplayName("Excel 下拉框")
class ExcelDropdownTest {

    private final FrameworkExcelProperties properties = new FrameworkExcelProperties();
    private final FesodExcelExporter exporter = new FesodExcelExporter(properties);

    @Test
    @DisplayName("静态 value 与动态 source 各生成一列数据有效性，落在正确的列上")
    void staticAndSourceDropdownsWritten() throws Exception {
        byte[] xlsx = write(List.of(), DropdownRow.class, ExcelWriteOptions.defaults());

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            XSSFSheet data = wb.getSheetAt(0);
            List<? extends DataValidation> validations = data.getDataValidations();
            assertThat(validations).hasSize(2);

            DataValidation typeDv = validationOnColumn(validations, 1);   // 单位性质 = 第 2 列
            DataValidation statusDv = validationOnColumn(validations, 2); // 状态 = 第 3 列

            CellRangeAddress typeRange = typeDv.getRegions().getCellRangeAddresses()[0];
            assertThat(typeRange.getFirstRow()).isEqualTo(1);
            assertThat(typeRange.getLastRow()).isEqualTo(ExcelColumnDropdown.DEFAULT_LAST_ROW);
            assertThat(typeDv.getSuppressDropDownArrow()).isTrue();
            assertThat(typeDv.getValidationConstraint().getFormula1()).contains("_dd_options");
            assertThat(statusDv.getErrorStyle()).isEqualTo(DataValidation.ErrorStyle.STOP);

            int optionsIdx = wb.getSheetIndex("_dd_options");
            assertThat(optionsIdx).isGreaterThanOrEqualTo(0);
            assertThat(wb.getSheetVisibility(optionsIdx)).isEqualTo(SheetVisibility.HIDDEN);

            XSSFSheet options = wb.getSheetAt(optionsIdx);
            assertThat(cellText(options, 0, 0)).isEqualTo("国有企业");
            assertThat(cellText(options, 2, 0)).isEqualTo("外资企业");
            assertThat(cellText(options, 0, 1)).isEqualTo("启用");
            assertThat(cellText(options, 1, 1)).isEqualTo("停用");
        }
    }

    @Test
    @DisplayName("ExcelWriteOptions.dropdown(...) 按字段名覆盖注解的候选值")
    void programmaticOverrideWins() throws Exception {
        ExcelWriteOptions options = ExcelWriteOptions.builder()
                .dropdown("type", List.of("甲", "乙"))
                .build();

        byte[] xlsx = write(List.of(), DropdownRow.class, options);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            XSSFSheet opt = wb.getSheet("_dd_options");
            assertThat(cellText(opt, 0, 0)).isEqualTo("甲");
            assertThat(cellText(opt, 1, 0)).isEqualTo("乙");
            assertThat((Object) opt.getRow(2)).isNull(); // source 的三个选项没有被写进去
        }
    }

    @Test
    @DisplayName("字段名没有注解时，仅凭 ExcelWriteOptions.dropdown(...) 也能生成下拉")
    void dropdownFromOptionsOnlyForUnannotatedField() throws Exception {
        ExcelWriteOptions options = ExcelWriteOptions.builder()
                .dropdown("dept", List.of("研发", "财务"))
                .build();

        byte[] xlsx = write(List.of(), DemoRow.class, options);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            List<? extends DataValidation> validations = wb.getSheetAt(0).getDataValidations();
            assertThat(validations).hasSize(1);
            assertThat(validations.get(0).getRegions().getCellRangeAddresses()[0].getFirstColumn())
                    .isEqualTo(2); // DemoRow 第 3 列是「部门」
        }
    }

    @Test
    @DisplayName("行模型没有任何下拉配置：不生成数据有效性，也不建隐藏表")
    void noDropdownConfigLeavesFileUntouched() throws Exception {
        byte[] xlsx = write(
                List.of(new DemoRow(1L, "张三", "研发", LocalDate.now(), true)),
                DemoRow.class, ExcelWriteOptions.defaults());

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheetAt(0).getDataValidations()).isEmpty();
            assertThat(wb.getSheetIndex("_dd_options")).isEqualTo(-1);
        }
    }

    @Test
    @DisplayName("strict = false 时错误样式是 WARNING，不阻断录入")
    void nonStrictUsesWarningStyle() throws Exception {
        byte[] xlsx = write(List.of(), LooseRow.class, ExcelWriteOptions.defaults());

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            List<? extends DataValidation> validations = wb.getSheetAt(0).getDataValidations();
            assertThat(validations).hasSize(1);
            assertThat(validations.get(0).getErrorStyle()).isEqualTo(DataValidation.ErrorStyle.WARNING);
        }
    }

    // --- helpers ---

    private <T> byte[] write(List<T> rows, Class<T> model, ExcelWriteOptions options) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.write(out, rows, model, options);
        return out.toByteArray();
    }

    private static DataValidation validationOnColumn(List<? extends DataValidation> validations, int column) {
        return validations.stream()
                .filter(v -> v.getRegions().getCellRangeAddresses()[0].getFirstColumn() == column)
                .findFirst()
                .orElseThrow(() -> new AssertionError("没有作用在第 " + column + " 列的数据有效性"));
    }

    private static String cellText(XSSFSheet sheet, int row, int col) {
        return sheet.getRow(row).getCell(col).getStringCellValue();
    }

    /** strict = false 的下拉。 */
    public static class LooseRow {
        @ExcelProperty("等级")
        @ExcelColumnDropdown(value = {"A", "B", "C"}, strict = false)
        private String grade;

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }
    }
}
