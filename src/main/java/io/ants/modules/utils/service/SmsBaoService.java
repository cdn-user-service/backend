package io.ants.modules.utils.service;

import com.alibaba.fastjson.JSONObject;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.utils.config.SmsBaoConfig;
import io.ants.modules.utils.service.fuiou.HttpUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;

public class SmsBaoService {

    final private Logger logger = LoggerFactory.getLogger(getClass());

    SmsBaoConfig smsBaoConfig;

    public SmsBaoService(SmsBaoConfig smsBaoConfig){
        this.smsBaoConfig=smsBaoConfig;
    }

    public R sendSms(String mobile, String tempName , JSONObject paramJson ){
        String eMsg="发送失败！";
        try{
            ValidatorUtils.validateEntity(smsBaoConfig);
            String sendContent="";
            for (SmsBaoConfig.SmsTempVo item:smsBaoConfig.getTemplateIds()){
                if (1==item.getStatus() && tempName.equals(item.getName())){
                    sendContent=item.getContent();

                }
            }
            if (StringUtils.isBlank(sendContent)){
                return R.error("发送失败，SmsBaoConfig未配置");
            }
            if (mobile.contains("+86")){
                mobile=mobile.replace("+86","");
            }
            if (null!=paramJson){
                for (String key:paramJson.keySet()){
                    sendContent=sendContent.replace(key,paramJson.get(key).toString());
                }
            }
            String gUrl=String.format(smsBaoConfig.getApi_addr()+"?u=%s&p=%s&m=%s&c=%s",smsBaoConfig.getUsername(),smsBaoConfig.getApi_key(), URLEncoder.encode(mobile,"UTF-8"),URLEncoder.encode(sendContent,"UTF-8"));
           //logger.debug(gUrl);
            String res= HttpUtils.get(gUrl);
            if (StringUtils.isNotBlank(res)  ){
                if ("0".equals(res)){
                    return R.ok(res);
                }else {
                    return R.error(res);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static void main(String[] args) {
        SmsBaoConfig smsBaoConfig=new SmsBaoConfig();
        smsBaoConfig.setApi_addr("https://api.smsbao.com/sms");
//        smsBaoConfig.setUsername("331434376");
        smsBaoConfig.setUsername("cf2419750946");
//        smsBaoConfig.setApi_key("2e77032b055948399f4ae148260b7e79");
        smsBaoConfig.setApi_key("2fa7e795f4604f3eb9c73939453687cc");
        JSONObject object=new JSONObject();
        object.put("name","张三");
        object.put("code","112244");
        R r= new  SmsBaoService(smsBaoConfig).sendSms("15629529961","code",object);
        System.out.println(r.toString());
    }
}
