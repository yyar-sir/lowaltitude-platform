package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 应急事件表 */
@Data
@TableName("incident")
public class Incident {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单 id */
    private Long orderId;

    /** 上报人 user.id */
    private Long reporterId;

    /** 事件标题 */
    private String title;

    /** 事件描述 */
    private String content;

    /** 1设备故障 2空域冲突 3恶劣天气 4其他 */
    private Integer incidentType;

    /** 1一般 2较大 3重大 */
    private Integer level;

    /** 0待处理 1处理中 2已解决 */
    private Integer status;

    /** 处置结果 */
    private String handleResult;

    /** 上报时间 */
    private LocalDateTime createdAt;

    /** 解决时间 */
    private LocalDateTime resolvedAt;
}
