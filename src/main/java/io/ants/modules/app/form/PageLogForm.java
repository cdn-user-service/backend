package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PageLogForm {
    @NotNull(message = "page is null")
    private  Integer page;

    @NotNull(message = "limit is null")
    private Integer limit;

    //private String method;

    //搜索KEY
    private String key;

    private String start_date;

    private  String end_data;

    private Integer logType;

    private String params_key;

}
