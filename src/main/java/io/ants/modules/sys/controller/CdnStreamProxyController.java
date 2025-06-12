package io.ants.modules.sys.controller;

import io.ants.common.annotation.SysLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbStreamProxyEntity;
import io.ants.modules.app.form.QueryStreamListForm;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.sys.service.TbStreamProxyService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sys/cdnsys/stream/proxy/")
public class CdnStreamProxyController extends AbstractController{

    @Autowired
    private TbStreamProxyService streamProxyService;
    @Autowired
    private TbUserService userService;


    @PostMapping("/list")
    public R list(@RequestBody QueryStreamListForm params){
        if(StringUtils.isNotBlank(params.getUser())){
            String userIds=userService.key2userIds(params.getUser());
            params.setUserIds(userIds);
        }
        return R.ok().put("data",streamProxyService.streamList(params));
    }

    @GetMapping("/detail")
    public R detail(Integer id) {
        return streamProxyService.getDetailById(null, id);
    }

    @PostMapping("/sava")
    public R sava( @RequestBody Map params){
        TbStreamProxyEntity sp= DataTypeConversionUtil.map2entity(params, TbStreamProxyEntity.class);
        TbStreamProxyEntity newSp=streamProxyService.saveProxy(sp);
        if (null==newSp){
            return R.error("创建失败！");
        }
        return R.ok().put("data",newSp);
    }

    @SysLog("修改转发状态")
    @GetMapping("/change/status")
    public R change_site_status( Integer proxyId, Integer status){
        return R.ok().put("data",streamProxyService.changeProxyStatus(null,proxyId,status));
    }

    @PostMapping("/batch/delete")
    public R delete(@RequestBody Map params){
        if(!params.containsKey("ids")){
            return R.error("参数为空");
        }
        String ids=params.get("ids").toString();
        streamProxyService.batchDelete(null,ids);
        return R.ok();
    }
}
