package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.entity.Aircraft;
import com.example.eduplatform.entity.Pilot;
import com.example.eduplatform.entity.PricingRule;
import com.example.eduplatform.entity.Station;
import com.example.eduplatform.mapper.AircraftMapper;
import com.example.eduplatform.mapper.PilotMapper;
import com.example.eduplatform.mapper.PricingRuleMapper;
import com.example.eduplatform.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 低空资源管理：飞行器、起降场站、作业人员、定价规则。
 *
 * <p>资源数据是订单调度的前提——调度时会校验飞行器可用性与载重，
 * 因此这里的台账必须准确。所有查询都按 merchantId 收敛范围。
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final AircraftMapper aircraftMapper;
    private final StationMapper stationMapper;
    private final PilotMapper pilotMapper;
    private final PricingRuleMapper pricingMapper;

    // ---------- 飞行器 ----------

    public List<Aircraft> aircraftList(Long merchantId, Integer status) {
        return aircraftMapper.selectList(new LambdaQueryWrapper<Aircraft>()
                .eq(merchantId != null, Aircraft::getMerchantId, merchantId)
                .eq(status != null, Aircraft::getStatus, status)
                .orderByAsc(Aircraft::getId));
    }

    public Aircraft saveAircraft(Long merchantId, Aircraft dto) {
        if (dto.getAircraftNo() == null || dto.getAircraftNo().isBlank()) {
            throw new BizException(1002, "请填写飞行器编号");
        }
        if (dto.getId() == null) {
            dto.setMerchantId(merchantId);
            dto.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dto.setBattery(dto.getBattery() == null ? 100 : dto.getBattery());
            dto.setCreatedAt(LocalDateTime.now());
            aircraftMapper.insert(dto);
        } else {
            Aircraft exist = aircraftMapper.selectById(dto.getId());
            if (exist == null || !exist.getMerchantId().equals(merchantId)) {
                throw new BizException(403, "只能维护自己商家的飞行器");
            }
            dto.setMerchantId(merchantId);
            aircraftMapper.updateById(dto);
        }
        return dto;
    }

    /** 飞行器状态流转：1可用 2任务中 3维保中 4停用 */
    public Aircraft changeAircraftStatus(Long merchantId, Long id, Integer status) {
        Aircraft a = aircraftMapper.selectById(id);
        if (a == null || !a.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "只能维护自己商家的飞行器");
        }
        if (a.getStatus() != null && a.getStatus() == 2 && status != 2) {
            throw new BizException(1001, "飞行器正在执行任务，不能变更状态");
        }
        Aircraft update = new Aircraft();
        update.setId(id);
        update.setStatus(status);
        aircraftMapper.updateById(update);
        return aircraftMapper.selectById(id);
    }

    // ---------- 起降场站 ----------

    public List<Station> stationList(Long merchantId) {
        return stationMapper.selectList(new LambdaQueryWrapper<Station>()
                .eq(merchantId != null, Station::getMerchantId, merchantId)
                .orderByAsc(Station::getId));
    }

    public Station saveStation(Long merchantId, Station dto) {
        if (dto.getStationNo() == null || dto.getStationNo().isBlank()) {
            throw new BizException(1002, "请填写场站编号");
        }
        if (dto.getId() == null) {
            dto.setMerchantId(merchantId);
            dto.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dto.setCapacity(dto.getCapacity() == null ? 1 : dto.getCapacity());
            dto.setCreatedAt(LocalDateTime.now());
            stationMapper.insert(dto);
        } else {
            Station exist = stationMapper.selectById(dto.getId());
            if (exist == null || !exist.getMerchantId().equals(merchantId)) {
                throw new BizException(403, "只能维护自己商家的场站");
            }
            dto.setMerchantId(merchantId);
            stationMapper.updateById(dto);
        }
        return dto;
    }

    // ---------- 作业人员 ----------

    public List<Pilot> pilotList(Long merchantId) {
        return pilotMapper.selectList(new LambdaQueryWrapper<Pilot>()
                .eq(merchantId != null, Pilot::getMerchantId, merchantId)
                .orderByAsc(Pilot::getId));
    }

    public Pilot savePilot(Long merchantId, Pilot dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BizException(1002, "请填写机长姓名");
        }
        if (dto.getLicenseNo() == null || dto.getLicenseNo().isBlank()) {
            throw new BizException(1002, "请填写驾驶证编号");
        }
        if (dto.getLicenseExpire() != null && dto.getLicenseExpire().isBefore(java.time.LocalDate.now())) {
            throw new BizException(1001, "驾驶证已过期，无法录入");
        }
        if (dto.getId() == null) {
            dto.setMerchantId(merchantId);
            dto.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dto.setCreatedAt(LocalDateTime.now());
            pilotMapper.insert(dto);
        } else {
            Pilot exist = pilotMapper.selectById(dto.getId());
            if (exist == null || !exist.getMerchantId().equals(merchantId)) {
                throw new BizException(403, "只能维护自己商家的作业人员");
            }
            dto.setMerchantId(merchantId);
            pilotMapper.updateById(dto);
        }
        return dto;
    }

    // ---------- 定价规则 ----------

    public List<PricingRule> pricingList(Long merchantId) {
        return pricingMapper.selectList(new LambdaQueryWrapper<PricingRule>()
                .eq(merchantId != null, PricingRule::getMerchantId, merchantId)
                .orderByAsc(PricingRule::getId));
    }

    public PricingRule savePricing(Long merchantId, PricingRule dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BizException(1002, "请填写规则名称");
        }
        // 定价必须大于 0，否则订单费用会算出 0 元
        if (dto.getBaseFee() == null || dto.getBaseFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(1002, "基础配送费必须大于 0");
        }
        if (dto.getId() == null) {
            dto.setMerchantId(merchantId);
            dto.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dto.setPerKmFee(dto.getPerKmFee() == null ? BigDecimal.ZERO : dto.getPerKmFee());
            dto.setAirspaceFee(dto.getAirspaceFee() == null ? BigDecimal.ZERO : dto.getAirspaceFee());
            dto.setUrgentFee(dto.getUrgentFee() == null ? BigDecimal.ZERO : dto.getUrgentFee());
            dto.setCreatedAt(LocalDateTime.now());
            pricingMapper.insert(dto);
        } else {
            PricingRule exist = pricingMapper.selectById(dto.getId());
            if (exist == null || !exist.getMerchantId().equals(merchantId)) {
                throw new BizException(403, "只能维护自己商家的定价规则");
            }
            dto.setMerchantId(merchantId);
            pricingMapper.updateById(dto);
        }
        return dto;
    }

    /** 资源总览（商家工作台用） */
    public Map<String, Object> overview(Long merchantId) {
        Map<String, Object> m = new HashMap<>();
        List<Aircraft> aircraft = aircraftList(merchantId, null);
        m.put("aircraftTotal", aircraft.size());
        m.put("aircraftAvailable", aircraft.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 1).count());
        m.put("aircraftMaintaining", aircraft.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 3).count());
        m.put("stationTotal", stationList(merchantId).size());
        m.put("pilotTotal", pilotList(merchantId).size());
        m.put("pricingTotal", pricingList(merchantId).size());
        return m;
    }

    /** 可用飞行器（调度页面下拉用：只返回可用且电量足够的） */
    public List<Aircraft> availableAircraft(Long merchantId, BigDecimal weight) {
        List<Aircraft> all = aircraftMapper.selectList(new LambdaQueryWrapper<Aircraft>()
                .eq(Aircraft::getMerchantId, merchantId)
                .eq(Aircraft::getStatus, 1)
                .orderByAsc(Aircraft::getId));
        List<Aircraft> result = new ArrayList<>();
        for (Aircraft a : all) {
            boolean payloadOk = weight == null || a.getMaxPayload() == null
                    || a.getMaxPayload().compareTo(weight) >= 0;
            boolean batteryOk = a.getBattery() == null || a.getBattery() >= 30;
            if (payloadOk && batteryOk) {
                result.add(a);
            }
        }
        return result;
    }
}
