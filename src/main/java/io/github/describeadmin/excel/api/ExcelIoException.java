package io.github.describeadmin.excel.api;

import java.io.Serial;

/**
 * 导出写出过程中的 I/O 失败（包装 {@link java.io.IOException}）。
 *
 * <p>通常发生在客户端提前断开、磁盘写满等场景。走 {@code @ExcelResponse} 时若在响应流
 * 已提交后才发生，只能由框架兜底异常处理器记录，无法再改写响应。
 */
public class ExcelIoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExcelIoException(String message, Throwable cause) {
        super(message, cause);
    }
}
