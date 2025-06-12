package io.ants.modules.utils.config;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class StripeConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String apiKey="";

    //usd转cny汇率
    private BigDecimal fxRate=new BigDecimal(7.3);

    //默认基础货币，小写
    private String baseCurrency="cny";

    private String endpointSecret="";

    private String noticeUrl="";

    private String webHookUrl="";


}
