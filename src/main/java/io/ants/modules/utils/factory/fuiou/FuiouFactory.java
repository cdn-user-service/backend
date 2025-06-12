package io.ants.modules.utils.factory.fuiou;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.fuiou.FuiouConfig;
import io.ants.modules.utils.service.fuiou.FuiouService;

public class FuiouFactory {
    private static SysConfigService sysConfigService;

    static {
        FuiouFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static FuiouService build(){
        FuiouConfig config = sysConfigService.getConfigObject(ConfigConstantEnum.FUIOU_CONFIG_KEY.getConfKey(), FuiouConfig.class);
        return  new FuiouService(config);
    }
}
