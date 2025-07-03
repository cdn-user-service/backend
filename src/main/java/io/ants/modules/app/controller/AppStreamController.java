package io.ants.modules.app.controller;

import io.ants.common.annotation.UserLog;
import io.ants.common.utils.R;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.entity.TbStreamProxyEntity;
import io.ants.modules.app.form.QueryStreamListForm;
import io.ants.modules.app.service.TbUserLogService;
import io.ants.modules.sys.service.TbStreamProxyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
 

import java.util.Map;

@RestController
@RequestMapping("/app/stream/")
@Tag(name = "stream管理")
public class AppStreamController {

    @Autowired
    private TbStreamProxyService streamProxyService;

    @Login
    @PostMapping("/list")
    @Operation(summary = "列表")
    public R list(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QueryStreamListForm params) {
        params.setUserIds(userId.toString());
        return R.ok().put("data", streamProxyService.streamList(params));
    }

    @Login
    @PostMapping("/sava")
    @Operation(summary = "四层转发保存")
    @UserLog("四层转发保存")
    public R sava(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody TbStreamProxyEntity streamProxy) {
        streamProxy.setUserId(userId);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        TbStreamProxyEntity proxyEntity = streamProxyService.saveProxy(streamProxy);
        return R.ok().put("data", proxyEntity);
    }

    @Login
    @GetMapping("/detail")
    @Operation(summary = "")
    public R detail(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, Integer id) {
        return streamProxyService.getDetailById(userId, id);
    }

    @Login
    @GetMapping("/change/status")
    @Operation(summary = "修改转发状态")
    public R change_site_status(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            Integer streamProxyId,
            Integer status) {
        return R.ok().put("data", streamProxyService.changeProxyStatus(userId, streamProxyId, status));
    }

    @Login
    @PostMapping("/batch/delete")
    @Operation(summary = "四层转发删除")
    @UserLog("四层转发删除")
    public R delete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map params) {
        if (!params.containsKey("ids")) {
            return R.error("参数为空");
        }
        String ids = params.get("ids").toString();
        streamProxyService.batchDelete(userId, ids);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return R.ok();
    }

    @Login
    @GetMapping("/all/port")
    @Operation(summary = "获取所有四层转发端口")
    public R allPort(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return streamProxyService.getAllPort(userId);
    }

}
