package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "修改密码 表单")
public class ModifyPwdForm {

    @ApiModelProperty(value = "旧密码")
    @NotBlank(message="旧密码 不能为空")
    private String o_password;

    @ApiModelProperty(value = "新密码")
    @NotBlank(message="新密码 不能为空")
    private String n_password;

    @ApiModelProperty(value = "uuid")
    @NotBlank(message="uuid 不能为空")
    private String uuid;

    @ApiModelProperty(value = "图形验证码",example = "0000")
    @NotBlank(message="验证码 不能为空")
    private String code;
}
