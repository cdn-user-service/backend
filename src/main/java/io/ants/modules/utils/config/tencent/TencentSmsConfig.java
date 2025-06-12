package io.ants.modules.utils.config.tencent;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TencentSmsConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Data
    public class  SmsTempVo{
        private String name;
        private String title;
        private String id;
        private String temp;
        private Integer status=0;
    }

    private String secretid;

    private String secretkey;

    private String sdkappId;

    private String signname;

    private List<SmsTempVo> templateIds;

    public void updateTempTips(){
        if (null==this.templateIds || templateIds.isEmpty() ){
            return;
        }
        templateIds.forEach(item->{
            switch (item.getName()){
                case "code":
                    item.setTemp("您的验证码为：{1}，{2}分钟内有效。");
                    break;
                case "meal_out":
                    item.setTemp("您的套餐（ID: {1}，名称:{2}）已过期，系统已暂停您的服务。您可随时续费恢复服务。");
                    break;
                case "meal_expire":
                    item.setTemp("您的套餐（ID: {1}，名称:{2}）即期过期，为避免影响您的服务，请及时续费。");
                    break;
                case "flow_out":
                    item.setTemp("您的套餐（ID: {1}，名称:{2}）流量已用尽，系统已暂停您的服务。请及时购买升级包服务。");
                    break;
                case "flow_expire":
                    item.setTemp("您的套餐（ID: {1}，名称:{2}）流量余量不足，为避免影响您的服务，请及时购买升级包。");
                    break;
                default:
                    break;
            }
        });
    }
}
