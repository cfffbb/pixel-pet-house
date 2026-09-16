package com.pet.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.entity.InboxMessage;
import com.pet.entity.Pet;
import com.pet.entity.PomodoroRecord;
import com.pet.entity.User;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PomodoroRecordMapper;
import com.pet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * 官方好友定时推送(Iter-07)
 * 每日 08:00 推送当日游戏币记录;每月 1 号 08:00 推送上月汇总。
 * 消息从官方好友(admin id=1)发出,显示在用户的社交列表「官方助手」对话中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfficialPushJob {

    private static final Long OFFICIAL_ID = 1L;

    private final UserMapper userMapper;
    private final PomodoroRecordMapper pomodoroRecordMapper;
    private final PetMapper petMapper;
    private final InboxMessageMapper inboxMessageMapper;

    /** 每日 08:00 推送当日游戏币记录 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyCoinReport() {
        log.info("[官方推送] 开始发送每日游戏币记录...");
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        sendCoinReport(start, end, "每日游戏币记录");
        log.info("[官方推送] 每日游戏币记录发送完成");
    }

    /** 每月 1 号 08:00 推送上月汇总 */
    @Scheduled(cron = "0 0 8 1 * ?")
    public void monthlyCoinReport() {
        log.info("[官方推送] 开始发送每月游戏币汇总...");
        YearMonth last = YearMonth.now().minusMonths(1);
        LocalDateTime start = last.atDay(1).atStartOfDay();
        LocalDateTime end = last.plusMonths(1).atDay(1).atStartOfDay();
        sendCoinReport(start, end, "上月游戏币汇总(" + last + ")");
        log.info("[官方推送] 每月游戏币汇总发送完成");
    }

    private void sendCoinReport(LocalDateTime start, LocalDateTime end, String title) {
        List<User> activeUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0)
                .ne(User::getId, OFFICIAL_ID));
        if (activeUsers.isEmpty()) return;

        for (User u : activeUsers) {
            List<PomodoroRecord> pomos = pomodoroRecordMapper.selectList(new LambdaQueryWrapper<PomodoroRecord>()
                    .eq(PomodoroRecord::getUserId, u.getId())
                    .eq(PomodoroRecord::getCompleted, 1)
                    .ge(PomodoroRecord::getEndedAt, start)
                    .lt(PomodoroRecord::getEndedAt, end));

            int pomoCount = pomos.size();
            int coinsEarned = pomos.stream()
                    .mapToInt(p -> p.getCoinsEarned() == null ? 0 : p.getCoinsEarned())
                    .sum();

            List<Pet> pets = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                    .eq(Pet::getUserId, u.getId())
                    .eq(Pet::getStatus, "ALIVE")
                    .orderByDesc(Pet::getCreatedAt)
                    .last("LIMIT 1"));
            int currentCoins = pets.isEmpty() ? 0 : (pets.get(0).getCoins() == null ? 0 : pets.get(0).getCoins());

            StringBuilder sb = new StringBuilder();
            sb.append("【").append(title).append("】\n");
            sb.append("玩家:").append(u.getNickname()).append("\n");
            sb.append("ID:").append(u.getPlayerId() != null ? u.getPlayerId() : "P" + String.format("%05d", u.getId())).append("\n");
            sb.append("━━━━━━━━━━\n");
            sb.append("🍅 完成番茄钟:").append(pomoCount).append(" 个\n");
            sb.append("💰 赚取游戏币:").append(coinsEarned).append(" 币\n");
            sb.append("🏦 当前余额:").append(currentCoins).append(" 币\n");
            if (pomoCount == 0) {
                sb.append("💡 今天还没有完成番茄钟,加油哦!\n");
            } else {
                sb.append("🎉 表现不错,继续努力!\n");
            }
            sb.append("━━━━━━━━━━\n");
            sb.append("感谢你的陪伴~");

            InboxMessage msg = new InboxMessage();
            msg.setSenderId(OFFICIAL_ID);
            msg.setReceiverId(u.getId());
            msg.setType("CHAT");
            msg.setContent(sb.toString());
            msg.setStatus("UNREAD");
            msg.setIsOfficial(1);
            inboxMessageMapper.insert(msg);
        }
    }
}
