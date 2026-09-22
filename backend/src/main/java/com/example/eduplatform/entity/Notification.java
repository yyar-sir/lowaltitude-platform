package com.example.eduplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息表 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 user.id */
    private Long userId;

    /** order订单/plan审批/violation违规/announce公告/info */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 点击跳转的前端路由 */
    private String link;

    /** 0未读 1已读（列名不能用 read，是保留字） */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
