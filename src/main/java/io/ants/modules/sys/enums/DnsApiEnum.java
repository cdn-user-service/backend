package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.List;

public enum DnsApiEnum {

    //@SuppressWarnings("AlibabaCommentsMustBeJavadocFormat")
    DNSPOD("dnspod",""),
    ALIYUN("aliyun",""),
    GODADDY("godaddy",""),
    ANTSDNS("antsdns",""),
    CLOUDFLARE("cloudflare","https://dash.cloudflare.com/"),
    DNS99DNS("99dns","https://dns.99dns.com/api/app/capi/"),
    DNSXDP("xdpdns","http://54.151.117.123/api/"),
    //https://dns.99dns.com/api/
    ;
    private String name;
    private String webSite;

    DnsApiEnum(String name,String webSite){
        this.name=name;
        this.webSite=webSite;
    }

    public String getName() {
        return name;
    }

    public String getWebSite() {
        return webSite;
    }

    public static List<String> getAllType(){
        List<String> list=new ArrayList();
        for (DnsApiEnum item:DnsApiEnum.values()){
            list.add(item.getName());
        }
        return list;
    }



}
