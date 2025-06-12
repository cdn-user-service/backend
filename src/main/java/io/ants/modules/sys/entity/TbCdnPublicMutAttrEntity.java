package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_cdn_public_mut_attr")
public class TbCdnPublicMutAttrEntity {

    private  Integer id;

    private Integer parentId;

    private String pkey;

    private String pvalue;

    private Integer status;

    private int weight;

    private Date updateTime;
}
