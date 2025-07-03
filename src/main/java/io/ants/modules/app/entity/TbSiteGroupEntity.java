package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@TableName("tb_site_group")
public class TbSiteGroupEntity {

    private Integer id;

    private Long userId;

    @NotNull
    private String name;

    @NotNull
    private String siteIds;

    private Integer weight = 0;

    private String remark;

    private Integer createUserType;

    private Date createTime = new Date();

    @TableField(exist = false)
    private List<TbSiteEntity> siteList;

}
