package io.github.describeadmin.excel.web;

import io.github.describeadmin.common.api.Result;
import io.github.describeadmin.excel.api.ExcelBody;
import io.github.describeadmin.excel.api.ExcelImportResult;
import io.github.describeadmin.excel.api.ExcelWriteOptions;
import io.github.describeadmin.excel.autoconfigure.FrameworkExcelProperties;
import io.github.describeadmin.excel.core.DemoRow;
import io.github.describeadmin.excel.core.ExcelBodyArgumentResolver;
import io.github.describeadmin.excel.core.ExcelMediaType;
import io.github.describeadmin.excel.core.FesodExcelExporter;
import io.github.describeadmin.web.core.GlobalExceptionHandler;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("@ExcelBody resolver（MockMvc multipart）")
class ExcelBodyArgumentResolverTest {

    private static final long SNOWFLAKE = 1234567890123456789L;

    private final FrameworkExcelProperties properties = new FrameworkExcelProperties();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExcelBodyArgumentResolver resolver = new ExcelBodyArgumentResolver(
                new io.github.describeadmin.excel.core.FesodExcelImporter(properties));
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setCustomArgumentResolvers(resolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("上传 xlsx 绑成 List<T> 参数，中文与 19 位 Long 到位")
    void bindsUploadedXlsxToListParam() throws Exception {
        byte[] xlsx = writeRows(List.of(new DemoRow(SNOWFLAKE, "张三", "研发中心", LocalDate.of(2026, 8, 28), true)));

        mockMvc.perform(multipart("/imp/list").file(file(xlsx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("张三|1234567890123456789"));
    }

    @Test
    @DisplayName("绑成 ExcelImportResult<T>：坏的编号单元格进 errors，好行进 rows")
    void bindsToExcelImportResultParamWithRowErrors() throws Exception {
        byte[] xlsx = rawXlsx(new String[]{"编号", "姓名"},
                new String[][]{{"1", "张三"}, {"abc", "李四"}});

        mockMvc.perform(multipart("/imp/result").file(file(xlsx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows.length()").value(1))
                .andExpect(jsonPath("$.data.rows[0].name").value("张三"))
                .andExpect(jsonPath("$.data.errors.length()").value(1))
                .andExpect(jsonPath("$.data.errors[0].rowIndex").value(3))
                .andExpect(jsonPath("$.data.errors[0].column").value("编号"))
                .andExpect(jsonPath("$.data.errors[0].rejectedValue").value("abc"));
    }

    @Test
    @DisplayName("非 multipart 请求 → BizException(40000)")
    void nonMultipartRequestThrowsBadRequest() throws Exception {
        mockMvc.perform(post("/imp/list").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("请求不是 multipart 上传，无法读取 Excel 文件"));
    }

    @Test
    @DisplayName("multipart 但没有文件 part → BizException(40000)")
    void missingFilePartThrowsBadRequest() throws Exception {
        mockMvc.perform(multipart("/imp/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("未找到上传文件"));
    }

    @Test
    @DisplayName("上传的不是 Excel → BizException(40000)，由 GlobalExceptionHandler 渲染而非 500")
    void corruptFileThrowsBadRequest() throws Exception {
        MockMultipartFile bad = new MockMultipartFile(
                "file", "x.xlsx", ExcelMediaType.XLSX_VALUE, "not a zip at all".getBytes());

        mockMvc.perform(multipart("/imp/list").file(bad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("文件无法解析为 Excel")));
    }

    @Test
    @DisplayName("failFast 参数：首个坏行就抛 BizException(40000)")
    void failFastParamThrowsOnFirstRowError() throws Exception {
        byte[] xlsx = rawXlsx(new String[]{"编号", "姓名"}, new String[][]{{"abc", "张三"}});

        mockMvc.perform(multipart("/imp/ff").file(file(xlsx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    // --- helpers ---

    private static MockMultipartFile file(byte[] xlsx) {
        return new MockMultipartFile("file", "rows.xlsx", ExcelMediaType.XLSX_VALUE, xlsx);
    }

    private byte[] writeRows(List<DemoRow> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new FesodExcelExporter(properties).write(out, rows, DemoRow.class, ExcelWriteOptions.defaults());
        return out.toByteArray();
    }

    private static byte[] rawXlsx(String[] header, String[][] dataRows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Sheet1");
            var head = sheet.createRow(0);
            for (int c = 0; c < header.length; c++) {
                head.createCell(c).setCellValue(header[c]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < dataRows[r].length; c++) {
                    row.createCell(c).setCellValue(dataRows[r][c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @RestController
    static class DemoController {

        @PostMapping("/imp/list")
        Result<String> imp(@ExcelBody List<DemoRow> rows) {
            DemoRow first = rows.get(0);
            return Result.ok(first.getName() + "|" + first.getId());
        }

        @PostMapping("/imp/result")
        Result<ExcelImportResult<DemoRow>> impResult(@ExcelBody ExcelImportResult<DemoRow> result) {
            return Result.ok(result);
        }

        @PostMapping("/imp/ff")
        Result<Integer> failFast(@ExcelBody(failFast = true) List<DemoRow> rows) {
            return Result.ok(rows.size());
        }
    }
}
