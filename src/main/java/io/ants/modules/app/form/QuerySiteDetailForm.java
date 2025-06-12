package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QuerySiteDetailForm {

    @NotNull(message = "id 不可为空")
    private Integer id;

    private Integer siteId;

    private String group;

    private String key;

}
