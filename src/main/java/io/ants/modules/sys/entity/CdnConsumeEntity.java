package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 已消耗资源记录表
 */
@Data
@TableName("cdn_consume")
public class CdnConsumeEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String serialNumber;

    private String attrName="flow";

    private Long sValue=0L;

    private Integer startTime;

    private Integer endTime;

    private Integer status=1;


    /**
     * 记录值类型0==套餐1==增值
     */
    private Integer rType=0;

   // private Date createTime;

}
