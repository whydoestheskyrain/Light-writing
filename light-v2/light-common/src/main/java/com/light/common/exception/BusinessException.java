package com.light.common.exception;

import com.light.common.result.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 *
 * 为什么自定义异常而非直接抛 RuntimeException：
 * - GlobalExceptionHandler 可以精确捕获 BusinessException 做对应响应，
 *   而把其它 RuntimeException 视为系统异常（500）
 * - 业务层抛异常比层层 if/return 更清晰：发现违规立即抛，控制器专注正常路径
 *
 * 使用示例：
 *   if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
