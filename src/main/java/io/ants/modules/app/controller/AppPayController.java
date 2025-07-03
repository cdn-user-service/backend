package io.ants.modules.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.UserLog;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.HttpContextUtils;
import io.ants.common.utils.IPUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.dao.TbOrderDao;
import io.ants.modules.app.entity.TbOrderEntity;
import io.ants.modules.app.form.AlipaySubmitOrderForm;
import io.ants.modules.app.form.FuiouPayForm;
import io.ants.modules.app.form.PayBalanceForm;
import io.ants.modules.app.form.WechatPaySumbitOrderForm;
import io.ants.modules.sys.enums.PayStatusEnum;
import io.ants.modules.sys.service.PayService;
import io.ants.modules.utils.config.alipay.AlipayBeanConfig;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;

@RestController
@RequestMapping("/app/pay/")
@Tag(name = "支付 相关接口")
public class AppPayController {

    @Autowired
    private PayService payService;

    @Autowired
    private TbOrderDao orderDao;

    @PostMapping(value = "/alipay")
    @Operation(summary = "支付宝支付")
    public R alipay(@RequestBody AlipaySubmitOrderForm form) {
        ValidatorUtils.validateEntity(form);
        TbOrderEntity order = orderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number", form.getOutTradeNo())
                .eq("status", PayStatusEnum.PAY_NOT_PAY.getId())
                .last("limit 1"));
        if (null == order) {
            return R.error("订单号错误");
        }
        if (order.getCreateTime().before(DateUtils.addDateHours(new Date(), -24))) {
            order.setStatus(PayStatusEnum.PAY_OUT_TIME.getId());
            orderDao.updateById(order);
            return R.error("订单已超时关闭");
        }
        AlipayBeanConfig alipayBean = new AlipayBeanConfig();
        alipayBean.setOut_trade_no(form.getOutTradeNo());
        alipayBean.setSubject(form.getSubject());
        // 将分转为元为单位
        int amount = Integer.parseInt(form.getTotalAmount());
        double amountF = amount / 100.00;
        BigDecimal bg = new BigDecimal(amountF);
        double d3 = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        alipayBean.setTotal_amount(String.valueOf(d3));
        alipayBean.setBody(form.getBody());
        String alipayResult = payService.aliPay(alipayBean, form.getMode());
        if (StringUtils.isBlank(alipayResult)) {
            return R.error("申请支付接入失败！");
        }
        return R.ok().put("data", alipayResult);
    }

    @GetMapping("/alipayQuery")
    // @Operation(summary = "支付宝查询订单")
    public R alipayQuery(String outTradeNo) {
        return R.ok().put("data", payService.aliPayQuery(outTradeNo));
    }

    @RequestMapping(value = "/alipaynoticecallback")
    // @Operation(summary = "alipay Notice Callback")
    public String alipayCallback(HttpServletRequest request) {
        return payService.aliPayNoticeCallback(request);

    }

    @GetMapping(value = "/alipayeturncallback")
    // @Operation(summary = "alipay Return Callback")
    public String alipayReturn() {
        return "<script></script>";
    }

    @RequestMapping(value = "/stripe/callback")
    public String stripeCallBack(HttpServletRequest request, HttpServletResponse response) {
        // return StripePayFactory.build().callBackHandle(request,response);
        // return payService.stripeCallBackHandle(request,response);
        return "404";
    }

    @RequestMapping(value = "/allinpay/callback")
    public void allinpayCallBack(HttpServletRequest request, HttpServletResponse response) {

        payService.allinpayCallback(request, response);
    }

    @RequestMapping(value = "/tokenpay/callback")
    public String tokenPayCallback(HttpServletRequest request, HttpServletResponse response) {
        return payService.tokenPayCallback(request, response);
    }

    @RequestMapping(value = "/cccyun/callback")
    public String cccYunCallback(HttpServletRequest request, HttpServletResponse response) {
        return payService.cccYunCallback(request, response);
    }

    @PostMapping(value = "/wechatPay")
    @Operation(summary = "微信支付")
    public R wechatPay(@RequestBody WechatPaySumbitOrderForm form) {
        ValidatorUtils.validateEntity(form);
        TbOrderEntity order = orderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number", form.getOutTradeNo())
                .eq("status", PayStatusEnum.PAY_NOT_PAY.getId())
                .last("limit 1"));
        if (null == order) {
            return R.error("订单号错误");
        }
        if (order.getCreateTime().before(DateUtils.addDateHours(new Date(), -24))) {
            order.setStatus(PayStatusEnum.PAY_OUT_TIME.getId());
            orderDao.updateById(order);
            return R.error("订单已超时关闭");
        }
        if (StringUtils.isNotBlank(form.getMode())) {
            HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
            String qrCodeUrl = null;
            if ("native".equals(form.getMode())) {
                qrCodeUrl = payService.wxPayNativePay(request, form.getOutTradeNo(), form.getTotalAmount(),
                        form.getBody());
            } else if ("mweb".equals(form.getMode())) {
                qrCodeUrl = payService.wxPayMWebPay(request, form.getOutTradeNo(), form.getTotalAmount(),
                        form.getBody());
            }
            if (null != qrCodeUrl && qrCodeUrl.length() > 0) {
                return R.ok().put("data", qrCodeUrl);
            } else {
                return R.error("获取支付二维码失败");
            }
        }
        return R.error("数据错误,接入失败");
    }

    @RequestMapping(value = "/wechatPayCallback")
    // @Operation(summary = "wechat pay callback")
    public String wechatCallback(HttpServletRequest request, HttpServletResponse response) {
        return payService.wxPayCallback(request, response);

    }

    @PostMapping(value = "/fuiouPay")
    @Operation(summary = "富友 支付")
    public R fuiouPay(@RequestBody FuiouPayForm form) {
        ValidatorUtils.validateEntity(form);
        TbOrderEntity order = orderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number", form.getOutTradeNo())
                .eq("status", PayStatusEnum.PAY_NOT_PAY.getId())
                .last("limit 1"));
        if (null == order) {
            return R.error("订单号错误");
        }
        if (order.getCreateTime().before(DateUtils.addDateHours(new Date(), -24))) {
            order.setStatus(PayStatusEnum.PAY_OUT_TIME.getId());
            orderDao.updateById(order);
            return R.error("订单已超时关闭");
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IPUtils.getIpAddr(request);
        System.out.println("ip=" + ip);
        return R.ok().put("data",
                payService.fuiouPay(ip, form.getOutTradeNo(), form.getTotalAmount(), form.getBody(), form.getMode()));
    }

    @PostMapping(value = "/fuioupayCallback")
    // @Operation(summary = "富友支付 CALLBACK")
    public String fuiouCallback(@RequestBody String req) {
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        payService.fuiouPayCallBack(request, req);
        return "1";
    }

    @GetMapping("/fuioupayQuery")
    // @Operation(summary = "富友支付 查询订单")
    public R fuiouPayQuery(String order_no, String order_type) {
        if (null == order_no || null == order_type) {
            return R.error(403, "parat is error");
        }
        return R.ok().put("data", payService.fuiouPayQuery(order_no, order_type));
    }

    @Login
    @PostMapping("/balancePay")
    @Operation(summary = "余额支付")
    @UserLog("余额支付")
    public R balancePay(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody PayBalanceForm form) {
        form.setUserId(userId);
        TbOrderEntity order = orderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number", form.getSerialNumber())
                .eq("status", PayStatusEnum.PAY_NOT_PAY.getId())
                .last("limit 1"));
        if (null == order) {
            return R.error("订单号错误");
        }
        if (order.getCreateTime().before(DateUtils.addDateHours(new Date(), -24))) {
            order.setStatus(PayStatusEnum.PAY_OUT_TIME.getId());
            orderDao.updateById(order);
            return R.error("订单已超时关闭");
        }
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.FINANCE_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return payService.orderPayBalance(form);
    }

    @Login
    @GetMapping("/order/detail")
    @Operation(summary = "订单详情")
    public R order_detail(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam String SerialNumber) {
        return payService.getOrderStatus(userId, SerialNumber);
    }

}
