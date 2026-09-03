package io.github.describeadmin.excel.api;

import java.util.List;

/**
 * {@link ExcelColumnDropdown#source()} 用的候选值提供者：导出时调用一次 {@link #options()}
 * 拿到某一列的下拉选项（字典、枚举、DB 查询等运行期才知道的来源）。
 *
 * <p><b>解析方式</b>：导出器优先把声明的类型当 Spring Bean 按类型取（因此实现类里可以注入
 * {@code SysDictDataService} / Mapper 之类）；只有在拿不到 Spring 容器时（纯门面、无 MVC 的
 * 消费方直接 {@code new FesodExcelExporter(properties)}）才退化成无参构造实例化。两条路都
 * 失败时记一条 WARN 并跳过该列的下拉——导出本身不受影响。
 *
 * <p>实现应当是无副作用的、可重复调用的；返回 {@code null} 或空集合等同于"这一列不加下拉"。
 */
@FunctionalInterface
public interface DropdownOptionSource {

    /** 这一列的下拉候选值。{@code null} 或空集合表示不加下拉。 */
    List<String> options();

    /**
     * {@link ExcelColumnDropdown#source()} 的默认占位，表示"未配置动态来源"。
     * 导出器识别到这个类型会直接跳过，转而用 {@link ExcelColumnDropdown#value()}。
     */
    enum None implements DropdownOptionSource {
        INSTANCE;

        @Override
        public List<String> options() {
            return List.of();
        }
    }
}
