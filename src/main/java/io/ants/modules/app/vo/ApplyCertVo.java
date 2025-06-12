package io.ants.modules.app.vo;

import lombok.Data;

@Data
public class ApplyCertVo {
    private Integer siteId;

    private Integer certId;

    private String domainList="";

    private String noticeCallBackCmd="";

    private int dnsConfigId;

    //0=acme-http01  2=acme-dns01
    private int mode;
}
