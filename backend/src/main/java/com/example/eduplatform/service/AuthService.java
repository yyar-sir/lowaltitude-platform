package com.example.eduplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eduplatform.common.BizException;
import com.example.eduplatform.dto.LoginDTO;
import com.example.eduplatform.dto.RegisterDTO;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.mapper.UserMapper;
import com.example.eduplatform.security.JwtUtil;
import com.example.eduplatform.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 账号与权限：登录、注册 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        // 账号不存在与密码错误返回同一文案，避免账号枚举
        if (user == null || !ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(1003, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(1004, "账号已被禁用，请联系管理员");
        }
        return toVO(user);
    }

    public LoginVO register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(1005, "用户名已存在");
        }
        int role = dto.getRole() == null ? 2 : dto.getRole();
        // 管理员不开放注册
        if (role == 0) {
            throw new BizException(1001, "管理员账号不开放注册");
        }
        // 商家注册必须带企业与资质信息，否则后续无法承接订单
        if (role == 1 && (dto.getCompanyName() == null || dto.getCompanyName().isBlank()
                || dto.getLicenseNo() == null || dto.getLicenseNo().isBlank())) {
            throw new BizException(1002, "商家注册需填写企业名称与空域运营许可编号");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(ENCODER.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setNickname(dto.getRealName());
        user.setAvatar(role == 1 ? "🚁" : "🏢");
        user.setRole(role);
        user.setPhone(dto.getPhone());
        user.setCompanyName(dto.getCompanyName());
        user.setLicenseNo(dto.getLicenseNo());
        user.setStatus(1);
        user.setNotifyEnabled(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        return toVO(user);
    }

    public User getById(Long id) {
        return id == null ? null : userMapper.selectById(id);
    }

    private LoginVO toVO(User user) {
        return LoginVO.builder()
                .token(jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole()))
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .realName(user.getRealName())
                .companyName(user.getCompanyName())
                .build();
    }
}
