package io.ants.modules.app.form;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("分布查询Page")
public class PageForm {
    @ApiModelProperty(value = "搜索条件KEY1",example = "")
    String search_key1;

    @ApiModelProperty(value = "搜索条件KEY2",example = "")
    String search_key2;

    @ApiModelProperty(value = "搜索条件KEY3",example = "")
    String search_key3;

    @ApiModelProperty(value = "review")
    Integer review;

    @ApiModelProperty(value = "domain_id",example = "1")
    @NotBlank(message="domain_id 不能为空")
    String domain_id;

    @ApiModelProperty(value = "查询页",example = "1")
    @NotNull(message="查询页 不能为空")
    Integer pagenum;

    @ApiModelProperty(value = "每页数量",example = "10")
    @NotNull(message="每页数量 不能为空")
    Integer pagesize;

}
