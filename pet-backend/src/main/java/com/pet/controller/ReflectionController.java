package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.ReflectionDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.ReflectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 反思答题(2 道开放式问题,每题 ≥10 字;答完才可重新抽蛋/领幼崽)
 */
@RestController
@RequestMapping("/api/reflection")
@RequiredArgsConstructor
public class ReflectionController {

    private final ReflectionService reflectionService;

    @GetMapping("/questions")
    public Result<Map<String, String>> questions() {
        return Result.ok(reflectionService.questions());
    }

    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody ReflectionDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        reflectionService.submit(userId, dto);
        return Result.ok();
    }
}
