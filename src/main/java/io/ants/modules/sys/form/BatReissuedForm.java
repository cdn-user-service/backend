package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BatReissuedForm {

    @NotNull
    private String ids;

    //-1=auto;0==let_http;1==zero_http;2==let_dns_api 3=certServer
    private int useMode=-1;


    private int dnsConfigId;
}
