package io.ants.modules.sys.oauth2;

import com.google.gson.Gson;
import io.ants.common.utils.HttpContextUtils;
import io.ants.common.utils.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.AntPathMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMethod;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.AuthenticationException;
import java.util.List;

public class OAuth2Filter extends OncePerRequestFilter {

    private AuthenticationManager authenticationManager;

    private static final List<String> WHITE_LIST = List.of(
            "/swagger-ui.html",
            "/admin/**",
            "/users/**",
            "/ws_ssh/**",
            "/webjars/**",
            "/druid/**",
            "/antsxdp/app/**",
            "/app/**",
            "/app/account/kaptcha.jpg",
            "/app/account/login",
            "/app/account/regist",
            "/sys-user/check",
            "/sys/login",
            "/sys/appkey/login",
            "/sys/google-auth/login",
            "/swagger/**",
            "/v2/api-docs",
            "/swagger-resources/**",
            "/captcha.jpg",
            "/sys/cdnsys/auth/node/addByNodeRequest",
            "/sys/cdnsys/auth/nginx/conf/feedbacks",
            "/sys/cdnsys/auth/nginx/nft/intercept/all",
            "/sys/cdnsys/auth/runTask",
            "/app/product/product/attr/object",
            "/sys/certify/zero/api/create/cert",
            "/sys/cdnsys/auth/check/import",
            "/sys/oss/upload",
            "/sys/oss/get/image/*", // 保持原版的单级匹配
            "/aaa.txt",
            "/sys/common/**");
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    public OAuth2Filter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        for (String whitePath : WHITE_LIST) {
            if (pathMatcher.match(whitePath, path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        // OPTIONS请求直接放行
        if (RequestMethod.OPTIONS.name().equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取token
        String token = getRequestToken(request);

        // token为空时的处理
        if (StringUtils.isBlank(token)) {
            response.setContentType("application/json;charset=utf-8");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", HttpContextUtils.getOrigin());

            String json = new Gson().toJson(R.error(HttpServletResponse.SC_UNAUTHORIZED, "invalid token"));
            response.getWriter().print(json);
            return; // 阻止继续处理
        }

        try {
            // 尝试认证
            Authentication auth = authenticationManager.authenticate(new OAuth2Token(token));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);

        } catch (AuthenticationException e) {

            response.setContentType("application/json;charset=utf-8");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", HttpContextUtils.getOrigin());

            // 处理登录失败的异常
            Throwable throwable = e.getCause() == null ? e : e.getCause();
            R r = R.error(HttpServletResponse.SC_UNAUTHORIZED, throwable.getMessage());
            String json = new Gson().toJson(r);
            response.getWriter().print(json);
        }
    }

    /**
     * 获取请求的token
     */
    private String getRequestToken(HttpServletRequest httpRequest) {
        // 从header中获取token
        String token = httpRequest.getHeader("token");

        // 如果header中不存在token，则从参数中获取token
        if (StringUtils.isBlank(token)) {
            token = httpRequest.getParameter("token");
        }

        return token;
    }
}