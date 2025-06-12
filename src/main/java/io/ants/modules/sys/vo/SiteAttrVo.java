package io.ants.modules.sys.vo;

import lombok.Data;


@Data
public class SiteAttrVo {

    private  int id=0;

    private int siteId=0;

    private String pkey;

    private String pvalue;

    private int status;

    private int weight;


}
