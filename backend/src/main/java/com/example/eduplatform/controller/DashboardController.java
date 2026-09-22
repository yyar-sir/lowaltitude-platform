package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.DashboardService;
import com.example.eduplatform.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据统计与报表（三端共用入口）。
 *
 * <p>同一端点按登录角色返回不同数据范围：
 * 管理员=全平台、商家=本商家、客户=本人订单。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @RequireRole({0, 1, 2})
    public Result<DashboardVO> dashboard() {
        return Result.ok(dashboardService.build(UserContext.userId(), UserContext.role()));
    }
}
