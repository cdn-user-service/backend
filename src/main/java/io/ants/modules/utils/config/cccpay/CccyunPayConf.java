package io.ants.modules.utils.config.cccpay;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 彩虹易支付
 */
@Data
public class CccyunPayConf {

    @NotNull
    private String interfaceAddress="";

    /**
     * 商户ID
     */
    @NotNull
    private Integer pId;

    @NotNull
    private String secret;

    /**
     * 支付方式alipay | wxpay | qqpay
     */
    @NotNull
    private String payType="";

    @NotNull
    private String notify_url="";

    @NotNull
    private String return_url="";
}
