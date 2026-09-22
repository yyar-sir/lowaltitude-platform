package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 定价规则表（商家自定义收费标准） */
@Data
@TableName("pricing_rule")
public class PricingRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家 user.id */
    private Long merchantId;

    /** 规则名称 */
    private String name;

    /** 基础配送费（元） */
    private BigDecimal baseFee;

    /** 每公里费用（元） */
    private BigDecimal perKmFee;

    /** 空域使用费（元） */
    private BigDecimal airspaceFee;

    /** 加急费（元） */
    private BigDecimal urgentFee;

    /** 1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
