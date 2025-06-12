package io.ants.modules.sys.form;

import lombok.Data;

@Data
public class QueryCdnProductForm {
    private Integer userType=1;
    private Integer page=1;
    private Integer limit=20;
    private String key="";
    //可销售
    private String vendibility="";
    private String productTypes="";
}
