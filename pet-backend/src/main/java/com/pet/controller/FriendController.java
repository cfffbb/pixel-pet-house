package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.FriendRequestDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 好友接口(M11,需登录)
 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /** 发好友申请(到对方收件箱;支持用户名/玩家ID/用户ID) */
    @PostMapping("/request")
    public Result<Void> request(@RequestBody FriendRequestDTO dto, HttpServletRequest request) {
        friendService.request(
                (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID),
                dto.getTargetUsername(),
                dto.getTargetPlayerId(),
                dto.getTargetUserId());
        return Result.ok();
    }

    /** 我的好友列表 */
    @GetMapping
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        return Result.ok(friendService.list((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 删除好友(双向) */
    @DeleteMapping("/{friendId}")
    public Result<Void> remove(@PathVariable Long friendId, HttpServletRequest request) {
        friendService.remove((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), friendId);
        return Result.ok();
    }

    /** 免打扰开关(Iter-06) */
    @PutMapping("/{friendId}/mute")
    public Result<Void> toggleMute(@PathVariable Long friendId, @RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        friendService.toggleMute(
                (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID),
                friendId,
                ((Number) body.get("muted")).intValue());
        return Result.ok();
    }
}
