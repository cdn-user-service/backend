package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NetNftVo {
    //{"ip":"127.0.0.1","port":"8080","protocol":"all","rule":"accept","remark":"1"}
    private String ip;
    private String port;
    private String protocol;
    private String rule;
}
