package com.light.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一返回结构。
 *
 * v1 改进点：
 * - 增加 timestamp：线上排查问题需要知道响应时刻，配合日志精确对齐
 * - code 改为 Integer 保留，前端按 code === 0 判断成功（兼容 v1 习惯）
 * - data 为 null 时不输出（@JsonInclude(NON_NULL)），减少响应体积
 *
 * 用法示例：
 *   Result.success(data)        -> { code:0, msg:"ok", data:{...}, timestamp:"..." }
 *   Result.error(ErrorCode.X)   -> { code:404, msg:"资源不存在", timestamp:"..." }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;
    private Instant timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setMsg(ErrorCode.SUCCESS.getMessage());
        r.setData(data);
        r.setTimestamp(Instant.now());
        return r;
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        Result<T> r = new Result<>();
        r.setCode(errorCode.getCode());
        r.setMsg(message);
        r.setTimestamp(Instant.now());
        return r;
    }
}
