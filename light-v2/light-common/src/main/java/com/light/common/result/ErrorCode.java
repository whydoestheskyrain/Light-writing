package com.light.common.result;

import lombok.Getter;

/**
 * 统一错误码枚举。
 *
 * 为什么用枚举而非散落的常量：
 * - 错误码集中管理，新增 / 调整一处即可，避免 magic number 散落各服务
 * - 每个码绑定 HTTP status，GlobalExceptionHandler 自动用对应 status 响应
 *   （401 而非 500，404 而非 200——前端能按 HTTP 语义处理）
 *
 * 设计原则：
 * - code 业务码（0 表示成功，其它为错误码），由各业务自行扩展
 * - httpStatus 对外暴露的 HTTP 状态码
 * - message 默认提示文案，可在 BusinessException 中覆盖
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, 200, "ok"),

    BAD_REQUEST(400, 400, "请求参数错误"),
    UNAUTHORIZED(401, 401, "未登录或登录已过期"),
    FORBIDDEN(403, 403, "无访问权限"),
    NOT_FOUND(404, 404, "资源不存在"),
    CONFLICT(409, 409, "资源冲突"),

    INTERNAL_ERROR(500, 500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, 503, "服务暂不可用");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
