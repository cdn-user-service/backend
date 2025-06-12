package io.ants.modules.job.task;

import java.util.LinkedHashMap;

public enum JavaJobEnum {
    CURLREQUESTURL("curlrequesturl","访问URL:参数为http.*开始的url","访问URL"),
    DISPATCHDNS("dispatchdns","节点调度：无参数","节点调度"),
    BYTES("bytes","流量记录：无参数","流量记录"),
    PAID("paid","计费监测：无参数","计费监测"),
    SSL("ssl","续签证书：无参数","续签证书"),
    CLEAN_LOG("clean_log","清理日志：无参数","清理日志"),
    AI_ANALYSIS("ai_analysis","ai分析日志：无参数","ai分析日志"),
    CHECK_SITE_CNAME("check_site_cname","检测域名解析状态：无参数","检测域名解析状态"),
    AutoPullCache("auto_pull_cache","自动缓存预取","自动缓存预取")
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
