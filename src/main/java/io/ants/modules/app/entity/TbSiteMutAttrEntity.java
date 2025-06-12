package io.ants.modules.app.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_site_mut_attr")
public class TbSiteMutAttrEntity {
    private  int id=0;

    private int siteId=0;

    private String pkey;

    private String pvalue;

    private long pvalue1=0l;
    private long pvalue2=0l;


    private String pType;

    private int status=1;

    private int weight=0;

    private Date updateTime=new Date();
}
