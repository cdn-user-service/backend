package io.ants.modules.sys.vo;

import com.alibaba.fastjson.JSONObject;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.modules.sys.entity.CdnSuitEntity;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;

@Data
public class ProductAttrVo {
    private Integer defense=0;
    private Long flow=0L;
    private Integer site=0;
    private Integer port_forwarding=0;
    private Integer public_waf=0;
    private Integer private_waf=0;
    private Integer ai_waf=0;
    private Integer sms=0;
    private Integer monitor=0;
    private Integer live_data=0;
    private String service="";
    private Integer charging_mode=0;
    private Integer bandwidth=0;
    private Integer custom_dns=0;
    private Integer dd_defense=0;
    private Integer url_rewrite=0;
    private Integer limit_rate=102400;
    private String node_type="";
    private Integer seo_dns=0;
    private Integer force_url=0;

    public static void updateAttrVoFromString(ProductAttrVo vo,String info){
        JSONObject attrJson=JSONObject.parseObject(info);
        for (String key:attrJson.keySet()){
            try {
                if (hasField(ProductAttrVo.class,key)){
                    Field field = vo.getClass().getDeclaredField(key);
                    if (null!=field){
                        field.setAccessible(true);
                        String value="";
                        if (null!=attrJson.get(key)){
                            value=attrJson.get(key).toString();
                        }
                        if (field.getType()==Integer.class ){
                            if (StringUtils.isNotBlank(value)){
                                field.set(vo,Integer.valueOf(value ));
                            }else {
                                field.set(vo,0);
                            }
                        }else if (field.getType()==Long.class ){
                            if (StringUtils.isNotBlank(value)){
                                field.set(vo,Long.valueOf(value) );
                            }else {
                                field.set(vo,0L );
                            }
                        }else if (field.getType()==String.class ){
                            field.set(vo,value);
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static boolean  hasField(Class<?> clazz, String fieldName){
        try {
            clazz.getDeclaredField(fieldName);
             return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    public static void updateSuitAttrByAttrVo(CdnSuitEntity suitEntity,ProductAttrVo vo){
        //System.out.println(suitEntity.toString());
        //System.out.println(vo.toString());
        Field[] fields = vo.getClass().getDeclaredFields();
        for (Field voField : fields) {
            String name=voField.getName();
            String tName= DataTypeConversionUtil.traceName(name);
            try{
                if (hasField(CdnSuitEntity.class,tName)){
                    Field entityField=suitEntity.getClass().getDeclaredField(tName);
                    if (null!=entityField){
                        //System.out.println("----->");
                        entityField.setAccessible(true);
                        String value=voField.get(vo).toString();
                        //System.out.println(name+"----"+tName+"----"+value);
                        if (entityField.getType()==Integer.class ){
                            entityField.set(suitEntity,Integer.valueOf(value ));
                        }else if(entityField.getType()==int.class){
                            entityField.setInt(suitEntity,Integer.parseInt(value));
                        } else if (entityField.getType()==Long.class ){
                            entityField.set(suitEntity,Long.valueOf(value ));
                        }else if (entityField.getType()==long.class){
                            entityField.setLong(suitEntity,Long.parseLong(value));
                        }else if (entityField.getType()==String.class ){
                            entityField.set(suitEntity,value);
                        }else {
                            System.out.println("error!----"+entityField.getType());
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
