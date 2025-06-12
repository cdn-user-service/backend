package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("tb_message")
public class TbMessageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;


    /**
     * 消息类型:1=站内推送消息;2=公告消息(无阅读状态)
     */
    private Integer type;


    /**
     * 发送类型 :0=指定user;1=指定分组;2=所有
     */
    private Integer sendType;

    private String sendObj;

    private String title;

    private String content;

    /**
     * 展示状态  1:展示 0:禁用
     */
    private Integer status;

    private java.sql.Timestamp createtime;

    @TableField(exist = false)
    private Integer readStatus;

}
