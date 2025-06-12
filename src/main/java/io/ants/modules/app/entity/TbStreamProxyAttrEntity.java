package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_stream_proxy_attr")
public class TbStreamProxyAttrEntity {
    private Integer id;

    private Integer streamId;

    private Integer areaId;

    private String pkey;

    private String pvalue;

    private Date createTime;

    @TableField(exist = false)
    private Object tbStreamObj;

}
