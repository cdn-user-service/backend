package io.ants.modules.job.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.job.entity.ScheduleJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import java.util.Map;
import java.util.List;

@Mapper
public interface ScheduleJobDao extends BaseMapper<ScheduleJobEntity> {

	/**
	 * 批量更新状态
	 */
	@Update({
			"<script>",
			"UPDATE schedule_job SET status = #{status} WHERE job_id IN ",
			"<foreach item='jobId' collection='list' open='(' separator=',' close=')'>",
			"#{jobId}",
			"</foreach>",
			"</script>"
	})
	int updateBatch(@Param("status") int status, @Param("list") List<Long> jobIds);
}