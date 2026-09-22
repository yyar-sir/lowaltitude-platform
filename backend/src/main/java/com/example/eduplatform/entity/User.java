package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户表（终端客户/物流运营商家/平台管理员） */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** BCrypt 密码散列（绝不随接口下发） */
    @JsonIgnore
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 昵称 */
    private String nickname;

    /** 头像 emoji */
    private String avatar;

    /** 0管理员 1物流运营商家 2终端客户 */
    private Integer role;

    /** 联系电话 */
    private String phone;

    /** 企业名称（商家） */
    private String companyName;

    /** 运营许可证号（商家） */
    private String licenseNo;

    /** 1启用 0禁用 */
    private Integer status;

    /** 1接收站内消息 0不接收 */
    private Integer notifyEnabled;

    /** 密码最后修改时间（作废此前 token） */
    private LocalDateTime passwordChangedAt;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
