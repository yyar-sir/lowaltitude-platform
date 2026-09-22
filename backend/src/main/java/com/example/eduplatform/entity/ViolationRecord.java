package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 违规记录表 */
@Data
@TableName("violation_record")
public class ViolationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单 id */
    private Long orderId;

    /** 违规商家 user.id */
    private Long merchantId;

    /** 涉及飞行器 id */
    private Long aircraftId;

    /** 违规类型 */
    private String violationType;

    /** 违规说明 */
    private String detail;

    /** 1轻微 2一般 3严重 */
    private Integer level;

    /** 0待处理 1已处理 */
    private Integer handleStatus;

    /** 处理结果 */
    private String handleResult;

    /** 发生时间 */
    private LocalDateTime createdAt;
}
