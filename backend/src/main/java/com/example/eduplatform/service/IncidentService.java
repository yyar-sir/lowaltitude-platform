package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.dto.IncidentDTO;
import com.example.eduplatform.entity.Incident;
import com.example.eduplatform.mapper.IncidentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应急与客服管理。
 *
 * <p>三端都可以上报事件，处理闭环为「待处理 → 处理中 → 已解决」；
 * 不允许从待处理直接跳到已解决，保证处置过程可追溯。
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentMapper incidentMapper;
    private final MessageService messageService;

    public List<Incident> list(Long reporterId, Integer status) {
        return incidentMapper.selectList(new LambdaQueryWrapper<Incident>()
                .eq(reporterId != null, Incident::getReporterId, reporterId)
                .eq(status != null, Incident::getStatus, status)
                .orderByAsc(Incident::getStatus)
                .orderByDesc(Incident::getCreatedAt));
    }

    public Incident report(Long reporterId, IncidentDTO dto) {
        Incident e = new Incident();
        e.setOrderId(dto.getOrderId());
        e.setReporterId(reporterId);
        e.setTitle(dto.getTitle());
        e.setContent(dto.getContent());
        e.setIncidentType(dto.getIncidentType() == null ? 4 : dto.getIncidentType());
        e.setLevel(dto.getLevel() == null ? 1 : dto.getLevel());
        e.setStatus(0);
        e.setCreatedAt(LocalDateTime.now());
        incidentMapper.insert(e);

        // 较大及以上事件广播给全体管理员，避免漏处理
        if (e.getLevel() >= 2) {
            messageService.sendToAdmins("info", "应急事件待处理", e.getTitle(), "/admin/incidents");
        }
        return e;
    }

    public Incident handle(Long id, Integer status, String result) {
        Incident e = incidentMapper.selectById(id);
        if (e == null) {
            throw new BizException(1001, "事件不存在");
        }
        int target = status == null ? 1 : status;
        if (e.getStatus() != null && e.getStatus() == 0 && target == 2) {
            throw new BizException(1001, "请先将事件置为处理中，再标记已解决");
        }
        e.setStatus(target);
        e.setHandleResult(result);
        if (target == 2) {
            e.setResolvedAt(LocalDateTime.now());
        }
        incidentMapper.updateById(e);
        if (e.getReporterId() != null) {
            messageService.send(e.getReporterId(), "info", "事件处理进展",
                    e.getTitle() + " → " + statusName(target), "/customer/incidents");
        }
        return e;
    }

    private String statusName(int s) {
        if (s == 1) {
            return "处理中";
        }
        if (s == 2) {
            return "已解决";
        }
        return "待处理";
    }

    public long pendingCount() {
        Long c = incidentMapper.selectCount(new LambdaQueryWrapper<Incident>()
                .ne(Incident::getStatus, 2));
        return c == null ? 0 : c;
    }
}
