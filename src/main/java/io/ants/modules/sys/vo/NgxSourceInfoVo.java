package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class NgxSourceInfoVo {
    //	"protocol": "https",
    //	"port": 44433,
    //	"s_protocol": "http",
    //	"upstream": "polling",
    //	"source_set": "ip",
    //	"line": [{
    //		"ip": "121.62.18.146",
    //		"domain": "",
    //		"port": "80",
    //		"line": 1,
    //		"weight": 1
    //	}]
    private String protocol;
    private Integer port;
    private String s_protocol;
    private String upstream;
    private String source_set;
    private int keepalive=1;
    private int keepalive_timeout=60;
    private List<NgxSourceLineVo> line;



}
