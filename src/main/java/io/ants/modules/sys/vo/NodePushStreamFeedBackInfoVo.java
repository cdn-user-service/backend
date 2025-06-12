package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NodePushStreamFeedBackInfoVo {
    private Integer code;
    private String msg;
    private long xLen;
    private String error_info;
    private String sn="";
    private Integer time;
}
