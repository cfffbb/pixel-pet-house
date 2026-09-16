package com.pet.security;

import com.pet.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 工具单测:签发 → 校验的往返、防篡改、防过期。
 * 用 ReflectionTestUtils 注入 @Value 字段,无需启动 Spring 容器。
 */
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-jwt-secret-very-long-for-signing-ok");
        ReflectionTestUtils.setField(jwtUtil, "expireDays", 7);
    }

    @Test
    void createAndParse_roundtrip_keepsClaims() {
        String token = jwtUtil.createToken(123L, "admin1", "ADMIN");

        var jwt = jwtUtil.parseAndVerify(token);
        assertThat(jwt.getPayload("userId").toString()).isEqualTo("123");
        assertThat(jwt.getPayload("username")).isEqualTo("admin1");
        assertThat(jwt.getPayload("role")).isEqualTo("ADMIN");
    }

    @Test
    void parseAndVerify_tamperedSignature_throwsUnauthorized() {
        String token = jwtUtil.createToken(1L, "u", "USER");
        // 篡改签名段:保留 header.payload,换掉签名
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidSig";

        assertThatThrownBy(() -> jwtUtil.parseAndVerify(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void parseAndVerify_expiredToken_throwsUnauthorized() {
        ReflectionTestUtils.setField(jwtUtil, "expireDays", -1); // 签发即过期
        String token = jwtUtil.createToken(1L, "u", "USER");

        assertThatThrownBy(() -> jwtUtil.parseAndVerify(token))
                .isInstanceOf(BusinessException.class);
    }
}
