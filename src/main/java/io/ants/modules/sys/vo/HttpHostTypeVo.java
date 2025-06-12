package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class HttpHostTypeVo {

    private String  httpHost;

    //1=HOST 2=HTTP_HOST
    private int type;

    private int siteId;

    private Long userId;
}
