package io.ants.modules.utils.service;

import io.ants.common.utils.R;
import io.ants.modules.utils.config.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;

import java.util.Properties;

public class MailService   {
    final private Logger logger = LoggerFactory.getLogger(getClass());

    private static JavaMailSenderImpl mailSender;

    MailConfig config;

    public MailService(MailConfig config){
        //logger.debug(config.toString());
        this.config = config;
        mailSender =  getMailSender(config.getHost(),config.getUsername(),config.getPassword(),config.getPort());

    }



    private  JavaMailSenderImpl getMailSender(String host,String username,String password,String port) {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        //链接服务器
        javaMailSender.setHost(host);
        //账号
        javaMailSender.setUsername(username);
        //密码
        javaMailSender.setPassword(password);
        javaMailSender.setDefaultEncoding("UTF-8");

        Properties properties = new Properties();
        //properties.setProperty("mail.debug", "true");//启用调试
        //设置链接超时
        properties.setProperty("mail.smtp.timeout", "30000");


        //开启认证
        if ( "smtp.office365.com".equals(host) || "smtp.gmail.com".equals(host) ){
            //office365 20230831 pass 需要登录再次验证手机号，解决垃圾邮件限制
            //gmail 20230831 pass pwd 使用的应用专用密码
            //设置通过ssl协议使用465端口发送、使用默认端口（25）时下面三行不需要
            properties.setProperty("mail.smtp.port","587");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.starttls.enable", "true");
        }else if("smtp.exmail.qq.com".equals(host) || "smtp.qq.com".equals(host) ) {
            // 腾讯邮箱可以通过
            //设置通过ssl协议使用465端口发送、使用默认端口（25）时下面三行不需要
            properties.setProperty("mail.smtp.port","465");
            //设置ssl端口
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.socketFactory.port", "465");
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.starttls.required", "true");

            // properties.setProperty("mail.smtp.ssl.enable", "false");
            properties.setProperty("mail.smtp.ssl.enable", "false");
            properties.setProperty("mail.imap.ssl.socketFactory.fallback", "false");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            //使用SSL，企业邮箱必需！
        }else if("smtp.qiye.aliyun.com".equals(host) ||"smtp.aliyun.com".equals(host)){
            //20230831 smtp.aliyun.com 测试通过
            properties.setProperty("mail.smtp.port","465");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.imap.ssl.socketFactory.fallback", "true");
            properties.setProperty("mail.smtp.ssl.enable", "true");
        }else {
            properties.setProperty("mail.smtp.port",port);
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.starttls.enable", "true");
        }
        javaMailSender.setJavaMailProperties(properties);
        return javaMailSender;
    }

    public R sendEmail(String sendTo, String title, String content)  {
        String eMsg="";
        try{
            MimeMessage msg = mailSender.createMimeMessage();
            //创建MimeMessageHelper对象，处理MimeMessage的辅助类
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            //使用辅助类MimeMessage设定参数
            helper.setFrom(mailSender.getUsername());
            helper.setTo(sendTo);
            helper.setSubject(title);
            helper.setText(content, true);
            mailSender.send(msg);
            //logger.info(sendTo+"-->"+content);
            return  R.ok();
        }catch (Exception e){
            eMsg=e.getMessage();
            logger.error("send email error:"+e);
        }
        return R.error(eMsg);
    }


    public static void main(String[] args) {
        MailConfig mailConfig=new MailConfig();
        mailConfig.setHost("smtp.aliyun.com");
        mailConfig.setPort("465");
        mailConfig.setUsername("xx@aliyun.com");
        mailConfig.setPassword("xxx");
        MailService m=new MailService(mailConfig);
        m.sendEmail("331434376@qq.com","reg mail verify","[cdn]you verify code is [ac712e]");
        System.out.println("xxx");
    }

}
