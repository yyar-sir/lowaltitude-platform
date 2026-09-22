package com.example.eduplatform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 客户下单入参 */
@Data
public class OrderCreateDTO {

    @NotBlank(message = "请填写寄件地址")
    private String sendAddress;

    @NotBlank(message = "请填写收件地址")
    private String receiveAddress;

    @NotBlank(message = "请选择物品类型")
    private String goodsType;

    @NotNull(message = "请填写物品重量")
    @DecimalMin(value = "0.01", message = "物品重量必须大于 0")
    private BigDecimal goodsWeight;

    private Integer deliveryType;

    private Integer priority;

    private String remark;

    private Long merchantId;
}
