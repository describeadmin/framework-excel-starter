package io.github.describeadmin.excel.web;

import io.github.describeadmin.common.api.PageResult;
import io.github.describeadmin.common.api.Result;
import io.github.describeadmin.excel.api.ExcelImportResult;
import io.github.describeadmin.excel.api.ExcelReadOptions;
import io.github.describeadmin.excel.api.ExcelResponse;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import io.github.describeadmin.excel.core.DemoRow;
import io.github.describeadmin.excel.core.ExcelMediaType;
import io.github.describeadmin.excel.core.ExcelResponseBodyAdvice;
import io.github.describeadmin.excel.core.FesodExcelExporter;
import io.github.describeadmin.excel.core.FesodExcelImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("@ExcelResponse advice（MockMvc）")
class ExcelResponseBodyAdviceTest {

    private static final long SNOWFLAKE = 1234567890123456789L;

    private final FrameworkExcelProperties properties = new FrameworkExcelProperties();
    private final FesodExcelImporter importer = new FesodExcelImporter(properties);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExcelResponseBodyAdvice advice = new ExcelResponseBodyAdvice(new FesodExcelExporter(properties));
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(advice)
                .build();
    }

    @Test
    @DisplayName("标注端点：返回 xlsx 流，字节能解析回原始行")
    void annotatedEndpointStreamsXlsx() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/list"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo(ExcelMediaType.XLSX_VALUE);
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("filename=\"demo.xlsx\"");

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()),
                DemoRow.class, ExcelReadOptions.defaults());
        assertThat(read.rows().get(0).getId()).isEqualTo(SNOWFLAKE);
        assertThat(read.rows().get(0).getName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("Result<PageResult<T>> 返回形状被解包为 records")
    void resultPageResultReturnShapeUnwrapped() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/page")).andExpect(status().isOk()).andReturn();

        ExcelImportResult<DemoRow> read = importer.read(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()),
                DemoRow.class, ExcelReadOptions.defaults());
        assertThat(read.rows()).extracting(DemoRow::getName).containsExactly("李四");
    }

    @Test
    @DisplayName("未标注端点：仍按普通 JSON 返回，advice 不插手")
    void plainEndpointStillJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/demo/plain")).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        assertThat(result.getResponse().getContentAsString()).startsWith("[");
    }

    @Test
    @DisplayName("不支持的返回形状：抛 IllegalStateException（有 GlobalExceptionHandler 时会渲染成 500）")
    void badReturnShapeSurfacesIllegalState() {
        assertThatThrownBy(() -> mockMvc.perform(get("/demo/bad")))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @RestController
    static class DemoController {

        @GetMapping("/demo/list")
        @ExcelResponse(fileName = "demo.xlsx")
        List<DemoRow> list() {
            return List.of(new DemoRow(SNOWFLAKE, "张三", "研发中心", LocalDate.of(2026, 8, 28), true));
        }

        @GetMapping("/demo/page")
        @ExcelResponse
        Result<PageResult<DemoRow>> page() {
            return Result.ok(new PageResult<>(
                    List.of(new DemoRow(2L, "李四", "财务部", LocalDate.of(2020, 1, 1), false)), 1, 1, 10));
        }

        @GetMapping("/demo/plain")
        List<DemoRow> plain() {
            return List.of(new DemoRow(3L, "王五", "行政部", LocalDate.of(2019, 6, 1), true));
        }

        @GetMapping("/demo/bad")
        @ExcelResponse
        String bad() {
            return "not a supported shape";
        }
    }
}
