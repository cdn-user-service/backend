package io.ants.modules.sys.oauth2;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.entity.SysUserTokenEntity;
import io.ants.modules.sys.service.ShiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

// 对应原oauth2realm

@Component
public class OAuth2AuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private ShiroService shiroService;

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2Token.class.isAssignableFrom(authentication);
    }

    /**
     * 认证(登录时调用) - 完全复制原版 doGetAuthenticationInfo 逻辑
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String accessToken = (String) authentication.getPrincipal();

        // 根据accessToken，查询用户信息 - 完全复制原版逻辑
        SysUserTokenEntity tokenEntity = shiroService.queryByToken(accessToken);

        // token失效 - 完全复制原版逻辑和异常类型
        if (tokenEntity == null || tokenEntity.getExpireTime().getTime() < System.currentTimeMillis()) {
            throw new BadCredentialsException("token失效，请重新登录");
        }

        // 查询用户信息 - 完全复制原版逻辑
        SysUserEntity user = shiroService.queryUser(tokenEntity.getUserId());

        // 账号锁定 - 完全复制原版逻辑和异常类型
        if (user.getStatus() == 0) {
            throw new LockedException("账号已被锁定,请联系管理员");
        }

        // 获取用户权限列表 - 复制原版授权逻辑 doGetAuthorizationInfo
        Long userId = user.getUserId();
        Set<String> permsSet = shiroService.getUserPermissions(userId);

        // 转换为 Spring Security 的权限格式
        List<GrantedAuthority> authorities = permsSet.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 返回认证信息 - 对应原版的 SimpleAuthenticationInfo
        return new UsernamePasswordAuthenticationToken(user, accessToken, authorities);
    }
}