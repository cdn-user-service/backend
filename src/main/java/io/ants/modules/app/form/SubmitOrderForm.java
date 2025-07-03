package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "提交订单")
public class SubmitOrderForm {

    @Schema(description = "用户ID")
    @NotBlank(message = "用户ID不能为空")
    private Long userId;

    private Integer orderType;

    private Integer targetId;

    private String initJson;

}
