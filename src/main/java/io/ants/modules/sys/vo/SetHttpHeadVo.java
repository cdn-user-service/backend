package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class SetHttpHeadVo {
    //{"id":0,"type":"custom","header":"123","content":"124","info":"1231"}
    private Integer id;

    private String type;

    private String header;

    private String content;

    private String info;
}
