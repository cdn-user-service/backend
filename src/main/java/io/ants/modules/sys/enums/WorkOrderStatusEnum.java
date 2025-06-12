package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum WorkOrderStatusEnum {
    UNKNOWN(0,"处理中"),
    NORMAL(1,"处理完成"),
    UPGRADE(2,"申诉处理中"),
    DISABLE(3,"结单"),
    TIMEOUT(4,"超时结单"),
    ;
    private  final  Integer id;
    private  final  String name;
    WorkOrderStatusEnum(Integer id, String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static  Map getAll(){
        Map map=new HashMap();
        for (WorkOrderStatusEnum item : WorkOrderStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }

}
