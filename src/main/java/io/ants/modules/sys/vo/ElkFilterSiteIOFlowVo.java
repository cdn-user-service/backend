package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ElkFilterSiteIOFlowVo {

    @Data
    private class  Total{
        private Long value=0l;
    }
    @Data
    private class BucketsItem{
        private String key="";
        private Integer doc_count=0;
        private Total total;
    }


    @Data
    public class Flow{
        private    List<BucketsItem>  buckets;
    }

    @Data
    public class Aggregations{
        private Flow flow;
    }

    private Integer took=0;
    private boolean time_out=false;
    private Aggregations aggregations;


    public  Map<String,Long> getSiteFlowData(){
        Map<String,Long> res=new HashMap<>();
        if (null==this.aggregations){
            return res;
        }
        if (null==this.aggregations.getFlow()){
            return res;
        }
        if (null==this.aggregations.getFlow().buckets ||this.aggregations.getFlow().buckets.isEmpty() ){
            return res;
        }
        for (BucketsItem item:this.aggregations.getFlow().buckets){
            String key=item.getKey();
            res.put(key,item.getTotal().getValue());
        }
        return res;
    }
}
