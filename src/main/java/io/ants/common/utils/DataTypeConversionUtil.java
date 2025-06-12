package io.ants.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.cglib.beans.BeanMap;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Administrator
 */
public class DataTypeConversionUtil {


    private static final String DOMAIN_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(DOMAIN_REGEX);

    private static final String DOMAIN_PORT_REGEX="^([a-zA-Z0-9-]+\\.)*[a-zA-Z0-9-]+\\.[a-zA-Z]{2,7}(:[0-9]{1,5})?$";
    private static final Pattern  DOMAIN_PORT_PATTERN=Pattern.compile(DOMAIN_PORT_REGEX);


    private static final String URI_REGEX = "^http(s)?://([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    private static  <T>T jsonToMapOrJavaBean(JSON jsonObject, Class<T> clazz){
        return jsonObject.toJavaObject(clazz);
    }

    private static JSONObject mapToJsonOrJavaBean(Map map){
        JSONObject jsonObject = new JSONObject(map);
        return  jsonObject;
    }

    public static  JSONObject map2json(Map map){
        return  mapToJsonOrJavaBean(map);
    }


    public static Map entity2map(Object entityObj){
        Map map = new HashMap();
        try{
            if (null!=entityObj){
                return BeanMap.create(entityObj);
            }
        }catch (Exception e){
            map.put("eMsg",e.getMessage());
            e.printStackTrace();
        }
        return map;
    }

    public static void daoEntity2VoEntity(Object sourceDao,Object targetVo){
        try {
            //noinspection AlibabaAvoidApacheBeanUtilsCopy
            BeanUtils.copyProperties( targetVo,sourceDao);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static <T>T map2entity(Map map, Class<T> clazz){
        JSONObject jsonObject=mapToJsonOrJavaBean(map);
        //CdnProductEntity productEntity=jsonToMapOrJavaBean(jsonObject,io.cdn.modules.sys.entity.CdnProductEntity.class);
        return jsonToMapOrJavaBean(jsonObject,clazz);
    }

    public static <T>T json2entity(JSONObject object,Class<T> clazz){
        try {
            return jsonToMapOrJavaBean(object,clazz);
        }catch (Exception e){
            e.printStackTrace();
        }
       return null;
    }

    public static JSONObject entity2json(Object object){
        JSONObject json=new JSONObject();
        try{
           return  JSON.parseObject(JSON.toJSONString(object));
        }catch (Exception e){
            json.put("eMsg",e.getMessage());
            e.printStackTrace();
        }
        return json;
    }
    public static <T> T string2Entity(String str, Class<T> clazz) {
        try{
            if(StringUtils.isNotBlank(str)){
                return new Gson().fromJson(str, clazz);
            }
            return clazz.newInstance();
        }catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }



    public static  String entity2jonsStr(Object object){
        try{
            return  JSON.parseObject(JSON.toJSONString(object)).toJSONString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject entity2jsonObj(Object object){
        try{
            return  JSONObject.parseObject(JSONObject.toJSONString(object));
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject string2Json(String jsonStr){
        try{
            if (StringUtils.isNotBlank(jsonStr)){
                return JSONObject.parseObject(jsonStr);
            }
        }catch (Exception e){
            System.out.println("string2Json fail :"+jsonStr+ ","+e.getMessage());
        }
        return new JSONObject();
    }

    public static JSONArray string2JsonArray(String jsonStr){
        try{
            return JSONArray.parseArray(jsonStr);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new JSONArray();
    }



    public static Map urlParams2Map(String str){
        try{
            //info=nginx%3A+%5Bemerg%5D+unknown+directive+%22unknown_chunk%22+in+%2Fhome%2Flocal%2Fnginx%2Fconf%2Fconf%2Fsite%2F1_text.com_.conf%3A18%0Anginx%3A+configuration+file+%2Fhome%2Flocal%2Fnginx%2Fconf%2Fnginx.conf+test+failed%0A&recordId=1652425342491-0
            Map map =new HashMap();
            String[] params=str.split("&");
            for (String param:params){
                Integer i=param.indexOf("=");
                if(-1!=i){
                    String key=param.substring(0,i);
                    String value=param.substring(i+1);
                    String r_value= URLDecoder.decode(value,"utf-8");
                    map.put(key,r_value);
                }
            }
            return map;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public static Map jsonToMap(JSONObject s) {
        try{
            Map  map = (Map) s;
            return map;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static void updateEntity(  Map oldM,Map newM){
        try {
            newM.keySet().forEach(item->{
                if (null==newM.get(item)){
                    newM.put(item,oldM.get(item));
                }else if(StringUtils.isBlank(newM.get(item).toString())){
                    newM.put(item,oldM.get(item));
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public static boolean isValidDomain(String domain){
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    public static boolean isValidDomainPort(String domainPortStr){
        return DOMAIN_PORT_PATTERN.matcher(domainPortStr).matches();
    }

    public static boolean isValidUri(String uri){
        return URI_PATTERN.matcher(uri).matches();
    }

    /**
     * 下划线转驼峰
     * @param str
     * @return
     */
    public static String traceName(String str){
        //String str = "my_variable_name";
        String[] parts = str.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        String camelCaseStr = sb.toString();
        //System.out.println(camelCaseStr);
        return  camelCaseStr;
    }

}
