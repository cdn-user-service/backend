package io.ants.modules.utils.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysLoginKeyConfig implements Serializable {
    private static final long serialVersionUID = 1L;

     private String appKey;

     private String whiteIps;

}
