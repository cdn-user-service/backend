package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class GoogleAuthBindForm {
    @NotNull
    private String code;

    @NotNull
    private String secretKey;
}
