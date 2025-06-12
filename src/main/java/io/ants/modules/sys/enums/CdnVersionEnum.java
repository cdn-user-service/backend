package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum CdnVersionEnum {

    M_JAVA_JAR(1,"web_controller","接口单元",""),
    M_WEB_MANAGER(1,"web_view_manager","管理平台",""),
    M_WEB_USER(1,"web_view_user","用户平台",""),
    M_MYSQL_DB(1,"db","数据库",""),
    M_NODE_NGINX(2,"nginx","节点",""),
    M_NODE_AGENT(2,"ants_agent","节点守护程序",""),
    ;
    private int group;
    private String  key;
    private String name;
    private String remark;
    CdnVersionEnum(int group,String key,String name,String remark){
        this.group=group;
        this.key=key;
        this.name=name;
        this.remark=remark;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getGroup() {
        return group;
    }

    public static CdnVersionEnum getEnumItemByKey(String key){
        for (CdnVersionEnum item:CdnVersionEnum.values()){
           if (item.getKey().equals(key)){
               return item;
           }
        }
        return null;
    }

    public static String getNameByKey(String key){
        for (CdnVersionEnum item:CdnVersionEnum.values()){
            if (item.getKey().equals(key)){
                return item.name;
            }
        }
        return "";
    }

    public static Map<String,String> getAllKN(){
        Map<String,String> m=new HashMap();
        for (CdnVersionEnum item:CdnVersionEnum.values()){
            m.put(item.key,item.name);
        }
        return m;
    }
}
