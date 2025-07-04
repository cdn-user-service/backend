package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum LogUserTypeEnum {
    MANAGER_TYPE(1,"管理"),
    USER_TYPE(2,"用户");
    private final Integer id;
    private final String name;
    LogUserTypeEnum(Integer id, String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Map GetAll(){
        Map map=new HashMap();
        for (LogUserTypeEnum item : LogUserTypeEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }
}
