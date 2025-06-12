package io.ants.modules.utils.vo;

import lombok.Data;

@Data
public class AntsDnsRecordItemVo {
    // {"main_domain":"165668.com","tcp":0,"record_line":"100000000-999999999","poll_data":[],"operation_type":"add","line":{},"linetype":1,"prid":"0","weight":"1","mx":"1","ttl":"601","record_type":"A","record_id":"1701982124","poll_rule":0,"record_line_name":"默认","top":"*.4ad5281653","value":"121.62.17.151"}
    private String main_domain="";

    private int tcp=0;

    private String record_line="";

    private String operation_type="";

    private String record_type="";

    private String top="";

    private String value="";

    private int ttl=601;

    private String prid="";

    private String weight="";

    private int mx=1;

    private String record_id="";

    private int poll_rule=0;

    private String record_line_name="";

    private String record_line_id="";

    private int linetype=1;



}
