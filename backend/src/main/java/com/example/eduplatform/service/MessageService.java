package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.eduplatform.entity.Notification;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.mapper.NotificationMapper;
import com.example.eduplatform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内消息服务。
 *
 * <p>消息不是定时任务，而是业务动作的副作用：订单状态流转、飞行计划审批、
 * 违规处置都要调用本服务写一条记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public void send(Long userId, String type, String title, String content, String link) {
        if (userId == null) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null || (user.getNotifyEnabled() != null && user.getNotifyEnabled() == 0)) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type == null ? "info" : type);
        n.setTitle(title);
        n.setContent(content);
        n.setLink(link);
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /**
   * 广播给全体平台管理员。
   *
   * <p>管理员可能有多个，且运行期会新增，因此每次按角色查询而不是写死某个 id。
   */
    public void sendToAdmins(String type, String title, String content, String link) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 0)
                .eq(User::getStatus, 1));
        for (User admin : admins) {
            try {
                send(admin.getId(), type, title, content, link);
            } catch (Exception e) {
                log.warn("向管理员 {} 推送消息失败", admin.getId(), e);
            }
        }
    }

    public List<Notification> myList(Long userId, String type) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(type != null && !type.isBlank(), Notification::getType, type)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId)
                .last("limit 200"));
    }

    public long unreadCount(Long userId) {
        Long c = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return c == null ? 0 : c;
    }

    public void markRead(Long userId, Long id) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }
}
