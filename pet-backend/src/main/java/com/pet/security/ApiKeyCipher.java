package com.pet.security;

import cn.hutool.crypto.SecureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * API Key 加解密:AES 对称加密(M2 拍板),密钥来自配置占位符,明文绝不落库
 */
@Component
public class ApiKeyCipher {

    @Value("${pet.security.aes-key}")
    private String aesKey;

    /** AES 密钥固定取 16 字节(过短补零,过长截断) */
    private static final int KEY_LEN = 16;

    private byte[] keyBytes() {
        return Arrays.copyOf(aesKey.getBytes(StandardCharsets.UTF_8), KEY_LEN);
    }

    /** 加密 Key,存库前调用 */
    public String encrypt(String plain) {
        return SecureUtil.aes(keyBytes()).encryptBase64(plain);
    }

    /** 解密 Key,仅用于给用户本人展示脱敏信息;密钥不对时返回 null */
    public String decrypt(String cipher) {
        try {
            return SecureUtil.aes(keyBytes()).decryptStr(cipher);
        } catch (Exception e) {
            return null;
        }
    }
}
