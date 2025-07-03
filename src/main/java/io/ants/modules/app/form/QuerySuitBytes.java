package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(name = "查询套餐用量")
public class QuerySuitBytes {
    @NotBlank
    private String SerialNumber;

    @NotNull
    private Long startTime;

    @NotNull
    private Long endTime;
}
