package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ElkFilterCnameTotalVo {

    @Data
    public class TotalObj{
        private Integer value=0;

        private String relation;
    }

    @Data
    public class HitsObj{
        private TotalObj total;
    }

    private int took;

    private boolean time_out;

    private HitsObj hits;

}
