package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 气泡提示库(Iter-05)
 * 按种类/性格/性别/阶段组合,多样化气泡文本
 */
@Data
@TableName("bubble_prompt")
public class BubblePrompt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宠物种类(留空=通用) */
    private String species;

    /** 性格(留空=通用) */
    private String personality;

    /** 性别(留空=通用) */
    private String gender;

    /** 成长阶段(留空=通用) */
    private String stage;

    /** 类别:greeting / sad / happy / hungry / sleepy / study / play / random */
    private String category;

    /** 气泡文本 */
    private String content;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    private LocalDateTime createdAt;
}
