package io.ants.modules.sys.enums;

import java.util.LinkedHashMap;

public enum JavaJobEnum {
    CURLREQUESTURL("curlrequesturl","访问URL:参数为http.*开始的url","访问URL"),
    DISPATCHDNS("dispatchdns","节点调度：无参数","节点调度"),
    BYTES("bytes","流量记录：无参数","流量记录"),
    PAID("paid","计费监测：无参数","计费监测"),
    SSL("ssl","续签证书：无参数","续签证书"),
    ;
    private final String name;
    private final String remark;
    private final String title;

    JavaJobEnum(String name,String remark,String title){
        this.name=name;
        this.remark=remark;
        this.title=title;
    }

    public String getName() {
        return name;
    }

    public String getRemark() {
        return remark;
    }

    public String getTitle() {
        return title;
    }

    public static LinkedHashMap GetAll(){
        LinkedHashMap map=new LinkedHashMap();
        for (JavaJobEnum item : JavaJobEnum.values()) {
            map.put(item.getName(),item.getRemark());
        }
        return map;
    }

    public static String getCnNameByBeanName(String name){
        for (JavaJobEnum item : JavaJobEnum.values()) {
            if (item.getName().equals(name)){
                return item.getRemark();
            }
        }
        return "";
    }

    public static String getCnTitleByBeanName(String name){
        for (JavaJobEnum item : JavaJobEnum.values()) {
            if (item.getName().equals(name)){
                return item.getTitle();
            }
        }
        return "";
    }
}
