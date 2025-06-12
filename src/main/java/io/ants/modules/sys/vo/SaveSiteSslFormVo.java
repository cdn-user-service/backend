package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SaveSiteSslFormVo {

    public SaveSiteSslFormVo(String siteId,String pem,String key){
        this.siteId=siteId;
        SslInfoVo  pemVo=this.new SslInfoVo();
        pemVo.setValue(pem);
        this.other_ssl_pem.add(pemVo);
        SslInfoVo  keyVo=this.new SslInfoVo();
        keyVo.setValue(key);
        this.other_ssl_key.add(keyVo);
    }

    public SaveSiteSslFormVo(){

    }

    @Data
    public class SslInfoVo{
        private String value;
    }
    private String siteId;
    private int ssl_not_insert=1;
    private List<SslInfoVo> other_ssl_pem=new ArrayList<>();
    private List<SslInfoVo> other_ssl_key=new ArrayList<>();




}
