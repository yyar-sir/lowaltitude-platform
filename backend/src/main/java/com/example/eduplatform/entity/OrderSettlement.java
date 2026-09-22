package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单结算表（一对一订单） */
@Data
@TableName("order_settlement")
public class OrderSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 id（外键，唯一） */
    private Long orderId;

    /** 结算金额（元） */
    private BigDecimal settleAmount;

    /** 优惠金额（元） */
    private BigDecimal discountAmount;

    /** 1微信 2支付宝 3对公转账 */
    private Integer payType;

    /** 0未支付 1已支付 2支付失败 */
    private Integer payStatus;

    /** 支付流水号 */
    private String payNo;

    /** 客户评分 1-5 */
    private Integer score;

    /** 评价内容 */
    private String comment;

    /** 结算时间 */
    private LocalDateTime settleTime;
}
