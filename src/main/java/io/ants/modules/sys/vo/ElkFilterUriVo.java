package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ElkFilterUriVo {

    @Data
    private class Buckets {
        private String key;
        private Integer doc_count;
    }

    @Data
    private class Total {
        private Integer doc_count_error_upper_bound;
        private Integer sum_other_doc_count;
        private List<Buckets> buckets;
    }

    @Data
    private class Uri_total{
        private Integer doc_count;
        private Total total;
    }

    @Data
   private class Aggregations{
        private Uri_total uri_total;
   }

   private Integer took;
   private boolean time_out;
   private Aggregations aggregations;

   public List<String> getKeyList(){
       List<String> list=new ArrayList<>();
       if(null==this.getAggregations() || null==this.getAggregations().uri_total || null==this.getAggregations().uri_total.total || null==this.getAggregations().uri_total.total.buckets){
           return list;
       }
       for (Buckets b:this.getAggregations().uri_total.total.buckets){
           list.add(b.getKey());
       }
       return list;
   }
}
