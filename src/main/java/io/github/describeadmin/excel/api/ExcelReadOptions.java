package io.github.describeadmin.excel.api;

/**
 * 导入选项。不可变；用 {@link #builder()} 构造，{@link #defaults()} 取全默认。
 *
 * <p>可空的包装类型字段取 {@code null} 表示"继承 {@code describeadmin.excel.import.*} 的配置值"。
 */
public final class ExcelReadOptions {

    private final Integer headRowNumber;
    private final Integer maxRows;
    private final Boolean trim;
    private final Boolean failFast;

    private ExcelReadOptions(Builder b) {
        this.headRowNumber = b.headRowNumber;
        this.maxRows = b.maxRows;
        this.trim = b.trim;
        this.failFast = b.failFast;
    }

    public static ExcelReadOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 表头所在行数（1 基）；{@code null} 表示用配置值（默认 1，数据从第 2 行开始）。 */
    public Integer headRowNumber() {
        return headRowNumber;
    }

    /** 数据行数上限；{@code null} 表示用配置值，{@code 0} 表示不限。 */
    public Integer maxRows() {
        return maxRows;
    }

    /** 是否裁剪字符串字段两端空白；{@code null} 表示用配置值（默认 true）。 */
    public Boolean trim() {
        return trim;
    }

    /**
     * 收集完再由调用方决定怎么处理时为 {@code false}（收集全部 {@code RowError}）；
     * 遇到第一处错误就停止读取为 {@code true}。{@code null} 表示用配置值（默认 false）。
     *
     * <p>注意：门面本身<b>永远不因单行问题抛异常</b>，{@code failFast=true} 只是让它
     * 提前停止、把已发现的那一条错误放进 {@code errors()} 返回。是否要因此抛
     * {@code BizException} 由上层（{@code @ExcelBody} resolver）决定。
     */
    public Boolean failFast() {
        return failFast;
    }

    public static final class Builder {
        private Integer headRowNumber;
        private Integer maxRows;
        private Boolean trim;
        private Boolean failFast;

        public Builder headRowNumber(Integer headRowNumber) {
            this.headRowNumber = headRowNumber;
            return this;
        }

        public Builder maxRows(Integer maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public Builder trim(Boolean trim) {
            this.trim = trim;
            return this;
        }

        public Builder failFast(Boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public ExcelReadOptions build() {
            return new ExcelReadOptions(this);
        }
    }
}
