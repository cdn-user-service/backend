package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "绑定")
public class WechatBindForm {

    private String mail;

    private  String mobile;

    @NotNull(message="openId 不能为空")
    private String openId;

    @ApiModelProperty(value = "code",example = "1111")
    @NotNull(message="code 不能为空")
    private String code;
}
