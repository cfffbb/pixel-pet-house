package com.pet.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pet.common.exception.BusinessException;
import com.pet.service.AdminApiConfigService.ApiEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TTS 语音合成服务(Iter-05)
 * 使用 OpenAI 兼容的 /audio/speech 接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final AdminApiConfigService adminApiConfigService;

    /**
     * 文本转语音,返回 mp3 字节数组
     */
    public byte[] synthesize(Long userId, String text) {
        if (StrUtil.isBlank(text)) {
            throw new BusinessException("朗读内容不能为空");
        }
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }

        ApiEndpoint tts = adminApiConfigService.resolveTtsApi(userId);
        String url = StrUtil.blankToDefault(tts.baseUrl(), "https://api.openai.com/v1")
                .replaceAll("/+$", "") + "/audio/speech";

        JSONObject body = new JSONObject();
        body.set("model", StrUtil.blankToDefault(tts.modelName(), "tts-1"));
        body.set("input", text);
        body.set("voice", StrUtil.blankToDefault(tts.voice(), "alloy"));
        body.set("response_format", "mp3");

        try (HttpResponse resp = HttpRequest.post(url)
                .header("Authorization", "Bearer " + tts.apiKey())
                .header("Content-Type", "application/json")
                .timeout(30000)
                .body(body.toString())
                .execute()) {
            if (resp.getStatus() != 200) {
                log.error("TTS 调用失败: status={}, body={}", resp.getStatus(), resp.body());
                if (resp.getStatus() == 401 || resp.getStatus() == 403) {
                    throw new BusinessException("TTS API Key 无效,请检查配置");
                }
                throw new BusinessException("语音合成失败:" + resp.getStatus());
            }
            return resp.bodyBytes();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS 调用异常", e);
            throw new BusinessException("语音合成失败:" + e.getMessage());
        }
    }
}
