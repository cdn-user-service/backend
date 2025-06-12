package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DnsAddRecordForm {
    @NotNull(message = "id 不能为空！")
    private Integer id;

    private String top;
    private String recordType;
    private String line;
    private String value;
    private String ttl ;

    private String domain;
}
