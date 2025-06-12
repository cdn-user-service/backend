package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ProductAttrItemVo {
    //"attr": "flow",
    //	"id": 58,
    //	"name": "流量",
    //	"valueType": "int",
    //	"unit": "G",
    //	"value": 204000

    private String attr;

    private String name;

    private String valueType;

    private String unit;

    private Object value;


}
