package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 桌面整理分区规则(按类型分区,分区可自定义)
 */
@Data
@TableName("desktop_zone")
public class DesktopZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 分区名,如 文档 / 图片 / 视频 / 安装包 */
    private String zoneName;

    /** 匹配扩展名,逗号分隔,如 jpg,png,gif;空表示按文件名关键词 */
    private String extensions;

    /** 目标文件夹名(相对桌面) */
    private String targetDir;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
