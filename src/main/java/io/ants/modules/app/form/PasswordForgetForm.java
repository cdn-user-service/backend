package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "找回密码表单")
public class PasswordForgetForm {

    @Schema(description = "user_id", hidden = true)
    private Long userId;

    @Schema(description = "手机号", example = "手机号(邮箱二选一 非必填)")
    private String mobile;

    @Schema(description = "邮箱", example = "邮箱(手机二选一 非必填)")
    private String mail;

    @Schema(description = "新密码", example = "新密码 必填")
    @NotBlank(message = "新密码 不能为空")
    private String password;

    @Schema(description = "验证码", example = "验证码 必填")
    @NotBlank(message = "验证码不能为空")
    private String code;

}
