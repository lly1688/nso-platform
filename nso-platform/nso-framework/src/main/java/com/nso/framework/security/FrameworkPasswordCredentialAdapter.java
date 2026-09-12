package com.nso.framework.security;

import com.nso.system.security.PasswordCredentialPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// 临时密码凭证适配器。
@Component
public class FrameworkPasswordCredentialAdapter implements PasswordCredentialPort {

    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SYMBOLS = "!@#$%".toCharArray();
    private static final char[] ALL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
    private static final int PASSWORD_LENGTH = 14;

    // 密码编码器
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public FrameworkPasswordCredentialAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // 生成满足复杂度要求的随机临时密码。
    @Override
    public TemporaryPassword issueTemporaryPassword() {
        char[] value = new char[PASSWORD_LENGTH];
        value[0] = random(UPPER);
        value[1] = random(LOWER);
        value[2] = random(DIGITS);
        value[3] = random(SYMBOLS);
        for (int index = 4; index < value.length; index++) {
            value[index] = random(ALL);
        }
        for (int index = value.length - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            char current = value[index];
            value[index] = value[swapIndex];
            value[swapIndex] = current;
        }
        String plainText = new String(value);
        return new TemporaryPassword(plainText, passwordEncoder.encode(plainText));
    }

    private char random(char[] alphabet) {
        return alphabet[secureRandom.nextInt(alphabet.length)];
    }
}
