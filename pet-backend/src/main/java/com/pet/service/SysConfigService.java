package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.entity.SysConfig;
import com.pet.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统配置(管理员维护)
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    public static final String KEY_CHAT_MEMORY = "chat_memory";
    private static final int DEFAULT_CHAT_MEMORY = 10;

    private final SysConfigMapper configMapper;

    /** 对话记忆条数(默认 10,管理员可改) */
    public int chatMemory() {
        SysConfig c = get(KEY_CHAT_MEMORY);
        if (c == null || !StrUtil.isNumeric(c.getCfgValue())) {
            return DEFAULT_CHAT_MEMORY;
        }
        return Integer.parseInt(c.getCfgValue());
    }

    public void setChatMemory(int value) {
        if (value < 1 || value > 100) {
            throw new com.pet.common.exception.BusinessException("记忆条数需在 1–100 之间");
        }
        upsert(KEY_CHAT_MEMORY, String.valueOf(value));
    }

    private SysConfig get(String key) {
        return configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCfgKey, key).last("LIMIT 1"));
    }

    private void upsert(String key, String value) {
        SysConfig c = get(key);
        if (c == null) {
            c = new SysConfig();
            c.setCfgKey(key);
            c.setCfgValue(value);
            configMapper.insert(c);
        } else {
            c.setCfgValue(value);
            configMapper.updateById(c);
        }
    }
}
