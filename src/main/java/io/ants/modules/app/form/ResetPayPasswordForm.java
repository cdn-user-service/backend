package io.ants.modules.app.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ResetPayPasswordForm {

    @ApiModelProperty(value = "newPayPassword",example = "123456")
    @NotNull(message="code 不能为空")
    private String newPayPassword;

    private String mail;

    private  String mobile;

    @ApiModelProperty(value = "code",example = "1111")
    @NotNull(message="code 不能为空")
    private String code;

}
