package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum PayTypeEnum {

    PRO_TYPE_sys(0,"余额支付"),
    PRO_TYPE_admin(1,"后台支付"),
    PRO_TYPE_alipay(2,"支付宝支付"),
    PRO_TYPE_wechat(3,"微信支付"),
    PRO_TYPE_fuiou(4,"富友支付"),
    PRO_TYPE_bank(5,"银联支付"),
    PRO_TYPE_paid(6,"预扣费"),
    //PRO_TYPE_STRIPE(7,"stripe"),
    PRO_TYPE_TOKENPAY(8,"tokenpay"),
    PRO_TYPE_CCCYUN(9,"cccyun"),
    PRO_TYPE_ALLINPAY(10,"allinpay"),
    ;
    private  final  Integer id;
    private  final String name;
    PayTypeEnum(Integer id, String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public static Map<Integer,String> getallpaytype() {
        Map<Integer,String> map =new HashMap<>();
        for (PayTypeEnum item : PayTypeEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }
}
