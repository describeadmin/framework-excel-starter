# 更新日志

本文件遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 的组织方式，
版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)。

每个版本固定分 **Breaking Changes / New Features / Bug Fixes** 三类
（见组织编码规范第 5 节）。没有内容的类别保留标题并写「无」，
这样使用者不必怀疑是遗漏还是确实没有。

> **本插件的版本线与框架独立**。插件发 1.3.0 完全可能仍然只要求框架 1.0.0，
> 因此每个版本都必须写明适配的框架版本，见下方各条目的「框架要求」。

## 0.2.0 (2026-08-31)

describeadmin 的第一个 Web 层插件：为框架补上 Excel 导入 / 导出能力。
首个发布版本，版本号与 framework 0.2.0 对齐（本轮四个插件统一按框架号发布）。

**框架要求：0.2.0 及以上**（依赖 framework-web-starter 0.2.0 的 MVC 装配点与
`GlobalExceptionHandler` 契约；门面部分只依赖 framework-common）

### Breaking Changes

无（首个版本）。

### New Features

- 门面 `ExcelExporter` / `ExcelImporter`（`api/`，零 MVC 依赖可用）：
  `writeToResponse(HttpServletResponse, List<T>, Class<T>, ExcelWriteOptions)` 设置 xlsx
  `Content-Type` 与 RFC 5987 UTF-8 文件名，流式写出；
  `read(InputStream | MultipartFile, Class<T>) -> ExcelImportResult<T>` 收集逐行解析错误。
- `@ExcelResponse`（方法级）+ `ExcelResponseBodyAdvice`：把 `List<T>` / `PageResult<T>` /
  `Result<List<T>>` / `Result<PageResult<T>>` 直接当 xlsx 下载；`supports()` 仅做一次注解
  查找，不干扰任何 JSON 响应。
- `@ExcelBody`（参数级）+ `HandlerMethodArgumentResolver`：把上传文件绑成 `List<T>` 或
  `ExcelImportResult<T>`；非 multipart / 空文件 / 非 Excel 一律抛
  `BizException(BAD_REQUEST)`，由 `GlobalExceptionHandler` 渲染成 `Result`（不是 500）。
- `ExcelImportResult<T>` + `RowError`（`api/`，record，结构化逐行错误：行号 / 字段 / 列名 /
  消息 / 拒绝值）—— 框架此前没有这个形状。
- 雪花 `Long` / `long` 默认写成**文本单元格**（19 位超出 Excel 数值精度，同 `CLAUDE.md`
  4.8 的 JS `Number` 问题）；`@ExcelLongNumber` / `@ExcelResponse(longAsText = NUMBER)` /
  `describeadmin.excel.export.long-as-text=false` 三个逃生舱；导入时文本回解析为 `Long`。
- 运行时开关 `describeadmin.excel.enabled`（默认 `true`），关掉后 advice / resolver 均不
  注册，等同没引这个 jar。
- 导入前校验文件头（xlsx = ZIP，xls = OLE2）——Fesod / POI 对无法识别的字节不抛异常而是
  当 CSV 静默解析出 0 行，这个校验把"文件不对"变成一个明确信号。
- 基于 Apache Fesod（`fesod-sheet:2.0.2-incubating`，EasyExcel / FastExcel 的 ASF 捐赠版）；
  刻意不 import `fesod-bom`（它只仲裁 slf4j-api 1.7.36 与 lombok，会顶掉 Spring Boot 3.5
  的 slf4j 2.x）。
- `ExcelConverter<J>`（`api/`）：Fesod `Converter` SPI 的薄再导出，业务方可用
  `@ExcelProperty(converter = X.class)` 自解字典 / 枚举。DB 版 `@ExcelDict` 推迟到 v0.3.0。
- 启动期 `FrameworkVersion.requireCompatible` 自检。

### Bug Fixes

无（首个版本）。

### 仓库

- 独立成仓、独立版本线、独立发布（原方案 3.1.1 的既定拓扑）。
- POM **不继承 `framework-parent`**，改为 `import framework-bom`——这正是业务方消费框架的
  姿势，插件用同一套姿势才能提前暴露业务方会遇到的问题。CI 按框架版本矩阵跑完整测试。
- 发布晚于 framework 0.2.0：`import` 的 `framework-bom` 必须是一个真实存在的已发布版本，
  因此本插件的首发版本直接从 0.2.0 起。
- ⚠️ 已知风险：Apache Fesod 仍在孵化，其 POM 自述 EasyExcel 衍生代码 IP clearance 未完成，
  下游需自行做许可审查——已写进 README「兼容性」小节。
