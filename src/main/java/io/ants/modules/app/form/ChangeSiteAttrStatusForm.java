package io.ants.modules.app.form;

import lombok.Data;

@Data
public class ChangeSiteAttrStatusForm {
    private Long userId;
    private Integer attrId;
    private Integer siteId ;
    private String pkey ;
    private Integer status ;
}
