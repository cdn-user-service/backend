package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ElkFilterSiteIOPFlowFilterVo {

    @Data
    private class  FlowIn{
        private long value=0l;
    }
    @Data
    private class  FlowOut{
        private long value=0l;
    }
    @Data
    private class BucketsItem{
        private Integer doc_count=0;
        private FlowIn flow_in;
        private FlowOut flow_out;
    }


    @Data
    public class Flow{
        private   BucketsItem flow_buckets;
    }


    @Data
    public class Aggregations{
        private Flow flow;
    }

    private Integer took=0;
    private boolean time_out=false;
    private Aggregations aggregations;


    public  long getSiteFlowData(){

        if (null==this.aggregations){
            return 0l;
        }
        if (null==this.aggregations.getFlow()){
            return 0l;
        }
        if (null==this.aggregations.getFlow().getFlow_buckets()  ){
            return 0l;
        }
        if ( null==this.aggregations.getFlow().getFlow_buckets().getFlow_in() ||  null==this.aggregations.getFlow().getFlow_buckets().getFlow_out()){
            return 0l;
        }
        return this.aggregations.getFlow().getFlow_buckets().getFlow_in().getValue()+this.aggregations.getFlow().getFlow_buckets().getFlow_out().getValue();
    }
}
