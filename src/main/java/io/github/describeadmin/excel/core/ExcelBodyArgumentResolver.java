package io.github.describeadmin.excel.core;

import io.github.describeadmin.common.api.BizException;
import io.github.describeadmin.common.api.ResultCode;
import io.github.describeadmin.excel.api.ExcelBody;
import io.github.describeadmin.excel.api.ExcelImportResult;
import io.github.describeadmin.excel.api.ExcelImporter;
import io.github.describeadmin.excel.api.ExcelParseException;
import io.github.describeadmin.excel.api.ExcelReadOptions;
import io.github.describeadmin.excel.api.RowError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Collection;

/**
 * 把 {@code @ExcelBody} 标注的方法参数，从 multipart 上传的 Excel 文件绑成
 * {@code List<T>} 或 {@code ExcelImportResult<T>}。
 *
 * <p>非 multipart 请求、找不到文件、文件无法解析为 Excel，一律抛
 * {@code BizException(ResultCode.BAD_REQUEST, ...)}，由框架 {@code GlobalExceptionHandler}
 * 渲染成 {@code Result}（HTTP 200 + body {@code code=40000}），而不是 500。
 */
public class ExcelBodyArgumentResolver implements HandlerMethodArgumentResolver {

    private final ExcelImporter importer;

    public ExcelBodyArgumentResolver(ExcelImporter importer) {
        this.importer = importer;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExcelBody.class);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        ExcelBody annotation = parameter.getParameterAnnotation(ExcelBody.class);
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (!(request instanceof MultipartHttpServletRequest multipart)) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求不是 multipart 上传，无法读取 Excel 文件");
        }

        MultipartFile file = pickFile(multipart, annotation.part());
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    annotation.part().isEmpty() ? "未找到上传文件" : "未找到名为 " + annotation.part() + " 的上传文件");
        }

        Class<?> model = resolveModel(annotation, parameter);
        ExcelReadOptions options = ExcelReadOptions.builder()
                .trim(annotation.trim())
                .failFast(annotation.failFast())
                .maxRows(annotation.maxRows() > 0 ? annotation.maxRows() : null)
                .build();

        ExcelImportResult<?> result;
        try {
            result = importer.read(file, (Class) model, options);
        } catch (ExcelParseException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件无法解析为 Excel：" + e.getMessage());
        }

        if (ExcelImportResult.class.isAssignableFrom(parameter.getParameterType())) {
            return result;
        }
        if (annotation.failFast() && result.hasErrors()) {
            RowError first = result.errors().get(0);
            String where = "第 " + first.rowIndex() + " 行"
                    + (first.column() == null ? "" : "（" + first.column() + "）");
            throw new BizException(ResultCode.BAD_REQUEST, where + ": " + first.message());
        }
        return result.rows();
    }

    private static MultipartFile pickFile(MultipartHttpServletRequest request, String part) {
        if (!part.isEmpty()) {
            return request.getFile(part);
        }
        Collection<MultipartFile> files = request.getFileMap().values();
        return files.isEmpty() ? null : files.iterator().next();
    }

    private static Class<?> resolveModel(ExcelBody annotation, MethodParameter parameter) {
        if (annotation.model() != Void.class) {
            return annotation.model();
        }
        Class<?> generic = ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve();
        if (generic != null && generic != Object.class) {
            return generic;
        }
        throw new IllegalStateException(
                "@ExcelBody 无法推断行类型，请用 @ExcelBody(model = Xxx.class) 显式指定");
    }
}
