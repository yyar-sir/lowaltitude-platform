package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.dto.DispatchDTO;
import com.example.eduplatform.dto.OrderCreateDTO;
import com.example.eduplatform.dto.SettleDTO;
import com.example.eduplatform.entity.Aircraft;
import com.example.eduplatform.entity.AirspaceZone;
import com.example.eduplatform.entity.DispatchRecord;
import com.example.eduplatform.entity.OrderInfo;
import com.example.eduplatform.entity.OrderSettlement;
import com.example.eduplatform.entity.OrderTrack;
import com.example.eduplatform.entity.Pilot;
import com.example.eduplatform.entity.PricingRule;
import com.example.eduplatform.entity.Station;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.mapper.AircraftMapper;
import com.example.eduplatform.mapper.AirspaceZoneMapper;
import com.example.eduplatform.mapper.DispatchRecordMapper;
import com.example.eduplatform.mapper.OrderInfoMapper;
import com.example.eduplatform.mapper.OrderSettlementMapper;
import com.example.eduplatform.mapper.OrderTrackMapper;
import com.example.eduplatform.mapper.PilotMapper;
import com.example.eduplatform.mapper.PricingRuleMapper;
import com.example.eduplatform.mapper.StationMapper;
import com.example.eduplatform.mapper.UserMapper;
import com.example.eduplatform.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 订单全流程管理（平台核心业务模块）。
 *
 * <p>覆盖「创建 → 承接 → 调度 → 跟踪 → 结算」五个阶段，状态流转规则集中在本类维护：
 * <pre>
 * 1 待接单 --接单--> 2 待调度 --调度--> 3 飞行中 --送达--> 4 已送达 --结算--> 5 已完成
 *    |                    |
 *    +---拒单------------> 6 已取消
 * </pre>
 * <b>状态只能按上述规则流转</b>：每个变更方法都先校验当前状态，非法跳转直接拒绝，
 * 这样即使前端被绕过，也无法造出「待接单直接结算」这类脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderInfoMapper orderMapper;
    private final DispatchRecordMapper dispatchMapper;
    private final OrderTrackMapper trackMapper;
    private final OrderSettlementMapper settlementMapper;
    private final PricingRuleMapper pricingMapper;
    private final AircraftMapper aircraftMapper;
    private final StationMapper stationMapper;
    private final PilotMapper pilotMapper;
    private final AirspaceZoneMapper zoneMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    /** 订单状态 */
    public static final int ST_PENDING = 1;
    public static final int ST_DISPATCHING = 2;
    public static final int ST_FLYING = 3;
    public static final int ST_ARRIVED = 4;
    public static final int ST_FINISHED = 5;
    public static final int ST_CANCELED = 6;

    private static final String[] STATUS_NAMES =
            {"", "待接单", "待调度", "飞行中", "已送达", "已完成", "已取消"};
    private static final String[] DELIVERY_NAMES = {"", "普通配送", "加急配送"};
    private static final String[] PRIORITY_NAMES = {"", "普通", "较高", "紧急"};

    private final Random random = new Random();

    // ==================== 一、订单创建 ====================

    /**
     * 客户下单。
     *
     * <p>下单时完成三件事：地址与物品合规校验、按商家定价规则预估费用、生成正式订单。
     * 校验不通过直接抛业务异常，前端停留在当前步骤并给出具体原因。
     */
    @Transactional
    public OrderVO create(Long customerId, OrderCreateDTO dto) {
        // ① 地址校验：寄收地址不能相同，且不能为空
        if (dto.getSendAddress().trim().equals(dto.getReceiveAddress().trim())) {
            throw new BizException(1001, "寄件地址与收件地址不能相同");
        }
        // ② 空域校验：收件地址若落在禁飞区范围内则拒单
        checkAirspace(dto.getReceiveAddress());

        // ③ 匹配商家：指定则校验，未指定则取第一个可用的商家
        Long merchantId = dto.getMerchantId();
        if (merchantId == null) {
            User merchant = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, 1)
                    .eq(User::getStatus, 1)
                    .orderByAsc(User::getId)
                    .last("limit 1"));
            if (merchant == null) {
                throw new BizException(1001, "当前没有可用的运营商家，请稍后再试");
            }
            merchantId = merchant.getId();
        } else {
            User merchant = userMapper.selectById(merchantId);
            if (merchant == null || merchant.getRole() == null || merchant.getRole() != 1
                    || (merchant.getStatus() != null && merchant.getStatus() == 0)) {
                throw new BizException(1001, "所选商家不可用");
            }
        }

        // ④ 费用预估：按商家启用的定价规则计算
        int deliveryType = dto.getDeliveryType() == null ? 1 : dto.getDeliveryType();
        BigDecimal distance = estimateDistance(dto.getSendAddress(), dto.getReceiveAddress());
        BigDecimal fee = calcFee(merchantId, distance, deliveryType);

        OrderInfo order = new OrderInfo();
        order.setOrderNo(genOrderNo());
        order.setCustomerId(customerId);
        order.setMerchantId(merchantId);
        order.setSendAddress(dto.getSendAddress().trim());
        order.setReceiveAddress(dto.getReceiveAddress().trim());
        order.setGoodsType(dto.getGoodsType());
        order.setGoodsWeight(dto.getGoodsWeight());
        order.setDeliveryType(deliveryType);
        order.setPriority(dto.getPriority() == null ? 1 : dto.getPriority());
        order.setDistanceKm(distance);
        order.setEstimateFee(fee);
        order.setOrderStatus(ST_PENDING);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 副作用：通知商家有新订单
        messageService.send(merchantId, "order", "新订单待接单",
                "订单 " + order.getOrderNo() + " 等待接单", "/merchant/orders");
        return toVO(order);
    }

    /** 空域校验：命中禁飞区/临时管制区则拒绝下单 */
    private void checkAirspace(String address) {
        List<AirspaceZone> zones = zoneMapper.selectList(new LambdaQueryWrapper<AirspaceZone>()
                .eq(AirspaceZone::getStatus, 1)
                .in(AirspaceZone::getZoneType, List.of(2, 3)));
        for (AirspaceZone z : zones) {
            // 演示环境用地址关键字匹配空域名称，真实系统应做地理围栏坐标判定
            if (z.getName() != null && address.contains(z.getName().replace("区", "").replace("核心", ""))) {
                throw new BizException(1001, "收件地址位于「" + z.getName() + "」范围内，暂不支持配送");
            }
        }
        if (address.contains("机场") || address.contains("禁飞")) {
            throw new BizException(1001, "该地址不在服务空域范围内，请更换收件地址");
        }
    }

    /**
     * 航程估算。
     *
     * <p>演示环境缺少真实路网数据，用地址长度差做稳定可复现的估算
     * （同一对地址每次算出同一距离，便于验证费用计算是幂等的）。
     */
    private BigDecimal estimateDistance(String from, String to) {
        int seed = Math.abs(from.hashCode() - to.hashCode()) % 120;
        BigDecimal d = BigDecimal.valueOf(3 + seed / 10.0);
        return d.setScale(2, RoundingMode.HALF_UP);
    }

    /** 费用计算：基础配送费 + 航程费 + 空域使用费（+ 加急费） */
    private BigDecimal calcFee(Long merchantId, BigDecimal distance, int deliveryType) {
        PricingRule rule = pricingMapper.selectOne(new LambdaQueryWrapper<PricingRule>()
                .eq(PricingRule::getMerchantId, merchantId)
                .eq(PricingRule::getStatus, 1)
                .orderByAsc(PricingRule::getId)
                .last("limit 1"));
        if (rule == null) {
            // 商家未配置定价规则时用平台默认值兜底，避免下单直接被卡住
            rule = new PricingRule();
            rule.setBaseFee(BigDecimal.valueOf(20));
            rule.setPerKmFee(BigDecimal.valueOf(3));
            rule.setAirspaceFee(BigDecimal.valueOf(5));
            rule.setUrgentFee(BigDecimal.valueOf(8));
        }
        BigDecimal fee = rule.getBaseFee()
                .add(rule.getPerKmFee().multiply(distance))
                .add(rule.getAirspaceFee());
        if (deliveryType == 2) {
            fee = fee.add(rule.getUrgentFee());
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private String genOrderNo() {
        return "TS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (100 + random.nextInt(900));
    }

    // ==================== 二、订单承接 ====================

    /**
     * 商家接单：待接单 → 待调度。
     *
     * <p>用状态条件更新（where order_status=1）保证并发下只有一个商家能抢到单，
     * 而不是先查后改——后者在并发时两个商家都能接单。
     */
    @Transactional
    public OrderVO accept(Long merchantId, Long orderId) {
        OrderInfo order = mustGet(orderId);
        if (order.getMerchantId() != null && !order.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "该订单不属于你的商家");
        }
        if (order.getOrderStatus() != ST_PENDING) {
            throw new BizException(1001, "订单当前状态为「" + statusName(order.getOrderStatus()) + "」，无法接单");
        }
        OrderInfo update = new OrderInfo();
        update.setId(orderId);
        update.setMerchantId(merchantId);
        update.setOrderStatus(ST_DISPATCHING);
        update.setAcceptTime(LocalDateTime.now());
        int rows = orderMapper.update(update, new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getOrderStatus, ST_PENDING));
        if (rows == 0) {
            throw new BizException(1001, "订单已被其他商家接单");
        }
        messageService.send(order.getCustomerId(), "order", "订单已接单",
                "订单 " + order.getOrderNo() + " 已被接单，正在安排调度", "/customer/orders");
        return toVO(mustGet(orderId));
    }

    /** 商家拒单：待接单 → 已取消（必须填写原因，会同步给客户） */
    @Transactional
    public void reject(Long merchantId, Long orderId, String reason) {
        OrderInfo order = mustGet(orderId);
        if (order.getOrderStatus() != ST_PENDING) {
            throw new BizException(1001, "只有待接单的订单可以拒单");
        }
        if (reason == null || reason.isBlank()) {
            throw new BizException(1002, "拒单必须填写原因");
        }
        OrderInfo update = new OrderInfo();
        update.setId(orderId);
        update.setOrderStatus(ST_CANCELED);
        update.setRejectReason(reason);
        orderMapper.updateById(update);
        messageService.send(order.getCustomerId(), "order", "订单被拒单",
                "订单 " + order.getOrderNo() + " 未接单，原因：" + reason, "/customer/orders");
    }

    /** 客户取消订单：仅待接单 / 待调度 可取消 */
    @Transactional
    public void cancel(Long customerId, Long orderId, String reason) {
        OrderInfo order = mustGet(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new BizException(403, "只能取消自己的订单");
        }
        if (order.getOrderStatus() != ST_PENDING && order.getOrderStatus() != ST_DISPATCHING) {
            throw new BizException(1001, "订单已在飞行中或已完成，无法取消");
        }
        OrderInfo update = new OrderInfo();
        update.setId(orderId);
        update.setOrderStatus(ST_CANCELED);
        update.setRejectReason(reason == null ? "客户主动取消" : reason);
        orderMapper.updateById(update);
        if (order.getMerchantId() != null) {
            messageService.send(order.getMerchantId(), "order", "客户取消了订单",
                    "订单 " + order.getOrderNo() + " 已被客户取消", "/merchant/orders");
        }
    }

    // ==================== 三、订单调度 ====================

    /**
     * 生成调度方案：待调度 → 飞行中。
     *
     * <p>调度前校验三件事：飞行器可用且载重足够、机长在岗、空域无冲突。
     * 任一不满足都拒绝，并把具体原因返回给调度员。
     */
    @Transactional
    public OrderVO dispatch(Long merchantId, DispatchDTO dto) {
        OrderInfo order = mustGet(dto.getOrderId());
        if (order.getMerchantId() == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "只能调度自己商家的订单");
        }
        if (order.getOrderStatus() != ST_DISPATCHING) {
            throw new BizException(1001, "订单当前状态为「" + statusName(order.getOrderStatus()) + "」，无法调度");
        }

        // ① 飞行器：可用 + 载重足够 + 电量充足
        Aircraft aircraft = pickAircraft(merchantId, dto.getAircraftId(), order.getGoodsWeight());
        // ② 机长：在岗
        Pilot pilot = pickPilot(merchantId, dto.getPilotId());
        // ③ 场站
        Station station = pickStation(merchantId, dto.getStationId());

        String path = dto.getFlightPath() != null && !dto.getFlightPath().isBlank()
                ? dto.getFlightPath()
                : planPath(station, order.getReceiveAddress());

        DispatchRecord record = new DispatchRecord();
        record.setOrderId(order.getId());
        record.setAircraftId(aircraft.getId());
        record.setAircraftNo(aircraft.getAircraftNo());
        record.setStationId(station == null ? null : station.getId());
        record.setPilotId(pilot == null ? null : pilot.getId());
        record.setPilotName(pilot == null ? null : pilot.getName());
        record.setFlightPath(path);
        record.setPlanTakeoffTime(LocalDateTime.now());
        // 预计送达：按航程粗估，每公里约 1.5 分钟
        int minutes = Math.max(5, order.getDistanceKm() == null ? 10
                : order.getDistanceKm().multiply(BigDecimal.valueOf(1.5)).intValue());
        record.setPlanArriveTime(LocalDateTime.now().plusMinutes(minutes));
        record.setDispatchStatus(2);
        record.setDispatchTime(LocalDateTime.now());
        dispatchMapper.insert(record);

        // 飞行器与机长转为任务中，避免被重复调度
        updateAircraftStatus(aircraft.getId(), 2);
        if (pilot != null) {
            Pilot p = new Pilot();
            p.setId(pilot.getId());
            p.setStatus(2);
            pilotMapper.updateById(p);
        }

        OrderInfo update = new OrderInfo();
        update.setId(order.getId());
        update.setOrderStatus(ST_FLYING);
        orderMapper.updateById(update);

        // 写入首个跟踪点，订单跟踪页立刻有数据可展示
        insertTrack(order.getId(), aircraft.getAircraftNo(), 1, 0, null);

        messageService.send(order.getCustomerId(), "order", "订单已起飞",
                "订单 " + order.getOrderNo() + " 已安排飞行器 " + aircraft.getAircraftNo() + " 配送",
                "/customer/track/" + order.getId());
        return toVO(mustGet(order.getId()));
    }

    private Aircraft pickAircraft(Long merchantId, Long aircraftId, BigDecimal weight) {
        Aircraft aircraft;
        if (aircraftId != null) {
            aircraft = aircraftMapper.selectById(aircraftId);
            if (aircraft == null || !aircraft.getMerchantId().equals(merchantId)) {
                throw new BizException(1001, "所选飞行器不存在或不属于你的商家");
            }
        } else {
            aircraft = aircraftMapper.selectOne(new LambdaQueryWrapper<Aircraft>()
                    .eq(Aircraft::getMerchantId, merchantId)
                    .eq(Aircraft::getStatus, 1)
                    .ge(Aircraft::getMaxPayload, weight)
                    .ge(Aircraft::getBattery, 30)
                    .orderByAsc(Aircraft::getId)
                    .last("limit 1"));
            if (aircraft == null) {
                throw new BizException(1001, "没有满足载重与电量要求的可用飞行器");
            }
        }
        if (aircraft.getStatus() != null && aircraft.getStatus() != 1) {
            throw new BizException(1001, "飞行器 " + aircraft.getAircraftNo() + " 当前不可用（维保中或执行任务中）");
        }
        if (aircraft.getMaxPayload() != null && weight != null
                && aircraft.getMaxPayload().compareTo(weight) < 0) {
            throw new BizException(1001, "飞行器 " + aircraft.getAircraftNo() + " 载重不足（最大 "
                    + aircraft.getMaxPayload() + "kg，订单 " + weight + "kg）");
        }
        if (aircraft.getBattery() != null && aircraft.getBattery() < 30) {
            throw new BizException(1001, "飞行器 " + aircraft.getAircraftNo() + " 电量不足 30%，请先换电");
        }
        return aircraft;
    }

    private Pilot pickPilot(Long merchantId, Long pilotId) {
        if (pilotId != null) {
            Pilot p = pilotMapper.selectById(pilotId);
            if (p == null || !p.getMerchantId().equals(merchantId)) {
                throw new BizException(1001, "所选机长不存在或不属于你的商家");
            }
            if (p.getStatus() != null && p.getStatus() == 2) {
                throw new BizException(1001, "机长 " + p.getName() + " 正在执行其他任务");
            }
            return p;
        }
        // 未指定则取第一个在岗机长；没有也不阻断调度（可无人值守自动飞行）
        return pilotMapper.selectOne(new LambdaQueryWrapper<Pilot>()
                .eq(Pilot::getMerchantId, merchantId)
                .eq(Pilot::getStatus, 1)
                .orderByAsc(Pilot::getId)
                .last("limit 1"));
    }

    private Station pickStation(Long merchantId, Long stationId) {
        if (stationId != null) {
            Station s = stationMapper.selectById(stationId);
            if (s == null || !s.getMerchantId().equals(merchantId)) {
                throw new BizException(1001, "所选场站不存在或不属于你的商家");
            }
            return s;
        }
        return stationMapper.selectOne(new LambdaQueryWrapper<Station>()
                .eq(Station::getMerchantId, merchantId)
                .eq(Station::getStatus, 1)
                .orderByAsc(Station::getId)
                .last("limit 1"));
    }

    private void updateAircraftStatus(Long aircraftId, int status) {
        Aircraft a = new Aircraft();
        a.setId(aircraftId);
        a.setStatus(status);
        aircraftMapper.updateById(a);
    }

    /** 自动规划飞行路径（演示用：场站 → 收件地址） */
    private String planPath(Station station, String receiveAddress) {
        String from = station == null ? "默认起降点" : station.getStationNo();
        return from + "→" + receiveAddress;
    }

    // ==================== 四、订单跟踪 ====================

    /**
     * 上报飞行跟踪点。
     *
     * <p>只允许飞行中的订单上报；异常类型非 0 时同步给客户与商家各发一条预警消息。
     */
    @Transactional
    public OrderTrack reportTrack(Long orderId, BigDecimal lng, BigDecimal lat,
                                  Integer battery, Integer flightStatus,
                                  Integer abnormalType, String abnormalDesc) {
        OrderInfo order = mustGet(orderId);
        if (order.getOrderStatus() != ST_FLYING) {
            throw new BizException(1001, "只有飞行中的订单可以上报跟踪数据");
        }
        DispatchRecord latest = latestDispatch(orderId);
        String aircraftNo = latest == null ? null : latest.getAircraftNo();
        OrderTrack track = insertTrack(orderId, aircraftNo,
                flightStatus == null ? 2 : flightStatus,
                abnormalType == null ? 0 : abnormalType, abnormalDesc);
        if (track.getAbnormalType() != null && track.getAbnormalType() != 0) {
            String tip = track.getAbnormalDesc() == null ? "飞行异常" : track.getAbnormalDesc();
            messageService.send(order.getCustomerId(), "order", "订单异常提醒",
                    "订单 " + order.getOrderNo() + "：" + tip, "/customer/track/" + orderId);
            if (order.getMerchantId() != null) {
                messageService.send(order.getMerchantId(), "order", "订单异常提醒",
                        "订单 " + order.getOrderNo() + "：" + tip, "/merchant/orders");
            }
        }
        return track;
    }

    private OrderTrack insertTrack(Long orderId, String aircraftNo, int flightStatus,
                                   int abnormalType, String abnormalDesc) {
        OrderTrack t = new OrderTrack();
        t.setOrderId(orderId);
        t.setAircraftNo(aircraftNo);
        // 演示环境围绕深圳生成抖动坐标；真实系统由飞行器终端上报
        t.setLongitude(BigDecimal.valueOf(113.95 + (random.nextDouble() - 0.5) * 0.06)
                .setScale(6, RoundingMode.HALF_UP));
        t.setLatitude(BigDecimal.valueOf(22.54 + (random.nextDouble() - 0.5) * 0.06)
                .setScale(6, RoundingMode.HALF_UP));
        t.setBattery(60 + random.nextInt(40));
        t.setFlightStatus(flightStatus);
        t.setAbnormalType(abnormalType);
        t.setAbnormalDesc(abnormalDesc);
        t.setCollectTime(LocalDateTime.now());
        trackMapper.insert(t);
        return t;
    }

    /** 订单跟踪数据（按时间正序，供轨迹绘制） */
    public List<OrderTrack> tracks(Long orderId, Long userId, Integer role) {
        OrderInfo order = mustGet(orderId);
        checkVisible(order, userId, role);
        return trackMapper.selectList(new LambdaQueryWrapper<OrderTrack>()
                .eq(OrderTrack::getOrderId, orderId)
                .orderByAsc(OrderTrack::getCollectTime));
    }

    // ==================== 五、送达与结算 ====================

    /** 标记送达：飞行中 → 已送达（同时释放飞行器与机长） */
    @Transactional
    public OrderVO arrive(Long merchantId, Long orderId) {
        OrderInfo order = mustGet(orderId);
        if (order.getMerchantId() == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "只能操作自己商家的订单");
        }
        if (order.getOrderStatus() != ST_FLYING) {
            throw new BizException(1001, "只有飞行中的订单可以标记送达");
        }
        DispatchRecord latest = latestDispatch(orderId);
        if (latest != null) {
            DispatchRecord d = new DispatchRecord();
            d.setId(latest.getId());
            d.setDispatchStatus(3);
            dispatchMapper.updateById(d);
            if (latest.getAircraftId() != null) {
                Aircraft a = new Aircraft();
                a.setId(latest.getAircraftId());
                a.setStatus(1);
                aircraftMapper.updateById(a);
            }
            if (latest.getPilotId() != null) {
                Pilot p = new Pilot();
                p.setId(latest.getPilotId());
                p.setStatus(1);
                pilotMapper.updateById(p);
            }
        }
        OrderInfo update = new OrderInfo();
        update.setId(orderId);
        update.setOrderStatus(ST_ARRIVED);
        update.setFinishTime(LocalDateTime.now());
        update.setActualFee(order.getEstimateFee());
        orderMapper.updateById(update);

        messageService.send(order.getCustomerId(), "order", "订单已送达",
                "订单 " + order.getOrderNo() + " 已送达，请完成支付与评价", "/customer/settlement");
        return toVO(mustGet(orderId));
    }

    /** 模拟推进飞行（演示用：一键补充若干跟踪点） */
    @Transactional
    public OrderVO simulateFlight(Long merchantId, Long orderId) {
        OrderInfo order = mustGet(orderId);
        if (order.getMerchantId() == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "只能操作自己商家的订单");
        }
        if (order.getOrderStatus() != ST_FLYING) {
            throw new BizException(1001, "只有飞行中的订单可以推进");
        }
        DispatchRecord latest = latestDispatch(orderId);
        String no = latest == null ? null : latest.getAircraftNo();
        insertTrack(orderId, no, 2, 0, null);
        insertTrack(orderId, no, 4, 0, null);
        return toVO(mustGet(orderId));
    }

    /**
     * 结算与评价：已送达 → 已完成。
     *
     * <p>写入结算记录（订单与结算一对一，靠唯一约束保证）并把订单置为已完成；
     * 客户评价随结算记录一并保存。
     */
    @Transactional
    public OrderSettlement settle(Long customerId, SettleDTO dto) {
        OrderInfo order = mustGet(dto.getOrderId());
        if (!order.getCustomerId().equals(customerId)) {
            throw new BizException(403, "只能结算自己的订单");
        }
        if (order.getOrderStatus() != ST_ARRIVED) {
            throw new BizException(1001, "只有已送达的订单可以结算");
        }
        OrderSettlement exists = settlementMapper.selectOne(new LambdaQueryWrapper<OrderSettlement>()
                .eq(OrderSettlement::getOrderId, order.getId()));
        if (exists != null) {
            throw new BizException(1001, "该订单已结算，请勿重复提交");
        }

        OrderSettlement s = new OrderSettlement();
        s.setOrderId(order.getId());
        s.setSettleAmount(order.getActualFee() == null ? order.getEstimateFee() : order.getActualFee());
        s.setDiscountAmount(BigDecimal.ZERO);
        s.setPayType(dto.getPayType() == null ? 1 : dto.getPayType());
        s.setPayStatus(1);
        s.setPayNo("PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        s.setScore(dto.getScore());
        s.setComment(dto.getComment());
        s.setSettleTime(LocalDateTime.now());
        settlementMapper.insert(s);

        OrderInfo update = new OrderInfo();
        update.setId(order.getId());
        update.setOrderStatus(ST_FINISHED);
        orderMapper.updateById(update);

        if (order.getMerchantId() != null) {
            messageService.send(order.getMerchantId(), "order", "订单已完成结算",
                    "订单 " + order.getOrderNo() + " 已支付 " + s.getSettleAmount() + " 元", "/merchant/settlement");
        }
        return s;
    }

    /** 结算详情 */
    public OrderSettlement settlementOf(Long orderId, Long userId, Integer role) {
        OrderInfo order = mustGet(orderId);
        checkVisible(order, userId, role);
        return settlementMapper.selectOne(new LambdaQueryWrapper<OrderSettlement>()
                .eq(OrderSettlement::getOrderId, orderId));
    }

    // ==================== 六、查询 ====================

    /**
     * 订单列表（按角色收敛数据范围）。
     *
     * <p>客户只看自己的、商家只看自己商家的、管理员看全平台——
     * 数据范围在服务端收敛，不依赖前端隐藏菜单。
     */
    public List<OrderVO> list(Long userId, Integer role, Integer status, String keyword) {
        LambdaQueryWrapper<OrderInfo> q = new LambdaQueryWrapper<>();
        if (role != null && role == 2) {
            q.eq(OrderInfo::getCustomerId, userId);
        } else if (role != null && role == 1) {
            q.eq(OrderInfo::getMerchantId, userId);
        }
        if (status != null) {
            q.eq(OrderInfo::getOrderStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(OrderInfo::getOrderNo, keyword)
                    .or().like(OrderInfo::getSendAddress, keyword)
                    .or().like(OrderInfo::getReceiveAddress, keyword));
        }
        q.orderByDesc(OrderInfo::getCreateTime).orderByDesc(OrderInfo::getId).last("limit 200");
        List<OrderVO> result = new ArrayList<>();
        for (OrderInfo o : orderMapper.selectList(q)) {
            result.add(toVO(o));
        }
        return result;
    }

    /** 待接单列表（商家端调度页面用） */
    public List<OrderVO> pendingList() {
        List<OrderVO> result = new ArrayList<>();
        for (OrderInfo o : orderMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderStatus, ST_PENDING)
                .orderByDesc(OrderInfo::getCreateTime))) {
            result.add(toVO(o));
        }
        return result;
    }

    /** 订单详情（含调度摘要与状态时间轴） */
    public OrderVO detail(Long orderId, Long userId, Integer role) {
        OrderInfo order = mustGet(orderId);
        checkVisible(order, userId, role);
        return toVO(order);
    }

    private void checkVisible(OrderInfo order, Long userId, Integer role) {
        if (role == null) {
            throw new BizException(401, "请先登录");
        }
        if (role == 0) {
            return;
        }
        if (role == 2 && !order.getCustomerId().equals(userId)) {
            throw new BizException(403, "无权查看他人订单");
        }
        if (role == 1 && (order.getMerchantId() == null || !order.getMerchantId().equals(userId))) {
            throw new BizException(403, "无权查看其他商家的订单");
        }
    }

    private OrderInfo mustGet(Long orderId) {
        OrderInfo order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(1001, "订单不存在");
        }
        return order;
    }

    private DispatchRecord latestDispatch(Long orderId) {
        return dispatchMapper.selectOne(new LambdaQueryWrapper<DispatchRecord>()
                .eq(DispatchRecord::getOrderId, orderId)
                .orderByDesc(DispatchRecord::getId)
                .last("limit 1"));
    }

    public static String statusName(Integer s) {
        if (s == null || s < 0 || s >= STATUS_NAMES.length) {
            return "未知";
        }
        return STATUS_NAMES[s];
    }

    private OrderVO toVO(OrderInfo o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setCustomerId(o.getCustomerId());
        vo.setMerchantId(o.getMerchantId());
        vo.setSendAddress(o.getSendAddress());
        vo.setReceiveAddress(o.getReceiveAddress());
        vo.setGoodsType(o.getGoodsType());
        vo.setGoodsWeight(o.getGoodsWeight());
        vo.setDeliveryType(o.getDeliveryType());
        vo.setDeliveryTypeName(nameOf(DELIVERY_NAMES, o.getDeliveryType()));
        vo.setPriority(o.getPriority());
        vo.setPriorityName(nameOf(PRIORITY_NAMES, o.getPriority()));
        vo.setDistanceKm(o.getDistanceKm());
        vo.setEstimateFee(o.getEstimateFee());
        vo.setActualFee(o.getActualFee());
        vo.setOrderStatus(o.getOrderStatus());
        vo.setOrderStatusName(statusName(o.getOrderStatus()));
        vo.setRejectReason(o.getRejectReason());
        vo.setRemark(o.getRemark());
        vo.setCreateTime(o.getCreateTime());
        vo.setAcceptTime(o.getAcceptTime());
        vo.setFinishTime(o.getFinishTime());

        User customer = userMapper.selectById(o.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(displayName(customer));
        }
        if (o.getMerchantId() != null) {
            User merchant = userMapper.selectById(o.getMerchantId());
            if (merchant != null) {
                vo.setMerchantName(merchant.getCompanyName() == null
                        ? displayName(merchant) : merchant.getCompanyName());
            }
        }

        DispatchRecord d = latestDispatch(o.getId());
        if (d != null) {
            vo.setDispatchId(d.getId());
            vo.setAircraftNo(d.getAircraftNo());
            vo.setPilotName(d.getPilotName());
            vo.setFlightPath(d.getFlightPath());
            vo.setDispatchStatus(d.getDispatchStatus());
        }
        vo.setTimeline(buildTimeline(o));
        return vo;
    }

    /** 状态流转时间轴：订单跟踪页左侧展示 */
    private List<Map<String, Object>> buildTimeline(OrderInfo o) {
        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(node("下单", o.getCreateTime(), true));
        timeline.add(node("接单", o.getAcceptTime(), o.getAcceptTime() != null));
        DispatchRecord d = latestDispatch(o.getId());
        boolean flying = o.getOrderStatus() != null && o.getOrderStatus() >= ST_FLYING;
        timeline.add(node("起飞调度", d == null ? null : d.getDispatchTime(), flying));
        timeline.add(node("送达", o.getFinishTime(),
                o.getOrderStatus() != null && o.getOrderStatus() >= ST_ARRIVED));
        timeline.add(node("完成结算", o.getOrderStatus() != null && o.getOrderStatus() == ST_FINISHED
                ? o.getFinishTime() : null, o.getOrderStatus() != null && o.getOrderStatus() == ST_FINISHED));
        return timeline;
    }

    private Map<String, Object> node(String label, LocalDateTime time, boolean done) {
        Map<String, Object> m = new HashMap<>();
        m.put("label", label);
        m.put("time", time == null ? null
                : time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        m.put("done", done);
        return m;
    }

    private String nameOf(String[] arr, Integer v) {
        if (v == null || v < 0 || v >= arr.length) {
            return "未知";
        }
        return arr[v];
    }

    private String displayName(User u) {
        if (u.getCompanyName() != null && !u.getCompanyName().isBlank()) {
            return u.getCompanyName();
        }
        return u.getRealName() == null ? u.getUsername() : u.getRealName();
    }
}
