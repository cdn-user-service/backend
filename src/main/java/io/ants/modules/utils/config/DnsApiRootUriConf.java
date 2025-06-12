package io.ants.modules.utils.config;

import lombok.Data;

@Data
public class DnsApiRootUriConf {


    private String siteUrl="";

    //自建DNs 平台 api 路径     //https://www.vedns.com/ants-dns-api/
    private String apiRootPath="";

    //同步注册
    private int syncRegister=0;
}
