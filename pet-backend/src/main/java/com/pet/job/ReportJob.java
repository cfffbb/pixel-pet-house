package com.pet.job;

import com.pet.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务(M5 拍板):
 * 每晚 00:00 发当日工资单到收件箱;每月 1 号 00:00 汇总上月。
 */
@Component
@RequiredArgsConstructor
public class ReportJob {

    private final ScheduleService scheduleService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyWage() {
        scheduleService.sendDailyWage();
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void monthlySummary() {
        scheduleService.sendMonthlySummary();
    }
}
