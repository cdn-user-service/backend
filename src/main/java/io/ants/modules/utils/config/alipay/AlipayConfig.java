package io.ants.modules.utils.config.alipay;

import lombok.Data;

import java.io.Serializable;


/**
 * @author Administrator
 */
@Data
public class AlipayConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String returnUrl;
    private String notifyUrl;
}
