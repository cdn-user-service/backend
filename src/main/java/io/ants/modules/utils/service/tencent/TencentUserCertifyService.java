package io.ants.modules.utils.service.tencent;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.faceid.v20180301.FaceidClient;
import com.tencentcloudapi.faceid.v20180301.models.DetectAuthRequest;
import com.tencentcloudapi.faceid.v20180301.models.DetectAuthResponse;
import com.tencentcloudapi.faceid.v20180301.models.GetDetectInfoRequest;
import com.tencentcloudapi.faceid.v20180301.models.GetDetectInfoResponse;
import io.ants.common.utils.R;
import io.ants.modules.utils.config.tencent.TencentUserCertifyConfig;

public class TencentUserCertifyService {
    private static TencentUserCertifyConfig tencentUserCertifyConfig;


    public TencentUserCertifyService(TencentUserCertifyConfig tencentUserCertifyConfig) {
        TencentUserCertifyService.tencentUserCertifyConfig = tencentUserCertifyConfig;
    }

    public R tencentUserCertify_init(String name, String idCard){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            //Credential cred = new Credential(SecretId, SecretKey);
            Credential cred = new Credential(tencentUserCertifyConfig.getSecretId(), tencentUserCertifyConfig.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("faceid.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            FaceidClient client = new FaceidClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectAuthRequest req = new DetectAuthRequest();
            req.setRuleId(tencentUserCertifyConfig.getRuleId());
            req.setIdCard(idCard);
            req.setName(name);
            // 返回的resp是一个DetectAuthResponse的实例，与请求对象对应
            DetectAuthResponse resp = client.DetectAuth(req);
            // 输出json格式的字符串回包
            //System.out.println(DetectAuthResponse.toJsonString(resp));
            //return DetectAuthResponse.toJsonString(resp);
            return R.ok().put("data",DetectAuthResponse.toJsonString(resp)).put("url",resp.getUrl());
        } catch (TencentCloudSDKException e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }



    public  R tencentUserCertify_query(String biztoken ){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(tencentUserCertifyConfig.getSecretId(), tencentUserCertifyConfig.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("faceid.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            FaceidClient client = new FaceidClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            GetDetectInfoRequest req = new GetDetectInfoRequest();
            req.setBizToken(biztoken);
            req.setRuleId(tencentUserCertifyConfig.getRuleId());
            // 返回的resp是一个GetDetectInfoResponse的实例，与请求对象对应
            GetDetectInfoResponse resp = client.GetDetectInfo(req);
            // 输出json格式的字符串回包
            System.out.println(GetDetectInfoResponse.toJsonString(resp));
            //return GetDetectInfoResponse.toJsonString(resp);
            return R.ok().put("data",GetDetectInfoResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
             eMsg=e.getMessage();
             e.printStackTrace();
        }
        return R.error(eMsg);
    }
}
