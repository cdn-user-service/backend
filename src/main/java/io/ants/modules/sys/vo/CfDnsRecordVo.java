package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class CfDnsRecordVo {

    private String id;

    private String zone_name;

    private String name;

    private String type;

    private String content;

    private long ttl;


}
