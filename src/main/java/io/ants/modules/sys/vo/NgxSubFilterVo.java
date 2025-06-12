package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class NgxSubFilterVo {
    private Integer status=0;
    private String  fileTypeS="text/html";
    private Integer onceFlag=0;
    private List<String> filterContent;
}
