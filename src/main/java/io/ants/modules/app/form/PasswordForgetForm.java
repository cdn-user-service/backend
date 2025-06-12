package io.ants.modules.app.form;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("找回密码表单")
public class PasswordForgetForm {

    @ApiModelProperty(value = "user_id",hidden = true)
    private Long userId;

    @ApiModelProperty(value = "手机号",example = "手机号(邮箱二选一 非必填)")
    private String mobile;

    @ApiModelProperty(value = "邮箱",example = "邮箱(手机二选一 非必填)")
    private String mail;


    @ApiModelProperty(value = "新密码",example = "新密码 必填")
    @NotBlank(message = "新密码 不能为空")
    private String password;


    @ApiModelProperty(value = "验证码",example = "验证码 必填")
    @NotBlank(message = "验证码不能为空")
    private String code;

}
