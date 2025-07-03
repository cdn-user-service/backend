package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class LoginByGoogleAuthForm {
    @NotNull
    private String user;

    @NotNull
    private String code;
}
