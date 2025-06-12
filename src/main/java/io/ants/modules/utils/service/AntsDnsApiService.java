package io.ants.modules.utils.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.HashUtils;
import io.ants.common.utils.HttpRequest;
import io.ants.common.utils.R;
import io.ants.modules.sys.vo.DnsLineVo;
import io.ants.modules.sys.vo.DnsRecordInputPollDataVo;
import io.ants.modules.utils.vo.AntsDnsLineVo;
import io.ants.modules.utils.vo.AntsDnsRVo;
import io.ants.modules.utils.vo.AntsDnsRecordListVo;
import lombok.Data;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AntsDnsApiService {


    private final static Logger logger = LoggerFactory.getLogger(AntsDnsApiService.class);

    public static String getSign(Map map){
        String LineParams="";
        TreeMap<String,Object> tree_Map=new TreeMap(map);
        for (Map.Entry<String, Object> entry : tree_Map.entrySet()) {
            String mapKey = entry.getKey();
            if(!"sign".equals(mapKey)){
                Object mapValue =entry.getValue() ;
                if(LineParams.length()==0){
                    LineParams+=mapKey+"="+mapValue;
                }else {
                    LineParams+="&"+mapKey+"="+mapValue;
                }
            }

        }
        String cal_sign= HashUtils.md5ofString(LineParams);
       //logger.debug("sign_param:"+LineParams);
        return cal_sign;
    }


    public static R getLine(String ApiUrl, String domain, String appId, String appKey,int parentId){
        LinkedHashMap map=new LinkedHashMap();
        try{
            String url=ApiUrl+"Line.List";
            Map params=new HashMap(2048);
            params.put("domain",domain);
            params.put("secretId",appId);
            params.put("secretKey",appKey);
            params.put("offset",1);
            params.put("length",100);
            params.put("parentId",parentId);
            params.put("sign",AntsDnsApiService.getSign(params));
            params.remove("secretKey");
            String json_params= DataTypeConversionUtil.map2json(params).toJSONString();
            //String[] cmds={"curl","-X","POST","--header","Content-Type: application/json","--header","Accept: application/json","-d",json_params,url};
            String res= HttpRequest.okHttpPost(url,json_params);
            if(StringUtils.isBlank(res)){
                return R.error("获取dns线路失败！[1]");
            }
            JSONObject jsonRes=DataTypeConversionUtil.string2Json(res);
            if (null==jsonRes || !jsonRes.containsKey("code")){
                return R.error("获取dns线路失败！[2]");
            }
            if(null!=jsonRes && jsonRes.containsKey("code") && 1==jsonRes.getInteger("code")){
                JSONArray jsonArray=jsonRes.getJSONArray("data");
                for (int i = 0; i <jsonArray.size() ; i++) {
                    JSONObject obj=jsonArray.getJSONObject(i);
                    if(obj.containsKey("name")){
                        map.put(obj.getString("name"),obj.getString("name"));
                    }
                }
               return R.ok().put("data",map);
            }
        }catch (Exception e){
            map.put("err",e.getMessage());
            e.printStackTrace();
        }
        return R.error().put("data",map);
    }


    public static R getLineV2(String ApiUrl, String domain, String appId, String appKey,int parentId){
        List<DnsLineVo> dLines=new ArrayStack();
        try{
            String url=ApiUrl+"Line.List";
            Map params=new HashMap(2048);
            params.put("domain",domain);
            params.put("secretId",appId);
            params.put("secretKey",appKey);
            params.put("offset",1);
            params.put("length",1000);
            params.put("parentId",parentId);
            params.put("sign",AntsDnsApiService.getSign(params));
            params.remove("secretKey");
            //logger.info(DataTypeConversionUtil.entity2jonsStr(params));
            String json_params= DataTypeConversionUtil.map2json(params).toJSONString();
            //String[] cmds={"curl","-X","POST","--header","Content-Type: application/json","--header","Accept: application/json","-d",json_params,url};
            String res= HttpRequest.okHttpPost(url,json_params);
            if(StringUtils.isBlank(res)){
                return R.error("获取dns线路失败！[1]");
            }
            JSONObject jsonRes=DataTypeConversionUtil.string2Json(res);
            if (null==jsonRes || !jsonRes.containsKey("code")){
                return R.error("获取dns线路失败！[2]");
            }
            if(null!=jsonRes && jsonRes.containsKey("code") && 1==jsonRes.getInteger("code")){
                JSONArray jsonArray=jsonRes.getJSONArray("data");
                for (int i = 0; i <jsonArray.size() ; i++) {
                    JSONObject obj=jsonArray.getJSONObject(i);
                    logger.info(obj.toJSONString());
                    AntsDnsLineVo adVo=DataTypeConversionUtil.json2entity(obj,AntsDnsLineVo.class);
                    if(null!=adVo){
                        DnsLineVo vo=new DnsLineVo();
                        vo.setName(obj.getString("name"));
                        vo.setId(obj.getString("id"));
                        vo.setChild(adVo.getChild());
                        dLines.add(vo);
                    }
                }
                return R.ok().put("data",dLines);
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


    public static R getRecordList(String ApiUrl,String domain,String appId,String appKey){
        String eMsg="";
        try{
            String url=ApiUrl+"Record.List";
            Map params=new HashMap(1024);
            params.put("domain",domain);
            params.put("secretId",appId);
            params.put("secretKey",appKey);
            params.put("page",1);
            params.put("limit",2000);
            params.put("sign",AntsDnsApiService.getSign(params));
            params.remove("secretKey");
            String jsonParams=DataTypeConversionUtil.map2json(params).toJSONString();
            String res= HttpRequest.okHttpPost(url,jsonParams);
            if(StringUtils.isBlank(res)){
                return R.error("获取dns记录失败！[1]");
            }
            if (StringUtils.isNotBlank(res)){
                //logger.info(res);
                AntsDnsRecordListVo vo=DataTypeConversionUtil.string2Entity(res,AntsDnsRecordListVo.class);
                if (null==vo){
                    logger.error("getRecordList error :"+res);
                    return R.error(res);
                }
                if (1==vo.getCode()){
                    if (null==vo.getData() || vo.getData().isEmpty()){
                        if (null!=vo.getObjData() && !vo.getObjData().isEmpty()){
                            List<String> records=new ArrayList<>();
                            for (int i = 0; i <vo.getObjData().size() ; i++) {
                                records.add(DataTypeConversionUtil.entity2jonsStr(vo.getObjData().get(i)));
                            }
                            return R.ok().put("data",records);
                        }
                    }
                    return R.ok().put("data",vo.getData());

                }
                return R.error(vo.getMsg());

            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R removeRecord(String ApiUrl,String domain,String appId,String appKey,String recordId){
        String eMsg="";
        try{
            String url=ApiUrl+"Record.Remove";
            Map params=new HashMap(1024);
            params.put("domain",domain);
            params.put("record_id",recordId);
            params.put("secretId",appId);
            params.put("secretKey",appKey);
            params.put("sign",AntsDnsApiService.getSign(params));
            params.remove("secretKey");
            String json_params=DataTypeConversionUtil.map2json(params).toJSONString();
            //String[] cmds={"curl","-X","POST","--header","Content-Type: application/json","--header","Accept: application/json","-d",json_params,url};
            String res= HttpRequest.okHttpPost(url,json_params);
            if (StringUtils.isNotBlank(res)){
                AntsDnsRVo r=DataTypeConversionUtil.string2Entity(res,AntsDnsRVo.class);
                if (null==r){
                    return R.error(res);
                }
                return r.thisVo2R();
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R modifyRecord(String ApiUrl, String domain, String appId, String appKey, String recordId, String top, String recordType, String line, String value, String ttl){
        String eMsg="";
        try{
            String url=ApiUrl+"Record.Modify";
            Map params=new HashMap(1024);
            params.put("domain",domain);
            params.put("record_id",recordId);
            params.put("record_type",recordType);
            params.put("top",top);
            params.put("line",line);
            params.put("value",value);
            params.put("ttl",ttl);
            params.put("mx",1);
            params.put("weight",1);
            params.put("secretId",appId);
            params.put("secretKey",appKey);
            params.put("poll_rule",0);
            JSONArray jsonArray=new JSONArray();
            DnsRecordInputPollDataVo vo=new DnsRecordInputPollDataVo();
            vo.setData(value);
            jsonArray.add(vo);
            params.put("poll_data_array",jsonArray);
            params.put("sign",AntsDnsApiService.getSign(params));
            params.remove("secretKey");
            String json_params=DataTypeConversionUtil.map2json(params).toJSONString();
            //String[] cmds={"curl","-X","POST","--header","Content-Type: application/json","--header","Accept: application/json","-d",json_params,url};
            String res= HttpRequest.okHttpPost(url,json_params);
            if (StringUtils.isNotBlank(res)){
                AntsDnsRVo r=DataTypeConversionUtil.string2Entity(res,AntsDnsRVo.class);
                if (null==r){
                    return R.error(res);
                }
                return r.thisVo2R();
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R addRecord(String ApiUrl,String domain,String appId,String appKey,String top,String recordType,String line,String value,String ttl){
        String eMsg="";
        try{
            String url=ApiUrl+"Record.Create";
            Map params=new HashMap(1024);
            params.put("domain",domain);
            params.put("record_type",recordType);
            params.put("top",top);
            if(StringUtils.isNotBlank(line)){
                params.put("line",line);
            }else {
                params.put("line","默认");
            }
            params.put("poll_rule",0);
            JSONArray jsonArray=new JSONArray();
            DnsRecordInputPollDataVo vo=new DnsRecordInputPollDataVo();
            vo.setData(value);
            jsonArray.add(vo);
            params.put("poll_data_array",jsonArray);
            params.put("value",value);
            params.put("ttl",ttl);
            params.put("mx","1");
            params.put("weight","1");
            params.put("secretId",appId);
            params.put("secretKey",appKey);
            params.put("sign",AntsDnsApiService.getSign(params));
            params.remove("secretKey");
            String json_params=DataTypeConversionUtil.map2json(params).toJSONString();
            //String[] cmds={"curl","-X","POST","--header","Content-Type: application/json","--header","Accept: application/json","-d",json_params,url};
            String res= HttpRequest.okHttpPost(url,json_params);
            if (StringUtils.isNotBlank(res)){
                AntsDnsRVo r=DataTypeConversionUtil.string2Entity(res,AntsDnsRVo.class);
                if (null==r){
                    return R.error(res);
                }
                return r.thisVo2R();
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }

    public static R GetRecordByInfo(String ApiUrl,String domain,String appId,String appKey,String top,String type,String line){
        String eMsg="";
        try{
            List<JSONObject> result=new ArrayList<>();
            R rl= getRecordList(ApiUrl,domain,appId,appKey);
            if (1!=rl.getCode()){
                return rl;
            }
            List<String > antsDnsList=(List<String>)rl.get("data");
            for (String str:antsDnsList){
                JSONObject dnsJsonObject=DataTypeConversionUtil.string2Json(str);
                if(null!=dnsJsonObject){
                    if(dnsJsonObject.containsKey("top") && dnsJsonObject.containsKey("record_type") && dnsJsonObject.containsKey("record_line_name")){
                        if(dnsJsonObject.getString("top").equals(top)){
                            if(dnsJsonObject.getString("record_type").equals(type)){
                                if (StringUtils.isBlank(line)){
                                    result.add(dnsJsonObject);
                                }else {
                                    if(dnsJsonObject.getString("record_line_name").equals(line)){
                                        result.add(dnsJsonObject);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return R.ok().put("data",result);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }

}
