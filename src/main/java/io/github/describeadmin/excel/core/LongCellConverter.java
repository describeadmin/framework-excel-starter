package io.github.describeadmin.excel.core;

import io.github.describeadmin.excel.api.ExcelLongNumber;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.converters.ReadConverterContext;
import org.apache.fesod.sheet.converters.WriteConverterContext;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * {@code Long} / {@code long} 单元格的读写转换器。
 *
 * <p><b>写</b>：默认写成文本单元格（雪花 ID 19 位，超出 Excel 数值精度，见 CLAUDE.md 4.8）。
 * 字段上标了 {@link ExcelLongNumber} 则强制数值单元格；否则跟随构造参数 {@code defaultAsText}
 * （由每次导出时的 {@code ExcelWriteOptions.longAsText()} + 全局配置解析而来）。
 *
 * <p><b>读</b>：无论单元格是文本还是数值，一律解析回 {@code Long}。空 → {@code null}；
 * 非数字 → 抛异常，Fesod 会包装成 {@code ExcelDataConvertException} 交给
 * {@link CollectingReadListener#onException}，最终成为一条 {@code RowError}。
 *
 * <p>为 {@code Long.class} 与 {@code long.class} 各注册一个实例——Fesod 按字段的运行时
 * 类型键查找转换器，两者分开（同 {@code FrameworkJsonModule} 对 {@code Long.class} /
 * {@code Long.TYPE} 双注册的处理）。
 */
final class LongCellConverter implements Converter<Long> {

    private final Class<?> javaTypeKey;
    private final boolean defaultAsText;

    private LongCellConverter(Class<?> javaTypeKey, boolean defaultAsText) {
        this.javaTypeKey = javaTypeKey;
        this.defaultAsText = defaultAsText;
    }

    static LongCellConverter forWrapper(boolean defaultAsText) {
        return new LongCellConverter(Long.class, defaultAsText);
    }

    static LongCellConverter forPrimitive(boolean defaultAsText) {
        return new LongCellConverter(long.class, defaultAsText);
    }

    @Override
    public Class<?> supportJavaTypeKey() {
        return javaTypeKey;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        // null = 适配所有 Excel 单元格类型，读取时不挑类型
        return null;
    }

    @Override
    public Long convertToJavaData(ReadConverterContext<?> context) {
        ReadCellData<?> cell = context.getReadCellData();
        CellDataTypeEnum type = cell.getType();
        if (type == CellDataTypeEnum.EMPTY) {
            return null;
        }
        if (type == CellDataTypeEnum.NUMBER) {
            BigDecimal n = cell.getNumberValue();
            if (n == null) {
                return null;
            }
            return n.toBigIntegerExact().longValueExact();
        }
        String s = cell.getStringValue();
        if (s == null || s.strip().isEmpty()) {
            return null;
        }
        return Long.parseLong(s.strip());
    }

    @Override
    public WriteCellData<?> convertToExcelData(WriteConverterContext<Long> context) {
        Long value = context.getValue();
        if (value == null) {
            return new WriteCellData<>(CellDataTypeEnum.EMPTY);
        }
        return asNumber(context.getContentProperty())
                ? new WriteCellData<>(new BigDecimal(value))
                : new WriteCellData<>(value.toString());
    }

    private boolean asNumber(ExcelContentProperty property) {
        Field field = property == null ? null : property.getField();
        if (field != null && field.isAnnotationPresent(ExcelLongNumber.class)) {
            return true;
        }
        return !defaultAsText;
    }
}
