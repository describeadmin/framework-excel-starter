package io.github.describeadmin.excel.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

/** {@code describeadmin.excel} 前缀下的全部配置项。 */
@ConfigurationProperties(prefix = "describeadmin.excel")
public class FrameworkExcelProperties {

    /** 顶层运行时开关，关掉后等同于没引这个 jar（advice / resolver 都不注册）。 */
    private boolean enabled = true;

    @NestedConfigurationProperty
    private final Export export = new Export();

    @NestedConfigurationProperty
    private final Import imp = new Import();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Export getExport() {
        return export;
    }

    /**
     * 绑定 {@code describeadmin.excel.import.*}。
     *
     * <p>{@code import} 只有作为「语句」时才是关键字，用作方法名的一部分完全合法，
     * 所以 getter 就叫 {@code getImport()}——Spring Boot 按 JavaBean 属性名推导，
     * 得到的键前缀正是 {@code import}。字段名不能叫 {@code import}，故用 {@code imp}。
     */
    public Import getImport() {
        return imp;
    }

    /** 导出相关，前缀 {@code describeadmin.excel.export}。 */
    public static class Export {

        /** 未指定工作表名时用它。 */
        private String defaultSheetName = "Sheet1";

        /** {@code Long} / {@code long} 字段默认写成文本单元格（见 CLAUDE.md 4.8）。 */
        private boolean longAsText = true;

        /** 单次导出的行数上限；超过时写出前抛 {@code BizException(BAD_REQUEST)}。{@code 0} 表示不限。 */
        private int maxRows = 100_000;

        /** 流式写出时的内存行窗口；{@code 0} 表示用 Fesod 默认。 */
        private int sheetWriteWindow = 0;

        public String getDefaultSheetName() {
            return defaultSheetName;
        }

        public void setDefaultSheetName(String defaultSheetName) {
            this.defaultSheetName = defaultSheetName;
        }

        public boolean isLongAsText() {
            return longAsText;
        }

        public void setLongAsText(boolean longAsText) {
            this.longAsText = longAsText;
        }

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        public int getSheetWriteWindow() {
            return sheetWriteWindow;
        }

        public void setSheetWriteWindow(int sheetWriteWindow) {
            this.sheetWriteWindow = sheetWriteWindow;
        }
    }

    /**
     * 导入相关，前缀 {@code describeadmin.excel.import}。
     *
     * <p>{@code import} 作为类型名合法（它只在语句位置才是关键字）。
     */
    public static class Import {

        /** 数据行数上限；{@code 0} 表示不限。 */
        private int maxRows = 50_000;

        /** 上传文件大小上限；{@code null}（不设）表示完全交给 {@code spring.servlet.multipart.max-file-size}。 */
        private DataSize maxFileSize;

        /** 裁剪字符串字段两端空白。 */
        private boolean trimStrings = true;

        /** 遇到第一处错误就停止并抛出，而不是收集全部 {@code RowError}。 */
        private boolean failFast = false;

        /** 表头所在行数（1 基）；数据从 {@code headRowNumber + 1} 开始。 */
        private int headRowNumber = 1;

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        public DataSize getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(DataSize maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public boolean isTrimStrings() {
            return trimStrings;
        }

        public void setTrimStrings(boolean trimStrings) {
            this.trimStrings = trimStrings;
        }

        public boolean isFailFast() {
            return failFast;
        }

        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }

        public int getHeadRowNumber() {
            return headRowNumber;
        }

        public void setHeadRowNumber(int headRowNumber) {
            this.headRowNumber = headRowNumber;
        }
    }
}
