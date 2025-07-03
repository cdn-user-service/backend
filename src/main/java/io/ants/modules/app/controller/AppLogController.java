package io.ants.modules.app.controller;

import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.form.QueryLogForm;
import io.ants.modules.sys.service.SysLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
 

import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/app/log/")
@Tag(name = "日志管理")
public class AppLogController {

    @Autowired
    private SysLogService sysLogService;

    @Login
    @ResponseBody
    @PostMapping("/list")
    @Operation(summary = "用户日志列表")
    public R list(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        params.put("userType", UserTypeEnum.USER_TYPE.getId());
        params.put("userId", userId);
        QueryLogForm form = DataTypeConversionUtil.map2entity(params, QueryLogForm.class);

        PageUtils page = sysLogService.querySysLogPage(form);
        return R.ok().put("page", page);
    }

    @Login
    @GetMapping("/log/delete")
    @Operation(summary = "删除用户日志")
    public R logDelete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, String ids) {
        sysLogService.deleteLog(userId, ids);
        return R.ok();

    }

}
