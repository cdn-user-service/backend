package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态：-【10待支付11过期12关闭13失败14成功15退款】
 * +【20待付21过期22关闭23失败24成功】
 */

public enum PayStatusEnum {
    PAY_UNKNOW(0,"未知"),
    PAY_NOT_PAY(10,"待支付"),
    PAY_OUT_TIME(11,"过期"),
    PAY_CLOSE(12,"关闭"),
    PAY_FAIL(13,"失败"),
    PAY_COMPLETE(14,"成功"),

    PAY_REQ_REFOUND(15,"请求退款"),

    PAY_REFOUND_WAIT(20,"退款待付"),
    PAY_REFOUND_OUT_TIME(21,"退款过期"),
    PAY_REFOUND_CLOSE(22,"退款关闭"),
    PAY_REFOUND_FAIL(23,"退款失败"),
    PAY_REFOUND_COMPLETE(24,"退款完成"),


    PAY_SYS_PAID(30,"系统扣出预付费"),

    ;
    private final Integer id;
    private final  String name;
    PayStatusEnum(Integer id, String name){
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
        for (PayStatusEnum item : PayStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }
}
