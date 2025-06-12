package io.ants.modules.sys.vo;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class NgxBaseInfoDefaultVo {

    @Data
    public class LineVo{
        private String ip;
        private String domain="";
        private String port;
        private Integer line=1;
        private Integer weight=1;
        private String s_protocol="";
    }

    private String protocol="http";
    private Integer port=80;
    private String s_protocol="http";
    private String upstream="polling";
    private String source_set="ip";
    private List<LineVo> line=new ArrayList<>();



}
