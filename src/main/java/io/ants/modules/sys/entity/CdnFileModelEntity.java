package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cdn_file_model")
public class CdnFileModelEntity {

    private Integer id;

    @Schema(description = "类型0=目录;1=文件", example = "1")
    private Integer objectMode;// '类型0=目录;1=文件',

    private String absolutePathModel;// '路径模板',

    private String fileModel;// 文件模板

    @Schema(description = "版本号", example = "2.0.*")
    private String version;// '版本号'

    @Schema(description = "状态[0:停用;1:启用]", example = "1")
    private Integer status;

    /**
     * 是否需要-t 检测及回滚
     */
    private Integer nginxCheck;

    private Integer weight;

    private Date createtime;

    private Date updatetime;

}
