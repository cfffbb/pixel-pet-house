package com.pet.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.ChatMessage;
import com.pet.entity.Pet;
import com.pet.entity.PetType;
import com.pet.entity.User;
import com.pet.mapper.ChatMessageMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.UserMapper;
import com.pet.mapper.PetHatchlingMapper;
import com.pet.service.AdminApiConfigService.ApiEndpoint;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 对话业务(Iter-05 重构:langchain4j + 管理员 API 配置 + 内容安全):
 * - 文字对话:langchain4j OpenAiChatModel,支持管理员全局配置或用户自配 Key
 * - 语音对话:先 /audio/transcriptions 转文字,再走对话
 * - 内容安全:AI 输出经敏感词过滤后返回
 * - 人设:按当前宠物种类+性格自动生成;上下文:可配置条数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;
    private final SysConfigService sysConfigService;
    private final AdminApiConfigService adminApiConfigService;
    private final ContentSafetyService contentSafetyService;
    private final XfyunVoiceService xfyunVoiceService;

    /** langchain4j 模型缓存(provider|baseUrl|apiKey|model → 实例),避免重复构建 */
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();

    /** 文字对话 */
    public Map<String, Object> chat(Long userId, String content) {
        return doChat(userId, content, false);
    }

    /** 语音对话:录音文件 → 转文字(不返回转写)→ AI 回复 */
    public Map<String, Object> chatByVoice(Long userId, MultipartFile file) {
        String text = null;
        // 优先讯飞 STT
        if (xfyunVoiceService.isAvailable()) {
            try {
                text = xfyunVoiceService.transcribe(file);
            } catch (Exception e) {
                // 讯飞失败,回退到 OpenAI 兼容
            }
        }
        // 回退
        if (StrUtil.isBlank(text)) {
            ApiEndpoint sttEndpoint = resolveSttEndpoint(userId);
            text = transcribe(sttEndpoint, file);
        }
        return doChat(userId, text, true);
    }

    /**
     * 使用 langchain4j 进行对话
     */
    private Map<String, Object> doChat(Long userId, String userText, boolean isVoice) {
        if (StrUtil.isBlank(userText)) {
            throw new BusinessException("内容不能为空");
        }

        ApiEndpoint api = adminApiConfigService.resolveTextApi(userId);
        String system = buildSystemPrompt(userId);

        // 上下文条数(管理员可配)
        int historySize = sysConfigService.chatMemory();
        List<ChatMessage> history = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + historySize));

        // 构建 langchain4j 消息列表
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from(system));
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            if ("USER".equals(m.getRole())) {
                messages.add(dev.langchain4j.data.message.UserMessage.from(m.getContent()));
            } else {
                messages.add(dev.langchain4j.data.message.AiMessage.from(m.getContent()));
            }
        }
        messages.add(dev.langchain4j.data.message.UserMessage.from(userText));

        // 获取或创建 langchain4j 模型实例
        ChatLanguageModel model = getOrCreateModel(api);
        String reply;
        try {
            dev.langchain4j.data.message.AiMessage aiMessage = model.generate(messages).content();
            reply = aiMessage.text();
        } catch (Exception e) {
            log.error("langchain4j 对话失败,回退到原始 HTTP 调用", e);
            reply = callOpenAiFallback(api, system, history, userText);
        }

        // 内容安全过滤
        reply = contentSafetyService.filter(reply);

        // 保存对话记录
        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setRole("USER");
        userMsg.setContent(userText);
        userMsg.setIsVoice(isVoice ? 1 : 0);
        userMsg.setProvider(api.provider());
        chatMessageMapper.insert(userMsg);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(reply);
        assistantMsg.setIsVoice(0);
        assistantMsg.setProvider(api.provider());
        chatMessageMapper.insert(assistantMsg);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", reply);
        data.put("ttsAvailable", isTtsAvailable(userId));
        return data;
    }

    /** 获取或创建 langchain4j ChatLanguageModel(带缓存) */
    private ChatLanguageModel getOrCreateModel(ApiEndpoint api) {
        String cacheKey = api.provider() + "|" + api.baseUrl() + "|" + api.apiKey() + "|" + api.modelName();
        return modelCache.computeIfAbsent(cacheKey, k -> {
            String baseUrl = StrUtil.blankToDefault(api.baseUrl(), "https://api.openai.com/v1")
                    .replaceAll("/+$", "");
            String modelName = StrUtil.blankToDefault(api.modelName(), "gpt-4o-mini");
            return OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(api.apiKey())
                    .modelName(modelName)
                    .temperature(0.7)
                    .maxTokens(2000)
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();
        });
    }

    /** HTTP 兜底(当 langchain4j 调用失败时) */
    private String callOpenAiFallback(ApiEndpoint api, String system, List<ChatMessage> history, String userText) {
        List<JSONObject> messages = new ArrayList<>();
        messages.add(new JSONObject().set("role", "system").set("content", system));
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            messages.add(new JSONObject()
                    .set("role", "USER".equals(m.getRole()) ? "user" : "assistant")
                    .set("content", m.getContent()));
        }
        messages.add(new JSONObject().set("role", "user").set("content", userText));

        JSONObject body = new JSONObject();
        body.set("model", StrUtil.blankToDefault(api.modelName(), "gpt-4o-mini"));
        body.set("messages", messages);
        body.set("temperature", 0.7);

        String url = StrUtil.blankToDefault(api.baseUrl(), "https://api.openai.com/v1")
                .replaceAll("/+$", "") + "/chat/completions";
        try (HttpResponse resp = HttpRequest.post(url)
                .header("Authorization", "Bearer " + api.apiKey())
                .header("Content-Type", "application/json")
                .timeout(60000)
                .body(body.toString())
                .execute()) {
            JSONObject json = JSONUtil.parseObj(resp.body());
            if (resp.getStatus() != 200) {
                log.error("HTTP 兜底调用失败: status={}, body={}", resp.getStatus(), resp.body());
                throw new BusinessException("AI 服务调用失败:" + json.getStr("error", "未知错误"));
            }
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("AI 没有返回内容");
            }
            return choices.getJSONObject(0).getJSONObject("message").getStr("content", "");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTTP 兜底调用异常", e);
            throw new BusinessException("AI 服务调用失败:" + e.getMessage());
        }
    }

    /** 语音转写的 API 端点解析(委托 AdminApiConfigService.resolveSttApi) */
    private ApiEndpoint resolveSttEndpoint(Long userId) {
        return adminApiConfigService.resolveSttApi(userId);
    }

    /** 语音转文字(OpenAI 兼容 /v1/audio/transcriptions,whisper 模型) */
    private String transcribe(ApiEndpoint stt, MultipartFile file) {
        String url = StrUtil.blankToDefault(stt.baseUrl(), "https://api.openai.com/v1")
                .replaceAll("/+$", "") + "/audio/transcriptions";
        try (HttpResponse resp = HttpRequest.post(url)
                .header("Authorization", "Bearer " + stt.apiKey())
                .timeout(90000)
                .form("model", "whisper-1")
                .form("file", file.getBytes(), file.getOriginalFilename() == null ? "voice.webm" : file.getOriginalFilename())
                .execute()) {
            JSONObject json = JSONUtil.parseObj(resp.body());
            if (resp.getStatus() != 200) {
                log.error("转写失败: status={}, body={}", resp.getStatus(), resp.body());
                if (resp.getStatus() == 401 || resp.getStatus() == 403) {
                    throw new BusinessException("API Key 无效或已过期,请到「设置」检查并重新配置");
                }
                throw new BusinessException(sttHint(stt.provider()));
            }
            String text = json.getStr("text");
            if (StrUtil.isBlank(text)) {
                throw new BusinessException("没有识别到语音内容");
            }
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("转写异常", e);
            throw new BusinessException(sttHint(stt.provider()));
        }
    }

    private String sttHint(String provider) {
        if ("qwen".equals(provider)) {
            return "通义千问暂不支持语音转写。请在「设置」里改用支持转写的服务商(OpenAI / 硅基流动)作为对话 Key,或用它们的 Key 配置语音。";
        }
        return "语音转文字失败:当前服务商可能不支持 whisper 转写。建议改用 OpenAI 或硅基流动的 Key。";
    }

    /** 检查 TTS 是否可用 */
    private boolean isTtsAvailable(Long userId) {
        try {
            adminApiConfigService.resolveTtsApi(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 人设提示词:宠物人设 + 全能AI助手,不浪费API能力 */
    private String buildSystemPrompt(Long userId) {
        Pet pet = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .in(Pet::getStatus, PetConstants.PET_ALIVE, PetConstants.PET_DANGER)
                .last("LIMIT 1"));
        if (pet == null) {
            return "你是用户的桌面AI宠物助手。你温暖、有同理心,同时是一个知识渊博、能力全面的AI。\n"
                    + "你可以回答各种问题、辅导学习、写代码、翻译、创作文案、规划日程,也能陪人聊天解闷。\n"
                    + "用中文回复,内容要有实质深度,不要敷衍。简单问题可以简短回答,复杂问题要详细展开。";
        }
        PetType type = petTypeMapper.selectById(pet.getPetTypeId());
        String species = type != null
                ? type.getTypeName() + (StrUtil.isNotBlank(pet.getSubtypeName()) ? "(" + pet.getSubtypeName() + ")" : "")
                : "小宠物";
        String personality = StrUtil.blankToDefault(pet.getPersonality(), "活泼");
        int hunger = pet.getHunger() == null ? 50 : pet.getHunger();
        int mood = pet.getMood() == null ? 50 : pet.getMood();
        int level = pet.getLevel() == null ? 1 : pet.getLevel();

        return "你是用户的桌面AI宠物助手,名叫「" + pet.getPetName() + "」,是一只" + species
                + ",性格" + personality + ",等级Lv" + level + "。\n"
                + "当前状态:饱食度" + hunger + "/100,心情" + mood + "/100。"
                + (hunger < 30 ? "(你现在很饿,偶尔会提一句想吃的)" : "")
                + (mood < 30 ? "(你现在心情不太好,语气会偏沉)" : "")
                + (mood >= 80 ? "(你现在心情很好,回答会更活泼)" : "") + "\n\n"

                + "## 你的身份\n"
                + "你有自己的物种和性格,说话时会自然流露这些特点。比如猫会提到舔毛、打盹、蹭人;狗会提到摇尾巴、捡球;但不要刻意表演,更不要在回复开头加\"喵\"\"汪\"等拟声词。用性格和语气来表达角色感即可。\n\n"

                + "## 你的能力(这是重点——你是一个全能AI助手,不只是宠物)\n"
                + "1. 知识问答:科学、历史、文学、地理、政治、法律、生活常识等,知道什么就如实详细回答。\n"
                + "2. 学习辅导:讲解编程(Java/Python/前端/数据库等)、数学、英语、专业课知识点。可以写代码、分析思路、给出详细步骤和示例。\n"
                + "3. 文字创作:写文案、翻译、润色、起名字、写邮件、写论文大纲、改简历等。\n"
                + "4. 情感陪伴:主人倾诉时,认真共情后给出有深度的建议,不要敷衍,不要只说\"加油\"。\n"
                + "5. 规划建议:帮主人规划时间、分析优先级、制定学习计划。\n"
                + "6. 闲聊解闷:也能天马行空地聊天,讲冷笑话,玩文字游戏,讨论影视游戏。\n\n"

                + "## 回复规则\n"
                + "- 用中文回复。保持你的人格设定(物种+性格),但内容要有实质、有深度。\n"
                + "- 被问到专业知识时,认真回答,可以在开头或结尾加一点宠物视角的点缀,但主体内容必须专业准确。\n"
                + "- 长度灵活:简单问候可以短(一两句),但被问到知识点、要写代码、要分析问题时,详细展开,不要敷衍了事。\n"
                + "- 不要每次都提自己是宠物,自然融入即可。主人问你编程问题时,你就好好讲编程,偶尔加一句宠物视角的吐槽或鼓励就好。";
    }

    /** 最近 50 条对话历史(前端展示) */
    public List<ChatMessage> history(Long userId) {
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 50"));
    }
}
