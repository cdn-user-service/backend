package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "支付宝支付表单")
public class AlipaySubmitOrderForm {
    @ApiModelProperty(value = "请求的唯一标识")
    @NotBlank(message="请求的唯一标识不能为空")
    private String outTradeNo;

    @ApiModelProperty(value = "主题",example = "cdn test")
    @NotBlank(message="主题不能为空")
    private String subject;

    @ApiModelProperty(value = "金额-分" ,example = "1")
    @NotBlank(message="金额不能为空")
    private String totalAmount;

    @ApiModelProperty(value = "描述" ,example = "alipay test pay")
    private String body;

    @ApiModelProperty(value = "支付方式(取值为pc||或h5)",example = "pc")
    @NotBlank(message="支付方式不能为空")
    private String mode;


    /**
     * 登录表单
     *
     * @author Mark sunlightcs@gmail.com
     */
    @Data
    @ApiModel(value = "登录表单")
    public static class LoginForm {

        @ApiModelProperty(value = "用户名", example = "string")
        @NotBlank(message = "用户名不能为空")
        private String username;


        @ApiModelProperty(value = "密码", example = "string")
        @NotBlank(message = "密码不能为空")
        private String password;


        @ApiModelProperty(value = "图形验证码", example = "0000")
        @NotBlank(message = "验证码 不能为空")
        private String captcha;


        @ApiModelProperty(value = "验证码id", example = "")
        @NotBlank(message = "验证码id 不能为空")
        private String uuid;
    }
}
