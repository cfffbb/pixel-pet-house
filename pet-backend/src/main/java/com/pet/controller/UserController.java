package com.pet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.Result;
import com.pet.entity.User;
import com.pet.mapper.UserMapper;
import com.pet.security.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户搜索接口(需登录)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /** 搜索用户(按用户名/昵称/玩家ID模糊搜索) */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String keyword, HttpServletRequest request) {
        Long myId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        if (keyword == null || keyword.isBlank()) {
            return Result.ok(List.of());
        }
        String kw = keyword.trim();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ne(User::getId, myId)
                .eq(User::getStatus, 1)
                .and(w -> w
                        .like(User::getUsername, kw)
                        .or().like(User::getNickname, kw)
                        .or().like(User::getPlayerId, kw))
                .last("LIMIT 20"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("playerId", u.getPlayerId());
            m.put("role", u.getRole());
            result.add(m);
        }
        return Result.ok(result);
    }
}
