package io.ants.modules.utils.factory.wxchat;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.wechat.ConfigUtil;

public class WXPayFactory {
    private static SysConfigService sysConfigService;
    static{
        WXPayFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static ConfigUtil build() throws Exception {
        ConfigUtil config = sysConfigService.getConfigObject(ConfigConstantEnum.WXPAY_CONFIG_KEY.getConfKey(), ConfigUtil.class);
        return config;
    }
}
