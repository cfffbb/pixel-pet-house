package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.ReportCreateDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报接口(需登录;举报内容进管理员审核中心)
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public Result<Void> create(@Valid @RequestBody ReportCreateDTO dto, HttpServletRequest request) {
        reportService.create((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }
}
