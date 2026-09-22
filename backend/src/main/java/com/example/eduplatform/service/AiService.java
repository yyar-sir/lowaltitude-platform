package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.entity.AiConfig;
import com.example.eduplatform.entity.OrderInfo;
import com.example.eduplatform.mapper.AiConfigMapper;
import com.example.eduplatform.mapper.OrderInfoMapper;
import com.example.eduplatform.security.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 大模型能力：智能调度建议、违规识别辅助、运营问答。
 *
 * <p>核心原则：把大模型当成随时会挂、会超时、会返回垃圾数据的不可靠外部依赖。
 * 五件事缺一不可：配置落库、按用户限流、内容安全、三级降级、密钥脱敏。
 *
 * <p>三级降级：网络异常 → 场景化文案；建议解析失败 → 内置规则建议；
 * 未配置 Key → 业务码 1101。接口层永远不抛未处理异常。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiConfigMapper aiConfigMapper;
    private final OrderInfoMapper orderMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_FALLBACK = "AI 助手暂时开小差了，请稍后再试～";
    private static final String DISPATCH_FALLBACK = "调度建议生成失败，已为你按航程与载重给出默认推荐。";
    private static final String COMPLIANCE_FALLBACK = "合规检查服务繁忙，请人工复核该订单的空域与载重信息。";

    private static final String SAFETY_PROMPT =
            "你是低空物流运营平台的专业助手，回答要客观、准确，涉及飞行安全与空域法规时务必谨慎，不要给出违规建议";

    private static final String[] SENSITIVE_WORDS = { "绕过管制", "违规飞行教程", "伪造许可" };
    private static final String SENSITIVE_FALLBACK = "这个请求涉及违规风险，我不能提供相关建议。";

    /** 按用户维度的每分钟限流器缓存 */
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    // ---------- 对外能力 ----------

    /** 运营问答：通用对话 */
    public String ask(String question) {
        String prompt = "你是低空物流运营平台的运营助手。请用简洁专业的中文回答运营人员的提问。\n提问：" + question;
        return doChat(SAFETY_PROMPT, prompt, DEFAULT_FALLBACK);
    }

    /**
   * 智能调度建议。
   *
   * <p>把订单与可用资源一起交给模型，要求返回结构化建议；
   * 解析失败时回退为按航程与载重计算的规则建议（二级降级）。
   */
    public Map<String, Object> dispatchAdvice(Long orderId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(1001, "订单不存在");
        }
        String prompt = "订单信息：重量 " + order.getGoodsWeight() + "kg，航程 " + order.getDistanceKm()
                + "km，时效 " + (order.getDeliveryType() != null && order.getDeliveryType() == 2 ? "加急" : "普通")
                + "，优先级 " + order.getPriority()
                + "。请给出一条调度建议，说明应优先选用的飞行器特征、是否需要注意空域冲突，"
                + "不超过 120 字。";
        String advice = doChat(SAFETY_PROMPT, prompt, DISPATCH_FALLBACK);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderNo", order.getOrderNo());
        result.put("advice", advice);
        // 规则兜底建议始终给出，前端可与 AI 建议对照
        result.put("ruleAdvice", ruleAdvice(order));
        result.put("fromAi", !DISPATCH_FALLBACK.equals(advice));
        return result;
    }

    /** 规则建议：不依赖大模型，任何时候都能给出 */
    private String ruleAdvice(OrderInfo order) {
        StringBuilder sb = new StringBuilder();
        if (order.getGoodsWeight() != null && order.getGoodsWeight().doubleValue() > 5) {
            sb.append("建议选用载重 8kg 以上的机型；");
        } else {
            sb.append("常规 5kg 机型即可满足载重；");
        }
        if (order.getDistanceKm() != null && order.getDistanceKm().doubleValue() > 12) {
            sb.append("航程较长，请确认飞行器电量与续航；");
        }
        if (order.getPriority() != null && order.getPriority() >= 3) {
            sb.append("该订单为紧急优先级，建议优先调度并在起飞前复核空域状态。");
        } else {
            sb.append("起飞前请复核当日空域管制通告。");
        }
        return sb.toString();
    }

    /** 合规检查辅助：给出风险提示清单 */
    public Map<String, Object> complianceCheck(Long orderId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(1001, "订单不存在");
        }
        String prompt = "请检查该低空配送订单的合规风险点：物品类型「" + order.getGoodsType()
                + "」，重量 " + order.getGoodsWeight() + "kg，收件地址「" + order.getReceiveAddress()
                + "」。列出 2-3 条需要人工确认的合规要点，每条不超过 40 字。";
        String check = doChat(SAFETY_PROMPT, prompt, COMPLIANCE_FALLBACK);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("check", check);
        result.put("fromAi", !COMPLIANCE_FALLBACK.equals(check));
        return result;
    }

    // ---------- 配置管理 ----------

    /** 读取配置（apiKey 已脱敏，仅回显后 4 位） */
    public AiConfig getMaskedConfig() {
        AiConfig cfg = rawConfig();
        cfg.setApiKey(mask(cfg.getApiKey()));
        return cfg;
    }

    /** 更新配置：apiKey 传空表示保留原值，不覆盖 */
    public AiConfig updateConfig(AiConfig config) {
        AiConfig exist = rawConfig();
        if (config.getProvider() != null && !config.getProvider().isBlank()) {
            exist.setProvider(config.getProvider());
        }
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            exist.setBaseUrl(config.getBaseUrl());
        }
        if (config.getModel() != null && !config.getModel().isBlank()) {
            exist.setModel(config.getModel());
        }
        // 传空串表示保留原值：页面上看不到完整 Key，也不会误清空
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            exist.setApiKey(config.getApiKey().trim());
        }
        if (config.getTimeoutSec() != null) {
            exist.setTimeoutSec(config.getTimeoutSec());
        }
        if (config.getRateLimitPerMin() != null) {
            exist.setRateLimitPerMin(config.getRateLimitPerMin());
        }
        if (config.getContentFilter() != null) {
            exist.setContentFilter(config.getContentFilter());
        }
        exist.setUpdatedAt(LocalDateTime.now());
        aiConfigMapper.updateById(exist);
        return getMaskedConfig();
    }

    /** 可用性自检 */
    public Map<String, Object> status() {
        AiConfig cfg = rawConfig();
        Map<String, Object> m = new HashMap<>();
        boolean configured = cfg.getApiKey() != null && !cfg.getApiKey().isBlank();
        m.put("configured", configured);
        m.put("provider", cfg.getProvider());
        m.put("model", cfg.getModel());
        m.put("baseUrl", cfg.getBaseUrl());
        return m;
    }

    // ---------- 内部实现 ----------

    private AiConfig rawConfig() {
        AiConfig cfg = aiConfigMapper.selectById(1L);
        if (cfg == null) {
            cfg = new AiConfig();
            cfg.setId(1L);
            cfg.setProvider("deepseek");
            cfg.setBaseUrl("https://api.deepseek.com/v1");
            cfg.setModel("deepseek-chat");
            cfg.setApiKey("");
            cfg.setTimeoutSec(60);
            cfg.setRateLimitPerMin(20);
            cfg.setContentFilter(1);
            aiConfigMapper.insert(cfg);
        }
        return cfg;
    }

    /** 统一对话入口：任何失败都转成 fallback 文案，绝不向上抛异常 */
    private String doChat(String systemPrompt, String userPrompt, String fallback) {
        AiConfig cfg = rawConfig();
        // 三级降级：未配置 apiKey → 1101
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new BizException(1101, "AI 功能未配置，请联系管理员在「系统配置」中填写 API Key");
        }
        String uid = String.valueOf(UserContext.userId() == null ? "anonymous" : UserContext.userId());
        checkRateLimit(cfg, uid);
        try {
            String content = callOpenAi(cfg, systemPrompt, userPrompt);
            return safetyFilter(content, cfg);
        } catch (Exception e) {
            // 一级降级：网络异常 / 超时 / 非 200 → 场景化文案
            log.warn("大模型调用失败，使用降级文案。model={}", cfg.getModel(), e);
            return fallback;
        }
    }

    /** 调用 OpenAI 兼容的 /chat/completions；非 200 一律抛异常交给上层降级 */
    private String callOpenAi(AiConfig cfg, String systemPrompt, String userPrompt) throws Exception {
        String base = cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank()
                ? "https://api.deepseek.com/v1" : cfg.getBaseUrl().replaceAll("/+$", "");
        String url = base + "/chat/completions";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", cfg.getModel() == null ? "deepseek-chat" : cfg.getModel());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        payload.put("messages", messages);
        payload.put("stream", false);

        int timeout = cfg.getTimeoutSec() == null ? 60 : Math.max(5, cfg.getTimeoutSec());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(15, timeout)))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cfg.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new java.io.IOException("大模型返回非 200：" + resp.statusCode());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new java.io.IOException("大模型返回结构异常");
        }
        return content.asText();
    }

    /** 内容安全：命中敏感词替换为安全文案（不是报错） */
    private String safetyFilter(String content, AiConfig cfg) {
        if (content == null) {
            return SENSITIVE_FALLBACK;
        }
        if (cfg.getContentFilter() == null || cfg.getContentFilter() == 0) {
            return content;
        }
        for (String w : SENSITIVE_WORDS) {
            if (content.contains(w)) {
                log.info("AI 返回命中敏感词，已替换为安全文案");
                return SENSITIVE_FALLBACK;
            }
        }
        return content;
    }

    private void checkRateLimit(AiConfig cfg, String userId) {
        int limit = cfg.getRateLimitPerMin() == null ? 20 : Math.max(1, cfg.getRateLimitPerMin());
        // 用 computeIfAbsent 而不是先 get 再 put，避免并发下创建多个限流器导致限流失效
        RateLimiter limiter = limiters.computeIfAbsent(userId, k -> new RateLimiter(limit));
        if (!limiter.tryAcquire()) {
            throw new BizException(1001, "请求太频繁啦，休息一下再试～");
        }
    }

    /** 密钥脱敏：只回显后 4 位 */
    private String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }

    /** 简约每分钟固定窗口限流器 */
    private static class RateLimiter {
        private final int limit;
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger count = new AtomicInteger();

        RateLimiter(int limit) {
            this.limit = limit;
        }

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() >= 60_000) {
                windowStart.set(now);
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
