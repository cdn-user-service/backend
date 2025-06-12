/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.app.entity.TbUserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 用户
 *
 * @author Mark sunlightcs@gmail.com
 */
@Mapper
public interface TbUserDao extends BaseMapper<TbUserEntity> {

    List<Map<String,Object>> query7regdata();


    Map<String,Object> getUserNamesByUserId(long userId);

}
