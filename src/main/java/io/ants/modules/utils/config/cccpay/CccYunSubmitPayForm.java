package io.ants.modules.utils.config.cccpay;

import lombok.Data;

@Data
public class CccYunSubmitPayForm {

    private String type;

    private String out_trade_no;

    private String name="cdn";

    private Integer amount;

    private String clientip;
}
