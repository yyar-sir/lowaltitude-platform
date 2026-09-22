package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.entity.Aircraft;
import com.example.eduplatform.entity.FlightPlan;
import com.example.eduplatform.entity.OrderInfo;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.ViolationRecord;
import com.example.eduplatform.mapper.AircraftMapper;
import com.example.eduplatform.mapper.DispatchRecordMapper;
import com.example.eduplatform.mapper.FlightPlanMapper;
import com.example.eduplatform.mapper.OrderInfoMapper;
import com.example.eduplatform.mapper.UserMapper;
import com.example.eduplatform.mapper.ViolationRecordMapper;
import com.example.eduplatform.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计与报表。
 *
 * <p>同一套出参结构服务三端，差别只在数据范围：
 * 管理员看全平台、商家看自己、客户看自己下单数据——范围在服务端收敛。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderInfoMapper orderMapper;
    private final AircraftMapper aircraftMapper;
    private final DispatchRecordMapper dispatchMapper;
    private final ViolationRecordMapper violationMapper;
    private final FlightPlanMapper planMapper;
    private final UserMapper userMapper;

    private static final String[] STATUS_NAMES =
            {"", "待接单", "待调度", "飞行中", "已送达", "已完成", "已取消"};

    /**
   * 看板数据。
   *
   * @param role 0管理员(全平台) 1商家(自己) 2客户(自己)
   */
    public DashboardVO build(Long userId, Integer role) {
        DashboardVO vo = new DashboardVO();
        LambdaQueryWrapper<OrderInfo> base = scope(role, userId);
        List<OrderInfo> orders = orderMapper.selectList(base);

        // ---------- 顶部数字卡 ----------
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderTotal", orders.size());
        summary.put("orderPending", countStatus(orders, 1));
        summary.put("orderDispatching", countStatus(orders, 2));
        summary.put("orderFlying", countStatus(orders, 3));
        summary.put("orderFinished", countStatus(orders, 5));
        summary.put("orderCanceled", countStatus(orders, 6));
        // 订单完成率 = 已完成 / 非取消订单
        long valid = orders.stream()
                .filter(o -> o.getOrderStatus() != null && o.getOrderStatus() != 6).count();
        long finished = countStatus(orders, 5);
        summary.put("finishRate", valid == 0 ? 0 : Math.round(finished * 100.0 / valid));
        // 营收：已完成的实际费用合计
        BigDecimal revenue = orders.stream()
                .filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == 5)
                .map(o -> o.getActualFee() == null ? BigDecimal.ZERO : o.getActualFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("revenue", revenue);

        if (role != null && role == 0) {
            summary.put("userTotal", userMapper.selectCount(null));
            summary.put("merchantTotal", userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRole, 1)));
            summary.put("customerTotal", userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRole, 2)));
            summary.put("aircraftTotal", aircraftMapper.selectCount(null));
        } else if (role != null && role == 1) {
            summary.put("aircraftTotal", aircraftMapper.selectCount(
                    new LambdaQueryWrapper<Aircraft>().eq(Aircraft::getMerchantId, userId)));
            summary.put("aircraftAvailable", aircraftMapper.selectCount(
                    new LambdaQueryWrapper<Aircraft>()
                            .eq(Aircraft::getMerchantId, userId)
                            .eq(Aircraft::getStatus, 1)));
        }
        vo.setSummary(summary);

        // ---------- 订单状态分布（饼图） ----------
        List<Map<String, Object>> dist = new ArrayList<>();
        for (int st = 1; st <= 6; st++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", STATUS_NAMES[st]);
            item.put("value", countStatus(orders, st));
            dist.add(item);
        }
        vo.setOrderStatusDist(dist);

        // ---------- 近 14 天订单趋势（折线图） ----------
        Map<String, Integer> byDay = new HashMap<>();
        for (OrderInfo o : orders) {
            if (o.getCreateTime() == null) {
                continue;
            }
            String d = o.getCreateTime().toLocalDate().toString();
            byDay.merge(d, 1, Integer::sum);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", d.format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("count", byDay.getOrDefault(d.toString(), 0));
            trend.add(item);
        }
        vo.setOrderTrend(trend);

        // ---------- 飞行器状态分布 ----------
        LambdaQueryWrapper<Aircraft> aq = new LambdaQueryWrapper<>();
        if (role != null && role == 1) {
            aq.eq(Aircraft::getMerchantId, userId);
        }
        List<Aircraft> aircraftList = role != null && role == 2 ? List.of() : aircraftMapper.selectList(aq);
        Map<Integer, String> aircraftStatusName = Map.of(1, "可用", 2, "任务中", 3, "维保中", 4, "停用");
        List<Map<String, Object>> aDist = new ArrayList<>();
        for (Map.Entry<Integer, String> e : aircraftStatusName.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getValue());
            item.put("value", aircraftList.stream()
                    .filter(a -> a.getStatus() != null && a.getStatus().equals(e.getKey())).count());
            aDist.add(item);
        }
        vo.setAircraftStatusDist(aDist);

        // ---------- 飞行器任务数 Top（资源利用率） ----------
        if (!aircraftList.isEmpty()) {
            vo.setAircraftUsage(aircraftUsage(aircraftList));
        } else {
            vo.setAircraftUsage(List.of());
        }

        // ---------- 违规类型分布 ----------
        LambdaQueryWrapper<ViolationRecord> vq = new LambdaQueryWrapper<>();
        if (role != null && role == 1) {
            vq.eq(ViolationRecord::getMerchantId, userId);
        }
        List<ViolationRecord> violations = role != null && role == 2
                ? List.of() : violationMapper.selectList(vq);
        Map<String, Integer> vMap = new HashMap<>();
        for (ViolationRecord v : violations) {
            vMap.merge(v.getViolationType() == null ? "其他" : v.getViolationType(), 1, Integer::sum);
        }
        List<Map<String, Object>> vDist = new ArrayList<>();
        vMap.forEach((k, v) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", k);
            item.put("value", v);
            vDist.add(item);
        });
        vo.setViolationDist(vDist);

        // ---------- 待办提醒 ----------
        vo.setTodos(todos(userId, role, orders));
        return vo;
    }

    private List<Map<String, Object>> aircraftUsage(List<Aircraft> aircraftList) {
        List<Map<String, Object>> usage = new ArrayList<>();
        for (Aircraft a : aircraftList) {
            Long c = dispatchMapper.selectCount(new LambdaQueryWrapper<com.example.eduplatform.entity.DispatchRecord>()
                    .eq(com.example.eduplatform.entity.DispatchRecord::getAircraftId, a.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", a.getAircraftNo());
            item.put("value", c == null ? 0 : c);
            usage.add(item);
        }
        usage.sort((x, y) -> Long.compare(((Number) y.get("value")).longValue(),
                ((Number) x.get("value")).longValue()));
        return usage.size() > 6 ? usage.subList(0, 6) : usage;
    }

    private List<String> todos(Long userId, Integer role, List<OrderInfo> orders) {
        List<String> todos = new ArrayList<>();
        if (role == null) {
            return todos;
        }
        if (role == 0) {
            Long pendingPlans = planMapper.selectCount(new LambdaQueryWrapper<FlightPlan>()
                    .eq(FlightPlan::getStatus, 0));
            if (pendingPlans != null && pendingPlans > 0) {
                todos.add("有 " + pendingPlans + " 条飞行计划待审批");
            }
            Long pendingViolations = violationMapper.selectCount(new LambdaQueryWrapper<ViolationRecord>()
                    .eq(ViolationRecord::getHandleStatus, 0));
            if (pendingViolations != null && pendingViolations > 0) {
                todos.add("有 " + pendingViolations + " 条违规记录待处置");
            }
            long flying = countStatus(orders, 3);
            if (flying > 0) {
                todos.add("当前有 " + flying + " 个订单正在飞行中");
            }
        } else if (role == 1) {
            long pending = countStatus(orders, 1);
            if (pending > 0) {
                todos.add("有 " + pending + " 个订单待接单");
            }
            long dispatching = countStatus(orders, 2);
            if (dispatching > 0) {
                todos.add("有 " + dispatching + " 个订单待调度");
            }
            long flying = countStatus(orders, 3);
            if (flying > 0) {
                todos.add("有 " + flying + " 个订单飞行中，请关注异常预警");
            }
        } else {
            long flying = countStatus(orders, 3);
            if (flying > 0) {
                todos.add("你有 " + flying + " 个订单正在配送中");
            }
            long arrived = countStatus(orders, 4);
            if (arrived > 0) {
                todos.add("有 " + arrived + " 个订单已送达，待支付结算");
            }
        }
        return todos;
    }

    /** 数据范围收敛：客户=自己的订单，商家=自己商家的订单，管理员=全部 */
    private LambdaQueryWrapper<OrderInfo> scope(Integer role, Long userId) {
        LambdaQueryWrapper<OrderInfo> q = new LambdaQueryWrapper<>();
        if (role != null && role == 2) {
            q.eq(OrderInfo::getCustomerId, userId);
        } else if (role != null && role == 1) {
            q.eq(OrderInfo::getMerchantId, userId);
        }
        return q;
    }

    private long countStatus(List<OrderInfo> orders, int status) {
        return orders.stream()
                .filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == status)
                .count();
    }
}
