/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.sys.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置信息
 *
 * @author Mark sunlightcs@gmail.com
 */
@Mapper
public interface SysConfigDao extends BaseMapper<SysConfigEntity> {

	/**
	 * 根据key，查询value
	 */
	SysConfigEntity queryByKey(String paramKey);

	/**
	 * 根据key，更新value
	 */
	int updateValueByKey(@Param("paramKey") String paramKey, @Param("paramValue") String paramValue);
	
}
