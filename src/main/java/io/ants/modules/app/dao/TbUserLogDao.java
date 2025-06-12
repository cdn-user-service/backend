package io.ants.modules.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.app.entity.TbUserLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbUserLogDao extends BaseMapper<TbUserLogEntity> {
}
