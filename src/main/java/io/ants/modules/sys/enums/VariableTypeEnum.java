package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum VariableTypeEnum {
    VAR_TYPE_CONSTANT(0,"常量"),
    VAR_TYPE_CALCULATION(1,"指定计算"),
    VAR_TYPE_FROM_TABLE(2,"来之表"),
    VAR_TYPE_FROM_FUNCTION(3,"反射求值（CLASS ,FUNCTION,PARAM）"),
    VAR_TYPE_FROM_HTTP(4,"http获取方式(URL,PARAM)"),
    ;
    //(0=常量;1:计算;2=来之表)
    private  final Integer type;
    private  final String name;
    VariableTypeEnum(Integer type,String name){
        this.type=type;
        this.name=name;
    }

   public Integer getType(){
        return this.type;
   }

    public String getName() {
        return this.name;
    }

    public static Map GetAllType(){
        Map map=new HashMap();
        for (VariableTypeEnum item : VariableTypeEnum.values()) {
            map.put(item.getType(),item.getName());
        }
        return map;
    }
}
