package com.pet.security;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import com.pet.common.ResultCode;
import com.pet.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具:Hutool 自带 JWT 支持,无额外依赖
 */
@Component
public class JwtUtil {

    @Value("${pet.security.jwt-secret}")
    private String secret;

    @Value("${pet.security.jwt-expire-days}")
    private int expireDays;

    /** 生成 token(有效期:M2 拍板 7 天) */
    public String createToken(Long userId, String username, String role) {
        long expireMillis = System.currentTimeMillis() + expireDays * 24L * 3600 * 1000;
        return JWT.create()
                .setPayload("userId", userId)
                .setPayload("username", username)
                .setPayload("role", role)
                .setExpiresAt(new Date(expireMillis))
                .setKey(secret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /**
     * 解析并校验 token(签名 + 过期时间),任一不通过都抛 401
     */
    public JWT parseAndVerify(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        jwt.setKey(secret.getBytes(StandardCharsets.UTF_8));
        if (!jwt.verify()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        try {
            JWTValidator.of(jwt).validateDate(new Date());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return jwt;
    }
}
