package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ElkFilterSiteIOFlowFilterVo {

    @Data
    private class  FlowTotal{
        private Long value=0l;
    }
    @Data
    private class BucketsItem{
        private String key="";
        private Integer doc_count=0;
        private FlowTotal flow_total;
    }


    @Data
    public class FlowBuckets{
        private    List<BucketsItem>  buckets;
    }


    @Data
    public class Flow{
        private   FlowBuckets flow_buckets;
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
        if (null==this.aggregations.getFlow().getFlow_buckets()  ){
            return res;
        }
        if ( null==this.aggregations.getFlow().getFlow_buckets().getBuckets() ||  this.aggregations.getFlow().getFlow_buckets().getBuckets().isEmpty() ){
            return res;
        }
        for (BucketsItem item:this.aggregations.getFlow().getFlow_buckets().getBuckets()){
            String key=item.getKey();
            res.put(key,item.getFlow_total().getValue());
        }
        return res;
    }
}
