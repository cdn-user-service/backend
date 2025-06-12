package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum IpControlEnum {
    UNKNOWN(0,"未知"),
    PASS_7(1,"7层持久白名单"),
    FORBID_7(2,"7层持久黑名单"),
    FORBID_3_LITTLE(3,"3层临时封禁"),
    FORBID_3_LONG(4,"3层持久封禁"),
    ;
    private final Integer id;

    private final String name;

    IpControlEnum(Integer id,String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Map getAll(){
        Map map=new HashMap();
        for (IpControlEnum item : IpControlEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }

    public static List<Integer> getAllControl(){
        List<Integer> ls=new ArrayList<>();
        for (IpControlEnum item : IpControlEnum.values()) {
             ls.add(item.getId());
        }
        return ls;
    }
}
