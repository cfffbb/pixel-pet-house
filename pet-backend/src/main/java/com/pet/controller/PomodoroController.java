package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.PomodoroRecord;
import com.pet.entity.PomodoroTask;
import com.pet.security.JwtInterceptor;
import com.pet.service.PomodoroService;
import jakarta.servlet.http.HttpServletRequest;
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
 * 番茄钟接口(M7,需登录)
 */
@RestController
@RequestMapping("/api/pomodoro")
@RequiredArgsConstructor
public class PomodoroController {

    private final PomodoroService pomodoroService;

    /** 开始一轮(durationMinutes 玩家自定义,默认 25;label 可选;taskId 可选关联任务) */
    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody(required = false) Map<String, Object> body,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        Integer minutes = body == null ? null : (Integer) body.get("durationMinutes");
        String label = body == null ? null : (String) body.get("label");
        Long taskId = body == null ? null : body.get("taskId") == null ? null : ((Number) body.get("taskId")).longValue();
        return Result.ok(pomodoroService.start(userId, minutes, label, taskId));
    }

    /** 暂停(每轮最多 2 次) */
    @PostMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id, HttpServletRequest request) {
        pomodoroService.pause((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    @PostMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id, HttpServletRequest request) {
        pomodoroService.resume((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    /** 完成:专注分钟 × 1 币 */
    @PostMapping("/{id}/complete")
    public Result<Map<String, Object>> complete(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(pomodoroService.complete((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id));
    }

    /** 放弃:扣除部分游戏币(可为负数) */
    @PostMapping("/{id}/abandon")
    public Result<Map<String, Object>> abandon(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(pomodoroService.abandon((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id));
    }

    /** 记录查询:日期范围/排序(时间新旧/专注分钟)/分页 */
    @GetMapping("/my")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<PomodoroRecord>> my(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(pomodoroService.my(userId, from, to, sort, page, size));
    }

    /** 统计:今日/本周/最近7天柱状图数据 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(pomodoroService.stats(userId));
    }

    // ==================== 任务管理(参考番茄Todo) ====================

    /** 查询任务列表(view: today/todo/done/all) */
    @GetMapping("/tasks")
    public Result<List<PomodoroTask>> tasks(
            @RequestParam(defaultValue = "all") String view,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(pomodoroService.taskList(userId, view, tag, date));
    }

    /** 创建任务 */
    @PostMapping("/tasks")
    public Result<PomodoroTask> createTask(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String title = (String) body.get("title");
        String tag = body.get("tag") == null ? null : (String) body.get("tag");
        Integer estimateCount = body.get("estimateCount") == null ? null : ((Number) body.get("estimateCount")).intValue();
        String planDate = body.get("planDate") == null ? null : (String) body.get("planDate");
        return Result.ok(pomodoroService.createTask(userId, title, tag, estimateCount, planDate));
    }

    /** 更新任务 */
    @PutMapping("/tasks/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String title = body.get("title") == null ? null : (String) body.get("title");
        String tag = body.get("tag") == null ? null : (String) body.get("tag");
        Integer estimateCount = body.get("estimateCount") == null ? null : ((Number) body.get("estimateCount")).intValue();
        String planDate = body.get("planDate") == null ? null : (String) body.get("planDate");
        Integer sortOrder = body.get("sortOrder") == null ? null : ((Number) body.get("sortOrder")).intValue();
        pomodoroService.updateTask(userId, id, title, tag, estimateCount, planDate, sortOrder);
        return Result.ok();
    }

    /** 勾选完成/取消完成 */
    @PostMapping("/tasks/{id}/toggle")
    public Result<Void> toggleTask(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        pomodoroService.toggleTask(userId, id);
        return Result.ok();
    }

    /** 删除任务 */
    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        pomodoroService.deleteTask(userId, id);
        return Result.ok();
    }

    /** 标签统计 */
    @GetMapping("/tag-stats")
    public Result<List<Map<String, Object>>> tagStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(pomodoroService.tagStats(userId));
    }
}
