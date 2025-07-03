/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.controller;

import io.ants.common.utils.R;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.annotation.LoginUser;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.sys.makefile.MakeFileFromResService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import io.ants.common.utils.RedisUtils;

/**
 * APP测试接口
 */
@RestController
@RequestMapping("/app")
@Tag(name = "APP测试接口")
public class AppTestController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MakeFileFromResService makeFileFromResService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Login
    @GetMapping("userInfo")
    @Operation(summary = "获取用户信息")
    public R userInfo(@LoginUser TbUserEntity user) {
        return R.ok().put("user", user);
    }

    @Login
    @GetMapping("userId")
    @Operation(summary = "获取用户ID")
    public R userInfo(@Parameter(hidden = true) @RequestAttribute("userId") Integer userId) {
        return R.ok().put("userId", userId);
    }

    @GetMapping("notToken")
    @Operation(summary = "忽略Token验证测试")
    public R notToken() {
        logger.info(ch.qos.logback.classic.spi.ThrowableProxy.class.getName());
        logger.error("Error message", new RuntimeException("Something went wrong"));
        return R.ok().put("msg", "");
    }

    @GetMapping("test/alive")
    public String alive() {
        return "✅ Server OK!";
    }

    @GetMapping("test/redis")
    @Operation(summary = "测试Redis是否连接成功")
    public R testRedis() {
        try {
            // 用最简单的方式 set 一下值，看会不会抛异常
            redisTemplate.opsForValue().set("test-key", "test-value");
            String value = (String) redisTemplate.opsForValue().get("test-key");

            return R.ok().put("redis", value);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("Redis连接失败: " + e.getMessage());
        }
    }
}