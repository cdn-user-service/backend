package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CertSubjectVo {


    private String infos;
    private int version;
    private String SerialNumber;
    private String SubjectDN;
    private Object IssuerDN;
    private Date NotBefore;
    private Date   NotAfter;
    private String SigAlgName;
    private String sign;
    private String publicKey;
    private String subjectAlternativeNames;

}
