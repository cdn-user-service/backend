package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class DnsCommonRecordVo {
//        obj.put("recordId",item.getRecordId());
//        obj.put("top",item.getName());
//        obj.put("value",item.getValue());
//        obj.put("line",item.getLine());
//        obj.put("ttl",item.getTTL());
//        obj.put("type",item.getType());
//        obj.put("recordType",item.getType());
    private String recordId="";

    private String top="";

    private String value="";

    private String line="default";

    private String ttl="600";

    private String type="";

    private String recordType="";
}
