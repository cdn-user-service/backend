package io.ants.modules.app.interceptor;

import io.ants.common.exception.RRException;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限(Token)验证
 * 
 * @author Mark sunlightcs@gmail.com
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    public static final String USER_KEY = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 非方法处理器，直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 没有 @Login 注解，直接放行
        Login annotation = handlerMethod.getMethodAnnotation(Login.class);
        if (annotation == null) {
            return true;
        }

        // 获取 token
        String token = request.getHeader(jwtUtils.getHeader());
        if (StringUtils.isBlank(token)) {
            token = request.getParameter(jwtUtils.getHeader());
        }

        if (StringUtils.isBlank(token)) {
            throw new RRException(jwtUtils.getHeader() + "不能为空", HttpStatus.UNAUTHORIZED.value());
        }

        Claims claims = jwtUtils.getClaimByToken(token);
        if (claims == null || jwtUtils.isTokenExpired(claims.getExpiration())) {
            throw new RRException(jwtUtils.getHeader() + "失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
        }

        // 设置 userId 到 request 属性中，供后续使用
        request.setAttribute(USER_KEY, Long.parseLong(claims.getSubject()));

        return true;
    }
}