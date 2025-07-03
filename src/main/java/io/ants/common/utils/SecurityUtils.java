package io.ants.common.utils;

import io.ants.common.exception.RRException;
import io.ants.modules.sys.entity.SysUserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

/**
 * Spring Security工具类
 *
 * @author Mark sunlightcs@gmail.com
 */
public class SecurityUtils {

    public static HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }

    public static Authentication getSubject() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static SysUserEntity getUserEntity() {
        Authentication authentication = getSubject();
        if (authentication != null && authentication.getPrincipal() instanceof SysUserEntity) {
            return (SysUserEntity) authentication.getPrincipal();
        }
        return null;
    }

    public static Long getUserId() {
        SysUserEntity userEntity = getUserEntity();
        return userEntity != null ? userEntity.getUserId() : null;
    }

    public static void setSessionAttribute(Object key, Object value) {
        getSession().setAttribute(key.toString(), value);
    }

    public static Object getSessionAttribute(Object key) {
        return getSession().getAttribute(key.toString());
    }

    public static boolean isLogin() {
        Authentication authentication = getSubject();
        return authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof SysUserEntity;
    }

    public static String getKaptcha(String key) {
        Object kaptcha = getSessionAttribute(key);
        if (kaptcha == null) {
            throw new RRException("验证码已失效");
        }
        getSession().removeAttribute(key);
        return kaptcha.toString();
    }
}