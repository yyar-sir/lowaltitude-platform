package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.dto.IncidentDTO;
import com.example.eduplatform.entity.Incident;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.IncidentService;
import com.example.eduplatform.service.OperationLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应急与客服管理。
 *
 * <p>三端都可上报；处置（状态流转）仅管理员与商家，且必须按
 * 「待处理 → 处理中 → 已解决」顺序推进。
 */
@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final OperationLogService opLog;

    /** 事件列表：客户看自己上报的，管理员与商家看全部 */
    @GetMapping("/list")
    @RequireRole({0, 1, 2})
    public Result<List<Incident>> list(@RequestParam(required = false) Integer status) {
        Long reporterId = UserContext.role() == 2 ? UserContext.userId() : null;
        return Result.ok(incidentService.list(reporterId, status));
    }

    /** 上报事件 */
    @PostMapping("/report")
    @RequireRole({0, 1, 2})
    public Result<Incident> report(@Valid @RequestBody IncidentDTO dto) {
        return Result.ok(incidentService.report(UserContext.userId(), dto));
    }

    /** 处置事件 */
    @PostMapping("/{id}/handle")
    @RequireRole({0, 1})
    public Result<Incident> handle(@PathVariable Long id,
                                  @RequestParam Integer status,
                                  @RequestParam(required = false) String result) {
        Incident e = incidentService.handle(id, status, result);
        opLog.record("处置应急事件", e.getTitle() + " → 状态 " + e.getStatus());
        return Result.ok(e);
    }
}
