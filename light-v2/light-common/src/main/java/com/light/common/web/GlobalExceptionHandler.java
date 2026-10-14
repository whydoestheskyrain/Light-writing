package com.light.common.web;

import com.light.common.exception.BusinessException;
import com.light.common.result.ErrorCode;
import com.light.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理（Servlet 栈）。
 *
 * 设计要点：
 * - 按"异常类型"分别处理：业务异常透传错误码，参数校验失败返回 400，
 *   未知异常归为 500——前端只需看 HTTP status 和 code 字段
 * - 用 ResponseEntity 让 HTTP status 与 ErrorCode.httpStatus 对齐，
 *   避免前端看到 200 + code:500 这种语义混乱
 * - 业务异常打 warn，系统异常打 error 并带 stack trace，便于分级告警
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：返回对应 ErrorCode 的 httpStatus */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("业务异常 [{}] {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus()).body(Result.error(code, ex.getMessage()));
    }

    /** 参数校验失败：@Valid 触发，返回首个字段错误和 HTTP 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        FieldError fe = ex.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : "参数错误";
        log.warn("参数校验失败 [{}] {} - {}", req.getMethod(), req.getRequestURI(), msg);
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(Result.error(ErrorCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException ex, HttpServletRequest req) {
        FieldError fe = ex.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : "参数错误";
        return ResponseEntity.badRequest().body(Result.error(ErrorCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleRequestParameter(Exception ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(Result.error(ErrorCode.BAD_REQUEST, "请求参数格式错误"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(Result.error(ErrorCode.BAD_REQUEST, "上传文件超过大小限制"));
    }

    /** 兜底：未知异常统一 500，不向前端暴露堆栈 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("系统异常 [{}] {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(Result.error(ErrorCode.INTERNAL_ERROR));
    }
}
