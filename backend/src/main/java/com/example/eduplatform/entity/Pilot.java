package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 作业人员（机长）表 */
@Data
@TableName("pilot")
public class Pilot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家 user.id */
    private Long merchantId;

    /** 姓名 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 驾驶证编号 */
    private String licenseNo;

    /** 驾驶证到期日 */
    private LocalDate licenseExpire;

    /** 1在岗 2任务中 0休息 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
