package io.ants.modules.utils.vo;

import lombok.Data;

@Data
public class DnsRecordItemVo {
    private String recordId="";

    private String top="";

    private String value="";

    private String line="";

    private String ttl="600";

    private String type="";

    private String recordType="";
}
