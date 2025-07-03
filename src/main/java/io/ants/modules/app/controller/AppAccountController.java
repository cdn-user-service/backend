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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
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
@Tag(name = "APP 账号 接口")
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

    // 测试环境为TRUE 不验证验证码

    /**
     * username 登录
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public R byUsernameLogin(@RequestBody LoginForm form) {
        return userService.userLogin(form);
    }

    @GetMapping("/access/login")
    @Operation(summary = "第三方（dns）使用access登录CDN")
    public R access_token_login(@RequestParam String access_token) {
        return userService.loginByAccessToken(access_token);
    }

    @GetMapping("/kaptcha.jpg")
    @Operation(summary = "获取验证码")
    public void captcha(HttpServletResponse response, String uuid) throws IOException {
        System.out.println("🎯 Captcha 被访问了！");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");
        // 获取图片验证码
        BufferedImage image = appCaptchaService.getCaptcha(uuid);
        if (null == image) {
            logger.error("image get fail");
            return;
        }
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        IOUtils.closeQuietly(out);
    }

    @PostMapping("/regist")
    @Operation(summary = "注册")
    public R resister(@RequestBody RegisterForm form) {
        return userService.userRegister(form);
    }

    @PostMapping("/api/register")
    public R noCodeRegister(@RequestBody RegisterForm form) {
        return userService.userRegisterWithNoVerifyCode(form);

    }

    @GetMapping("/sendmail/code")
    @Operation(summary = "发送邮件验证码")
    public R sendmail(@RequestParam String mail) {

        if (StringUtils.isBlank(mail) || !mail.contains("@")) {
            return R.error("输入有误");
        }
        return userService.sendVerifyByEmailCode(mail);

    }

    @GetMapping("/sendsms/code")
    @Operation(summary = "发送短信验证码")
    public R sendSms(@RequestParam String mobile, String uuid, String captcha_code) {

        return userService.sendVerifyBySmsCode(mobile, uuid, captcha_code);

    }

    @PostMapping("/forget/password")
    @Operation(summary = "忘记密码")
    public R forget(@RequestBody PasswordForgetForm form) {
        return userService.forgetPassWord(form);
    }

    @Login
    @PostMapping("/update/cdn_access_token")
    @Operation(summary = "更新CDN ACCESS TOKEN")
    public R updateCdnAccessToken(@Parameter(hidden = true) @LoginUser TbUserEntity user, @RequestBody Map param) {
        return R.error("empty access");
    }

    @Login
    @PostMapping("/update/dns_access_token")
    @Operation(summary = "绑定dns ACCESS TOKEN")
    public R bindDnsAccessToken(@Parameter(hidden = true) @LoginUser TbUserEntity user,
            @RequestBody BindDnsForm param) {

        return userService.bindDnsAccessToken(user, param);

    }

    @Login
    @GetMapping("/userinfo")
    @Operation(summary = "获取用户信息")
    public R userInfo(@Parameter(hidden = true) @LoginUser TbUserEntity user) {
        return userService.getUserInfoDetail(user);

    }

    @Login
    @PostMapping("/user-module/update")
    @Operation(summary = "更新用户信息")
    public R updateUserModule(@Parameter(hidden = true) @LoginUser TbUserEntity user,
            @RequestBody UpdateAppUserForm form) {
        return userService.updateUserModules(user, form);

    }

    @Login
    @PostMapping("/message/list")
    @Operation(summary = "用户消息列表")
    public R userMessageList(@Parameter(hidden = true) @RequestAttribute("userId") Integer userId, PageForm form) {
        return userService.getUserMessageList(userId, form);
    }

    @Login
    @PostMapping("/message/delete")
    @Operation(summary = "删除消息")
    public R userMessageDelete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map param) {

        return userService.deleteUserMessage(userId, param);

    }

    @Login
    @GetMapping("/message/info")
    @Operation(summary = "用户查看消息")
    public R userMessageInfo(@Parameter(hidden = true) @RequestAttribute("userId") Integer userId, String id) {
        return userService.viewUserMessageDetail(userId, id);

    }

    @Login
    @PostMapping("/changepwd")
    @Operation(summary = "修改密码")
    @UserLog("修改密码")
    public R userChangePassWord(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody ModifyPwdForm form) {
        return userService.userChangePassWord(userId, form);
    }

    @Login
    @PostMapping("/bind")
    @Operation(summary = "邮箱|手机 绑定")
    public R bindMailOrMail(@Parameter(hidden = true) @LoginUser TbUserEntity user, @RequestBody BindForm form) {
        return userService.bindMailOrMobile(user, form);

    }

    @GetMapping("/login/wechat/qrcode")
    @Operation(summary = "获取获取登录URL")
    public R wechat_login() {
        try {
            String wechatLoginUrl = WXLoginFactory.build().wechatLogin_req_url("0666666", null);
            return R.ok().put("data", wechatLoginUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.error("参数错误");
    }

    @Login
    @GetMapping("/bind/wechat/qrcode_v2")
    @Operation(summary = "获取绑定WECHAT URL v2")
    public R wechat_bind_v2(@Parameter(hidden = true) @RequestAttribute("userId") Integer userId, String redirect_url) {
        String eMsg = "";
        try {
            String appid = WXLoginFactory.build().wechatLogin_appid();
            String RandomStr = String.valueOf(RandomUtil.randomInt(1000000, 9999999));
            redisUtils.set(RandomStr, userId, 600);
            Map<String, String> map = new HashMap<>();
            map.put("state", RandomStr);
            map.put("appid", appid);
            return R.ok().put("data", map);
        } catch (Exception e) {
            eMsg = e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    @GetMapping("/login/wechat/qrcode_v2")
    @Operation(summary = "获取获取登录URL_data v2")
    public R wechatLoginV2() {
        String eMsg = "";
        try {
            String appid = WXLoginFactory.build().wechatLogin_appid();
            String state = "0666666";
            Map<String, String> map = new HashMap<>();
            map.put("state", state);
            map.put("appid", appid);
            return R.ok().put("data", map);
        } catch (Exception e) {
            eMsg = e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }

    @PostMapping("/google-auth/login")
    @Operation(summary = "使用GOOGLE-AUTH登录")
    public R googleAuthLogin(@RequestBody LoginByGoogleAuthForm form) {

        return userService.loginByGoogleAuthCode(form);
    }

    @Login
    @PostMapping("/google-auth/bind")
    @Operation(summary = "GOOGLE-AUTH绑定")
    public R googleAuthBind(@Parameter(hidden = true) @RequestAttribute("userId") Integer userId,
            @RequestBody GoogleAuthBindForm form) {
        return userService.bindByGoogleAuth(userId, form);
    }

    @Login
    @GetMapping("/google-auth/qrcode/view")
    @Operation(summary = "获取Google-auth CODE")
    public R viewGoogleAuthData(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return R.ok().put("data", GoogleAuthUtils.buildOneData(userId));
    }

    @Login
    @GetMapping("/google-auth/status")
    @Operation(summary = "")
    public R statusWithGoogleAuth(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, int status) {
        return userService.statusWithGoogleAuthSave(userId, status);
    }

    @Login
    @GetMapping("/bind/wechat/qrcode")
    @Operation(summary = "获取绑定WECHAT URL")
    public R wechat_bind(@Parameter(hidden = true) @RequestAttribute("userId") Integer userId) {
        return userService.bindByWechat(userId);
    }

    @GetMapping("/login/wechat/regist")
    @Operation(summary = "微信 -- 登录 --注册")
    public R WechatRegister(String nickname, String openid) {
        return userService.wechatRegisterAndLogin(nickname, openid);

    }

    @PostMapping("/login/wechat/bind")
    @Operation(summary = "微信--登录--绑定")
    public R wechat_login_bind(@RequestBody WechatBindForm form) {

        return userService.loginByWechatOrRegister(form);

    }

    @GetMapping("/login/wechat/callback_v2")
    @Operation(summary = "--wechat --登录回调v2")
    public R WechatLoginCallbackV2(String code, String state) {
        return userService.wechatLoginCallbackV2(code, state);

    }

    @Login
    @GetMapping("/user/password/set")
    @Operation(summary = "设置密码（密码为空时可设置）")
    public R set_pwd(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, String pwd) {
        return userService.nullPasswordSetPassword(userId, pwd);

    }

    @Login
    @GetMapping("/user/propertyPayPassword/set")
    @Operation(summary = "设置支付密码（密码为空时可设置）")
    public R set_propertyPayPassword(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, String pwd) {
        return userService.nullPropertyPayPasswordSetPassword(userId, pwd);

    }

    @Login
    @PostMapping("/user/propertyPayPassword/reset")
    @Operation(summary = "重置支付密码")
    @UserLog("重置支付密码")
    public R resetPropertyPayPassword(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody ResetPayPasswordForm form) {
        return userService.userResetPropertyPayPassword(userId, form);

    }

    @GetMapping("/login/wechat/callback")
    @Operation(summary = "--wechat --登录回调")
    public void WechatLoginCallback(HttpServletResponse response, String code, String state) {
        userService.loginByWechatCallbackV1(response, code, state);
    }

    @Login
    @GetMapping("/user/index")
    @Operation(summary = "index")
    public R userInfoStatisticDetailIndex(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return userService.userIndexDataStatistics(userId);

    }

    @GetMapping("/user/check")
    @Operation(summary = "用户注册检测")
    public R userRegisterCheck(@RequestParam String user) {
        return userService.checkTbUserInfo(user);
    }

    @Login
    @PostMapping("/request")
    @Operation(summary = "request")
    public R request(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody RequestVo requestVo) {
        return userService.proxyRequest(userId, requestVo);

    }

    @Login
    @PostMapping("/user/login-white-ip/list")
    @Operation(summary = "login-white-ip list")
    public R loginWhiteIPList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QueryLoginWhiteIpForm form) {
        return whiteIpService.loginWhiteIPList(UserTypeEnum.USER_TYPE.getId(), userId, form);
    }

    @Login
    @PostMapping("/user/login-white-ip/save")
    @Operation(summary = "login-white-ip save")
    public R loginWhiteIPSave(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody CdnLoginWhiteIpEntity entity) {
        return whiteIpService.saveLoginWhiteIp(UserTypeEnum.USER_TYPE.getId(), userId, entity);
    }

    @Login
    @PostMapping("/user/login-white-ip/delete")
    @Operation(summary = "login-white-ip delete")
    public R loginWhiteIPDelete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody DeleteIdsForm form) {
        return whiteIpService.deleteLoginWhiteIps(UserTypeEnum.USER_TYPE.getId(), userId, form.getIds());
    }

}
