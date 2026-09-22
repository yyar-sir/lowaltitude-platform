package com.example.eduplatform.dto;

import lombok.Data;

/** 结算与评价入参 */
@Data
public class SettleDTO {

    private Long orderId;

    private Integer payType;

    private Integer score;

    private String comment;
}
