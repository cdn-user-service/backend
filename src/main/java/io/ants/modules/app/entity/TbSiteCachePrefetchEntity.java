package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_site_cache_prefetch")
public class TbSiteCachePrefetchEntity {

    //SELECT  id,site_id,site_server_name,user_id,user_name,pf_path,interval,status,create_time

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer siteId;

    private String siteServerName;

    private Integer userId;
    
    private String userName;

    private String pfPath;

    private Integer frequency=600;

    private Integer status=1;

    private Integer createTime=0;


    @TableField(exist = false)
    private String pfPaths;


}
