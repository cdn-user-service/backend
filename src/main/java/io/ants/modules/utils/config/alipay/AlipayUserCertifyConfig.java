package io.ants.modules.utils.config.alipay;

import lombok.Data;

import java.io.Serializable;

@Data
public class AlipayUserCertifyConfig  implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String cost;
}
