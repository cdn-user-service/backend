package io.ants.modules.sys.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CdnConsumeUsedInfoVo {

    private   BigDecimal mainFlowGb=BigDecimal.ZERO;

    private   BigDecimal addFlowGb=BigDecimal.ZERO;

    private   BigDecimal totalFlowGb=BigDecimal.ZERO;
}
