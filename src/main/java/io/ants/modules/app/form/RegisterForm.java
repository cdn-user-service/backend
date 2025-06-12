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

import javax.validation.constraints.NotBlank;

/**
 * 注册表单
 *
 * @author Mark sunlightcs@gmail.com
 */
@Data
@ApiModel(value = "注册表单")
public class RegisterForm {
    private String userId;

    private String user_id;

    @ApiModelProperty(value = "username")
    private String username;

    @ApiModelProperty(value = "手机号")
    private String mobile;

    @ApiModelProperty(value = "邮箱")
    private String mail;


    @ApiModelProperty(value = "密码")
    @NotBlank(message="密码不能为空")
    private String password;


    @ApiModelProperty(value = "验证码(username 为图形验证码，mail|mobile 为发送的验证码) ",example = "0000")
    private String code;

    @ApiModelProperty(value = "uuid",example = "106123456789")
    private String uuid;

}
