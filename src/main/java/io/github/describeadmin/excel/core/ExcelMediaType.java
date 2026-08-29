package io.github.describeadmin.excel.core;

import org.springframework.http.MediaType;

/** xlsx 的 MIME 类型常量。 */
public final class ExcelMediaType {

    /** {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet} */
    public static final String XLSX_VALUE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final MediaType XLSX = MediaType.parseMediaType(XLSX_VALUE);

    private ExcelMediaType() {
    }
}
