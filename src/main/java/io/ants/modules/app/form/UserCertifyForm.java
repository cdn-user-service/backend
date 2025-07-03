package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "支付宝实名提交表单")
public class UserCertifyForm {

    @Schema(description = "认证名")
    private String cert_name;

    @Schema(description = "认证证件号")
    private String cert_no;

}
