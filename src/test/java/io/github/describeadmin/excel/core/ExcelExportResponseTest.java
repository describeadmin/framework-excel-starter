package io.github.describeadmin.excel.core;

import io.github.describeadmin.common.api.BizException;
import io.github.describeadmin.excel.api.ExcelImportResult;
import io.github.describeadmin.excel.api.ExcelReadOptions;
import io.github.describeadmin.excel.api.ExcelWriteOptions;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("门面写入 HttpServletResponse")
class ExcelExportResponseTest {

    private final FrameworkExcelProperties properties = new FrameworkExcelProperties();
    private final FesodExcelExporter exporter = new FesodExcelExporter(properties);
    private final FesodExcelImporter importer = new FesodExcelImporter(properties);

    @Test
    @DisplayName("设置 xlsx 的 Content-Type 与 attachment Content-Disposition（中文名 RFC 5987）")
    void setsXlsxContentTypeAndAttachmentDisposition() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        exporter.writeToResponse(response,
                List.of(new DemoRow(1234567890123456789L, "张三", "研发中心", LocalDate.of(2026, 8, 28), true)),
                DemoRow.class,
                ExcelWriteOptions.builder().fileName("员工名单.xlsx").build());

        assertThat(response.getContentType()).isEqualTo(ExcelMediaType.XLSX_VALUE);
        String disposition = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).startsWith("attachment;");
        assertThat(disposition).contains("filename*=UTF-8''").contains("%E5%91%98%E5%B7%A5");
        assertThat(response.getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @DisplayName("响应字节能被解析回原始行（中文 + 19 位 Long）")
    void responseBytesParseBackToOriginalRows() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        exporter.writeToResponse(response,
                List.of(new DemoRow(1234567890123456789L, "张三", "研发中心", LocalDate.of(2026, 8, 28), true)),
                DemoRow.class, ExcelWriteOptions.defaults());

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(response.getContentAsByteArray()), DemoRow.class, ExcelReadOptions.defaults());

        assertThat(read.rows().get(0).getId()).isEqualTo(1234567890123456789L);
        assertThat(read.rows().get(0).getName()).isEqualTo("张三");
        assertThat(read.rows().get(0).getDept()).isEqualTo("研发中心");
    }

    @Test
    @DisplayName("导出行数超限：抛 BizException(40000)，响应体一个字节都没写")
    void exportRowLimitThrowsBizExceptionBeforeWriting() {
        properties.getExport().setMaxRows(1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<DemoRow> two = List.of(
                new DemoRow(1L, "a", "x", LocalDate.now(), true),
                new DemoRow(2L, "b", "y", LocalDate.now(), true));

        assertThatThrownBy(() -> exporter.writeToResponse(response, two, DemoRow.class, ExcelWriteOptions.defaults()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40000));
        assertThat(response.getContentAsByteArray()).isEmpty();
    }
}
