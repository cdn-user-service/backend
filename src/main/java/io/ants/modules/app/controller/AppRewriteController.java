package io.ants.modules.app.controller;

import java.util.Map;

import io.ants.common.annotation.UserLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.entity.TbRewriteEntity;
import io.ants.modules.app.service.TbRewriteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
 

/**
 * 
 *
 * @author chenshun
 * @date 2022-09-22 10:11:35
 */
@RestController
@RequestMapping("app/rewrite")
@Tag(name = "URL转发相关接口")
public class AppRewriteController {
    @Autowired
    private TbRewriteService rewriteService;

    /**
     * 列表
     */
    @Login
    @PostMapping("/list")
    @Operation(summary = "列表")
    public R list(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        params.put("userIds", userId.toString());
        PageUtils page = rewriteService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @Login
    @GetMapping("/info/{id}")
    @Operation(summary = "查看")
    public R info(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @PathVariable("id") Integer id) {
        TbRewriteEntity rewrite = rewriteService.getById(id);
        return R.ok().put("rewrite", rewrite);
    }

    /**
     * 保存
     */
    @Login
    @PostMapping("/save")
    @Operation(summary = "url转发保存")
    @UserLog("url转发保存")
    public R save(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        TbRewriteEntity rewrite = DataTypeConversionUtil.map2entity(params, TbRewriteEntity.class);
        if (0 != rewrite.getId()) {
            TbRewriteEntity s_rewite = rewriteService.getById(rewrite.getId());
            if (!s_rewite.getUserId().equals(userId)) {
                return R.error("参数有误！");
            }
        }
        rewrite.setUserId(userId);
        rewriteService.saveObj(userId, rewrite);
        return R.ok();
    }

    /**
     * 修改
     */
    @Login
    @PostMapping("/update")
    @Operation(summary = "url转发更新")
    @UserLog("url转发更新")
    public R update(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        TbRewriteEntity rewrite = DataTypeConversionUtil.map2entity(params, TbRewriteEntity.class);
        if (0 != rewrite.getId()) {
            TbRewriteEntity s_rewite = rewriteService.getById(rewrite.getId());
            if (!s_rewite.getUserId().equals(userId)) {
                return R.error("参数有误！");
            }
        }
        rewrite.setUserId(userId);
        rewriteService.saveObj(userId, rewrite);
        return R.ok();
    }

    /**
     * 删除
     */
    @Login
    @PostMapping("/delete")
    @Operation(summary = "url转发删除")
    @UserLog("url转发删除")
    public R delete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map param) {
        if (!param.containsKey("ids")) {
            return R.error("[ids] is empty");
        }
        String ids = param.get("ids").toString();
        return R.ok().put("data", rewriteService.removeByIds(userId, ids));
    }

}
