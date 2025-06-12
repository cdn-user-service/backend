package io.ants.modules.app.vo;

import lombok.Data;

@Data
public class ZeroSslApiCertInfoVo {

    private String domains="";

    private String certPem="";

    private String privateKey="";

    private String csr="";

    private String id="";

    private int certificate_validity_days=90;
}
