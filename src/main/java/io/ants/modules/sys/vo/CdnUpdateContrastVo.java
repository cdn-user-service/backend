package io.ants.modules.sys.vo;

import com.alibaba.fastjson.JSONObject;
import io.ants.modules.sys.enums.CdnVersionEnum;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

/**
 * 版本更新本地与远程对比
 */
@Data
public class CdnUpdateContrastVo {
    private String key;
    private long checktime;
    private String local;
    private String local_version_date;
    private String name;
    private String remote;
    private String remote_version_date;
    private JSONObject detail;


    public void updateName(){
        if (StringUtils.isNotBlank(this.key)){
            this.setName(CdnVersionEnum.getNameByKey(this.key));
        }
    }
}
