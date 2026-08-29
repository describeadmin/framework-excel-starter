package io.github.describeadmin.excel.api;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 导出选项。不可变；用 {@link #builder()} 构造，{@link #defaults()} 取全默认。
 *
 * <p>可空的包装类型字段（{@code longAsText} / {@code maxRows} / {@code sheetWriteWindow}）
 * 取 {@code null} 表示"继承 {@code describeadmin.excel.export.*} 的配置值"。
 */
public final class ExcelWriteOptions {

    private final String sheetName;
    private final String fileName;
    private final LongCellMode longAsText;
    private final Integer maxRows;
    private final Integer sheetWriteWindow;
    private final Set<String> includeFields;
    private final Set<String> excludeFields;

    private ExcelWriteOptions(Builder b) {
        this.sheetName = b.sheetName;
        this.fileName = b.fileName;
        this.longAsText = b.longAsText;
        this.maxRows = b.maxRows;
        this.sheetWriteWindow = b.sheetWriteWindow;
        this.includeFields = b.includeFields == null ? null : Set.copyOf(b.includeFields);
        this.excludeFields = b.excludeFields == null ? null : Set.copyOf(b.excludeFields);
    }

    public static ExcelWriteOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 工作表名；{@code null} 表示用配置里的默认名。 */
    public String sheetName() {
        return sheetName;
    }

    /** 下载文件名；{@code null} 表示用 {@code export.xlsx}。缺 {@code .xlsx} 后缀时由写出方补齐。 */
    public String fileName() {
        return fileName;
    }

    /** {@code Long} 单元格类型；{@code null} 表示 {@link LongCellMode#DEFAULT}。 */
    public LongCellMode longAsText() {
        return longAsText;
    }

    /** 行数上限；{@code null} 表示用配置值，{@code 0} 表示不限。 */
    public Integer maxRows() {
        return maxRows;
    }

    /** 流式写出时的内存行窗口；{@code null} 表示用配置值，{@code 0} 表示 Fesod 默认。 */
    public Integer sheetWriteWindow() {
        return sheetWriteWindow;
    }

    /** 仅导出这些字段（Java 字段名）；{@code null} 表示不限制。 */
    public Set<String> includeFields() {
        return includeFields;
    }

    /** 排除这些字段（Java 字段名）；{@code null} 表示不排除。 */
    public Set<String> excludeFields() {
        return excludeFields;
    }

    public static final class Builder {
        private String sheetName;
        private String fileName;
        private LongCellMode longAsText;
        private Integer maxRows;
        private Integer sheetWriteWindow;
        private Set<String> includeFields;
        private Set<String> excludeFields;

        public Builder sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder longAsText(LongCellMode longAsText) {
            this.longAsText = longAsText;
            return this;
        }

        public Builder maxRows(Integer maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public Builder sheetWriteWindow(Integer sheetWriteWindow) {
            this.sheetWriteWindow = sheetWriteWindow;
            return this;
        }

        public Builder includeFields(Collection<String> fields) {
            this.includeFields = fields == null ? null : new LinkedHashSet<>(fields);
            return this;
        }

        public Builder excludeFields(Collection<String> fields) {
            this.excludeFields = fields == null ? null : new LinkedHashSet<>(fields);
            return this;
        }

        public ExcelWriteOptions build() {
            return new ExcelWriteOptions(this);
        }
    }
}
