package io.github.describeadmin.excel.core;

import org.apache.fesod.sheet.annotation.ExcelProperty;

/**
 * 把 {@code 编号} 列当字符串读回来，用于断言"默认写出的是文本单元格"——
 * 若写成了数值单元格，读成 String 时会得到 {@code "1.23456789012346E18"} 这类科学计数法，
 * 与期望的精确 19 位字符串对不上。
 */
public class StringIdRow {

    @ExcelProperty("编号")
    private String id;

    @ExcelProperty("姓名")
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
