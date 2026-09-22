package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单信息表（订单全流程核心主表） */
@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 客户 user.id（外键） */
    private Long customerId;

    /** 商家 user.id（外键，接单后写入） */
    private Long merchantId;

    /** 寄件地址 */
    private String sendAddress;

    /** 收件地址 */
    private String receiveAddress;

    /** 物品类型 */
    private String goodsType;

    /** 物品重量（kg） */
    private BigDecimal goodsWeight;

    /** 1普通配送 2加急配送 */
    private Integer deliveryType;

    /** 优先级 1普通 2较高 3紧急 */
    private Integer priority;

    /** 预估航程（km） */
    private BigDecimal distanceKm;

    /** 预估费用（元） */
    private BigDecimal estimateFee;

    /** 实际费用（元） */
    private BigDecimal actualFee;

    /** 1待接单 2待调度 3飞行中 4已送达 5已完成 6已取消 */
    private Integer orderStatus;

    /** 拒单原因 */
    private String rejectReason;

    /** 订单备注 */
    private String remark;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 送达时间 */
    private LocalDateTime finishTime;
}
