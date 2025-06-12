package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.ants.modules.utils.vo.DnsRecordItemVo;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cdn_client_group_dns_conf")
public class CdnClientGroupChildConfEntity {
    private Integer id;

    private Integer parentId;

    private Integer groupId;

    private Integer clientId;

    private Long ttl;

    private String line;

    private Integer status;

    private Date createTime;

    @TableField(exist = false)
    private String clientIp;


    /**
     * dns server  返回的记录信息
     */
    @TableField(exist = false)
    private DnsRecordItemVo recordInfos;

    @TableField(exist = false)
    private Object child;

    @TableField(exist = false)
    private Integer dnsType=0;

    @TableField(exist = false)
    private String cname;

    /**
     * client IP  信息
     */
    @TableField(exist = false)
    private CdnClientEntity clientEntity;

}
