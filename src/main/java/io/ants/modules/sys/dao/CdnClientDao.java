package io.ants.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.sys.entity.CdnClientEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.LinkedHashMap;
import java.util.List;

@Mapper
public interface CdnClientDao  extends BaseMapper<CdnClientEntity> {

    List<LinkedHashMap<String,Object>> queryOnlineClient();
}
