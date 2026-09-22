package com.example.eduplatform.service;

import com.example.eduplatform.entity.OperationLog;
import com.example.eduplatform.mapper.OperationLogMapper;
import com.example.eduplatform.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** 操作日志：管理端关键动作留痕；写失败不影响主业务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void record(String opType, String detail) {
        try {
            OperationLog row = new OperationLog();
            row.setOperatorId(UserContext.userId());
            row.setOperatorRole(UserContext.role());
            row.setOpType(opType);
            row.setDetail(detail);
            row.setIp(currentIp());
            row.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("操作日志写入失败：{} - {}", opType, detail, e);
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
