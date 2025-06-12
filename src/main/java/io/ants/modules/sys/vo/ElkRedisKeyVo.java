package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class ElkRedisKeyVo {
    private String elasticsearchmethod="http";
    private String elasticsearchhost="127.0.0.1";
    private String elasticsearchpwd="";
    private String elasticsearchport="9200";
    private String elasticsearchca_path="";
}
