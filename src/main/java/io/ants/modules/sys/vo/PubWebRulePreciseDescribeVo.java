package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class PubWebRulePreciseDescribeVo {
    private String name;
    private String remark;
    private int botCheckHttpStatusCode=200;

}
