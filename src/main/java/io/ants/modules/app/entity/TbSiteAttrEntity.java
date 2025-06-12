package io.ants.modules.app.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_site_attr")
public class TbSiteAttrEntity {

    private  int id;

    private int siteId;

    private String pkey;

    private String pvalue;

    private  long pvalue1=0l;

    private  long pvalue2=0l;

    private String pType;

    private int status;

    private int weight;

    private Date updateTime;

}
