/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.Query;
import io.ants.modules.job.dao.ScheduleJobDao;
import io.ants.modules.job.dao.ScheduleJobLogDao;
import io.ants.modules.job.entity.ScheduleJobEntity;
import io.ants.modules.job.entity.ScheduleJobLogEntity;
import io.ants.modules.job.service.ScheduleJobLogService;
import io.ants.modules.job.task.JavaJobEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service("scheduleJobLogService")
public class ScheduleJobLogServiceImpl extends ServiceImpl<ScheduleJobLogDao, ScheduleJobLogEntity> implements ScheduleJobLogService {


	@Autowired
	private ScheduleJobDao scheduleJobDao;

	//删除1天前的任务日志
	private  void deleteTimeOutTaskLog(){
		Date now=new Date();
		Date lastDate= DateUtils.addDateDays(now,-1);
		//System.out.println(lastDate);
		QueryWrapper<ScheduleJobLogEntity> wrapper=new QueryWrapper<>();
		wrapper.le("create_time",lastDate);
		this.remove(wrapper);
		//logger.debug("delete time out log");
	}

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		this.deleteTimeOutTaskLog();
		String jobId = (String)params.get("jobId");
		String beanName=null;
		if(params.containsKey("beanName")){
			beanName=params.get("beanName").toString();
		}

		IPage<ScheduleJobLogEntity> page = this.page(
			new Query<ScheduleJobLogEntity>().getPage(params),
			new QueryWrapper<ScheduleJobLogEntity>()
					.eq(StringUtils.isNotBlank(beanName),"bean_name",beanName)
					.like(StringUtils.isNotBlank(jobId),"job_id", jobId)
					.orderByDesc("create_time")
		);

		page.getRecords().forEach(item->{
			ScheduleJobEntity job=scheduleJobDao.selectById(item.getJobId());
			item.setJobObj(job);
			item.setBeanName(JavaJobEnum.getCnTitleByBeanName(item.getBeanName()));
		});
		return new PageUtils(page);
	}

}
