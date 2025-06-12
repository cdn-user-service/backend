package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxGzipVo {
    /*
    *
     gzip on;
    gzip_buffers 32 4K;
    gzip_http_version 1.1;
    gzip_disable "MSIE [1-6]\.";
    gzip_min_length 1k;
    gzip_comp_level 2;
    gzip_vary on;
    gzip_types ;*/

    private String gzip="on" ;
    private String gzip_buffers="32 4K";
    private String gzip_http_version="1.1";
    private String gzip_disable="MSIE [1-6]\\.";
    private String gzip_min_length="1k";
    private String gzip_comp_level="2";
    private String gzip_vary="on";
    private String gzip_types="";

}
