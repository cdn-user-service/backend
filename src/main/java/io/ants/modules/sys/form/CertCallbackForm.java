package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CertCallbackForm {


    @Data
    public class DnsRecordInfo{
        private String top;
        private String type;
        private String value;
    }

    //0=fail 1=success
    @NotNull
    private int code;
    @NotNull
    private int siteId;
    //code=0
    private String msg="";

    //0=cert_callback 1=add_dns_record
    @NotNull
    private int type=0;

    //type=1
    private DnsRecordInfo dnsRecordInfo;


    //type=0
    @NotNull
    private String pem="";
    //type=0
    @NotNull
    private String key="";



    private int usemode;
    private int dnsconfigid;
}
