/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.job.controller;

import io.ants.common.annotation.SysLog;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.job.entity.ScheduleJobEntity;
import io.ants.modules.job.service.ScheduleJobService;
import io.ants.modules.job.task.JavaJobEnum;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 定时任务
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/sys/schedule")
public class ScheduleJobController {
	@Autowired
	private ScheduleJobService scheduleJobService;


	@GetMapping("/jobBeanList")
	public R job_list(){
		return R.ok().put("data", JavaJobEnum.GetAll());
	}

	/**
	 * 定时任务列表
	 */
	@PostMapping("/list")
	@RequiresPermissions("sys:schedule:list")
	public R list(@RequestBody Map<String, Object> params){
		PageUtils page = scheduleJobService.queryPage(params);

		return R.ok().put("page", page);
	}
	
	/**
	 * 定时任务信息
	 */
	@RequestMapping("/info/{jobId}")
	@RequiresPermissions("sys:schedule:info")
	public R info(@PathVariable("jobId") Long jobId){
		ScheduleJobEntity schedule = scheduleJobService.getById(jobId);
		
		return R.ok().put("schedule", schedule);
	}
	
	/**
	 * 保存定时任务
	 */
	@SysLog("保存定时任务")
	@RequestMapping("/save")
	@RequiresPermissions("sys:schedule:save")
	public R save(@RequestBody ScheduleJobEntity scheduleJob){
		ValidatorUtils.validateEntity(scheduleJob);
		if (null!=scheduleJob.getStatus()){
			Integer newStatus=0==scheduleJob.getStatus()?1:0;
			scheduleJob.setStatus(newStatus);
		}
		scheduleJobService.saveJob(scheduleJob);
		
		return R.ok();
	}
	
	/**
	 * 修改定时任务
	 */
	@SysLog("修改定时任务")
	@RequestMapping("/update")
	@RequiresPermissions("sys:schedule:update")
	public R update(@RequestBody ScheduleJobEntity scheduleJob){
		ValidatorUtils.validateEntity(scheduleJob);
		if (null!=scheduleJob.getStatus()){
			Integer newStatus=0==scheduleJob.getStatus()?1:0;
			scheduleJob.setStatus(newStatus);
		}
		scheduleJobService.update(scheduleJob);
		
		return R.ok();
	}
	
	/**
	 * 删除定时任务
	 */
	@SysLog("删除定时任务")
	@RequestMapping("/delete")
	@RequiresPermissions("sys:schedule:delete")
	public R delete(@RequestBody Map params){
		if(!params.containsKey("ids")){
			return R.error("ids参数缺失");
		}
		String ids_=params.get("ids").toString();
		String[]  id_s=ids_.split(",");
		Long[] jobIds = new Long[id_s.length];
		for(int i=0;i<id_s.length;i++){
			jobIds[i] = Long.parseLong(id_s[i]);
		}
		scheduleJobService.deleteBatch(jobIds);
		return R.ok();
	}
	
	/**
	 * 立即执行任务
	 */
	@SysLog("立即执行任务")
	@RequestMapping("/run")
	@RequiresPermissions("sys:schedule:run")
	public R run( @RequestBody Map params){
		if(!params.containsKey("ids")){
			return R.error("参数ids缺失");
		}
		String ids_=params.get("ids").toString();
		String[]  id_s=ids_.split(",");
		Long[] jobIds = new Long[id_s.length];
		for(int i=0;i<id_s.length;i++){
			jobIds[i] = Long.parseLong(id_s[i]);
		}
		scheduleJobService.run(jobIds);
		return R.ok();
	}
	
	/**
	 * 暂停定时任务
	 */
	@SysLog("暂停定时任务")
	@RequestMapping("/pause")
	@RequiresPermissions("sys:schedule:pause")
	public R pause(@RequestBody Map params){
		if(!params.containsKey("ids")){
			return R.error("参数ids缺失");
		}
		String ids_=params.get("ids").toString();
		String[]  id_s=ids_.split(",");
		Long[] jobIds = new Long[id_s.length];
		for(int i=0;i<id_s.length;i++){
			jobIds[i] = Long.parseLong(id_s[i]);
		}
		scheduleJobService.pause(jobIds);
		return R.ok();
	}
	
	/**
	 * 恢复定时任务
	 */
	@SysLog("恢复定时任务")
	@RequestMapping("/resume")
	@RequiresPermissions("sys:schedule:resume")
	public R resume(@RequestBody Map params){
		if(!params.containsKey("ids")){
			return R.error("参数ids缺失");
		}
		String ids_=params.get("ids").toString();
		String[]  id_s=ids_.split(",");
		Long[] jobIds = new Long[id_s.length];
		for(int i=0;i<id_s.length;i++){
			jobIds[i] = Long.parseLong(id_s[i]);
		}
		scheduleJobService.resume(jobIds);
		
		return R.ok();
	}

}
