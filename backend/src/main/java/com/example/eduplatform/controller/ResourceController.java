package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.entity.Aircraft;
import com.example.eduplatform.entity.Pilot;
import com.example.eduplatform.entity.PricingRule;
import com.example.eduplatform.entity.Station;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 低空资源管理：飞行器台账、起降场站、作业人员、定价规则。
 *
 * <p>写操作仅物流运营商家（role=1）；管理员与商家可读（管理员用于全局监管）。
 * 所有查询按登录商家的 merchantId 收敛，见 ResourceService。
 */
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /** 资源总览（商家工作台） */
    @GetMapping("/overview")
    @RequireRole({0, 1})
    public Result<Map<String, Object>> overview() {
        return Result.ok(resourceService.overview(UserContext.userId()));
    }

    // ---------- 飞行器 ----------

    @GetMapping("/aircraft")
    @RequireRole({0, 1})
    public Result<List<Aircraft>> aircraft(@RequestParam(required = false) Integer status) {
        return Result.ok(resourceService.aircraftList(UserContext.userId(), status));
    }

    @PostMapping("/aircraft")
    @RequireRole(1)
    public Result<Aircraft> saveAircraft(@RequestBody Aircraft dto) {
        return Result.ok(resourceService.saveAircraft(UserContext.userId(), dto));
    }

    /** 飞行器状态流转：1可用 2任务中 3维保中 4停用 */
    @PostMapping("/aircraft/{id}/status")
    @RequireRole(1)
    public Result<Aircraft> changeAircraftStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(resourceService.changeAircraftStatus(UserContext.userId(), id, status));
    }

    // ---------- 起降场站 ----------

    @GetMapping("/station")
    @RequireRole({0, 1})
    public Result<List<Station>> stations() {
        return Result.ok(resourceService.stationList(UserContext.userId()));
    }

    @PostMapping("/station")
    @RequireRole(1)
    public Result<Station> saveStation(@RequestBody Station dto) {
        return Result.ok(resourceService.saveStation(UserContext.userId(), dto));
    }

    // ---------- 作业人员 ----------

    @GetMapping("/pilot")
    @RequireRole({0, 1})
    public Result<List<Pilot>> pilots() {
        return Result.ok(resourceService.pilotList(UserContext.userId()));
    }

    @PostMapping("/pilot")
    @RequireRole(1)
    public Result<Pilot> savePilot(@RequestBody Pilot dto) {
        return Result.ok(resourceService.savePilot(UserContext.userId(), dto));
    }

    // ---------- 定价规则 ----------

    @GetMapping("/pricing")
    @RequireRole({0, 1})
    public Result<List<PricingRule>> pricing() {
        return Result.ok(resourceService.pricingList(UserContext.userId()));
    }

    @PostMapping("/pricing")
    @RequireRole(1)
    public Result<PricingRule> savePricing(@RequestBody PricingRule dto) {
        return Result.ok(resourceService.savePricing(UserContext.userId(), dto));
    }
}
