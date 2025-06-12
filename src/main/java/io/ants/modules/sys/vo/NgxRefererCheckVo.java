package io.ants.modules.sys.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class NgxRefererCheckVo {
    @NotNull
    private String refererUri;

    @NotNull
    private Integer code;

    private String content;

    private Integer maxAge=0;
}
