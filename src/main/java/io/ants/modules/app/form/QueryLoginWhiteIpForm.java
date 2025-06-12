package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueryLoginWhiteIpForm {

    private String ip;

    @NotNull
    private Integer page=1;

    @NotNull
    private Integer limit=20;
}
