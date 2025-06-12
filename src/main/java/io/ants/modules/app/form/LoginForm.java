/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 登录表单
 *
 * @author Mark sunlightcs@gmail.com
 */
@Data
@ApiModel(value = "登录表单")
public class LoginForm {
    @ApiModelProperty(value = "account(username|mail|mobile) ",example = "ahalim")
    @NotNull
    private String account;

//    @ApiModelProperty(value = "手机号",example = "13888888888")
//    private String mobile;
//
//    @ApiModelProperty(value = "邮箱地址",example = "13888888888@qq.com")
//    //@Email(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$",message = "邮箱地址格式错误")
//    private String mail;

    @ApiModelProperty(value = "密码")
    @NotNull
    private String password;


    @ApiModelProperty(value = "验证码",example = "0000")
    private String code;

    @ApiModelProperty(value = "uuid",example = "106123456789")
    private String uuid;


    @ApiModelProperty(value = "google_code",example = "000000")
    private String googleAuthCode;

    private String accessCode;

}
