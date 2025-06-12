package io.ants.modules.utils.vo;

//import com.sun.corba.se.spi.ior.IdentifiableFactory;
import io.ants.common.utils.R;
import lombok.Data;

@Data
public class AntsDnsRVo {
    //{"msg":"更新记录成功","record_id":"1693622514","code":1,"create_at":1693793713310}
    private int code=0;

    private String  msg;

    private Object data;

    private long create_at;

    public R thisVo2R(){
        if (1!=code){
            return R.error(msg);
        }
        return R.ok().put("data",data);
    }
}
