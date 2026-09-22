package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.entity.Notification;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息中心（三端共用）。
 *
 * <p>三个角色都要能调：若遗漏某个角色，前端布局件的轮询会拿到 403，
 * 而 403 被误当登录失效处理时会把用户反复踢回登录页。
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/list")
    @RequireRole({0, 1, 2})
    public Result<List<Notification>> list(@RequestParam(required = false) String type) {
        return Result.ok(messageService.myList(UserContext.userId(), type));
    }

    @GetMapping("/unread")
    @RequireRole({0, 1, 2})
    public Result<Map<String, Object>> unread() {
        Map<String, Object> m = new HashMap<>();
        m.put("count", messageService.unreadCount(UserContext.userId()));
        return Result.ok(m);
    }

    @PostMapping("/{id}/read")
    @RequireRole({0, 1, 2})
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(UserContext.userId(), id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    @RequireRole({0, 1, 2})
    public Result<Void> readAll() {
        messageService.markAllRead(UserContext.userId());
        return Result.ok();
    }
}
