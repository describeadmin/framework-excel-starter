package io.github.describeadmin.excel.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单字段逃生舱：让这个 {@code Long} / {@code long} 字段在导出时写成<b>数值</b>单元格，
 * 而不是插件默认的文本单元格。
 *
 * <p>语义等同于框架 JSON 约定里的 {@code @JsonFormat(shape = JsonFormat.Shape.NUMBER)}
 * （见 CLAUDE.md 4.8）。只在确定该字段的取值不会超过 2^53（如分页页码、数量、年份）时使用；
 * 雪花主键一律保持默认的文本形态。
 *
 * <p>优先级：本注解 &gt; {@code @ExcelResponse(longAsText = ...)} &gt; 全局
 * {@code describeadmin.excel.export.long-as-text}。
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelLongNumber {
}
