package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "微信 支付表单")
public class WechatPaySumbitOrderForm {
    @ApiModelProperty(value = "请求的唯一标识")
    @NotBlank(message="请求的唯一标识不能为空")
    private String outTradeNo;



    @ApiModelProperty(value = "金额",example = "1")
    @NotBlank(message="金额不能为空")
    private String totalAmount;

    @ApiModelProperty(value = "描述",example = "cdn test")
    private String body;


    @ApiModelProperty(value = "支付方式(取值为 native)",example = "native")
    @NotBlank(message="支付方式不能为空")
    private String mode;
}
