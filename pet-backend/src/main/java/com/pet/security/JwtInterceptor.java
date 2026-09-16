package com.pet.security;

import cn.hutool.jwt.JWT;
import com.pet.common.ResultCode;
import com.pet.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器:校验请求头 Authorization: Bearer <token>,
 * 通过后把 userId / role 放进请求属性,控制器里直接取
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLE = "role";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 静态资源等非控制器请求不拦截
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String token = header.substring(7);
        JWT jwt = jwtUtil.parseAndVerify(token);

        Object userId = jwt.getPayload("userId");
        Object role = jwt.getPayload("role");
        if (userId == null || role == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        request.setAttribute(ATTR_USER_ID, ((Number) userId).longValue());
        request.setAttribute(ATTR_ROLE, String.valueOf(role));
        return true;
    }
}
