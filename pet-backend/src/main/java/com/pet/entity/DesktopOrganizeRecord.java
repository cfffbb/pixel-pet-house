package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 桌面整理流水(每次整理留痕)
 */
@Data
@TableName("desktop_organize_record")
public class DesktopOrganizeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 分区 desktop_zone.id */
    private Long zoneId;

    /** 本次移动文件数 */
    private Integer movedCount;

    /** 移动明细 JSON:[{fileName,from,to}] */
    private String detailsJson;

    /** 1 已撤销 */
    private Integer undone;

    /** 整理时间 */
    private LocalDateTime organizedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
