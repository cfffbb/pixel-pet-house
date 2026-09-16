package com.pet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Key 加解密单测:加密 → 解密可还原;非法密文解密返回 null(不抛异常)。
 */
class ApiKeyCipherTest {

    private final ApiKeyCipher cipher = new ApiKeyCipher();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cipher, "aesKey", "test-aes-key-16bytes");
    }

    @Test
    void encryptThenDecrypt_roundtrip_restoresPlain() {
        String plain = "sk-abcdef1234567890";

        String enc = cipher.encrypt(plain);

        assertThat(enc).isNotEqualTo(plain);
        assertThat(cipher.decrypt(enc)).isEqualTo(plain);
    }

    @Test
    void decrypt_garbageCipher_returnsNull() {
        // 非法密文不应抛异常,而是返回 null(见 ApiKeyCipher.decrypt 的 try-catch)
        assertThat(cipher.decrypt("not-a-valid-cipher-text")).isNull();
    }
}
