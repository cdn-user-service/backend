package io.ants.modules.utils.config.wechat;


import io.ants.modules.utils.service.wechat.IWXPayDomain;
import io.ants.modules.utils.service.wechat.WXPayConstants;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.*;


@NoArgsConstructor
@Data
public class ConfigUtil extends AbstractWXPayConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private  byte[] certData;

    private  String appId;
    private  String mchId;
    private  String key;
    private  String secret;
    private  String certPath;
    private  String notifyUrl;


    //private ConfigUtil configUtil;




    @Override
    public String getAppID() {
        return this.appId;
    }

    @Override
    public String getMchID() {
        return this.mchId;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    //public final String TRANSFERS_PAY = "https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers"; // 企业付款

    public String getNotifyUrl(){
        return this.notifyUrl;
    }

    @Override
    public String getCertPath() {
        return this.certPath;
    }

    @Override
    public InputStream getCertStream() {
        return new ByteArrayInputStream(this.certData);
    }

    @Override
    public IWXPayDomain getWXPayDomain() {
        IWXPayDomain iwxPayDomain = new IWXPayDomain() {
            @Override
            public void report(String domain, long elapsedTimeMillis, Exception ex) {

            }

            @Override
            public DomainInfo getDomain(AbstractWXPayConfig config) {
                return new DomainInfo(WXPayConstants.DOMAIN_API,true);
            }
        };
        return iwxPayDomain;
    }


    @Override
    public int getHttpConnectTimeoutMs() {
        return 8000;
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return 10000;
    }



    public void setCertPath(String certPath) throws IOException {
        this.certPath=certPath;
        File file = new File(certPath);
        if(file.isFile()){
            InputStream certStream = new FileInputStream(file);
            this.certData = new byte[(int) file.length()];
            certStream.read(this.certData);
            certStream.close();
        }
    }
}
