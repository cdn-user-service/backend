package io.ants.modules.utils.config.fuiou;

import lombok.Data;

import java.io.Serializable;


@Data
public class FuiouConfig  implements Serializable {
    private static final long serialVersionUID = 1L;
    //---------------------------------------------------------------------------------------------------
    //------------------------------------用户参数------------------------------------------------------
    //---------------------------------------------------------------------------------------------------

    //机构私钥
    //public static final String ins_private_key ="MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJgAzD8fEvBHQTyxUEeK963mjziMWG7nxpi+pDMdtWiakc6xVhhbaipLaHo4wVI92A2wr3ptGQ1/YsASEHm3m2wGOpT2vrb2Ln/S7lz1ShjTKaT8U6rKgCdpQNHUuLhBQlpJer2mcYEzG/nGzcyalOCgXC/6CySiJCWJmPyR45bJAgMBAAECgYBHFfBvAKBBwIEQ2jeaDbKBIFcQcgoVa81jt5xgz178WXUg/awu3emLeBKXPh2i0YtN87hM/+J8fnt3KbuMwMItCsTD72XFXLM4FgzJ4555CUCXBf5/tcKpS2xT8qV8QDr8oLKA18sQxWp8BMPrNp0epmwun/gwgxoyQrJUB5YgZQJBAOiVXHiTnc3KwvIkdOEPmlfePFnkD4zzcv2UwTlHWgCyM/L8SCAFclXmSiJfKSZZS7o0kIeJJ6xe3Mf4/HSlhdMCQQCnTow+TnlEhDTPtWa+TUgzOys83Q/VLikqKmDzkWJ7I12+WX6AbxxEHLD+THn0JGrlvzTEIZyCe0sjQy4LzQNzAkEAr2SjfVJkuGJlrNENSwPHMugmvusbRwH3/38ET7udBdVdE6poga1Z0al+0njMwVypnNwy+eLWhkhrWmpLh3OjfQJAI3BV8JS6xzKh5SVtn/3Kv19XJ0tEIUnn2lCjvLQdAixZnQpj61ydxie1rggRBQ/5vLSlvq3H8zOelNeUF1fT1QJADNo+tkHVXLY9H2kdWFoYTvuLexHAgrsnHxONOlSA5hcVLd1B3p9utOt3QeDf6x2i1lqhTH2w8gzjvsnx13tWqg==";
    public   String ins_private_key;

    //机构公钥
    public static final String INS_PUBLIC_KEY="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCYAMw/HxLwR0E8sVBHivet5o84jFhu58aYvqQzHbVompHOsVYYW2oqS2h6OMFSPdgNsK96bRkNf2LAEhB5t5tsBjqU9r629i5/0u5c9UoY0ymk/FOqyoAnaUDR1Li4QUJaSXq9pnGBMxv5xs3MmpTgoFwv+gskoiQliZj8keOWyQIDAQAB";

    public  String order_prefix;

    //机构号
    public  String ins_cd ;

    //商户号
    public  String mchnt_cd ;

    //终端号
    public  String term_id ;

    //异步通知
    public  String notify_url ;




    //---------------------------------------------------------------------------------------------------
    //------------------------------------富友系统参数------------------------------------------------------
    //---------------------------------------------------------------------------------------------------
    public static String charset = "GBK";


    //富友公钥  用于验签
    //public static final String FY_PUBLIC_KEY ="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwpUExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB";
    public static final String FY_PUBLIC_KEY ="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCapX95RGC3pJKVVnPTlc0+zAz838tZtAH/54TADxv1hBLWlvbBPDrQWUmnLSvxrZDEQjIIQCNv9CBpWmoKSh2z6cMnn2fAfQypnm3ZRqpLkdgl9zV85J8TCprVw33/Jh194rnfiHgrHxPaOSaWc7GOVcmbsS2gvMyEj5d26HU3iQIDAQAB";


    //终端IP
    public static String term_ip = "127.0.0.1";


    //下单
    //public static String fuiou_21_url = "https://fundwx.fuiou.com/preCreate";//test
    public static String fuiou_21_url = "https://spay-mc.fuiou.com/preCreate";//生产1

    //扫码
    public static String fuiou_22_url = "https://fundwx.fuiou.com/micropay";
    //公众号/服务窗统一下单
    public static String fuiou_23_url = "https://fundwx.fuiou.com/wxPreCreate";
    //退款
    public static String fuiou_24_url = "https://fundwx.fuiou.com/commonRefund";
    //资金划拨信息
    //public static String fuiou_xx_url = "https://fundwx.fuiou.com/queryChnlPayAmt";
    //查询可提现资金
    public static String fuiou_27_url = "https://fundwx.fuiou.com/queryWithdrawAmt";
    //查询手续费
    public static String fuiou_28_url = "https://fundwx.fuiou.com/queryFeeAmt";
    //提现
    public static String fuiou_29_url = "https://fundwx.fuiou.com/withdraw";
    //查询
    public static String fuiou_30_url = "https://spay-mc.fuiou.com/commonQuery";



}
