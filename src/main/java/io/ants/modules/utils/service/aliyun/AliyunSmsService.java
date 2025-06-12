package io.ants.modules.utils.service.aliyun;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import io.ants.common.utils.R;
import io.ants.modules.utils.config.aliyun.AliyunSmsConfig;
import org.apache.commons.lang.StringUtils;

public class AliyunSmsService {

    private  AliyunSmsConfig aliyunSmsConfig;


    public AliyunSmsService( AliyunSmsConfig aliyunSmsConfig){
        this.aliyunSmsConfig=aliyunSmsConfig;
    }

    /**
     * 使用AK&SK初始化账号Client
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    private com.aliyun.dysmsapi20170525.Client createClient(String accessKeyId, String accessKeySecret)  {
        try{
            com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                    // 您的 AccessKey ID
                    .setAccessKeyId(accessKeyId)
                    // 您的 AccessKey Secret
                    .setAccessKeySecret(accessKeySecret);
            // 访问的域名
            config.endpoint = "dysmsapi.aliyuncs.com";
            return new com.aliyun.dysmsapi20170525.Client(config);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private com.aliyun.dysmsapi20170525.Client createClient(){
        return  this.createClient(aliyunSmsConfig.getAccessKeyId(),aliyunSmsConfig.getAccessKeySecret());
    }

    public R sendSms(String mobile, String tempName, JSONObject paramObj){
        String eMsg="";
        com.aliyun.dysmsapi20170525.Client client = this.createClient();
        if (null==client){
            return R.error("createClient fail!");

        }
        String moduleCode="";
        for (AliyunSmsConfig.SmsTempVo item:aliyunSmsConfig.getTemplateIds()){
            if (1==item.getStatus() && tempName.equals(item.getName())){
                moduleCode=item.getId();
                break;
            }
        }
        if (StringUtils.isBlank(moduleCode)){
            return R.error("code is empty!");
        }
        com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                .setPhoneNumbers(mobile)
                .setSignName(this.aliyunSmsConfig.getSignname())
                .setTemplateCode(moduleCode)
                .setTemplateParam(paramObj.toJSONString());
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            // 复制代码运行请自行打印 API 的返回值
            SendSmsResponse res= client.sendSmsWithOptions(sendSmsRequest, runtime);
            return R.ok().put("data",res);
        } catch (TeaException error) {
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
            eMsg=error.message;
        } catch (Exception error2) {
            TeaException error = new TeaException(error2.getMessage(), error2);
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
            eMsg=error.message;
        }
        return R.error(eMsg);
    }

    public static void main(String[] args) {
        AliyunSmsConfig config=new AliyunSmsConfig();
        AliyunSmsService aliSms=new AliyunSmsService(config);
        JSONObject object=new JSONObject();
        object.put("name","张三");
        object.put("number","15629529961");
        aliSms.sendSms("15629529961","code",object);
    }
}
