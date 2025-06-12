package io.ants.modules.utils.factory;

import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.AllinpayConfig;
import io.ants.modules.utils.service.allinpay.AllinpaySybPayService;

public class AllinpayFactory {

    private static SysConfigService sysConfigService;

    static {
        AllinpayFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static AllinpaySybPayService build(){
        AllinpayConfig conf=sysConfigService.getConfigObject(ConfigConstantEnum.ALLINPAY_PAY_CONF.getConfKey(),AllinpayConfig.class );
        return new AllinpaySybPayService(conf);
    }
}
