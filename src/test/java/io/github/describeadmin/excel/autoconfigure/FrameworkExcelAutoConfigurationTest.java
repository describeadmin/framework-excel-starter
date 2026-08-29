package io.github.describeadmin.excel.autoconfigure;

import io.github.describeadmin.common.api.FrameworkVersion;
import io.github.describeadmin.excel.api.ExcelExporter;
import io.github.describeadmin.excel.api.ExcelImporter;
import io.github.describeadmin.excel.core.ExcelResponseBodyAdvice;
import io.github.describeadmin.excel.core.FesodExcelExporter;
import io.github.describeadmin.excel.core.FesodExcelImporter;
import io.github.describeadmin.web.autoconfigure.FrameworkWebAutoConfiguration;
import io.github.describeadmin.web.core.FrameworkJsonModule;
import io.github.describeadmin.web.core.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** 插件装配机制的验证——"把这个 jar 放进 classpath，它到底有没有接管"。 */
@DisplayName("插件装配")
class FrameworkExcelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FrameworkExcelAutoConfiguration.class));

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FrameworkExcelAutoConfiguration.class));

    @Test
    @DisplayName("插件声明的最低框架版本，与它实际构建所依赖的框架是自洽的")
    void declaredRequirementIsSatisfiedByTheFrameworkItBuildsAgainst() {
        assertThat(FrameworkVersion.current()).isNotEqualTo(FrameworkVersion.UNKNOWN);
        assertThatNoException().isThrownBy(() -> FrameworkVersion.requireCompatible(
                "framework-excel-starter", FrameworkExcelAutoConfiguration.REQUIRED_FRAMEWORK_VERSION));
    }

    @Test
    @DisplayName("框架比插件要求的旧时，启动即失败而不是等到运行期报错")
    void incompatibleFrameworkFailsFast() {
        assertThatThrownBy(() -> FrameworkVersion.requireCompatible("framework-excel-starter", "99.0.0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("framework-excel-starter");
    }

    @Test
    @DisplayName("不引本插件时，不会注册任何相关 Bean（等同没引这个 jar）")
    void withoutPluginNoBeansRegistered() {
        new ApplicationContextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ExcelExporter.class);
            assertThat(context).doesNotHaveBean(ExcelImporter.class);
        });
    }

    @Test
    @DisplayName("describeadmin.excel.enabled=false 时完全不装配")
    void disabledFallsBackToAbsent() {
        runner.withPropertyValues("describeadmin.excel.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(FesodExcelExporter.class);
            assertThat(context).doesNotHaveBean(FesodExcelImporter.class);
            assertThat(context).doesNotHaveBean(ExcelResponseBodyAdvice.class);
        });
    }

    @Test
    @DisplayName("默认配置下，门面 Bean 被注册")
    void enabledRegistersFacadeBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(FesodExcelExporter.class);
            assertThat(context).hasSingleBean(FesodExcelImporter.class);
        });
    }

    @Test
    @DisplayName("业务方自己注册的 ExcelExporter / ExcelImporter 一定赢")
    void businessBeanWins() {
        ExcelExporter customExporter = mock(ExcelExporter.class);
        ExcelImporter customImporter = mock(ExcelImporter.class);
        runner.withBean("myExporter", ExcelExporter.class, () -> customExporter)
                .withBean("myImporter", ExcelImporter.class, () -> customImporter)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FesodExcelExporter.class);
                    assertThat(context).doesNotHaveBean(FesodExcelImporter.class);
                    assertThat(context.getBean(ExcelExporter.class)).isSameAs(customExporter);
                    assertThat(context.getBean(ExcelImporter.class)).isSameAs(customImporter);
                });
    }

    @Test
    @DisplayName("非 Web 上下文：门面在，但不注册 MVC advice/resolver")
    void webExtensionsAbsentInNonWebContext() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(FesodExcelExporter.class);
            assertThat(context).doesNotHaveBean(ExcelResponseBodyAdvice.class);
            assertThat(context).doesNotHaveBean(WebMvcConfigurer.class);
        });
    }

    @Test
    @DisplayName("Servlet Web 上下文：注册 @ExcelResponse advice 与 @ExcelBody resolver 的 WebMvcConfigurer")
    void webExtensionsRegisteredInServletContext() {
        webRunner.run(context -> {
            assertThat(context).hasSingleBean(ExcelResponseBodyAdvice.class);
            assertThat(context).hasBean("excelBodyWebMvcConfigurer");
        });
    }

    @Test
    @DisplayName("describeadmin.excel.export.* 与 describeadmin.excel.import.* 都能绑定")
    void exportAndImportPropertiesBind() {
        runner.withPropertyValues(
                "describeadmin.excel.export.max-rows=7",
                "describeadmin.excel.export.long-as-text=false",
                "describeadmin.excel.import.max-rows=9",
                "describeadmin.excel.import.fail-fast=true",
                "describeadmin.excel.import.head-row-number=2").run(context -> {
            FrameworkExcelProperties props = context.getBean(FrameworkExcelProperties.class);
            assertThat(props.getExport().getMaxRows()).isEqualTo(7);
            assertThat(props.getExport().isLongAsText()).isFalse();
            assertThat(props.getImport().getMaxRows()).isEqualTo(9);
            assertThat(props.getImport().isFailFast()).isTrue();
            assertThat(props.getImport().getHeadRowNumber()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("与 FrameworkWebAutoConfiguration 共存，互不顶替")
    void coexistsWithFrameworkWebAutoConfiguration() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        FrameworkWebAutoConfiguration.class, FrameworkExcelAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                    assertThat(context).hasSingleBean(FrameworkJsonModule.class);
                    assertThat(context).hasSingleBean(ExcelResponseBodyAdvice.class);
                });
    }
}
