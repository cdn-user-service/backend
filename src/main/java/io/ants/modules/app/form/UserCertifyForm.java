package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "支付宝实名提交表单")
public class UserCertifyForm {



    @ApiModelProperty(value = "认证名")
    private String cert_name;

    @ApiModelProperty(value = "认证证件号")
    private String cert_no;


}
