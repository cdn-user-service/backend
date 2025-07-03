package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class DeleteIdsForm {
    @NotNull
    private String ids = "";
}
