package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxSiteUriRewriteVo {
    private Integer id=0;

    private String path="";

    private String rewriteMode="";

    private String rewritePath="";

    private Integer status=1;

}
