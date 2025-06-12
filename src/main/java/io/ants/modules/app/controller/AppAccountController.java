package io.ants.modules.app.controller;


import cn.hutool.core.util.RandomUtil;
import io.ants.common.annotation.UserLog;
import io.ants.common.utils.GoogleAuthUtils;
import io.ants.common.utils.R;
import io.ants.common.utils.RedisUtils;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.annotation.LoginUser;
import io.ants.modules.app.entity.CdnLoginWhiteIpEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.*;
import io.ants.modules.app.service.AppCaptchaService;
import io.ants.modules.app.service.CdnLoginWhiteIpService;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.utils.factory.wxchat.WXLoginFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * APP登录授权
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/app/account/")
@Api(tags = "APP 账号 接口")
public class AppAccountController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TbUserService userService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private CdnLoginWhiteIpService whiteIpService;

    @Autowired
    private AppCaptchaService appCaptchaService;


    //测试环境为TRUE 不验证验证码


    /**
     * username 登录
     */
    @PostMapping("/login")
    @ApiOperation("登录")
    public R byUsernameLogin(@RequestBody LoginForm form) {
       return  userService.userLogin(form);
    }

    @GetMapping("/access/login")
    @ApiOperation("第三方（dns）使用access登录CDN")
    public R access_token_login(@RequestParam String access_token){
        return  userService.loginByAccessToken(access_token);
    }

    @GetMapping("/kaptcha.jpg")
    @ApiOperation(value = "获取验证码",produces = "application/octet-stream")
    public void captcha(HttpServletResponse response, String uuid)throws IOException{
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");
        //获取图片验证码
        BufferedImage image = appCaptchaService.getCaptcha(uuid);
        if (null==image){
            logger.error("image get fail");
            return;
        }
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        IOUtils.closeQuietly(out);
    }




    @PostMapping("/regist")
    @ApiOperation("注册")
    public R resister(@RequestBody RegisterForm form){
        return userService.userRegister(form);
    }


    @PostMapping("/api/register")
    public R noCodeRegister(@RequestBody RegisterForm form){
        return userService.userRegisterWithNoVerifyCode(form);

    }





    @GetMapping("/sendmail/code")
    @ApiOperation("发送邮件验证码")
    public  R sendmail(@RequestParam String mail){

        if(StringUtils.isBlank(mail) || !mail.contains("@")){
            return  R.error("输入有误");
        }
        return userService.sendVerifyByEmailCode(mail);

    }


    @GetMapping("/sendsms/code")
    @ApiOperation("发送短信验证码")
    public  R sendSms(@RequestParam String mobile, String uuid, String captcha_code){

        return userService.sendVerifyBySmsCode(mobile, uuid, captcha_code);


    }

    @PostMapping("/forget/password")
    @ApiOperation("忘记密码")
    public  R forget(@RequestBody PasswordForgetForm form){
        return userService.forgetPassWord(form);
    }


    @Login
    @PostMapping("/update/cdn_access_token")
    @ApiOperation("更新CDN ACCESS TOKEN")
    public R updateCdnAccessToken(@ApiIgnore @LoginUser TbUserEntity user, @RequestBody Map param){
          return R.error("empty access");
    }

    @Login
    @PostMapping("/update/dns_access_token")
    @ApiOperation("绑定dns ACCESS TOKEN")
    public R bindDnsAccessToken(@ApiIgnore @LoginUser TbUserEntity user, @RequestBody BindDnsForm param){

        return userService.bindDnsAccessToken(user,param);

    }

    @Login
    @GetMapping("/userinfo")
    @ApiOperation("获取用户信息")
    public  R userInfo(@ApiIgnore @LoginUser TbUserEntity user) {
        return userService.getUserInfoDetail(user);

    }

    @Login
    @PostMapping("/user-module/update")
    @ApiOperation("更新用户信息")
    public  R updateUserModule(@ApiIgnore @LoginUser TbUserEntity user,@RequestBody UpdateAppUserForm form) {
        return userService.updateUserModules(user,form);

    }


    @Login
    @PostMapping("/message/list")
    @ApiOperation("用户消息列表")
    public  R userMessageList( @ApiIgnore @RequestAttribute("userId") Integer userId, PageForm form){
        return userService.getUserMessageList(userId,form);
    }

    @Login
    @PostMapping("/message/delete")
    @ApiOperation("删除消息")
    public  R userMessageDelete( @ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody Map param){

        return userService.deleteUserMessage(userId,param);

    }

    @Login
    @GetMapping("/message/info")
    @ApiOperation("用户查看消息")
    public  R userMessageInfo( @ApiIgnore @RequestAttribute("userId") Integer userId, String id){
        return userService.viewUserMessageDetail(userId,id);

    }


    @Login
    @PostMapping("/changepwd")
    @ApiOperation("修改密码")
    @UserLog("修改密码")
    public  R userChangePassWord(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody ModifyPwdForm form){
        return userService.userChangePassWord(userId,form);
    }


    @Login
    @PostMapping("/bind")
    @ApiOperation("邮箱|手机 绑定")
    public  R bindMailOrMail(@ApiIgnore @LoginUser TbUserEntity user,@RequestBody BindForm form){
        return userService.bindMailOrMobile(user,form);

    }



    @GetMapping("/login/wechat/qrcode")
    @ApiOperation("获取获取登录URL")
    public  R wechat_login()  {
        try
        {
            String wechatLoginUrl= WXLoginFactory.build().wechatLogin_req_url("0666666",null);
            return R.ok().put("data",wechatLoginUrl);
        }catch (Exception e){
            e.printStackTrace();
        }
        return R.error("参数错误");
    }

    @Login
    @GetMapping("/bind/wechat/qrcode_v2")
    @ApiOperation("获取绑定WECHAT URL v2")
    public  R wechat_bind_v2(@ApiIgnore @RequestAttribute("userId") Integer userId,String redirect_url){
        String eMsg="";
        try{
            String appid= WXLoginFactory.build().wechatLogin_appid();
            String RandomStr=String.valueOf( RandomUtil.randomInt(1000000,9999999));
            redisUtils.set(RandomStr,userId,600);
            Map<String,String>map=new HashMap<>();
            map.put("state",RandomStr);
            map.put("appid",appid);
            return R.ok().put("data",map);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }
    @GetMapping("/login/wechat/qrcode_v2")
    @ApiOperation("获取获取登录URL_data v2")
    public  R wechatLoginV2( )  {
        String eMsg="";
        try{
            String appid= WXLoginFactory.build().wechatLogin_appid();
            String state="0666666";
            Map<String,String>map=new HashMap<>();
            map.put("state",state);
            map.put("appid",appid);
            return R.ok().put("data",map);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }

    @PostMapping("/google-auth/login")
    @ApiOperation("使用GOOGLE-AUTH登录")
    public R googleAuthLogin(@RequestBody LoginByGoogleAuthForm form) {

        return userService.loginByGoogleAuthCode(form);
    }

    @Login
    @PostMapping("/google-auth/bind")
    @ApiOperation("GOOGLE-AUTH绑定")
    public R googleAuthBind(@ApiIgnore @RequestAttribute("userId") Integer userId, @RequestBody GoogleAuthBindForm form) {
        return userService.bindByGoogleAuth(userId, form);
    }

    @Login
    @GetMapping("/google-auth/qrcode/view")
    @ApiOperation("获取Google-auth CODE")
    public R viewGoogleAuthData(@ApiIgnore @RequestAttribute("userId") Long userId){
        return R.ok().put("data",GoogleAuthUtils.buildOneData(userId));
    }

    @Login
    @GetMapping("/google-auth/status")
    @ApiOperation("")
    public R statusWithGoogleAuth(@ApiIgnore @RequestAttribute("userId") Long userId,int status){
        return userService.statusWithGoogleAuthSave(userId,status);
    }

    @Login
    @GetMapping("/bind/wechat/qrcode")
    @ApiOperation("获取绑定WECHAT URL")
    public  R wechat_bind(@ApiIgnore @RequestAttribute("userId") Integer userId){
        return userService.bindByWechat(userId);
    }




    @GetMapping("/login/wechat/regist")
    @ApiOperation("微信 -- 登录 --注册")
    public   R WechatRegister(String nickname,String openid){
        return userService.wechatRegisterAndLogin( nickname, openid);

    }

    @PostMapping("/login/wechat/bind")
    @ApiOperation("微信--登录--绑定")
    public  R wechat_login_bind(@RequestBody WechatBindForm form){

        return userService.loginByWechatOrRegister(form);


    }


    @GetMapping("/login/wechat/callback_v2")
    @ApiOperation("--wechat --登录回调v2")
    public  R WechatLoginCallbackV2(String code,String state){
        return userService.wechatLoginCallbackV2(code,state);

    }


    @Login
    @GetMapping("/user/password/set")
    @ApiOperation("设置密码（密码为空时可设置）")
    public  R set_pwd(@ApiIgnore @RequestAttribute("userId") Long userId,String pwd){
        return userService.nullPasswordSetPassword(userId,pwd);

    }

    @Login
    @GetMapping("/user/propertyPayPassword/set")
    @ApiOperation("设置支付密码（密码为空时可设置）")
    public  R set_propertyPayPassword(@ApiIgnore @RequestAttribute("userId") Long userId,String pwd){
        return userService.nullPropertyPayPasswordSetPassword(userId,pwd);

    }

    @Login
    @PostMapping("/user/propertyPayPassword/reset")
    @ApiOperation("重置支付密码")
    @UserLog("重置支付密码")
    public  R resetPropertyPayPassword(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody ResetPayPasswordForm form){
        return userService.userResetPropertyPayPassword(userId,form);

    }

    @GetMapping("/login/wechat/callback")
    @ApiOperation("--wechat --登录回调")
    public void WechatLoginCallback(HttpServletResponse response,String code,String state){
        userService.loginByWechatCallbackV1(response,code,state);
    }





    @Login
    @GetMapping("/user/index")
    @ApiOperation("index")
    public R userInfoStatisticDetailIndex(@ApiIgnore @RequestAttribute("userId") Long userId){
        return userService.userIndexDataStatistics(userId);

    }




    @GetMapping("/user/check")
    @ApiOperation(("用户注册检测"))
    public R userRegisterCheck(@RequestParam String user){
       return  userService.checkTbUserInfo(user);
    }





    @Login
    @PostMapping("/request")
    @ApiOperation("request")
    public R request(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody RequestVo requestVo){
        return userService.proxyRequest(userId,requestVo);

    }


    @Login
    @PostMapping("/user/login-white-ip/list")
    @ApiOperation("login-white-ip list")
    public R loginWhiteIPList(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody QueryLoginWhiteIpForm form){
        return whiteIpService.loginWhiteIPList(UserTypeEnum.USER_TYPE.getId(),userId,form);
    }


    @Login
    @PostMapping("/user/login-white-ip/save")
    @ApiOperation("login-white-ip save")
    public R loginWhiteIPSave(@ApiIgnore @RequestAttribute("userId") Long userId, @RequestBody CdnLoginWhiteIpEntity entity){
        return whiteIpService.saveLoginWhiteIp(UserTypeEnum.USER_TYPE.getId(),userId,entity);
    }

    @Login
    @PostMapping("/user/login-white-ip/delete")
    @ApiOperation("login-white-ip delete")
    public R loginWhiteIPDelete(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody DeleteIdsForm form){
        return whiteIpService.deleteLoginWhiteIps(UserTypeEnum.USER_TYPE.getId(),userId,form.getIds());
    }



}
