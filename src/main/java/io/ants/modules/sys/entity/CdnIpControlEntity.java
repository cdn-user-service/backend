package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cdn_ip_control")
public class CdnIpControlEntity {

    private Integer id;

    private Integer parentId;

    private String ip;

    private Long ipStart;

    private Long ipEnd;

    // 0:自定义  1:云端
    private int ipSource;

    //-1  1:ip 白名单  4:ip 黑名单
    private Integer control;

    private String remark;

    private Integer status;

    private Date createTime;

    @TableField(exist = false)
    private Object area;

    @TableField(exist = false)
    private Integer childCount;

    @TableField(exist = false)
    private Object child;

    @TableField(exist = false)
    private String ips;



}
