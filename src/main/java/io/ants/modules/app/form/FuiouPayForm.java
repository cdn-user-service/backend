package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "富友支付 表单")
public class FuiouPayForm {
    @Schema(description = "请求的唯一标识")
    @NotBlank(message = "请求的唯一标识不能为空")
    private String outTradeNo;

    @Schema(description = "金额|单位分", example = "1")
    @NotBlank(message = "金额不能为空")
    private String totalAmount;

    @Schema(description = "描述", example = "dns test")
    private String body;

    @Schema(description = "支付方式(取值为 ALIPAY|WECHAT|UNIONPAY)", example = "ALIPAY")
    @NotBlank(message = "支付方式不能为空")
    private String mode;
}
