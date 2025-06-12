package io.ants.modules.app.constant;

import lombok.Data;

@Data
public class CacheConfig {
    private String type;

    private String content;

    private Integer timeout;

    private Integer weight;

}
