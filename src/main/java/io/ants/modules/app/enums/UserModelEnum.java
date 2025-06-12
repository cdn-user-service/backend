package io.ants.modules.app.enums;

import java.util.LinkedHashMap;

public enum UserModelEnum {
    // public final static String[] all_user_mode={"site","stream","work-order","message","log"};
    SITE("site","站点"),
    STREAM("stream","四层转发"),
    WORK_ORDER("work_order","工单"),
    MESSAGE("message","消息"),
    LOG("log","日志"),
    CERTIFY("certify","实名认认证"),
    ;
    private final String name;
    private final String title;

    UserModelEnum(String name,  String title){
        this.name=name;
        this.title=title;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public static LinkedHashMap getAll(){
        LinkedHashMap map=new LinkedHashMap();
        for (UserModelEnum item : UserModelEnum.values()) {
            map.put(item.getName(),item.getTitle());
        }
        return map;
    }

}
