package io.ants.modules.sys.controller;

import io.ants.common.annotation.SysLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.R;
import io.ants.common.utils.RedisUtils;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.sys.form.SaveTbUserInfoFrom;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.app.utils.JwtUtils;
import io.ants.modules.sys.service.SysConfigService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sys/tb/user")
public class TbUserController extends AbstractController{

    @Autowired
    private TbUserService userService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private SysConfigService sysConfigService;


    @PostMapping("/list")
    @RequiresPermissions("app:user:list")
    public R list(@RequestBody Map<String, Object> params){
        int page=1;
        int limit=10;
        String user="";
        if(params.containsKey("page")){
            page=Integer.parseInt(params.get("page").toString());
        }
        if(params.containsKey("limit")){
            limit=Integer.parseInt(params.get("limit").toString());
        }
        if(params.containsKey("user")){
            user=params.get("user").toString();
        }
        return  R.ok().put("data",userService.userList(page,limit,user));
    }

    @GetMapping("/detail")
    @RequiresPermissions("app:user:info")
    public R detail(@RequestParam String userId){
        Long user_id=Long.parseLong(userId);
        return R.ok().put("data",userService.userDetail(user_id));
    }

    @SysLog("删除用户")
    @PostMapping("/deleta")
    @RequiresPermissions("app:user:delete")
    public R delete(@RequestBody Map<String, Object> params){
        checkDemoModify();
        if(params.containsKey("ids")){
            userService.delete(params.get("ids").toString());
            return R.ok();
        }
        return R.error("参数不完整");
    }




    @SysLog("修改用户信息")
    @PostMapping("/modify")
    @RequiresPermissions("app:user:update")
    public R modify(@RequestBody Map<String, Object> params){
        checkDemoModify();
        SaveTbUserInfoFrom userForm= DataTypeConversionUtil.map2entity(params, SaveTbUserInfoFrom.class);
        TbUserEntity user=userService.saveUserByUserForm(userForm);
        return R.ok().put("data",user);
    }


    @SysLog("创建用户")
    @PostMapping("/create")
    @RequiresPermissions("app:user:save")
    public R create(@RequestBody Map<String, Object> params){
        SaveTbUserInfoFrom userForm= DataTypeConversionUtil.map2entity(params, SaveTbUserInfoFrom.class);
        TbUserEntity user=userService.saveUserByUserForm(userForm);
        return R.ok().put("data",user);

    }

    @SysLog("后台登录")
    @GetMapping("/back/login")
    public R backLogin(@RequestParam Integer userId){
        this.recordInfo(redisUtils);
        String token = jwtUtils.generateToken(userId);
        return R.ok().put("token", token).put("expire", jwtUtils.getExpire()).put("env",sysConfigService.getValue("WEB_DIR_CONF"));
    }

}
