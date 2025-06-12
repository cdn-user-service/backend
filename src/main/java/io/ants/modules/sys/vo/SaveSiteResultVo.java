package io.ants.modules.sys.vo;

import io.ants.modules.app.entity.TbSiteEntity;
import lombok.Data;

@Data
public class SaveSiteResultVo {
    private Integer code;

    private String msg;

    private TbSiteEntity siteEntity;
}
