package io.ants.modules.sys.enums;

public enum TaskOperationTypeEnum {
    ADD_SITE("addSite","添加站点"),
    DEL_SITE("delSite","删除站点"),
    SAVE_SITE_SSL("saveSiteSSL","保存站点SSL 文件"),
    DEL_SITE_SSL("saveSiteSSL","删除站点SSL 文件"),
    ADD_STREAM_PROXY("addStreamProxy","添加端口转发"),
    DEL_STREAM_PROXY("delStreamProxy","删除端口转发"),
    ADD_SITE_WAF("addWaf","保存WAF"),
    DEL_SITE_WAF("delWaf","删除WAF"),
    OPERATION_PUBLIC_NET_WAF("net_waf","nft网络防火墙"),
    INIT_ALL_NODE("initAllNode","始始化所有节点"),
    INIT_NODE("initNode","始始化节点")
    ;
    private final String name;
    private final String describe;
    TaskOperationTypeEnum(String name,String describe){
        this.name=name;
        this.describe=describe;
    }

    public String getName() {
        return name;
    }

    public String getDescribe() {
        return describe;
    }
}
