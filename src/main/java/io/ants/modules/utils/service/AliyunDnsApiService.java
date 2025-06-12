package io.ants.modules.utils.service;


import com.aliyun.alidns20150109.models.*;
import com.aliyun.teaopenapi.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import io.ants.common.utils.R;
import io.ants.modules.sys.vo.DnsLineVo;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class AliyunDnsApiService {

    /*
    api
    * https://next.api.aliyun.com/api/Alidns/2015-01-09/AddDomainRecord?lang=JAVA&params={%22Line%22:%22default%22,%22Priority%22:1,%22TTL%22:600,%22Value%22:%221.2.3.3%22,%22Type%22:%22a%22,%22RR%22:%22testapi%22,%22DomainName%22:%2291hu.top%22}&tab=DEMO
    * */
   private final static long PAGESITE=100l;


    /**
     * 使用AK&SK初始化账号Client
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.alidns20150109.Client createClient(String accessKeyId, String accessKeySecret) {
        try{
            Config config = new Config()
                    // 您的AccessKey ID
                    .setAccessKeyId(accessKeyId)
                    // 您的AccessKey Secret
                    .setAccessKeySecret(accessKeySecret);
            // 访问的域名
            config.endpoint = "alidns.cn-shanghai.aliyuncs.com";
            return new com.aliyun.alidns20150109.Client(config);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static R getLine(String appId, String appKey){
        LinkedHashMap map=new LinkedHashMap();
        try{
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            if(null!=client){
                DescribeSupportLinesRequest describeSupportLinesRequest = new DescribeSupportLinesRequest();
                DescribeSupportLinesResponse res= client.describeSupportLines(describeSupportLinesRequest);
                List<DescribeSupportLinesResponseBody.DescribeSupportLinesResponseBodyRecordLinesRecordLine> list=res.getBody().getRecordLines().getRecordLine();
                for (DescribeSupportLinesResponseBody.DescribeSupportLinesResponseBodyRecordLinesRecordLine item:list){
                    map.put(item.getLineCode(),item.getLineName());
                }
                return  R.ok().put("data",map);
            }
        }catch (Exception e){
            map.put("err",e.getMessage());
            e.printStackTrace();
        }
        return R.error().put("data",map);
    }

    public static R getLineV2(String appId, String appKey){
        List<DnsLineVo> dLines=new ArrayStack();
        try{
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            if(null!=client){
                DescribeSupportLinesRequest describeSupportLinesRequest = new DescribeSupportLinesRequest();
                DescribeSupportLinesResponse res= client.describeSupportLines(describeSupportLinesRequest);
                List<DescribeSupportLinesResponseBody.DescribeSupportLinesResponseBodyRecordLinesRecordLine> list=res.getBody().getRecordLines().getRecordLine();
                for (DescribeSupportLinesResponseBody.DescribeSupportLinesResponseBodyRecordLinesRecordLine item:list){
                    DnsLineVo vo=new DnsLineVo();
                    vo.setName(item.getLineCode());
                    vo.setId(item.getLineCode());
                    dLines.add(vo);
                }
                return  R.ok().put("data",dLines);
            }
        }catch (Exception e){
            DnsLineVo vo=new DnsLineVo();
            vo.setName(e.getMessage());
            vo.setId("-1");
            dLines.add(vo);
            e.printStackTrace();
        }
        return R.error().put("data",dLines);
    }

    public static R addRecord(String domain,String appId,String appKey,String top, String recordType, String line, String value, String ttl){
        String eMsg="";
        try {
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            AddDomainRecordRequest addDomainRecordRequest = new AddDomainRecordRequest()
                    .setPriority(1L)
                    .setTTL(Long.parseLong(ttl))
                    .setValue(value)
                    .setType(recordType.toUpperCase())
                    .setRR(top)
                    .setDomainName(domain);
            if(StringUtils.isNotBlank(line)){
                addDomainRecordRequest.setLine(line);
            }else{
                addDomainRecordRequest.setLine("default");
            }

            // 复制代码运行请自行打印 API 的返回值
           AddDomainRecordResponse res= client.addDomainRecord(addDomainRecordRequest);
           //return res.getBody().getRecordId()
           return R.ok().put("data",res.getBody().getRecordId());
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R GetRecordByInfo(String domain,String appId,String appKey,String top, String recordType, String line){
        String eMsg="";
        try {
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest()
                    .setDomainName(domain)
                    .setRRKeyWord(top)
                    .setType(recordType)
                    .setPageSize(PAGESITE)
                    .setPageNumber(1l);
            if(StringUtils.isNotBlank(line)){
                describeDomainRecordsRequest.setLine(line);
            }
            RuntimeOptions runtime = new RuntimeOptions();
            // 复制代码运行请自行打印 API 的返回值
            DescribeDomainRecordsResponse res= client.describeDomainRecordsWithOptions(describeDomainRecordsRequest, runtime);
            //;
            return R.ok().put("data",res.getBody().getDomainRecords().getRecord());
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    private static  List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord>  getRecordList(String domain,String appId,String appKey,int pageNum){
        try {
            //https://next.api.aliyun.com/api/Alidns/2015-01-09/AddDomainRecord?lang=JAVA&params={%22Line%22:%22default%22,%22Priority%22:1,%22TTL%22:600,%22Value%22:%221.2.3.3%22,%22Type%22:%22a%22,%22RR%22:%22testapi%22,%22DomainName%22:%2291hu.top%22}&tab=DEMO
            //分页查询时设置的每页行数，最大值500，默认为20。
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest()
                    .setDomainName(domain)
                    .setPageNumber((long)pageNum)
                    .setPageSize(PAGESITE);
            // 复制代码运行请自行打印 API 的返回值
            DescribeDomainRecordsResponse res=  client.describeDomainRecords(describeDomainRecordsRequest);
            return res.getBody().getDomainRecords().getRecord();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static R getRecordList(String domain,String appId,String appKey){
        String eMsg="";
        try {
            //https://next.api.aliyun.com/api/Alidns/2015-01-09/AddDomainRecord?lang=JAVA&params={%22Line%22:%22default%22,%22Priority%22:1,%22TTL%22:600,%22Value%22:%221.2.3.3%22,%22Type%22:%22a%22,%22RR%22:%22testapi%22,%22DomainName%22:%2291hu.top%22}&tab=DEMO
            //分页查询时设置的每页行数，最大值500，默认为20。
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest()
                    .setDomainName(domain)
                    .setPageNumber(1L)
                    .setPageSize(PAGESITE);
            // 复制代码运行请自行打印 API 的返回值
           DescribeDomainRecordsResponse res=  client.describeDomainRecords(describeDomainRecordsRequest);
           if (res.getBody().getTotalCount()<=PAGESITE){
               //  List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord>
               return R.ok().put("data",res.getBody().getDomainRecords().getRecord());
           }else{
               List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> allList=new ArrayList<>();
               allList.addAll(res.getBody().getDomainRecords().getRecord());
               int totalPageSum=(int) Math.ceil(res.getBody().getTotalCount()/PAGESITE);
               for (int i = 2; i <=totalPageSum ; i++) {
                   List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> l=getRecordList(domain,appId,appKey,i);
                   if (null!=l){
                       allList.addAll(l);
                   }
               }
               return R.ok().put("data",allList);
           }

        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public static R getDescribeDomainInfo(String domain,String appId,String appKey){
        String eMsg="";
        try{
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            DescribeDomainInfoRequest describeDomainInfoRequest = new DescribeDomainInfoRequest()
                    .setDomainName(domain);

            DescribeDomainInfoResponse res = client.describeDomainInfo(describeDomainInfoRequest);
            return R.ok().put("data",res.getBody()) ;
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static  R removeRecord(String appId,String appKey,String recordId){
        String eMsg="";
        try{
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            DeleteDomainRecordRequest deleteDomainRecordRequest = new DeleteDomainRecordRequest()
                    .setRecordId(recordId);
            // 复制代码运行请自行打印 API 的返回值
           DeleteDomainRecordResponse res=  client.deleteDomainRecord(deleteDomainRecordRequest);
           return R.ok().put("data",res.getBody().getRecordId()) ;
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }


    public static Object removeRecordByInfo(String domain,String appId,String appKey,String top,String type){
        try{
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            DeleteSubDomainRecordsRequest deleteSubDomainRecordsRequest = new DeleteSubDomainRecordsRequest()
                    .setDomainName(domain)
                    .setRR(top)
                    .setType(type);
            RuntimeOptions runtime = new RuntimeOptions();
            // 复制代码运行请自行打印 API 的返回值
            DeleteSubDomainRecordsResponse res= client.deleteSubDomainRecordsWithOptions(deleteSubDomainRecordsRequest, runtime);
            return res.getBody().getRequestId();
        }catch (Exception e){

        }
        return null;
    }

    public static R modifyRecord(String domain, String appId, String appKey, String recordId, String top, String recordType, String line, String value, String ttl){
        String eMsg="";
        try{
            com.aliyun.alidns20150109.Client client = AliyunDnsApiService.createClient(appId, appKey);
            UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest()
                    .setRecordId(recordId)
                    .setRR(top)
                    .setType(recordType)
                    .setValue(value)
                    .setTTL(Long.parseLong(ttl))
                    .setLine(line);
            RuntimeOptions runtime = new RuntimeOptions();
            // 复制代码运行请自行打印 API 的返回值
            UpdateDomainRecordResponse res=  client.updateDomainRecordWithOptions(updateDomainRecordRequest, runtime);
            return R.ok().put("data",res.getBody().recordId).put("res",res);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public static void main(String[] args) {
       R r= getDescribeDomainInfo("91hus.top"," "," ");
        System.out.println(r.toJsonString());
    }
}
