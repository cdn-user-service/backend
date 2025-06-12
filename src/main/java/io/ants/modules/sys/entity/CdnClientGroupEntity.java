package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author Administrator
 */
@Data
@TableName("cdn_client_group")
public class CdnClientGroupEntity {
    private Integer id;

    private Integer  areaId=0;

    private String name;

    private String hash;

    private int dnsConfigId;

    private Integer weight;

    private Integer status;

    //0==A 记录（cdn调度） 1==自建DNS GTM 调度
    private int recordMode=0;

    private String rdata="";

    private Date createTime;


    @TableField(exist = false)
    private String clientIds;

    @TableField(exist = false)
    private Object clientInfos;

    @TableField(exist = false)
    private Object cname;

    @TableField(exist = false)
    private Object areaName;
}
