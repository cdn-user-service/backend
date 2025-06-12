package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 */

public enum ClientStatusEnum {
    UNKNOWN(0,"未知"),
    ALREADY_REGISTER(1,"已注册"),
    REMOTE_EMPTY(2,"授权不存在")
    ;
    private  final  Integer id;
    private  final  String name;
    ClientStatusEnum(Integer id,String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public static Map getAllType(){
        Map map=new HashMap();
        for (ClientStatusEnum item : ClientStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }
}
