package io.ants.modules.utils.factory.alipay;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.alipay.AlipayConfig;

public class AlipayFactory {
    private static SysConfigService sysConfigService;
    static{
        AlipayFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static AlipayConfig build(){
        AlipayConfig alipayConfig = sysConfigService.getConfigObject(ConfigConstantEnum.ALIPAY_CONFIG_KEY.getConfKey(), AlipayConfig.class);
        return alipayConfig;
    }
}
