package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxErrPageAttrVo {
    //{"type":2,"html_1":"<!DOCTYPE html><html lang=\"zh-CN\"><head></head><body></body></html>","html_2":"<!DOCTYPE html><html lang=\"zh-CN\"><head></head><body></body></html>","html_3":""}
    //{"type":1,"html_1":"<!DOCTYPE html><html lang=\"zh-CN\"><head></head><body></body></html>","html_2":"<!DOCTYPE html><html lang=\"zh-CN\"><head></head><body></body></html>","html_3":""}

    //1=sys;2=source;3=site
    private Integer type=1;

    private Integer err_code;

    //1 cdn 平台定义的错误页
    private String html_1="";

    //2 =0,不定义
    private String html_2="";

    //3 自定义
    private String html_3="";


}
