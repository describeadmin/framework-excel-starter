package io.github.describeadmin.excel.api;

import java.util.List;

/**
 * 一次导入的结果：解析成功的行 + 逐行错误清单。
 *
 * <p>框架此前没有结构化的"逐行错误"形状（{@code GlobalExceptionHandler} 只会把校验消息
 * 拼成一个字符串塞进 {@code Result.message}），所以这两个类型是新增的，位于 {@code api/}
 * 兼容面。推荐业务方的导入端点返回 {@code Result<ExcelImportResult<XxxDto>>}——本类是
 * record，Jackson 会按分量序列化，前端能拿到 {@code rows} / {@code errors} / {@code totalDataRows}。
 *
 * @param <T>           行模型类型
 * @param rows          解析并（可选）裁剪成功的行；不可变
 * @param errors        逐行错误清单；不可变
 * @param totalDataRows 实际读取的数据行数（不含表头，含被 {@code errors} 拒掉的行）
 */
public record ExcelImportResult<T>(List<T> rows, List<RowError> errors, int totalDataRows) {

    public ExcelImportResult {
        rows = List.copyOf(rows);
        errors = List.copyOf(errors);
    }

    public static <T> ExcelImportResult<T> of(List<T> rows, List<RowError> errors, int totalDataRows) {
        return new ExcelImportResult<>(rows, errors, totalDataRows);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** 既没有通过的行、也没有错误——通常意味着传了个只有表头的空文件。 */
    public boolean isEmpty() {
        return rows.isEmpty() && errors.isEmpty();
    }
}
