package io.ants.modules.app.constant;

import lombok.Data;

@Data
public class SourceBaseInfo {

    private String ProtocolType;

    private String ListeningPort;

    private String SourceProtocol;

    private String EquilibriumMode;

    private String SourceTarget;

    //["1.1.1.1,80,1,10","127.1.1.1,80,1,10"]
    private String Line;

}
