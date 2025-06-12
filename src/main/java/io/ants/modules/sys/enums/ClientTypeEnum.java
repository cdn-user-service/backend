package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */

public enum ClientTypeEnum {
    UNKNOWN(0,"未知","-"),
    MAIN_NODE(1,"主节点IP","node"),
    MIDDLE_NODE(2,"中间节点","-"),
    BACKUP_NODE(3,"主节点备用节点","back"),
    CACHE_LEVEL_2(4,"二级缓存节点","node"),
    CACHE_LEVEL_3(5,"三级缓存节点","node"),
    ;
    private  final  Integer id;
    private  final  String name;
    private  final  String group;
    ClientTypeEnum(Integer id, String name,String group){
        this.id=id;
        this.name=name;
        this.group=group;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }


    public String getGroup() {
        return group;
    }

    public static Map getAllType(){
        Map map=new HashMap();
        for (ClientTypeEnum item : ClientTypeEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }

    public static List<Integer> getClientTypes(String group){
        List<Integer> ls=new ArrayList<>();
        for (ClientTypeEnum item : ClientTypeEnum.values()) {
            if(item.getGroup().equals(group)){
                ls.add(item.getId());
            }
        }
        return ls;
    }
}
