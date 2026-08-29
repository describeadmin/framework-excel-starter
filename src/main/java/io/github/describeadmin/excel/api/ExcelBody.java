package io.github.describeadmin.excel.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在导入端点的方法参数上，把 multipart 上传的 Excel 文件解析成行对象。
 *
 * <p>参数类型可以是：
 * <ul>
 *   <li>{@code List<T>} —— 只拿解析并校验通过的行；{@code failFast=true} 且存在错误时抛
 *       {@code BizException(BAD_REQUEST, 首个错误)}</li>
 *   <li>{@code ExcelImportResult<T>} —— 同时拿到通过的行和逐行错误清单（推荐）</li>
 * </ul>
 *
 * <p>非 multipart 请求、找不到文件、文件不是可读的 Excel，一律抛
 * {@code BizException(ResultCode.BAD_REQUEST, ...)}，由框架全局异常处理器渲染成
 * {@code Result}（HTTP 200 + body {@code code=40000}）。
 *
 * <p>{@code describeadmin.excel.enabled=false} 时本注解不生效（resolver 根本不注册）。
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelBody {

    /**
     * 行模型类型。留空（{@code Void.class}）时由参数的泛型（{@code List<X>} / {@code ExcelImportResult<X>}）
     * 推断；推断不出时才需要显式指定。
     */
    Class<?> model() default Void.class;

    /** 遇到第一处错误就抛 {@code BizException}，而不是收集全部。默认 false（收集）。 */
    boolean failFast() default false;

    /** 裁剪字符串字段两端空白。默认 true。 */
    boolean trim() default true;

    /** 数据行数上限；{@code 0} 表示用 {@code describeadmin.excel.import.max-rows} 的配置值。 */
    int maxRows() default 0;

    /** multipart part 名；留空则取请求里的第一个文件 part。 */
    String part() default "";
}
