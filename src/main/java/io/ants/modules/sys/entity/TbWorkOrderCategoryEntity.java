package io.ants.modules.sys.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_work_order_category")
public class TbWorkOrderCategoryEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer parentId;

    private String title;

    private String description;

    private Integer status;

    private Date createdate;

    @TableField(exist = false)
    private Integer childCount;
}
