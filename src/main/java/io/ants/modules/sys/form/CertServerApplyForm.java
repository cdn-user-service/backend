package io.ants.modules.sys.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class CertServerApplyForm {

    private int id;
    private String domains;
    private String top;
    private String callback;
    //3=http 4=dns 5=zeroSsl
    private int usemode;
    private int dnsconfigid;
    private JSONObject zeroverify;

}
