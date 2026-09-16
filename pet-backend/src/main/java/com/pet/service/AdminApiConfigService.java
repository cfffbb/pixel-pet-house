package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.entity.AdminApiConfig;
import com.pet.entity.UserApiKey;
import com.pet.entity.UserVoicePref;
import com.pet.mapper.AdminApiConfigMapper;
import com.pet.mapper.UserApiKeyMapper;
import com.pet.mapper.UserVoicePrefMapper;
import com.pet.security.ApiKeyCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 管理员 API 配置服务(Iter-05)
 * 管理员可配置三类 API(text/voice/image),统一应用到所有用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApiConfigService {

    private final AdminApiConfigMapper adminApiConfigMapper;
    private final UserVoicePrefMapper userVoicePrefMapper;
    private final UserApiKeyMapper userApiKeyMapper;
    private final ApiKeyCipher apiKeyCipher;

    /** 获取所有管理员 API 配置(列表,Key 脱敏) */
    public List<Map<String, Object>> listAll() {
        List<AdminApiConfig> configs = adminApiConfigMapper.selectList(
                new LambdaQueryWrapper<AdminApiConfig>().orderByAsc(AdminApiConfig::getApiType));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminApiConfig c : configs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("apiType", c.getApiType());
            m.put("provider", c.getProvider());
            m.put("baseUrl", c.getBaseUrl());
            m.put("modelName", c.getModelName());
            m.put("voice", c.getVoice());
            m.put("enabled", c.getEnabled());
            m.put("remark", c.getRemark());
            m.put("keyMasked", mask(apiKeyCipher.decrypt(c.getApiKeyEnc())));
            // 讯飞专用字段
            if ("xfyun".equals(c.getProvider())) {
                m.put("appId", c.getAppId());
                m.put("hasApiSecret", StrUtil.isNotBlank(c.getApiSecretEnc()));
            }
            result.add(m);
        }
        return result;
    }

    /** 保存/更新管理员 API 配置
     *  - 传了 id → 按 id 更新
     *  - 没传 id → 新增一条(支持同类型多条)
     */
    public void save(Map<String, Object> body) {
        Object idObj = body.get("id");
        String apiType = (String) body.get("apiType");
        String provider = (String) body.get("provider");
        String apiKey = (String) body.get("apiKey");
        String apiSecret = (String) body.get("apiSecret");
        String appId = (String) body.get("appId");
        if (StrUtil.isBlank(apiType) || StrUtil.isBlank(provider)) {
            throw new BusinessException("API 类型和服务商不能为空");
        }

        AdminApiConfig config = null;
        boolean isNew;
        if (idObj != null) {
            // 按 id 查找并更新
            Long id = Long.valueOf(idObj.toString());
            config = adminApiConfigMapper.selectById(id);
            if (config == null) throw new BusinessException("配置不存在");
            isNew = false;
        } else {
            // 新增
            config = new AdminApiConfig();
            config.setApiType(apiType);
            isNew = true;
        }
        config.setProvider(provider);
        config.setBaseUrl((String) body.get("baseUrl"));
        config.setModelName((String) body.get("modelName"));
        config.setVoice((String) body.get("voice"));
        config.setEnabled(body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : 1);
        config.setRemark((String) body.get("remark"));

        // API Key
        if (StrUtil.isNotBlank(apiKey)) {
            config.setApiKeyEnc(apiKeyCipher.encrypt(apiKey));
        } else if (isNew && !"xfyun".equals(provider)) {
            throw new BusinessException("首次配置必须填写 API Key");
        }

        // 讯飞专用:AppID 和 APISecret
        if ("xfyun".equals(provider)) {
            if (StrUtil.isNotBlank(appId)) {
                config.setAppId(appId);
            } else if (isNew) {
                throw new BusinessException("讯飞配置必须填写 AppID");
            }
            if (StrUtil.isNotBlank(apiSecret)) {
                config.setApiSecretEnc(apiKeyCipher.encrypt(apiSecret));
            } else if (isNew) {
                throw new BusinessException("讯飞配置必须填写 APISecret");
            }
            if (isNew && StrUtil.isBlank(apiKey)) {
                throw new BusinessException("讯飞配置必须填写 APIKey");
            }
        }

        if (isNew) {
            adminApiConfigMapper.insert(config);
        } else {
            adminApiConfigMapper.updateById(config);
        }
    }

    /** 删除管理员 API 配置 */
    public void delete(Long id) {
        adminApiConfigMapper.deleteById(id);
    }

    /** 获取指定类型的启用配置(内部调用,返回解密后的 Key) */
    public AdminApiConfig getEnabled(String apiType) {
        return adminApiConfigMapper.selectOne(
                new LambdaQueryWrapper<AdminApiConfig>()
                        .eq(AdminApiConfig::getApiType, apiType)
                        .eq(AdminApiConfig::getEnabled, 1)
                        .last("LIMIT 1"));
    }

    /**
     * 解密管理员 API Key
     */
    public String decryptKey(AdminApiConfig config) {
        return apiKeyCipher.decrypt(config.getApiKeyEnc());
    }

    /**
     * 获取用户可用的文本对话 API 配置:
     * 1. 用户选择"用管理员配置"→ 返回管理员的 text 配置
     * 2. 用户选择"自行配置"→ 返回用户自己的 UserApiKey
     */
    public ApiEndpoint resolveTextApi(Long userId) {
        UserVoicePref pref = getUserPref(userId);
        if (pref != null && pref.getUseAdminKey() == 1) {
            AdminApiConfig config = getEnabled("text");
            if (config != null) {
                return new ApiEndpoint(config.getProvider(), config.getBaseUrl(),
                        decryptKey(config), config.getModelName());
            }
        }
        // 回退到用户自己的 Key
        UserApiKey key = userApiKeyMapper.selectOne(
                new LambdaQueryWrapper<UserApiKey>()
                        .eq(UserApiKey::getUserId, userId)
                        .eq(UserApiKey::getEnabled, 1)
                        .orderByAsc(UserApiKey::getId)
                        .last("LIMIT 1"));
        if (key == null) {
            // 尝试管理员配置作为最终兜底
            AdminApiConfig config = getEnabled("text");
            if (config != null) {
                return new ApiEndpoint(config.getProvider(), config.getBaseUrl(),
                        decryptKey(config), config.getModelName());
            }
            throw new BusinessException("还没有配置 AI API Key,请到「设置」里添加,或联系管理员配置全局 API");
        }
        return new ApiEndpoint(key.getProvider(), key.getBaseUrl(),
                apiKeyCipher.decrypt(key.getKeyEncrypted()), key.getModelName());
    }

    /**
     * 获取用户可用的 TTS API 配置
     */
    public ApiEndpoint resolveTtsApi(Long userId) {
        UserVoicePref pref = getUserPref(userId);
        if (pref != null && pref.getUseAdminKey() == 1) {
            AdminApiConfig config = getEnabled("voice");
            if (config != null) {
                return new ApiEndpoint(config.getProvider(), config.getBaseUrl(),
                        decryptKey(config), config.getModelName(), config.getVoice());
            }
        }
        // 回退到用户的 openai/siliconflow key
        List<UserApiKey> keys = userApiKeyMapper.selectList(
                new LambdaQueryWrapper<UserApiKey>()
                        .eq(UserApiKey::getUserId, userId)
                        .eq(UserApiKey::getEnabled, 1)
                        .orderByAsc(UserApiKey::getId));
        for (UserApiKey k : keys) {
            if ("openai".equals(k.getProvider()) || "siliconflow".equals(k.getProvider())) {
                return new ApiEndpoint(k.getProvider(), k.getBaseUrl(),
                        apiKeyCipher.decrypt(k.getKeyEncrypted()), "tts-1",
                        pref != null ? pref.getVoice() : "alloy");
            }
        }
        // 尝试管理员配置
        AdminApiConfig config = getEnabled("voice");
        if (config != null) {
            return new ApiEndpoint(config.getProvider(), config.getBaseUrl(),
                    decryptKey(config), config.getModelName(), config.getVoice());
        }
        throw new BusinessException("未配置语音 TTS API,请联系管理员或自行配置 OpenAI/硅基流动 Key");
    }

    /**
     * 获取用户可用的 STT(语音转写) API 配置
     * 优先管理员独立 stt 配置,回退到 text 配置,再回退到用户 Key
     */
    public ApiEndpoint resolveSttApi(Long userId) {
        // 1. 优先管理员独立 stt 配置
        AdminApiConfig sttConfig = getEnabled("stt");
        if (sttConfig != null) {
            return new ApiEndpoint(sttConfig.getProvider(), sttConfig.getBaseUrl(),
                    decryptKey(sttConfig), StrUtil.blankToDefault(sttConfig.getModelName(), "whisper-1"));
        }
        // 2. 回退到管理员 text 配置(仅 openai/siliconflow 支持 whisper)
        AdminApiConfig textConfig = getEnabled("text");
        if (textConfig != null && ("openai".equals(textConfig.getProvider()) || "siliconflow".equals(textConfig.getProvider()))) {
            return new ApiEndpoint(textConfig.getProvider(), textConfig.getBaseUrl(),
                    decryptKey(textConfig), "whisper-1");
        }
        // 3. 回退到用户自己的 Key
        List<UserApiKey> keys = userApiKeyMapper.selectList(
                new LambdaQueryWrapper<UserApiKey>()
                        .eq(UserApiKey::getUserId, userId)
                        .eq(UserApiKey::getEnabled, 1)
                        .orderByAsc(UserApiKey::getId));
        for (UserApiKey k : keys) {
            if ("openai".equals(k.getProvider()) || "siliconflow".equals(k.getProvider())) {
                return new ApiEndpoint(k.getProvider(), k.getBaseUrl(),
                        apiKeyCipher.decrypt(k.getKeyEncrypted()), "whisper-1");
            }
        }
        throw new BusinessException("未配置语音转写 STT API,请联系管理员配置 stt 类型 API,或自行配置 OpenAI/硅基流动 Key");
    }

    /** 获取用户语音偏好 */
    public UserVoicePref getUserPref(Long userId) {
        return userVoicePrefMapper.selectOne(
                new LambdaQueryWrapper<UserVoicePref>().eq(UserVoicePref::getUserId, userId).last("LIMIT 1"));
    }

    /** 保存用户语音偏好 */
    public void saveUserPref(Long userId, Map<String, Object> body) {
        UserVoicePref pref = getUserPref(userId);
        boolean isNew = pref == null;
        if (isNew) {
            pref = new UserVoicePref();
            pref.setUserId(userId);
            pref.setUseAdminKey(1);
            pref.setTtsEnabled(0);
            pref.setVoice("alloy");
            pref.setTtsSpeed(new BigDecimal("1.0"));
            pref.setBubbleRead(0);
        }
        if (body.containsKey("useAdminKey")) {
            pref.setUseAdminKey(((Number) body.get("useAdminKey")).intValue());
        }
        if (body.containsKey("ttsEnabled")) {
            pref.setTtsEnabled(((Number) body.get("ttsEnabled")).intValue());
        }
        if (body.containsKey("voice")) {
            pref.setVoice((String) body.get("voice"));
        }
        if (body.containsKey("ttsSpeed")) {
            pref.setTtsSpeed(new BigDecimal(body.get("ttsSpeed").toString()));
        }
        if (body.containsKey("bubbleRead")) {
            pref.setBubbleRead(((Number) body.get("bubbleRead")).intValue());
        }
        if (isNew) {
            userVoicePrefMapper.insert(pref);
        } else {
            userVoicePrefMapper.updateById(pref);
        }
    }

    /** 获取可用 TTS 音色列表 */
    public List<String> availableVoices() {
        return List.of("alloy", "echo", "fable", "onyx", "nova", "shimmer");
    }

    private String mask(String key) {
        if (StrUtil.isBlank(key) || key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /** API 端点信息(内部传输对象) */
    public record ApiEndpoint(String provider, String baseUrl, String apiKey, String modelName, String voice) {
        public ApiEndpoint(String provider, String baseUrl, String apiKey, String modelName) {
            this(provider, baseUrl, apiKey, modelName, null);
        }
    }
}
