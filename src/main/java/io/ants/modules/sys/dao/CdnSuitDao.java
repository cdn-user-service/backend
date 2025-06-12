package io.ants.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.ants.modules.sys.entity.CdnSuitEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CdnSuitDao extends BaseMapper<CdnSuitEntity> {


    //@Select("SELECT c.serial_number FROM cdn_suit c   INNER JOIN ( SELECT serial_number, MAX(end_time) AS max_end_time    FROM cdn_suit    WHERE STATUS = 1 AND suit_type=10  AND end_time > NOW()  GROUP BY serial_number) AS grouped_cdn    ON c.serial_number = grouped_cdn.serial_number   AND c.end_time = grouped_cdn.max_end_time WHERE c.STATUS = 1")
    @Select("select id,serial_number from cdn_suit where end_time>now() and status=1 group by serial_number")
    List<CdnSuitEntity> getAllAvailableSuit();
}
