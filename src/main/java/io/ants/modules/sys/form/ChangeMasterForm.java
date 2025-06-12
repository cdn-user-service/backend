package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
