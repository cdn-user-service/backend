package io.ants.modules.sys.service;

import io.ants.common.utils.R;
import io.ants.modules.app.form.PayBalanceForm;
import io.ants.modules.utils.config.alipay.AlipayBeanConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Administrator
 */
public interface PayService {

    R orderPayBalance(PayBalanceForm from);

    void operateRecord();

    R getOrderStatus(Long useId, String SerialNumber);

    R adminRecharge(Long useId,Integer amount,String remark);

        
    String aliPay(AlipayBeanConfig alipayBean, String mode) ;
    String aliPayQuery(String out_trade_no) ;
    String aliPayNoticeCallback(HttpServletRequest request);



    String wxPayNativePay(HttpServletRequest request, String outTradeNo, String totalAmount, String body);

    String wxPayMWebPay(HttpServletRequest request, String outTradeNo, String totalAmount, String body);

    String wxPayCallback(HttpServletRequest request, HttpServletResponse response);

    Map<String,String> fuiouPay(String ip, String mchnt_order_no, String order_amt, String goods_des, String order_type) ;

    void fuiouPayCallBack(HttpServletRequest request,String req) ;

    Map<String,String> fuiouPayQuery(String order_no,String order_type) ;


    void payResultSub(String SerialNumber, Integer payPaid, String out_trade_id, Integer payType, String payMsg);


    //String stripeCallBackHandle(HttpServletRequest request, HttpServletResponse response);

    String tokenPayCallback(HttpServletRequest request, HttpServletResponse response);

    String cccYunCallback(HttpServletRequest request, HttpServletResponse response);

    void allinpayCallback(HttpServletRequest request, HttpServletResponse response);
}
