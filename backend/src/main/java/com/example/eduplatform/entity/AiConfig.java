package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** AI 接口配置表（单行） */
@Data
@TableName("ai_config")
public class AiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务商 */
    private String provider;

    /**
     * 密钥（仅后端持有）。
     * ⚠️ 这里刻意<b>不加</b> @JsonIgnore：管理端查询配置时需要回显脱敏后的值（****后4位），
     *    脱敏在 AiService.getMaskedConfig() 里统一处理；整个实体从不直接返回给前端。
     */
    private String apiKey;

    /** Base URL */
    private String baseUrl;

    /** 模型名 */
    private String model;

    /** 超时（秒） */
    private Integer timeoutSec;

    /** 最大并发 */
    private Integer maxConcurrency;

    /** 每人每分钟限流 */
    private Integer rateLimitPerMin;

    /** 内容安全开关 */
    private Integer contentFilter;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
