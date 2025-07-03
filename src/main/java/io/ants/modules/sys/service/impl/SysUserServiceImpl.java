/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有,侵权必究！
 */

package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.form.GoogleAuthBindForm;
import io.ants.modules.app.form.LoginByGoogleAuthForm;
import io.ants.modules.sys.dao.SysUserDao;
import io.ants.modules.sys.entity.SysRoleEntity;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.entity.SysUserRoleEntity;
import io.ants.modules.sys.form.UpdateSysUserModuleForm;
import io.ants.modules.sys.service.SysRoleService;
import io.ants.modules.sys.service.SysUserRoleService;
import io.ants.modules.sys.service.SysUserService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统用户
 *
 * @author Mark sunlightcs@gmail.com
 */
@Service("sysUserService")
public class SysUserServiceImpl extends ServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {
	@Autowired
	private SysUserRoleService sysUserRoleService;
	@Autowired
	private SysRoleService sysRoleService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String username = (String) params.get("username");
		Long createUserId = (Long) params.get("createUserId");

		IPage<SysUserEntity> page = this.page(
				new Query<SysUserEntity>().getPage(params),
				new QueryWrapper<SysUserEntity>()
						.like(StringUtils.isNotBlank(username), "username", username)
						.eq(createUserId != null, "create_user_id", createUserId));

		page.getRecords().forEach(item -> {
			List<SysUserRoleEntity> rlist = sysUserRoleService
					.list(new QueryWrapper<SysUserRoleEntity>().eq("user_id", item.getUserId()));
			List<Long> roleidlist = rlist.stream().map(t -> t.getRoleId()).collect(Collectors.toList());
			item.setRoleIdList(roleidlist);
			List<Map<String, Object>> rolenamelist = new ArrayList<>();
			roleidlist.forEach(roleid -> {
				Map<String, Object> sysRoleMap = sysRoleService
						.getMap(new QueryWrapper<SysRoleEntity>().eq("role_id", roleid));
				rolenamelist.add(sysRoleMap);
			});
			item.setRoleNameList(rolenamelist);
		});
		return new PageUtils(page);
	}

	@Override
	public List<String> queryAllPerms(Long userId) {
		return baseMapper.queryAllPerms(userId);
	}

	@Override
	public List<Long> queryAllMenuId(Long userId) {
		return baseMapper.queryAllMenuId(userId);
	}

	@Override
	public SysUserEntity queryByUserName(String username) {
		return baseMapper.queryByUserName(username);
	}

	@SuppressWarnings("AlibabaTransactionMustHaveRollback")
	@Override
	@Transactional
	public void saveUser(SysUserEntity user) {
		user.setCreateTime(new Date());
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String encodedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);
		this.save(user);

		// 检查角色是否越权
		checkRole(user);

		// 保存用户与角色关系
		sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIdList());
	}

	@SuppressWarnings("AlibabaTransactionMustHaveRollback")
	@Override
	@Transactional
	public void update(SysUserEntity user) {
		if (StringUtils.isBlank(user.getPassword())) {
			user.setPassword(null);
		} else {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		this.updateById(user);

		// 检查角色是否越权
		checkRole(user);

		// 保存用户与角色关系
		sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIdList());
	}

	@Override
	public void deleteBatch(Long[] userId) {
		this.removeByIds(Arrays.asList(userId));
	}

	@Override
	public boolean updatePassword(Long userId, String password, String newPassword) {
		SysUserEntity user = this.getById(userId);
		if (user == null) {
			return false;
		}

		if (!passwordEncoder.matches(password, user.getPassword())) {
			return false;
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		return this.updateById(user);
	}

	@Override
	public R loginByGoogleAuthCodeGetUid(LoginByGoogleAuthForm form) {
		ValidatorUtils.validateEntity(form);
		SysUserEntity user = this.getOne(new QueryWrapper<SysUserEntity>()
				.and(q -> q.eq("username", form.getUser()).or().eq("mobile", form.getUser()).or().eq("email",
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
		return R.ok().put("data", user.getUserId());
		// return
		// R.ok(this.generateAppUserToken(user.getUserId(),false,"","googleAuth"));
	}

	@Override
	public R bindByGoogleAuth(long sysUserId, GoogleAuthBindForm form) {
		ValidatorUtils.validateEntity(form);
		SysUserEntity user = this.getById(sysUserId);
		if (null == user) {
			return R.error("用户不存在");
		}
		if (StringUtils.isNotBlank(user.getGoogleAuthSecretKey())) {
			return R.error("绑定失败，已绑定");
		}
		if (!GoogleAuthUtils.checkCode(form.getSecretKey(), form.getCode())) {
			return R.error("绑定失败，CODE错误");
		}
		this.update(null, new UpdateWrapper<SysUserEntity>()
				.eq("user_id", sysUserId)
				.set("google_auth_secret_key", form.getSecretKey())
				.set("google_auth_status", 1));
		return R.ok();
	}

	@Override
	public R statusWithGoogleAuthSave(long sysUserId, int status) {
		SysUserEntity user = this.getById(sysUserId);
		if (null == user) {
			return R.error("用户不存在");
		}
		if (StringUtils.isBlank(user.getGoogleAuthSecretKey())) {
			return R.error("未绑定");
		}
		this.update(null, new UpdateWrapper<SysUserEntity>()
				.eq("user_id", sysUserId)
				.set("google_auth_status", status));
		return R.ok();
	}

	@Override
	public R updateSysUserModules(long sysUserId, UpdateSysUserModuleForm form) {
		this.update(null, new UpdateWrapper<SysUserEntity>()
				.eq("user_id", sysUserId)
				.set("white_ip_status", form.getWhiteIpStatus()));
		return R.ok();
	}

	@Override
	public R checkSysUserInfo(String userName) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		SysUserEntity user = this.getOne(new QueryWrapper<SysUserEntity>()
				.and(q -> q.eq("username", userName).or().eq("mobile", userName).or().eq("email", userName))
				.isNotNull("google_auth_secret_key")
				.eq("google_auth_status", 1)
				.select("user_id,google_auth_secret_key")
				.last("limit 1"));
		retMap.put("google_auth_status", 0);
		if (null != user) {
			retMap.put("google_auth_status", 1);
		}
		return R.ok(retMap);
	}

	/**
	 * 检查角色是否越权
	 */
	private void checkRole(SysUserEntity user) {
		if (user.getRoleIdList() == null || user.getRoleIdList().size() == 0) {
			return;
		}
		// 如果不是超级管理员,则需要判断用户的角色是否自己创建
		if (user.getCreateUserId() == Constant.SUPER_ADMIN) {
			return;
		}

		// 查询用户创建的角色列表
		List<Long> roleIdList = sysRoleService.queryRoleIdList(user.getCreateUserId());

		// 判断是否越权
		if (!roleIdList.containsAll(user.getRoleIdList())) {
			throw new RRException("新增用户所选角色,不是本人创建");
		}
	}
}