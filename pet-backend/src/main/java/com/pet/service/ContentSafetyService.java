package com.pet.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容安全防护(Iter-05)
 * 对 AI 输出做敏感词过滤
 */
@Service
public class ContentSafetyService {

    private static final List<String> SENSITIVE_WORDS = List.of(
            "自杀", "自残", "毒品", "赌博", "色情", "暴力", "炸弹", "枪支",
            "fuck", "shit", "damn", "asshole"
    );

    private static final String REPLACE = "***";

    /**
     * 过滤敏感词,返回安全文本
     */
    public String filter(String text) {
        if (StrUtil.isBlank(text)) return text;
        String result = text;
        for (String word : SENSITIVE_WORDS) {
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(word), REPLACE);
        }
        return result;
    }

    /**
     * 检查是否包含敏感词(返回 true 表示有敏感内容)
     */
    public boolean hasSensitive(String text) {
        if (StrUtil.isBlank(text)) return false;
        String lower = text.toLowerCase();
        return SENSITIVE_WORDS.stream().anyMatch(w -> lower.contains(w.toLowerCase()));
    }
}
