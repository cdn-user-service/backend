/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 登录表单
 *
 * @author Mark sunlightcs@gmail.com
 */
@Data
@Schema(name = "登录表单")
public class LoginForm {
    @Schema(description = "account(username|mail|mobile) ", example = "ahalim")
    @NotNull
    private String account;

    // @Schema(description = "手机号",example = "13888888888")
    // private String mobile;
    //
    // @Schema(description = "邮箱地址",example = "13888888888@qq.com")
    // //@Email(regexp =
    // "^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$",message =
    // "邮箱地址格式错误")
    // private String mail;

    @Schema(description = "密码")
    @NotNull
    private String password;

    @Schema(description = "验证码", example = "0000")
    private String code;

    @Schema(description = "uuid", example = "106123456789")
    private String uuid;

    @Schema(description = "google_code", example = "000000")
    private String googleAuthCode;

    private String accessCode;

}
