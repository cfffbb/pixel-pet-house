package com.pet.controller;

import com.pet.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口:M1 用来验证"后端能启动 + 数据库连得上"
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Long one = jdbcTemplate.queryForObject("SELECT 1", Long.class);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "pet-backend");
        data.put("status", "UP");
        data.put("db", one != null && one == 1L ? "UP" : "DOWN");
        return Result.ok(data);
    }
}
