# framework-excel-starter

describeadmin 的**可选插件**：Excel 导入 / 导出——门面 `ExcelExporter` / `ExcelImporter`
（零 MVC 依赖也能用）+ `@ExcelResponse` / `@ExcelBody` 两个注解糖。基于
**Apache Fesod（孵化中）**——Alibaba EasyExcel / FastExcel 的 ASF 捐赠版。

不引它，框架没有 Excel 能力，行为与从前完全一致（POI 是重依赖，按 `CLAUDE.md` 4.6
必须做插件）。引了它：

| 能力 | 说明 |
|---|---|
| 门面 `ExcelExporter` / `ExcelImporter` | `api/` 包，不依赖 Spring MVC，任意代码里注入即用 |
| `@ExcelResponse`（方法级） | 把 `List<T>` / `PageResult<T>` / `Result<…>` 直接当 xlsx 下载流写出 |
| `@ExcelBody`（参数级） | 把上传文件绑成 `List<T>` 或 `ExcelImportResult<T>`（结构化逐行错误 `RowError`） |

**不改 `BaseController`、不新增端点、不新增权限动作。** Excel 导入导出是少数业务的需求——
业务方在自己的 Controller 里写 `@GetMapping("/export")` / `@PostMapping("/import")`，
并用 Spring Security 原生 `@PreAuthorize` 自行控权（`CLAUDE.md` 4.5 的 list/add/edit/remove
四动作不变）。

***

## 兼容性

| 插件版本 | 最低框架版本 | 说明 |
|---|---|---|
| 0.2.0 | **0.2.0** | 依赖 framework-web-starter 0.2.0 的 MVC 装配点与 `GlobalExceptionHandler` 契约；门面部分只依赖 framework-common |

这张表不是文档承诺，是**可执行的**：

- 插件在启动时自检框架版本（`FrameworkVersion.requireCompatible`），装到更旧的框架上会
  **启动失败**并给出一句能照着做的提示，而不是等到某个请求走到那行代码才
  `NoSuchMethodError`
- CI 对表中每个框架版本各跑一遍完整测试（见 `.github/workflows/ci.yml`）

> ⚠️ **0.x 期间请逐版本核对**。SemVer 对 `0.x` 不作任何保证，本项目的 0.2.0 就带过
> Breaking Change。框架比插件构建时更新且主版本相同时，插件只记一条 WARN 而不阻断——
> 那条 WARN 要当回事。

> ⚠️ **Apache Fesod 仍在 ASF 孵化**。其 POM 自述"IP Clearance and license header updates
> for legacy code derived from Alibaba EasyExcel are currently in progress"，并提示下游
> "need to conduct a thorough licensing review"。把本插件用于对外发布的产品前，请自行完成
> 许可审查。当前依赖 `org.apache.fesod:fesod-sheet:2.0.2-incubating`。

字典 / 枚举 ⇄ 字面量转换 v1 **未内置**（推迟到 v0.3.0，见下方「进阶」）。

***

## 接入

```xml
<dependency>
  <groupId>io.github.describeadmin</groupId>
  <artifactId>framework-excel-starter</artifactId>
  <version>0.2.0</version>
</dependency>
```

版本号需要显式写。**框架的 `framework-bom` 不仲裁插件版本**——插件有自己的版本线，
让 BOM 按框架版本去解析插件，会解析到一个根本不存在的制品。

上传文件大小默认交给 Spring Boot 的 `spring.servlet.multipart.max-file-size` 管，
不另设一套：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

describeadmin:
  excel:
    enabled: true              # 运行时开关，关掉后完全等同于没引这个 jar（advice/resolver 都不注册）
    export:
      default-sheet-name: Sheet1
      long-as-text: true       # Long 默认写成文本单元格，见下「雪花 Long」
      max-rows: 100000         # 单次导出行数上限；超过时写第一个字节之前抛 BizException(BAD_REQUEST)。0 = 不限
      sheet-write-window: 0    # 流式写出的内存行窗口；0 = Fesod 默认
    import:
      max-rows: 50000          # 数据行数上限；0 = 不限
      max-file-size:           # 不设 = 交给 spring.servlet.multipart.max-file-size
      trim-strings: true       # 裁剪字符串字段两端空白
      fail-fast: false         # false = 收集全部 RowError；true = 遇到第一处错误就停
      head-row-number: 1       # 表头所在行；数据从 head-row-number + 1 开始
```

### 配置项（前缀 `describeadmin.excel`）

| 键 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 顶层开关；`false` = 等同没引 jar |
| `export.default-sheet-name` | `Sheet1` | 未指定工作表名时用它 |
| `export.long-as-text` | `true` | `Long` / `long` 字段默认写成文本单元格 |
| `export.max-rows` | `100000` | 单次导出行数上限；`0` = 不限 |
| `export.sheet-write-window` | `0` | 流式写出的内存行窗口；`0` = Fesod 默认 |
| `import.max-rows` | `50000` | 数据行数上限；`0` = 不限 |
| `import.max-file-size` | *（不设）* | 不设时完全交给 `spring.servlet.multipart.max-file-size` |
| `import.trim-strings` | `true` | 裁剪字符串字段两端空白 |
| `import.fail-fast` | `false` | `false` = 收集全部 `RowError`；`true` = 遇到第一处错误就停 |
| `import.head-row-number` | `1` | 表头所在行数（1 基） |

***

## 用法

### 1. 门面（无 MVC 依赖）

```java
@Service
class EmployeeExportService {
    private final ExcelExporter exporter;   // 由本插件注册

    void exportTo(HttpServletResponse response, List<EmployeeRow> rows) {
        exporter.writeToResponse(response, rows, EmployeeRow.class,
                ExcelWriteOptions.builder().fileName("员工.xlsx").build());
    }
}
```

`ExcelImporter.read(MultipartFile, Class<T>)` 返回 `ExcelImportResult<T>`：
`rows()` 是解析并（可选）裁剪成功的行，`errors()` 是逐行错误（行号 / 字段 / 列名 /
消息 / 拒绝值）。**门面永远不因单行问题抛异常**；只有整个流不是可读工作簿时才抛
`ExcelParseException`。

### 2. `@ExcelResponse`（方法级）

VO 字段标 `org.apache.fesod.sheet.annotation.ExcelProperty("列名")`，Controller 方法标
`@ExcelResponse`：

```java
@RestController
@RequestMapping("/api/employee")
class EmployeeExcelController {

    @PreAuthorize("hasAuthority('employee:list')")   // 权限由业务方自己声明
    @GetMapping("/export")
    @ExcelResponse(fileName = "员工.xlsx")
    public List<EmployeeRow> export() {
        return service.list().stream().map(EmployeeRow::from).toList();
    }
}
```

支持的返回形状（其它形状写出时抛 `IllegalStateException`）：

| 返回类型 | 取出的行 |
|---|---|
| `List<T>` | 自身 |
| `PageResult<T>` | `getRecords()` |
| `Result<List<T>>` | `getData()` |
| `Result<PageResult<T>>` | `getData().getRecords()` |

`supports()` 只做一次注解查找，不干扰任何未标注的端点——它们照常按 JSON 返回。

### 3. `@ExcelBody`（参数级）

```java
@PreAuthorize("hasAuthority('employee:add')")
@PostMapping("/import")
public Result<ExcelImportResult<EmployeeRow>> importRows(
        @ExcelBody ExcelImportResult<EmployeeRow> parsed) {
    parsed.rows().forEach(r -> service.save(r.toEntity()));
    return Result.ok(parsed);   // 前端能拿到 rows / errors / totalDataRows
}
```

参数类型可以是 `List<T>`（只要通过的行；`@ExcelBody(failFast = true)` 时首个错误抛
`BizException`）或 `ExcelImportResult<T>`（同时拿到通过的行与逐行错误）。

非 multipart 请求、找不到文件、文件不是可读的 Excel，一律抛
`BizException(ResultCode.BAD_REQUEST, ...)`，由框架 `GlobalExceptionHandler` 渲染成
`Result`（HTTP 200 + body `code=40000`），**不是 500**。

### 4. 权限

本插件**不新增权限动作**。业务方在自己的导出 / 导入端点上用 Spring Security 原生
`@PreAuthorize("hasAuthority('<模块>:<对象>:<动作>')")`，动作沿用 `CLAUDE.md` 4.5 的
`list` / `add` / `edit` / `remove`（导出通常复用 `list`，导入复用 `add` 或 `edit`）。

***

## 雪花 `Long` 为什么默认写成文本

雪花 ID 是 19 位，超过 Excel 数值单元格约 15 位有效数字的精度（与 JavaScript
`Number.MAX_SAFE_INTEGER` 同类问题，见 `CLAUDE.md` 4.8）。写成数值单元格会丢末几位。
所以 `Long` / `long` 字段默认写成**文本**单元格；导入时文本一律解析回 `Long`
（空 → `null`，非数字 → 一条 `RowError`）。

三个逃生舱（优先级从高到低）：

| 手段 | 作用范围 |
|---|---|
| `@io.github.describeadmin.excel.api.ExcelLongNumber` | 单字段——语义等同 `@JsonFormat(shape = NUMBER)` |
| `@ExcelResponse(longAsText = LongCellMode.NUMBER)` | 单个导出端点 |
| `describeadmin.excel.export.long-as-text: false` | 全局 |

只在确定字段取值不会超过 2^53（页码、数量、年份）时用逃生舱；雪花主键保持默认。

***

## 进阶：字典 / 枚举 ⇄ 字面量

v1 **不内置** DB 版 `@ExcelDict`（推迟到 v0.3.0）。在那之前，`api/ExcelConverter<J>` 是
Fesod `Converter` SPI 的薄再导出——业务方写一个实现类，在字段上挂
`@ExcelProperty(converter = X.class)` 即可：

```java
public class StatusConverter implements ExcelConverter<Integer> {
    private final SysDictDataService dict;   // 业务方自己的 Bean（framework-system-starter）

    @Override public Class<Integer> supportJavaTypeKey() { return Integer.class; }

    @Override public Integer convertToJavaData(ReadConverterContext<?> ctx) {
        String label = ctx.getReadCellData().getStringValue();
        return dict.listByType("sys_status").stream()
                .filter(d -> d.getDictLabel().equals(label))
                .map(d -> Integer.valueOf(d.getDictValue())).findFirst().orElse(null);
    }

    @Override public WriteCellData<?> convertToExcelData(WriteConverterContext<Integer> ctx) {
        // code -> label ...
    }
}
```

***

## 开发 / 测试

```bash
# 框架尚未发布 0.2.0 时，先从框架仓装一份到本地仓库
mvn -f ../framework/pom.xml clean install -DskipTests -Dgpg.skip=true

mvn clean test                                # 无需 Docker / 中间件
mvn test -Ddescribeadmin.version=0.3.0        # 针对另一个框架版本跑，即 CI 矩阵做的事
```

构建**不要求特定 JDK 版本**，唯一前提是 **JDK >= 17**（由 enforcer 的 `requireJavaVersion`
把关）。用 `mvn -v` 看 Maven 实际使用的 JDK，**不要用 `java -version`**。

### Fesod 2.x 类名（已用 `unzip` 核实，勿凭记忆）

| 用途 | FQN |
|---|---|
| 写 / 读入口 | `org.apache.fesod.sheet.FesodSheet`（`EasyExcel` / `FastExcel` 为别名） |
| 列注解 | `org.apache.fesod.sheet.annotation.ExcelProperty` / `ExcelIgnore` |
| 转换器 SPI | `org.apache.fesod.sheet.converters.Converter<T>`（`convertToJavaData(ReadConverterContext)` / `convertToExcelData(WriteConverterContext)` 均为 default 方法） |
| 行监听 | `org.apache.fesod.sheet.read.listener.ReadListener<T>`（`invoke` / `onException` / `doAfterAllAnalysed` / `hasNext`） |
| 写字段可见性 | `WriteConverterContext.getContentProperty().getField()` 可拿到 `java.lang.reflect.Field`（`@ExcelLongNumber` 逃生舱据此判断） |
| 坏单元格异常 | `org.apache.fesod.sheet.exception.ExcelDataConvertException`（带 `getRowIndex` / `getColumnIndex`） |

> Fesod / POI 遇到无法识别的字节时不抛异常，而是当成 CSV 静默解析出 0 行。
> `FesodExcelImporter` 因此在交给 Fesod 之前先校验文件头（xlsx = ZIP `PK..`，xls = OLE2）。

POI 用 `log4j-api` 作日志门面，由 Spring Boot 的 `log4j-to-slf4j` 桥（恒在
`spring-boot-starter-logging` 里）路由，无需额外配置。

***

## 相关文档

- 插件准入规范与目录：docs 仓 `registry.md`
- 编码规范：`CLAUDE.md`（组织级母本在 docs 仓，本仓为副本，**不要单独修改**）
- 发布步骤：docs 仓 `RELEASE.md`——发到 Maven Central 的版本不可撤回、不可覆盖

## License

Apache License 2.0
