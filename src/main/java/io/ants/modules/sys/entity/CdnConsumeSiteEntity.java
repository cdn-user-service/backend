package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 站点已消耗资源记录表
 */
@Data
@TableName("cdn_consume_site")
public class CdnConsumeSiteEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer siteId;

    private Long totalFlow=0l;
    private Long usedFlow=0l;

    private Integer startTime;

    private Integer endTime;


}
