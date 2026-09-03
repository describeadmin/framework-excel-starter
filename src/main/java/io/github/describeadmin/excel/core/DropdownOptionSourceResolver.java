package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.DropdownOptionSource;

/**
 * 把 {@link io.github.describeadmin.excel.api.ExcelColumnDropdown#source()} 声明的类型解析成
 * {@link DropdownOptionSource} 实例。
 *
 * <p>装配时由 {@code FrameworkExcelAutoConfiguration} 注入一个「先查 Spring 容器、拿不到再无参
 * 实例化」的实现；纯门面消费方直接 {@code new FesodExcelExporter(properties)} 时为 {@code null}，
 * 此时 {@link FesodExcelExporter} 用 {@link #reflective()} 兜底。
 */
@FunctionalInterface
public interface DropdownOptionSourceResolver {

    /**
     * @return 该类型的实例；无法提供时返回 {@code null}（调用方会记 WARN 并跳过这一列的下拉）。
     */
    DropdownOptionSource resolve(Class<? extends DropdownOptionSource> type);

    /** 无 Spring 容器时的兜底：无参构造实例化，失败返回 {@code null}。 */
    static DropdownOptionSourceResolver reflective() {
        return type -> {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        };
    }
}
