package io.github.describeadmin.excel.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Content-Disposition 头")
class ExcelContentDispositionTest {

    @Test
    @DisplayName("中文名给出 RFC 5987 的 filename* 百分号编码")
    void chineseName() {
        String header = ExcelContentDisposition.header("员工.xlsx");
        assertThat(header).startsWith("attachment;");
        assertThat(header).contains("filename*=UTF-8''");
        // "员工" 的 UTF-8 百分号编码
        assertThat(header).contains("%E5%91%98%E5%B7%A5");
    }

    @Test
    @DisplayName("纯 ASCII 名保留 filename=\"...\"")
    void asciiName() {
        assertThat(ExcelContentDisposition.header("plain.xlsx"))
                .contains("filename=\"plain.xlsx\"");
    }

    @Test
    @DisplayName("缺 .xlsx 后缀会补上")
    void appendsSuffix() {
        assertThat(ExcelContentDisposition.header("no-ext")).contains("no-ext.xlsx");
    }

    @Test
    @DisplayName("空名回退成 export.xlsx")
    void blankFallsBack() {
        assertThat(ExcelContentDisposition.header(null)).contains("export.xlsx");
        assertThat(ExcelContentDisposition.header("   ")).contains("export.xlsx");
    }

    @Test
    @DisplayName("剥掉 CR/LF/引号，防头注入")
    void stripsControlChars() {
        String header = ExcelContentDisposition.header("a\r\nb.xlsx");
        assertThat(header).doesNotContain("\r").doesNotContain("\n");
        assertThat(header).contains("ab.xlsx");
    }
}
