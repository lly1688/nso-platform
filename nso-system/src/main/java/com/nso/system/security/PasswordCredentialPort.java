package com.nso.system.security;

/**
 * 临时密码凭证端口接口。
 */
public interface PasswordCredentialPort {

    /**
     * 签发一次性临时密码。
     *
     * @return 临时密码及其散列
     */
    TemporaryPassword issueTemporaryPassword();

    /**
     * 临时密码凭证。
     *
     * @param plainText 明文临时密码
     * @param passwordHash 密码散列
     */
    record TemporaryPassword(String plainText, String passwordHash) {
    }
}
