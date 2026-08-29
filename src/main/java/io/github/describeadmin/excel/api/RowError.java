package io.github.describeadmin.excel.api;

/**
 * 导入时某一行的一处错误。
 *
 * @param rowIndex      1 基的电子表格行号（表头是第 1 行，首个数据行是第 2 行）——
 *                      直接对应用户在 Excel 里看到的行号，便于定位
 * @param field         出错的 Java 字段名；整行级错误（如超出行数上限）为 {@code null}
 * @param column        该字段解析出的表头文本（{@code @ExcelProperty} 的值或字段名）；可能为 {@code null}
 * @param message       给人看的错误描述
 * @param rejectedValue 被拒绝的原始单元格值；可能为 {@code null}
 */
public record RowError(int rowIndex, String field, String column, String message, Object rejectedValue) {

    /** 整行级错误（没有具体到某个字段）。 */
    public static RowError wholeRow(int rowIndex, String message) {
        return new RowError(rowIndex, null, null, message, null);
    }
}
