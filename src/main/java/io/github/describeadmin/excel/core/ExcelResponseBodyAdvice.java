package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.ExcelExporter;
import io.github.describeadmin.excel.api.ExcelResponse;
import io.github.describeadmin.excel.api.ExcelWriteOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * 拦截标了 {@link ExcelResponse} 的端点，把返回值当作 xlsx 下载流写出。
 *
 * <p><b>不干扰其它端点</b>：{@link #supports} 只做一次注解查找；命中后把字节直接写进裸的
 * {@link HttpServletResponse} 流并返回 {@code null}——Spring 的
 * {@code AbstractMessageConverterMethodProcessor} 对 {@code null} body 会跳过消息转换
 * （"Nothing to write: null body"），不会再往流里追加任何东西。
 *
 * <p>写到一半才发生的 {@link io.github.describeadmin.excel.api.ExcelIoException} 会冒泡到
 * 框架的 {@code GlobalExceptionHandler} 兜底分支（此时字节已提交，只能记录）。
 * 行数超限的 {@code BizException} 在写第一个字节之前抛出，能正常渲染成 JSON 错误。
 */
@RestControllerAdvice
public class ExcelResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ExcelExporter exporter;

    public ExcelResponseBodyAdvice(ExcelExporter exporter) {
        this.exporter = exporter;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethodAnnotation(ExcelResponse.class) != null;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        ExcelResponse annotation = returnType.getMethodAnnotation(ExcelResponse.class);
        List<?> rows = ResponseShapeUnwrapper.unwrap(body);
        Class<?> model = resolveModel(annotation, returnType, rows);

        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        writeXlsx(servletResponse, rows, model, annotation);
        return null;
    }

    private Class<?> resolveModel(ExcelResponse annotation, MethodParameter returnType, List<?> rows) {
        if (annotation.model() != Void.class) {
            return annotation.model();
        }
        Class<?> inferred = ResponseShapeUnwrapper.elementType(returnType);
        if (inferred != null && inferred != Object.class) {
            return inferred;
        }
        if (!rows.isEmpty() && rows.get(0) != null) {
            return rows.get(0).getClass();
        }
        throw new IllegalStateException(
                "@ExcelResponse 无法推断行类型，请用 @ExcelResponse(model = Xxx.class) 显式指定");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void writeXlsx(HttpServletResponse response, List<?> rows, Class<?> model, ExcelResponse annotation) {
        ExcelWriteOptions options = ExcelWriteOptions.builder()
                .fileName(annotation.fileName().isEmpty() ? null : annotation.fileName())
                .sheetName(annotation.sheetName().isEmpty() ? null : annotation.sheetName())
                .longAsText(annotation.longAsText())
                .build();
        exporter.writeToResponse(response, (List) rows, (Class) model, options);
    }
}
