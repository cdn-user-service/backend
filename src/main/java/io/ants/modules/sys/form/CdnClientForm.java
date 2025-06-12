package io.ants.modules.sys.form;

import lombok.Data;

@Data
public class CdnClientForm {

    private  Integer id;

    private String clientIp;

    private Integer clientType;

    private String area;

    private String line;

    private String remark;

    private String clientInfo;

    private Integer areaId;
}
