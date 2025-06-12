package io.ants.modules.app.form;

import lombok.Data;

@Data
public class QueryElkForm {
    private String method="GET";
    private String path="";
    private String param="";
}
