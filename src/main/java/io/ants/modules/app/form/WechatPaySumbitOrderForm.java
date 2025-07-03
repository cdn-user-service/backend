package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "微信 支付表单")
public class WechatPaySumbitOrderForm {
    @Schema(description = "请求的唯一标识")
    @NotBlank(message = "请求的唯一标识不能为空")
    private String outTradeNo;

    @Schema(description = "金额", example = "1")
    @NotBlank(message = "金额不能为空")
    private String totalAmount;

    @Schema(description = "描述", example = "cdn test")
    private String body;

    @Schema(description = "支付方式(取值为 native)", example = "native")
    @NotBlank(message = "支付方式不能为空")
    private String mode;
}
