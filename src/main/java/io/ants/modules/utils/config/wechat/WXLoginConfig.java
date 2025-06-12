package io.ants.modules.utils.config.wechat;

import lombok.Data;

import java.io.Serializable;

@Data
public class WXLoginConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appId;
    private String appSecret;
    private String redirectUri;
}
