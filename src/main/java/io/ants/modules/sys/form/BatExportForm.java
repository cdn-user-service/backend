package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BatExportForm {

    @NotNull
    private String siteIds;

    @NotNull
    private String keys;
}
