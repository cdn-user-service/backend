package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxSourceLineVo {
    //	"line": [{
    //		"ip": "121.62.18.146",
    //		"domain": "",
    //		"port": "80",
    //		"line": 1,
    //		"weight": 1
    //	}]
    private String ip;
    private String domain;
    private String port;
    //1=主 2=备
    private Integer line=1;
    private Integer weight=0;
    private String s_protocol="";
}
