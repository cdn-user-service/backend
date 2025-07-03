package io.ants.modules.sys.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class CertReIssuedVo {

    private Integer id;

    // -1=auto;0==let_http;1==zero_http;2==let_dns_api 3=let_http_v2;4=let_dns_v2
    @NotNull
    private int type;

    private String ids;

    private int dnsConfigId;
}
