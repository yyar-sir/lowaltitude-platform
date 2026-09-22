package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 飞行器台账表 */
@Data
@TableName("aircraft")
public class Aircraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家 user.id */
    private Long merchantId;

    /** 飞行器编号 */
    private String aircraftNo;

    /** 型号 */
    private String model;

    /** 最大载重（kg） */
    private BigDecimal maxPayload;

    /** 续航时间（分钟） */
    private Integer enduranceMinutes;

    /** 当前电量百分比 */
    private Integer battery;

    /** 1可用 2任务中 3维保中 4停用 */
    private Integer status;

    /** 停放场站编号 */
    private String stationNo;

    /** 下次维保时间 */
    private LocalDateTime maintainAt;

    /** 入库时间 */
    private LocalDateTime createdAt;
}
