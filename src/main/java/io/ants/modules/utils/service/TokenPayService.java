package io.ants.modules.utils.service;

import com.alibaba.fastjson.JSONObject;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.HttpRequest;
import io.ants.common.utils.R;
import io.ants.modules.utils.config.TokenPayConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class TokenPayService {



    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static  TokenPayConfig config;

    public TokenPayService(TokenPayConfig config) {
        TokenPayService.config=config;
    }

    public TokenPayConfig getConfig(){
        return TokenPayService.config;
    }

//    @Data
//    private class PostFormVo{
//        private String OutOrderId;
//        private String OrderUserKey;
//        private Double ActualAmount;
//        private String Currency;
//        private String PassThroughInfo;
//        private String NotifyUrl;
//        private String RedirectUrl;
//        private String Signature;
//    }

    public String getSignStr(JSONObject postData){
        if (null==postData || postData.isEmpty()){
            return "";
        }
        try{
            // 使用TreeMap存储排序后的键值对
            Map<String, Object> sortedData = new TreeMap<>(postData);


            StringBuilder concatenatedStr = new StringBuilder();
            for (String key : sortedData.keySet()) {
                if (!key.equalsIgnoreCase("Signature")){
                    concatenatedStr.append(key).append("=").append(sortedData.get(key)).append("&");
                }
            }
            String finalStr = concatenatedStr.substring(0, concatenatedStr.length() - 1) + config.getApiToken();
            logger.info(finalStr);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = finalStr.getBytes();
            byte[] md5hash = md.digest(data);

            StringBuilder hexString = new StringBuilder();
            for (byte b : md5hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";

    }

    public R sendOrder(String productName, double amount, String serialNumber, String current){
        String eMsg="";
        try{
            String tCurrent="";
            String[] cList=config.getCurrency().split("\\|");
            if (StringUtils.isBlank(current)){
                tCurrent=cList[0];
            }else{
                if (!Arrays.asList(cList).contains(current)){
                    return R.error("["+current+"]不支持！");
                }
                tCurrent=current;
            }
            JSONObject postData=new JSONObject();
            postData.put("OutOrderId",serialNumber);
            postData.put("OrderUserKey","productName="+productName);
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedAmount = df.format(amount);
            postData.put("ActualAmount",formattedAmount);
            postData.put("Currency",tCurrent);
            postData.put("NotifyUrl",config.getCallBackUrl());
            postData.put("RedirectUrl",config.getRedirectUrl());
            postData.put("Signature",this.getSignStr(postData));
            String r= HttpRequest.okHttpPost(config.getWebSiteUrl()+"/CreateOrder",postData.toJSONString());
            if (StringUtils.isNotBlank(r)){
                logger.info(r);
                JSONObject jsonObject= DataTypeConversionUtil.string2Json(r);
                if (null!=jsonObject && jsonObject.containsKey("success") && true==jsonObject.getBoolean("success")) {
                    return R.ok().put("data",jsonObject);
                }else{
                    return R.error(r);
                }
            }
            eMsg="创建"+tCurrent+"支付订单失败！，请联系管理员！";
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public static void main(String[] args) {
        TokenPayConfig conf=new TokenPayConfig();
        conf.setWebSiteUrl("https://pay.ddnn.com");
        conf.setApiToken("asdfvcxzvsgn!@");
        conf.setCurrency("USDT_TRC20|EVM_ETH_USDT_ERC20|EVM_ETH_ETH");
        conf.setCallBackUrl("http://demo.xxx.com");
        conf.setRedirectUrl("http://demo.xxx.com");
        //USDT_TRC20  ERC20 ETH
        R r=  new TokenPayService(conf).sendOrder("cdn",1000.00,"1122","EVM_ETH_ETH");
        System.out.println(r);
    }

}
