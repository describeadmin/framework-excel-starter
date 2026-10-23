# framework-excel-starter

describeadmin 的可选插件。Excel 导入导出：`ExcelExporter` / `ExcelImporter` 门面，加上 `@ExcelResponse` / `@ExcelBody` 两个注解，基于 Apache Fesod。

**文档：[https://describeadmin.github.io/docs/how-to/excel](https://describeadmin.github.io/docs/how-to/excel)**，全部配置项见 [插件配置](https://describeadmin.github.io/docs/reference/configuration/plugins)。

## 引入

```xml
<dependency>
  <groupId>io.github.describeadmin</groupId>
  <artifactId>framework-excel-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

版本号要显式写：`framework-bom` 不管插件的版本，插件有自己的版本线。

不需要配置，引入即可用。

> ⚠️ Apache Fesod 仍在 ASF 孵化，其 POM 自述「IP Clearance and license header updates」尚未完成。对许可合规有要求的项目，引入前自行做一次许可审查。

运行时开关是 `describeadmin.excel.enabled`（默认 `true`），关掉后与没有引入这个 jar 完全一样。

## 兼容性

| 插件版本 | 最低框架版本 |
|---|---|
| 1.0.0 | 1.0.0 |

插件启动时检查框架版本：框架比上表要求的旧、或主版本不同（0.x 与 1.x 不互通），应用直接拒绝启动并说明原因。
CI 对表里的每个框架版本各跑一遍完整测试。版本对应关系的总表见 [插件目录](https://describeadmin.github.io/docs/reference/plugins)。

## 开发

```bash
mvn clean test                                # 不需要 Docker 或中间件
mvn clean test -Ddescribeadmin.version=1.0.0
```

构建只要求 JDK ≥ 17（enforcer 把关，产物固定为 Java 17 字节码）；用 `mvn -v` 看 Maven 实际用的 JDK。
框架有尚未发布到 Central 的版本时，先 `mvn -f ../framework/pom.xml clean install -DskipTests -Dgpg.skip=true` 装到本地仓库。

本插件不继承 `framework-parent`，而是 import `framework-bom`，与业务方的用法相同。插件开发规范见
[编写插件](https://describeadmin.github.io/docs/dev/plugin-dev)，发布照 [发布手册](https://describeadmin.github.io/docs/dev/release) 执行。

## 维护须知

### Fesod 2.x 类名（用 `unzip` 核实过，不要凭记忆写）

| 用途 | FQN |
|---|---|
| 写 / 读入口 | `org.apache.fesod.sheet.FesodSheet`（`EasyExcel` / `FastExcel` 为别名） |
| 列注解 | `org.apache.fesod.sheet.annotation.ExcelProperty` / `ExcelIgnore` |
| 转换器 SPI | `org.apache.fesod.sheet.converters.Converter<T>`（`convertToJavaData(ReadConverterContext)` / `convertToExcelData(WriteConverterContext)` 均为 default 方法） |
| 行监听 | `org.apache.fesod.sheet.read.listener.ReadListener<T>`（`invoke` / `onException` / `doAfterAllAnalysed` / `hasNext`） |
| 写字段可见性 | `WriteConverterContext.getContentProperty().getField()` 可拿到 `java.lang.reflect.Field`（`@ExcelLongNumber` 据此判断） |
| 坏单元格异常 | `org.apache.fesod.sheet.exception.ExcelDataConvertException`（带 `getRowIndex` / `getColumnIndex`） |

Fesod / POI 遇到无法识别的字节时不抛异常，而是当成 CSV 静默解析出 0 行。`FesodExcelImporter` 因此在交给 Fesod 之前先校验文件头
（xlsx 是 ZIP 的 `PK..`，xls 是 OLE2）。

POI 用 `log4j-api` 作日志门面，由 Spring Boot 的 `log4j-to-slf4j` 桥路由，不需要额外配置。

## 变更记录与许可证

[CHANGELOG.md](./CHANGELOG.md) · [Apache-2.0](./LICENSE)
