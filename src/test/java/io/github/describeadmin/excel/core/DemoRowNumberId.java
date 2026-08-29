package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.ExcelLongNumber;
import org.apache.fesod.sheet.annotation.ExcelProperty;

/** {@code id} 用 {@link ExcelLongNumber} 逃生舱，应写成数值单元格。 */
public class DemoRowNumberId {

    @ExcelLongNumber
    @ExcelProperty("编号")
    private Long id;

    @ExcelProperty("姓名")
    private String name;

    public DemoRowNumberId() {
    }

    public DemoRowNumberId(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
