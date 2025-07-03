package io.ants.modules.sys.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class AdminRechargeForm {

    @NotNull
    private Long userId;

    @NotNull
    private Integer amount;

    private String remark = "";

}
