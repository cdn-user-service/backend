package io.ants.modules.sys.controller;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbRewriteEntity;
import io.ants.modules.app.service.TbRewriteService;
import io.ants.modules.app.service.TbUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-09-22 10:11:35
 */

@RestController
@RequestMapping("/sys/cdn/rewrite/")
public class TbRewriteController {
    @Autowired
    private TbRewriteService rewriteService;

    @Autowired
    private TbUserService userService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @PreAuthorize("hasAuthority('tb:rewrite:list')")
    public R list(@RequestBody Map<String, Object> params) {
        String userIds;
        if (params.containsKey("user")) {
            userIds = userService.key2userIds(params.get("user").toString());
            params.put("userIds", userIds);
        }
        PageUtils page = rewriteService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @PreAuthorize("hasAuthority('tb:rewrite:info')")
    public R info(@PathVariable("id") Integer id) {
        TbRewriteEntity rewrite = rewriteService.getById(id);
        return R.ok().put("rewrite", rewrite);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @PreAuthorize("hasAuthority('tb:rewrite:save')")
    public R save(@RequestBody TbRewriteEntity rewrite) {
        rewriteService.saveObj(rewrite.getUserId(), rewrite);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @PreAuthorize("hasAuthority('tb:rewrite:update')")
    public R update(@RequestBody TbRewriteEntity rewrite) {
        rewriteService.saveObj(rewrite.getUserId(), rewrite);
        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('tb:rewrite:delete')")
    public R delete(@RequestBody Map param) {
        if (!param.containsKey("ids")) {
            return R.error("[ids] is empty");
        }
        String ids = param.get("ids").toString();
        return R.ok().put("data", rewriteService.removeByIds(null, ids));
    }

}
