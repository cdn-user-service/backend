/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.oss.cloud;


import io.ants.common.utils.ConfigConstant;
import io.ants.common.utils.Constant;
import io.ants.common.utils.SpringContextUtils;
import io.ants.modules.sys.service.SysConfigService;

/**
 * 文件上传Factory
 *
 * @author Mark sunlightcs@gmail.com
 */
public final class OSSFactory {
    private static SysConfigService sysConfigService;

    static {
        OSSFactory.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }

    public static AbstractCloudStorageService build(){
        //获取云存储配置信息
        CloudStorageConfig config = sysConfigService.getConfigObject(ConfigConstant.CLOUD_STORAGE_CONFIG_KEY, CloudStorageConfig.class);
        if(null!=config.getType()){
            if(config.getType() == Constant.CloudService.QINIU.getValue()){
                return new QiniuAbstractCloudStorageService(config);
            }else if(config.getType() == Constant.CloudService.ALIYUN.getValue()){
                return new AliyunAbstractCloudStorageService(config);
            }else if(config.getType() == Constant.CloudService.QCLOUD.getValue()){
                return new QcloudAbstractCloudStorageService(config);
            }
        }
        return new LocalStorageServiceAbstract();

    }

}
