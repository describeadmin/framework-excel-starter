package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.DropdownOptionSource;
import io.github.describeadmin.excel.api.ExcelColumnDropdown;
import java.util.List;
import org.apache.fesod.sheet.annotation.ExcelProperty;

/**
 * 下拉框测试用的行模型：{@code type} 走 {@link DropdownOptionSource}（动态），{@code status}
 * 走静态 {@code value}，{@code name} 不加下拉。
 */
public class DropdownRow {

    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("单位性质")
    @ExcelColumnDropdown(source = TypeOptions.class)
    private String type;

    @ExcelProperty("状态")
    @ExcelColumnDropdown({"启用", "停用"})
    private String status;

    public DropdownRow() {
    }

    public DropdownRow(String name, String type, String status) {
        this.name = name;
        this.type = type;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** 无参可实例化——覆盖"拿不到 Spring 容器时反射兜底"这条路径。 */
    public static class TypeOptions implements DropdownOptionSource {
        @Override
        public List<String> options() {
            return List.of("国有企业", "民营企业", "外资企业");
        }
    }
}
