package io.ants.modules.utils.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class ZeroSslConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    //0 系统默认 1==zeroSsl
    private int  configId=0;

    private String api_key;

    private String eab_kid;

    private String eab_hmac_key;
}
