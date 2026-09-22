package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.entity.AirspaceZone;
import com.example.eduplatform.service.AirspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开接口（无需登录）。
 *
 * <p>统一收敛到 /api/public/** 前缀，WebConfig 白名单只需维护一条规则。
 * ⚠️ 放进这个前缀的接口等于对全世界开放，不要在这里返回任何用户数据。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final AirspaceService airspaceService;

    /** 连通性自检：前端与验收脚本用它确认服务存活 */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("app", "天枢·低空智能运营管控平台");
        data.put("time", LocalDateTime.now().toString());
        return Result.ok(data);
    }

    /**
   * 空域公告（公开）。
   *
   * <p>禁飞区与临时管制区属于应向公众告知的信息，因此单独放开；
   * 只返回空域本身，不含任何订单或用户数据。
   */
    @GetMapping("/airspace-notice")
    public Result<List<AirspaceZone>> airspaceNotice() {
        return Result.ok(airspaceService.zones(null, 1));
    }
}
