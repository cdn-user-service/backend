package io.ants.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class AppConfigHelper {
    
    private static Environment environment;
    
    private static String redisPassword;
    
    @Autowired
    public void setEnvironment(Environment env) {
        AppConfigHelper.environment = env;
    }
    
    @Value("${spring.redis.password:}")
    public void setRedisPassword(String password) {
        AppConfigHelper.redisPassword = password;
    }
    
    public static boolean isDev() {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile)) {
                return true;
            }
        }
        return false;
    }
    
    public static String getConfiguredRedisPassword() {
        return redisPassword;
    }
}