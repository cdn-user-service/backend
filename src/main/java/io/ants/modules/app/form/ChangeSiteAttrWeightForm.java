package io.ants.modules.app.form;

import lombok.Data;

@Data
public class ChangeSiteAttrWeightForm {
    private Long userId;
    private Integer siteId ;
    private String pkey ;
    private Integer attrId ;
    private Integer opMode;
}
