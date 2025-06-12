package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class SimpleSuitInfoVo {

    private Integer id;

    private String productName;

    private String serial_number;

    private Long endTime;

    private long usedFlow;
    private long totalFlow;

    private String bindSiteSum;

    private Integer status;
    private String statusMsg;

}
