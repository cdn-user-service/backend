package io.ants.modules.utils.config;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SmsParamVo {

    private String[] tencentParam;
    private JSONObject aliParam;
    private JSONObject smsBaoParam;
}
