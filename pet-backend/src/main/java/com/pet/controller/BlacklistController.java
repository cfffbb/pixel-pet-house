package com.pet.controller;

import com.pet.common.Result;
import com.pet.security.JwtInterceptor;
import com.pet.service.BlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 拉黑接口(需登录;被拉黑者无法给你发消息)
 */
@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    /** 我的拉黑列表 */
    @GetMapping
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        return Result.ok(blacklistService.list((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 拉黑某用户 */
    @PostMapping("/{userId}")
    public Result<Void> add(@PathVariable Long userId, HttpServletRequest request) {
        blacklistService.add((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), userId);
        return Result.ok();
    }

    /** 取消拉黑 */
    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId, HttpServletRequest request) {
        blacklistService.remove((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), userId);
        return Result.ok();
    }
}
