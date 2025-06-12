package io.ants.modules.sys.form;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SuitFlowMode {
    Map<Integer,Long> flowData;

    //总流量
    long totalFlow;

    //峰值带宽
    long topFlow;

    public SuitFlowMode(){
        flowData=new HashMap<>();
        totalFlow=0L;
        topFlow=0L;
    }
}
