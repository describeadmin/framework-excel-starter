package io.github.describeadmin.excel.core;

import io.github.describeadmin.common.api.PageResult;
import io.github.describeadmin.common.api.Result;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;

import java.util.List;

/**
 * 把 {@code @ExcelResponse} 端点的返回值 / 返回类型，规约成"行列表 + 行类型"。
 *
 * <p>支持的形状（D5）：{@code List<T>} / {@code PageResult<T>} / {@code Result<List<T>>} /
 * {@code Result<PageResult<T>>}。其它一律抛 {@code IllegalStateException}——这是开发期错误，
 * 不该悄悄降级。
 */
final class ResponseShapeUnwrapper {

    private ResponseShapeUnwrapper() {
    }

    /** 从运行时返回值里取出行列表。 */
    static List<?> unwrap(Object body) {
        if (body instanceof Result<?> result) {
            return unwrap(result.getData());
        }
        if (body instanceof PageResult<?> page) {
            return page.getRecords();
        }
        if (body instanceof List<?> list) {
            return list;
        }
        throw new IllegalStateException("@ExcelResponse 不支持的返回类型: "
                + (body == null ? "null" : body.getClass().getName())
                + "。仅支持 List<T> / PageResult<T> / Result<List<T>> / Result<PageResult<T>>");
    }

    /**
     * 从方法返回类型的泛型里推断行类型 {@code T}。
     * 逐层下钻 {@code Result} → {@code PageResult} / {@code List} → 元素类型。
     *
     * @return 推断出的行类型；推断不出时返回 {@code null}（由调用方回退到 {@code @ExcelResponse.model()}）
     */
    static Class<?> elementType(MethodParameter returnType) {
        return resolve(ResolvableType.forMethodParameter(returnType));
    }

    private static Class<?> resolve(ResolvableType type) {
        Class<?> raw = type.resolve();
        if (raw == null) {
            return null;
        }
        if (Result.class.isAssignableFrom(raw)) {
            return resolve(type.getGeneric(0));
        }
        if (PageResult.class.isAssignableFrom(raw)) {
            return type.getGeneric(0).resolve();
        }
        if (List.class.isAssignableFrom(raw)) {
            return type.getGeneric(0).resolve();
        }
        return null;
    }
}
