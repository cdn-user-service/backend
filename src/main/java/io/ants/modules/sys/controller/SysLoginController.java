/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.utils.*;
import io.ants.modules.app.form.LoginByGoogleAuthForm;
import io.ants.modules.app.service.CdnLoginWhiteIpService;
import io.ants.modules.sys.dao.SysLogDao;
import io.ants.modules.sys.entity.SysLogEntity;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.enums.LogTypeEnum;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.form.SysLoginForm;
import io.ants.modules.sys.service.SysCaptchaService;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.sys.service.SysUserService;
import io.ants.modules.sys.service.SysUserTokenService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.SysLoginKeyConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import javax.imageio.ImageIO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * 登录相关
 *
 * @author Mark sunlightcs@gmail.com
 */

@RestController
public class SysLoginController extends AbstractController {
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysUserTokenService sysUserTokenService;
	@Autowired
	private SysCaptchaService sysCaptchaService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private RedisUtils redisUtils;
	@Autowired
	private SysLogDao sysLogDao;
	@Autowired
	private SysConfigService sysConfigService;
	@Autowired
	private CdnLoginWhiteIpService whiteIpService;

	private void recordLoginLog(SysUserEntity user) {
		SysLogEntity logEntity = new SysLogEntity();
		logEntity.setUserType(UserTypeEnum.MANAGER_TYPE.getId());
		logEntity.setUserId(user.getUserId());
		logEntity.setLogType(LogTypeEnum.LOGIN_LOG.getId());
		logEntity.setUsername(user.getUsername());
		String method = getClass().toString() + ".login()";
		if (method.length() > 64) {
			logEntity.setMethod(method.substring(0, 64));
		} else {
			logEntity.setMethod(method);
		}
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		logEntity.setIp(IPUtils.getIpAddr(request));
		logEntity.setCreateDate(new Date());
		sysLogDao.insert(logEntity);
	}

	/**
	 * 验证码
	 */
	@GetMapping("captcha.jpg")
	public void captcha(HttpServletResponse response, String uuid) throws IOException {
		// 更新MASTER全局变量
		this.recordInfo(redisUtils);

		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setContentType("image/jpeg");

		// 获取图片验证码
		BufferedImage image = sysCaptchaService.getCaptcha(uuid);

		ServletOutputStream out = response.getOutputStream();
		ImageIO.write(image, "jpg", out);
		IOUtils.closeQuietly(out);
	}

	private R loginAndToken(SysUserEntity user, boolean checkIpFlag) {
		if (user.getStatus() == 0) {
			return R.error("账号已被锁定,请联系管理员");
		}
		if (checkIpFlag) {
			HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
			String ip = IPUtils.getIpAddr(request);
			R r1 = whiteIpService.checkCanLoginInCurrentIp(UserTypeEnum.MANAGER_TYPE.getId(), user.getUserId(), ip);
			if (1 != r1.getCode()) {
				return r1;
			}
		}

		// 记录日志
		recordLoginLog(user);

		// 生成token，并保存到数据库
		return sysUserTokenService.createToken(user.getUserId());
	}

	@GetMapping("/sys-user/check")
	@Operation(summary = ("管理员用户检测"))
	public R sysUserCheck(@RequestParam String userName) {
		return sysUserService.checkSysUserInfo(userName);
	}

	/**
	 * 登录
	 */
	@PostMapping("/sys/login")
	public Map<String, Object> login(@RequestBody SysLoginForm form) throws IOException {
		// 更新MASTER全局变量
		this.recordInfo(redisUtils);

		// boolean not_check_vcode=false;
		// if( !"0000".equals(form.getUuid())){
		// not_check_vcode=true;
		// }
		boolean captcha = sysCaptchaService.validate(form.getUuid(), form.getCaptcha());
		if (!captcha) {
			return R.error("验证码不正确");
		}

		// 用户信息
		SysUserEntity user = sysUserService.queryByUserName(form.getUsername());

		// 账号不存在、密码错误
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		if (user == null || !passwordEncoder.matches(form.getPassword(), user.getPassword())) {
			return R.error("账号或密码不正确");
		}
		SysUserEntity gUser = sysUserService.getOne(new QueryWrapper<SysUserEntity>()
				.eq("user_id", user.getUserId())
				.isNotNull("google_auth_secret_key")
				.eq("google_auth_status", 1)
				.select("user_id,google_auth_secret_key")
				.last("limit 1"));
		if (null != gUser) {
			if (StringUtils.isBlank(form.getGoogleAuthCode())
					|| !GoogleAuthUtils.checkCode(gUser.getGoogleAuthSecretKey(),
							Long.parseLong(form.getGoogleAuthCode()), System.currentTimeMillis())) {
				return R.error("失败002，错误的GOOGLE验证码");
			}
		}

		return loginAndToken(user, true);
	}

	/**
	 * 退出
	 */
	@PostMapping("/sys/logout")
	public R logout() {
		sysUserTokenService.logout(getSysUserId());
		return R.ok();
	}

	@PostMapping("/sys/appkey/login")
	public R appKeyLogin(@RequestBody Map map) {
		if (!map.containsKey("username") || !map.containsKey("password") || !map.containsKey("appKey")) {
			return R.error("【username】【password】【appKey】为空");
		}
		SysLoginKeyConfig apiloginconf = sysConfigService.getConfigObject(ConfigConstantEnum.SYS_APP_LOGIN.getConfKey(),
				SysLoginKeyConfig.class);
		if (null == apiloginconf) {
			return R.error("功能未开启");
		}
		if (StringUtils.isBlank(apiloginconf.getAppKey())) {
			return R.error("功能未开启[2]");
		}
		if (!apiloginconf.getAppKey().equals(map.get("appKey").toString())) {
			return R.error("appKey错误");
		}
		if (StringUtils.isNotBlank(apiloginconf.getWhiteIps())) {
			HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
			String[] ips = apiloginconf.getWhiteIps().split(",");
			String ip = IPUtils.getIpAddr(request);
			if (!Arrays.asList(ips).contains(ip)) {
				return R.error("未授权的IP");
			}
		}
		// 用户信息
		SysUserEntity user = sysUserService.queryByUserName(map.get("username").toString());

		// 账号不存在、密码错误
		if (user == null || !passwordEncoder.matches(map.get("password").toString(), user.getPassword())) {
			return R.error("账号或密码不正确");
		}

		return loginAndToken(user, true);
	}

	@PostMapping("/sys/google-auth/login")
	public R googleAuthLogin(@RequestBody LoginByGoogleAuthForm form) {
		R r1 = sysUserService.loginByGoogleAuthCodeGetUid(form);
		if (1 != r1.getCode() || !r1.containsKey("data")) {
			return r1;
		}
		long userId = Long.parseLong(r1.get("data").toString());
		// 用户信息
		SysUserEntity user = sysUserService.getById(userId);

		// 账号不存在、密码错误
		if (user == null) {
			return R.error("user not found");
		}
		return loginAndToken(user, false);
	}

}
