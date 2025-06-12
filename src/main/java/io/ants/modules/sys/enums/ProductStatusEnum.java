package io.ants.modules.sys.enums;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ProductStatusEnum {
    DISABLE(0,"下架（不可购买不可续费）"),
    ENABLE(1,"上架（可购买可续费）"),
    ONLY_BUY(2,"体验版(可新购不可续费)"),
    ONLY_RENEW(3,"典藏版（不可新购可续费）"),
    ONLY_FIRST(4,"注册赠送（账号注册赠送套餐,不可购不可续费）"),
    ;
    private  final Integer id;
    private  final String name;
    ProductStatusEnum(Integer id,String name){
        this.id=id;
        this.name=name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Map<Integer,String> getAllType() {
        Map<Integer,String> map =new HashMap<>();
        for (ProductStatusEnum item : ProductStatusEnum.values()) {
            map.put(item.getId(),item.getName());
        }
        return map;
    }

    public static List<Integer> canBuyStatus(){
        List<Integer>list=new ArrayList<>(64);
        list.add(ProductStatusEnum.ENABLE.getId());
        list.add(ProductStatusEnum.ONLY_BUY.getId());
        list.add(ProductStatusEnum.ONLY_FIRST.getId());
        return list;
    }


    public static  List<Integer> canRenewStatus(){
        List<Integer>list=new ArrayList<>(64);
        list.add(ProductStatusEnum.ENABLE.getId());
        list.add(ProductStatusEnum.ONLY_RENEW.getId());
        return list;
    }

    public static List<Integer> canUpStatus(){
        List<Integer>list=new ArrayList<>(64);
        list.add(ProductStatusEnum.ENABLE.getId());
        list.add(ProductStatusEnum.ONLY_BUY.getId());
        list.add(ProductStatusEnum.ONLY_RENEW.getId());
        return list;
    }
}
