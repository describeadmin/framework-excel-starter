package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.DropdownOptionSource;
import io.github.describeadmin.excel.api.ExcelColumnDropdown;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 把「行模型上的 {@link ExcelColumnDropdown} 注解」+「{@code ExcelWriteOptions.dropdowns()}
 * 编程式覆盖」合成一份最终的下拉配置，交给 {@link DropdownSheetWriteHandler}。
 *
 * <p>列位置不在这里定——留到 {@code afterSheetCreate} 时按 Fesod 的表头映射用字段名反查，
 * 这样 {@code @ExcelProperty(index=...)} / {@code includeFields} / {@code excludeFields} 都能对齐。
 */
final class DropdownConfig {

    private static final Logger log = LoggerFactory.getLogger(DropdownConfig.class);

    private DropdownConfig() {
    }

    /**
     * @param overrides {@code ExcelWriteOptions.dropdowns()}，可为 {@code null}
     * @param resolver  解析 {@code source()} 声明的类型；不会为 {@code null}（门面侧兜底为反射实例化）
     * @return 每个元素对应一列下拉；已剔除选项为空的列。顺序：注解字段按声明序，其后是仅出现在
     *         {@code overrides} 里的字段
     */
    static List<DropdownSheetWriteHandler.FieldDropdown> resolve(
            Class<?> model, Map<String, List<String>> overrides, DropdownOptionSourceResolver resolver) {

        Map<String, DropdownSheetWriteHandler.FieldDropdown> byField = new LinkedHashMap<>();

        for (Class<?> c = model; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                ExcelColumnDropdown a = f.getAnnotation(ExcelColumnDropdown.class);
                if (a == null || byField.containsKey(f.getName())) {
                    continue; // 子类同名字段先入先得，父类的忽略
                }
                byField.put(f.getName(), new DropdownSheetWriteHandler.FieldDropdown(
                        f.getName(), resolveOptions(a, resolver, f.getName()),
                        a.strict(), a.firstRow(), a.lastRow()));
            }
        }

        if (overrides != null) {
            overrides.forEach((field, options) -> {
                DropdownSheetWriteHandler.FieldDropdown base = byField.get(field);
                byField.put(field, base != null
                        ? new DropdownSheetWriteHandler.FieldDropdown(
                                field, options, base.strict(), base.firstRow(), base.lastRow())
                        : new DropdownSheetWriteHandler.FieldDropdown(
                                field, options, true, 1, ExcelColumnDropdown.DEFAULT_LAST_ROW));
            });
        }

        return byField.values().stream()
                .filter(d -> d.options() != null && !d.options().isEmpty())
                .toList();
    }

    private static List<String> resolveOptions(
            ExcelColumnDropdown a, DropdownOptionSourceResolver resolver, String field) {

        Class<? extends DropdownOptionSource> source = a.source();
        if (source == null || source == DropdownOptionSource.None.class) {
            return Arrays.asList(a.value());
        }
        DropdownOptionSource instance = resolver == null ? null : resolver.resolve(source);
        if (instance == null) {
            log.warn("字段 {} 的 @ExcelColumnDropdown(source = {}) 无法解析为实例，该列不加下拉",
                    field, source.getName());
            return List.of();
        }
        List<String> options = instance.options();
        return options == null ? List.of() : options;
    }
}
