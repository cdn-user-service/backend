package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_stream_proxy")
public class TbStreamProxyEntity {
    private Integer id;

    private Long userId;

    private String serialNumber;

    private String bindPort;

    private Integer areaId=0;

    private String confInfo;

    private Integer status;


    private String suitJsonObj;

    private Date createTime;

    @TableField(exist = false)
    private Object user;
    @TableField(exist = false)
    private Object suit;
    @TableField(exist = false)
    private Object cname;

}
