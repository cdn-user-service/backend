package io.ants.modules.utils.factory;

import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.cccpay.CccyunPayConf;
import io.ants.modules.utils.service.CccyunPayService;

public class CccyunPayFactory {

    private static SysConfigService sysConfigService;

    static {
        CccyunPayFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static CccyunPayService build(){
        CccyunPayConf conf=sysConfigService.getConfigObject(ConfigConstantEnum.CCCYUN_PAY_CONF.getConfKey(),CccyunPayConf.class );
        return new CccyunPayService(conf);
    }

}
