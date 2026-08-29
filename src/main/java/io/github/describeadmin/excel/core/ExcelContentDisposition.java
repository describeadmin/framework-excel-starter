package io.github.describeadmin.excel.core;

import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;

/**
 * 生成 {@code Content-Disposition} 响应头，单点、可单测。
 *
 * <p>用 Spring 的 {@link ContentDisposition} 构造——它已经按 RFC 6266 / RFC 5987 同时给出
 * ASCII 的 {@code filename="..."} 回退和 UTF-8 百分号编码的 {@code filename*=UTF-8''...}，
 * 中文文件名不需要自己拼。
 */
public final class ExcelContentDisposition {

    private static final String XLSX_SUFFIX = ".xlsx";

    private ExcelContentDisposition() {
    }

    /**
     * @param rawFileName 原始文件名，可能含中文、可能没有 {@code .xlsx} 后缀、可能含控制字符
     * @return 形如 {@code attachment; filename="export.xlsx"; filename*=UTF-8''%E5%91%98%E5%B7%A5.xlsx}
     */
    public static String header(String rawFileName) {
        String name = sanitize(rawFileName);
        ContentDisposition.Builder builder = ContentDisposition.attachment();
        // 纯 ASCII 名直接给 filename="..."，避免 Spring 对它做 RFC 2047 encoded-word（=?UTF-8?Q?...?=）；
        // 含非 ASCII 才带上 charset，此时 Spring 会同时给出 filename* 的 RFC 5987 百分号编码。
        if (isAscii(name)) {
            builder.filename(name);
        } else {
            builder.filename(name, StandardCharsets.UTF_8);
        }
        return builder.build().toString();
    }

    private static boolean isAscii(String s) {
        return s.chars().allMatch(c -> c < 128);
    }

    private static String sanitize(String rawFileName) {
        String name = rawFileName == null ? "" : rawFileName;
        // 去掉 CR/LF/双引号，避免响应头注入或畸形头
        name = name.replaceAll("[\\r\\n\"]", "").strip();
        if (name.isEmpty()) {
            name = "export";
        }
        if (!name.toLowerCase().endsWith(XLSX_SUFFIX)) {
            name = name + XLSX_SUFFIX;
        }
        return name;
    }
}
