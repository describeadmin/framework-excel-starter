package io.github.describeadmin.excel.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在导出行模型的字段上：给这一列在生成的 xlsx 里加一个「下拉框」（Excel 数据有效性），
 * 用户从预设选项里选而不是手填。<b>只影响导出 / 模板下载</b>，导入侧不读它
 * （导入的合法性仍由业务代码自己校验）。
 *
 * <p>候选值来源二选一：
 * <ul>
 *   <li>{@link #value()} —— 写死的静态选项，如 {@code {"启用", "停用"}}；</li>
 *   <li>{@link #source()} —— 一个 {@link DropdownOptionSource} 实现，运行期取（字典 / DB）。</li>
 * </ul>
 * 两者都给时 {@link #source()} 优先。都为空、或 {@code source} 解析出空列表时，这一列
 * 不加下拉（静默跳过，不报错）。
 *
 * <p>还有一条<b>更高优先级</b>的旁路：{@link ExcelWriteOptions.Builder#dropdown(String, java.util.List)}
 * 按字段名直接塞选项，覆盖本注解——适合调用方手上已经有选项数据、不想再单独写一个
 * {@link DropdownOptionSource} 的场合（比如 Controller 里已经查好了字典 label 列表）。
 *
 * <p><b>实现细节</b>：选项写进一张隐藏工作表，数据有效性用公式引用它，因此不受 Excel
 * 「内联清单 255 字符」限制。下拉作用在数据区的 {@code [firstRow, lastRow]} 行（0 基、含两端，
 * 默认 {@code 1..}{@value #DEFAULT_LAST_ROW}，即从表头下面第一行起）。列位置由框架按 Fesod
 * 实际的表头映射推导，{@code @ExcelProperty(index = ...)} / {@code excludeFields} 都会被正确对齐。
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumnDropdown {

    /** {@link #lastRow()} 的默认值。 */
    int DEFAULT_LAST_ROW = 5000;

    /** 静态候选值。与 {@link #source()} 二选一；都给时 {@code source} 优先。 */
    String[] value() default {};

    /**
     * 动态候选值来源。默认 {@link DropdownOptionSource.None} 表示「不用动态来源」。
     * 优先按类型解析为 Spring Bean；拿不到容器时退化为无参实例化。
     */
    Class<? extends DropdownOptionSource> source() default DropdownOptionSource.None.class;

    /**
     * 严格模式。{@code true}（默认）：填了选项外的值，Excel 弹错并拒绝录入
     * （{@code ErrorStyle.STOP}）；{@code false}：仅警告，允许保留。
     */
    boolean strict() default true;

    /** 下拉生效的首行（0 基，含）。默认 {@code 1} —— 第 0 行是表头。 */
    int firstRow() default 1;

    /** 下拉生效的末行（0 基，含）。默认 {@value #DEFAULT_LAST_ROW}。 */
    int lastRow() default DEFAULT_LAST_ROW;
}
