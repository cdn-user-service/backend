package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class BatchSearchModifySiteAttrForm {

    /**
     * 站点IDS【,分割】，传all为所有站
     */
    @NotNull
    private String siteIds;

    @NotNull
    private String key;

    /**
     * 搜索值
     */
    private String s_value;

    /**
     * 修改值
     */
    private String t_value;

    /**
     * 值为1是不修改，只返回搜索到的结果数
     */
    private int test = 0;
}
