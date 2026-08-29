package io.github.describeadmin.excel.api;

import org.apache.fesod.sheet.converters.Converter;

/**
 * Fesod 单元格转换器 SPI 的一层薄再导出，让业务代码依赖一个 describeadmin 拥有的类型，
 * 而不是直接 import {@code org.apache.fesod.sheet.converters.Converter}。
 *
 * <p>典型用途是字典 / 枚举 ⇄ 字面量转换。v1 <b>不内置</b> DB 版 {@code @ExcelDict}
 * （推迟到 v0.3.0，见 README「进阶」小节与 docs 仓 registry.md）；在那之前，业务方
 * 自己写一个实现类，在字段上用
 * {@code @ExcelProperty(converter = MyDictConverter.class)} 挂上即可：
 *
 * <pre>{@code
 * public class StatusConverter implements ExcelConverter<Integer> {
 *     private final SysDictDataService dict; // 业务方自己的 Bean
 *     public Class<Integer> supportJavaTypeKey() { return Integer.class; }
 *     public Integer convertToJavaData(ReadConverterContext<?> ctx) { ... }
 *     public WriteCellData<?> convertToExcelData(WriteConverterContext<Integer> ctx) { ... }
 * }
 * }</pre>
 *
 * @param <J> Java 侧的字段类型
 */
public interface ExcelConverter<J> extends Converter<J> {
}
