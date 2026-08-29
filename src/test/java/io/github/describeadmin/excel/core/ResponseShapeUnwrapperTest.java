package io.github.describeadmin.excel.core;

import io.github.describeadmin.common.api.PageResult;
import io.github.describeadmin.common.api.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("@ExcelResponse 返回形状解包")
class ResponseShapeUnwrapperTest {

    private static final DemoRow ROW = new DemoRow(1L, "张三", "研发", null, true);

    @Test
    @DisplayName("List<T> 原样返回")
    void unwrapList() {
        assertThat(ResponseShapeUnwrapper.unwrap(List.of(ROW))).singleElement().isEqualTo(ROW);
    }

    @Test
    @DisplayName("PageResult<T> 取 records")
    void unwrapPageResult() {
        assertThat(ResponseShapeUnwrapper.unwrap(new PageResult<>(List.of(ROW), 1, 1, 10)))
                .singleElement().isEqualTo(ROW);
    }

    @Test
    @DisplayName("Result<List<T>> 取 data")
    void unwrapResultOfList() {
        assertThat(ResponseShapeUnwrapper.unwrap(Result.ok(List.of(ROW)))).singleElement().isEqualTo(ROW);
    }

    @Test
    @DisplayName("Result<PageResult<T>> 取 data.records")
    void unwrapResultOfPageResult() {
        assertThat(ResponseShapeUnwrapper.unwrap(Result.ok(new PageResult<>(List.of(ROW), 1, 1, 10))))
                .singleElement().isEqualTo(ROW);
    }

    @Test
    @DisplayName("不支持的形状抛 IllegalStateException，消息里带类名")
    void unwrapUnsupportedThrows() {
        assertThatThrownBy(() -> ResponseShapeUnwrapper.unwrap("just a string"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("String");
        assertThatThrownBy(() -> ResponseShapeUnwrapper.unwrap(Result.ok("x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("String");
    }

    @Test
    @DisplayName("从返回类型泛型推断行类型")
    void elementTypeFromGenerics() throws Exception {
        assertThat(ResponseShapeUnwrapper.elementType(returnOf("listOfRows"))).isEqualTo(DemoRow.class);
        assertThat(ResponseShapeUnwrapper.elementType(returnOf("resultOfPage"))).isEqualTo(DemoRow.class);
        assertThat(ResponseShapeUnwrapper.elementType(returnOf("rawList"))).isNull();
    }

    private static MethodParameter returnOf(String method) throws NoSuchMethodException {
        return new MethodParameter(Sample.class.getDeclaredMethod(method), -1);
    }

    @SuppressWarnings("unused")
    static class Sample {
        List<DemoRow> listOfRows() {
            return List.of();
        }

        Result<PageResult<DemoRow>> resultOfPage() {
            return Result.ok(PageResult.empty(1, 10));
        }

        @SuppressWarnings("rawtypes")
        List rawList() {
            return List.of();
        }
    }
}
