package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ProduceMiniVo {
    //  "createtime": 1659499295000,
    //	"productJson": "{\"m\":{\"value\":100,\"status\":1},\"s\":{\"value\":100,\"status\":1},\"y\":{\"value\":100,\"status\":1}}",
    //	"name": "流量月包",
    //	"serverGroupIds": "",
    //	"attrJson": "[{\"attr\":\"flow\",\"id\":58,\"name\":\"流量\",\"valueType\":\"int\",\"unit\":\"G\",\"value\":204000}]",
    //	"weight": 2,
    //	"id": 18,
    //	"productType": 12,
    //	"status": 1

    private Integer id;

    private String name;

    private String productJson;

    private String attrJson;



    private Integer productType;

    private String serverGroupIds;
}
