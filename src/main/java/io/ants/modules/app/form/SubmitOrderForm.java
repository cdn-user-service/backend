package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "提交订单")
public class SubmitOrderForm {

    @ApiModelProperty(value = "用户ID")
    @NotBlank(message="用户ID不能为空")
    private Long userId;

    private Integer orderType;

    private Integer targetId;

    private String initJson;


}
