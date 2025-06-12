package io.ants.modules.utils.factory.wxchat;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.wechat.WXLoginConfig;
import io.ants.modules.utils.service.wechat.WXLoginService;

public class WXLoginFactory {
    private static SysConfigService sysConfigService;
    static{
        WXLoginFactory.sysConfigService= (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static WXLoginService build(){
        WXLoginConfig config = sysConfigService.getConfigObject(ConfigConstantEnum.WXLOGIN_CONFIG_KEY.getConfKey(), WXLoginConfig.class);
        return new WXLoginService(config);
    }
}
