package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.DrawDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.PetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 宠物核心接口(需登录)
 */
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /** 抽蛋(一生一次;死过必须先反思答题;管理员可传 typeCode 自由选种) */
    @PostMapping("/draw")
    public Result<Map<String, Object>> draw(@Valid @RequestBody DrawDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String role = (String) request.getAttribute(JwtInterceptor.ATTR_ROLE);
        return Result.ok(petService.draw(userId, role, dto.getPetName(), dto.getTypeCode()));
    }

    /** 我的宠物(状态实时推进:饥饿/病危剩余时间) */
    @GetMapping("/my")
    public Result<Map<String, Object>> my(HttpServletRequest request) {
        return Result.ok(petService.my((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 改名 */
    @PostMapping("/rename")
    public Result<Void> rename(@RequestParam String newName, HttpServletRequest request) {
        petService.rename((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), newName);
        return Result.ok();
    }

    /** 玩耍:10 种模式(抚摸/跑步/学习/玩球/散步/听音乐/跳舞/捉迷藏/晒太阳/扑蝶),每天最多 5 次 */
    @PostMapping("/play")
    public Result<Map<String, Object>> play(@RequestBody(required = false) Map<String, String> body,
                                            HttpServletRequest request) {
        String mode = body == null ? null : body.get("mode");
        return Result.ok(petService.play((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), mode));
    }

    /** 玩耍模式列表(UI 配置用) */
    @GetMapping("/play-modes")
    public Result<List<Map<String, Object>>> playModes() {
        return Result.ok(petService.playModes());
    }

    /** 上架/下架配种市场(雄/活着/成熟 3 天/非冷却才能上架;管理员可无视成熟) */
    @PostMapping("/listed")
    public Result<Map<String, Object>> listed(@RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        boolean on = body.get("listed") != null && body.get("listed");
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String role = (String) request.getAttribute(JwtInterceptor.ATTR_ROLE);
        return Result.ok(petService.setListed(userId, role, on));
    }

    /** 13 种宠物 + 亚种列表(展示用) */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> types() {
        return Result.ok(petService.types());
    }
}
