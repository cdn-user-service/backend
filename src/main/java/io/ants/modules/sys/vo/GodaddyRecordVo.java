package io.ants.modules.sys.vo;

import io.ants.common.utils.HashUtils;
import lombok.Data;

@Data
public class GodaddyRecordVo {
    //"data": "string",
    //    "name": "string",
    //    "port": 65535,
    //    "priority": 0,
    //    "protocol": "string",
    //    "service": "string",
    //    "ttl": 0,
    //    "type": "A",
    //    "weight": 0
    //[{"data":"Parked","name":"@","ttl":600,"type":"A"},{"data":"1.12.1.1","name":"*.13213aaa","ttl":600,"type":"A"},{"data":"1.1.1.1","name":"111","ttl":600,"type":"A"},{"data":"1.1.1.1","name":"1112","ttl":600,"type":"A"},{"data":"1.1.1.1","name":"111212","ttl":3600,"type":"A"},{"data":"1.1.1.1","name":"1112122","ttl":3600,"type":"A"}]

    private String data;

    private String name;

    private Long ttl=3600L;

    private String type;

    private Integer weight=0;


    public  String getCalRecordId(){
        String key=this.getType()+"#"+this.getName()+"#"+this.getData();
        return  HashUtils.getCRC32(key);
    }
}
