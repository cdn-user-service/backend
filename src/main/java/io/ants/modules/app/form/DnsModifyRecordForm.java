package io.ants.modules.app.form;

import lombok.Data;

@Data
public class DnsModifyRecordForm {
    private Integer id;
    private String recordId;
    private String top;
    private String recordType;
    private String line;
    private String value;
    private String ttl ;
}
