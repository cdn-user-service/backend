package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PageSimpleForm {
    @NotNull(message = "page is null")
    private  Integer page;

    @NotNull(message = "limit is null")
    private Integer limit;

    //搜索KEY
    private String key;
}
