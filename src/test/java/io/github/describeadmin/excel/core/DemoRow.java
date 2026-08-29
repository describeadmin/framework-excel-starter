package io.github.describeadmin.excel.core;

import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.time.LocalDate;
import java.util.Objects;

/** 往返测试用的行模型。{@code id} 是 {@code Long}，默认应写成文本单元格。 */
public class DemoRow {

    @ExcelProperty("编号")
    private Long id;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("部门")
    private String dept;

    @ExcelProperty("入职日期")
    private LocalDate hireDate;

    @ExcelProperty("在编")
    private Boolean active;

    public DemoRow() {
    }

    public DemoRow(Long id, String name, String dept, LocalDate hireDate, Boolean active) {
        this.id = id;
        this.name = name;
        this.dept = dept;
        this.hireDate = hireDate;
        this.active = active;
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

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DemoRow other)) {
            return false;
        }
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(dept, other.dept)
                && Objects.equals(hireDate, other.hireDate)
                && Objects.equals(active, other.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dept, hireDate, active);
    }
}
