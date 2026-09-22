package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志表 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 user.id */
    private Long operatorId;

    /** 操作人角色 */
    private Integer operatorRole;

    /** 操作类型 */
    private String opType;

    /** 详情 */
    private String detail;

    /** IP 地址 */
    private String ip;

    /** 时间 */
    private LocalDateTime createdAt;
}
