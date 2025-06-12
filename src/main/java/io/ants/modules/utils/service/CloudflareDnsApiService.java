package io.ants.modules.utils.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.R;
import io.ants.modules.sys.vo.CfDnsRecordVo;
import io.ants.modules.sys.vo.DnsLineVo;
import org.apache.commons.collections.ArrayStack;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.*;

public class CloudflareDnsApiService {
    //DOC
    //https://developers.cloudflare.com/api/operations/dns-records-for-a-zone-list-dns-records

    private static R cfRequest(String method, String uri, String apiToken, String queryData){
        String eMsg="";
        String ret="";
        HttpsURLConnection httpsConn=null;
        try{
            URL url = new URL(uri);
            URLConnection conn = url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                 httpsConn = (HttpsURLConnection) conn;
                // 设置 SSL 上下文（可选）
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, new SecureRandom());
                httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
            }
            if (null==httpsConn){
                return  R.error("not a https :"+uri);
            }
            if (StringUtils.isBlank(method)){
                httpsConn.setRequestMethod("GET");
            }else {
                httpsConn.setRequestMethod(method.toUpperCase(Locale.ROOT));
            }
            httpsConn.setRequestProperty("Content-Type", "application/json");
            //byte[] message = ("elastic:8881130py").getBytes("UTF-8");
            //byte[] message = ("elastic:"+password).getBytes("UTF-8");
            //String basicAuth = DatatypeConverter.printBase64Binary(message);
            httpsConn.setRequestProperty("Authorization", "Bearer " + apiToken);
            httpsConn.setDoOutput(true);

            if (StringUtils.isNotBlank(queryData)){
                try (OutputStreamWriter writer = new OutputStreamWriter(httpsConn.getOutputStream())) {
                    writer.write(queryData);
                    writer.flush();
                }
            }

            InputStream responseStream = httpsConn.getResponseCode() / 100 == 2
                    ? httpsConn.getInputStream()
                    : httpsConn.getErrorStream();

            try (Scanner s = new Scanner(responseStream).useDelimiter("\\A")) {
                String response = s.hasNext() ? s.next() : "";
                ret = response;
            }
            if (StringUtils.isNotBlank(ret)){
                JSONObject retObj= DataTypeConversionUtil.string2Json(ret);
                if (retObj.containsKey("success") && true== retObj.getBoolean("success")){
                    return R.ok().put("data",ret);
                }
            }
            return R.error(ret);
        }catch (Exception e){
            eMsg= e.getMessage();
            e.printStackTrace();
        }finally {
            if (httpsConn != null) {
                httpsConn.disconnect();
            }
        }
        return R.error(eMsg);

    }

    public static R getLine(String domainName, String appId, String appKey) {
            String zoneId=appKey;
            if (StringUtils.isBlank(appKey)){
                zoneId=getZoneId(domainName,appId);
            }
          String url=  String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records",zoneId);
          R r=cfRequest("GET",url,appId,"");
          if (1==r.getCode()){
              Map map=new HashMap(2);
              map.put("default","default");
              return R.ok().put("data",map);
          }
          return r;
    }

    public static R getZones(String domainName, String appId){
        String url="https://api.cloudflare.com/client/v4/zones";
        if (StringUtils.isNotBlank(domainName)){
             url="https://api.cloudflare.com/client/v4/zones?name="+domainName;
        }
        return cfRequest("GET",url,appId,"");
    }

    public static String getZoneId(String domainName, String token){
        R r1=getZones(domainName,token);
        if (1!=r1.getCode()){
            return "";
        }
        if (!r1.containsKey("data")){
            return "";
        }
        String ret=r1.get("data").toString();
        JSONObject regJson= DataTypeConversionUtil.string2Json(ret);
        if (!regJson.containsKey("success") || !regJson.containsKey("result")){
            return "";
        }
        if (false==regJson.getBoolean("success")){
            return "";
        }
        JSONArray array=regJson.getJSONArray("result");
        if (0==array.size()){
            return "";
        }
        for (int i = 0; i <array.size() ; i++) {
            JSONObject obj=array.getJSONObject(i);
//            System.out.println(obj.containsKey("name"));
//            System.out.println(obj.get("name").toString());
//            System.out.println(domainName);
//            System.out.println(obj.get("name").toString().equals(domainName.trim()));
            if (obj.containsKey("name") && obj.get("name").toString().equals(domainName.trim())){
                return obj.getString("id");
            }
        }
        return "";
    }

    public static R getLineV2( String domainName,String appId, String appKey) {
        List<DnsLineVo> dLines=new ArrayStack();
        String zoneId=appKey;
        if (StringUtils.isBlank(appKey)){
            zoneId=getZoneId(domainName,appId);
        }
        String url=  String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records",zoneId);
        R r=cfRequest("GET",url,appId,"");
        if (1==r.getCode()){
            DnsLineVo vo=new DnsLineVo();
            vo.setName("default");
            vo.setId("-1");
            dLines.add(vo);
            return R.ok().put("data",dLines);
        }
        return r;
    }

    public static R getRecordList(String domainName, String appId, String appKey) {
        String zoneId=appKey;
        if (StringUtils.isBlank(appKey)){
            zoneId=getZoneId(domainName,appId);
        }
        String url=  String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records?per_page=50000",zoneId);
        R r=cfRequest("GET",url,appId,"");
        if (1==r.getCode() && r.containsKey("data")){
            String ret=r.get("data").toString();
            JSONObject regJson= DataTypeConversionUtil.string2Json(ret);
            if (regJson.containsKey("success") && regJson.containsKey("result")){
                if (false==regJson.getBoolean("success")){
                    return R.error(ret);
                }
                JSONArray array=regJson.getJSONArray("result");
                return  R.ok().put("data",array);
            }
        }
        return r;
    }

    public static R removeRecord(String domainName, String appId, String appKey, String recordId) {
        String zoneId=appKey;
        if (StringUtils.isBlank(appKey)){
            zoneId=getZoneId(domainName,appId);
        }
        String url=String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s",zoneId,recordId);
        R r=cfRequest("DELETE",url,appId,"");
        return r;
    }

    public static R addRecord(String appDomain, String appId, String appKey, String top, String recordType,  String value, String ttl) {
        String zoneId=appKey;
        if (StringUtils.isBlank(appKey)){
            zoneId=getZoneId(appDomain,appId);
        }
        String url=String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records",zoneId);
       CfDnsRecordVo vo=new CfDnsRecordVo();
       vo.setType(recordType);
       vo.setContent(value);
       vo.setName(top+"."+appDomain);
       vo.setTtl(Long.parseLong(ttl));
       return cfRequest("POST",url,appId,DataTypeConversionUtil.entity2jonsStr(vo));
    }

    public static R modifyRecord(String appDomain, String appId, String appKey, String recordId, String top, String recordType, String value, String ttl) {
        String zoneId=appKey;
        if (StringUtils.isBlank(appKey)){
            zoneId=getZoneId(appDomain,appId);
        }
        String url=String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s",zoneId,recordId);
        //System.out.println(url);
        CfDnsRecordVo vo=new CfDnsRecordVo();
        vo.setType(recordType);
        vo.setContent(value);
        vo.setName(top+"."+appDomain);
        vo.setTtl(Long.parseLong(ttl));
        //System.out.println(DataTypeConversionUtil.entity2jonsStr(vo));
        return cfRequest("PUT",url,appId,DataTypeConversionUtil.entity2jonsStr(vo));
    }

    public static R getRecordByInfo(String appDomain, String appId, String appKey, String top, String type) {
        R r=getRecordList(appDomain,appId,appKey);
        if (1==r.getCode()){
            JSONArray array=(JSONArray)r.get("data");
            List<JSONObject> fArray=new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                String name=top+"."+appDomain;
                CfDnsRecordVo vo =DataTypeConversionUtil.json2entity(array.getJSONObject(i),CfDnsRecordVo.class);
                if (null!=vo){
                    if (!vo.getName().equals(name)){
                       continue;
                    }
                    if (!vo.getType().equals(type)){
                        continue;
                    }
                    fArray.add(array.getJSONObject(i));
                }
            }
            return R.ok().put("data",fArray);
        }
        return r;
    }

    public static R getDomainInZone(String appDomain, String token, String zoneId){
        //https://api.cloudflare.com/client/v4/accounts/{account_id}/dns_settings/views
        //https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records
        //https://api.cloudflare.com/client/v4/zones/{zone_id}/settings
        //System.out.println(url);
        R r=getZones(appDomain,token);
        if (1!=r.getCode()){
            return r;
        }
        try{
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(r.get("data").toString());
            if (!jsonObject.containsKey("result")){
                return R.error("result is null");
            }
            JSONArray array=jsonObject.getJSONArray("result");
            if (null==array){
                return R.error("result is null");
            }
            for (int i = 0; i < array.size(); i++) {
                JSONObject resJson=array.getJSONObject(i);
                if (resJson.containsKey("name")&& resJson.getString("name").equals(appDomain)){
                    return R.ok().put("data",resJson);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return R.error(r.toString());
    }

    public static R getDnsSettings(String token,String zoneId){
        String url=String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_settings",zoneId);
        System.out.println(url);
        R r=cfRequest("GET",url,token,"");
        System.out.println(r.get("data").toString());
        return r;
    }

    public static void main(String[] args) {
        String domain="my1314.asia";
        //appid=token
        String token="64wtV61BiJ2iMvrgMlQzPoKqed2r2EGo0jJky3dD";
        //appkey==zoneId
        String zoneId="1e902100415a7442a8cd5e93cd9f49b4";
        // getDomainInZone(domain,token,zoneId);
        R r2=  getZones(domain,token);
        System.out.println(r2.toJsonString());
        //R r=getRecordList(token,zoneId);
        //System.out.println(r);
        //r=getDnsSettingsViews(domain,token,zoneId);
        //R r=getRecordByInfo(domain,token,zoneId,"aaa","A");
        //R r=addRecord(domain,token,zoneId,"aaa","A","2.2.2.20","600");
        //R r=modifyRecord(domain,token,zoneId,rid,"aa","TXT","AABBCC","601");
        //R r=removeRecord(token,zoneId,"1");
        //System.out.println(r);
        //curl --request PUT   --url https://api.cloudflare.com/client/v4/zones/zone_identifier/dns_records/identifier \
        //  --header 'Content-Type: application/json' \
        //  --header 'X-Auth-Email: ' \
        //  --data '{
        //  "content": "198.51.100.4",
        //  "name": "example.com",
        //  "proxied": false,
        //  "type": "A",
        //  "comment": "Domain verification record",
        //  "tags": [
        //    "owner:dns-team"
        //  ],
        //  "ttl": 3600
        //}'
    }
}
