package io.ants.modules.utils.service;


import io.ants.common.utils.R;
import io.ants.modules.sys.enums.DnsApiEnum;

import java.util.Map;

public class DnsXdpDnsApiService {

    private final static String dnsXdpDnsApiUrl= DnsApiEnum.DNSXDP.getWebSite();

    public static String getSign(Map map){
         return AntsDnsApiService.getSign(map);
    }

    public static R getLine(String domain, String appId, String appKey, int parentId){
        return AntsDnsApiService.getLine(dnsXdpDnsApiUrl,domain,appId,appKey,parentId);
    }


    public static R getLineV2(String domain, String appId, String appKey,int parentId){
         return AntsDnsApiService.getLineV2(dnsXdpDnsApiUrl,domain,appId,appKey,parentId);
    }


    public static R getRecordList(String domain,String appId,String appKey){
       return AntsDnsApiService.getRecordList(dnsXdpDnsApiUrl,domain,appId,appKey);
    }

    public static R removeRecord(String domain,String appId,String appKey,String recordId){
        return AntsDnsApiService.removeRecord(dnsXdpDnsApiUrl,domain,appId,appKey,recordId);
    }

    public static R modifyRecord(String domain, String appId, String appKey, String recordId, String top, String recordType, String line, String value, String ttl){

      return AntsDnsApiService.modifyRecord(dnsXdpDnsApiUrl,domain,appId,appKey,recordId,top,recordType,line,value,ttl);
    }

    public static R addRecord(String domain,String appId,String appKey,String top,String recordType,String line,String value,String ttl){
       return AntsDnsApiService.addRecord(dnsXdpDnsApiUrl,domain,appId,appKey,top,recordType,line,value,ttl);
    }

    public static R GetRecordByInfo(String domain,String appId,String appKey,String top,String type,String line){
        return AntsDnsApiService.GetRecordByInfo(dnsXdpDnsApiUrl, domain,appId,appKey,top,type,line);
    }
}
