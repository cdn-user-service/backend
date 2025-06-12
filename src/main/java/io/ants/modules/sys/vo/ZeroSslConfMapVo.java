package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ZeroSslConfMapVo {
    private String siteId;
    private String cert_verify_zero_ssl_uri;
    private String cert_verify_zero_ssl_value;
}
