package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum CommandEnum {
    CLEAN_ERROR_LOG(1,"清理NGINX error日志","echo > /home/local/nginx/logs/error.log && echo > /home/local/nginx/logs/ants_access.log &&  echo > /home/local/nginx/logs/block.log &&  echo > /home/local/nginx/logs/stream.log","null"),
    RELOAD_NGINX(2,"重载 NGINX","/home/local/nginx/sbin/nginx -s reload","null"),
    PKILL_NGINX(3,"重启 NGINX","pkill nginx","null"),
    NGINX_VERSION(4,"获取ants_nginx版本","get nginx version","null"),
    INIT_DEL_DIR_COMMAND(5,"init_del_dir","init","null"),
    CLEAN_SHORT_CC_NFT(6,"清理short_cc","/usr/sbin/nft flush set inet filter short_cc","null"),
    PKILL_AGENT(7,"重启节点守护程序","pkill ants_agent","null"),
    CLEAN_SHORT_CC(8,"清理所有short_cc IP","/usr/sbin/ipset flush short_cc","null"),
    REBOOT_CUR_DEVICE(9,"重启当前设备","reboot","null"),
    SYS_WS_ON(10,"节点开ws","install_fw on",""),
    SYS_WS_OFF(11,"节点关ws","install_fw off",""),
    MIGRATE(12,"主控迁移","migrate",""),
    INIT_ELK(13,"初始化ELK","init elk",""),
    AI_ANALYSIS(14,"AI模型扫描日志","chmod +x  /home/local/nginx/logs/ants_ai_waf && /home/local/nginx/logs/ants_ai_waf -a",""),
    IP_TABLE_OUTPUT_F(15,"IPTABLE OUTPUT","iptables -F OUTPUT ",""),


    ;
    private final Integer id;

    private final String name;

    private final String content;

    private final String param;

    CommandEnum(Integer id,String name, String content,String param){
        this.id=id;
        this.name=name;
        this.content=content;
        this.param=param;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getParam() {
        return param;
    }

    public static Map getAll(){
        Map map=new HashMap();
        for (CommandEnum item : CommandEnum.values()) {
           map.put(item.getName(),item.getId());
        }
        return map;
    }

    public static String getContentById(Integer id){
        for (CommandEnum item : CommandEnum.values()) {
            if(id.equals(item.getId())){
                return item.getContent();
            }
        }
        return "";
    }

    public static Integer getCommandIdByContent(String content){
        for (CommandEnum item : CommandEnum.values()) {
            if(content==item.getContent()){
                return item.getId();
            }
        }
        return -1;
    }

    public static Map GetAll(){
        Map map=new HashMap();
        for (CommandEnum item : CommandEnum.values()) {
            String[] p={item.getParam(), item.getName()};
            map.put(item.getId(),p);
        }
        return map;
    }
}
