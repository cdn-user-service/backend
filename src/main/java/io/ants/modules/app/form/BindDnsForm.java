package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BindDnsForm {

    @NotNull
    private String account;

    @NotNull
    private String password;
}
