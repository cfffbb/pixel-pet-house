package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Pet;
import com.pet.entity.PomodoroRecord;
import com.pet.entity.PomodoroTask;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PomodoroRecordMapper;
import com.pet.mapper.PomodoroTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 番茄钟业务(M7 拍板):
 * - 时长玩家自定义(默认 25 分钟示例)
 * - 1 币/分钟(20 分钟 20 币、1 小时 60 币)
 * - 每轮最多暂停 2 次;放弃得 0 币
 * - 等级 = 累计专注分钟,每 1200 分钟(20 小时)升 1 级,最高 100 级
 */
@Service
@RequiredArgsConstructor
public class PomodoroService {

    private final PomodoroRecordMapper recordMapper;
    private final PomodoroTaskMapper taskMapper;
    private final PetMapper petMapper;
    private final PetService petService;

    /** 开始一轮(时长玩家自定义,默认 25 分钟;label 可选;taskId 可选关联任务) */
    public Map<String, Object> start(Long userId, Integer durationMinutes, String label, Long taskId) {
        Pet pet = petService.getMyPet(userId);
        if (pet == null) {
            throw new BusinessException("你还没有宠物");
        }
        int minutes = durationMinutes == null || durationMinutes <= 0 ? 25 : durationMinutes;
        if (minutes > PetConstants.POMODORO_MAX_MINUTES) {
            throw new BusinessException("单轮最多 " + PetConstants.POMODORO_MAX_MINUTES + " 分钟");
        }
        PomodoroRecord r = new PomodoroRecord();
        r.setUserId(userId);
        r.setPetId(pet.getId());
        r.setStartedAt(LocalDateTime.now());
        r.setPausedCount(0);
        r.setTotalPausedMinutes(0);
        r.setDurationMinutes(0);
        r.setCompleted(0);
        r.setCoinsEarned(0);
        r.setLabel(label);
        r.setTaskId(taskId);
        recordMapper.insert(r);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", r.getId());
        data.put("durationMinutes", minutes);
        return data;
    }

    public void pause(Long userId, Long recordId) {
        PomodoroRecord r = requireOwn(userId, recordId);
        if (r.getCompleted() != null && r.getCompleted() == 1) {
            throw new BusinessException("本轮已结束");
        }
        if (r.getPausedAt() != null) {
            throw new BusinessException("已在暂停中");
        }
        r.setPausedAt(LocalDateTime.now());
        r.setPausedCount(r.getPausedCount() + 1);
        recordMapper.updateById(r);
    }

    public void resume(Long userId, Long recordId) {
        PomodoroRecord r = requireOwn(userId, recordId);
        if (r.getPausedAt() == null) {
            throw new BusinessException("当前不在暂停中");
        }
        long pausedMin = Duration.between(r.getPausedAt(), LocalDateTime.now()).toMinutes();
        r.setTotalPausedMinutes((r.getTotalPausedMinutes() == null ? 0 : r.getTotalPausedMinutes()) + (int) pausedMin);
        r.setPausedAt(null);
        recordMapper.updateById(r);
    }

    /** 完成:专注分钟 × 1 币,加经验升级(M7 拍板) */
    public Map<String, Object> complete(Long userId, Long recordId) {
        PomodoroRecord r = requireOwn(userId, recordId);
        if (r.getCompleted() != null && r.getCompleted() == 1) {
            throw new BusinessException("本轮已结束");
        }
        if (r.getPausedAt() != null) {
            long pausedMin = Duration.between(r.getPausedAt(), LocalDateTime.now()).toMinutes();
            r.setTotalPausedMinutes((r.getTotalPausedMinutes() == null ? 0 : r.getTotalPausedMinutes()) + (int) pausedMin);
            r.setPausedAt(null);
        }
        long elapsedMin = Duration.between(r.getStartedAt(), LocalDateTime.now()).toMinutes();
        int focused = Math.max(0, (int) elapsedMin - (r.getTotalPausedMinutes() == null ? 0 : r.getTotalPausedMinutes()));
        int coins = focused * PetConstants.POMODORO_COIN_PER_MINUTE;
        r.setEndedAt(LocalDateTime.now());
        r.setDurationMinutes(focused);
        r.setCompleted(1);
        r.setCoinsEarned(coins);
        recordMapper.updateById(r);

        // 关联任务的完成番茄数 +1
        if (r.getTaskId() != null) {
            PomodoroTask task = taskMapper.selectById(r.getTaskId());
            if (task != null && task.getUserId().equals(userId) && task.getStatus() == 0) {
                task.setDoneCount((task.getDoneCount() == null ? 0 : task.getDoneCount()) + 1);
                taskMapper.updateById(task);
            }
        }

        Pet pet = petService.getMyPet(userId);
        if (pet != null) {
            pet.setCoins(pet.getCoins() + coins);
            pet.setExp((pet.getExp() == null ? 0 : pet.getExp()) + focused); // exp = 累计专注分钟
            pet.setLevel(calcLevel(pet.getExp()));
            petMapper.updateById(pet);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("coins", coins);
        data.put("level", pet != null ? pet.getLevel() : null);
        data.put("message", "专注 " + focused + " 分钟,获得 " + coins + " 币"
                + (pet != null ? ",等级 " + pet.getLevel() : ""));
        return data;
    }

    /** 放弃:扣除专注时长对应游戏币的一半(可为负数) */
    public Map<String, Object> abandon(Long userId, Long recordId) {
        PomodoroRecord r = requireOwn(userId, recordId);
        if (r.getCompleted() != null && r.getCompleted() == 1) {
            throw new BusinessException("本轮已结束");
        }
        long elapsedMin = Duration.between(r.getStartedAt(), LocalDateTime.now()).toMinutes();
        int focused = Math.max(0, (int) elapsedMin - (r.getTotalPausedMinutes() == null ? 0 : r.getTotalPausedMinutes()));
        int penalty = -(focused * PetConstants.POMODORO_COIN_PER_MINUTE / 2);
        r.setEndedAt(LocalDateTime.now());
        r.setCompleted(0);
        r.setCoinsEarned(penalty);
        r.setDurationMinutes(0);
        recordMapper.updateById(r);

        Pet pet = petService.getMyPet(userId);
        if (pet != null) {
            pet.setCoins(pet.getCoins() + penalty);
            petMapper.updateById(pet);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("coins", penalty);
        data.put("message", "已放弃,扣除 " + Math.abs(penalty) + " 游戏币"
                + (pet != null ? ",当前 " + pet.getCoins() + " 币" : ""));
        return data;
    }

    /** 等级 = 1 + 累计分钟/1200,上限 100(M7 拍板) */
    public static int calcLevel(int totalMinutes) {
        return Math.min(PetConstants.LEVEL_MAX, 1 + totalMinutes / PetConstants.LEVEL_MINUTES_PER_LEVEL);
    }

    /** 记录查询:按日期范围/排序/分页(M12) */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<PomodoroRecord> my(
            Long userId, String from, String to, String sort, long page, long size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PomodoroRecord> pg = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<PomodoroRecord> qw = new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getUserId, userId);
        if (cn.hutool.core.util.StrUtil.isNotBlank(from)) {
            qw.ge(PomodoroRecord::getStartedAt, java.time.LocalDate.parse(from).atStartOfDay());
        }
        if (cn.hutool.core.util.StrUtil.isNotBlank(to)) {
            qw.lt(PomodoroRecord::getStartedAt, java.time.LocalDate.parse(to).plusDays(1).atStartOfDay());
        }
        if ("min".equals(sort)) {
            qw.orderByDesc(PomodoroRecord::getDurationMinutes);
        } else if ("old".equals(sort)) {
            qw.orderByAsc(PomodoroRecord::getStartedAt);
        } else {
            qw.orderByDesc(PomodoroRecord::getStartedAt);
        }
        return recordMapper.selectPage(pg, qw);
    }

    /** 统计:今日/本周/最近7天每天的专注分钟 */
    public Map<String, Object> stats(Long userId) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(6); // 最近7天(含今天)

        // 今日完成的总分钟
        LambdaQueryWrapper<PomodoroRecord> todayQw = new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getUserId, userId)
                .eq(PomodoroRecord::getCompleted, 1)
                .ge(PomodoroRecord::getStartedAt, todayStart);
        List<PomodoroRecord> todayList = recordMapper.selectList(todayQw);
        int todayMin = todayList.stream().mapToInt(r -> r.getDurationMinutes() == null ? 0 : r.getDurationMinutes()).sum();
        int todayCount = todayList.size();

        // 最近7天每天的专注分钟
        LambdaQueryWrapper<PomodoroRecord> weekQw = new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getUserId, userId)
                .eq(PomodoroRecord::getCompleted, 1)
                .ge(PomodoroRecord::getStartedAt, weekStart)
                .orderByAsc(PomodoroRecord::getStartedAt);
        List<PomodoroRecord> weekList = recordMapper.selectList(weekQw);

        // 按天聚合
        int[] dailyMinutes = new int[7];
        String[] dailyLabels = new String[7];
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate d = todayStart.toLocalDate().minusDays(6 - i);
            dailyLabels[i] = d.getMonthValue() + "/" + d.getDayOfMonth();
        }
        for (PomodoroRecord r : weekList) {
            java.time.LocalDate d = r.getStartedAt().toLocalDate();
            long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(d, todayStart.toLocalDate());
            if (daysAgo >= 0 && daysAgo < 7) {
                int idx = 6 - (int) daysAgo;
                dailyMinutes[idx] += r.getDurationMinutes() == null ? 0 : r.getDurationMinutes();
            }
        }

        // 本周总分钟
        int weekMin = 0;
        for (int m : dailyMinutes) weekMin += m;

        // 总累计
        LambdaQueryWrapper<PomodoroRecord> totalQw = new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getUserId, userId)
                .eq(PomodoroRecord::getCompleted, 1);
        Long totalCount = recordMapper.selectCount(totalQw);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayMinutes", todayMin);
        data.put("todayCount", todayCount);
        data.put("weekMinutes", weekMin);
        data.put("totalCount", totalCount == null ? 0 : totalCount.intValue());
        data.put("dailyLabels", dailyLabels);
        data.put("dailyMinutes", dailyMinutes);
        return data;
    }

    private PomodoroRecord requireOwn(Long userId, Long recordId) {
        PomodoroRecord r = recordMapper.selectById(recordId);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new BusinessException("计时记录不存在");
        }
        return r;
    }

    // ==================== 任务管理(参考番茄Todo) ====================

    /** 查询任务列表(view: today/todo/done/all) */
    public List<PomodoroTask> taskList(Long userId, String view, String tag, String date) {
        LambdaQueryWrapper<PomodoroTask> qw = new LambdaQueryWrapper<PomodoroTask>()
                .eq(PomodoroTask::getUserId, userId);
        if ("today".equals(view) && date != null) {
            qw.eq(PomodoroTask::getPlanDate, date);
        } else if ("todo".equals(view)) {
            qw.eq(PomodoroTask::getStatus, 0);
        } else if ("done".equals(view)) {
            qw.eq(PomodoroTask::getStatus, 1);
        }
        if (cn.hutool.core.util.StrUtil.isNotBlank(tag)) {
            qw.eq(PomodoroTask::getTag, tag);
        }
        qw.orderByAsc(PomodoroTask::getSortOrder).orderByDesc(PomodoroTask::getCreatedAt);
        return taskMapper.selectList(qw);
    }

    /** 创建任务 */
    public PomodoroTask createTask(Long userId, String title, String tag, Integer estimateCount, String planDate) {
        if (cn.hutool.core.util.StrUtil.isBlank(title)) {
            throw new BusinessException("任务名称不能为空");
        }
        PomodoroTask t = new PomodoroTask();
        t.setUserId(userId);
        t.setTitle(title.trim());
        t.setTag(tag);
        t.setEstimateCount(estimateCount == null ? 1 : estimateCount);
        t.setDoneCount(0);
        t.setStatus(0);
        t.setSortOrder(0);
        t.setPlanDate(planDate);
        taskMapper.insert(t);
        return t;
    }

    /** 更新任务 */
    public void updateTask(Long userId, Long taskId, String title, String tag, Integer estimateCount, String planDate, Integer sortOrder) {
        PomodoroTask t = requireOwnTask(userId, taskId);
        if (title != null) t.setTitle(title.trim());
        if (tag != null) t.setTag(tag);
        if (estimateCount != null) t.setEstimateCount(estimateCount);
        if (planDate != null) t.setPlanDate(planDate);
        if (sortOrder != null) t.setSortOrder(sortOrder);
        taskMapper.updateById(t);
    }

    /** 勾选完成/取消完成 */
    public void toggleTask(Long userId, Long taskId) {
        PomodoroTask t = requireOwnTask(userId, taskId);
        t.setStatus(t.getStatus() == 1 ? 0 : 1);
        taskMapper.updateById(t);
    }

    /** 删除任务 */
    public void deleteTask(Long userId, Long taskId) {
        PomodoroTask t = requireOwnTask(userId, taskId);
        taskMapper.deleteById(t.getId());
    }

    /** 统计标签分布 */
    public List<Map<String, Object>> tagStats(Long userId) {
        List<PomodoroTask> all = taskMapper.selectList(new LambdaQueryWrapper<PomodoroTask>()
                .eq(PomodoroTask::getUserId, userId));
        Map<String, int[]> m = new LinkedHashMap<>();
        for (PomodoroTask t : all) {
            String key = t.getTag() == null || t.getTag().isEmpty() ? "未分类" : t.getTag();
            m.computeIfAbsent(key, k -> new int[]{0, 0});
            m.get(key)[0]++;
            if (t.getStatus() != null && t.getStatus() == 1) m.get(key)[1]++;
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<String, int[]> e : m.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("tag", e.getKey());
            r.put("total", e.getValue()[0]);
            r.put("done", e.getValue()[1]);
            result.add(r);
        }
        return result;
    }

    private PomodoroTask requireOwnTask(Long userId, Long taskId) {
        PomodoroTask t = taskMapper.selectById(taskId);
        if (t == null || !t.getUserId().equals(userId)) {
            throw new BusinessException("任务不存在");
        }
        return t;
    }
}
