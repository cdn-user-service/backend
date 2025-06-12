package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "查询套餐用量")
public class QuerySuitBytes {
    @NotBlank
    private String SerialNumber;

    @NotNull
    private Long startTime;

    @NotNull
    private Long endTime;
}
