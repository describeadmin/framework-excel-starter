package io.github.describeadmin.excel.api;

import java.io.Serial;

/**
 * 整个输入流无法作为工作簿读取时抛出（文件损坏、根本不是 xlsx、加密无密钥等）。
 *
 * <p>与 {@link RowError} 的分工：<b>单行</b>的解析 / 校验失败进 {@code RowError}，不抛异常；
 * 只有连表头都读不出来这种"整份文件无效"才抛本异常。
 *
 * <p>在 Spring MVC 之外直接用 {@link ExcelImporter} 的调用方需要自己捕获它；
 * 走 {@code @ExcelBody} 时 {@code ExcelBodyArgumentResolver} 会把它转成
 * {@code BizException(ResultCode.BAD_REQUEST, ...)}，由框架全局异常处理器渲染。
 */
public class ExcelParseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExcelParseException(String message) {
        super(message);
    }

    public ExcelParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
