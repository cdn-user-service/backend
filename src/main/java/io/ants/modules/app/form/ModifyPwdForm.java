package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "修改密码 表单")
public class ModifyPwdForm {

    @Schema(description = "旧密码")
    @NotBlank(message = "旧密码 不能为空")
    private String o_password;

    @Schema(description = "新密码")
    @NotBlank(message = "新密码 不能为空")
    private String n_password;

    @Schema(description = "uuid")
    @NotBlank(message = "uuid 不能为空")
    private String uuid;

    @Schema(description = "图形验证码", example = "0000")
    @NotBlank(message = "验证码 不能为空")
    private String code;
}
