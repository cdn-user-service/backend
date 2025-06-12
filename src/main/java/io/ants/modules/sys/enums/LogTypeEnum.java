package io.ants.modules.sys.enums;

import java.util.LinkedHashMap;

public enum LogTypeEnum {
    OTHER_LOG(0,"其它日志"),
    LOGIN_LOG(1,"登录日志"),
    OPERATION_LOG(2,"操作日志"),
    PRODUCT_LOG(3,"产品日志"),
    FINANCE_LOG(4,"财务日志"),
    DISPATCH_LOG(5,"调度日志"),
    ;
    final private   Integer id;
    final private  String name;
    LogTypeEnum(Integer id,String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static LinkedHashMap GetAll(){
        LinkedHashMap map=new LinkedHashMap();
        for (LogTypeEnum item : LogTypeEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }
}
