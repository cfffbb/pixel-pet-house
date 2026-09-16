package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.SettingDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.UserSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户设置接口(需登录)
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;

    /** 读取设置(含配种授权开关) */
    @GetMapping
    public Result<Map<String, Object>> get(HttpServletRequest request) {
        return Result.ok(userSettingService.get((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 配种授权开关(默认开,M3.5 拍板) */
    @PutMapping("/allow-breeding")
    public Result<Void> setAllowBreeding(@Valid @RequestBody SettingDTO dto, HttpServletRequest request) {
        userSettingService.setAllowBreeding((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto.getValue());
        return Result.ok();
    }

    /** 应用感知开关(默认关,隐私红线,M9) */
    @PutMapping("/app-monitor")
    public Result<Void> setAppMonitor(@Valid @RequestBody SettingDTO dto, HttpServletRequest request) {
        userSettingService.setAppMonitor((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto.getValue());
        return Result.ok();
    }
}
