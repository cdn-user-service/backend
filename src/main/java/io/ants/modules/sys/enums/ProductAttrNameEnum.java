package io.ants.modules.sys.enums;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.modules.sys.vo.ProductAttrVo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ProductAttrNameEnum {
    ATTR_CC_DEFENSE(1,"defense","CC防御","QPS","int","cc防御流量上限","100",1,0),
    ATTR_FLOW(2,"flow","流量","G","int","每月流量上限","10",1,0),
    ATTR_SITE(1,"site","站点","个","int","可添加站点数","10",1,0),
    ATTR_PORT_FORWARDING(1,"port_forwarding","端口转发","个","int","端口转发上限","5",1,0),
    ATTR_PUBLIC_WAF(1,"public_waf","公共WAF","","bool","防火墙功能是否可用","1",2,0),
    ATTR_PRIVATE_WAF(1,"private_waf","专属WAF","","bool","专属防火墙功能是否可用","1",2,0),
    ATTR_AI_WAF(1,"ai_waf","AI WAF","","bool","智能WAF功能","0",2,0),
    ATTR_SMS(1,"sms","短信通知","条","int","每月短信通知数量","100",1,0),
    ATTR_MONITOR(1,"monitor","流量监控","","bool","站点流量监控数据","1",2,0),
    ATTR_LIVE_DATA(1,"live_data","实时数据","","bool","实时数据","1",2,0),
    ATTR_SERVICE(1,"service","服务级别","","text","服务级别","7x24专属服务",3,0),
    ATTR_CHARGING_MODE(2,"charging_mode","计费方式","","select","{\"1\":\"流量月结\",\"2\":\"平均日峰带宽\",\"3\":\"月95带宽\"}","1",0,0),
    ATTR_BANDWIDTH_PRICE(2,"bandwidth","带宽","元/Mbps/月","price_int","元/Mbps/月","20",0,0),
    ATTR_CUSTOM_DNS(1,"custom_dns","自定义dns","","bool","自定义dns","0",2,0),
    ATTR_DD_DEFENSE(1,"dd_defense","DDos防御","GB","int","ddos防御流量上限","100",1,0),
    ATTR_URL_REWRITE(1,"url_rewrite","URL转发","个","int","URL转发数量","100",1,2),
    ATTR_MAX_LIMIT_RATE(1,"limit_rate","传输速度","k/s","int","限制连接传输速度,1024k 为 1MB/s","102400",2,2),
    ATTR_NODE_TYPE(1,"node_type","节点类型","","text","节点类型","共享节点",3,2) ,

    ATTR_FORCE_URL_TYPE(1,"force_url","强制跳转","个","int","强制跳转","10",1,1),
    ATTR_ANTI_SHIELDING_TYPE(1,"anti_shielding","防屏蔽","","bool","防屏蔽","1",3,1),
    ATTR_SSL_CERT(1,"ssl_cert","SSL证书","个","int","免费ssl证书","999999",3,1),
    SEO_DNS(1,"seo_dns","搜索引擎回源","","bool","搜索引擎回源","0",2,0),

    //防屏蔽 SSL证书
    ;

    private final  int  group;

    private final  String attr;

    private final  String name;

    private final  String suffix;

    private final  String type;

    private final  String describe;

    private final  String defaultValue;

    private final  Integer canAddMode;

    private final  Integer plusFlag;

    ProductAttrNameEnum(int  group,String attr,String name,String suffix,String type,String describe,String defaultValue,Integer canAddMode,Integer plusFlag){
        this.group=group;
        this.attr=attr;
        this.name=name;
        this.suffix=suffix;
        this.type=type;
        this.describe=describe;
        this.defaultValue=defaultValue;
        this.canAddMode=canAddMode;
        this.plusFlag=plusFlag;
    }

    public int getGroup() {
        return group;
    }

    public String getAttr() {
        return attr;
    }

    public String getName() {
        return this.name;
    }

    public String getDescribe() {
        return this.describe;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Integer getCanAddMode() {
        return canAddMode;
    }

    public Integer getPlusFlag() {
        return plusFlag;
    }

    private static   boolean getInFlag(Integer flag){
        if (0==flag  ){
            return true;
        }else if(1==flag && QuerySysAuth.PLUS_1_FLAG ){
            return true;
        }else if (2==flag && QuerySysAuth.PLUS_2_FLAG ){
            return true;
        }
        return false;
    }


    public static  List<String> getAllAttr(){
        List<String> list=new ArrayList<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if ( getInFlag(item.getPlusFlag())   ){
                list.add(item.getAttr());
            }
        }
        return list;
    }


    public static ProductAttrNameEnum getEnum(String attrName){
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if(item.getAttr().equals(attrName)){
                return item;
            }else if(item.getName().equals(attrName)){
                return item;
            }
        }
        return null;
    }

    public static ProductAttrNameEnum getEnumName(String name){
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if(item.getName().equals(name)){
                return item;
            }
        }
        return null;
    }

    public static  List<String> getAllName(){
        List<String> list=new ArrayList<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if ( getInFlag(item.getPlusFlag()) ){
                list.add(item.getName());
            }

        }
        return list;
    }

    public static Map getInfo(ProductAttrNameEnum item){
        Map result=new HashMap();
        result.put("group",item.getGroup());
        result.put("attr",item.getAttr());
        result.put("name",item.getName());
        result.put("suffix",item.getSuffix());
        result.put("type",item.getSuffix());
        result.put("describe",item.getDescribe());
        result.put("defalutValue",item.getDefaultValue());
        //Object[] result= {item.getAttr(),item.getName(),item.getSuffix(),item.getType(),item.getDescribe(),item.getDefaultValue(),item.getGroup()};
        return result;
    }

    public static Map<String,Object> getAll() {
        Map<String,Object> map =new HashMap<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            String[] name_desc={item.getName(),item.getSuffix(),item.getType(), item.getDescribe(),item.getDefaultValue(),String.valueOf(item.getGroup())};
            if (getInFlag(item.getPlusFlag()) ){
                map.put(item.getAttr(),name_desc);
            }

        }
        return map;
    }


    public static List<String> getAllTypes(){
        List<String> result=new ArrayList<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
           if(!result.contains(item.getType())){
               if (getInFlag(item.getPlusFlag())  ){
                   result.add(item.getType());
               }
           }
        }
        return  result;
    }

    public static Map<String,String > getKeyValues(){
        Map<String,String> map =new HashMap<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if (getInFlag(item.getPlusFlag())  ){
                map.put(item.getAttr(),item.getName());
            }

        }
        return map;
    }

    public static String getAttrCnName(String attr){
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
             if (item.getAttr().equals(attr) || item.getName().equals(attr)){
                 return  item.getName();
             }
        }
        return null;
    }

    public static ProductAttrNameEnum getEnumObjByAttr(String attr){
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if (item.getAttr().equals(attr) ){
                return  item;
            }
        }
        return null;
    }


    public static Map<String,String > getSuffixKeyValues(){
        Map<String,String> map =new HashMap<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if (getInFlag(item.getPlusFlag()) ){
                map.put(item.getAttr(),item.getSuffix());
            }

        }
        return map;
    }

    public static Map<String,Map> getAttrInfos() {
        Map<String,Map> map =new HashMap<>();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            String[] name_desc={item.getName(),item.getSuffix(),item.getType(), item.getDescribe(),item.getDefaultValue() };
            Map<String,Object> result=new HashMap<>();
            result.put("baseData",name_desc);
            result.put("group",item.getGroup());
            result.put("name",item.getName());
            result.put("suffix",item.getSuffix());
            result.put("type",item.getSuffix());
            result.put("describe",item.getDescribe());
            result.put("defalutValue",item.getDefaultValue());
            map.put(item.getAttr(),result);
        }
        return map;
    }

    /**
     * @param AttrJson
     * @return
     */
    public static JSONObject getAllAttrMaxValueJson(JSONObject AttrJson){
        JSONObject final_max_attr_json=new JSONObject();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            String attr=item.getAttr();
            if(AttrJson.containsKey(attr)){
                final_max_attr_json.put(attr,AttrJson.get(attr));
            }else{
                final_max_attr_json.put(attr,null);
            }
        }
        return final_max_attr_json;
    }

    public static Map buildProductAttr(){
        Map result=new HashMap();
        Map productAttrObj=new HashMap();
        Map productAttr=new HashMap();
        for (ProductAttrNameEnum item : ProductAttrNameEnum.values()) {
            if (getInFlag(item.getPlusFlag())  ){
                if ( 1 == item.getGroup() ){
                    Map attrMap=new HashMap();
                    attrMap.put("name",item.getName());
                    attrMap.put("valueType",item.getType());
                    attrMap.put("value",item.getDefaultValue());
                    attrMap.put("unit",item.getSuffix());
                    attrMap.put("superpositionMode",item.getCanAddMode());
                    if ("select".equals(item.getType())){
                        attrMap.put("select",item.getDescribe());
                    }
                    productAttrObj.put(item.getAttr(),attrMap);
                }else if (2==item.getGroup()){
                    Map attrMap=new HashMap();
                    attrMap.put("name",item.getName());
                    attrMap.put("valueType",item.getType());
                    attrMap.put("value",item.getDefaultValue());
                    if ("select".equals(item.getType())){
                        attrMap.put("select", DataTypeConversionUtil.string2Json(item.getDescribe()) );
                    }
                    productAttr.put(item.getAttr(),attrMap);
                }
            }
        }
        result.put("productAttr",productAttr);
        result.put("productAttrObj",productAttrObj);
        return result;
    }

    public static JSONObject getFinalAttrJson(JSONArray product_attr_json_array){
        //[
        // {"id":3,"name":"defense","unit":"G","transferMode":null,"valueType":"int","value":20,"status":1,"weight":2},
        // {"id":2,"name":"flow","unit":"G","transferMode":null,"valueType":"int","value":1001,"status":1,"weight":1}
        // ]
        JSONObject final_attr_json=new JSONObject();
        for (int i = 0; i <product_attr_json_array.size() ; i++) {
            JSONObject json_item=product_attr_json_array.getJSONObject(i);
            if(json_item.containsKey("name")&& json_item.containsKey("value")){
                String attrName=  json_item.getString("name");
                ProductAttrNameEnum enum_obj=ProductAttrNameEnum.getEnum(attrName);
                if(null==enum_obj){
                    continue;
                }
                List<String> attr_list=ProductAttrNameEnum.getAllTypes();
                if(attr_list.contains(enum_obj.getType())){
                    final_attr_json.put(enum_obj.getAttr(),json_item.get("value").toString());
                }else{
                    System.out.println("["+attrName +"] unknown type ");
                }

            }
        }
        return  final_attr_json;
    }


    public static void getFinalAttrJson(ProductAttrVo vo, JSONArray product_attr_json_array){
        for (int i = 0; i <product_attr_json_array.size() ; i++) {
            JSONObject json_item=product_attr_json_array.getJSONObject(i);
            if(json_item.containsKey("name")&& json_item.containsKey("value")){
                String attrName=  json_item.getString("name");
                ProductAttrNameEnum enum_obj=ProductAttrNameEnum.getEnum(attrName);
                if(null==enum_obj){
                    continue;
                }
                List<String> attr_list=ProductAttrNameEnum.getAllTypes();
                if(attr_list.contains(enum_obj.getType())){
                    String key=enum_obj.getAttr();
                    String value=json_item.get("value").toString();
                    try {
                        Field field = vo.getClass().getDeclaredField(key);
                        if (null!=field){
                            field.setAccessible(true);
                            if (field.getType()==Integer.class ){
                                field.set(vo,Integer.valueOf(value) );
                            }else if (field.getType()==Long.class ){
                                field.set(vo,Long.valueOf(value) );
                            }else if (field.getType()==String.class ){
                                field.set(vo,value);
                            }
                        }
                    }catch (Exception e){
                        System.out.println(e.getMessage()+",is empty!");
                    }
                }else{
                    System.out.println(attrName +" unknown type ");
                }

            }
        }

    }

    public static JSONObject getFinalAttrJsonByAttrJsonArrayStr(String attr_json_array_str){
        try{
            JSONArray product_attr_json_array=DataTypeConversionUtil.string2JsonArray(attr_json_array_str);
            return getFinalAttrJson(product_attr_json_array);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
