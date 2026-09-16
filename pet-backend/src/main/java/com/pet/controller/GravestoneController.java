package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.PetGravestone;
import com.pet.security.JwtInterceptor;
import com.pet.service.ReflectionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 墓碑(墓地界面数据源;点击墓碑查看反思内容)
 */
@RestController
@RequestMapping("/api/gravestones")
@RequiredArgsConstructor
public class GravestoneController {

    private final ReflectionService reflectionService;

    @GetMapping
    public Result<List<PetGravestone>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(reflectionService.listMine(userId));
    }
}
