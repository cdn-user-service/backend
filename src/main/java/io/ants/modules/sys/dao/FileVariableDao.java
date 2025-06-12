package io.ants.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.sys.entity.CdnVariableEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileVariableDao extends BaseMapper<CdnVariableEntity> {
}
