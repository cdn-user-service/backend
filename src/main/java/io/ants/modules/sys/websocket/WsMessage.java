package io.ants.modules.sys.websocket;

import io.ants.common.utils.DataTypeConversionUtil;
import lombok.Data;

@Data
public class WsMessage {
    private long timeTmp=System.currentTimeMillis();
    private String msg;

    public String toString(){
        return DataTypeConversionUtil.entity2jonsStr(this);
    }
}
