package io.ants.modules.sys.vo;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MakeFileParamVo {
    private String modulePath="";
    private Map<Object,Object> valuesMap=new HashMap<>();
    private String valuesString="";
    private String ngxConfPath="";

}
