package io.ants.modules.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.app.entity.TbCertifyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TbCertifyDao extends BaseMapper<TbCertifyEntity> {

    //select * from tb_certify mod(id,2)=0 and status=2
    @Select("select * from tb_certify where mod(id,2)=#{index} and status=#{status}")
    List<TbCertifyEntity> getApplyingListByStatus(int index,int status);
}
