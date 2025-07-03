/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.controller;

import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.sys.entity.SysUserEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Controller公共组件
 *
 * @author Mark sunlightcs@gmail.com
 */
public abstract class AbstractController {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected SysUserEntity sysGetSysUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SysUserEntity) {
			return (SysUserEntity) authentication.getPrincipal();
		}
		return null;
	}

	protected Long getSysUserId() {
		return sysGetSysUser().getUserId();
	}

	protected void checkDemoModify() {
		if (StaticVariableUtils.demoIp.equals(StaticVariableUtils.authMasterIp)) {
			String username = sysGetSysUser().getUsername();
			if (!Arrays.asList(StaticVariableUtils.ANTS_USER).contains(username)) {
				throw new RRException("演示站不可修改");
			}
		}
	}

	/**
	 * 更新公共信息 至 REDIS
	 */
	protected void recordInfo(RedisUtils redisUtils) {
		final String MAGIC_LOCALHOST = "localhost";
		final String MAGIC_127 = "127.0.0.1";
		if (StringUtils.isNotBlank(StaticVariableUtils.masterWebSeverName)) {
			return;
		}

		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		if (null == request) {
			return;
		}
		if (MAGIC_LOCALHOST.equals(request.getServerName())) {
			return;
		}
		if (MAGIC_127.equals(request.getServerName())) {
			return;
		}
		StaticVariableUtils.masterWebSeverName = request.getServerName();
		StaticVariableUtils.MasterWebPort = request.getServerPort();
		redisUtils.set("public:master:info:servername", StaticVariableUtils.masterWebSeverName, -1);
		redisUtils.set("public:master:info:port", StaticVariableUtils.MasterWebPort, -1);
		String fStr = StaticVariableUtils.masterWebSeverName + ":" + StaticVariableUtils.MasterWebPort + "/antsxdp";
		redisUtils.set("public:master:api:addNodePath", fStr + StaticVariableUtils.ADD_NODE_PATH, -1);
		redisUtils.set("public:master:api:errorFeedbacks", fStr + StaticVariableUtils.FEEDBACKS, -1);
		StaticVariableUtils.MasterProtocol = HttpRequest.getSchemeByServer(StaticVariableUtils.masterWebSeverName);
		if (StringUtils.isBlank(StaticVariableUtils.checkNodeInputToken)
				&& StringUtils.isNotBlank(StaticVariableUtils.authMasterIp)) {
			StaticVariableUtils.checkNodeInputToken = HashUtils
					.md5ofString("check_" + StaticVariableUtils.authMasterIp);
		}

	}
}
