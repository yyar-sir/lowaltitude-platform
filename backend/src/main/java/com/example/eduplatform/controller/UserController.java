package com.example.eduplatform.controller;

import com.example.eduplatform.common.BizException;
import com.example.eduplatform.common.Result;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.mapper.UserMapper;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 个人中心（三端共用）。
 *
 * <p>改密后写入 password_changed_at，鉴权拦截器据此拒绝签发时间更早的 token ——
 * 改密 = 下线所有旧设备。同一账号 15 分钟内失败 5 次即锁定。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 失败计数：userId → [次数, 窗口起点] */
    private static final ConcurrentHashMap<Long, long[]> FAILURES = new ConcurrentHashMap<>();
    private static final int MAX_FAIL = 5;
    private static final long LOCK_WINDOW_MS = 15 * 60 * 1000L;

    /** 我的资料 */
    @GetMapping("/profile")
    @RequireRole({0, 1, 2})
    public Result<User> profile() {
        return Result.ok(userMapper.selectById(UserContext.userId()));
    }

    /** 更新资料（昵称/头像/电话/通知开关） */
    @PostMapping("/profile")
    @RequireRole({0, 1, 2})
    public Result<User> updateProfile(@RequestBody User dto) {
        User u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "登录状态已失效");
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            u.setNickname(dto.getNickname().trim());
        }
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()) {
            u.setAvatar(dto.getAvatar());
        }
        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            u.setRealName(dto.getRealName().trim());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            u.setPhone(dto.getPhone().trim());
        }
        if (dto.getCompanyName() != null && !dto.getCompanyName().isBlank()) {
            u.setCompanyName(dto.getCompanyName().trim());
        }
        if (dto.getNotifyEnabled() != null) {
            u.setNotifyEnabled(dto.getNotifyEnabled());
        }
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        return Result.ok(u);
    }

    /** 修改密码 */
    @PostMapping("/password")
    @RequireRole({0, 1, 2})
    public Result<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        Long uid = UserContext.userId();
        checkLocked(uid);
        User u = userMapper.selectById(uid);
        if (u == null) {
            throw new BizException(401, "登录状态已失效");
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        // 文案统一，不区分账号是否存在，避免账号枚举
        if (oldPassword == null || !ENCODER.matches(oldPassword, u.getPassword())) {
            recordFailure(uid);
            throw new BizException(1003, "原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BizException(1002, "新密码长度需为 6-20 位");
        }
        u.setPassword(ENCODER.encode(newPassword));
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        FAILURES.remove(uid);
        return Result.ok(Map.of("changed", true, "message", "密码已修改，其他设备需重新登录"));
    }

    private void checkLocked(Long uid) {
        long[] f = FAILURES.get(uid);
        if (f == null) {
            return;
        }
        if (System.currentTimeMillis() - f[1] > LOCK_WINDOW_MS) {
            FAILURES.remove(uid);
            return;
        }
        if (f[0] >= MAX_FAIL) {
            throw new BizException(1004, "尝试次数过多，请 15 分钟后再试");
        }
    }

    private void recordFailure(Long uid) {
        FAILURES.compute(uid, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v[1] > LOCK_WINDOW_MS) {
                return new long[]{1, now};
            }
            v[0] = v[0] + 1;
            return v;
        });
    }
}
