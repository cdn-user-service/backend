package io.ants.modules.utils.service.alipay;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayUserCertifyOpenCertifyRequest;
import com.alipay.api.request.AlipayUserCertifyOpenInitializeRequest;
import com.alipay.api.request.AlipayUserCertifyOpenQueryRequest;
import com.alipay.api.response.AlipayUserCertifyOpenCertifyResponse;
import com.alipay.api.response.AlipayUserCertifyOpenInitializeResponse;
import com.alipay.api.response.AlipayUserCertifyOpenQueryResponse;
import io.ants.common.utils.R;
import io.ants.modules.utils.config.alipay.AlipayUserCertifyConfig;

public final class AlipayUserCertifyService {
    private static AlipayUserCertifyConfig certifyConfig;

    //密钥类型RSA
    // PSC1-->PSC8 PHP 生成的PSC1 应用私钥需转换为JAVA PSC8格式的密钥
    //https://miniu.alipay.com/keytool/format
    /** 支付宝网关 **/
    private static String URL = "https://openapi.alipay.com/gateway.do";


    public AlipayUserCertifyService(AlipayUserCertifyConfig certifyConfig) {
        AlipayUserCertifyService.certifyConfig = certifyConfig;
    }

    public R certifyInitialize(String outer_order_no, String cert_name, String cert_no, String return_url )   {
        /** 支付宝开放认证初始化服务，通过入参用户身份信息拿到本次认证的唯一ID（certifyID），该ID商户需要妥善保存，后续接口调用需要作为入参 */
        String eMsg="";
        try{
            //System.out.println(APP_PRIVATE_KEY);
            //System.out.println(certifyConfig.getPrivateKey());
            //System.out.println(ALIPAY_PUBLIC_KEY);
            //System.out.println(certifyConfig.getAlipayPublicKey());
            /** 初始化 **/
            //AlipayClient alipayClient = new DefaultAlipayClient(URL, APP_ID, APP_PRIVATE_KEY, "json", "UTF-8", ALIPAY_PUBLIC_KEY, "RSA2");
            DefaultAlipayClient alipayClient = new DefaultAlipayClient(URL, certifyConfig.getAppId(), certifyConfig.getPrivateKey(), "json", "UTF-8", certifyConfig.getAlipayPublicKey(), "RSA2");
            AlipayUserCertifyOpenInitializeRequest request = new AlipayUserCertifyOpenInitializeRequest();

//        request.setBizContent("{" +
//
//                /**商户请求的唯一标识，商户要保证其唯一性*/
//                "\"outer_order_no\":\""+outer_order_no+"\"," +
//
//                /**bizCode：认证场景码。*/
//                "\"biz_code\":\"FACE\"," +
//
//                "\"identity_param\":{" +
//
//                /**身份信息参数类型，必填，必须传入 CERT_INFO**/
//                "\"identity_type\":\"CERT_INFO\"," +
//
//                /**真实姓名，必填，填写需要验证的真实姓名，与证件类型对应**/
//                "\"cert_type\":\"IDENTITY_CARD\"," +
//
//                /**证件号码，必填，填写需要验证的证件号码，与证件类型对应*/
//                "\"cert_name\":\""+cert_name+"\"," +
//
//                /**需要验证的身份信息参数，格式为 json。*/
//                "\"cert_no\":\""+cert_no+"\"" +
//
//                "}," +
//
//                "\"merchant_config\":{" +
//
//                /**需要回跳的目标地址，必填，一般指定为商户业务页面。**/
//                "\"return_url\":\""+return_url+"\"" +
//
//                "}," +
//
//                "}");

            JSONObject jsonObject=new JSONObject();
            jsonObject.put("outer_order_no",outer_order_no);
            jsonObject.put("biz_code","FACE");
            JSONObject jsonObject_identity_param=new JSONObject();
            jsonObject_identity_param.put("identity_type","CERT_INFO");
            jsonObject_identity_param.put("cert_type","IDENTITY_CARD");
            jsonObject_identity_param.put("cert_name",cert_name);
            jsonObject_identity_param.put("cert_no",cert_no);
            jsonObject.put("identity_param",jsonObject_identity_param);
            JSONObject jsonObject_merchant_config=new JSONObject();
            jsonObject_merchant_config.put("return_url",return_url);
            jsonObject.put("merchant_config",jsonObject_merchant_config);
            System.out.println("Alipay_User_Certify_certify_Initialize:"+jsonObject.toJSONString());
            request.setBizContent(jsonObject.toJSONString());
            AlipayUserCertifyOpenInitializeResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                System.out.println(  response.getCertifyId()   );
                return R.ok().put("data",response.getCertifyId());
                //return response.getCertifyId();
            } else {
                //System.out.println(response);
                //System.out.println("调用失败");
                return R.error("认证失败").put("data",response);
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public  R  certify(String certify_id)  {
        /** 在完成开放认证初始化后，通过certifyID组装出本次认证的URL发起认证。*/
        String  eMsg="";
        try{
            /** 初始化 **/
            AlipayClient alipayClient = new DefaultAlipayClient(URL, certifyConfig.getAppId(),certifyConfig.getPrivateKey() , "json", "UTF-8", certifyConfig.getAlipayPublicKey(), "RSA2");

            AlipayUserCertifyOpenCertifyRequest request = new AlipayUserCertifyOpenCertifyRequest();

            //        request.setBizContent("{" +
            //
            //                /** 本次申请操作的唯一标识，由开放认证初始化接口调用后生成，后续的操作都需要用到 **/
            //                "\"certify_id\":\""+certify_id+"\"," +
            //
            //                "}");
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("certify_id",certify_id);
            request.setBizContent(jsonObject.toJSONString());
            System.out.println("certify:"+jsonObject.toJSONString());
            /**生成请求链接，这里一定要使用GET模式*/
            AlipayUserCertifyOpenCertifyResponse response = alipayClient.pageExecute(request, "GET");
            if (response.isSuccess()) {
                System.out.println("开始认证服务调用成功");
                String certifyUrl = response.getBody();
                System.out.println(certifyUrl);
                //return  certifyUrl;
                return R.ok().put("data",certifyUrl);
                //执行后续流程...
            } else {
                System.out.println("调用失败");
                //return  null;
                return R.error("认证失败").put("data",response);
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public  R certifyQuery(String certifyId)  {
        /** 商户在开放认证完成后，调用本接口入参certifyID查询认证状态和相关数据。*/
        String eMsg="";
        /** 初始化 **/
        try{
            AlipayClient alipayClient = new DefaultAlipayClient(URL, certifyConfig.getAppId(), certifyConfig.getPrivateKey(), "json", "UTF-8", certifyConfig.getAlipayPublicKey(), "RSA2");

            AlipayUserCertifyOpenQueryRequest request = new AlipayUserCertifyOpenQueryRequest();
//        request.setBizContent("{" +
//                /** 本次申请操作的唯一标识，由开放认证初始化接口调用后生成，后续的操作都需要用到 **/
//                "\"certify_id\":\""+certify_id+"\"," +
//                "}");
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("certify_id",certifyId);
            request.setBizContent(jsonObject.toJSONString());

            AlipayUserCertifyOpenQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                //System.out.println("调用成功");
                System.out.println(response.getBody());
                //return  response.getBody();
                return R.ok().put("data",response.getBody());
                //执行后续流程...
            } else {
                //System.out.println("调用失败");
               // return null;
                return R.error("认证失败").put("data",response);
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }
}
