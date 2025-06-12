package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author Administrator
 */
@Data
@TableName("cdn_client_area")
public class CdnClientAreaEntity {
    private Integer id;


    private String name;


    private String remark;

    @TableField(exist = false)
    private Integer nodeNum;


}
