package io.github.describeadmin.excel.api;

/**
 * 导出时 {@code Long} / {@code long} 字段写成什么类型的单元格。
 *
 * <p>雪花 ID 是 19 位，超过 Excel 数值单元格约 15 位有效数字的精度（与 JavaScript
 * {@code Number.MAX_SAFE_INTEGER} 同类问题，见 CLAUDE.md 4.8）。因此默认写成文本。
 */
public enum LongCellMode {

    /** 跟随全局配置 {@code describeadmin.excel.export.long-as-text}（默认 true，即文本）。 */
    DEFAULT,

    /** 强制写成文本单元格。 */
    TEXT,

    /** 强制写成数值单元格——仅当确定该字段不会超过 2^53 时才用。 */
    NUMBER
}
