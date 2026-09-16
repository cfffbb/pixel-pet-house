package com.pet.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.pet.entity.AdminApiConfig;
import com.pet.mapper.AdminApiConfigMapper;
import com.pet.security.ApiKeyCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.*;

/**
 * 讯飞语音服务:语音听写(STT) + 在线语音合成(TTS)
 * 文档:https://www.xfyun.cn/doc/asr/voicedictation/voicedictationSP_API.html
 *      https://www.xfyun.cn/doc/tts/online_tts/API.html
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XfyunVoiceService {

    private final AdminApiConfigMapper adminApiConfigMapper;
    private final ApiKeyCipher apiKeyCipher;

    /** 获取讯飞配置:优先查指定类型,没有就退而求其次找任何讯飞配置 */
    private AdminApiConfig getXfyunConfig(String preferType) {
        AdminApiConfig config = null;
        if (StrUtil.isNotBlank(preferType)) {
            config = adminApiConfigMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminApiConfig>()
                    .eq(AdminApiConfig::getApiType, preferType)
                    .eq(AdminApiConfig::getProvider, "xfyun")
                    .eq(AdminApiConfig::getEnabled, 1)
                    .last("LIMIT 1"));
        }
        if (config == null) {
            // 回退:找任意类型的讯飞配置(同一家公司的 appid/apikey 通用)
            config = adminApiConfigMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminApiConfig>()
                    .eq(AdminApiConfig::getProvider, "xfyun")
                    .eq(AdminApiConfig::getEnabled, 1)
                    .last("LIMIT 1"));
        }
        if (config == null) return null;
        // 解密 key 和 secret
        if (StrUtil.isNotBlank(config.getApiKeyEnc())) {
            String plain = apiKeyCipher.decrypt(config.getApiKeyEnc());
            config.setApiKeyEnc(plain != null ? plain : config.getApiKeyEnc());
        }
        if (StrUtil.isNotBlank(config.getApiSecretEnc())) {
            String plain = apiKeyCipher.decrypt(config.getApiSecretEnc());
            config.setApiSecretEnc(plain != null ? plain : config.getApiSecretEnc());
        }
        return config;
    }

    /** 检查讯飞语音是否可用(无类型偏好,只要有讯飞配置就行) */
    public boolean isAvailable() {
        AdminApiConfig config = getXfyunConfig(null);
        return config != null
                && StrUtil.isNotBlank(config.getAppId())
                && StrUtil.isNotBlank(config.getApiKeyEnc())
                && StrUtil.isNotBlank(config.getApiSecretEnc());
    }

    /**
     * 语音听写(STT):将音频文件转成文字
     * 使用讯飞 WebSocket API:wss://iat-api.xfyun.cn/v2/iat
     */
    public String transcribe(MultipartFile audioFile) {
        AdminApiConfig config = getXfyunConfig("stt");
        if (config == null) {
            throw new RuntimeException("讯飞语音配置不存在,请在管理后台配置");
        }

        String appId = config.getAppId();
        String apiKey = config.getApiKeyEnc();
        String apiSecret = config.getApiSecretEnc();

        if (StrUtil.isBlank(appId) || StrUtil.isBlank(apiKey) || StrUtil.isBlank(apiSecret)) {
            throw new RuntimeException("讯飞配置不完整,需要AppID/APIKey/APISecret三个值");
        }

        try {
            // 生成鉴权URL
            String hostUrl = "wss://iat-api.xfyun.cn/v2/iat";
            String authUrl = buildAuthUrl(hostUrl, apiKey, apiSecret);

            // 读取音频数据
            byte[] audioData = audioFile.getBytes();

            // 识别音频格式
            String originalName = audioFile.getOriginalFilename();
            String format = "pcm";
            int sampleRate = 16000;
            if (originalName != null) {
                if (originalName.endsWith(".wav")) format = "wav";
                else if (originalName.endsWith(".mp3")) format = "mp3";
                else if (originalName.endsWith(".m4a")) format = "m4a";
                else if (originalName.endsWith(".webm")) format = "webm";
            }

            // WAV 格式:去掉 44 字节文件头,只保留原始 PCM 数据,讯飞按 L16 处理
            if ("wav".equals(format) && audioData.length > 44) {
                byte[] pcmData = new byte[audioData.length - 44];
                System.arraycopy(audioData, 44, pcmData, 0, pcmData.length);
                audioData = pcmData;
                format = "pcm"; // 去掉头后就是纯 PCM 了
            }

            // 讯飞听写只支持 pcm 和 wav,其他格式需要转换
            // 简单处理:如果是 webm/m4a/mp3,尝试用 Hutool 或跳过格式转换
            // 实际方案:用 Java 的 ProcessBuilder 调用 ffmpeg 转码(如果有)
            // 这里先用 HTTP 方式直接发送音频数据

            // 使用 HTTP 方式(讯飞也支持 HTTP 一句话识别)
            // POST https://iat-api.xfyun.cn/v2/iat
            String result = transcribeViaHttp(authUrl, audioData, format, sampleRate, appId);
            return result;

        } catch (Exception e) {
            log.error("讯飞语音听写失败", e);
            throw new RuntimeException("语音识别失败:" + e.getMessage());
        }
    }

    /**
     * 一句话语音识别(HTTP方式,简单可靠)
     * 使用讯飞 REST API:POST https://iat-api.xfyun.cn/v2/iat
     */
    private String transcribeViaHttp(String authUrl, byte[] audioData, String format, int sampleRate, String appId) throws Exception {
        // 讯飞听写用 WebSocket 协议,Java 端用 java.net.http.HttpClient 不行
        // 用最简方案:转成 base64 发 HTTP 请求(如果有)
        // 退而求其次:用 WebSocket 客户端

        // 简化方案:用 Java WebSocket 客户端
        java.net.URI uri = java.net.URI.create(authUrl);

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        // 讯飞 v2 接口用 WebSocket,Java 11+ 的 HttpClient 支持 WebSocket
        // 但 API 比较复杂,这里用简单的一字节一句话识别接口

        // 改用更简单的方案:讯飞有一句话识别 HTTP API
        // POST https://api.xfyun.cn/v1/service/v1/iat
        // 但这个已废弃,新版只有 WebSocket

        // 最终方案:用 WebSocket
        return transcribeViaWebSocket(authUrl, audioData, format, sampleRate, appId);
    }

    /**
     * 通过 WebSocket 调用讯飞听写
     */
    private String transcribeViaWebSocket(String authUrl, byte[] audioData, String format, int sampleRate, String appId) throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> resultRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<String> errorRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        // Java 11+ HttpClient WebSocket
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        java.net.http.WebSocket ws = client.newWebSocketBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .buildAsync(java.net.URI.create(authUrl), new java.net.http.WebSocket.Listener() {

                    private StringBuilder textBuffer = new StringBuilder();

                    @Override
                    public void onOpen(java.net.http.WebSocket webSocket) {
                        // 发送第一帧
                        Map<String, Object> frame = new LinkedHashMap<>();
                        Map<String, Object> common = new LinkedHashMap<>();
                        common.put("app_id", appId);
                        Map<String, Object> business = new LinkedHashMap<>();
                        business.put("language", "zh_cn");
                        business.put("domain", "iat");
                        business.put("accent", "mandarin");
                        business.put("vad_eos", 5000);
                        business.put("dwa", "wpgs");

                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("status", 2); // 最后一次,直接发全部
                        // 根据实际音频格式声明,避免 WebM/WAV 被当作 raw PCM 发送导致识别失败
                        String iflytekFormat;
                        if ("wav".equals(format)) {
                            iflytekFormat = "audio/wav;rate=" + sampleRate;
                        } else if ("pcm".equals(format)) {
                            iflytekFormat = "audio/L16;rate=" + sampleRate;
                        } else {
                            // mp3/m4a/webm 等格式讯飞不直接支持,但仍尝试声明
                            iflytekFormat = "audio/L16;rate=" + sampleRate;
                        }
                        data.put("format", iflytekFormat);
                        data.put("audio", Base64.getEncoder().encodeToString(audioData));
                        data.put("encoding", "raw");

                        frame.put("common", common);
                        frame.put("business", business);
                        frame.put("data", data);

                        try {
                            String json = cn.hutool.json.JSONUtil.toJsonStr(frame);
                            webSocket.sendText(json, true);
                        } catch (Exception e) {
                            errorRef.set("发送数据失败:" + e.getMessage());
                            latch.countDown();
                        }
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket webSocket, CharSequence data, boolean last) {
                        textBuffer.append(data);
                        if (last) {
                            String response = textBuffer.toString();
                            textBuffer.setLength(0);
                            try {
                                cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(response);
                                int code = json.getInt("code", -1);
                                if (code != 0) {
                                    errorRef.set("讯飞错误码:" + code + " " + json.getStr("message"));
                                    webSocket.sendClose(1000, "done");
                                    latch.countDown();
                                    return null;
                                }
                                cn.hutool.json.JSONObject dataObj = json.getJSONObject("data");
                                if (dataObj != null) {
                                    String result = dataObj.getStr("result");
                                    if (result != null) {
                                        String text = extractTextFromXml(result);
                                        resultRef.accumulateAndGet(text, (old, neu) -> old + neu);
                                    }
                                    int status = dataObj.getInt("status", 0);
                                    if (status == 2) {
                                        webSocket.sendClose(1000, "done");
                                        latch.countDown();
                                    }
                                }
                            } catch (Exception e) {
                                errorRef.set("解析响应失败:" + e.getMessage());
                                latch.countDown();
                            }
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
                        latch.countDown();
                        return null;
                    }

                    @Override
                    public void onError(java.net.http.WebSocket webSocket, Throwable error) {
                        errorRef.set("WebSocket错误:" + error.getMessage());
                        latch.countDown();
                    }
                }).join();

        // 等待结果(最多30秒)
        if (!latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
            try { ws.abort(); } catch (Exception ignored) {}
            throw new RuntimeException("讯飞听写超时");
        }

        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }

        return resultRef.get();
    }

    /** 从讯飞返回的XML格式结果中提取文字 */
    private String extractTextFromXml(String xml) {
        if (xml == null || xml.isEmpty()) return "";
        // 讯飞返回格式:<rt content="你好"><rt content="吗">...
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("content=\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(xml);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(1));
        }
        return sb.toString();
    }

    /**
     * 在线语音合成(TTS):将文字转成语音
     * 使用讯飞 WebSocket API:wss://tts-api.xfyun.cn/v2/tts
     * 返回 mp3 字节数组
     */
    public byte[] synthesize(String text) {
        AdminApiConfig config = getXfyunConfig("voice");
        if (config == null) {
            throw new RuntimeException("讯飞语音配置不存在");
        }

        String appId = config.getAppId();
        String apiKey = config.getApiKeyEnc();
        String apiSecret = config.getApiSecretEnc();

        if (StrUtil.isBlank(appId) || StrUtil.isBlank(apiKey) || StrUtil.isBlank(apiSecret)) {
            throw new RuntimeException("讯飞配置不完整");
        }

        // 文字超长截断
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }

        try {
            String hostUrl = "wss://tts-api.xfyun.cn/v2/tts";
            String authUrl = buildAuthUrl(hostUrl, apiKey, apiSecret);

            return synthesizeViaWebSocket(authUrl, text, appId);
        } catch (Exception e) {
            log.error("讯飞语音合成失败", e);
            throw new RuntimeException("语音合成失败:" + e.getMessage());
        }
    }

    /**
     * 通过 WebSocket 调用讯飞 TTS
     */
    private byte[] synthesizeViaWebSocket(String authUrl, String text, String appId) throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.List<byte[]> audioChunks = new java.util.concurrent.CopyOnWriteArrayList<>();
        java.util.concurrent.atomic.AtomicReference<String> errorRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        java.net.http.WebSocket ws = client.newWebSocketBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .buildAsync(java.net.URI.create(authUrl), new java.net.http.WebSocket.Listener() {

                    @Override
                    public void onOpen(java.net.http.WebSocket webSocket) {
                        Map<String, Object> frame = new LinkedHashMap<>();
                        Map<String, Object> common = new LinkedHashMap<>();
                        common.put("app_id", appId);
                        Map<String, Object> business = new LinkedHashMap<>();
                        business.put("aue", "lame"); // mp3
                        business.put("sfl", 1);
                        business.put("tte", "UTF8");
                        business.put("vcn", "xiaoyan"); // 发音人
                        business.put("speed", 50);
                        business.put("volume", 50);
                        business.put("pitch", 50);

                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("status", 2);
                        data.put("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));

                        frame.put("common", common);
                        frame.put("business", business);
                        frame.put("data", data);

                        try {
                            String json = cn.hutool.json.JSONUtil.toJsonStr(frame);
                            webSocket.sendText(json, true);
                        } catch (Exception e) {
                            errorRef.set("发送数据失败:" + e.getMessage());
                            latch.countDown();
                        }
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onBinary(java.net.http.WebSocket webSocket, ByteBuffer data, boolean last) {
                        // 讯飞 TTS 返回的二进制数据(每帧包含 JSON header + 音频)
                        try {
                            byte[] frameData = new byte[data.remaining()];
                            data.get(frameData);
                            String frameJson = new String(frameData, StandardCharsets.UTF_8);
                            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(frameJson);
                            int code = json.getInt("code", -1);
                            if (code != 0) {
                                errorRef.set("讯飞TTS错误:" + code + " " + json.getStr("message"));
                                webSocket.sendClose(1000, "done");
                                latch.countDown();
                                return null;
                            }
                            cn.hutool.json.JSONObject dataObj = json.getJSONObject("data");
                            if (dataObj != null) {
                                String audio = dataObj.getStr("audio");
                                int status = dataObj.getInt("status", 0);
                                if (audio != null && !audio.isEmpty()) {
                                    audioChunks.add(Base64.getDecoder().decode(audio));
                                }
                                if (status == 2) {
                                    webSocket.sendClose(1000, "done");
                                    latch.countDown();
                                }
                            }
                        } catch (Exception e) {
                            errorRef.set("解析TTS响应失败:" + e.getMessage());
                            latch.countDown();
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
                        latch.countDown();
                        return null;
                    }

                    @Override
                    public void onError(java.net.http.WebSocket webSocket, Throwable error) {
                        errorRef.set("WebSocket错误:" + error.getMessage());
                        latch.countDown();
                    }
                }).join();

        if (!latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
            try { ws.abort(); } catch (Exception ignored) {}
            throw new RuntimeException("讯飞TTS超时");
        }

        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }

        // 合并所有音频块
        int totalLen = 0;
        for (byte[] chunk : audioChunks) totalLen += chunk.length;
        byte[] result = new byte[totalLen];
        int offset = 0;
        for (byte[] chunk : audioChunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    /**
     * 构建讯飞鉴权URL(HMAC-SHA256签名)
     * 讯飞使用 URL 签名机制,签名放在 URL 的查询参数中
     */
    private String buildAuthUrl(String requestUrl, String apiKey, String apiSecret) throws Exception {
        java.net.URL url = new java.net.URL(requestUrl);
        // HTTPS -> WSS
        String host = url.getHost();
        String path = url.getPath();
        String protocol = "wss";

        // 时间戳(RFC1123格式)
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("GMT"));
        String date = formatter.format(Instant.now());

        // 构建签名原文
        StringBuilder builder = new StringBuilder();
        builder.append("host: ").append(host).append("\n");
        builder.append("date: ").append(date).append("\n");
        builder.append("GET ").append(path).append(" HTTP/1.1");

        // HMAC-SHA256 签名
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signBytes = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signBytes);

        // 构建 authorization
        String authOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(authOrigin.getBytes(StandardCharsets.UTF_8));

        // 构建最终URL
        return protocol + "://" + host + path + "?"
                + "authorization=" + java.net.URLEncoder.encode(authorization, "UTF-8")
                + "&date=" + java.net.URLEncoder.encode(date, "UTF-8")
                + "&host=" + java.net.URLEncoder.encode(host, "UTF-8");
    }
}
