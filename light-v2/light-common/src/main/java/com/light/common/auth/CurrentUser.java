package com.light.common.auth;

import com.light.common.exception.BusinessException;
import com.light.common.result.ErrorCode;

/** 请求范围内的当前用户。请求结束后由过滤器清理，避免线程复用造成身份串线。 */
public final class CurrentUser {
    private static final ThreadLocal<JwtService.UserPrincipal> HOLDER = new ThreadLocal<>();

    private CurrentUser() {}

    public static void set(JwtService.UserPrincipal principal) { HOLDER.set(principal); }
    public static void clear() { HOLDER.remove(); }
    public static JwtService.UserPrincipal get() { return HOLDER.get(); }

    public static Long requireId() {
        JwtService.UserPrincipal principal = get();
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return principal.id();
    }
}
