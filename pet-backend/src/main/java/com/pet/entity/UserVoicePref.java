package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户语音偏好(Iter-05)
 */
@Data
@TableName("user_voice_pref")
public class UserVoicePref {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 1=用管理员配置的API 0=用自己的Key */
    private Integer useAdminKey;

    /** 是否开启语音朗读 */
    private Integer ttsEnabled;

    /** TTS 音色 */
    private String voice;

    /** 语速 0.5-2.0 */
    private BigDecimal ttsSpeed;

    /** 气泡是否朗读 */
    private Integer bubbleRead;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
