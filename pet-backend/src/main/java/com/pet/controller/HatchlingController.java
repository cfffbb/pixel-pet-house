package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.ClaimDTO;
import com.pet.dto.GiftDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.HatchlingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 托管所接口(需登录)
 */
@RestController
@RequestMapping("/api/hatchling")
@RequiredArgsConstructor
public class HatchlingController {

    private final HatchlingService hatchlingService;

    /** 我的托管幼崽(顺带结算托管费,欠费冻结) */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        return Result.ok(hatchlingService.listMine((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 领取幼崽(无活宠 + 已通过反思答题 + 强制起名) */
    @PostMapping("/claim")
    public Result<Map<String, Object>> claim(@Valid @RequestBody ClaimDTO dto, HttpServletRequest request) {
        return Result.ok(hatchlingService.claim((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto));
    }

    /** 赠送申请(发到对方收件箱,对方同意后转移) */
    @PostMapping("/gift")
    public Result<Void> gift(@Valid @RequestBody GiftDTO dto, HttpServletRequest request) {
        hatchlingService.giftRequest((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }
}
