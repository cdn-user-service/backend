package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class MailTemplateVo {
//    "name": "flow_expire",
//		"title": "【antsCDN】流量即将超限",
//		"content": "【antsCDN】您的域名 #domain#，套餐 #product# 流量即将用尽，用尽后将暂停相关业务，请及时购买流量包！",
//		"status": 1

    private String name="";
    private String title="";
    private String content="";
    private Integer status=0;
}
