package io.ants.modules.app.form;

import lombok.Data;

@Data
public class RecordCleanForm {
    //Integer dnsConfigId,String mainDomain,String top,String recordType,String line,String value,String ttl
    private Integer dnsConfigId;
    private String mainDomain;
    private String top;
    private String recordType;
    private String line;
    private String value;
    private String ttl;
}
