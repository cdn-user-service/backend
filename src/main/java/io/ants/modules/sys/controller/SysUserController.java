/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.utils.Constant;
import io.ants.common.utils.GoogleAuthUtils;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.AbstractAssert;
import io.ants.common.validator.ValidatorUtils;
import io.ants.common.validator.group.AddGroup;
import io.ants.common.validator.group.UpdateGroup;
import io.ants.modules.app.entity.CdnLoginWhiteIpEntity;
import io.ants.modules.app.service.CdnLoginWhiteIpService;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.form.PasswordForm;
import io.ants.modules.sys.form.UpdateSysUserModuleForm;
import io.ants.modules.sys.service.SysUserRoleService;
import io.ants.modules.sys.service.SysUserService;
import io.ants.modules.app.form.DeleteIdsForm;
import io.ants.modules.app.form.GoogleAuthBindForm;
import io.ants.modules.app.form.QueryLoginWhiteIpForm;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统用户
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysUserRoleService sysUserRoleService;
	@Autowired
	private CdnLoginWhiteIpService whiteIpService;


	/**
	 * 所有用户列表
	 */
	@PostMapping("/list")
	@RequiresPermissions("sys:user:list")
	public R list(@RequestBody Map<String, Object> params){
		//只有超级管理员，才能查看所有管理员列表
		if(getSysUserId() != Constant.SUPER_ADMIN){
			params.put("createUserId", getSysUserId());
		}
		PageUtils page = sysUserService.queryPage(params);

		return R.ok().put("page", page);
	}
	
	/**
	 * 获取当前登录的用户信息
	 */
	@GetMapping("/info")
	public R info(){
		return R.ok().put("user", sysGetSysUser());
	}
	
	/**
	 * 修改登录用户密码
	 */
	@SysLog("修改密码")
	@PostMapping("/password")
	public R password(@RequestBody PasswordForm form){
		AbstractAssert.isBlank(form.getNewPassword(), "新密码不为能空");
		
		//sha256加密
		String password = new Sha256Hash(form.getPassword(), sysGetSysUser().getSalt()).toHex();
		//sha256加密
		String newPassword = new Sha256Hash(form.getNewPassword(), sysGetSysUser().getSalt()).toHex();
				
		//更新密码
		boolean flag = sysUserService.updatePassword(getSysUserId(), password, newPassword);
		if(!flag){
			return R.error("原密码不正确");
		}
		return R.ok();
	}

	@SysLog("sys-user-module修改")
	@PostMapping("/sys-user-module/update")
	public  R updateUserModule( @RequestBody UpdateSysUserModuleForm form) {
		long sysUserId= getSysUserId();
		return sysUserService.updateSysUserModules(sysUserId,form);

	}
	
	/**
	 * 用户信息
	 */
	@GetMapping("/info/{userId}")
	@RequiresPermissions("sys:user:info")
	public R info(@PathVariable("userId") Long userId){
		SysUserEntity user = sysUserService.getById(userId);
		
		//获取用户所属的角色列表
		List<Long> roleIdList = sysUserRoleService.queryRoleIdList(userId);
		user.setRoleIdList(roleIdList);
		
		return R.ok().put("user", user);
	}



	/**
	 * 保存用户
	 */
	@SysLog("保存用户")
	@PostMapping("/save")
	@RequiresPermissions("sys:user:save")
	public R save(@RequestBody SysUserEntity user){
		ValidatorUtils.validateEntity(user, AddGroup.class);
		String userName=user.getUsername();
		String email=user.getEmail();
		String mobile=user.getMobile();
		int count=sysUserService.count(new QueryWrapper<SysUserEntity>()
				.eq(StringUtils.isNotBlank(userName),"username",userName)
				.or()
				.eq(StringUtils.isNotBlank(email),"email",email)
				.or()
				.eq(StringUtils.isNotBlank(mobile),"mobile",mobile)
		);
		if(count>0){
			return R.error("存在重复的用户数据！");
		}
		user.setCreateUserId(getSysUserId());
		sysUserService.saveUser(user);
		return R.ok();
	}
	
	/**
	 * 修改用户
	 */
	@SysLog("修改用户")
	@PostMapping("/update")
	@RequiresPermissions("sys:user:update")
	public R update(@RequestBody SysUserEntity user){
		checkDemoModify();
		ValidatorUtils.validateEntity(user, UpdateGroup.class);
		String userName=user.getUsername();
		String email=user.getEmail();
		String mobile=user.getMobile();
		int count=sysUserService.count(new QueryWrapper<SysUserEntity>()
						.and(q->q.eq(StringUtils.isNotBlank(userName),"username",userName)
								.or()
								.eq(StringUtils.isNotBlank(email),"email",email)
								.or()
								.eq(StringUtils.isNotBlank(mobile),"mobile",mobile))
						.ne("user_id",user.getUserId())
		);
		if(count>0){
			return R.error("存在重复的用户数据！");
		}
		user.setCreateUserId(getSysUserId());
		sysUserService.update(user);
		return R.ok();
	}
	
	/**
	 * 删除用户
	 */
	@SysLog("删除用户")
	@PostMapping("/delete")
	@RequiresPermissions("sys:user:delete")
	public R delete(@RequestBody Map params){
		checkDemoModify();
		if(!params.containsKey("ids")){
			return R.error("参数缺失");
		}
		String ids=params.get("ids").toString();
		String[]  id_s=ids.split(",");
		Long[] userIds = new Long[id_s.length];
		for(int i=0;i<id_s.length;i++){
			userIds[i] = Long.parseLong(id_s[i]);
		}
		if(ArrayUtils.contains(userIds, "1")){
			return R.error("系统管理员不能删除");
		}
		
		if(ArrayUtils.contains(userIds, getSysUserId())){
			return R.error("当前用户不能删除");
		}
		sysUserService.deleteBatch(userIds);
		return R.ok();
	}




	@PostMapping("/google-auth/bind")
	public R googleAuthBind( @RequestBody GoogleAuthBindForm form) {
		long sysUserId= getSysUserId();
		return sysUserService.bindByGoogleAuth(sysUserId, form);
	}


	@GetMapping("/google-auth/qrcode/view")
	public R viewGoogleAuthData(){
		long sysUserId= getSysUserId();
		return R.ok().put("data", GoogleAuthUtils.buildOneData(sysUserId));
	}


	@GetMapping("/google-auth/status")
	public R statusWithGoogleAuth(int status){
		long sysUserId= getSysUserId();
		return sysUserService.statusWithGoogleAuthSave(sysUserId,status);
	}


	@PostMapping("/login-white-ip/list")
	public R loginWhiteIPList(@RequestBody QueryLoginWhiteIpForm form){
		long sysUserId= getSysUserId();
		return whiteIpService.loginWhiteIPList(UserTypeEnum.MANAGER_TYPE.getId(),sysUserId,form);
	}



	@PostMapping("/login-white-ip/save")
	public R loginWhiteIPSave(@RequestBody CdnLoginWhiteIpEntity entity){
		long sysUserId= getSysUserId();
		return whiteIpService.saveLoginWhiteIp(UserTypeEnum.MANAGER_TYPE.getId(),sysUserId,entity);
	}


	@PostMapping("/login-white-ip/delete")
	public R loginWhiteIPDelete(@RequestBody DeleteIdsForm form){
		long sysUserId= getSysUserId();
		return whiteIpService.deleteLoginWhiteIps(UserTypeEnum.MANAGER_TYPE.getId(),sysUserId,form.getIds());
	}
}
