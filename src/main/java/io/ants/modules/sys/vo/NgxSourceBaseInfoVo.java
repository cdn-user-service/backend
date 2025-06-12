package io.ants.modules.sys.vo;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class NgxSourceBaseInfoVo {

    @Data
    public class LineVo{
        private String ip="";
        private String domain="";
        private String port="";
        //1=主 2=备
        private Integer line=1;
        private Integer weight=1;
        private int max_fails=0;
        private int fail_timeout=60;
        private String s_protocol="";
    }

    private int id;
    private String protocol="http";
    private Integer port=80;
    private String s_protocol="http";
    private String upstream="polling";
    private String source_set="ip";
    private int keepalive=1;
    private int keepalive_timeout=60;
    private List<LineVo> line=new ArrayList<>();



}
