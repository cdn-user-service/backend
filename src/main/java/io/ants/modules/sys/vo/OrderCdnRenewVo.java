package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class OrderCdnRenewVo {
    //{"serialNumber":"1650361752071001","type":"m","sum":2}
    //"{\"serialNumber\":\"1673836321266002\",\"sum\":1,\"type\":\"m\"}"
    private String serialNumber="";
    private String type="";
    private Integer sum=1;


}
