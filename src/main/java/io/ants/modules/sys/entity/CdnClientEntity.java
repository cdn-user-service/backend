package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cdn_client")
public class CdnClientEntity {


    private  Integer id;

    private Integer  areaId=0;

    //1=main;2=middle;3=backup;4=回源IP
    private Integer clientType;

    private Integer parentId;

    private String clientIp="";

    private String clientIp2;

    private String area;

    private String line;

    private String remark;

    // private Integer redisPort;

    // private String redisAuth;

    private String clientInfo;

    private String regInfo;

    private String confInfo;

    private Date effectiveStartTime;

    private Date effectiveEndingTime;

    private String ngxVersion;
    private String version;
    @TableField(exist = false)
    private String os_name;

    private String agentVersion;

    private Integer status=0;

    private Integer stableScore=0;

    private Integer checkTime=0;


    private Integer sshPort;
    private String sshUser;
    private String sshPwd;

    private Date createtime;

    private Date updatetime;

    //线路优化开开关
    private Integer sysWsStatus=0;

    @TableField(exist = false)
    private Object childBackupIpList;

    @TableField(exist = false)
    private Integer inGroupCount;

    @TableField(exist = false)
    private Object pushInfo;

    @TableField(exist = false)
    private Object pushResult;

    @TableField(exist = false)
    private String lastPushStreamId;

    @TableField(exist = false)
    private Object areaName;

}
