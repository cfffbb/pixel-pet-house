package com.pet.controller;

import com.pet.common.Result;
import com.pet.security.JwtInterceptor;
import com.pet.service.AppUsageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 应用采样上报接口(M9;需登录且授权开关开启才会上报)
 */
@RestController
@RequestMapping("/api/app-usage")
@RequiredArgsConstructor
public class AppUsageController {

    private final AppUsageService appUsageService;

    @PostMapping("/log")
    public Result<Void> log(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        appUsageService.log(userId, body.get("appName"), body.get("windowTitle"));
        return Result.ok();
    }
}
