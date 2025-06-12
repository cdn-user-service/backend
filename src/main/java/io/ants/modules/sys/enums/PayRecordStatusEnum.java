package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 */

public enum PayRecordStatusEnum {
    STATUS_UNKNOWN(0,"未处理"),
    STATUS_SUCCESS(1,"处理成功"),
    STATUS_PRICE_UNEQUAL(2,"价格不相等"),
    STATUS_UNKNOWN_ORDER(3,"未知订单"),
    STATUS_ERROR(4,"数据异常"),
    STATUS_OUT_TIME(5,"超时（24小时）"),
    ;
    private final Integer id;
    private final String name;
    PayRecordStatusEnum(Integer id,String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId(){
        return id;
    }

    public String getName(){
        return  name;
    }

    public static Map<Integer,String> getAllStatus() {
        Map<Integer,String> map =new HashMap<>();
        for (PayRecordStatusEnum item : PayRecordStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }
}
