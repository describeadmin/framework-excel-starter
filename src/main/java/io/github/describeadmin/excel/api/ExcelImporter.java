package io.github.describeadmin.excel.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 导入门面。不依赖 Spring MVC；{@code @ExcelBody} 只是建在它之上的糖。
 *
 * <p><b>单行</b>的解析 / 校验失败被收集进 {@link ExcelImportResult#errors()}，不抛异常；
 * 只有整个流不是可读的工作簿时才抛 {@link ExcelParseException}。文本形态的
 * {@code Long} 单元格会被解析回 {@code Long}（空 → {@code null}，非数字 → {@link RowError}）。
 */
public interface ExcelImporter {

    /** 从 {@code in} 读取。不关闭 {@code in}。 */
    <T> ExcelImportResult<T> read(InputStream in, Class<T> model, ExcelReadOptions options);

    /** 全默认选项的便捷重载。 */
    default <T> ExcelImportResult<T> read(InputStream in, Class<T> model) {
        return read(in, model, ExcelReadOptions.defaults());
    }

    /** 从上传文件读取。 */
    <T> ExcelImportResult<T> read(MultipartFile file, Class<T> model, ExcelReadOptions options);

    /** 全默认选项的便捷重载。 */
    default <T> ExcelImportResult<T> read(MultipartFile file, Class<T> model) {
        return read(file, model, ExcelReadOptions.defaults());
    }
}
