package io.ants.modules.app.vo;

import lombok.Data;

import java.util.*;

@Data
public class AcmeDnsVo {

    @Data
    public class TxtDomainValue{
        private String domain="";
        private String top="";
        private String mainDomain="";
        private String value="";
        private String type="TXT";
    }


    private Integer code=0;
    private String msg="";
    //0=fail 1==applying 2=success  3=需要添加TXT记录 4=需要添加CNAME记录
    private int status=0;
    private Map<String,TxtDomainValue> tvMap=new HashMap<String,TxtDomainValue>();
    private String certCrt="";
    private String certKey="";
    private Date endDate;
    private int siteId;
    private int certId;
    //0=acme-http01 1=zero-HTTP; 2=acme-dns01;3=ZERO-DNS
    private int mode;
    private int dnsConfigId;
    private String noticeCallBackCmd="";


}
