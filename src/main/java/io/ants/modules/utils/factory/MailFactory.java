package io.ants.modules.utils.factory;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.MailConfig;
import io.ants.modules.utils.service.MailService;

public final class MailFactory {
    private static SysConfigService sysConfigService;
    static {
        MailFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static MailService build(){
        MailConfig mailConfig= sysConfigService.getConfigObject(ConfigConstantEnum.MAIL_CONFIG_KEY.getConfKey(), MailConfig.class);
        return new MailService(mailConfig);
    }
}
