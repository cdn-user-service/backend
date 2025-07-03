package io.ants.modules.utils.config.cccpay;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class CccYunPayForm {

    /**
     * 商户ID
     */
    @NotNull
    private Integer pid;

    /**
     * 支付方式 alipay | wxpay | qqpay
     */
    @NotNull
    private String type;

    /**
     * 商户订单号
     */
    @NotNull
    private String out_trade_no;

    @NotNull
    private String notify_url = "";

    private String return_url = "";

    /**
     * 商品名称
     */
    @NotNull
    private String name = "";

    /**
     * 商品金额 单位：元，最大2位小数
     */
    @NotNull
    private String money;

    /**
     * 用户发起支付的IP地址
     */
    @NotNull
    private String clientip;

    private String device = "pc";

    /**
     * 支付后原样返回
     */
    private String param;

    private String sign = "";

    @NotNull
    private String sign_type = "MD5";

}
