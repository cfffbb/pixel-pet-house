package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物档案(一只宠物一行)
 */
@Data
@TableName("pet")
public class Pet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 种类 pet_type.id */
    private Long petTypeId;

    /** 宠物昵称(强制起名) */
    private String petName;

    /** 亚种名(仅展示) */
    private String subtypeName;

    /** 性别:MALE 公 / FEMALE 母(50/50 随机) */
    private String gender;

    /** 性格:活泼/高冷/粘人/贪吃/傲娇 */
    private String personality;

    /** 等级 */
    private Integer level;

    /** 经验 */
    private Integer exp;

    /** 饥饿度(0–100,数值规则 M3/M7 拍板) */
    private Integer hunger;

    /** 心情值(0–100) */
    private Integer mood;

    /** 游戏币(兑换规则 M7 拍板) */
    private Integer coins;

    /** 状态:ALIVE 活着 / DANGER 病危 / STARVED 已死 */
    private String status;

    /** 1 已上架配种市场(需雄/活着/成熟3天/非冷却,M12) */
    private Integer listed;

    /** 0 普通宠物 / 1 系统NPC（单机配种候选） */
    private Integer isNpc;

    /** 出生/孵化时间 */
    private LocalDateTime hatchedAt;

    /** 上次喂食时间 */
    private LocalDateTime lastFedAt;

    /** 上次结算饥饿的时间(懒计算用) */
    private LocalDateTime hungerCalcAt;

    /** 进入病危的时间(24 小时内喂食可救) */
    private LocalDateTime dangerSince;

    /** 怀孕截止时间(到期产崽) */
    private LocalDateTime pregnantUntil;

    /** 配种冷却截止时间 */
    private LocalDateTime breedingCoolUntil;

    /** 连续喂食天数 */
    private Integer feedingStreak;

    /** 上次喂食日期(YYYY-MM-DD,用于连续天数判断) */
    private String lastFeedDate;

    /** 玩耍日期(每日重置) */
    private LocalDate playDate;

    /** 今日玩耍次数 */
    private Integer playCount;

    /** 行为事件发生日期(每天最多 1 次) */
    private LocalDate behaviorDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
