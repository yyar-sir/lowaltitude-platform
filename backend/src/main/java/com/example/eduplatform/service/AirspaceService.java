package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.dto.FlightPlanDTO;
import com.example.eduplatform.entity.AirspaceZone;
import com.example.eduplatform.entity.FlightPlan;
import com.example.eduplatform.entity.ViolationRecord;
import com.example.eduplatform.mapper.AirspaceZoneMapper;
import com.example.eduplatform.mapper.FlightPlanMapper;
import com.example.eduplatform.mapper.ViolationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 空域合规运营：空域规则维护、飞行计划申报与审批、违规记录。
 *
 * <p>飞行计划审批链：商家申报（待审批）→ 管理员审批（通过/驳回）→ 执行后置为已执行。
 * 审批结果通过站内消息同步给申报商家。
 */
@Service
@RequiredArgsConstructor
public class AirspaceService {

    private final AirspaceZoneMapper zoneMapper;
    private final FlightPlanMapper planMapper;
    private final ViolationRecordMapper violationMapper;
    private final MessageService messageService;

    private final Random random = new Random();

    // ---------- 空域 ----------

    public List<AirspaceZone> zones(Integer zoneType, Integer status) {
        return zoneMapper.selectList(new LambdaQueryWrapper<AirspaceZone>()
                .eq(zoneType != null, AirspaceZone::getZoneType, zoneType)
                .eq(status != null, AirspaceZone::getStatus, status)
                .orderByAsc(AirspaceZone::getZoneType)
                .orderByAsc(AirspaceZone::getId));
    }

    public AirspaceZone saveZone(AirspaceZone dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BizException(1002, "请填写空域名称");
        }
        if (dto.getId() == null) {
            dto.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dto.setMaxAltitude(dto.getMaxAltitude() == null ? 120 : dto.getMaxAltitude());
            dto.setCreatedAt(LocalDateTime.now());
            zoneMapper.insert(dto);
        } else {
            zoneMapper.updateById(dto);
        }
        return dto;
    }

    public void deleteZone(Long id) {
        zoneMapper.deleteById(id);
    }

    // ---------- 飞行计划 ----------

    public List<FlightPlan> planList(Long merchantId, Integer status) {
        return planMapper.selectList(new LambdaQueryWrapper<FlightPlan>()
                .eq(merchantId != null, FlightPlan::getMerchantId, merchantId)
                .eq(status != null, FlightPlan::getStatus, status)
                .orderByDesc(FlightPlan::getCreatedAt)
                .orderByDesc(FlightPlan::getId));
    }

    /**
   * 商家申报飞行计划。
   *
   * <p>申报时校验飞行高度不超过可用空域上限，避免申报一个注定被驳回的计划。
   */
    public FlightPlan submit(Long merchantId, FlightPlanDTO dto) {
        int altitude = dto.getAltitude() == null ? 100 : dto.getAltitude();
        Integer maxAltitude = maxAllowedAltitude();
        if (maxAltitude != null && altitude > maxAltitude) {
            throw new BizException(1001, "申报高度 " + altitude + " 米超过可用空域上限 " + maxAltitude + " 米");
        }
        FlightPlan plan = new FlightPlan();
        plan.setOrderId(dto.getOrderId());
        plan.setMerchantId(merchantId);
        plan.setPlanNo("FP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (10 + random.nextInt(90)));
        plan.setRoute(dto.getRoute());
        plan.setAltitude(altitude);
        plan.setPlanStart(dto.getPlanStart());
        plan.setPlanEnd(dto.getPlanEnd());
        plan.setStatus(0);
        plan.setCreatedAt(LocalDateTime.now());
        planMapper.insert(plan);
        return plan;
    }

    private Integer maxAllowedAltitude() {
        List<AirspaceZone> zones = zoneMapper.selectList(new LambdaQueryWrapper<AirspaceZone>()
                .eq(AirspaceZone::getZoneType, 1)
                .eq(AirspaceZone::getStatus, 1));
        return zones.stream()
                .map(AirspaceZone::getMaxAltitude)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    /**
   * 管理员审批：0待审批 → 1通过 / 2驳回。
   *
   * <p>驳回必须填写原因；审批结果同时写站内消息通知商家。
   */
    @Transactional
    public FlightPlan review(Long adminId, Long planId, boolean pass, String reason) {
        FlightPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(1001, "飞行计划不存在");
        }
        if (plan.getStatus() != null && plan.getStatus() != 0) {
            throw new BizException(1001, "该计划已审批，不能重复处理");
        }
        if (!pass && (reason == null || reason.isBlank())) {
            throw new BizException(1002, "驳回必须填写原因");
        }
        plan.setStatus(pass ? 1 : 2);
        plan.setRejectReason(pass ? null : reason);
        plan.setReviewerId(adminId);
        plan.setReviewTime(LocalDateTime.now());
        planMapper.updateById(plan);

        messageService.send(plan.getMerchantId(), "plan",
                pass ? "飞行计划已通过" : "飞行计划被驳回",
                "计划 " + plan.getPlanNo() + (pass ? " 已通过审批" : " 被驳回：" + reason),
                "/merchant/plans");
        return plan;
    }

    /** 计划执行完成后标记为已执行 */
    public void markExecuted(Long planId) {
        FlightPlan plan = planMapper.selectById(planId);
        if (plan != null && plan.getStatus() != null && plan.getStatus() == 1) {
            FlightPlan update = new FlightPlan();
            update.setId(planId);
            update.setStatus(3);
            planMapper.updateById(update);
        }
    }

    // ---------- 违规记录 ----------

    public List<ViolationRecord> violations(Long merchantId, Integer handleStatus) {
        return violationMapper.selectList(new LambdaQueryWrapper<ViolationRecord>()
                .eq(merchantId != null, ViolationRecord::getMerchantId, merchantId)
                .eq(handleStatus != null, ViolationRecord::getHandleStatus, handleStatus)
                .orderByDesc(ViolationRecord::getCreatedAt)
                .orderByDesc(ViolationRecord::getId));
    }

    /** 管理员处置违规记录 */
    public ViolationRecord handleViolation(Long id, String result) {
        ViolationRecord v = violationMapper.selectById(id);
        if (v == null) {
            throw new BizException(1001, "违规记录不存在");
        }
        v.setHandleStatus(1);
        v.setHandleResult(result);
        violationMapper.updateById(v);
        if (v.getMerchantId() != null) {
            messageService.send(v.getMerchantId(), "violation", "违规处置通知",
                    v.getViolationType() + "：" + (result == null ? "已处理" : result), "/merchant/violations");
        }
        return v;
    }

    /** 记录违规（订单跟踪发现异常时调用） */
    public void recordViolation(Long orderId, Long merchantId, String type, String detail, int level) {
        ViolationRecord v = new ViolationRecord();
        v.setOrderId(orderId);
        v.setMerchantId(merchantId);
        v.setViolationType(type);
        v.setDetail(detail);
        v.setLevel(level);
        v.setHandleStatus(0);
        v.setCreatedAt(LocalDateTime.now());
        violationMapper.insert(v);
    }
}
