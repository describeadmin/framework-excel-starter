package io.github.describeadmin.excel.autoconfigure;

import io.github.describeadmin.common.api.FrameworkVersion;
import io.github.describeadmin.excel.api.ExcelExporter;
import io.github.describeadmin.excel.api.ExcelImporter;
import io.github.describeadmin.excel.core.ExcelBodyArgumentResolver;
import io.github.describeadmin.excel.core.ExcelResponseBodyAdvice;
import io.github.describeadmin.excel.core.FesodExcelExporter;
import io.github.describeadmin.excel.core.FesodExcelImporter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * framework-excel-starter 的自动配置。
 *
 * <p><b>装配模型</b>：本插件<b>不替换</b>任何核心的单一默认实现，而是<b>新增</b>框架此前
 * 没有的 Web 扩展点（{@code ResponseBodyAdvice} + {@code HandlerMethodArgumentResolver}）。
 * framework-web-starter 里既没有 {@code ResponseBodyAdvice} 也没有
 * {@code HandlerMethodArgumentResolver}，因此不存在需要用 {@code before} 去抢的
 * {@code @ConditionalOnMissingBean} 时序竞争——照抄 cache-redis 的 {@code before} 写法在这里
 * 反而是"照抄单例覆盖模型"的错误（auth-email 的 javadoc 点过名）。
 *
 * <p>{@code afterName = "...FrameworkWebAutoConfiguration"} 是文档性 + 安全性的：消费方若确实
 * 引了 framework-web-starter，让它的 {@code GlobalExceptionHandler} /
 * {@code FrameworkJsonModule} / {@code TraceIdFilter} 先定义。用<b>字符串</b>形式是因为
 * framework-web-starter 对本插件是 {@code provided} + {@code optional}，{@code .class}
 * 引用在纯门面消费方那里会 {@code NoClassDefFoundError}。
 *
 * <p>每个 {@code @Bean} 都带 {@code @ConditionalOnMissingBean} 且<b>绑定到具体实现类</b>
 * （{@code FesodExcelExporter} / {@code FesodExcelImporter} / {@code ExcelResponseBodyAdvice}），
 * 业务方自己注册的同类型 Bean 一定赢（registry.md 准入规范第 7 条）。
 */
@AutoConfiguration(afterName = "io.github.describeadmin.web.autoconfigure.FrameworkWebAutoConfiguration")
@ConditionalOnProperty(prefix = "describeadmin.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FrameworkExcelProperties.class)
public class FrameworkExcelAutoConfiguration {

    /** 本插件要求的最低框架版本：依赖 framework-web-starter 0.2.0 的 MVC 装配点与 GlobalExceptionHandler 契约。 */
    public static final String REQUIRED_FRAMEWORK_VERSION = "0.2.0";

    public FrameworkExcelAutoConfiguration() {
        // 放在构造函数里：条件都满足、真的要装配本插件时才检查，插件未激活时不因版本不匹配把应用打死。
        FrameworkVersion.requireCompatible("framework-excel-starter", REQUIRED_FRAMEWORK_VERSION);
    }

    // @ConditionalOnMissingBean 绑定到【接口】而不是具体类：ExcelExporter / ExcelImporter 是
    // 单一门面（不是 CryptoProvider 那种多实现共存模型），业务方注册自己的 ExcelExporter 就应当
    // 整体顶替插件的实现（registry.md 准入规范第 7 条）。
    @Bean
    @ConditionalOnMissingBean(ExcelExporter.class)
    public FesodExcelExporter excelExporter(FrameworkExcelProperties properties) {
        return new FesodExcelExporter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ExcelImporter.class)
    public FesodExcelImporter excelImporter(FrameworkExcelProperties properties) {
        return new FesodExcelImporter(properties);
    }

    /**
     * MVC 糖：{@code @ExcelResponse} advice + {@code @ExcelBody} resolver。
     *
     * <p>嵌套配置类 + 类级 {@code @ConditionalOnClass}：Spring 用 ASM 读注解，不会因为纯门面
     * （无 Spring MVC）的消费方而提前 link-load {@code ResponseBodyAdvice}
     * （registry.md 准入规范第 3 条的写法，同 crypto 的 {@code Sm4Configuration}）。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = {
            "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice",
            "org.springframework.web.servlet.config.annotation.WebMvcConfigurer"})
    static class ExcelWebMvcConfiguration {

        @Bean
        @ConditionalOnMissingBean(ExcelResponseBodyAdvice.class)
        public ExcelResponseBodyAdvice excelResponseBodyAdvice(ExcelExporter exporter) {
            return new ExcelResponseBodyAdvice(exporter);
        }

        @Bean
        @ConditionalOnMissingBean(name = "excelBodyWebMvcConfigurer")
        public WebMvcConfigurer excelBodyWebMvcConfigurer(ExcelImporter importer) {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(new ExcelBodyArgumentResolver(importer));
                }
            };
        }
    }

    /**
     * 字典 / 枚举 ⇄ 字面量转换的装配位——<b>v0.3.0 的空桩</b>。
     *
     * <p>v1 只在 {@code api/} 重导出 Fesod 的 {@code Converter} SPI（{@code ExcelConverter}），
     * 业务方用 {@code @ExcelProperty(converter = X.class)} 自解；DB 版 {@code @ExcelDict}
     * 推迟到 v0.3.0，届时在此处按 {@code @ConditionalOnClass} 字符串形式挂
     * {@code framework-system-starter} 的 {@code SysDictDataService}。现在只把接缝留出来。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.github.describeadmin.system.service.SysDictDataService")
    static class DictConverterConfiguration {
        // TODO(v0.3.0): DB-backed @ExcelDict("dict_type") Converter，字符串形式 @ConditionalOnClass 守卫。
    }
}
