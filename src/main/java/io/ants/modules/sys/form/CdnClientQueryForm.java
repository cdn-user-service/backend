package io.ants.modules.sys.form;

import lombok.Data;

@Data
public class CdnClientQueryForm {
    //{"ip":"","page":1,"limit":20}
    private Integer page=1;
    private Integer limit=20;

    private String ip="";
    private Integer clientType;
    //节点分组
    private Integer areaId;
    //业务分组
    private Integer group_id;

    //节点健康状态 筛选 null=全部 0=故障(0-0) 1=在线(1-10) 2=正常(>10)
    private Integer stableScoreStatus;

}
