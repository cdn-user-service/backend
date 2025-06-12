package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum TbCertifyStatusEnum {
    ////-1=待申请；0= 申请中; 1=申请成功;2=申请失败；3=自有证书;4=证书过期
    NEED_APPLY(-1,"待申请"),
    APPLYING(0,"申请中"),
    SUCCESS(1,"申请成功"),
    FAIL(2,"申请失败"),
    USER(3,"自有证书"),
    TIMEOUT(4,"证书过期"),
    NEED_AUTH(5,"等待验证"),
    EXCESS_CERT(6,"多余证书"),
    RE_APPLY(7,"已重签"),

    ;
    private  final  int id;
    private  final  String name;
    TbCertifyStatusEnum(Integer id, String name){
        this.id=id;
        this.name=name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static  Map getAll(){
        Map map=new HashMap();
        for (TbCertifyStatusEnum item : TbCertifyStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }

}
