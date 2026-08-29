package io.github.describeadmin.excel.api;

import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.util.List;

/**
 * 导出门面。不依赖 Spring MVC，可在任意代码里直接注入使用；{@code @ExcelResponse} 只是
 * 建在它之上的糖。
 *
 * <p>{@code Long} / {@code long} 字段默认写成<b>文本</b>单元格（雪花 ID 19 位，超出 Excel
 * 数值精度，见 CLAUDE.md 4.8）。用 {@link ExcelLongNumber} 逐字段、
 * {@link ExcelWriteOptions#longAsText()} 逐次调用改这个默认。
 */
public interface ExcelExporter {

    /**
     * 把 {@code rows} 写成 xlsx 到 {@code out}。不关闭 {@code out}。
     *
     * @param model {@code T} 的运行时类型（列由其字段上的 {@code @ExcelProperty} 决定）
     * @throws io.github.describeadmin.common.api.BizException 行数超过上限时（写出前抛，{@code code=40000}）
     * @throws ExcelIoException 写出过程中发生 I/O 失败
     */
    <T> void write(OutputStream out, List<T> rows, Class<T> model, ExcelWriteOptions options);

    /**
     * 设置 xlsx 的 {@code Content-Type} 与 {@code Content-Disposition}（RFC 5987 处理中文名），
     * 然后把 {@code rows} 流式写进响应体。不关闭响应流。
     *
     * <p>行数超过上限时在写第一个字节之前抛 {@code BizException(BAD_REQUEST, ...)}，
     * 这样框架能正常渲染 JSON 错误；一旦开始写字节就无法再回退。
     */
    <T> void writeToResponse(HttpServletResponse response, List<T> rows, Class<T> model, ExcelWriteOptions options);
}
