/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.common.aspect;

import com.google.gson.Gson;
import io.ants.common.annotation.SysLog;
import io.ants.common.utils.HttpContextUtils;
import io.ants.common.utils.IPUtils;
import io.ants.modules.sys.entity.SysLogEntity;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.enums.LogTypeEnum;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.SysLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * 系统日志，切面处理类
 *
 * @author Mark sunlightcs@gmail.com
 */
@Aspect
@Component
public class SysLogAspect {
	@Autowired
	private SysLogService sysLogService;

	@Pointcut("@annotation(io.ants.common.annotation.SysLog)")
	public void logPointCut() {

	}

	@Around("logPointCut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		long beginTime = System.currentTimeMillis();
		// 执行方法
		Object result = point.proceed();
		// 执行时长(毫秒)
		long time = System.currentTimeMillis() - beginTime;

		// 保存日志
		saveSysLog(point, time);

		return result;
	}

	private void saveSysLog(ProceedingJoinPoint joinPoint, long time) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		SysLogEntity sysLog = new SysLogEntity();
		SysLog syslog = method.getAnnotation(SysLog.class);
		if (syslog != null) {
			// 注解上的描述
			sysLog.setOperation(syslog.value());
		}

		// 请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		String final_method = className + "." + methodName + "()";
		if (final_method.length() > 128) {
			sysLog.setMethod(final_method.substring(0, 127));
		} else {
			sysLog.setMethod(final_method);
		}

		if (-1 != final_method.indexOf("TbUserController.modify()")
				|| -1 != final_method.indexOf(".SysConfigController.")
				|| -1 != final_method.indexOf(".SysMenuController.")) {
			sysLog.setLogType(LogTypeEnum.OPERATION_LOG.getId());
		} else if (-1 != final_method.indexOf("PayController.adminRecharge()")) {
			sysLog.setLogType(LogTypeEnum.FINANCE_LOG.getId());
		} else if (-1 != final_method.indexOf("pushDataToNode()") || -1 != final_method.indexOf("pushSiteConfToNode()")
				|| -1 != final_method.indexOf("runTask()")) {
			sysLog.setLogType(LogTypeEnum.OPERATION_LOG.getId());
		} else if (-1 != final_method.indexOf("CdnProductController")) {
			sysLog.setLogType(LogTypeEnum.PRODUCT_LOG.getId());
		} else if (-1 != final_method.indexOf("CdnSiteController")) {
			sysLog.setLogType(LogTypeEnum.OPERATION_LOG.getId());
		} else {
			sysLog.setLogType(LogTypeEnum.OTHER_LOG.getId());
		}

		// 请求的参数
		Object[] args = joinPoint.getArgs();
		try {
			String params = new Gson().toJson(args);
			if (params.length() > 128) {
				sysLog.setParams(params.substring(0, 127));
			} else {
				sysLog.setParams(params);
			}
		} catch (Exception e) {

		}

		// 获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		// 设置IP地址
		sysLog.setIp(IPUtils.getIpAddr(request));

		sysLog.setUserType(UserTypeEnum.MANAGER_TYPE.getId());
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.getPrincipal() instanceof SysUserEntity) {
			SysUserEntity user = (SysUserEntity) authentication.getPrincipal();
			sysLog.setUserId(user.getUserId());
			sysLog.setUsername(user.getUsername());
		}
		sysLog.setTime(time);
		sysLog.setCreateDate(new Date());
		// 保存系统日志
		sysLogService.save(sysLog);
	}
}
