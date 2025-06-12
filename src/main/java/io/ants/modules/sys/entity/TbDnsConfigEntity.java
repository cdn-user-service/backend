package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tb_dns_config")
public class TbDnsConfigEntity  implements Serializable {
    private Integer id;

    private Integer userType;

    private Long userId;

    private String remark;

    private  String source;

    private String appDomain;

    private String appId;

    private String appKey;

    private String appUrl;

    private Integer status;

    private Date createTime;
}
