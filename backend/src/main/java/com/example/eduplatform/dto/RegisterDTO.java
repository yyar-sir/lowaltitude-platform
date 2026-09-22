package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册入参。
 *
 * <p>平台开放注册两类角色：终端客户（role=2）与物流运营商家（role=1）。
 * 平台管理员（role=0）不开放注册，由管理员在用户管理中创建。
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    @NotBlank(message = "名称不能为空")
    private String realName;

    private Integer role;

    private String phone;

    private String companyName;

    private String licenseNo;
}
