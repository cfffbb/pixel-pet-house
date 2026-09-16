package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.ChatMessage;
import com.pet.entity.UserVoicePref;
import com.pet.security.JwtInterceptor;
import com.pet.service.AdminApiConfigService;
import com.pet.service.BubblePromptService;
import com.pet.service.ChatService;
import com.pet.service.TtsService;
import com.pet.service.XfyunVoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话接口(Iter-05 增强:langchain4j + TTS + 气泡库)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final TtsService ttsService;
    private final AdminApiConfigService adminApiConfigService;
    private final BubblePromptService bubblePromptService;
    private final XfyunVoiceService xfyunVoiceService;

    /** 最近对话历史 */
    @GetMapping("/history")
    public Result<List<ChatMessage>> history(HttpServletRequest request) {
        return Result.ok(chatService.history((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 文字对话 */
    @PostMapping("/message")
    public Result<Map<String, Object>> message(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(chatService.chat(userId, body.get("content")));
    }

    /** 语音对话:录音文件 → 语音转文字(前端不显示转写过程)→ AI 回复 */
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> voice(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(chatService.chatByVoice(userId, file));
    }

    /**
     * TTS 语音合成:将文本转为 mp3 音频
     * 优先使用讯飞,不可用时回退到 OpenAI 兼容接口
     */
    @PostMapping(value = "/tts", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void tts(@RequestBody Map<String, String> body, HttpServletRequest request,
                    HttpServletResponse response) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        byte[] audio = null;
        // 优先讯飞
        if (xfyunVoiceService.isAvailable()) {
            try {
                audio = xfyunVoiceService.synthesize(body.get("text"));
            } catch (Exception e) {
                // 讯飞失败,回退
            }
        }
        // 回退到 OpenAI 兼容
        if (audio == null || audio.length == 0) {
            audio = ttsService.synthesize(userId, body.get("text"));
        }
        response.setContentType("audio/mpeg");
        response.setContentLength(audio.length);
        try {
            response.getOutputStream().write(audio);
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException("音频输出失败", e);
        }
    }

    /**
     * 获取用户语音偏好
     */
    @GetMapping("/voice-pref")
    public Result<Map<String, Object>> voicePref(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        UserVoicePref pref = adminApiConfigService.getUserPref(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (pref != null) {
            data.put("useAdminKey", pref.getUseAdminKey());
            data.put("ttsEnabled", pref.getTtsEnabled());
            data.put("voice", pref.getVoice());
            data.put("ttsSpeed", pref.getTtsSpeed());
            data.put("bubbleRead", pref.getBubbleRead());
        } else {
            data.put("useAdminKey", 1);
            data.put("ttsEnabled", 0);
            data.put("voice", "alloy");
            data.put("ttsSpeed", 1.0);
            data.put("bubbleRead", 0);
        }
        data.put("availableVoices", adminApiConfigService.availableVoices());
        return Result.ok(data);
    }

    /** 保存用户语音偏好 */
    @PutMapping("/voice-pref")
    public Result<Void> saveVoicePref(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        adminApiConfigService.saveUserPref(userId, body);
        return Result.ok();
    }

    /**
     * 获取一条随机气泡文本(按宠物属性匹配)
     * query param: category=greeting|happy|sad|hungry|sleepy|study|play|random
     */
    @GetMapping("/bubble")
    public Result<Map<String, Object>> bubble(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String category = request.getParameter("category");
        if (category == null) category = "random";
        String text = bubblePromptService.randomBubble(userId, category);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", text);
        return Result.ok(data);
    }

    /** 获取多条随机气泡(悬浮窗轮播用) */
    @GetMapping("/bubbles")
    public Result<Map<String, Object>> bubbles(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        List<String> texts = bubblePromptService.randomBubbles(userId, 5);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("texts", texts);
        return Result.ok(data);
    }
}
