package io.ants.modules.utils.config.cccpay;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CccYunNotifyForm {

    @NotNull
    private Integer pid;
    /**
     * 	彩虹易支付订单号
     */
    @NotNull
    private String  trade_no;
    /**
     * 商户系统内部的订单号
     */
    @NotNull
    private String  out_trade_no;
    @NotNull
    private String  type;
    @NotNull
    private String  name;
    @NotNull
    private String  money;

    /**
     * 支付状态只有TRADE_SUCCESS是成功
     */
    @NotNull
    private String  trade_status;
    private String  param;
    private String  sign="";
    private String  sign_type="MD5";
}
