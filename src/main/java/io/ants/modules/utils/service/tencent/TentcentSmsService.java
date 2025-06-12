package io.ants.modules.utils.service.tencent;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20190711.SmsClient;
import com.tencentcloudapi.sms.v20190711.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse;
import io.ants.common.utils.R;
import io.ants.modules.utils.config.tencent.TencentSmsConfig;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class TentcentSmsService {
    //private final static Logger logger = LoggerFactory.getLogger(TentcentSmsService.class);

    private TencentSmsConfig config;

//    private  static String SecretId="";
//    private  static  String SecretKey="";
//    private  static String SdkAppId="";
//    private  static String SignName="";
//    private  static JSONArray templateIds=null;

    public TentcentSmsService(TencentSmsConfig config) {
        this.config = config;

    }

    private TentcentSmsService(String SecretId, String SecretKey, String SdkAppId, String SignName, List<TencentSmsConfig.SmsTempVo> templateIds){
        this.config.setSecretid(SecretId);
        this.config.setSecretkey(SecretKey);
        this.config.setSdkappId(SdkAppId);
        this.config.setSignname(SignName);
        this.config.setTemplateIds(templateIds);
    }


    public R sendSms(String mobile, String templateName, String[] templateParam ){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            //  Credential cred = new Credential(SecretId, SecretKey);
            if(null==this.config){
                //System.out.println("短信配置有缺失[1]");
                return R.error("短信配置有缺失[1]");
            }
            if(StringUtils.isBlank(this.config.getSecretid()) || StringUtils.isBlank(this.config.getSecretkey())){
                //System.out.println("短信配置有缺失[2]");
                return  R.error("短信配置有缺失[2]");
            }
            Credential cred = new Credential(this.config.getSecretid(), this.config.getSecretkey());
            if(null==cred){
                //System.out.println("构造Credential fail !");
                return  R.error("构造Credential fail !");
            }
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            SmsClient client = new SmsClient(cred, "ap-beijing", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            SendSmsRequest req = new SendSmsRequest();
            String[] phoneNumberSet1 = {mobile};
            req.setPhoneNumberSet(phoneNumberSet1);

            //req.setSmsSdkAppid(SdkAppId);
            req.setSmsSdkAppid(this.config.getSdkappId());
            //req.setSign(SignName);
            req.setSign(this.config.getSignname());
            String TemplateID="";
            for (TencentSmsConfig.SmsTempVo tempVo:this.config.getTemplateIds()) {
                 if ( 1==tempVo.getStatus() && tempVo.getName().equals(templateName) ){
                     TemplateID=tempVo.getId();
                     break;
                 }

            }
            if(StringUtils.isEmpty(TemplateID)){
                //System.out.println("TemplateID is null ,seed error !");
                return R.error("TemplateID is null ,seed error !");
            }
            req.setTemplateID(TemplateID);
            // String[] templateParam = {"12", "123"};
            req.setTemplateParamSet(templateParam);
            // 返回的resp是一个SendSmsResponse的实例，与请求对象对应
            SendSmsResponse resp = client.SendSms(req);
            // 输出json格式的字符串回包
            //System.out.println(SendSmsResponse.toJsonString(resp));
            return R.ok().put("data",SendSmsResponse.toJsonString(resp)) ;
        } catch (TencentCloudSDKException e) {
            //System.out.println(SecretId+","+SecretKey+","+SdkAppId+","+SignName+","+templateIds);
            System.out.println(this.config.toString());
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return  R.error(eMsg);
    }

}
