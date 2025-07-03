package io.ants.modules.sys.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class BatExportForm {

    @NotNull
    private String siteIds;

    @NotNull
    private String keys;
}
