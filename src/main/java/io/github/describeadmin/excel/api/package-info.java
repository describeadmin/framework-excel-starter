/**
 * framework-excel-starter 的对外契约面。
 *
 * <p>本包下的 public 签名属于兼容性承诺范围，改动需走 SemVer（见 CLAUDE.md 第 2、5 节）。
 * 门面 {@link io.github.describeadmin.excel.api.ExcelExporter} /
 * {@link io.github.describeadmin.excel.api.ExcelImporter} 不依赖 Spring MVC，可在任意
 * 代码里直接使用；{@code core/} 包里的 {@code @ExcelResponse} advice 与 {@code @ExcelBody}
 * resolver 只是建在门面之上的薄糖。
 */
package io.github.describeadmin.excel.api;
