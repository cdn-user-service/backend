package io.ants.modules.sys.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class PushResultVo {
    private String config;

    private String info;

    private String module;

    // yyyy-mm-dd hh:mm:ss
    private String publish;

    private JSONObject xid;

    private String src;
}
