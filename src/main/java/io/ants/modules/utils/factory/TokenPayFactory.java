package io.ants.modules.utils.factory;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.TokenPayConfig;
import io.ants.modules.utils.service.TokenPayService;

public class TokenPayFactory {
    private static SysConfigService sysConfigService;

    static {
        TokenPayFactory.sysConfigService= (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static TokenPayService build(){
        TokenPayConfig config = sysConfigService.getConfigObject(ConfigConstantEnum.TOKEN_PAY_CONF.getConfKey(), TokenPayConfig.class);
        return new TokenPayService(config);
    }

}
