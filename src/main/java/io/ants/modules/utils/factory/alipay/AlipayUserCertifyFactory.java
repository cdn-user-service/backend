package io.ants.modules.utils.factory.alipay;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.alipay.AlipayUserCertifyConfig;
import io.ants.modules.utils.service.alipay.AlipayUserCertifyService;

public class AlipayUserCertifyFactory {

    private static SysConfigService sysConfigService;

    static{
        AlipayUserCertifyFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static AlipayUserCertifyService build(){
        AlipayUserCertifyConfig config = sysConfigService.getConfigObject(ConfigConstantEnum.ALIPAYUSERCERTIFY_CONFIG_KEY.getConfKey(), AlipayUserCertifyConfig.class);
        return new AlipayUserCertifyService(config);
    }
}
