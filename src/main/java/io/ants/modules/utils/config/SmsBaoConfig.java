package io.ants.modules.utils.config;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class SmsBaoConfig implements Serializable {

    @Data
    public class SmsTempVo {
        //// {"name":"flow_out","title":"","id":"1276490","status":1},
        private String name;
        private String temp;
        private String content;
        private String id;
        private Integer status = 0;
    }

    @NotNull(message = "接口地址不能为空！")
    private String api_addr = "";

    @NotNull(message = "用户名不能为空")
    private String username = "";

    @NotNull(message = "api_key不能为空")
    private String api_key = "";

    private List<SmsTempVo> templateIds;

    public void updateTempTips() {
        if (null == this.templateIds || templateIds.isEmpty()) {
            return;
        }
        templateIds.forEach(item -> {
            switch (item.getName()) {
                case "code":
                    item.setTemp("您的验证码是：code，如非本人操作，请忽略本短信。");
                    break;
                case "meal_out":
                    item.setTemp("您的套餐[id][name]已过期，系统已暂停您的服务。您可随时续费恢复服务。");
                    break;
                case "meal_expire":
                    item.setTemp("您的套餐[id][name]即期过期，为避免影响您的服务，请及时续费。");
                    break;
                case "flow_out":
                    item.setTemp("您的套餐[id][name]流量已用尽，系统已暂停您的服务。请及时购买升级包服务。");
                    break;
                case "flow_expire":
                    item.setTemp("您的套餐[id][name]流量余量不足，为避免影响您的服务，请及时购买升级包。");
                    break;
                default:
                    break;
            }
        });
    }
}
