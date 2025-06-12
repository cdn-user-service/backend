package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class NgxStreamConfVo {
    //{
    //	"listen": 88|89|90,
    //	"protocol": "TCP",
    //	"server_mode": "weight",
    //	"proxy_protocol": 0,
    //	"proxy_timeout": "30s",
    //	"proxy_connect_timeout": "60s",
    //	"server": ["1.1.1.1:56 weight=1"]
    //}

    private String listen;
    private String protocol;
    private String server_mode;
    private Integer proxy_protocol=0;
    private String proxy_timeout;
    private String proxy_connect_timeout;
    private List<String> server;
}