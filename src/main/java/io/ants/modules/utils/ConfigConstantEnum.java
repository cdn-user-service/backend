package io.ants.modules.utils;

import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.modules.oss.cloud.CloudStorageConfig;
import io.ants.modules.utils.config.alipay.AlipayConfig;
import io.ants.modules.utils.config.alipay.AlipayUserCertifyConfig;
import io.ants.modules.utils.config.aliyun.AliyunSmsConfig;
import io.ants.modules.utils.config.cccpay.CccyunPayConf;
import io.ants.modules.utils.config.fuiou.FuiouConfig;
import io.ants.modules.utils.config.tencent.TencentSmsConfig;
import io.ants.modules.utils.config.tencent.TencentUserCertifyConfig;
import io.ants.modules.utils.config.wechat.ConfigUtil;
import io.ants.modules.utils.config.wechat.WXLoginConfig;
import io.ants.modules.utils.config.*;

import java.lang.reflect.Field;
import java.util.*;

public enum ConfigConstantEnum {
    WEB_SITE_CONFIG_KEY(1,"WEB_SITE_CONFIG_KEY",null,"站点","站点基本配置",true),
    USER_AUTH_ID(2,"USER_AUTH_ID",null,"站点","实名配置选择",false),
    PAY_IDS(3,"PAY_IDS",null,"站点","支付配置选择",true),
    CLOUD_STORAGE_CONFIG_KEY(4,"CLOUD_STORAGE_CONFIG_KEY", CloudStorageConfig.class,"云存储","云存储",false),
    MAIL_CONFIG_KEY(5,"MAIL_CONFIG_KEY", MailConfig.class,"通信","邮件配置",true),
    SMS_CONFIG_KEY(6,"SMS_CONFIG_KEY", TencentSmsConfig.class,"通信","腾讯云短信配置",true),
    ALIYUN_SMS_CONFIG_KEY(7,"ALIYUN_SMS_CONFIG_KEY", AliyunSmsConfig.class,"通信","阿里云短信配置",true),
    SMS_BAO(8,"SMS_BAO", SmsBaoConfig.class,"通信","短信宝短信配置",true),
    WXLOGIN_CONFIG_KEY(9,"WXLOGIN_CONFIG_KEY", WXLoginConfig.class,"第三方登录","微信扫码登录注册",true),
    ALIPAYUSERCERTIFY_CONFIG_KEY(10,"ALIPAYUSERCERTIFY_CONFIG_KEY", AlipayUserCertifyConfig.class,"实名","支付宝实名认证配置",true),
    TENCENTUSERCERTIFY_CONFIG_KEY(11,"TENCENTUSERCERTIFY_CONFIG_KEY", TencentUserCertifyConfig.class,"实名","微信实名认证配置",true),
    ALIPAY_CONFIG_KEY(12,"ALIPAY_CONFIG_KEY", AlipayConfig.class,"支付","支付宝支付配置",true),
    FUIOU_CONFIG_KEY(13,"FUIOU_CONFIG_KEY", FuiouConfig.class,"支付","富友支付配置",false),
    WXPAY_CONFIG_KEY(14,"WXPAY_CONFIG_KEY", ConfigUtil.class,"支付","微信支付配置",true),
    WEB_AGREEMENT(15,"WEB_AGREEMENT", null,"站点","同意协议",true),
    CDN_NODECHECK_KEY(16,"CDN_NODECHECK_KEY", NodeCheckConfig.class,"系统","cdn节点检测配置",true),
    CDN_PUBLIC_KEY(17,"CDN_PUBLIC_KEY", PublicCdnConfig.class,"系统","cdn系统公共配置",false),
    WEB_DIR_CONF(18,"WEB_DIR_CONF", WebDirConfig.class,"系统","Web站点根目录配置",true),
    SYS_APP_LOGIN(19,"SYS_APP_LOGIN", SysLoginKeyConfig.class, "系统接口授权", "系统接口授权",true),
    //STRIPE_PAY_CONF("STRIPE_PAY_CONF",StripeConfig.class,"支付","stripe支付配置",true),
    TOKEN_PAY_CONF(20,"TOKEN_PAY_CONF",TokenPayConfig.class,"支付","tokenPay支付配置",true),
    CCCYUN_PAY_CONF(21,"CCCYUN_PAY_CONF", CccyunPayConf.class,"支付","彩虹易支付API配置",true),
    ALLINPAY_PAY_CONF(22,"ALLINPAY_PAY_CONF", AllinpayConfig.class,"支付","通联支付API配置",true),
    DNS_USER_API_ROOT_URI(23,"DNS_USER_API_ROOT_URI", DnsApiRootUriConf.class,"站点交互","站点",true),
    ZERO_SSL_CONFIG(24,"ZERO_SSL_CONFIG",ZeroSslConfig.class,"zeroSsl","ssl证书",true),
    ;
    private final int cid;
    private final String confKey;
    private final Object paramClass;
    private final String remark;
    private final String name;
    private final boolean status;

     ConfigConstantEnum(int cid,String confKey,Object paramClass,String remark,String name,boolean status){
         this.cid=cid;
          this.confKey=confKey;
          this.paramClass=paramClass;
          this.remark=remark;
          this.name=name;
          this.status=status;
     }

    public int getCid() {
        return cid;
    }

    public String getConfKey() {
        return confKey;
    }

    public Object getParamClass() {
        return paramClass;
    }

    public String getRemark() {
        return remark;
    }

    public String getName() {
        return name;
    }

    public boolean getStatus(){
         return status;
    }

    public static  List<String> getParamClass(String confKey){
        for (ConfigConstantEnum item:ConfigConstantEnum.values()){
            if(item.getConfKey().equals(confKey)){
                if(null!=item.getParamClass()){
                    try{
                        Class c=(Class) item.getParamClass();
                        Field[] allFields= c.getDeclaredFields();
                        List<String> confing_names= new ArrayList<>();
                        for (Field field : allFields){
                            confing_names.add(field.getName());
                        }
                        return confing_names;
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }
        return null;
    }

    public static int getConfIdByName(String confKey){
        for (ConfigConstantEnum item:ConfigConstantEnum.values()){
            if(item.getConfKey().equals(confKey)){
                return item.getCid();
            }
        }
         return 0;
    }

    public static List<String> isEnableKeys(){
        List<String> list=new ArrayList<>();
        for (ConfigConstantEnum item:ConfigConstantEnum.values()){
            if (Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).contains(item.getCid())){
                list.add(item.getConfKey());
            }
        }
        return list;
    }

    public static List<String> getOtherConfKey(){
         String[] ret={"STRIPE_PAY_CONF","TOKEN_PAY_CONF"};
        return Arrays.asList(ret);
    }

    public static List<String> getAllConfKey(String remark){
        List<String> list=new ArrayList<>();
        for (ConfigConstantEnum item:ConfigConstantEnum.values()){
            if (!Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).contains(item.getCid())){
               continue;
            }
            if(null==remark || remark.equals(item.getRemark())){
                if (getOtherConfKey().contains(item.getConfKey()) ){
                    if (QuerySysAuth.getShowOtherSysConf()){
                        list.add(item.getConfKey());
                    }
                }else {
                    list.add(item.getConfKey());
                }
            }
        }
        return list;
    }

    public static List<Map> getAll(String remark){
        List<Map> list=new ArrayList<>();
        for (ConfigConstantEnum item:ConfigConstantEnum.values()){
            if(null==remark || remark.equals(item.getRemark())){
                Map map=new HashMap();
                map.put(item.getConfKey(),"");
                list.add(map);
            }
        }
        return list;
    }

    public static void main(String[] args) {
        int cid=ConfigConstantEnum.getConfIdByName("WXLOGIN_CONFIG_KEY");
        System.out.println(cid);
        System.out.println(Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS));
        System.out.println(QuerySysAuth.EXISTS_CONFIG_IDS.length);
        System.out.println(Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).size());
        System.out.println(Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).contains(cid));
        if (!Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).contains(cid)){
            System.out.println("xxx");
        }
        System.out.println(111);
    }
}
