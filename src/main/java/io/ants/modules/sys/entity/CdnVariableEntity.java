package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author Administrator
 */
@Data
@TableName("cdn_file_variable")
public class CdnVariableEntity {
    private Integer id;

    @Schema(description = "0=常量;1:计算;2=来之表", example = "0")
    private Integer variableMode;

    private String variableName;

    private String variableValue;

    @Schema(description = "版本号", example = "2.0.*")
    private String version;

    @Schema(description = "状态0：禁用；1：启用", example = "1")
    private Integer status;

    private Date createtime;

}
