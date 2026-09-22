package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/** 飞行计划申报入参 */
@Data
public class FlightPlanDTO {

    private Long orderId;

    @NotBlank(message = "请填写计划航线")
    private String route;

    private Integer altitude;

    private LocalDateTime planStart;

    private LocalDateTime planEnd;
}
