package com.example.eduplatform.controller;

import com.example.eduplatform.common.Result;
import com.example.eduplatform.dto.LoginDTO;
import com.example.eduplatform.dto.RegisterDTO;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.security.RequireRole;
import com.example.eduplatform.security.UserContext;
import com.example.eduplatform.service.AuthService;
import com.example.eduplatform.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口。
 *
 * <p>本 Controller 下的路径必须在 WebConfig 的白名单里，
 * 否则未登录时调登录接口本身就会被拦截器拦掉（死循环）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（公开） */
    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    /** 注册（公开）：终端客户或物流运营商家 */
    @PostMapping("/auth/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    /** 当前登录用户信息（密码散列已 @JsonIgnore，不会下发） */
    @GetMapping("/user/me")
    @RequireRole({0, 1, 2})
    public Result<User> me() {
        return Result.ok(authService.getById(UserContext.userId()));
    }
}
