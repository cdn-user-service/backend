package io.ants.modules.utils.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class TokenPayConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    //tokenpay 站点地址
    private String webSiteUrl="";

    private String currency="USDT_TRC20";

    private String ApiToken="";

    private String callBackUrl="";

    private String redirectUrl="";


}
