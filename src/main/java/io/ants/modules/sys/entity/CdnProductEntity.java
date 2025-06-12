package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cdn_product")
public class CdnProductEntity {

    private Integer id;

    private Integer productType;

    private String name;

    private String productJson;

    private String attrJson;

    //选用的节点组ID 组
    private String serverGroupIds;



    private Integer status;

    private  Integer weight;

    private Date createtime;

    @TableField(exist = false)
    private  Object client_group_list;

    @TableField(exist = false)
    private  Object attr;

    @TableField(exist = false)
    private  Object sortAttr;

}
