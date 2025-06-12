package io.ants.modules.app.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cdn_login_white_ip")
public class CdnLoginWhiteIpEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    //manager=1;appUser=2
    private int type;

    private long uid;

    private String ip;

    private String remark;

    private String status;

    private Date createTime;
}
