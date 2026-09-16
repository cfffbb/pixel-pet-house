package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ApiKeyDTO;
import com.pet.entity.UserApiKey;
import com.pet.mapper.UserApiKeyMapper;
import com.pet.security.ApiKeyCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户 API Key 业务:增删改查,加密存储,返回一律脱敏
 */
@Service
@RequiredArgsConstructor
public class UserApiKeyService {

    private final UserApiKeyMapper userApiKeyMapper;
    private final ApiKeyCipher apiKeyCipher;

    public List<Map<String, Object>> listMine(Long userId) {
        List<UserApiKey> keys = userApiKeyMapper.selectList(
                new LambdaQueryWrapper<UserApiKey>()
                        .eq(UserApiKey::getUserId, userId)
                        .orderByDesc(UserApiKey::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserApiKey k : keys) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("provider", k.getProvider());
            item.put("baseUrl", k.getBaseUrl());
            item.put("modelName", k.getModelName());
            item.put("remark", k.getRemark());
            item.put("enabled", k.getEnabled());
            item.put("keyMasked", mask(apiKeyCipher.decrypt(k.getKeyEncrypted())));
            item.put("createdAt", k.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    public void add(Long userId, ApiKeyDTO dto) {
        // M2 拍板:每个服务商最多 1 个 Key
        Long exist = userApiKeyMapper.selectCount(
                new LambdaQueryWrapper<UserApiKey>()
                        .eq(UserApiKey::getUserId, userId)
                        .eq(UserApiKey::getProvider, dto.getProvider()));
        if (exist > 0) {
            throw new BusinessException("该服务商已配置 Key,请直接修改");
        }
        if (StrUtil.isBlank(dto.getKey())) {
            throw new BusinessException("Key 不能为空");
        }
        UserApiKey k = new UserApiKey();
        k.setUserId(userId);
        k.setProvider(dto.getProvider());
        k.setKeyEncrypted(apiKeyCipher.encrypt(dto.getKey()));
        k.setBaseUrl(dto.getBaseUrl());
        k.setModelName(dto.getModelName());
        k.setRemark(dto.getRemark());
        k.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        userApiKeyMapper.insert(k);
    }

    public void update(Long userId, Long id, ApiKeyDTO dto) {
        UserApiKey k = getOwn(userId, id);
        // Key 留空表示不更换
        if (StrUtil.isNotBlank(dto.getKey())) {
            k.setKeyEncrypted(apiKeyCipher.encrypt(dto.getKey()));
        }
        if (dto.getBaseUrl() != null) {
            k.setBaseUrl(dto.getBaseUrl());
        }
        if (dto.getModelName() != null) {
            k.setModelName(dto.getModelName());
        }
        if (dto.getRemark() != null) {
            k.setRemark(dto.getRemark());
        }
        if (dto.getEnabled() != null) {
            k.setEnabled(dto.getEnabled());
        }
        userApiKeyMapper.updateById(k);
    }

    public void delete(Long userId, Long id) {
        getOwn(userId, id);
        userApiKeyMapper.deleteById(id);
    }

    private UserApiKey getOwn(Long userId, Long id) {
        UserApiKey k = userApiKeyMapper.selectById(id);
        if (k == null || !k.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作该 Key");
        }
        return k;
    }

    /** 脱敏:只保留前 3 位和后 4 位,中间打星;解密失败也显示 **** */
    private String mask(String plain) {
        if (StrUtil.isBlank(plain) || plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
    }
}
