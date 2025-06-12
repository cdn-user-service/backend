package io.ants.modules.utils.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.HttpRequest;
import io.ants.common.utils.R;
import io.ants.modules.sys.vo.DnsLineVo;
import io.ants.modules.sys.vo.GodaddyRecordVo;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class GodaddyDnsApiService {


    public static R getDomainLine(String domain, String appId, String appKey){
        String eMsg="";
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
             * {"authCode":"#q:GA1%!iE4b1AAi","contactAdmin":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactBilling":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactRegistrant":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactTech":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"createdAt":"2022-04-13T09:46:35.000Z","domain":"dns.live","domainId":368642781,"expirationProtected":false,"expires":"2023-04-13T09:46:35.000Z","exposeWhois":false,"holdRegistrar":false,"locked":true,"nameServers":["ns13.domaincontrol.com","ns14.domaincontrol.com"],"privacy":true,"registrarCreatedAt":"2022-04-13T02:46:27.690Z","renewAuto":false,"renewDeadline":"2023-05-28T02:46:31.000Z","renewable":true,"redeemable":false,"status":"ACTIVE","transferAwayEligibleAt":"2022-06-12T09:46:35.000Z","transferProtected":false}
             */
            if (StringUtils.isBlank(res)){
                res="获取失败";
                return R.error(eMsg);
            }
            JSONObject jsonObject= DataTypeConversionUtil.string2Json(res);
            if (null!=jsonObject && jsonObject.containsKey("status")){
                if("ACTIVE".equals(jsonObject.getString("status"))){
                    Map map=new HashMap(2);
                    map.put("default","default");
                    return R.ok().put("data",map);
                }
            }
            //System.out.println(res);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public static R getDomainLineV2(String domain, String appId, String appKey){
        List<DnsLineVo> dLines=new ArrayStack();
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
             * {"authCode":"#q:GA1%!iE4b1AAi","contactAdmin":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactBilling":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactRegistrant":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactTech":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"createdAt":"2022-04-13T09:46:35.000Z","domain":"dns.live","domainId":368642781,"expirationProtected":false,"expires":"2023-04-13T09:46:35.000Z","exposeWhois":false,"holdRegistrar":false,"locked":true,"nameServers":["ns13.domaincontrol.com","ns14.domaincontrol.com"],"privacy":true,"registrarCreatedAt":"2022-04-13T02:46:27.690Z","renewAuto":false,"renewDeadline":"2023-05-28T02:46:31.000Z","renewable":true,"redeemable":false,"status":"ACTIVE","transferAwayEligibleAt":"2022-06-12T09:46:35.000Z","transferProtected":false}
             */
            if (StringUtils.isBlank(res)){
                res="获取失败";
                return R.error(res);
            }
            JSONObject jsonObject= DataTypeConversionUtil.string2Json(res);
            if (null!=jsonObject && jsonObject.containsKey("status")){
                if("ACTIVE".equals(jsonObject.getString("status"))){
                    DnsLineVo vo=new DnsLineVo();
                    vo.setName("default");
                    vo.setId("0");
                    dLines.add(vo);
                    return R.ok().put("data",dLines);
                }
            }
            //System.out.println(res);
        }catch (Exception e){
            DnsLineVo vo=new DnsLineVo();
            vo.setName(e.getMessage());
            vo.setId("-1");
            dLines.add(vo);
            e.printStackTrace();
        }

        return R.ok().put("data",dLines);
    }


    public static String getDomainInfo(String domain,String appId,String appKey){
        /*
           curl -X 'GET' \
          'https://api.ote-godaddy.com/v1/domains/available?domain=dns.live&checkType=FAST&forTransfer=false' \
          -H 'accept: application/json' \
          -H 'Authorization: sso-key UzQxLikm_46KxDFnbjN7cQjmw6wocia:46L26ydpkwMaKZV6uVdDWe'
        * */
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
            * {"authCode":"#q:GA1%!iE4b1AAi","contactAdmin":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactBilling":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactRegistrant":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"contactTech":{"addressMailing":{"address1":"hubei shiyan","address2":"","city":"shiyan","country":"CN","postalCode":"442100","state":"Hubei"},"email":"331434376@qq.com","fax":"","nameFirst":"zhang","nameLast":"hu","organization":"","phone":"+86.15629529961"},"createdAt":"2022-04-13T09:46:35.000Z","domain":"dns.live","domainId":368642781,"expirationProtected":false,"expires":"2023-04-13T09:46:35.000Z","exposeWhois":false,"holdRegistrar":false,"locked":true,"nameServers":["ns13.domaincontrol.com","ns14.domaincontrol.com"],"privacy":true,"registrarCreatedAt":"2022-04-13T02:46:27.690Z","renewAuto":false,"renewDeadline":"2023-05-28T02:46:31.000Z","renewable":true,"redeemable":false,"status":"ACTIVE","transferAwayEligibleAt":"2022-06-12T09:46:35.000Z","transferProtected":false}
             */
            if (StringUtils.isBlank(res)){
                return "";
            }
            JSONObject jsonObject= DataTypeConversionUtil.string2Json(res);
            if (null!=jsonObject &&  jsonObject.containsKey("status")){
                if("ACTIVE".equals(jsonObject.getString("status"))){
                    return res;
                }
            }
            //System.out.println(res);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static String godaddyHttpRequest(String requestUrl,String appId,String appKey,String method){
        //URL url = new URL("https://api.ote-godaddy.com/v1/domains/aaaxxx.xyz/records/A/*");
        try{
            URL url = new URL(requestUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod(method.toUpperCase());

            httpConn.setRequestProperty("accept", "application/json");
            httpConn.setRequestProperty("Authorization", String.format("sso-key %s:%s",appId,appKey));

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";
            //System.out.println(response);
            return response;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static R getRecord(String domain, String appId, String appKey, String name){
        String eMsg="";
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain+"/records/A/"+name;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
             * [{"data":"Parked","name":"@","ttl":600,"type":"A"}]
             *
             */
            //JSONObject jsonObject= DataTypeConversionUtil.String2Json(res);
            //res=res.replaceAll("\\r\\n$","");
            if (StringUtils.isNotBlank(res)){
                return R.ok().put("data",DataTypeConversionUtil.string2JsonArray(res)) ;
            }else {
                return R.error("获取列表失败！");
            }
            //System.out.println(res);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static JSONArray getCNAMERecord(String domain,String appId,String appKey,String name){
        /*
        * curl -X 'GET' \
          'https://api.ote-godaddy.com/v1/domains/dns.live/records/A/*' \
          -H 'accept: application/json' \
          -H 'Authorization: sso-key UzQxLikm_46KxDFnbjN7cQjmw6wocia:46L26ydpkwMaKZV6uVdDWe'
         * */
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain+"/records/CNAME/"+name;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
             * [{"data":"Parked","name":"@","ttl":600,"type":"A"}]
             *
             */
            //JSONObject jsonObject= DataTypeConversionUtil.String2Json(res);
            //res=res.replaceAll("\\r\\n$","");
            if (StringUtils.isBlank(res)){
                return null;
            }
            return DataTypeConversionUtil.string2JsonArray(res);
            //System.out.println(res);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray getNsRecord(String domain,String appId,String appKey ){
        /*
        * curl -X 'GET' \
          'https://api.ote-godaddy.com/v1/domains/dns.live/records/A/*' \
          -H 'accept: application/json' \
          -H 'Authorization: sso-key UzQxLikm_46KxDFnbjN7cQjmw6wocia:46L26ydpkwMaKZV6uVdDWe'
         * */
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain+"/records/NS/@";
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
             * [{"data":"Parked","name":"@","ttl":600,"type":"A"}]
             *
             */
            //JSONObject jsonObject= DataTypeConversionUtil.String2Json(res);
            //res=res.replaceAll("\\r\\n$","");
            if (StringUtils.isBlank(res)){
                return null;
            }
            return DataTypeConversionUtil.string2JsonArray(res);
            //System.out.println(res);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static R addRecord(String domain,String appId,String appKey,String top,String recordType,String value,String ttl){
        String eMsg="";
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain+"/records";
            String auth="Authorization: sso-key "+appId+":"+appKey;
            String postData="[{\"data\":\""+value+"\",\"name\":\""+top+"\",\"port\":65535,\"priority\":0,\"ttl\":"+ttl+",\"type\":\""+recordType+"\",\"weight\":1}]";
            String[] cmds={"curl","-X","PATCH",url,"-H","accept: application/json","-H","Content-Type: application/json","-H",auth,"-d",postData};
            String res= HttpRequest.execCurl(cmds);
            if (StringUtils.isBlank(res)){
                return R.error("add fail");
            }
            return R.ok().put("data",res);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R RecordList(String domain,String appId,String appKey){
        JSONArray resultArray=new JSONArray();
        R r= getRecord(domain,appId,appKey,"");
        if (null==r||1!=r.getCode()){
            return r;
        }

        if (!r.get("data").getClass().getName().equals("JSONArray")){
            //System.out.println(r.get("data").getClass().getName());
            //System.out.println(r.toString());
            return r;
        }
        resultArray.addAll((JSONArray)r.get("data"));
        r = getRecord(domain,appId,appKey,"@");
        if (null==r||1!=r.getCode()){
            return r;
        }
        resultArray.addAll((JSONArray)r.get("data"));
        JSONArray cname=getCNAMERecord(domain,appId,appKey,"");
        if (null!=cname){
            resultArray.addAll(cname);
        }
        JSONArray ns=getNsRecord(domain,appId,appKey);
        if (null!=ns){
            resultArray.addAll(ns);
        }
        return R.ok().put("data",resultArray);
    }

    public static R GetRecordByInfo(String domain,String appId,String appKey,String top,String type){
        String eMsg="";
        try{
            String url="https://api.godaddy.com/v1/domains/"+domain+"/records/"+type+"/"+top;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","GET",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"GET");
            /*
             * [{"data":"Parked","name":"@","ttl":600,"type":"A"}]
             *
             */
            //JSONObject jsonObject= DataTypeConversionUtil.String2Json(res);
            //res=res.replaceAll("\\r\\n$","");
            if (StringUtils.isBlank(res)){
                return R.error("get fail");
            }
            JSONArray jsonArray=DataTypeConversionUtil.string2JsonArray(res);
            return R.ok().put("data",jsonArray);
            //System.out.println(res);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R removeRecordByRecordId(String domain, String appId, String appKey, String recordId){
        String eMsg="";
        try{
            R r=GodaddyDnsApiService.RecordList(domain,appId,appKey);
            if (1==r.getCode()){
                JSONArray array=(JSONArray)r.get("data");
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object=array.getJSONObject(i);
                    GodaddyRecordVo item=DataTypeConversionUtil.json2entity(object,GodaddyRecordVo.class);
                    if (recordId.equals(item.getCalRecordId())){
                        GodaddyDnsApiService.removeRecordByInfo(domain,appId,appKey,item.getType(),item.getName());
                    }
                }
                return R.ok();
            }
            return r;
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }

    public static void  removeRecordByInfo(String domain,String appId,String appKey,String type,String top){
         /*
        * curl -X 'DELETE' \
          'https://api.ote-godaddy.com/v1/domains/dns.live/records/A/test' \
          -H 'accept: application/json' \
          -H 'Authorization: sso-key UzQxLikm_46KxDFnbjN7cQjmw6wocia:46L26ydpkwMaKZV6uVdDWe'*/
        try{

            String url="https://api.godaddy.com/v1/domains/"+domain+"/records/"+type+"/"+top;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","DELETE",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"DELETE");
            //System.out.println(res);
            if (StringUtils.isBlank(res)){
                System.out.println("removeRecordByInfo fail");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public static String removeAllTypeRecord(String domain, String appId, String appKey, String top){
        /*
        * curl -X 'DELETE' \
          'https://api.ote-godaddy.com/v1/domains/dns.live/records/A/test' \
          -H 'accept: application/json' \
          -H 'Authorization: sso-key UzQxLikm_46KxDFnbjN7cQjmw6wocia:46L26ydpkwMaKZV6uVdDWe'*/
        String[] delete_types={"A","CNAME"};

        try{
            String ret="";
            for (String RecordType:delete_types){
                String url="https://api.godaddy.com/v1/domains/"+domain+"/records/"+RecordType+"/"+top;
                //String auth="Authorization: sso-key "+appId+":"+appKey;
                //String[] cmds={"curl","-X","DELETE",url,"-H","accept: application/json","-H",auth};
                //String res= HttpRequest.execCurl(cmds);
                String res=godaddyHttpRequest(url,appId,appKey,"DELETE");
                //System.out.println(res);
                ret+=res;
            }
            return ret;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static String removeRecord(String domain, String appId, String appKey, String type, String top){
        /*
        * curl -X 'DELETE' \
          'https://api.ote-godaddy.com/v1/domains/dns.live/records/A/test' \
          -H 'accept: application/json' \
          -H 'Authorization: sso-key UzQxLikm_46KxDFnbjN7cQjmw6wocia:46L26ydpkwMaKZV6uVdDWe'*/

        try{
            String url="https://api.godaddy.com/v1/domains/"+domain+"/records/"+type+"/"+top;
            //String auth="Authorization: sso-key "+appId+":"+appKey;
            //String[] cmds={"curl","-X","DELETE",url,"-H","accept: application/json","-H",auth};
            //String res= HttpRequest.execCurl(cmds);
            String res=godaddyHttpRequest(url,appId,appKey,"DELETE");
            //System.out.println(res);
            return res;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static void test(){
        //95471361
        final String API_KEY = "fYqYj457aAPn_C946EET8zY7sgpn123vGR9";
        final String API_SECRET = "8U8JxdagpxrUs5m4kJX5qH";
        final String DOMAIN = "dns.xyz";
        final String RECORD_NAME = "1112";
        final String RECORD_TYPE = "A";
        try {
            // 构造API请求URL
            String url = "https://api.godaddy.com/v1/domains/" + DOMAIN + "/records/" + RECORD_TYPE + "/" + RECORD_NAME;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方法为GET
            con.setRequestMethod("GET");
            // 设置请求头部信息
            con.setRequestProperty("Authorization", "sso-key " + API_KEY + ":" + API_SECRET);
            con.setRequestProperty("Content-Type", "application/json");
            // 发送API请求
            int responseCode = con.getResponseCode();
            // 处理API响应
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response);
            // 解析API响应结果，获取RECORD_ID
            //String recordId = response.toString().split("\"id\":\"")[1].split("\"")[0];

            // 输出RECORD_ID
            //System.out.println("Record ID: " + recordId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final String API_KEY = "fYqYj457aAPn_C946EET8zY7sgpn123vGR9";
        final String API_SECRET = "8U8JxdagpxrUs5m4kJX5qH";
        final String DOMAIN = "dns.xyz";
        //removeRecordByRecordId(DOMAIN,API_KEY,API_SECRET,"95471361");
        R r=    RecordList(DOMAIN,API_KEY,API_SECRET);
        System.out.println(r);
    }

}
