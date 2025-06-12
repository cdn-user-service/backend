package io.ants.modules.sys.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_work_order_list")
public class TbWorkOrderListEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Long userId;

    private Integer categoryId;

    private Integer urgentLevel;

    private String title;

    private String description;

    private String images;

    private Integer status;

    private Integer score;

    private Date createdate;

    @TableField(exist = false)
    private Object categoryObj;

    @TableField(exist = false)
    private Integer lastSenderType;

    @TableField(exist = false)
    private Date lastSubmitDate;

    @TableField(exist = false)
    private Object senderInfos;



    @TableField(exist = false)
    private String userName;
}
