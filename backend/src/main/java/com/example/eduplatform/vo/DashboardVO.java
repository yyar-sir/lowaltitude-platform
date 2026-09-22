package com.example.eduplatform.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 数据看板出参（三端共用，按角色填充不同范围） */
@Data
public class DashboardVO {
    private Map<String, Object> summary;
    private List<Map<String, Object>> orderStatusDist;
    private List<Map<String, Object>> orderTrend;
    private List<Map<String, Object>> aircraftStatusDist;
    private List<Map<String, Object>> aircraftUsage;
    private List<Map<String, Object>> violationDist;
    private List<String> todos;
}
