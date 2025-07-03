package io.ants.modules.utils.service.fuiou;

import io.ants.modules.utils.config.fuiou.FuiouConfig;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class FuiouService {

    static FuiouConfig fuiouConfig;

    public FuiouService() {
    }

    public FuiouService(FuiouConfig fuiouConfig) {
        FuiouService.fuiouConfig = fuiouConfig;
    }

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateSalt(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public FuiouConfig getFuiouConfig() {
        return fuiouConfig;
    }

    public Map<String, String> buildFuiou21() {
        Map<String, String> map = new HashMap<>();

        map.put("version", "1.0");
        // map.put("ins_cd", fuiouConfig.ins_cd);
        map.put("ins_cd", fuiouConfig.ins_cd);
        // map.put("mchnt_cd", Const.mchnt_cd);
        map.put("mchnt_cd", fuiouConfig.mchnt_cd);
        // map.put("term_id", Const.term_id);
        map.put("term_id", fuiouConfig.term_id);
        map.put("random_str", generateSalt(16));
        map.put("sign", "");
        map.put("order_type", "ALIPAY");
        map.put("goods_des", "goods_des--user-def");
        map.put("goods_detail", "");
        map.put("addn_inf", "");
        SimpleDateFormat sdf_no = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Calendar calendar = Calendar.getInstance();
        map.put("mchnt_order_no",
                fuiouConfig.order_prefix + sdf_no.format(calendar.getTime()) + (int) (RANDOM.nextDouble() * 100000));
        map.put("curr_type", "");
        map.put("order_amt", "1");
        map.put("term_ip", FuiouConfig.term_ip);
        SimpleDateFormat sdf_ts = new SimpleDateFormat("yyyyMMddHHmmss");
        map.put("txn_begin_ts", sdf_ts.format(calendar.getTime()));
        map.put("goods_tag", "");
        map.put("notify_url", fuiouConfig.notify_url);
        map.put("reserved_sub_appid", "");
        map.put("reserved_limit_pay", "");

        return map;
    }

    public Map<String, String> buildFuiou22() {
        Map<String, String> map = new HashMap<>();

        map.put("version", "1");
        map.put("ins_cd", fuiouConfig.ins_cd);
        map.put("mchnt_cd", fuiouConfig.mchnt_cd);
        map.put("term_id", "12345678");
        map.put("random_str", generateSalt(16));
        map.put("sign", "");
        map.put("order_type", "ALIPAY");
        map.put("goods_des", "卡盟测试");
        map.put("goods_detail", "");
        map.put("addn_inf", "");
        SimpleDateFormat sdf_no = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Calendar calendar = Calendar.getInstance();
        map.put("mchnt_order_no", sdf_no.format(calendar.getTime()) + (int) (RANDOM.nextDouble() * 100000));
        map.put("curr_type", "");
        map.put("order_amt", "1");
        map.put("term_ip", FuiouConfig.term_ip);
        SimpleDateFormat sdf_ts = new SimpleDateFormat("yyyyMMddHHmmss");
        map.put("txn_begin_ts", sdf_ts.format(calendar.getTime()));
        map.put("goods_tag", "");
        map.put("auth_code", "289387279140768146");
        map.put("sence", "1");
        map.put("reserved_sub_appid", "");
        map.put("reserved_limit_pay", "");

        return map;
    }

    public Map<String, String> buildFuiou23() {
        Map<String, String> map = new HashMap<>();

        map.put("version", "1.0");
        map.put("ins_cd", fuiouConfig.ins_cd);
        map.put("mchnt_cd", "0001210F0976403");// 0001210F0976403富友商户号服务商模式0001210F0976403
        map.put("term_id", "02020208");
        map.put("random_str", generateSalt(16));
        map.put("sign", "");
        map.put("goods_des", "卡盟测试");
        map.put("goods_detail", "");
        map.put("goods_tag", "");
        map.put("product_id", "");
        map.put("addn_inf", "");
        SimpleDateFormat sdf_no = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Calendar calendar = Calendar.getInstance();
        map.put("mchnt_order_no", sdf_no.format(calendar.getTime()) + (int) (RANDOM.nextDouble() * 100000));
        map.put("curr_type", "");
        map.put("order_amt", "1");
        map.put("term_ip", FuiouConfig.term_ip);
        SimpleDateFormat sdf_ts = new SimpleDateFormat("yyyyMMddHHmmss");
        map.put("txn_begin_ts", sdf_ts.format(calendar.getTime()));
        map.put("notify_url", fuiouConfig.notify_url);
        map.put("limit_pay", "");
        map.put("trade_type", "JSAPI");// 公众号支付
        // map.put("trade_type","FWC");//支付宝服务窗
        map.put("openid", ""); // 富友公众号 ooIeqs5VwPJnDUYfLweOKcR5AxpE
        map.put("sub_openid", "osgI-t3iTLkEdGhhwTwyYy_QiqFM");// 服务窗时填buyer_id的值 公众号的osgI-t3iTLkEdGhhwTwyYy_QiqFM
        map.put("sub_appid", "wx04bdf63c774e12ce");// 公众号的 wx04bdf63c774e12ce
        map.put("reserved_fy_term_id", "");
        map.put("reserved_expire_minute", "0");
        map.put("reserved_user_creid ", "");
        map.put("reserved_user_truename", "");
        map.put("reserved_user_mobile", "");
        return map;
    }

}
