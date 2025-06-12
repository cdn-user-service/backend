package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ElkServerVo {
    private String host="127.0.0.1";
    private String port="9200";
    private String pwd="";
    private String method="http";
    private String caPath="";
}
