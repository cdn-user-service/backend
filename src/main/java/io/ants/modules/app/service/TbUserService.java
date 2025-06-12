/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.service;


import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.*;
import io.ants.modules.sys.form.SaveTbUserInfoFrom;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 用户
 *
 * @author Mark sunlightcs@gmail.com
 */
public interface TbUserService extends IService<TbUserEntity> {



	/**
	 * 用户登录
	 * @param form    登录表单
	 * @return        返回用户ID
	 */
	long login(LoginForm form);


	String key2userIds(String key);

	R checkTbUserInfo(String user);

	PageUtils userList(Integer page, Integer limit, String user);


	TbUserEntity userDetail(Long userId);


	void delete(String ids);

	TbUserEntity saveUserByUserForm(SaveTbUserInfoFrom userForm);

	TbUserEntity modifyUserBaseInfo(Long userId, String username, String mobile, String mail, String password,Integer status);


	TbUserEntity createUser(String username, String mobile, String mail, String password);


	TbUserEntity queryByMobile(String mobile);

	TbUserEntity queryByEmail(String mail);

	TbUserEntity queryByUserName(String userName);

	/**
	 * 用户登录
	 *
	 * @param username
	 * @param password
	 * @return        返回用户ID
	 */
	long loginByUsername(String username,String password);


	long loginByMail(String mail,String password );


	long loginByMobil(String mobile,String password);


	R loginByGoogleAuthCode(LoginByGoogleAuthForm form);


	R userLogin(LoginForm form);

	R loginByAccessToken(String access_token);

	R userRegister(RegisterForm form);

	R userRegisterWithNoVerifyCode(RegisterForm form);

	R sendVerifyByEmailCode(String mail);

	R sendVerifyBySmsCode(String mobile, String uuid, String captcha_code);

	R forgetPassWord(PasswordForgetForm form);

	R bindDnsAccessToken(TbUserEntity user, BindDnsForm param);

	R getUserInfoDetail(TbUserEntity user);

	R proxyRequest(Long userId, RequestVo requestVo);

	R getUserMessageList(Integer userId, PageForm form);

	R deleteUserMessage(Long userId, Map param);

	R viewUserMessageDetail(Integer userId, String id);

	R userChangePassWord(Long userId, ModifyPwdForm form);

	R bindMailOrMobile(TbUserEntity user, BindForm form);

	R bindByWechat(Integer userId);

	R loginByWechatOrRegister(WechatBindForm form);

	R wechatLoginCallbackV2(String code, String state);

	R nullPasswordSetPassword(Long userId,String pwd);

	R nullPropertyPayPasswordSetPassword(Long userId, String pwd);

	R userResetPropertyPayPassword(Long userId, ResetPayPasswordForm form);

	void loginByWechatCallbackV1(HttpServletResponse response, String code, String state);

	R userIndexDataStatistics(Long userId);

	R bindByGoogleAuth(Integer userId, GoogleAuthBindForm form);

	R statusWithGoogleAuthSave(Long userId, int status);

	R wechatRegisterAndLogin(String nickname, String openid);

    R updateUserModules(TbUserEntity user, UpdateAppUserForm form);
}
