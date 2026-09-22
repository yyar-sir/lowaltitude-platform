package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.dto.FlightPlanDTO;
import com.example.eduplatform.entity.AirspaceZone;
import com.example.eduplatform.entity.FlightPlan;
import com.example.eduplatform.entity.ViolationRecord;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.AirspaceService;
import com.example.eduplatform.service.OperationLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 空域合规运营：空域规则、飞行计划申报与审批、违规记录。
 *
 * <p>权限边界：空域规则维护与计划审批仅管理员；计划申报与违规查询面向商家。
 */
@RestController
@RequestMapping("/api/airspace")
@RequiredArgsConstructor
public class AirspaceController {

    private final AirspaceService airspaceService;
    private final OperationLogService opLog;

    // ---------- 空域规则（三端可读：客户下单时也会用到空域校验结果） ----------

    @GetMapping("/zones")
    @RequireRole({0, 1, 2})
    public Result<List<AirspaceZone>> zones(@RequestParam(required = false) Integer zoneType,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(airspaceService.zones(zoneType, status));
    }

    @PostMapping("/zones")
    @RequireRole(0)
    public Result<AirspaceZone> saveZone(@RequestBody AirspaceZone dto) {
        AirspaceZone saved = airspaceService.saveZone(dto);
        opLog.record("空域规则维护", "保存空域「" + saved.getName() + "」");
        return Result.ok(saved);
    }

    @DeleteMapping("/zones/{id}")
    @RequireRole(0)
    public Result<Void> deleteZone(@PathVariable Long id) {
        airspaceService.deleteZone(id);
        opLog.record("空域规则维护", "删除空域 id=" + id);
        return Result.ok();
    }

    // ---------- 飞行计划 ----------

    /** 计划列表：商家看自己的，管理员看全部 */
    @GetMapping("/plans")
    @RequireRole({0, 1})
    public Result<List<FlightPlan>> plans(@RequestParam(required = false) Integer status) {
        Long merchantId = UserContext.role() == 0 ? null : UserContext.userId();
        return Result.ok(airspaceService.planList(merchantId, status));
    }

    /** 商家申报飞行计划 */
    @PostMapping("/plans")
    @RequireRole(1)
    public Result<FlightPlan> submitPlan(@Valid @RequestBody FlightPlanDTO dto) {
        return Result.ok(airspaceService.submit(UserContext.userId(), dto));
    }

    /** 管理员审批 */
    @PostMapping("/plans/{id}/review")
    @RequireRole(0)
    public Result<FlightPlan> review(@PathVariable Long id,
                                    @RequestParam boolean pass,
                                    @RequestParam(required = false) String reason) {
        FlightPlan plan = airspaceService.review(UserContext.userId(), id, pass, reason);
        opLog.record(pass ? "通过飞行计划" : "驳回飞行计划",
                "计划 " + plan.getPlanNo() + (pass ? " 已通过" : " 已驳回：" + reason));
        return Result.ok(plan);
    }

    // ---------- 违规记录 ----------

    @GetMapping("/violations")
    @RequireRole({0, 1})
    public Result<List<ViolationRecord>> violations(@RequestParam(required = false) Integer handleStatus) {
        Long merchantId = UserContext.role() == 0 ? null : UserContext.userId();
        return Result.ok(airspaceService.violations(merchantId, handleStatus));
    }

    @PostMapping("/violations/{id}/handle")
    @RequireRole(0)
    public Result<ViolationRecord> handleViolation(@PathVariable Long id,
                                                  @RequestParam(required = false) String result) {
        ViolationRecord v = airspaceService.handleViolation(id, result);
        opLog.record("处置违规记录", v.getViolationType() + " → " + result);
        return Result.ok(v);
    }
}
