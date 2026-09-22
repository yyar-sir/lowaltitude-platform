package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 起降场站表 */
@Data
@TableName("station")
public class Station {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家 user.id */
    private Long merchantId;

    /** 场站编号 */
    private String stationNo;

    /** 场站名称 */
    private String name;

    /** 场站地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 可容纳飞行器数量 */
    private Integer capacity;

    /** 1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
