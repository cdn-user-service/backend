/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.oss.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.oss.entity.SysOssEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传
 *
 * @author Mark sunlightcs@gmail.com
 */
@Mapper
public interface SysOssDao extends BaseMapper<SysOssEntity> {
	
}
