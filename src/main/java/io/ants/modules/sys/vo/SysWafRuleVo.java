package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SysWafRuleVo {

    @Data
    public class wafOpObj{
        private String key="";
        private String param="";
        private String handle="";
    }

    @Data
    public class ruleObj{
        private String type="";
        private String handle="";
        private String content="";
    }

    private wafOpObj waf_op=new wafOpObj();
    private List<ruleObj> rule=new ArrayList<>();
    private String remark="";
    private int sys_index=0;
    private int req_sum_5s=0;
    private String botCheckHttpStatusCode;
}
