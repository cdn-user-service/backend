package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "富友支付 表单")
public class FuiouPayForm {
    @ApiModelProperty(value = "请求的唯一标识")
    @NotBlank(message="请求的唯一标识不能为空")
    private String outTradeNo;

    @ApiModelProperty(value = "金额|单位分",example = "1")
    @NotBlank(message="金额不能为空")
    private String totalAmount;

    @ApiModelProperty(value = "描述",example = "dns test")
    private String body;


    @ApiModelProperty(value = "支付方式(取值为 ALIPAY|WECHAT|UNIONPAY)" ,example = "ALIPAY")
    @NotBlank(message="支付方式不能为空")
    private String mode;
}
