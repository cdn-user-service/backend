package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum CdnSuitStatusEnum {
    /*
    * */
    UNKNOWN(0,"不可用"),
    NORMAL(1,"正常启用"),
    UPGRADE(2,"已升级"),
    TIMEOUT(3,"已过期"),
    DISABLE(4,"被禁用"),
    CANCELLATION(5,"被注销"),
    LIQUIDATION(6,"已清算"),
    USED_RUN_OUT(7,"已用完")
    ;
    private  final  Integer id;
    private  final  String name;
    CdnSuitStatusEnum(Integer id,String name){
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
        for (CdnSuitStatusEnum item : CdnSuitStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }

    public static List<Integer> getAllStatus(){
        List<Integer> rList=new ArrayList<>();
        for (CdnSuitStatusEnum item : CdnSuitStatusEnum.values()) {
            rList.add(item.getId());
        }
        return rList;
    }

    public static String statusMsg(Integer status){
        if (null==status){
            return "未知";
        }
        for (CdnSuitStatusEnum item : CdnSuitStatusEnum.values()) {
            if (status==item.getId()){
                return item.getName();
            }
        }
        return "未知";
    }

}
