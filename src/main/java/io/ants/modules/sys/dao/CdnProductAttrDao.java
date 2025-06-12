package io.ants.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.sys.entity.CdnProductAttrEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CdnProductAttrDao extends BaseMapper<CdnProductAttrEntity> {
    /**
     * @param id
     */
    void deleteById(Integer id);
}
