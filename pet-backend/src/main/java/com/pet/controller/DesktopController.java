package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.DesktopZoneDTO;
import com.pet.entity.DesktopZone;
import com.pet.security.JwtInterceptor;
import com.pet.service.DesktopZoneService;
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
 * 桌面整理接口(M8,需登录;文件移动由客户端主进程执行)
 */
@RestController
@RequestMapping("/api/desktop")
@RequiredArgsConstructor
public class DesktopController {

    private final DesktopZoneService desktopZoneService;

    /** 分区列表(首次自动建默认分区) */
    @GetMapping("/zones")
    public Result<List<DesktopZone>> zones(HttpServletRequest request) {
        return Result.ok(desktopZoneService.list((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    @PostMapping("/zones")
    public Result<Void> addZone(@Valid @RequestBody DesktopZoneDTO dto, HttpServletRequest request) {
        desktopZoneService.add((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }

    @PutMapping("/zones/{id}")
    public Result<Void> updateZone(@PathVariable Long id, @Valid @RequestBody DesktopZoneDTO dto, HttpServletRequest request) {
        desktopZoneService.update((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id, dto);
        return Result.ok();
    }

    @DeleteMapping("/zones/{id}")
    public Result<Void> deleteZone(@PathVariable Long id, HttpServletRequest request) {
        desktopZoneService.delete((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    /** 客户端移动完成后上报记录 */
    @PostMapping("/organize/record")
    public Result<Void> saveRecord(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        int moved = body.get("movedCount") == null ? 0 : ((Number) body.get("movedCount")).intValue();
        desktopZoneService.saveRecord(userId, moved, (String) body.get("detailsJson"));
        return Result.ok();
    }

    /** 最近一次未撤销的整理记录(含明细) */
    @GetMapping("/organize/latest")
    public Result<Map<String, Object>> latest(HttpServletRequest request) {
        return Result.ok(desktopZoneService.latest((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 标记撤销完成 */
    @PostMapping("/organize/{id}/undone")
    public Result<Void> markUndone(@PathVariable Long id, HttpServletRequest request) {
        desktopZoneService.markUndone((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }
}
