package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.entity.AppUsageLog;
import com.pet.mapper.AppUsageLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 应用采样记录业务(M9;记录保留 7 天,每天凌晨清理)
 */
@Service
@RequiredArgsConstructor
public class AppUsageService {

    private static final int RETENTION_DAYS = 7;

    private final AppUsageLogMapper appUsageLogMapper;

    public void log(Long userId, String appName, String windowTitle) {
        AppUsageLog l = new AppUsageLog();
        l.setUserId(userId);
        l.setAppName(appName);
        l.setWindowTitle(windowTitle);
        l.setSampledAt(LocalDateTime.now());
        appUsageLogMapper.insert(l);
    }

    /** 每天 00:10 清理 7 天前的记录(M9 拍板) */
    @Scheduled(cron = "0 10 0 * * ?")
    public void cleanup() {
        appUsageLogMapper.delete(new LambdaQueryWrapper<AppUsageLog>()
                .lt(AppUsageLog::getSampledAt, LocalDateTime.now().minusDays(RETENTION_DAYS)));
    }
}
