package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @author Administrator
 */
@Data
@TableName("cdn_file_variable")
public class CdnVariableEntity {
    private Integer id;

    @ApiModelProperty(value = "0=常量;1:计算;2=来之表",example = "0")
    private Integer variableMode;


    private String variableName;

    private String variableValue;

    @ApiModelProperty(value = "版本号",example = "2.0.*")
    private String version;

    @ApiModelProperty(value = "状态0：禁用；1：启用",example = "1")
    private Integer status;

    private Date createtime;

}
