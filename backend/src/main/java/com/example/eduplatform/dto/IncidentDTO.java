package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 应急事件上报入参 */
@Data
public class IncidentDTO {

    private Long orderId;

    @NotBlank(message = "请填写事件标题")
    private String title;

    private String content;

    private Integer incidentType;

    private Integer level;
}
