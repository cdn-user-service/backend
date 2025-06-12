package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class NgxWafRuleVo {

    private Integer id;
    private Integer r_type=0;
    private String remark="";
    private List<RuleInfo> rule;
    private WafOp waf_op;

    private int req_sum_5s=0;
    private int sys_index=0;

    @Data
    public class RuleInfo{
        private String type;
        private String handle;
        private String content;
    }



    @Data
    public class WafOp{
        private String key;
        private String param="0";
        private String handle;
    }

}
