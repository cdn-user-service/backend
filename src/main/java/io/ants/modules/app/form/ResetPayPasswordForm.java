package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class ResetPayPasswordForm {

    @Schema(description = "newPayPassword", example = "123456")
    @NotNull(message = "code 不能为空")
    private String newPayPassword;

    private String mail;

    private String mobile;

    @Schema(description = "code", example = "1111")
    @NotNull(message = "code 不能为空")
    private String code;

}
