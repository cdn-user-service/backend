package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxAddHeadVo {
    //{"type":"custom","header":"x-test-h","content":"$hosst","info":"xxx"}
    //{"type":"Cache-Control","header":"","content":"1111","info":"111"}
    private String type="";
    private String header="";
    private String content="";
    private String info="";
}
