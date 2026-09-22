package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 调度记录表（一条订单可多次调度） */
@Data
@TableName("dispatch_record")
public class DispatchRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 id（外键） */
    private Long orderId;

    /** 飞行器 id */
    private Long aircraftId;

    /** 飞行器编号 */
    private String aircraftNo;

    /** 起降场站 id */
    private Long stationId;

    /** 执行机长 id */
    private Long pilotId;

    /** 执行机长姓名 */
    private String pilotName;

    /** 规划飞行路径 */
    private String flightPath;

    /** 计划起飞时间 */
    private LocalDateTime planTakeoffTime;

    /** 计划送达时间 */
    private LocalDateTime planArriveTime;

    /** 1待执行 2执行中 3已完成 4已取消 */
    private Integer dispatchStatus;

    /** 调度时间 */
    private LocalDateTime dispatchTime;
}
