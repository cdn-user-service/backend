package io.ants.modules.utils.vo;

import lombok.Data;

import java.util.List;

@Data
public class AntsDnsRecordListVo {

    @Data
    private class   ObjDataItem{
        private int id;
        private String operation_type;
        private String record_id;
        private String top;
        private String record_type;
        private String value;
        private int tcp;
        private String record_line;
        private String record_line_id;
        private String linetype;
        private String weight;
        private String mx;
        private String ttl;
        private String record_line_name;
    }

    private Integer code=0;
    private String msg;
    private Integer total;
    private List<ObjDataItem> objData;
    private Object soaData;
    private List<String > data;
}
