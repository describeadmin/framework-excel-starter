package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.RowError;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.exception.ExcelDataConvertException;
import org.apache.fesod.sheet.metadata.data.CellData;
import org.apache.fesod.sheet.read.listener.ReadListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 逐行收集器：解析并（可选）裁剪成功的行进 {@link #rows}，单元格解析失败进 {@link #errors}，
 * 结构性异常向上抛。每次导入 new 一个，不复用。
 *
 * @param <T> 行模型类型
 */
final class CollectingReadListener<T> implements ReadListener<T> {

    private final List<T> rows = new ArrayList<>();
    private final List<RowError> errors = new ArrayList<>();
    private final boolean trim;
    private final boolean failFast;
    private final int maxRows;

    private int dataRowCount = 0;
    private boolean stopped = false;

    CollectingReadListener(boolean trim, boolean failFast, int maxRows) {
        this.trim = trim;
        this.failFast = failFast;
        this.maxRows = maxRows;
    }

    List<T> rows() {
        return rows;
    }

    List<RowError> errors() {
        return errors;
    }

    int totalDataRows() {
        return dataRowCount;
    }

    @Override
    public void invoke(T row, AnalysisContext context) {
        dataRowCount++;
        if (maxRows > 0 && rows.size() >= maxRows) {
            errors.add(RowError.wholeRow(spreadsheetRow(context),
                    "超过导入行数上限 " + maxRows + " 行，其余行未读取"));
            stopped = true;
            return;
        }
        if (trim) {
            trimStringFields(row);
        }
        rows.add(row);
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        if (exception instanceof ExcelDataConvertException e) {
            dataRowCount++;
            Field field = e.getExcelContentProperty() == null ? null : e.getExcelContentProperty().getField();
            int row = e.getRowIndex() == null ? spreadsheetRow(context) : e.getRowIndex() + 1;
            errors.add(new RowError(
                    row,
                    field == null ? null : field.getName(),
                    columnOf(field),
                    "单元格解析失败: " + rootMessage(e),
                    e.getCellData() == null ? null : cellString(e.getCellData())));
            if (failFast) {
                stopped = true;
            }
            return;
        }
        // 结构性问题（损坏、加密、非 xlsx…）向上抛，由 FesodExcelImporter 转成 ExcelParseException
        throw exception;
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return !stopped;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 无需额外收尾——rows / errors / dataRowCount 均已在逐行回调里维护
    }

    private int spreadsheetRow(AnalysisContext context) {
        try {
            Integer idx = context.readRowHolder().getRowIndex();
            if (idx != null) {
                return idx + 1;
            }
        } catch (RuntimeException ignored) {
            // 取不到就退化成基于已处理行数的估算
        }
        return dataRowCount + 1;
    }

    private void trimStringFields(T row) {
        if (row == null) {
            return;
        }
        for (Field f : row.getClass().getDeclaredFields()) {
            if (f.getType() != String.class) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object v = f.get(row);
                if (v instanceof String s) {
                    String stripped = s.strip();
                    if (!stripped.equals(s)) {
                        f.set(row, stripped);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // 裁剪失败不影响该行本身的可用性
            }
        }
    }

    private static String columnOf(Field field) {
        if (field == null) {
            return null;
        }
        ExcelProperty ann = field.getAnnotation(ExcelProperty.class);
        if (ann != null && ann.value().length > 0) {
            String last = ann.value()[ann.value().length - 1];
            if (last != null && !last.isEmpty()) {
                return last;
            }
        }
        return field.getName();
    }

    private static String cellString(CellData<?> cell) {
        if (cell.getStringValue() != null) {
            return cell.getStringValue();
        }
        if (cell.getNumberValue() != null) {
            return cell.getNumberValue().toPlainString();
        }
        Object data = cell.getData();
        return data == null ? null : String.valueOf(data);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage() == null ? cur.getClass().getSimpleName() : cur.getMessage();
    }
}
