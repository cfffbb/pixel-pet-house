package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.ApiKeyDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.UserApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户自己的 AI API Key 管理(需登录)
 */
@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;

    /**
     * 服务商预设(下拉选择即可,自动带出接口地址/模型名/key 格式提示)
     * stt=true 表示支持 OpenAI 兼容语音转写(/audio/transcriptions)
     */
    @GetMapping("/providers")
    public Result<List<Map<String, Object>>> providers() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(provider("qwen", "通义千问(阿里)", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                List.of("qwen-plus", "qwen-turbo", "qwen-max"), "sk- 开头", false));
        list.add(provider("deepseek", "DeepSeek", "https://api.deepseek.com/v1",
                List.of("deepseek-chat", "deepseek-reasoner"), "sk- 开头", false));
        list.add(provider("openai", "OpenAI", "https://api.openai.com/v1",
                List.of("gpt-4o-mini", "gpt-4o"), "sk- 开头", true));
        list.add(provider("zhipu", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4",
                List.of("glm-4-flash", "glm-4-plus"), "长串字符", false));
        list.add(provider("moonshot", "Kimi(月之暗面)", "https://api.moonshot.cn/v1",
                List.of("moonshot-v1-8k", "moonshot-v1-32k"), "sk- 开头", false));
        list.add(provider("siliconflow", "硅基流动", "https://api.siliconflow.cn/v1",
                List.of("Qwen/Qwen2.5-7B-Instruct", "deepseek-ai/DeepSeek-V3"), "sk- 开头", true));
        list.add(provider("xinghuo", "讯飞星火", "https://spark-api-open.xf-yun.com/v1",
                List.of("spark-4.0-ultra", "spark-lite"), "APIKey:APISecret 组合", false));
        list.add(provider("baidu", "百度千帆", "https://qianfan.baidubce.com/v2",
                List.of("ernie-4.0-8k", "ernie-3.5-8k"), "AK/SK 或 Bearer", false));
        list.add(provider("hunyuan", "腾讯混元", "https://api.hunyuan.cloud.tencent.com/v1",
                List.of("hunyuan-turbo", "hunyuan-lite"), "长串字符", false));
        list.add(provider("minimax", "MiniMax", "https://api.minimax.chat/v1",
                List.of("abab6.5s-chat", "MiniMax-Text-01"), "长串字符", false));
        list.add(provider("stepfun", "阶跃星辰", "https://api.stepfun.com/v1",
                List.of("step-1-8k", "step-2-16k"), "长串字符", false));
        list.add(provider("groq", "Groq", "https://api.groq.com/openai/v1",
                List.of("llama-3.3-70b-versatile"), "gsk_ 开头", false));
        list.add(provider("ollama", "Ollama 本地", "http://localhost:11434/v1",
                List.of("qwen2.5:7b"), "本地无需 Key(任意填)", false));
        list.add(provider("custom", "自定义(OpenAI 兼容)", "",
                List.of(), "任意,接口地址必填", true));
        return Result.ok(list);
    }

    private Map<String, Object> provider(String name, String label, String baseUrl, List<String> models, String keyHint, boolean stt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("label", label);
        m.put("baseUrl", baseUrl);
        m.put("models", models);
        m.put("keyHint", keyHint);
        m.put("stt", stt);
        return m;
    }

    /** 我的 Key 列表(脱敏,绝不返回明文) */
    @GetMapping
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        return Result.ok(userApiKeyService.listMine(currentUserId(request)));
    }

    /** 新增(每个服务商最多 1 个) */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody ApiKeyDTO dto, HttpServletRequest request) {
        userApiKeyService.add(currentUserId(request), dto);
        return Result.ok();
    }

    /** 修改(Key 留空表示不更换) */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ApiKeyDTO dto, HttpServletRequest request) {
        userApiKeyService.update(currentUserId(request), id, dto);
        return Result.ok();
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        userApiKeyService.delete(currentUserId(request), id);
        return Result.ok();
    }

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
