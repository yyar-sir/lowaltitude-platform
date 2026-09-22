package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单跟踪表（飞行位置与状态） */
@Data
@TableName("order_track")
public class OrderTrack {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 id（外键） */
    private Long orderId;

    /** 飞行器编号 */
    private String aircraftNo;

    /** 当前位置经度 */
    private BigDecimal longitude;

    /** 当前位置纬度 */
    private BigDecimal latitude;

    /** 剩余电量百分比 */
    private Integer battery;

    /** 1起飞 2巡航 3悬停 4降落 5已结束 */
    private Integer flightStatus;

    /** 0正常 1设备故障 2空域管制 */
    private Integer abnormalType;

    /** 异常描述 */
    private String abnormalDesc;

    /** 采集时间 */
    private LocalDateTime collectTime;
}
