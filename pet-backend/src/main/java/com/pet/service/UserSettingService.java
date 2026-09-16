package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.entity.UserSetting;
import com.pet.mapper.UserSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户设置业务(当前:配种授权开关)
 */
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingMapper userSettingMapper;

    /** 读全部设置(配种授权默认开;应用感知默认关,M9 隐私红线) */
    public Map<String, Object> get(Long userId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allowBreeding", true);
        m.put("appMonitorEnabled", false);
        List<UserSetting> list = userSettingMapper.selectList(new LambdaQueryWrapper<UserSetting>()
                .eq(UserSetting::getUserId, userId));
        for (UserSetting s : list) {
            if (PetConstants.SETTING_ALLOW_BREEDING.equals(s.getSettingKey())) {
                m.put("allowBreeding", !"false".equalsIgnoreCase(s.getSettingValue()));
            }
            if (PetConstants.SETTING_APP_MONITOR.equals(s.getSettingKey())) {
                m.put("appMonitorEnabled", !"false".equalsIgnoreCase(s.getSettingValue()));
            }
        }
        return m;
    }

    public void setAllowBreeding(Long userId, boolean value) {
        UserSetting s = setting(userId, PetConstants.SETTING_ALLOW_BREEDING);
        if (s == null) {
            s = new UserSetting();
            s.setUserId(userId);
            s.setSettingKey(PetConstants.SETTING_ALLOW_BREEDING);
            s.setSettingValue(String.valueOf(value));
            userSettingMapper.insert(s);
        } else {
            s.setSettingValue(String.valueOf(value));
            userSettingMapper.updateById(s);
        }
    }

    public void setAppMonitor(Long userId, boolean value) {
        UserSetting s = setting(userId, PetConstants.SETTING_APP_MONITOR);
        if (s == null) {
            s = new UserSetting();
            s.setUserId(userId);
            s.setSettingKey(PetConstants.SETTING_APP_MONITOR);
            s.setSettingValue(String.valueOf(value));
            userSettingMapper.insert(s);
        } else {
            s.setSettingValue(String.valueOf(value));
            userSettingMapper.updateById(s);
        }
    }

    private UserSetting setting(Long userId, String key) {
        return userSettingMapper.selectOne(new LambdaQueryWrapper<UserSetting>()
                .eq(UserSetting::getUserId, userId)
                .eq(UserSetting::getSettingKey, key)
                .last("LIMIT 1"));
    }
}
