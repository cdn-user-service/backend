package io.ants.modules.utils.factory;


import io.ants.common.utils.R;
import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.SmsBaoConfig;
import io.ants.modules.utils.config.SmsParamVo;
import io.ants.modules.utils.config.aliyun.AliyunSmsConfig;
import io.ants.modules.utils.config.tencent.TencentSmsConfig;
import io.ants.modules.utils.service.SmsBaoService;
import io.ants.modules.utils.service.aliyun.AliyunSmsService;
import io.ants.modules.utils.service.tencent.TentcentSmsService;
import org.apache.commons.lang.StringUtils;

public final class SmsFactory {
    private static SysConfigService sysConfigService;

    static {
        SmsFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }


    public static R sendSms(String mobile, String tempName, SmsParamVo vo){
        SysConfigEntity config=sysConfigService.getConfigByKey(ConfigConstantEnum.SMS_CONFIG_KEY.getConfKey());
        if (null!=config && 1==config.getStatus()){
            TencentSmsConfig tencentSmsConfig = sysConfigService.getConfigObject(ConfigConstantEnum.SMS_CONFIG_KEY.getConfKey(), TencentSmsConfig.class);
            if (StringUtils.isNotBlank(tencentSmsConfig.getSdkappId())){
                return new TentcentSmsService(tencentSmsConfig).sendSms(mobile,tempName,vo.getTencentParam());
            }
        }
        config=sysConfigService.getConfigByKey(ConfigConstantEnum.ALIYUN_SMS_CONFIG_KEY.getConfKey());
        if (null!=config && 1==config.getStatus()){
            AliyunSmsConfig aliConf=sysConfigService.getConfigObject(ConfigConstantEnum.ALIYUN_SMS_CONFIG_KEY.getConfKey(), AliyunSmsConfig.class);
            if (StringUtils.isNotBlank(aliConf.getAccessKeyId())){
                return new AliyunSmsService(aliConf).sendSms(mobile,tempName,vo.getAliParam());
            }
        }
        config=sysConfigService.getConfigByKey(ConfigConstantEnum.SMS_BAO.getConfKey());
        if (null!=config && 1==config.getStatus()){
            SmsBaoConfig smsBaoConf=sysConfigService.getConfigObject(ConfigConstantEnum.SMS_BAO.getConfKey(), SmsBaoConfig.class);
            if (StringUtils.isNotBlank(smsBaoConf.getUsername())){
                return new SmsBaoService(smsBaoConf).sendSms(mobile,tempName,vo.getSmsBaoParam());
            }
        }
        return R.error("未配置短信");
    }
}
