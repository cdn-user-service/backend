package io.ants.modules.utils.service.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.ants.common.exception.RRException;
import io.ants.modules.utils.config.wechat.WXLoginConfig;
import io.ants.modules.utils.service.fuiou.HttpUtils;
import org.apache.commons.lang.StringUtils;

import java.net.URLEncoder;

public class WXLoginService {


    static WXLoginConfig config;

    public WXLoginService() {
    }

    public WXLoginService(WXLoginConfig config) {
        WXLoginService.config = config;
    }



    public String wechatLogin_appid(){
        if(null==config){
            return null;
        }
        return config.getAppId();
    }

    public   String wechatLogin_req_url(String state,String redirect_url)  {
        //final String REDIRECT_URI= URLEncoder.encode(redirecturi,"utf-8");
        try{
            String REDIRECT_URI="";
            if(StringUtils.isNotBlank(redirect_url)){
                REDIRECT_URI=redirect_url;
            }else {
                REDIRECT_URI= URLEncoder.encode(config.getRedirectUri(),"utf-8");
            }
            final String SCOPE="snsapi_login";
            String STATE=state;
            //return "https://open.weixin.qq.com/connect/qrconnect?appid="+wechatAppId+"&redirect_uri="+REDIRECT_URI+"&response_type=code&scope="+SCOPE+"&state="+STATE+"#wechat_redirect";
            return "https://open.weixin.qq.com/connect/qrconnect?appid="+ config.getAppId()+"&redirect_uri="+REDIRECT_URI+"&response_type=code&scope="+SCOPE+"&state="+STATE+"#wechat_redirect";

        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public  JSONObject wechatLoginCallback(String code){
        if(code.length()<1){
            throw new RRException("微信 登录 错误");
        }
        JSONObject ret_msg_json=new JSONObject();
        String url="https://api.weixin.qq.com/sns/oauth2/access_token?appid="+config.getAppId()+
                "&secret="+config.getAppSecret()+
                "&code="+code+
                "&grant_type=authorization_code";
        try{
            //code-->access_token
            String result = HttpUtils.get(url);
            JSONObject jsonObject= JSON.parseObject(result);
            if(jsonObject.containsKey("access_token")){
                String access_token=jsonObject.getString("access_token");
                String openid=jsonObject.getString("openid");
                // Access Token + openid ->
                url="https://api.weixin.qq.com/sns/userinfo?access_token="+access_token+"&openid="+openid;
                result=HttpUtils.get(url);
                jsonObject= JSON.parseObject(result);
                if(jsonObject.containsKey("unionid")){
                    ret_msg_json=jsonObject;
                }else {
                    System.out.println("error_unionid");
                }
            }else {
                System.out.println("error_access_token");
            }
        } catch (Exception e){
            e.printStackTrace();
        }



        return ret_msg_json;
    }

    private  String Htmlstr(String msg){
        String html =   "<!DOCTYPE html>" +
                "<html lang=\"zh-cn\">" +
                "<head>" +
                "	<title>antsxdp</title>" +
                "	<meta charset=\"utf-8\"/>" +
                "</head>" +
                "<body>" +
                "	<script type=\"text/javascript\">" +
                "	location.href = \"http://www.antsxdp.cn/login/?msg= "+msg +"\""+
                "	</script>" +
                "</body>" +
                "</html>";
        return  html;
    }

}
