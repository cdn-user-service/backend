package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BatchAddSiteAttrForm {
    /**
     * 站点IDS【,分割】，传all为所有站
     */
    @NotNull
    private String siteIds;

    @NotNull
    private String key;

    /**
     * 值
     */
    private Object value;
}
