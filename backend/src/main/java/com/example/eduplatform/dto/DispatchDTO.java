package com.example.eduplatform.dto;

import lombok.Data;

/** 调度入参：商家为订单生成调度方案 */
@Data
public class DispatchDTO {

    private Long orderId;

    private Long aircraftId;

    private Long stationId;

    private Long pilotId;

    private String flightPath;
}
