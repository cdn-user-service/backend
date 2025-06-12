package io.ants.modules.sys.enums;

import java.util.HashSet;
import java.util.Set;

public enum CacheKeyEnums {
    cert_apply_callback_url("TbCdnPublicMutAttrEntity","cert_apply_callback_url",5*60,"TbCdnPublicMutAttrEntity"),
    cert_apply_mode("TbCdnPublicMutAttrEntity","cert_apply_mode",5*60,"TbCdnPublicMutAttrEntity"),
    cert_apply_proxy_pass("TbCdnPublicMutAttrEntity","cert_apply_proxy_pass",5*60,"TbCdnPublicMutAttrEntity"),
    site_group_info("site_group_info","site_group_info",5*60,"jsonString"),
    ;
    final String group;
    final String keyName;
    final long expireTime;
    final String object;
    CacheKeyEnums(String group, String keyName, long expireTime,String object) {
        this.group = group;
        this.keyName = keyName;
        this.expireTime = expireTime;
        this.object=object;
    }



    public String getGroup() {
        return group;
    }

    public String getKeyName() {
        return keyName;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public static CacheKeyEnums getGroupByKey(String key){
        for (CacheKeyEnums item : CacheKeyEnums.values()) {
            if (item.getKeyName().equals(key)){
                return item;
            }
        }
        return null;
    }


    public static Set<String> getKeyByGroup(String group){
        Set<String> resultSet = new HashSet<>();
        for (CacheKeyEnums item : CacheKeyEnums.values()) {
            if (item.getGroup().equals(group)){
                resultSet.add(item.getKeyName());
            }
        }
        return resultSet;
    }

    public static boolean isExistKey(String key){
        for (CacheKeyEnums item : CacheKeyEnums.values()) {
            if (item.getKeyName().equals(key)){
               return true;
            }
        }
        return false;
    }
}

