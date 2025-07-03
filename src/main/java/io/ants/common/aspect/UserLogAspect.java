/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.common.aspect;

import com.google.gson.Gson;
import io.ants.common.annotation.UserLog;
import io.ants.common.utils.HttpContextUtils;
import io.ants.common.utils.IPUtils;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.dao.TbUserLogDao;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.entity.TbUserLogEntity;
import io.ants.modules.sys.enums.LogTypeEnum;
import io.ants.modules.sys.enums.UserTypeEnum;
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
public class UserLogAspect {
	@Autowired
	private TbUserLogDao tbUserLogDao;
	@Autowired
	private TbUserDao tbUserDao;

	@Pointcut("@annotation(io.ants.common.annotation.UserLog)")
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
		saveUserLog(point, time);

		return result;
	}

	private void saveUserLog(ProceedingJoinPoint joinPoint, long time) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		TbUserLogEntity tbUserLogEntity = new TbUserLogEntity();
		UserLog userLog = method.getAnnotation(UserLog.class);
		if (userLog != null) {
			// 注解上的描述
			tbUserLogEntity.setOperation(userLog.value());
			tbUserLogEntity.setMethod(userLog.value());
		}

		// 请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		String final_method = className + "." + methodName + "()";
		// if(final_method.length()>128){
		// tbUserLogEntity.setMethod(final_method.substring(0,127));
		// }else {
		// tbUserLogEntity.setMethod(final_method);
		// }

		if (-1 != final_method.indexOf("AppPayController")) {
			tbUserLogEntity.setLogType(LogTypeEnum.FINANCE_LOG.getId());
		} else if (-1 != final_method.indexOf("AppProductController")) {
			tbUserLogEntity.setLogType(LogTypeEnum.PRODUCT_LOG.getId());
		} else if (-1 != final_method.indexOf("AppSiteController") || -1 != final_method.indexOf("AppStreamController")
				|| -1 != final_method.indexOf("AppRewriteController")
				|| -1 != final_method.indexOf("AppCertifyController")) {
			tbUserLogEntity.setLogType(LogTypeEnum.OPERATION_LOG.getId());
		} else {
			tbUserLogEntity.setLogType(LogTypeEnum.OTHER_LOG.getId());
		}

		// 请求的参数
		Object[] args = joinPoint.getArgs();
		try {
			if (args.length > 0) {
				Object arg0 = args[0];
				if ("java.lang.Long".equals(arg0.getClass().getName())) {
					tbUserLogEntity.setUserId((Long) arg0);
					TbUserEntity user = tbUserDao.selectById(arg0.toString());
					if (null != user) {
						tbUserLogEntity.setUsername(user.getUsername());
						// tbUserLogEntity.setUserInfo(user);
					}
				}
			}
			String params = new Gson().toJson(args);
			if (params.length() > 128) {
				tbUserLogEntity.setParams(params.substring(0, 127));
			} else {
				tbUserLogEntity.setParams(params);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		// 设置IP地址
		tbUserLogEntity.setIp(IPUtils.getIpAddr(request));

		tbUserLogEntity.setUserType(UserTypeEnum.USER_TYPE.getId());

		tbUserLogEntity.setTime(time);
		tbUserLogEntity.setCreateDate(new Date());
		// 保存系统日志
		tbUserLogDao.insert(tbUserLogEntity);
	}
}
