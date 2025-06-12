package io.ants.modules.app.form;

import lombok.Data;

import java.util.List;

@Data
public class QueryCertPageForm {
    private Integer  page=1;

    private Integer limit=20;

    private String key="";

    private String user="";

    private Integer status;

    private List<Long> uids ;

    private String orderBy;

    private List<Long> notAfters;
}
