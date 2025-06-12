package io.ants.modules.utils.factory.tencent;


import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.tencent.TencentUserCertifyConfig;
import io.ants.modules.utils.service.tencent.TencentUserCertifyService;

public class TencentUserCertifyFactory {
    private static SysConfigService sysConfigService;


    static {
        TencentUserCertifyFactory.sysConfigService= (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static TencentUserCertifyService build(){
        TencentUserCertifyConfig config = sysConfigService.getConfigObject(ConfigConstantEnum.TENCENTUSERCERTIFY_CONFIG_KEY.getConfKey(), TencentUserCertifyConfig.class);
        return new TencentUserCertifyService(config);
    }

}
