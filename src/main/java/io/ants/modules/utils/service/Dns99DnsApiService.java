package io.ants.modules.utils.service;


import io.ants.common.utils.R;
import io.ants.modules.sys.enums.DnsApiEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Dns99DnsApiService {

    private final static String dns99ApiUrl= DnsApiEnum.DNS99DNS.getWebSite();

    public static String getSign(Map map){
         return AntsDnsApiService.getSign(map);
    }

    public static R getLine(String domain, String appId, String appKey, int parentId){
        return AntsDnsApiService.getLine(dns99ApiUrl,domain,appId,appKey,parentId);
    }


    public static R getLineV2(String domain, String appId, String appKey,int parentId){
         return AntsDnsApiService.getLineV2(dns99ApiUrl,domain,appId,appKey,parentId);
    }


    public static R getRecordList(String domain,String appId,String appKey){
       return AntsDnsApiService.getRecordList(dns99ApiUrl,domain,appId,appKey);
    }

    public static R removeRecord(String domain,String appId,String appKey,String recordId){
        return AntsDnsApiService.removeRecord(dns99ApiUrl,domain,appId,appKey,recordId);
    }

    public static R modifyRecord(String domain, String appId, String appKey, String recordId, String top, String recordType, String line, String value, String ttl){

      return AntsDnsApiService.modifyRecord(dns99ApiUrl,domain,appId,appKey,recordId,top,recordType,line,value,ttl);
    }

    public static R addRecord(String domain,String appId,String appKey,String top,String recordType,String line,String value,String ttl){
       return AntsDnsApiService.addRecord(dns99ApiUrl,domain,appId,appKey,top,recordType,line,value,ttl);
    }

    public static R GetRecordByInfo(String domain,String appId,String appKey,String top,String type,String line){
        return AntsDnsApiService.GetRecordByInfo(dns99ApiUrl, domain,appId,appKey,top,type,line);
    }
}
