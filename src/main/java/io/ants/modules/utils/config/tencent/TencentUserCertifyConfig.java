package io.ants.modules.utils.config.tencent;

import lombok.Data;

import java.io.Serializable;


@Data
public class TencentUserCertifyConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String secretId;
    private String secretKey;
    private String RuleId;
    private String cost;
}
