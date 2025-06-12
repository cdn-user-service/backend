package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class CdnUpdateQueryVo {

    private long checktime=System.currentTimeMillis();
    private String local_version_date;
    private String remote;
    private String remote_version_date;

}
