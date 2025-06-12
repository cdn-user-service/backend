package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CheckExportForm {
    @NotNull(message = "token is not be null")
    private String token;

    @NotNull(message = "type is not be null")
    private String type;

    private String NodeCheckResult;
}
