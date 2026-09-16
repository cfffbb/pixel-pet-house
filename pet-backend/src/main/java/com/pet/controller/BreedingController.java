package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.BreedingRequestDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.BreedingService;
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
 * 配种市场接口(需登录)
 */
@RestController
@RequestMapping("/api/breeding")
@RequiredArgsConstructor
public class BreedingController {

    private final BreedingService breedingService;

    /** 市场:已上架的宠物列表(支持种类筛选/排序/分页) */
    @GetMapping("/market")
    public Result<Map<String, Object>> market(@RequestParam(required = false) String species,
                                              @RequestParam(defaultValue = "newest") String sort,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "9") long size,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(breedingService.market(userId, species, sort, page, size));
    }

    /** 发起配种申请(我是母方;双方各 10 币,管理员免) */
    @PostMapping("/request")
    public Result<Void> request(@Valid @RequestBody BreedingRequestDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String role = (String) request.getAttribute(JwtInterceptor.ATTR_ROLE);
        breedingService.request(userId, role, dto);
        return Result.ok();
    }

    /** 我的配种记录(顺带推进产崽) */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(breedingService.myRecords(userId));
    }

    /** 繁育详情:怀孕进度+冷却+就绪检查+幼崽统计 */
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(breedingService.breedingDetail(userId));
    }

    // ==================== 单机版后院繁育 ====================

    /** NPC候选宠物列表（单机版，不依赖其他玩家） */
    @GetMapping("/npc-market")
    public Result<List<Map<String, Object>>> npcMarket(@RequestParam(required = false) String species) {
        return Result.ok(breedingService.npcMarket(species));
    }

    /** 单机配种：用户宠物 + NPC宠物 → 直接怀孕 */
    @PostMapping("/single")
    public Result<Map<String, Object>> singleBreed(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        Long npcPetId = Long.valueOf(body.get("npcPetId").toString());
        return Result.ok(breedingService.singleBreed(userId, npcPetId));
    }

    /** 管理员调试：强制完成当前用户的繁育(怀孕→出生) */
    @PostMapping("/debug/finish")
    public Result<Map<String, Object>> debugFinish(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(breedingService.debugFinishBreeding(userId));
    }
}
