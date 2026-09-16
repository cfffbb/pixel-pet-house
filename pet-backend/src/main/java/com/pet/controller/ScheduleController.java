package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.ScheduleDTO;
import com.pet.entity.Schedule;
import com.pet.security.JwtInterceptor;
import com.pet.service.ScheduleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 日程任务接口(M5,需登录)
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 我的日程列表 */
    @GetMapping
    public Result<List<Schedule>> list(HttpServletRequest request) {
        return Result.ok(scheduleService.list((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody ScheduleDTO dto, HttpServletRequest request) {
        scheduleService.add((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ScheduleDTO dto, HttpServletRequest request) {
        scheduleService.update((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        scheduleService.delete((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    /** 完成日程(宠物亲亲由前端表现) */
    @PostMapping("/{id}/done")
    public Result<Map<String, Object>> done(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(scheduleService.done((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id));
    }

    /** 取消完成(恢复为待办) */
    @PostMapping("/{id}/undone")
    public Result<Void> undone(@PathVariable Long id, HttpServletRequest request) {
        scheduleService.undone((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    /** 我的标签(预设+自定义) */
    @GetMapping("/tags")
    public Result<List<String>> tags(HttpServletRequest request) {
        return Result.ok(scheduleService.tags((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 统计:饼图/近7天柱状/启动时间分布(支持日期范围查询) */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest request) {
        return Result.ok(scheduleService.stats(
                (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), from, to));
    }

    /** 到期提醒(客户端主进程每分钟轮询;返回即视为已提醒) */
    @GetMapping("/remind")
    public Result<List<Map<String, Object>>> remind(HttpServletRequest request) {
        return Result.ok(scheduleService.dueReminders((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }
}
