package io.ants.modules.app.enums;

public enum BatchSearchUpdateSiteAttrEnum {

    B_SOURCE_IP("b_source_ip","source_base_info","修改回源IP"),
    B_SOURCE_IP_PORT("b_source_ip_port","source_base_info","修改回源IP:PORT"),
    B_SOURCE_LISTEN("b_source_listen","source_base_info","修改监听端口"),
    ;
    private String key;

    private String siteKey;

    private String remark;



    BatchSearchUpdateSiteAttrEnum(String key, String siteKey, String remark){
        this.key=key;
        this.siteKey=siteKey;
        this.remark=remark;
    }

    public String getKey() {
        return key;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public String getRemark() {
        return remark;
    }


    public static String getSitePkeyByKey(String key){
        for (BatchSearchUpdateSiteAttrEnum item: BatchSearchUpdateSiteAttrEnum.values()){
            if (item.getKey().equals(key)){
                return item.getSiteKey();
            }
        }
        return "";
    }
}
