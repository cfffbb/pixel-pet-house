package com.pet.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ScheduleDTO;
import com.pet.entity.InboxMessage;
import com.pet.entity.Pet;
import com.pet.entity.PomodoroRecord;
import com.pet.entity.Schedule;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PomodoroRecordMapper;
import com.pet.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日程业务:M5(增删改查 / 完成 / 提醒去重 / 标签 / 统计 / 工资单)
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final List<String> PRESET_TAGS = List.of("学习", "工作", "生活", "健康", "其他");

    private final ScheduleMapper scheduleMapper;
    private final PomodoroRecordMapper pomodoroRecordMapper;
    private final InboxMessageMapper inboxMessageMapper;
    private final PetMapper petMapper;
    private final PetService petService;

    public List<Schedule> list(Long userId) {
        return scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .orderByAsc(Schedule::getRemindAt));
    }

    public void add(Long userId, ScheduleDTO dto) {
        Schedule s = new Schedule();
        s.setUserId(userId);
        fill(s, dto);
        scheduleMapper.insert(s);
    }

    public void update(Long userId, Long id, ScheduleDTO dto) {
        Schedule s = requireOwn(userId, id);
        fill(s, dto);
        scheduleMapper.updateById(s);
    }

    private void fill(Schedule s, ScheduleDTO dto) {
        s.setTitle(dto.getTitle().trim());
        s.setDescription(dto.getDescription());
        s.setRemindAt(dto.getRemindAt());
        s.setRepeatType(StrUtil.isBlank(dto.getRepeatType()) ? "ONCE" : dto.getRepeatType());
        s.setPriority(dto.getPriority() == null ? 3 : dto.getPriority());
        s.setTag(StrUtil.isBlank(dto.getTag()) ? "其他" : dto.getTag().trim());
    }

    public void delete(Long userId, Long id) {
        requireOwn(userId, id);
        scheduleMapper.deleteById(id);
    }

    /** 完成:标记 DONE + 记录完成时间 + 随机奖励 1–5 币(M7 拍板);宠物"亲亲"由前端表现 */
    public Map<String, Object> done(Long userId, Long id) {
        Schedule s = requireOwn(userId, id);
        if ("DONE".equals(s.getStatus())) {
            throw new BusinessException("这条日程已经完成啦");
        }
        s.setStatus("DONE");
        s.setCompletedAt(LocalDateTime.now());
        scheduleMapper.updateById(s);

        int reward = RandomUtil.randomInt(1, 6);
        Pet pet = petService.getMyPet(userId);
        if (pet != null) {
            pet.setCoins(pet.getCoins() + reward);
            petMapper.updateById(pet);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kiss", true);
        data.put("reward", reward);
        data.put("message", "❤️ 宠物给了你一个亲亲,完成奖励 " + reward + " 币");
        return data;
    }

    /** 取消完成(恢复为待办) */
    public void undone(Long userId, Long id) {
        Schedule s = requireOwn(userId, id);
        if (!"DONE".equals(s.getStatus())) {
            throw new BusinessException("这条日程还未完成");
        }
        s.setStatus("PENDING");
        s.setCompletedAt(null);
        scheduleMapper.updateById(s);
    }

    /** 我的标签(预设 + 用过的) */
    public List<String> tags(Long userId) {
        Set<String> set = new LinkedHashSet<>(PRESET_TAGS);
        List<Schedule> mine = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .isNotNull(Schedule::getTag));
        for (Schedule s : mine) {
            if (StrUtil.isNotBlank(s.getTag())) {
                set.add(s.getTag());
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 到期提醒(M5 拍板:ONCE 一次 / DAILY 每天 / WEEKLY 每周,各自去重)。
     * 返回即视为已提醒(写 last_notified_at);10% 卖萌替代由客户端表现。
     */
    public List<Map<String, Object>> dueReminders(Long userId) {
        List<Schedule> due = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .eq(Schedule::getStatus, "PENDING")
                .le(Schedule::getRemindAt, LocalDateTime.now())
                .orderByAsc(Schedule::getRemindAt));
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Schedule s : due) {
            boolean shouldNotify;
            if ("DAILY".equals(s.getRepeatType())) {
                shouldNotify = s.getLastNotifiedAt() == null || !s.getLastNotifiedAt().toLocalDate().equals(today);
            } else if ("WEEKLY".equals(s.getRepeatType())) {
                shouldNotify = s.getLastNotifiedAt() == null
                        || s.getLastNotifiedAt().toLocalDate().isBefore(today.minusDays(6));
            } else {
                // ONCE: 首次通知后,30 分钟内不重复(防止轮询反复弹窗);超 30 分钟仍 PENDING 则再次提醒
                shouldNotify = s.getLastNotifiedAt() == null
                        || s.getLastNotifiedAt().isBefore(LocalDateTime.now().minusMinutes(30));
            }
            if (shouldNotify) {
                s.setLastNotifiedAt(LocalDateTime.now());
                scheduleMapper.updateById(s);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("title", s.getTitle());
                m.put("description", s.getDescription());
                m.put("remindAt", s.getRemindAt());
                m.put("priority", s.getPriority());
                m.put("tag", s.getTag());
                result.add(m);
            }
        }
        return result;
    }

    /**
     * 统计:汇总 + 标签饼图 + 番茄钟名称饼图(按 label 分组) + 近 7 天趋势 + 启动时间分布
     * 支持日期范围查询(from/to 为 null 时默认最近 30 天)
     */
    public Map<String, Object> stats(Long userId, String from, String to) {
        LocalDate endDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : LocalDate.now();
        LocalDate startDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : endDate.minusDays(29);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Schedule> done = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .eq(Schedule::getStatus, "DONE")
                .isNotNull(Schedule::getCompletedAt)
                .ge(Schedule::getCompletedAt, startDateTime)
                .lt(Schedule::getCompletedAt, endDateTime));
        List<PomodoroRecord> pomos = pomodoroRecordMapper.selectList(new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getUserId, userId)
                .eq(PomodoroRecord::getCompleted, 1)
                .ge(PomodoroRecord::getStartedAt, startDateTime)
                .lt(PomodoroRecord::getStartedAt, endDateTime));

        // 标签完成数饼图
        Map<String, Integer> tagCount = new LinkedHashMap<>();
        int[] hourBuckets = new int[24];
        for (Schedule s : done) {
            String tag = StrUtil.isBlank(s.getTag()) ? "其他" : s.getTag();
            tagCount.merge(tag, 1, Integer::sum);
        }
        List<Map<String, Object>> pie = new ArrayList<>();
        for (Map.Entry<String, Integer> e : tagCount.entrySet()) {
            pie.add(Map.of("name", e.getKey(), "value", e.getValue()));
        }

        // 番茄钟名称饼图(按 label 分组,统计专注分钟数)
        Map<String, Integer> labelMinutes = new LinkedHashMap<>();
        int totalMinutes = 0;
        int totalCoins = 0;
        for (PomodoroRecord p : pomos) {
            int mins = p.getDurationMinutes() == null ? 0 : p.getDurationMinutes();
            totalMinutes += mins;
            totalCoins += p.getCoinsEarned() == null ? 0 : p.getCoinsEarned();
            String label = StrUtil.isBlank(p.getLabel()) ? "未命名" : p.getLabel();
            labelMinutes.merge(label, mins, Integer::sum);
            if (p.getStartedAt() != null) {
                hourBuckets[p.getStartedAt().getHour()]++;
            }
        }
        List<Map<String, Object>> pomoPie = new ArrayList<>();
        for (Map.Entry<String, Integer> e : labelMinutes.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            pomoPie.add(m);
        }

        int sessions = pomos.size();
        double avg = sessions > 0 ? Math.round(totalMinutes * 10.0 / sessions) / 10.0 : 0;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDone", done.size());
        summary.put("totalSessions", sessions);
        summary.put("totalMinutes", totalMinutes);
        summary.put("totalCoins", totalCoins);
        summary.put("avgMinutes", avg);

        // 近 7 天趋势(以 endDate 为终点)
        List<Map<String, Object>> last7 = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = endDate.minusDays(i);
            long c = done.stream().filter(s -> s.getCompletedAt() != null
                    && s.getCompletedAt().toLocalDate().equals(d)).count();
            int minutes = pomos.stream()
                    .filter(p -> p.getStartedAt() != null && p.getStartedAt().toLocalDate().equals(d))
                    .mapToInt(p -> p.getDurationMinutes() == null ? 0 : p.getDurationMinutes())
                    .sum();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("count", c);
            item.put("minutes", minutes);
            last7.add(item);
        }

        List<Map<String, Object>> hourDist = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourDist.add(Map.of("hour", h, "count", hourBuckets[h]));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("pieByTag", pie);
        data.put("pomodoroPie", pomoPie);
        data.put("pomodoroTotalMinutes", totalMinutes);
        data.put("last7Days", last7);
        data.put("startHourDist", hourDist);
        data.put("from", startDate.toString());
        data.put("to", endDate.toString());
        return data;
    }

    /** 每晚 00:00:给当天有活动的用户发"今日工资单"到收件箱 */
    public void sendDailyWage() {
        LocalDate day = LocalDate.now();
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        sendWage(start, end, "今日工资单");
    }

    /** 每月 1 号 00:00:汇总上月工资单 */
    public void sendMonthlySummary() {
        YearMonth last = YearMonth.now().minusMonths(1);
        LocalDateTime start = last.atDay(1).atStartOfDay();
        LocalDateTime end = last.plusMonths(1).atDay(1).atStartOfDay();
        sendWage(start, end, "上月汇总(" + last + ")");
    }

    private void sendWage(LocalDateTime start, LocalDateTime end, String title) {
        // 有完成日程的用户
        List<Schedule> done = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getStatus, "DONE")
                .ge(Schedule::getCompletedAt, start)
                .lt(Schedule::getCompletedAt, end));
        Set<Long> userIds = new LinkedHashSet<>();
        for (Schedule s : done) {
            userIds.add(s.getUserId());
        }
        // 有完成番茄钟的用户
        List<PomodoroRecord> pomos = pomodoroRecordMapper.selectList(new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getCompleted, 1)
                .ge(PomodoroRecord::getEndedAt, start)
                .lt(PomodoroRecord::getEndedAt, end));
        for (PomodoroRecord p : pomos) {
            userIds.add(p.getUserId());
        }
        for (Long userId : userIds) {
            long scheduleCount = done.stream().filter(s -> s.getUserId().equals(userId)).count();
            long pomoCount = pomos.stream().filter(p -> p.getUserId().equals(userId)).count();
            int coins = pomos.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .mapToInt(p -> p.getCoinsEarned() == null ? 0 : p.getCoinsEarned())
                    .sum();
            String content = title + ":完成日程 " + scheduleCount + " 个,完成番茄钟 " + pomoCount
                    + " 个,获得游戏币 " + coins + " 币(日程完成奖励 M7 上线后计入)。";
            InboxMessage msg = new InboxMessage();
            msg.setSenderId(0L); // 系统
            msg.setReceiverId(userId);
            msg.setType("REPORT");
            msg.setContent(content);
            msg.setStatus("UNREAD");
            inboxMessageMapper.insert(msg);
        }
    }

    private Schedule requireOwn(Long userId, Long id) {
        Schedule s = scheduleMapper.selectById(id);
        if (s == null || !s.getUserId().equals(userId)) {
            throw new BusinessException("日程不存在");
        }
        return s;
    }
}
