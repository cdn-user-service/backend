/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.controller;

import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.form.QueryLogForm;
import io.ants.modules.sys.service.SysLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统日志
 *
 * @author Mark sunlightcs@gmail.com
 */
@Controller
@RequestMapping("/sys/log")
public class SysLogController extends AbstractController {

	@Autowired
	private SysLogService sysLogService;

	/**
	 * 列表
	 */
	@ResponseBody
	@PostMapping("/list")
	public R list(@RequestBody Map<String, Object> params) {
		QueryLogForm form = DataTypeConversionUtil.map2entity(params, QueryLogForm.class);
		PageUtils page = sysLogService.querySysLogPage(form);
		return R.ok().put("page", page);
	}

	@ResponseBody
	@GetMapping("/delete")
	@PreAuthorize("hasAuthority('sys:log:list')")
	public R logDelete(String ids) {
		sysLogService.deleteLog(null, ids);
		return R.ok();

	}

}
