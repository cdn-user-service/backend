package io.ants.modules.utils.factory;

import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.ZeroSslConfig;


public class ZeroSslFactory {
    private static SysConfigService sysConfigService;
    static {
        ZeroSslFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static  ZeroSslConfig build(){
        return sysConfigService.getConfigObject(ConfigConstantEnum.ZERO_SSL_CONFIG.getConfKey(),ZeroSslConfig.class );
    }
}
