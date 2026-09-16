package com.pet.controller;

import com.pet.common.Result;
import com.pet.security.JwtInterceptor;
import com.pet.service.CompanionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 陪伴推送文案接口(M5)
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final CompanionService companionService;

    /** 随机陪伴推送(一天 2–3 次,由客户端控制频率) */
    @GetMapping("/companion")
    public Result<Map<String, String>> companion(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(companionService.companion(userId));
    }

    /** 卖萌替代文案(10% 概率由客户端决定是否用) */
    @GetMapping("/naughty")
    public Result<Map<String, String>> naughty() {
        return Result.ok(companionService.naughty());
    }
}
