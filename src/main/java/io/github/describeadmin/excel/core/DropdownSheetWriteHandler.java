package io.github.describeadmin.excel.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fesod.sheet.metadata.Head;
import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.fesod.sheet.write.property.ExcelWriteHeadProperty;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 给指定列加「下拉框」（Excel 数据有效性）。候选值写进一张隐藏工作表、有效性用公式引用它，
 * 从而绕开 Excel「内联清单 255 字符」上限。
 *
 * <p>列位置在这里才定：{@code afterSheetCreate} 时从 Fesod 已算好的表头映射
 * （{@link ExcelWriteHeadProperty#getHeadMap()}）按字段名反查列下标，因此
 * {@code @ExcelProperty(index = ...)} / {@code excludeFields} 造成的错位都不受影响。
 */
final class DropdownSheetWriteHandler implements SheetWriteHandler {

    private static final Logger log = LoggerFactory.getLogger(DropdownSheetWriteHandler.class);

    /** 承载下拉选项的隐藏工作表名。带下划线前缀，降低与业务表重名的概率。 */
    private static final String OPTIONS_SHEET = "_dd_options";

    /** 一列下拉的最终配置（列位置留到运行期按字段名反查）。 */
    record FieldDropdown(String fieldName, List<String> options, boolean strict, int firstRow, int lastRow) {
    }

    private final List<FieldDropdown> dropdowns;

    DropdownSheetWriteHandler(List<FieldDropdown> dropdowns) {
        this.dropdowns = dropdowns;
    }

    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        if (dropdowns == null || dropdowns.isEmpty()) {
            return;
        }
        Map<String, Integer> columnByField = columnByField(context.getWriteSheetHolder().getExcelWriteHeadProperty());
        if (columnByField.isEmpty()) {
            log.debug("表头映射为空（非 class 模型？），跳过全部下拉");
            return;
        }

        Sheet sheet = context.getWriteSheetHolder().getSheet();
        Workbook workbook = context.getWriteWorkbookHolder().getWorkbook();
        Sheet optionsSheet = optionsSheet(workbook);
        DataValidationHelper helper = sheet.getDataValidationHelper();

        int optionColumn = 0;
        for (FieldDropdown dropdown : dropdowns) {
            if (dropdown.options() == null || dropdown.options().isEmpty()) {
                continue;
            }
            Integer targetColumn = columnByField.get(dropdown.fieldName());
            if (targetColumn == null) {
                log.debug("字段 {} 不在导出列里，跳过其下拉", dropdown.fieldName());
                continue;
            }

            writeOptionColumn(optionsSheet, optionColumn, dropdown.options());
            String columnLetter = CellReference.convertNumToColString(optionColumn);
            String formula = "'" + OPTIONS_SHEET + "'!$" + columnLetter + "$1:$"
                    + columnLetter + "$" + dropdown.options().size();

            DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
            CellRangeAddressList range = new CellRangeAddressList(
                    dropdown.firstRow(), dropdown.lastRow(), targetColumn, targetColumn);
            DataValidation validation = helper.createValidation(constraint, range);
            validation.setSuppressDropDownArrow(true); // XSSF：true 才会画出下拉箭头
            validation.setEmptyCellAllowed(true);
            validation.setShowErrorBox(true);
            validation.setErrorStyle(dropdown.strict()
                    ? DataValidation.ErrorStyle.STOP : DataValidation.ErrorStyle.WARNING);
            validation.createErrorBox("输入无效", "请从下拉列表中选择一个值");
            sheet.addValidationData(validation);

            optionColumn++;
        }
    }

    private static Map<String, Integer> columnByField(ExcelWriteHeadProperty headProperty) {
        Map<String, Integer> byField = new HashMap<>();
        if (headProperty == null || headProperty.getHeadMap() == null) {
            return byField;
        }
        for (Map.Entry<Integer, Head> entry : headProperty.getHeadMap().entrySet()) {
            String fieldName = entry.getValue().getFieldName();
            if (fieldName != null) {
                byField.put(fieldName, entry.getKey());
            }
        }
        return byField;
    }

    private static Sheet optionsSheet(Workbook workbook) {
        Sheet existing = workbook.getSheet(OPTIONS_SHEET);
        if (existing != null) {
            return existing;
        }
        Sheet created = workbook.createSheet(OPTIONS_SHEET);
        workbook.setSheetVisibility(workbook.getSheetIndex(created), SheetVisibility.HIDDEN);
        return created;
    }

    private static void writeOptionColumn(Sheet optionsSheet, int column, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            Row row = optionsSheet.getRow(i);
            if (row == null) {
                row = optionsSheet.createRow(i);
            }
            row.createCell(column).setCellValue(values.get(i));
        }
    }
}
