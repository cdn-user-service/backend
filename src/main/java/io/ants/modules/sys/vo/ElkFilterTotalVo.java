package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ElkFilterTotalVo {

    @Data
    private class Unique_uris{
        private  Integer value=0 ;
    }

    @Data
    private class Aggregations{
        private Unique_uris unique_uris;
    }

    private Integer took;
    private boolean time_out;
    private Aggregations aggregations;

    public  Integer getFilterSum(){
        if (null==this.getAggregations()){
            return 0;
        }
        if (null==this.getAggregations().getUnique_uris()){
            return 0;
        }
        return this.getAggregations().getUnique_uris().getValue();
    }

}
