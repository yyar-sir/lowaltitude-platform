package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.entity.AiConfig;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 大模型能力接口。
 *
 * <p>未配置 API Key 时统一返回业务码 1101，前端提示「AI 功能未配置，请联系管理员」，
 * 而不是「网络异常」——两者对用户的行动指引完全不同。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /** 运营问答 */
    @PostMapping("/ask")
    @RequireRole({0, 1, 2})
    public Result<Map<String, Object>> ask(@RequestParam String question) {
        Map<String, Object> m = new HashMap<>();
        m.put("question", question);
        m.put("answer", aiService.ask(question));
        return Result.ok(m);
    }

    /** 智能调度建议（含不依赖模型的规则兜底建议） */
    @GetMapping("/dispatch-advice/{orderId}")
    @RequireRole({0, 1})
    public Result<Map<String, Object>> dispatchAdvice(@PathVariable Long orderId) {
        return Result.ok(aiService.dispatchAdvice(orderId));
    }

    /** 合规检查辅助 */
    @GetMapping("/compliance-check/{orderId}")
    @RequireRole({0, 1})
    public Result<Map<String, Object>> complianceCheck(@PathVariable Long orderId) {
        return Result.ok(aiService.complianceCheck(orderId));
    }

    /** AI 可用性自检 */
    @GetMapping("/status")
    @RequireRole({0, 1, 2})
    public Result<Map<String, Object>> status() {
        return Result.ok(aiService.status());
    }

    /** 读取 AI 配置（密钥已脱敏） */
    @GetMapping("/config")
    @RequireRole(0)
    public Result<AiConfig> config() {
        return Result.ok(aiService.getMaskedConfig());
    }

    /** 更新 AI 配置（apiKey 传空表示保留原值） */
    @PostMapping("/config")
    @RequireRole(0)
    public Result<AiConfig> updateConfig(@RequestBody AiConfig dto) {
        return Result.ok(aiService.updateConfig(dto));
    }
}
