package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.dto.DispatchDTO;
import com.example.eduplatform.dto.OrderCreateDTO;
import com.example.eduplatform.dto.SettleDTO;
import com.example.eduplatform.entity.OrderSettlement;
import com.example.eduplatform.entity.OrderTrack;
import com.example.eduplatform.security.LoginUser;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.OrderService;
import com.example.eduplatform.service.ResourceService;
import com.example.eduplatform.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单全流程管理接口（平台核心业务模块）。
 *
 * <p>按业务阶段划分端点：
 * <ul>
 *   <li>创建：仅终端客户（role=2）</li>
 *   <li>承接与调度：仅物流运营商家（role=1）</li>
 *   <li>跟踪与结算：客户与商家都可读，结算写操作仅客户</li>
 * </ul>
 * 角色范围在服务层按登录态收敛，前端隐藏菜单只是体验层。
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ResourceService resourceService;

    // ---------- 一、订单创建（客户） ----------

    /** 创建订单：校验空域与地址 → 预估费用 → 生成正式订单 */
    @PostMapping("/create")
    @RequireRole(2)
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(UserContext.userId(), dto));
    }

    /** 客户取消订单 */
    @PostMapping("/{id}/cancel")
    @RequireRole(2)
    public Result<Void> cancel(@PathVariable Long id,
                              @RequestParam(required = false) String reason) {
        orderService.cancel(UserContext.userId(), id, reason);
        return Result.ok();
    }

    // ---------- 二、订单承接（商家） ----------

    /** 待接单列表 */
    @GetMapping("/pending")
    @RequireRole({0, 1})
    public Result<List<OrderVO>> pending() {
        return Result.ok(orderService.pendingList());
    }

    /** 接单 */
    @PostMapping("/{id}/accept")
    @RequireRole(1)
    public Result<OrderVO> accept(@PathVariable Long id) {
        return Result.ok(orderService.accept(UserContext.userId(), id));
    }

    /** 拒单（必须填原因） */
    @PostMapping("/{id}/reject")
    @RequireRole(1)
    public Result<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        orderService.reject(UserContext.userId(), id, reason);
        return Result.ok();
    }

    // ---------- 三、订单调度（商家） ----------

    /** 调度页面用的可用资源：飞行器 / 场站 / 机长 */
    @GetMapping("/{id}/dispatch-options")
    @RequireRole({0, 1})
    public Result<java.util.Map<String, Object>> dispatchOptions(@PathVariable Long id) {
        OrderVO order = orderService.detail(id, UserContext.userId(), UserContext.role());
        Long merchantId = order.getMerchantId() != null ? order.getMerchantId() : UserContext.userId();
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("aircraft", resourceService.availableAircraft(merchantId, order.getGoodsWeight()));
        m.put("stations", resourceService.stationList(merchantId));
        m.put("pilots", resourceService.pilotList(merchantId).stream()
                .filter(p -> p.getStatus() == null || p.getStatus() != 2).toList());
        return Result.ok(m);
    }

    /** 生成调度方案并把订单推进到飞行中 */
    @PostMapping("/dispatch")
    @RequireRole(1)
    public Result<OrderVO> dispatch(@RequestBody DispatchDTO dto) {
        return Result.ok(orderService.dispatch(UserContext.userId(), dto));
    }

    /** 模拟推进飞行（演示用，等价于飞行器持续上报位置） */
    @PostMapping("/{id}/simulate-flight")
    @RequireRole(1)
    public Result<OrderVO> simulateFlight(@PathVariable Long id) {
        return Result.ok(orderService.simulateFlight(UserContext.userId(), id));
    }

    /** 标记送达 */
    @PostMapping("/{id}/arrive")
    @RequireRole(1)
    public Result<OrderVO> arrive(@PathVariable Long id) {
        return Result.ok(orderService.arrive(UserContext.userId(), id));
    }

    // ---------- 四、订单跟踪 ----------

    /** 跟踪数据（轨迹点，供地图绘制） */
    @GetMapping("/{id}/tracks")
    @RequireRole({0, 1, 2})
    public Result<List<OrderTrack>> tracks(@PathVariable Long id) {
        LoginUser u = UserContext.get();
        return Result.ok(orderService.tracks(id, u.getId(), u.getRole()));
    }

    /** 上报跟踪点（真实系统由飞行器终端上报，此处保留接口便于联调） */
    @PostMapping("/{id}/track")
    @RequireRole({0, 1})
    public Result<OrderTrack> reportTrack(@PathVariable Long id,
                                        @RequestParam BigDecimal longitude,
                                        @RequestParam BigDecimal latitude,
                                        @RequestParam(required = false) Integer battery,
                                        @RequestParam(required = false) Integer flightStatus,
                                        @RequestParam(required = false) Integer abnormalType,
                                        @RequestParam(required = false) String abnormalDesc) {
        return Result.ok(orderService.reportTrack(id, longitude, latitude, battery,
                flightStatus, abnormalType, abnormalDesc));
    }

    // ---------- 五、结算与评价 ----------

    /** 结算并评价（客户） */
    @PostMapping("/settle")
    @RequireRole(2)
    public Result<OrderSettlement> settle(@RequestBody SettleDTO dto) {
        return Result.ok(orderService.settle(UserContext.userId(), dto));
    }

    /** 结算详情 */
    @GetMapping("/{id}/settlement")
    @RequireRole({0, 1, 2})
    public Result<OrderSettlement> settlement(@PathVariable Long id) {
        LoginUser u = UserContext.get();
        return Result.ok(orderService.settlementOf(id, u.getId(), u.getRole()));
    }

    // ---------- 查询 ----------

    /** 订单列表：按角色自动收敛数据范围 */
    @GetMapping("/list")
    @RequireRole({0, 1, 2})
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) String keyword) {
        LoginUser u = UserContext.get();
        return Result.ok(orderService.list(u.getId(), u.getRole(), status, keyword));
    }

    /** 订单详情（含状态时间轴与调度摘要） */
    @GetMapping("/{id}")
    @RequireRole({0, 1, 2})
    public Result<OrderVO> detail(@PathVariable Long id) {
        LoginUser u = UserContext.get();
        return Result.ok(orderService.detail(id, u.getId(), u.getRole()));
    }
}
