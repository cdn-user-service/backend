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

import jakarta.validation.constraints.NotBlank;

/**
 * 注册表单
 *
 * @author Mark sunlightcs@gmail.com
 */
@Data
@Schema(name = "注册表单")
public class RegisterForm {
    private String userId;

    private String user_id;

    @Schema(description = "username")
    private String username;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String mail;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码(username 为图形验证码，mail|mobile 为发送的验证码) ", example = "0000")
    private String code;

    @Schema(description = "uuid", example = "106123456789")
    private String uuid;

}
