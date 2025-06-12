package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("cdn_product_attr")
public class CdnProductAttrEntity {

    private Integer id;

    private String name;

    private String unit;

    private String valueType;

    private String value;

    private Integer status=1;

    private Integer weight=0;

    @TableField(exist = false)
    private Object standard;

    @TableField(exist = false)
    private String cnName;

    @TableField(exist = false)
    private String attr;
}
