package io.github.describeadmin.excel.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在自定义的导出端点方法上，把它的返回值当作 xlsx 下载流写出，而不是 JSON。
 *
 * <p>支持的返回形状（其它形状在写出时抛 {@code IllegalStateException}）：
 * <ul>
 *   <li>{@code List<T>}</li>
 *   <li>{@code io.github.describeadmin.common.api.PageResult<T>} —— 取 {@code getRecords()}</li>
 *   <li>{@code io.github.describeadmin.common.api.Result<List<T>>}</li>
 *   <li>{@code io.github.describeadmin.common.api.Result<PageResult<T>>}</li>
 * </ul>
 *
 * <p>行模型 {@code T} 的列由字段上的 {@code org.apache.fesod.sheet.annotation.ExcelProperty}
 * 决定。业务方仍然自己写端点、自己加 {@code @PreAuthorize} 控权——本插件不碰
 * {@code BaseController}，也不新增权限动作。
 *
 * <p>{@code describeadmin.excel.enabled=false} 时本注解不生效（advice 根本不注册），
 * 端点会按普通 JSON 返回。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelResponse {

    /** 下载文件名；留空则用 {@code export.xlsx}。缺 {@code .xlsx} 后缀会自动补齐，中文名按 RFC 5987 编码。 */
    String fileName() default "";

    /** 工作表名；留空则用 {@code describeadmin.excel.export.default-sheet-name}。 */
    String sheetName() default "";

    /**
     * 行模型类型。留空（{@code Void.class}）时由返回值的泛型元素类型推断；
     * 返回类型擦除严重、推断不出时才需要显式指定。
     */
    Class<?> model() default Void.class;

    /** 覆盖本端点的 {@code Long} 单元格类型，不用改字段注解。默认跟随全局配置。 */
    LongCellMode longAsText() default LongCellMode.DEFAULT;
}
