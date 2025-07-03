package io.ants.modules.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.common.validator.AbstractAssert;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.app.form.*;
import io.ants.modules.app.service.AppCaptchaService;
import io.ants.modules.app.service.CdnLoginWhiteIpService;
import io.ants.modules.app.service.TbUserLogService;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.app.utils.JwtUtils;
import io.ants.modules.app.vo.AppUserTokenVo;
import io.ants.modules.app.vo.UserModuleVo;
import io.ants.modules.sys.dao.CdnProductDao;
import io.ants.modules.sys.dao.CdnSuitDao;
import io.ants.modules.sys.dao.TbCdnPublicMutAttrDao;
import io.ants.modules.sys.entity.CdnProductEntity;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.SaveTbUserInfoFrom;
import io.ants.modules.sys.service.CdnSuitService;
import io.ants.modules.sys.service.PayService;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.sys.vo.DnsRewriteConfVo;
import io.ants.modules.sys.vo.OrderCdnProductVo;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.DnsApiRootUriConf;
import io.ants.modules.utils.config.MailConfig;
import io.ants.modules.utils.config.SmsParamVo;
import io.ants.modules.utils.factory.MailFactory;
import io.ants.modules.utils.factory.SmsFactory;
import io.ants.modules.utils.factory.wxchat.WXLoginFactory;
import io.ants.modules.utils.service.MailService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("TbUserService")
public class TbUserServiceImpl extends ServiceImpl<TbUserDao, TbUserEntity> implements TbUserService {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private CdnProductDao cdnProductDao;
	@Autowired
	private CdnSuitService cdnSuitService;
	@Autowired
	private PayService payService;
	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	private TbUserLogService tbLogService;
	@Autowired
	private AppCaptchaService appCaptchaService;
	@Autowired
	private RedisUtils redisUtils;
	@Autowired
	private SysConfigService sysConfigService;
	@Autowired
	private CdnSuitDao cdnSuitDao;
	@Autowired
	private TbMessageRelationDao tbMessageRelationDao;
	@Autowired
	private TbMessageDao tbMessageDao;
	@Autowired
	private TbSiteDao tbSiteDao;
	@Autowired
	private TbStreamProxyDao tbStreamProxyDao;
	@Autowired
	private TbCertifyDao tbcertifyDao;
	@Autowired
	private CdnLoginWhiteIpService whiteIpService;
	@Autowired
	private TbCdnPublicMutAttrDao tbCdnPublicMutAttrDao;

	final private Boolean isTest = false;

	private AppUserTokenVo generateAppUserTokenVo(long userId, boolean dnsLoginFlag, String accessCode,
			String loginType, boolean needCheckFlag) {
		if (needCheckFlag) {
			HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
			String ip = IPUtils.getIpAddr(request);
			R r1 = whiteIpService.checkCanLoginInCurrentIp(UserTypeEnum.USER_TYPE.getId(), userId, ip);
			if (1 != r1.getCode()) {
				throw new RRException("您的IP不在白名单内");
			}
		}
		String token = jwtUtils.generateToken(userId);
		AppUserTokenVo vo = new AppUserTokenVo();
		vo.setToken(token);
		vo.setExpire(jwtUtils.getExpire());
		if (dnsLoginFlag) {
			TbUserEntity user = this.userDetail(userId);
			this.update(null,
					new UpdateWrapper<TbUserEntity>().eq("user_id", userId).set("u_dns_access_token", accessCode));
			vo.setAccess_token(user.getUCdnAccessToken());
		}
		tbLogService.frontUserWriteLog(userId, LogTypeEnum.LOGIN_LOG.getId(), loginType + "登录", "");
		return vo;
	}

	private Map generateAppUserToken(long userId, boolean dnsLoginFlag, String accessCode, String loginType,
			boolean needCheckFlag) {
		return DataTypeConversionUtil
				.entity2map(generateAppUserTokenVo(userId, dnsLoginFlag, accessCode, loginType, needCheckFlag));
	}

	@Override
	public TbUserEntity queryByMobile(String mobile) {
		return baseMapper.selectOne(new QueryWrapper<TbUserEntity>().eq("mobile", mobile));
	}

	@Override
	public long login(LoginForm form) {
		TbUserEntity user = queryByMobile(form.getAccount());
		AbstractAssert.isNull(user, "账号或密码错误");

		// 密码错误
		if (!user.getPassword().equals(DigestUtils.sha256Hex(form.getPassword()))) {
			throw new RRException("账号号或密码错误");
		}

		return user.getUserId();
	}

	@Override
	public String key2userIds(String key) {
		if (StringUtils.isBlank(key)) {
			return "";
		}
		List<TbUserEntity> list = this.list(new QueryWrapper<TbUserEntity>()
				.like("username", key).or().like("mobile", key).or().like("mail", key)
				.select("user_id"));
		List<String> longList = list.stream().map(t -> t.getUserId().toString()).collect(Collectors.toList());
		return String.join(",", longList);
	}

	@Override
	public R checkTbUserInfo(String key) {
		TbUserEntity tbUser = this.getOne(new QueryWrapper<TbUserEntity>()
				.eq("username", key)
				.or()
				.eq("mobile", key)
				.or()
				.eq("mail", key)
				.select("user_id"));
		Map<String, Object> retMap = new HashMap();
		if (null == tbUser) {
			retMap.put("data", 0);
		} else {
			retMap.put("data", 1);
			TbUserEntity user = this.getOne(new QueryWrapper<TbUserEntity>()
					.eq("user_id", tbUser.getUserId())
					.isNotNull("google_auth_secret_key")
					.eq("google_auth_status", 1)
					.select("user_id,google_auth_secret_key")
					.last("limit 1"));
			if (null != user) {
				retMap.put("google_auth_status", 1);
			} else {
				retMap.put("google_auth_status", 0);
			}

		}
		return R.ok(retMap);

	}

	@Override
	public PageUtils userList(Integer page, Integer limit, String user) {
		IPage<TbUserEntity> ipage = this.page(
				new Page<>(page, limit),
				new QueryWrapper<TbUserEntity>()
						.orderByDesc("user_id")
						.like("username", user).or().like("mobile", user).or().like("mail", user));
		return new PageUtils(ipage);
	}

	@Override
	public TbUserEntity userDetail(Long userId) {
		TbUserEntity user = this.getById(userId);
		if (null != user) {
			this.setUserAccessToken(user);
		}
		return user;
	}

	@Override
	public void delete(String ids) {
		String[] id_s = ids.split(",");
		for (String id : id_s) {
			baseMapper.deleteById(id);
		}
	}

	@Override
	public TbUserEntity saveUserByUserForm(SaveTbUserInfoFrom userForm) {
		if (null == userForm.getUserId() || 0 == userForm.getUserId()) {
			// add
			Long c = baseMapper.selectCount(new QueryWrapper<TbUserEntity>()
					.and(q -> q.eq(StringUtils.isNotBlank(userForm.getUsername()), "username", userForm.getUsername())
							.or()
							.eq(StringUtils.isNotBlank(userForm.getMail()), "mail", userForm.getMail())
							.or()
							.eq(StringUtils.isNotBlank(userForm.getMobile()), "mobile", userForm.getMobile()))

			);
			if (c > 0) {
				throw new RRException("创建失败！存在的用户");
			}
			TbUserEntity userEntity = new TbUserEntity();
			DataTypeConversionUtil.daoEntity2VoEntity(userForm, userEntity);
			if (StringUtils.isNotBlank(userEntity.getPassword())) {
				userEntity.setPassword(DigestUtils.sha256Hex(userEntity.getPassword()));
			} else {
				userEntity.setPassword(DigestUtils.sha256Hex("123456"));
			}
			this.save(userEntity);
			return userEntity;
		} else {
			// modify
			TbUserEntity user = this.getById(userForm.getUserId());
			if (null == user) {
				throw new RRException("无此用户");
			}
			long userId = user.getUserId();
			String username = userForm.getUsername();
			String mobile = userForm.getMobile();
			String mail = userForm.getMobile();
			if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(mobile) || StringUtils.isNotBlank(mail)) {
				Long c = baseMapper.selectCount(new QueryWrapper<TbUserEntity>()
						.ne("user_id", userId)
						.and(StringUtils.isNotBlank(username), q -> q.eq("username", username))
						.and(StringUtils.isNotBlank(mobile), q -> q.eq("mobile", mobile))
						.and(StringUtils.isNotBlank(mail), q -> q.eq("mail", mail))

				);
				if (c > 0) {
					throw new RRException("修改失败！存在的用户");
				}
			}
			if (StringUtils.isNotBlank(userForm.getUsername())) {
				user.setUsername(userForm.getUsername());
			}
			if (StringUtils.isNotBlank(userForm.getMail())) {
				user.setMail(userForm.getMail());
			}
			if (StringUtils.isNotBlank(userForm.getMobile())) {
				user.setMobile(userForm.getMobile());
			}
			if (StringUtils.isNotBlank(userForm.getPassword())) {
				user.setPassword(DigestUtils.sha256Hex(userForm.getPassword()));
			}
			if (StringUtils.isNotBlank(userForm.getNote())) {
				user.setNote(userForm.getNote());
			}
			if (StringUtils.isNotBlank(userForm.getUnvalidModel())) {
				user.setUnvalidModel(userForm.getUnvalidModel());
			}
			if (null != userForm.getStatus()) {
				user.setStatus(userForm.getStatus());
			}
			this.updateById(user);
			return user;
		}

	}

	@Override
	public TbUserEntity modifyUserBaseInfo(Long userId, String username, String mobile, String mail, String password,
			Integer status) {
		TbUserEntity user = this.getById(userId);
		if (null != user) {
			if (StringUtils.isNotBlank(username)) {
				user.setUsername(username);
			}
			if (StringUtils.isNotBlank(mobile)) {
				user.setMobile(mobile);
			}
			if (StringUtils.isNotBlank(mail)) {
				user.setMail(mail);
			}
			if (StringUtils.isNotBlank(password)) {
				user.setPassword(DigestUtils.sha256Hex(password));
			}
			if (null != status) {
				user.setStatus(status);
			}
			Long count = 0L;
			if (StringUtils.isBlank(username) && StringUtils.isBlank(mail) && StringUtils.isBlank(mobile)) {
				// pass
			} else {
				count = this.count(new QueryWrapper<TbUserEntity>()
						.ne("user_id", userId)
						.and(q -> q.eq(StringUtils.isNotBlank(username), "username", username)
								.or()
								.eq(StringUtils.isNotBlank(mail), "mail", mail)
								.or()
								.eq(StringUtils.isNotBlank(mobile), "mobile", mobile))

				);
			}
			if (count > 0) {
				throw new RRException("存在重复的用户数据！");
			}
			this.updateById(user);
		}
		return user;
	}

	@Override
	public TbUserEntity createUser(String username, String mobile, String mail, String password) {
		Long count = this.count(new QueryWrapper<TbUserEntity>()
				.and(q -> q.eq(StringUtils.isNotBlank(username), "username", username).or()
						.eq(StringUtils.isNotBlank(mobile), "mobile", mobile).or()
						.eq(StringUtils.isNotBlank(mail), "mail", mail)));
		if (0 == count) {
			TbUserEntity userEntity = new TbUserEntity();
			userEntity.setUsername(username);
			userEntity.setMobile(mobile);
			userEntity.setMail(mail);
			if (StringUtils.isNotBlank(password)) {
				userEntity.setPassword(DigestUtils.sha256Hex(password));
			} else {
				userEntity.setPassword(DigestUtils.sha256Hex("123456"));
			}
			this.save(userEntity);
			return userEntity;
		}
		return null;
	}

	@Override
	public boolean save(TbUserEntity userEntity) {
		boolean ret = SqlHelper.retBool(this.getBaseMapper().insert(userEntity));
		if (ret) {
			// 添加init (注册赠送) 套餐
			CdnProductEntity product = cdnProductDao.selectOne(new QueryWrapper<CdnProductEntity>()
					.eq("status", ProductStatusEnum.ONLY_FIRST.getId())
					.eq("product_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
					.orderByDesc("id")
					.ne("is_delete", 1)
					.last("limit 1"));
			if (null != product) {
				SubmitOrderForm submitOrderForm = new SubmitOrderForm();
				submitOrderForm.setUserId(userEntity.getUserId());
				submitOrderForm.setOrderType(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId());
				submitOrderForm.setTargetId(product.getId());
				OrderCdnProductVo productJsonVo = DataTypeConversionUtil.string2Entity(product.getProductJson(),
						OrderCdnProductVo.class);
				JSONObject initJsonObject = new JSONObject();
				if (null == productJsonVo || null == productJsonVo.getV() || 0 == productJsonVo.getV().getStatus()
						|| 0 == productJsonVo.getV().getValue()) {
					initJsonObject.put("type", "m");
					initJsonObject.put("sum", 120);
				} else {
					initJsonObject.put("type", "m");
					initJsonObject.put("sum", productJsonVo.getV().getValue());
				}
				submitOrderForm.setInitJson(initJsonObject.toJSONString());
				R r = cdnSuitService.createOrder(submitOrderForm);
				if (1 == r.getCode()) {
					if (r.containsKey("data")) {
						TbOrderEntity order = (TbOrderEntity) r.get("data");
						PayBalanceForm payBalanceForm = new PayBalanceForm();
						payBalanceForm.setUserId(userEntity.getUserId());
						payBalanceForm.setSerialNumber(order.getSerialNumber());
						payService.orderPayBalance(payBalanceForm);
					} else {
						logger.error(r.toString());
					}
				}
			}
		}
		return ret;
	}

	// @Override
	// public TbUserEntity queryByMobile(String mobile) {
	// return this.getOne (new QueryWrapper<TbUserEntity>().eq("mobile",
	// mobile).orderByDesc("user_id").last("limit 1"));
	// }

	@Override
	public TbUserEntity queryByEmail(String mail) {
		return this.getOne(new QueryWrapper<TbUserEntity>().eq("mail", mail).orderByDesc("user_id").last("limit 1"));
	}

	@Override
	public TbUserEntity queryByUserName(String userName) {
		return this.getOne(
				new QueryWrapper<TbUserEntity>().eq("username", userName).orderByDesc("user_id").last("limit 1"));
	}

	private void setUserAccessToken(TbUserEntity user) {
		if (StringUtils.isBlank(user.getUCdnAccessToken())) {
			String cUuid = UUID.randomUUID().toString().replaceAll("-", "");
			user.setUCdnAccessToken(cUuid);
			this.update(null,
					new UpdateWrapper<TbUserEntity>().eq("user_id", user.getUserId()).set("u_cdn_access_token", cUuid));
		}
	}

	@Override
	public long loginByUsername(String username, String password) {
		TbUserEntity user = this.getOne(new QueryWrapper<TbUserEntity>().eq("username", username).last("limit 1"));
		if (null != user) {
			this.setUserAccessToken(user);
			if (0 == user.getStatus()) {
				throw new RRException("账户被禁用");
			}
			boolean flag = user.getPassword().equals(DigestUtils.sha256Hex(password));
			if (flag) {
				user.setLoginType("username");
				this.updateById(user);
				return user.getUserId();
			}
		}
		return 0;
	}

	@Override
	public long loginByMail(String mail, String password) {
		TbUserEntity user = this.getOne(new QueryWrapper<TbUserEntity>().eq("mail", mail).last("limit 1"));
		if (null != user) {
			this.setUserAccessToken(user);
			if (0 == user.getStatus()) {
				throw new RRException("账户被禁用");
			}
			boolean flag = user.getPassword().equals(DigestUtils.sha256Hex(password));
			if (flag) {
				user.setLoginType("mail");
				this.updateById(user);
				return user.getUserId();
			}
		}
		return 0;
	}

	@Override
	public long loginByMobil(String mobile, String password) {
		TbUserEntity user = this.getOne(new QueryWrapper<TbUserEntity>().eq("mobile", mobile).last("limit 1"));
		if (null != user) {
			this.setUserAccessToken(user);
			if (0 == user.getStatus()) {
				throw new RRException("账户被禁用");
			}
			boolean flag = user.getPassword().equals(DigestUtils.sha256Hex(password));
			if (flag) {
				user.setLoginType("mobile");
				this.updateById(user);
				return user.getUserId();
			}
		}
		return 0;
	}

	@Override
	public R loginByGoogleAuthCode(LoginByGoogleAuthForm form) {
		ValidatorUtils.validateEntity(form);
		TbUserEntity user = this.getOne(new QueryWrapper<TbUserEntity>()
				.and(q -> q.eq("username", form.getUser()).or().eq("mobile", form.getUser()).or().eq("mail",
						form.getUser()))
				.isNotNull("google_auth_secret_key")
				.eq("google_auth_status", 1)
				.select("user_id,google_auth_secret_key")
				.last("limit 1"));
		if (null == user) {
			return R.error("失败001，未绑定或未注册或未开启");
		}
		if (!GoogleAuthUtils.checkCode(user.getGoogleAuthSecretKey(), Long.parseLong(form.getCode()),
				System.currentTimeMillis())) {
			return R.error("失败002，错误的验证码");
		}
		return R.ok(this.generateAppUserToken(user.getUserId(), false, "", "googleAuth", false));
	}

	@Override
	public R userLogin(LoginForm form) {
		// 表单校验
		ValidatorUtils.validateEntity(form);
		boolean dns_login_flag = false;
		if (!isTest) {
			if (StringUtils.isNotBlank(form.getCode()) && StringUtils.isNotBlank(form.getUuid())
					&& form.getUuid().equals("0000") && form.getCode().equals("0000")
					&& StringUtils.isNotBlank(form.getAccessCode())) {
				// dns access bind login
				dns_login_flag = true;
			} else {
				boolean captcha = appCaptchaService.validate(form.getUuid(), form.getCode());
				if (!captcha) {
					return R.error("验证码不正确");
				}
			}
		}

		long userId = this.loginByUsername(form.getAccount(), form.getPassword());
		if (0 == userId) {
			userId = this.loginByMail(form.getAccount(), form.getPassword());
			if (0 == userId) {
				userId = this.loginByMobil(form.getAccount(), form.getPassword());
			}
		}

		TbUserEntity gUser = this.getOne(new QueryWrapper<TbUserEntity>()
				.eq("user_id", userId)
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
		if (0 != userId) {
			// 生成token
			return R.ok(this.generateAppUserToken(userId, dns_login_flag, form.getAccessCode(), "userLogin", true));
		}
		return R.error("登录失败,用户名或密码错误！");
	}

	@Override
	public R loginByAccessToken(String access_token) {
		if (!StringUtils.isNotBlank(access_token)) {
			return R.error("token is empty!");
		}
		TbUserEntity userEntity = this.getOne(new QueryWrapper<TbUserEntity>()
				.eq("u_cdn_access_token", access_token)
				.select("user_id")
				.last("limit 1"));
		if (null == userEntity) {
			return R.error("token error!");
		}
		return R.ok(this.generateAppUserToken(userEntity.getUserId(), false, "", "token", true));

	}

	private TbUserEntity priAppUserRegister(String username, String mail, String mobile, String password, String note) {
		TbUserEntity userEntity = new TbUserEntity();
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		userEntity.setRegistIp(IPUtils.getIpAddr(request));
		userEntity.setPassword(DigestUtils.sha256Hex(password));
		userEntity.setNote(note);
		userEntity.setUCdnAccessToken(HashUtils.md5ofString(new Date().toString()));
		if (StringUtils.isNotBlank(username)) {
			userEntity.setUsername(username);
			this.save(userEntity);
			return userEntity;
		} else if (StringUtils.isNotBlank(mail)) {
			userEntity.setMail(mail);
			userEntity.setUsername(mail);
			this.save(userEntity);
			redisUtils.delete(mail);
			return userEntity;
		} else if (StringUtils.isNotBlank(mobile)) {
			userEntity.setMobile(mobile);
			userEntity.setUsername(mobile);
			this.save(userEntity);
			redisUtils.delete(mobile);
			return userEntity;
		}
		return userEntity;
	}

	@Override
	public R userRegister(RegisterForm form) {
		ValidatorUtils.validateEntity(form);
		if (StringUtils.isBlank(form.getUsername()) && StringUtils.isBlank(form.getMail())
				&& StringUtils.isBlank(form.getMobile())) {
			return R.error("传入的数据有误");
		}

		TbUserEntity userEntity = null;
		if (StringUtils.isNotBlank(form.getUsername())) {
			// username 注册
			if (!isTest) {
				boolean captcha = appCaptchaService.validate(form.getUuid(), form.getCode());
				if (!captcha) {
					return R.error("验证码错误【captcha】");
				}
			}
			if (null != this.queryByUserName(form.getUsername())) {
				return R.error("用户名 已经存在");
			}
			userEntity = this.priAppUserRegister(form.getUsername(), "", "", form.getPassword(), "");
		} else if (StringUtils.isNotBlank(form.getMail())) {
			// mail 注册
			if (null != this.queryByEmail(form.getMail())) {
				return R.error("邮箱 已经存在");
			}
			String mailCode = redisUtils.get(form.getMail());
			if (StringUtils.isNotBlank(mailCode) && mailCode.equals(form.getCode())) {
				userEntity = this.priAppUserRegister("", form.getMail(), "", form.getPassword(), "");
			} else {
				return R.error("验证码错误[mail]");
			}
		} else if (StringUtils.isNotBlank(form.getMobile())) {
			// mobile
			if (null != this.queryByMobile(form.getMobile())) {
				return R.error("手机号 已经存在");
			}
			String mobile_code = "000000";
			if (!isTest) {
				mobile_code = redisUtils.get(form.getMobile());
			}
			if (StringUtils.isNotBlank(mobile_code) && mobile_code.equals(form.getCode())) {
				userEntity = this.priAppUserRegister("", "", form.getMobile(), form.getPassword(), "");
			} else {
				return R.error("验证码错误[mobile]");
			}
		}
		if (null != userEntity && null != userEntity.getUserId()) {
			DnsApiRootUriConf dnsApiRootUriConf = sysConfigService
					.getConfigObject(ConfigConstantEnum.DNS_USER_API_ROOT_URI.getConfKey(), DnsApiRootUriConf.class);
			if (null != dnsApiRootUriConf && 1 == dnsApiRootUriConf.getSyncRegister()) {
				if (StringUtils.isNotBlank(dnsApiRootUriConf.getApiRootPath())) {
					String str = HttpRequest.okHttpPost(
							dnsApiRootUriConf.getApiRootPath() + "/app/account/api/register",
							DataTypeConversionUtil.entity2jonsStr(form));
					logger.info("同步注册DNS:" + str);
				}
			}
			return R.ok(this.generateAppUserToken(userEntity.getUserId(), false, "", "register", true));
		}
		return R.error("注册失败");
	}

	@Override
	public R userRegisterWithNoVerifyCode(RegisterForm form) {
		ValidatorUtils.validateEntity(form);
		TbUserEntity userEntity = null;
		if (StringUtils.isNotBlank(form.getUsername())) {
			userEntity = this.priAppUserRegister(form.getUsername(), "", "", form.getPassword(), "API注册");
		} else if (StringUtils.isNotBlank(form.getMail())) {
			userEntity = this.priAppUserRegister("", form.getMail(), "", form.getPassword(), "API注册");
		} else if (StringUtils.isNotBlank(form.getMobile())) {
			userEntity = this.priAppUserRegister("", "", form.getMobile(), form.getPassword(), "API注册");
		}
		if (null != userEntity && null != userEntity.getUserId()) {
			return R.ok();
		}
		return R.error("注册失败");
	}

	private R priSendMailThreadHandle(String mail, String code) {
		SysConfigEntity mailSysConfig = sysConfigService
				.getConfigByKey(ConfigConstantEnum.MAIL_CONFIG_KEY.getConfKey());
		MailService mailService = MailFactory.build();
		if (null == mailService) {
			return R.error("邮件未配置！");
		}
		String eMsg = "";
		try {
			String title = "动态验证码";
			String content = "动态验证码为#code#";
			MailConfig mailConfig = DataTypeConversionUtil.string2Entity(mailSysConfig.getParamValue(),
					MailConfig.class);
			for (MailConfig.tempVo item : mailConfig.getTemplates()) {
				if (1 == item.getStatus() && "code".equals(item.getName())) {
					title = item.getTitle();
					content = item.getContent();
					break;
				}
			}
			String msg = content.replace("#code#", code);
			// logger.debug(msg);
			return mailService.sendEmail(mail, title, msg);
		} catch (Exception e) {
			eMsg = e.getMessage();
			e.printStackTrace();
		}
		return R.error(eMsg);
	}

	@Override
	public R sendVerifyByEmailCode(String mail) {
		String code = String.valueOf(RandomUtil.randomInt(100000, 999999));
		redisUtils.set(mail, code, 600);
		return this.priSendMailThreadHandle(mail, code);
	}

	@Override
	public R sendVerifyBySmsCode(String mobile, String uuid, String captcha_code) {
		final Pattern mobile_pattern = Pattern.compile("^\\+861.[0-9]{10}$");

		if (StringUtils.isBlank(mobile)) {
			return R.error("请输入正确的手机号");
		}
		if (true) {
			boolean captcha = appCaptchaService.validate(uuid, captcha_code);
			if (!captcha) {
				return R.error("图形验证码不正确");
			}
		}
		String cnFullMobile;
		String cn11Mobile;
		Matcher m = mobile_pattern.matcher(mobile);
		if (m.matches()) {
			cnFullMobile = mobile;
			cn11Mobile = mobile.substring(3);
		} else {
			cnFullMobile = "+86" + mobile;
			cn11Mobile = mobile;
		}
		if (!isTest) {
			String code = String.valueOf(RandomUtil.randomInt(100000, 999999));
			SmsParamVo vo = new SmsParamVo();
			String[] param = { code, "10" };
			vo.setTencentParam(param);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("code", code);
			jsonObject.put("validity", "10");
			vo.setAliParam(jsonObject);
			vo.setSmsBaoParam(jsonObject);
			R r = SmsFactory.sendSms(cnFullMobile, "code", vo);
			if (1 != r.getCode()) {
				return r;
			}
			redisUtils.set(cn11Mobile, code, 600);
		} else {
			redisUtils.set(cn11Mobile, "000000", 600);
		}
		return R.ok();
	}

	@Override
	public R forgetPassWord(PasswordForgetForm form) {
		ValidatorUtils.validateEntity(form);
		if (StringUtils.isBlank(form.getMail()) && StringUtils.isBlank(form.getMobile())) {
			return R.error("找回方式错误！");
		}
		if (StaticVariableUtils.demoIp.equals(StaticVariableUtils.authMasterIp)) {
			return R.error("验证站不可修改密码").put("code_mail", redisUtils.get(form.getMail())).put("code_mobile",
					redisUtils.get(form.getMobile()));
		}

		if (StringUtils.isNotBlank(form.getMail())) {
			String mail_code = redisUtils.get(form.getMail());
			if (StringUtils.isNotBlank(mail_code) && mail_code.equals(form.getCode())) {
				TbUserEntity userEntity = this.queryByEmail(form.getMail());
				if (null == userEntity) {
					return R.error("未注册");
				}
				userEntity.setPassword(DigestUtils.sha256Hex(form.getPassword()));
				this.updateById(userEntity);
				redisUtils.delete(form.getMail());
				tbLogService.frontUserWriteLog(userEntity.getUserId(), LogTypeEnum.OTHER_LOG.getId(), "忘记密码", "");
				return R.ok();
			} else {
				return R.error("验证码错误【mail】");
			}
		} else if (StringUtils.isNotBlank(form.getMobile())) {
			String mobile_code = redisUtils.get(form.getMobile());
			if (StringUtils.isNotBlank(mobile_code) && mobile_code.equals(form.getCode())) {
				TbUserEntity userEntity = this.queryByMobile(form.getMobile());
				if (null == userEntity) {
					return R.error("未注册");
				}
				userEntity.setPassword(DigestUtils.sha256Hex(form.getPassword()));
				this.updateById(userEntity);
				redisUtils.delete(form.getMobile());
				tbLogService.frontUserWriteLog(userEntity.getUserId(), LogTypeEnum.OTHER_LOG.getId(), "忘记密码", "");
				return R.ok();
			} else {
				return R.error("验证码错误【mobile】");
			}
		}
		return R.error("参数错误");
	}

	@Override
	public R bindDnsAccessToken(TbUserEntity user, BindDnsForm param) {
		ValidatorUtils.validateEntity(param);
		DnsApiRootUriConf dnsApiRootUriConf = sysConfigService
				.getConfigObject(ConfigConstantEnum.DNS_USER_API_ROOT_URI.getConfKey(), DnsApiRootUriConf.class);
		if (null != dnsApiRootUriConf) {
			if (StringUtils.isNotBlank(dnsApiRootUriConf.getApiRootPath())) {
				JSONObject loginFrom = new JSONObject();
				loginFrom.put("account", param.getAccount());
				loginFrom.put("password", param.getPassword());
				loginFrom.put("uuid", "0000");
				loginFrom.put("code", "0000");
				loginFrom.put("accessCode", user.getUCdnAccessToken());
				// System.out.println(user);
				String str = HttpRequest.okHttpPost(dnsApiRootUriConf.getApiRootPath() + "/app/account/login",
						DataTypeConversionUtil.entity2jonsStr(loginFrom));
				if (StringUtils.isNotBlank(str)) {
					JSONObject jsonObject = DataTypeConversionUtil.string2Json(str);
					if (null != jsonObject && jsonObject.containsKey("code")) {
						if (1 == jsonObject.getInteger("code") && jsonObject.containsKey("access_token")) {
							String dns_access_token = jsonObject.getString("access_token");
							this.update(null, new UpdateWrapper<TbUserEntity>().eq("user_id", user.getUserId())
									.set("u_dns_access_token", dns_access_token));
							return R.ok("绑定成功");
						} else {
							return R.error(str);
						}
					}
				}
			}
		}
		return R.error("绑定失败");
	}

	private int priGetMaxForceUrl(Long userId) {
		if (!QuerySysAuth.PLUS_1_FLAG) {
			return 0;
		}
		if (null == userId) {
			return 10000;
		}
		Integer[] sInList = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(), OrderTypeEnum.ORDER_CDN_RENEW.getTypeId(),
				OrderTypeEnum.ORDER_CDN_ADDED.getTypeId() };
		Integer[] sInStatusList = { CdnSuitStatusEnum.NORMAL.getId() };
		List<CdnSuitEntity> suitList = cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
				.eq("user_id", userId)
				.in("suit_type", sInList)
				.in("status", sInStatusList)
				.le("start_time", new Date())
				.ge("end_time", new Date()));
		int ret = 0;
		String key = ProductAttrNameEnum.ATTR_FORCE_URL_TYPE.getAttr();
		for (CdnSuitEntity suitEntity : suitList) {
			if (StringUtils.isBlank(suitEntity.getAttrJson())) {
				continue;
			}
			JSONObject vo = DataTypeConversionUtil.string2Json(suitEntity.getAttrJson());
			if (null == vo) {
				continue;
			}
			if (vo.containsKey(key) && null != vo.get(key)) {
				ret += vo.getInteger(key);
			}
		}
		return ret;
	}

	@Override
	public R getUserInfoDetail(TbUserEntity user) {
		if (null == user) {
			return R.error("获取用户信息失败!");
		}
		TbUserEntity userEntity = this.userDetail(user.getUserId());
		UserModuleVo userModuleVo = new UserModuleVo();
		userModuleVo.setMaxForceUrl(QuerySysAuth.defaultForceUrl + this.priGetMaxForceUrl(user.getUserId()));
		userEntity.setUserModuleVo(userModuleVo);
		if (StringUtils.isBlank(user.getUsername())) {
			if (StringUtils.isNotBlank(user.getMobile())) {
				user.setUsername(user.getMobile());
			} else if (StringUtils.isNotBlank(user.getMail())) {
				user.setUsername(user.getMail());
			}
			if (null != userEntity) {
				if (null == userEntity.getPassword() || StringUtils.isBlank(userEntity.getPassword())) {
					user.setUserModuleVo(userModuleVo);
					return R.ok().put("user", user).put("no_pwd", 1);
				}
			}
		}

		return R.ok().put("user", userEntity);
	}

	private R forceRewriteRequest(Long userId, RequestVo requestVo, String param) {
		JSONObject object = DataTypeConversionUtil.string2Json(param);
		if (null == object) {
			return R.error(String.format("参数PARAM`%s` is error", param));
		}
		if (!object.containsKey("id") || null == object.get("id") || 0 == object.getInteger("id")) {
			object.put("maxCount", this.priGetMaxForceUrl(userId));
			requestVo.setParam(object.toJSONString());
		}
		if (!requestVo.getUrl().startsWith("http")) {
			TbCdnPublicMutAttrEntity publicMutAttr = tbCdnPublicMutAttrDao
					.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
							.eq("pkey", PublicEnum.DNS_REWRITE_CONF.getName()).last("limit 1"));
			if (null == publicMutAttr || StringUtils.isBlank(publicMutAttr.getPvalue())) {
				return R.error("请先在管理后台中配置DNS转发!");
			}
			DnsRewriteConfVo rVo = DataTypeConversionUtil.string2Entity(publicMutAttr.getPvalue(),
					DnsRewriteConfVo.class);
			String newUrl = String.format("%s%s", rVo.getDnsAddress(), requestVo.getUrl());
			requestVo.setUrl(newUrl);
		}
		return HttpRequest.apiRequest(userId, requestVo);
	}

	@Override
	public R proxyRequest(Long userId, RequestVo requestVo) {
		ValidatorUtils.validateEntity(requestVo);
		if (requestVo.getUrl().contains("/app/rewrite/cname/save")) {
			if (StringUtils.isNotBlank(requestVo.getParam())) {
				// 单个
				return this.forceRewriteRequest(userId, requestVo, requestVo.getParam());
			} else if (null != requestVo.getParams() && !requestVo.getParams().isEmpty()) {
				// 多个
				String eMsg = "";
				for (String param : requestVo.getParams()) {
					RequestVo newVo = new RequestVo();
					newVo.setUrl(requestVo.getUrl());
					newVo.setMethod(requestVo.getMethod());
					newVo.setParam(param);
					R r1 = this.forceRewriteRequest(userId, newVo, param);
					if (1 != r1.getCode()) {
						eMsg += "\n" + r1.getMsg();
					}
				}
				return R.ok().put("msg", eMsg);
			}
		}
		return HttpRequest.apiRequest(userId, requestVo);
	}

	@Override
	public R getUserMessageList(Integer userId, PageForm form) {

		final Integer TYPE_SITE_USER_INFO = 1;
		final Integer TYPE_SITE_WEB_NOTICE = 2;
		final Integer SEND_TYPE_USERLIST = 0;
		final Integer SEND_TYPE_GROUPLIST = 1;
		final Integer SEND_TYPE_ALLUSER = 2;
		Integer page_n = 1;
		if (null != form.getPagenum()) {
			page_n = form.getPagenum();
		}
		Integer page_s = 20;
		if (null != form.getPagesize()) {
			page_s = form.getPagesize();
		}
		List<Integer> del_id_list = new ArrayList<>();

		List<TbMessageRelationEntity> del_r_list = tbMessageRelationDao
				.selectList(new QueryWrapper<TbMessageRelationEntity>().eq("user_id", userId).eq("read_status", 2));
		if (del_r_list.size() > 0) {
			del_id_list = del_r_list.stream().map(t -> t.getMessageId()).collect(Collectors.toList());
		}

		IPage<TbMessageEntity> page = tbMessageDao.selectPage(
				new Page<TbMessageEntity>(page_n, page_s),
				new QueryWrapper<TbMessageEntity>()
						.notIn(del_id_list.size() > 0, "id", del_id_list)
						.eq("type", TYPE_SITE_USER_INFO)
						.eq("status", 1)
						.orderByDesc("id")
						.and(q -> q.eq("send_type", SEND_TYPE_ALLUSER)
								.or(qq -> qq.eq("send_type", SEND_TYPE_USERLIST).like("send_obj", "," + userId + ",")))
						.like(StringUtils.isNotBlank(form.getSearch_key1()), "title", form.getSearch_key1())
						.like(StringUtils.isNotBlank(form.getSearch_key2()), "content", form.getSearch_key2()));
		page.getRecords().forEach(item -> {
			TbMessageRelationEntity msg_rel = tbMessageRelationDao.selectOne(new QueryWrapper<TbMessageRelationEntity>()
					.eq("message_id", item.getId()).eq("user_id", userId).last("limit 1"));
			if (null != msg_rel) {
				item.setReadStatus(msg_rel.getReadStatus());
			} else {
				item.setReadStatus(0);
			}
		});
		return R.ok().put("data", new PageUtils(page));
	}

	@Override
	public R deleteUserMessage(Long userId, Map param) {
		if (!param.containsKey("ids")) {
			return R.error("[ids]为空");
		}
		String ids = param.get("ids").toString();
		Integer success_sum = 0;
		for (String id : ids.split(",")) {
			if (StringUtils.isBlank(id)) {
				continue;
			}
			TbMessageRelationEntity tmre = tbMessageRelationDao.selectOne(new QueryWrapper<TbMessageRelationEntity>()
					.eq("message_id", id)
					.eq("user_id", userId)
					.last("limit 1"));
			if (null != tmre) {
				tmre.setReadStatus(2);
				tbMessageRelationDao.updateById(tmre);
				success_sum++;
			} else {
				tmre = new TbMessageRelationEntity();
				tmre.setMessageId(Integer.parseInt(id));
				tmre.setUserId(userId);
				tmre.setReadStatus(2);
				tbMessageRelationDao.insert(tmre);

			}

		}
		return R.ok().put("data", success_sum);
	}

	@Override
	public R viewUserMessageDetail(Integer userId, String id) {
		TbMessageEntity message = tbMessageDao.selectById(id);
		if (null != message) {
			TbMessageRelationEntity msg_rel = tbMessageRelationDao.selectOne(new QueryWrapper<TbMessageRelationEntity>()
					.eq("message_id", id).eq("user_id", userId).last("limit 1"));
			if (null != msg_rel) {
				// 存在修改为已读
				msg_rel.setReadStatus(1);
				String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
				Integer time_s = Integer.valueOf(timestamp);
				msg_rel.setReadTime(time_s);
				tbMessageRelationDao.updateById(msg_rel);
			} else {
				msg_rel = new TbMessageRelationEntity();
				msg_rel.setMessageId(Integer.valueOf(id));
				msg_rel.setUserId(Long.parseLong(userId.toString()));
				msg_rel.setReadStatus(1);
				String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
				Integer time_s = Integer.valueOf(timestamp);
				msg_rel.setReadTime(time_s);
				tbMessageRelationDao.insert(msg_rel);
			}
			message.setReadStatus(1);
			return R.ok().put("data", message);
		}
		return R.error("消息为空");
	}

	@Override
	public R userChangePassWord(Long userId, ModifyPwdForm form) {

		// 表单校验
		ValidatorUtils.validateEntity(form);

		boolean captcha = appCaptchaService.validate(form.getUuid(), form.getCode());
		if (!captcha) {
			return R.error("图形验证码不正确");
		}

		if (StaticVariableUtils.demoIp.equals(StaticVariableUtils.authMasterIp)) {
			return R.error("验示站不可修改密码");
		}

		TbUserEntity user = this.getById(userId);
		if (null != user) {
			if (user.getPassword().equals(DigestUtils.sha256Hex(form.getO_password()))) {
				user.setPassword(DigestUtils.sha256Hex(form.getN_password()));
				this.updateById(user);
				// tbLogService.FrontUserWriteLog(user.getUserId(),LogTypeEnum.OTHER_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
				return R.ok();
			}
		}
		return R.error("修改密码失败!");
	}

	@Override
	public R bindMailOrMobile(TbUserEntity user, BindForm form) {
		ValidatorUtils.validateEntity(form);
		if (StringUtils.isBlank(form.getMail()) && StringUtils.isBlank(form.getMobile())) {
			return R.error("找回方式错误！");
		}
		if (StringUtils.isNotBlank(form.getMail())) {
			String mail_code = redisUtils.get(form.getMail());
			if (StringUtils.isNotBlank(mail_code) && mail_code.equals(form.getCode())) {
				user.setMail(form.getMail());
				this.updateById(user);
				tbLogService.frontUserWriteLog(user.getUserId(), LogTypeEnum.OTHER_LOG.getId(), "邮箱绑定", "");
				return R.ok();
			} else {
				return R.error("验证码错误[mail]");
			}
		} else if (StringUtils.isNotBlank(form.getMobile())) {
			String mobile_code = redisUtils.get(form.getMobile());
			if (StringUtils.isNotBlank(mobile_code) && mobile_code.equals(form.getCode())) {
				user.setMobile(form.getMobile());
				this.updateById(user);
				tbLogService.frontUserWriteLog(user.getUserId(), LogTypeEnum.OTHER_LOG.getId(), "手机绑定", "");
				return R.ok();
			} else {
				return R.error("验证码错误[mobile]");
			}
		}
		return R.error("参数错误");
	}

	@Override
	public R bindByWechat(Integer userId) {
		String eMsg = "";
		try {
			String RandomStr = String.valueOf(RandomUtil.randomInt(100000, 999999));
			redisUtils.set(RandomStr, userId, 600);
			String wechatLoginUrl = WXLoginFactory.build().wechatLogin_req_url(RandomStr, null);
			return R.ok().put("data", wechatLoginUrl);
		} catch (Exception e) {
			eMsg = e.getMessage();
			e.printStackTrace();
		}
		return R.error(eMsg);
	}

	@Override
	public R loginByWechatOrRegister(WechatBindForm form) {
		ValidatorUtils.validateEntity(form);
		if (StringUtils.isBlank(form.getMail()) && StringUtils.isBlank(form.getMobile())) {
			return R.error("[mail][mobile]参数缺失！");
		}
		String rCode = "";
		TbUserEntity user = null;
		if (StringUtils.isNotBlank(form.getMail())) {
			rCode = redisUtils.get(form.getMail());
			user = this.queryByEmail(form.getMail());
		} else if (StringUtils.isNotBlank(form.getMobile())) {
			rCode = redisUtils.get(form.getMobile());
			user = this.queryByMobile(form.getMail());
		}
		if (StringUtils.isBlank(rCode) || !rCode.equals(form.getCode())) {
			return R.error("验证码错误！");
		}
		if (null == user) {
			// 无mail 用户---创建用户
			user = new TbUserEntity();
			if (StringUtils.isNotBlank(form.getMail())) {
				user.setMail(form.getMail());
			} else if (StringUtils.isNotBlank(form.getMobile())) {
				user.setMobile(form.getMobile());
			}
			user.setWechatOpenid(form.getOpenId());
			user.setLoginType("wechat-reg-bind");
			this.save(user);
			AppUserTokenVo avo = generateAppUserTokenVo(user.getUserId(), false, "", "wechat", true);
			return R.ok().put("data", avo.getToken()).put("no_pwd", 1).put("tips", "password is null");
		} else {
			user.setWechatOpenid(form.getOpenId());
			this.updateById(user);
			AppUserTokenVo avo = generateAppUserTokenVo(user.getUserId(), false, "", "wechat", true);
			return R.ok().put("data", avo.getToken());
		}
	}

	@Override
	public R wechatLoginCallbackV2(String code, String state) {
		String token = "";
		String eMsg = "";
		if (StringUtils.isNotBlank(code)) {
			try {
				JSONObject jsonObject = WXLoginFactory.build().wechatLoginCallback(code);
				if (null != jsonObject) {
					String openid = jsonObject.getString("openid");
					if (StringUtils.isNotBlank(openid)) {
						if ("0666666".equals(state)) {
							// wechat 登录--(未注册0-注册登录)
							// System.out.println(jsonObject.toJSONString());
							// {"country":"","unionid":"oQ_a86g4WFHXaiVNMBenF2FyqYw8","province":"","city":"","openid":"oakpe5zo3-4uk7RYzHCkqLrU0_VY","sex":0,"nickname":"……","headimgurl":"https://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83erL2wiaQ4FjnV5NvMrxQmYVzbahbgWdjceWlWuxabJUj5PfFBGlTOFD40uYt6BhFkzC0EHib9eBobDg/132","language":"","privilege":[]}
							TbUserEntity user = this.getOne(
									new QueryWrapper<TbUserEntity>().eq("wechat_openid", openid).last("limit 1"));
							if (null != user) {
								if (0 == user.getStatus()) {
									throw new RRException("账户被禁用");
								}
								// wechat login
								user.setLoginType("wechat");
								this.updateById(user);
								AppUserTokenVo avo = generateAppUserTokenVo(user.getUserId(), false, "", "wechat",
										true);
								return R.ok().put("data", avo.getToken()).put("step", 2).put("tips", "wechat login");
							} else {
								// wechat reg login
								user = new TbUserEntity();
								user.setLoginType("wechat_reg_login");
								user.setUsername(jsonObject.getString("nickname"));
								user.setWechatOpenid(openid);
								// 由前端重新注册 ，不直接注册账号
								// userService.save(user);
								// userService.updateById(user);
								// tbUserDao.insert(user);
								return R.ok().put("data", user).put("step", 1).put("tips",
										"need register or bind account");
							}
						} else {
							// 是绑定USER
							String userid = redisUtils.get(state);
							if (null != userid && StringUtils.isNotBlank(userid)) {
								TbUserEntity user = this
										.getOne(new QueryWrapper<TbUserEntity>().eq("user_id", userid).last("limit 1"));
								if (null != user) {
									user.setWechatOpenid(openid);
									this.updateById(user);
									return R.ok().put("data", user.getUserId()).put("step", 2).put("tips",
											"wechat bind user complete");
								}
							}
						}

					}
				}
			} catch (Exception e) {
				eMsg = e.getMessage();
				e.printStackTrace();
			}
		}
		return R.error(eMsg);
	}

	@Override
	public R nullPasswordSetPassword(Long userId, String pwd) {
		TbUserEntity userEntity = this.getById(userId);
		if (null != userEntity.getPassword()) {
			return R.error("已设置密码，不可重复设置！");
		}
		userEntity.setPassword(DigestUtils.sha256Hex(pwd));
		this.updateById(userEntity);
		return R.ok();
	}

	@Override
	public R nullPropertyPayPasswordSetPassword(Long userId, String pwd) {
		TbUserEntity userEntity = this.getById(userId);
		if (null != userEntity.getPropertyPayPassword()) {
			return R.error("已设置密码，不可重复设置！");
		}
		userEntity.setPropertyPayPassword(DigestUtils.sha256Hex(pwd));
		this.updateById(userEntity);
		return R.ok();
	}

	@Override
	public R userResetPropertyPayPassword(Long userId, ResetPayPasswordForm form) {
		TbUserEntity user = this.getById(userId);
		if (StringUtils.isBlank(form.getMail()) && StringUtils.isBlank(form.getMobile())) {
			return R.error("找回方式错误！");
		}
		if (StringUtils.isNotBlank(form.getMail())) {
			String mail_code = redisUtils.get(form.getMail());
			if (StringUtils.isNotBlank(mail_code) && mail_code.equals(form.getCode())) {
				user.setPropertyPayPassword(DigestUtils.sha256Hex(form.getNewPayPassword()));
				this.updateById(user);
				// tbLogService.FrontUserWriteLog(user.getUserId(),LogTypeEnum.OTHER_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
				return R.ok();
			} else {
				return R.error("验证码错误[mail]");
			}
		} else if (StringUtils.isNotBlank(form.getMobile())) {
			String mobile_code = redisUtils.get(form.getMobile());
			if (StringUtils.isNotBlank(mobile_code) && mobile_code.equals(form.getCode())) {
				user.setPropertyPayPassword(DigestUtils.sha256Hex(form.getNewPayPassword()));
				this.updateById(user);
				// tbLogService.FrontUserWriteLog(user.getUserId(),LogTypeEnum.OTHER_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
				return R.ok();
			} else {
				return R.error("验证码错误[mobile]");
			}
		}
		return R.ok();
	}

	private String priHtmlText(String token) {
		String Htmlstr = "";
		if (StringUtils.isNotBlank(token)) {
			Htmlstr = "<!DOCTYPE html>" +
					"<html lang=\"zh-cn\">" +
					"<head>" +
					"	<meta charset=\"utf-8\"/>" +
					"</head>" +
					"<body>" +
					"	<script type=\"text/javascript\">" +
					"       sessionStorage.setItem('token', '" + token + "'); " +
					"	    location.href = \"/users/#/home\";" +
					"	</script>" +
					"</body>" +
					"</html>";
		} else {
			Htmlstr = "<!DOCTYPE html>" +
					"<html lang=\"zh-cn\">" +
					"<head>" +
					"	<meta charset=\"utf-8\"/>" +
					"</head>" +
					"<body>" +
					"	<script type=\"text/javascript\">" +
					"	    location.href = \"/users/#/home\";" +
					"	</script>" +
					"</body>" +
					"</html>";
		}

		return Htmlstr;
	}

	@Override
	public void loginByWechatCallbackV1(HttpServletResponse response, String code, String state) {
		String token = "";
		if (StringUtils.isNotBlank(code)) {
			try {
				JSONObject jsonObject = WXLoginFactory.build().wechatLoginCallback(code);
				if (null != jsonObject) {
					String openid = jsonObject.getString("openid");
					if (StringUtils.isNotBlank(openid)) {
						if ("0666666".equals(state)) {
							// wechat 登录--未注册0-注册登录
							// System.out.println(jsonObject.toJSONString());
							// {"country":"","unionid":"oQ_a86g4WFHXaiVNMBenF2FyqYw8","province":"","city":"","openid":"oakpe5zo3-4uk7RYzHCkqLrU0_VY","sex":0,"nickname":"……","headimgurl":"https://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83erL2wiaQ4FjnV5NvMrxQmYVzbahbgWdjceWlWuxabJUj5PfFBGlTOFD40uYt6BhFkzC0EHib9eBobDg/132","language":"","privilege":[]}
							TbUserEntity user = this.getOne(
									new QueryWrapper<TbUserEntity>().eq("wechat_openid", openid).last("limit 1"));
							if (null != user) {
								// wechat login
								if (0 == user.getStatus()) {
									throw new RRException("账户被禁用");
								}
								user.setLoginType("wechat");
								this.updateById(user);
								AppUserTokenVo avo = generateAppUserTokenVo(user.getUserId(), false, "", "wechat",
										true);
								response.setHeader("Cache-Control", "no-store, no-cache");
								response.setContentType("text/html; charset=utf-8");
								ServletOutputStream out = response.getOutputStream();
								out.print(priHtmlText(avo.getToken()));
								IOUtils.closeQuietly(out);
							} else {
								// wechat reg login
								user = new TbUserEntity();
								user.setUsername(jsonObject.getString("nickname"));
								user.setWechatOpenid(openid);
								// userService.save(user);
								// return R.ok().put("data",user).put("step",1);
							}

						} else {
							// 是绑定USER
							String userid = redisUtils.get(state);
							if (StringUtils.isNotBlank(userid)) {
								TbUserEntity user = this.getOne(new QueryWrapper<TbUserEntity>().isNull("wechat_openid")
										.eq("user_id", userid).last("limit 1"));
								if (null != user) {
									user.setWechatOpenid(openid);
									this.updateById(user);
									response.setHeader("Cache-Control", "no-store, no-cache");
									response.setContentType("text/html; charset=utf-8");
									ServletOutputStream out = response.getOutputStream();
									out.print(priHtmlText(token));
									IOUtils.closeQuietly(out);
								}
							}
						}

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public R userIndexDataStatistics(Long userId) {
		Map map = new HashMap();
		Long siteCount = tbSiteDao.selectCount(new QueryWrapper<TbSiteEntity>().eq("user_id", userId));
		map.put("siteCount", siteCount);
		Long streamCount = tbStreamProxyDao
				.selectCount(new QueryWrapper<TbStreamProxyEntity>().eq("user_id", userId));
		map.put("streamCount", streamCount);
		Long suitCount = cdnSuitDao.selectCount(new QueryWrapper<CdnSuitEntity>().eq("user_id", userId)
				.eq("status", 1).eq("suit_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId()));
		map.put("suitCount", suitCount);
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		map.put("ip", IPUtils.getIpAddr(request));
		map.put("lastLoginLog", tbLogService.getLastLogin(userId));
		Long certCount = tbcertifyDao.selectCount(new QueryWrapper<TbCertifyEntity>()
				.eq("user_id", userId)
				.and(q -> q.eq("status", TbCertifyStatusEnum.SUCCESS.getId()).or().eq("status",
						TbCertifyStatusEnum.USER.getId())));
		map.put("certCount", certCount);
		return R.ok().put("data", map);
	}

	@Override
	public R bindByGoogleAuth(Integer userId, GoogleAuthBindForm form) {
		ValidatorUtils.validateEntity(form);
		TbUserEntity user = this.getById(userId);
		if (null == user) {
			return R.error("用户不存在");
		}
		if (StringUtils.isNotBlank(user.getGoogleAuthSecretKey())) {
			return R.error("绑定失败，已绑定");
		}
		if (!GoogleAuthUtils.checkCode(form.getSecretKey(), form.getCode())) {
			return R.error("绑定失败，CODE错误");
		}
		this.update(null, new UpdateWrapper<TbUserEntity>()
				.eq("user_id", userId)
				.set("google_auth_secret_key", form.getSecretKey())
				.set("google_auth_status", 1));
		return R.ok();
	}

	@Override
	public R statusWithGoogleAuthSave(Long userId, int status) {
		TbUserEntity user = this.getById(userId);
		if (null == user) {
			return R.error("用户不存在");
		}
		if (StringUtils.isBlank(user.getGoogleAuthSecretKey())) {
			return R.error("未绑定");
		}
		this.update(null, new UpdateWrapper<TbUserEntity>()
				.eq("user_id", userId)
				.set("google_auth_status", status));
		return R.ok();
	}

	@Override
	public R wechatRegisterAndLogin(String nickname, String openid) {
		TbUserEntity user = new TbUserEntity();
		user.setLoginType("wechat_reg_login");
		user.setUsername(nickname);
		user.setWechatOpenid(openid);
		this.save(user);
		AppUserTokenVo avo = generateAppUserTokenVo(user.getUserId(), false, "", "wechat", true);
		return R.ok().put("data", avo.getToken()).put("no_pwd", 1);
	}

	@Override
	public R updateUserModules(TbUserEntity user, UpdateAppUserForm form) {
		this.update(null, new UpdateWrapper<TbUserEntity>()
				.eq("user_id", user.getUserId())
				.set("white_ip_status", form.getWhiteIpStatus()));
		return R.ok();
	}

}
