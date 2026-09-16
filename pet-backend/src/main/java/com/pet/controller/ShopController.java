package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.ShopItem;
import com.pet.security.JwtInterceptor;
import com.pet.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商店与喂食接口(M7,需登录)
 */
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /** 商店物品(按当前宠物过滤 + 等级锁定) */
    @GetMapping("/items")
    public Result<Map<String, Object>> items(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(shopService.items(userId));
    }

    /** 我的宠物背包 */
    @GetMapping("/inventory")
    public Result<List<Map<String, Object>>> inventory(HttpServletRequest request) {
        return Result.ok(shopService.inventory((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 购买(扣币,进背包;管理员无限币) */
    @PostMapping("/buy")
    public Result<Void> buy(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String role = (String) request.getAttribute(JwtInterceptor.ATTR_ROLE);
        shopService.buy(userId, role, body.get("itemId"),
                body.get("quantity") == null ? 1 : body.get("quantity").intValue());
        return Result.ok();
    }

    /** 喂食(扣背包食物,恢复饥饿/心情,连续喂食/幸运事件/多样性) */
    @PostMapping("/feed")
    public Result<Map<String, Object>> feed(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(shopService.feed(userId, body.get("itemId")));
    }

    /** 喂食统计(连击/总数/幸运/最近10餐) */
    @GetMapping("/feeding-stats")
    public Result<Map<String, Object>> feedingStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(shopService.feedingStats(userId));
    }
}
