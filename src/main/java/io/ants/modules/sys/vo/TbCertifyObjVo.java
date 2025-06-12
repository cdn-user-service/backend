package io.ants.modules.sys.vo;

import lombok.Data;




@Data
public class TbCertifyObjVo {
    private String pem_cert="";
    private String private_key="";
    private Integer status=1;
    private long  notAfter=0l;
    private Integer version;
    private long notBefore=0l;
    private String subjectAlternativeNames;

}
