package io.ants.modules.sys.form;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class ChangeMasterForm {
    @NotNull
    private String masterIp;

    @Min(1)
    @Max(65535)
    private Integer redisPort;

    @NotNull
    private String redisPwd;
}
