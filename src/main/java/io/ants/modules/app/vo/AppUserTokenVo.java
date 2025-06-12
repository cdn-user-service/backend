package io.ants.modules.app.vo;

import lombok.Data;

@Data
public class AppUserTokenVo {

    private String token;

    private long expire;

    private String access_token;
}
