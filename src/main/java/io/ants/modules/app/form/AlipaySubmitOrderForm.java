package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "支付宝支付表单")
public class AlipaySubmitOrderForm {
    @Schema(description = "请求的唯一标识")
    @NotBlank(message = "请求的唯一标识不能为空")
    private String outTradeNo;

    @Schema(description = "主题", example = "cdn test")
    @NotBlank(message = "主题不能为空")
    private String subject;

    @Schema(description = "金额-分", example = "1")
    @NotBlank(message = "金额不能为空")
    private String totalAmount;

    @Schema(description = "描述", example = "alipay test pay")
    private String body;

    @Schema(description = "支付方式(取值为pc||或h5)", example = "pc")
    @NotBlank(message = "支付方式不能为空")
    private String mode;

    /**
     * 登录表单
     *
     * @author Mark sunlightcs@gmail.com
     */
    @Data
    @Schema(name = "登录表单")
    public static class LoginForm {

        @Schema(description = "用户名", example = "string")
        @NotBlank(message = "用户名不能为空")
        private String username;

        @Schema(description = "密码", example = "string")
        @NotBlank(message = "密码不能为空")
        private String password;

        @Schema(description = "图形验证码", example = "0000")
        @NotBlank(message = "验证码 不能为空")
        private String captcha;

        @Schema(description = "验证码id", example = "")
        @NotBlank(message = "验证码id 不能为空")
        private String uuid;
    }
}
