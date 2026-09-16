package com.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日程新增/修改请求
 */
@Data
public class ScheduleDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    /** 提醒时间(到点右下角推送) */
    private LocalDateTime remindAt;

    /** 重复:ONCE 一次 / DAILY 每天 / WEEKLY 每周 */
    private String repeatType;

    /** 优先级:1 紧急 / 2 高 / 3 中 / 4 低 */
    @Min(value = 1, message = "优先级最低为 1")
    @Max(value = 4, message = "优先级最高为 4")
    private Integer priority;

    /** 标签(学习/工作/生活/健康/其他,可自定义) */
    private String tag;
}
