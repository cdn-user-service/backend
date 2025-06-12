package io.ants.modules.utils.config;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MailConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Data
    public class tempVo{
        //{"protocol":"smtp","host":"smtp.gmail.com","username":"dnscdnvpn@gmail.com","password":"lqtmapalliobhhjg","port":"465","templates":[{"name":"code","title":"【antsCDN】邮件验证","content":"【antsCDN】您的动态验证码为 #code#","status":1},{"name":"meal_out","title":"【antsCDN】套餐过期提醒","content":"【antsCDN】您的域名 #domain#，套餐 #product# 已到期，系统已暂停您的服务。您可随时续费恢复服务！","status":1},{"name":"meal_expire","title":"【antsCDN】套餐即将过期提醒","content":"【antsCDN】您的域名 #domain#，套餐 #product# 即将到期，到期后将暂停相关业务，请及时续费！","status":1},{"name":"flow_out","title":"【antsCDN】流量已超限","content":"【antsCDN】您的域名 #domain#，套餐 #product# 流量已用尽，系统已暂停您的服务。请购买流量包恢复服务！","status":1},{"name":"flow_expire","title":"【antsCDN】流量即将超限","content":"【antsCDN】您的域名 #domain#，套餐 #product# 流量即将用尽，用尽后将暂停相关业务，请及时购买流量包！","status":1}],"id":31}
        private String name="";
        private String title="";
        private String content="";
        private Integer status =0;
    }

    //smtp.qq.com
    private String host;

    private String username;

    private String password;

    private String port;

    //smtp
    private String protocol;

    private List<tempVo> templates;

    //    //邮件主题
    //    private String title;
    //    //邮件内容
    //    private String content;



}
