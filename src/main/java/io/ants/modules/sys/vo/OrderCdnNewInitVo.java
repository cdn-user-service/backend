package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class OrderCdnNewInitVo {
    //{"serialNumber":"1650361752071001","startTime":"1650769152","type":"m","sum":2}
    //"{\"serialNumber\":\"1673836321266002\",\"sum\":1,\"type\":\"m\",\"startTime\":1681434642996}"
    private String type="";
    private Integer sum=1;

}
