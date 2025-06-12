package io.ants.modules.app.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_log")
public class TbUserLogEntity {

    private static final long serialVersionUID = 1L;
    @TableId
    private Integer id;

    private Long userId;

    private Integer userType;

    private Integer logType;

    private String method;

    private String params;

    private String ip;

    //用户操作
    private String operation;

    @TableField(exist = false)
    private String area;

    @JSONField(name = "status")
    private Integer isDelete;


    //用户名
    private String username;

    //执行时长(毫秒)
    private Long time;

    private Date createDate;

    //用户名
    @TableField(exist = false)
    private Object userInfo;
}
