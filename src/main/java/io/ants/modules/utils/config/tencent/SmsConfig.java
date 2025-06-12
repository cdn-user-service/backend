package io.ants.modules.utils.config.tencent;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.io.Serializable;

@Data
public class SmsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String secretid;

    private String secretkey;

    private String sdkappId;

    private String signname;

    private JSONArray templateIds;

}
