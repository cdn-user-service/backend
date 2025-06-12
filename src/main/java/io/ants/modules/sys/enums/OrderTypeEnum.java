package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum OrderTypeEnum {
    ORDER_AUTHENTICATION(1,"实名认证"),
    ORDER_RECHARGE(2,"充值"),
    ORDER_CDN_SUIT(10,"CDN套餐"),
    ORDER_CDN_RENEW(11,"CDN续费"),
    ORDER_CDN_ADDED(12,"增值服务"),
    ORDER_CDN_UPGRADE(13,"套餐升级"),
    ORDER_SYS_REFUND(20,"产品退款"),
    ORDER_SYS_PAID(30,"后付费扣款订单"),

    ;
    private  final  Integer type_id;

    private final  String name;

    OrderTypeEnum(Integer type_id,String name){
        this.type_id=type_id;
        this.name=name;
    }

    public int getTypeId() {
        return type_id;
    }

    public String getName() {
        return name;
    }

    public static Map GetAllType(){
        Map map=new HashMap();
        for (OrderTypeEnum item : OrderTypeEnum.values()) {
            map.put(item.getTypeId(),item.getName());
        }
        return map;
    }
}
