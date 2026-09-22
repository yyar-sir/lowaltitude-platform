package com.example.eduplatform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 订单展示体：订单字段 + 客户/商家名称 + 调度与结算摘要 */
@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long merchantId;
    private String merchantName;
    private String sendAddress;
    private String receiveAddress;
    private String goodsType;
    private BigDecimal goodsWeight;
    private Integer deliveryType;
    private String deliveryTypeName;
    private Integer priority;
    private String priorityName;
    private BigDecimal distanceKm;
    private BigDecimal estimateFee;
    private BigDecimal actualFee;
    private Integer orderStatus;
    private String orderStatusName;
    private String rejectReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime acceptTime;
    private LocalDateTime finishTime;

    private Long dispatchId;
    private String aircraftNo;
    private String pilotName;
    private String flightPath;
    private Integer dispatchStatus;

    /** 状态流转时间轴 */
    private List<Map<String, Object>> timeline;
}
