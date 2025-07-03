package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class QuerySiteCachePrefetchPageForm {

    @NotNull
    private Integer page = 1;

    @NotNull
    private Integer limit = 20;

    private String userName;

    private String serverName;

    private String key;

}
