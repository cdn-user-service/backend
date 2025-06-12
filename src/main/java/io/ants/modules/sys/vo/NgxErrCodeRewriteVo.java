package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxErrCodeRewriteVo {

    private int errorCode;

    private int rewriteCode;

    private String rewriteParam;
}
