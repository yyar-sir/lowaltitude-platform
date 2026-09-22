package com.example.eduplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.common.Result;
import com.example.eduplatform.entity.OperationLog;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.mapper.OperationLogMapper;
import com.example.eduplatform.mapper.UserMapper;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.MessageService;
import com.example.eduplatform.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端：用户与商家资质管理、操作日志。
 *
 * <p>类级 @RequireRole(0)：整个 Controller 仅管理员可调。
 * 角色编码 0=管理员 1=物流运营商家 2=终端客户。
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole(0)
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final OperationLogMapper logMapper;
    private final OperationLogService opLog;
    private final MessageService messageService;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @GetMapping("/users")
    public Result<Map<String, Object>> users(@RequestParam(required = false) Integer role,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(User::getUsername, keyword)
                                .or().like(User::getRealName, keyword)
                                .or().like(User::getCompanyName, keyword))
                .orderByAsc(User::getRole)
                .orderByDesc(User::getId);
        List<User> list = userMapper.selectList(q);
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("total", list.size());
        return Result.ok(m);
    }

    /** 新增用户（管理员可创建商家/客户账号） */
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BizException(1002, "用户名不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(1005, "用户名已存在");
        }
        User u = new User();
        u.setUsername(dto.getUsername().trim());
        String pwd = (dto.getPassword() == null || dto.getPassword().isBlank())
                ? "123456" : dto.getPassword();
        u.setPassword(ENCODER.encode(pwd));
        u.setRealName(dto.getRealName() == null ? dto.getUsername() : dto.getRealName());
        u.setNickname(dto.getNickname() == null ? u.getRealName() : dto.getNickname());
        u.setAvatar(dto.getAvatar() == null ? "🙂" : dto.getAvatar());
        u.setRole(dto.getRole() == null ? 2 : dto.getRole());
        u.setPhone(dto.getPhone());
        u.setCompanyName(dto.getCompanyName());
        u.setLicenseNo(dto.getLicenseNo());
        u.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        u.setNotifyEnabled(1);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);
        opLog.record("新增用户", "创建账号 " + u.getUsername() + "（角色 " + u.getRole() + "）");
        return Result.ok(u);
    }

    /** 编辑用户资料与资质（密码留空表示不改） */
    @PostMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User dto) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(1001, "用户不存在");
        }
        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            u.setRealName(dto.getRealName());
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            u.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()) {
            u.setAvatar(dto.getAvatar());
        }
        if (dto.getRole() != null) {
            u.setRole(dto.getRole());
        }
        if (dto.getPhone() != null) {
            u.setPhone(dto.getPhone());
        }
        if (dto.getCompanyName() != null) {
            u.setCompanyName(dto.getCompanyName());
        }
        if (dto.getLicenseNo() != null) {
            u.setLicenseNo(dto.getLicenseNo());
        }
        if (dto.getStatus() != null) {
            u.setStatus(dto.getStatus());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            u.setPassword(ENCODER.encode(dto.getPassword()));
            u.setPasswordChangedAt(LocalDateTime.now());
        }
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        opLog.record("编辑用户", "修改账号 " + u.getUsername());
        return Result.ok(u);
    }

    /** 冻结 / 解冻账号（冻结商家会阻断其接单） */
    @PostMapping("/users/{id}/status")
    public Result<User> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(1001, "用户不存在");
        }
        if (id.equals(UserContext.userId())) {
            throw new BizException(1001, "不能禁用当前登录的管理员账号");
        }
        u.setStatus(status == null ? 1 : status);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        String action = u.getStatus() == 1 ? "启用" : "禁用";
        opLog.record("账号状态变更", u.getUsername() + " → " + action);
        messageService.send(u.getId(), "info", "账号状态变更", "你的账号已被管理员" + action, "/login");
        return Result.ok(u);
    }

    /** 删除账号 */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        if (id.equals(UserContext.userId())) {
            throw new BizException(1001, "不能删除当前登录的账号");
        }
        User u = userMapper.selectById(id);
        userMapper.deleteById(id);
        opLog.record("删除用户", "删除账号 " + (u == null ? id : u.getUsername()));
        return Result.ok();
    }

    /** 重置密码（默认 123456，并作废该账号已签发的 token） */
    @PostMapping("/users/{id}/reset-password")
    public Result<Map<String, Object>> resetPassword(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(1001, "用户不存在");
        }
        u.setPassword(ENCODER.encode("123456"));
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        opLog.record("重置密码", "重置账号 " + u.getUsername() + " 的密码");
        return Result.ok(Map.of("username", u.getUsername(), "newPassword", "123456"));
    }

    @GetMapping("/logs")
    public Result<List<OperationLog>> logs() {
        return Result.ok(logMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt)
                .orderByDesc(OperationLog::getId)
                .last("limit 200")));
    }
}
