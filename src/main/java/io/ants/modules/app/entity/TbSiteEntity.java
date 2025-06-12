package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.ants.modules.sys.vo.SiteGroupMiniVo;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_site")
public class TbSiteEntity {

    private  Integer id;

    private Long userId;

    private String serialNumber;

    private String cname;

    private String mainServerName;

    //json obj suit info
    private String suitInfo;

    private Integer status;

    private Date createtime;


    @TableField(exist = false)
    private Object user;

    @TableField(exist = false)
    private Object suit;

    @TableField(exist = false)
    private Object attr;

    @TableField(exist = false)
    private Object ssl;

    @TableField(exist = false)
    private Object job_ssl_apply;

    @TableField(exist = false)
    private SiteGroupMiniVo groupVo=new SiteGroupMiniVo();


    @TableField(exist = false)
    private int job_check_site_cname=0;

}
