package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class NgxPurgeCacheVo {
    private String schema;
    private String serverName;
    private Integer ignoreUrlParamFlag=1;
    private List<String> urlList=new ArrayList<>();
    private List<Pattern> urlPatternList=new ArrayList<>();
}
