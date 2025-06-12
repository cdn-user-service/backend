package io.ants.modules.sys.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CertRemarkVo {

    @NotNull
    private Integer id;

    private Long userId;


    private String remark;
}
