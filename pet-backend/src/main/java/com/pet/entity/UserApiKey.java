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
 * 用户自填的 AI API Key(多端,加密存储)
 */
@Data
@TableName("user_api_key")
public class UserApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 sys_user.id */
    private Long userId;

    /** 服务商标识,如 openai / qwen / zhipu */
    private String provider;

    /** 加密后的 Key,明文绝不落库 */
    private String keyEncrypted;

    /** 可选:自定义接口地址 */
    private String baseUrl;

    /** 对话模型名,如 gpt-4o-mini / qwen-plus */
    private String modelName;

    /** 备注 */
    private String remark;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
