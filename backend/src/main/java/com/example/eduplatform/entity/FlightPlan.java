package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 飞行计划表（申报与审批） */
@Data
@TableName("flight_plan")
public class FlightPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单 id */
    private Long orderId;

    /** 申报商家 user.id */
    private Long merchantId;

    /** 计划编号 */
    private String planNo;

    /** 计划航线 */
    private String route;

    /** 计划飞行高度（米） */
    private Integer altitude;

    /** 计划开始时间 */
    private LocalDateTime planStart;

    /** 计划结束时间 */
    private LocalDateTime planEnd;

    /** 0待审批 1已通过 2已驳回 3已执行 */
    private Integer status;

    /** 驳回原因 */
    private String rejectReason;

    /** 审批人 user.id */
    private Long reviewerId;

    /** 审批时间 */
    private LocalDateTime reviewTime;

    /** 申报时间 */
    private LocalDateTime createdAt;
}
