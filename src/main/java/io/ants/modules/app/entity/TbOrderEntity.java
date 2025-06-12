package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_order")
public class TbOrderEntity {

    private Integer id;

    private String serialNumber;

    private Long userId;

    private int orderType;

    private int targetId;

    //应付
    private int payable;

    private String initJson;

    private String payJson;

    private String remark;

    private int status;

    private Date createTime=new Date();

    @TableField(exist = false)
    private Object payObject;

    @TableField(exist = false)
    private String user;

    @TableField(exist = false)
    private Object product;

}
