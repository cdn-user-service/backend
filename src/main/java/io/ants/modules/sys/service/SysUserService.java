/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.form.GoogleAuthBindForm;
import io.ants.modules.app.form.LoginByGoogleAuthForm;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.form.UpdateSysUserModuleForm;

import java.util.List;
import java.util.Map;


/**
 * 系统用户
 *
 * @author Mark sunlightcs@gmail.com
 */
public interface SysUserService extends IService<SysUserEntity> {

	PageUtils queryPage(Map<String, Object> params);

	/**
	 * 查询用户的所有权限
	 * @param userId  用户ID
	 */
	List<String> queryAllPerms(Long userId);
	
	/**
	 * 查询用户的所有菜单ID
	 */
	List<Long> queryAllMenuId(Long userId);

	/**
	 * 根据用户名，查询系统用户
	 */
	SysUserEntity queryByUserName(String username);

	/**
	 * 保存用户
	 */
	void saveUser(SysUserEntity user);
	
	/**
	 * 修改用户
	 */
	void update(SysUserEntity user);
	
	/**
	 * 删除用户
	 */
	void deleteBatch(Long[] userIds);

	/**
	 * 修改密码
	 * @param userId       用户ID
	 * @param password     原密码
	 * @param newPassword  新密码
	 */
	boolean updatePassword(Long userId, String password, String newPassword);


	R loginByGoogleAuthCodeGetUid(LoginByGoogleAuthForm form);

	R bindByGoogleAuth(long sysUserId, GoogleAuthBindForm form);

	R statusWithGoogleAuthSave(long sysUserId, int status);

    R updateSysUserModules(long sysUserId, UpdateSysUserModuleForm form);

	R checkSysUserInfo(String userName);
}
