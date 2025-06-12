package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class AntsDnsRecordVo {
    private String record_id;
    private String top;
    private String value;
    private String ttl;
    private String record_type;
    private String record_line_name;

}
