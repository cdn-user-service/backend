package io.ants.modules.app.form;

import lombok.Data;

@Data
public class QuerySiteGroupForm {
    private String name;
    private Integer page=1;
    private Integer limit=20;

}
