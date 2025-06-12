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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * APP测试接口
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/app")
@Api("APP测试接口")
public class AppTestController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private MakeFileFromResService makeFileFromResService;

    @Login
    @GetMapping("userInfo")
    @ApiOperation("获取用户信息")
    public R userInfo(@LoginUser TbUserEntity user){
        return R.ok().put("user", user);
    }

    @Login
    @GetMapping("userId")
    @ApiOperation("获取用户ID")
    public R userInfo(@ApiIgnore @RequestAttribute("userId") Integer userId){
        return R.ok().put("userId", userId);
    }

    @GetMapping("notToken")
    @ApiOperation("忽略Token验证测试")
    public R notToken(){
        //String account= AcmeShUtils.getAcmeAccount();
        logger.info( ch.qos.logback.classic.spi.ThrowableProxy.class.getName());
        logger.error("Error message", new RuntimeException("Something went wrong"));
        return R.ok().put("msg", "");
    }

}
