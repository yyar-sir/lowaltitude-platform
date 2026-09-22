package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 空域信息表（禁飞区/临时管制区） */
@Data
@TableName("airspace_zone")
public class AirspaceZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 空域名称 */
    private String name;

    /** 1可用空域 2禁飞区 3临时管制区 */
    private Integer zoneType;

    /** 空域边界（坐标点串） */
    private String boundary;

    /** 中心经度 */
    private String centerLng;

    /** 中心纬度 */
    private String centerLat;

    /** 最大允许高度（米） */
    private Integer maxAltitude;

    /** 生效时间 */
    private LocalDateTime effectiveFrom;

    /** 失效时间 */
    private LocalDateTime effectiveTo;

    /** 1生效中 0已失效 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
