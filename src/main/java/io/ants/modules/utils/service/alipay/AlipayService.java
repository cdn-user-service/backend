package io.ants.modules.utils.service.alipay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayFundTransToaccountTransferModel;
import com.alipay.api.domain.AlipayFundTransUniTransferModel;
import com.alipay.api.domain.Participant;
import com.alipay.api.request.*;
import com.alipay.api.response.AlipayFundTransToaccountTransferResponse;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import io.ants.modules.utils.config.alipay.AlipayBeanConfig;
import io.ants.modules.utils.config.alipay.AlipayConfig;
import io.ants.modules.utils.factory.alipay.AlipayFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 支付宝支付接口
 *
 */
public final class AlipayService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static String serverUrl = "https://openapi.alipay.com/gateway.do";
    public static String format = "json";
    public static String charset = "utf-8";
    public static String alipayPublicKey = "";
    public static String signType = "RSA2";



    private AlipayConfig alipayConfig = null;


    public AlipayService( ) {

        if (null== this.alipayConfig){
            this.alipayConfig = AlipayFactory.build();
           //logger.debug("AlipayFactory.build()");
        }
    }

    public AlipayConfig getAlipayConfig() {
        return alipayConfig;
    }

    /**
     * 支付宝PC支付接口
     *
     * @param alipayBean
     * @return
     * @throws AlipayApiException
     */
    public String pcpay(AlipayBeanConfig alipayBean)   {
        //System.out.println(serverUrl+","+appId+","+privateKey+","+format+","+ charset+","+alipayPublicKey+","+signType);
        //System.out.println(alipayConfig.getAppId());
        String result="";
        try{
            if (null==alipayConfig){
                logger.error("alipayConfig is null !");
                return result;
            }
            AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, alipayConfig.getAppId(), alipayConfig.getPrivateKey(), format, charset, alipayPublicKey, signType);
            // 2、设置请求参数
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

            // 页面跳转同步通知页面路径
            alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
            // 服务器异步通知页面路径
            alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
            // 封装参数
            alipayRequest.setBizContent(JSON.toJSONString(alipayBean));
            // 3、请求支付宝进行付款，并获取支付结果
             result = alipayClient.pageExecute(alipayRequest).getBody();
            //logger.debug(result);
            // 返回付款信息
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }


    public String h5pay(AlipayBeanConfig alipayBean) throws AlipayApiException {
        //System.out.println(serverUrl+","+appId+","+privateKey+","+format+","+ charset+","+alipayPublicKey+","+signType);
        try{
            if (null==alipayConfig){
                logger.error("alipayConfig is null !");
                return "";
            }
            AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, alipayConfig.getAppId(), alipayConfig.getPrivateKey(), format, charset, alipayConfig.getAlipayPublicKey(), signType);
            // 2、设置请求参数
            AlipayTradeWapPayRequest alipayWapPayRequest = new AlipayTradeWapPayRequest();

            // 页面跳转同步通知页面路径
            alipayWapPayRequest.setReturnUrl(alipayConfig.getReturnUrl());
            // 服务器异步通知页面路径
            alipayWapPayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
            // 封装参数
            alipayWapPayRequest.setBizContent(JSON.toJSONString(alipayBean));
            // 3、请求支付宝进行付款，并获取支付结果
            String result = alipayClient.pageExecute(alipayWapPayRequest).getBody();
            // 返回付款信息
            //logger.debug(result);
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public String query(String out_trade_no) throws AlipayApiException {
        //System.out.println(serverUrl+","+appId+","+privateKey+","+format+","+ charset+","+alipayPublicKey+","+signType);
        try{
            AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, alipayConfig.getAppId(), alipayConfig.getPrivateKey(), format, charset, alipayConfig.getAlipayPublicKey(), signType);
            // 2、设置请求参数
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            //bizContent.put("out_trade_no", "20150320010101001");
            //bizContent.put("trade_no", "2014112611001004680073956707");
            bizContent.put("out_trade_no", out_trade_no);
            request.setBizContent(bizContent.toString());
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
            } else {
                System.out.println("调用失败");
            }
            return response.getBody();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";

    }

    /**
     * 说明：单笔转账到支付宝账户
     *
     * @param out_biz_no      商户转账唯一订单号
     * @param payee_type      收款方账户类型  (1、ALIPAY_USERID：支付宝账号对应的支付宝唯一用户号。以2088开头的16位纯数字组成。2、ALIPAY_LOGONID：支付宝登录号，支持邮箱和手机号格式。)
     * @param payee_account   收款方账户
     * @param amount          转账金额
     * @param payer_show_name 付款方姓名
     * @param payee_real_name 收款方真实姓名
     * @param remark          转账备注
     * @author ArLen
     * @time：2018年12月5日 上午10:14:35
     */
    public String transferAccounts(String out_biz_no, String payee_type, String payee_account, String amount, String payer_show_name, String payee_real_name, String remark) {
        //填写自己创建的app的对应参数
        //AlipayClient alipayClient = new DefaultAlipayClient("支付宝网关", "appid", "私钥", "json", "utf-8", "公钥","RSA2");
        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, alipayConfig.getAppId(), alipayConfig.getPrivateKey(), format, charset, alipayConfig.getAlipayPublicKey(), signType);
        AlipayFundTransToaccountTransferRequest transferAccounts_request = new AlipayFundTransToaccountTransferRequest();


        AlipayFundTransToaccountTransferModel model = new AlipayFundTransToaccountTransferModel();
        model.setOutBizNo(out_biz_no);
        model.setPayeeType(payee_type);
        model.setPayeeAccount(payee_account);
        model.setAmount(amount);
        model.setPayerShowName(payer_show_name);
        model.setPayeeRealName(payee_real_name);
        model.setRemark(remark);
        transferAccounts_request.setBizModel(model);
        try {
            AlipayFundTransToaccountTransferResponse response = alipayClient.execute(transferAccounts_request);
            if (response.isSuccess()) {
                System.out.println(response.getBody());
                return response.getBody();
            } else {
                System.out.println("调用失败");
                return null;
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 2021 9 27 新版本支付宝转帐
     *
     * @param out_biz_no 商家侧唯一订单号，由商家自定义。对于不同转账请求，商家需保证该订单号在自身系统唯一。
     * @param amount     订单总金额，单位为元，精确到小数点后两位
     * @param payerId    付款方 参与方的唯一标识--[为商家自己id]
     * @param payeeId    收款方 唯一标识
     * @param payeeType  收款方   参与方的标识类型，目前支持如下类型： 1、ALIPAY_USER_ID 支付宝的会员ID 2、ALIPAY_LOGON_ID：支付宝登录号，支持邮箱和手机号格式
     * @param title      转账业务的标题，用于在支付宝用户的账单里显示
     * @param remark     业务备注
     * @return
     * @throws AlipayApiException
     */
    public String UniTransferV2(String out_biz_no, String amount, String payerId, String payeeId, String payeeType, String title, String remark) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, alipayConfig.getAppId(), alipayConfig.getPrivateKey(), format, charset, alipayConfig.getAlipayPublicKey(), signType);

        AlipayFundTransUniTransferRequest request = new AlipayFundTransUniTransferRequest();
        AlipayFundTransUniTransferModel model = new AlipayFundTransUniTransferModel();
        model.setOutBizNo(out_biz_no);
        model.setTransAmount(amount);
        model.setProductCode("TRANS_ACCOUNT_NO_PWD");
        model.setOrderTitle(title);
        //付款方信息--商家
        Participant payerInfo = new Participant();
        payerInfo.setIdentity(payerId);
        payerInfo.setIdentityType("ALIPAY_USER_ID");

        //如果付款方信息的identity_type为BANKCARD_NO需传递该参数
        //        BankcardExtInfo bankcardExtInfoDJOmU = new BankcardExtInfo();
        //        bankcardExtInfoDJOmU.setInstName("招商银行");
        //        bankcardExtInfoDJOmU.setAccountType("1");
        //        payerInfo.setBankcardExtInfo(bankcardExtInfoDJOmU);
        model.setPayerInfo(payerInfo);
        //收款方信息
        Participant payeeInfo = new Participant();
        payeeInfo.setIdentity(payeeId);
        payeeInfo.setIdentityType(payeeType);
        //如果收款方信息的identity_type为BANKCARD_NO需传递该参数
        //        BankcardExtInfo bankcardExtInfoubBzp = new BankcardExtInfo();
        //        bankcardExtInfoubBzp.setInstName("招商银行");
        //        bankcardExtInfoubBzp.setAccountType("1");
        //        payeeInfo.setBankcardExtInfo(bankcardExtInfoubBzp);
        model.setPayeeInfo(payeeInfo);
        model.setRemark(remark);
        request.setBizModel(model);
        AlipayFundTransUniTransferResponse response = alipayClient.certificateExecute(request);
        System.out.println(response.getBody());
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return response.getBody();
    }
}
