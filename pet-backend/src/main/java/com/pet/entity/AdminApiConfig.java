package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员 API 配置(Iter-05)
 * 三类 API:text(文本对话) / voice(语音TTS) / image(图片识别)
 */
@Data
@TableName("admin_api_config")
public class AdminApiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** API 类型:text / voice / image */
    private String apiType;

    /** 服务商标识:openai / qwen / siliconflow / zhipu / custom */
    private String provider;

    /** 接口地址 */
    private String baseUrl;

    /** 加密后的 API Key */
    private String apiKeyEnc;

    /** 讯飞 AppID */
    private String appId;

    /** 加密后的 APISecret(讯飞) */
    private String apiSecretEnc;

    /** 模型名 */
    private String modelName;

    /** TTS 音色(仅 voice 类型) */
    private String voice;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
