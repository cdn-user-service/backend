package io.ants.modules.app.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class ZeroSslAPiCreateCertForm {

    private Long userId = 0L;

    @NotNull
    private String domains = "";

    @NotNull(message = "value is 90 or 365,default is 90")
    private int certificate_validity_days = 90;

    private Integer siteId;

    private Integer certId;

    private Integer id;
}
