/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.job.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.job.entity.ScheduleJobEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 定时任务
 *
 * @author Mark sunlightcs@gmail.com
 */
@Mapper
public interface ScheduleJobDao extends BaseMapper<ScheduleJobEntity> {
	
	/**
	 * 批量更新状态
	 */
	int updateBatch(Map<String, Object> map);
}
