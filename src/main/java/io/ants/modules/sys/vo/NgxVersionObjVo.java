package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxVersionObjVo {
    //{
    //	"nginx_version": "1.21.4",
    //	"ants_waf": "3.4.1",
    //	"os": "centos"
    //}
    private String nginx_version;
    private String ants_waf;
    private String os;
}
