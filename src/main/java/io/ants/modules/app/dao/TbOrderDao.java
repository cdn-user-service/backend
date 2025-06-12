package io.ants.modules.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.app.entity.TbOrderEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface TbOrderDao extends BaseMapper<TbOrderEntity> {




    Map<String,Object> getAlreadyPaySum();

    Map<String,Object> getAlreadyRechargeSum();
}
