package com.nso.system.security;

/**
 * 公开账号支持请求限流端口接口。
 */
public interface PublicRequestRateLimitPort {

    /**
     * 判断密码恢复请求是否允许执行。
     *
     * @param clientIp 客户端地址
     * @param username 用户名
     * @return 是否允许请求
     */
    boolean allowPasswordRecovery(String clientIp, String username);

    /**
     * 判断访客支持请求是否允许执行。
     *
     * @param clientIp 客户端地址
     * @return 是否允许请求
     */
    boolean allowGuestSupport(String clientIp);
}
