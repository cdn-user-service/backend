package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.ants.modules.sys.vo.TbCertifyObjVo;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_certify")
public class TbCertifyEntity {
    private Integer id;

    private Long userId;

    private String commonName;

    private String objInfo;

    private long notAfter=0l;

    private String remark;

    private String recordId;

    private String acmeLog="";

    private String applyInfo="";

    private Integer siteId;

    //-1 0:待申请 1:成功 2失败 3自有
    private int status=-1;

    private String apiOrderInfo;

    //0=ACME.SH  1==ZEROSSL API
    private int srcType=0;

    private Date createTime;

    @TableField(exist = false)
    private Object user;

    @TableField(exist = false)
    private TbCertifyObjVo cert;

    @TableField(exist = false)
    private String statusMsg;

    @TableField(exist = false)
    private String applyLog;

    @TableField(exist = false)
    private boolean isCanReApply;

    @TableField(exist = false)
    private int canReApplyMode=-1;
}
