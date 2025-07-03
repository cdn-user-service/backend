package io.ants.config;

import io.ants.modules.sys.oauth2.OAuth2Filter;
import io.ants.modules.sys.oauth2.OAuth2AuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 确保方法级权限注解生效
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2Filter oauth2Filter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll() // ✅ 所有请求都放行，权限逻辑由 Filter 控制
                )
                .addFilterBefore(oauth2Filter, UsernamePasswordAuthenticationFilter.class); // ✅ 自定义认证拦截

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(OAuth2AuthenticationProvider provider) throws Exception {
        return new ProviderManager(List.of(provider));
    }

    @Bean
    public OAuth2Filter oAuth2Filter(AuthenticationManager authenticationManager) {
        return new OAuth2Filter(authenticationManager);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}