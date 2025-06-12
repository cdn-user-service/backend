package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class DnsRecordInputPollDataVo {

    private String data="";
    private Integer weight=0;
    private Integer ttl=60;
}
