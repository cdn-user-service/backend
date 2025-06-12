package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class ElkFilterStreamIOFlowVo {


    @Data
    public class KeyValue{
        private long value=0;
    }

    @Data
    public class BucketsItem{
        private Integer  doc_count=1;
        private KeyValue flow_out;
        private KeyValue flow_in;
        private KeyValue flow_total;
    }


    @Data
    public class Flow{
        private  List<BucketsItem> buckets;
    }

    @Data
    public class Aggregations{
        private Flow flow;
    }

    private Integer took;
    private Aggregations aggregations;

}
