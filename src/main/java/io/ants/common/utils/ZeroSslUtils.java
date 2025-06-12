package io.ants.common.utils;

import com.alibaba.fastjson.JSONObject;

import io.ants.modules.app.vo.ZeroSslApiCertInfoVo;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.security.auth.x500.X500Principal;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;


public class ZeroSslUtils {
    //39a6081fce2cb72285f4c816f943fd03
    //db40b0ea75f95744b5773e6be589defb
    private static  String  access_key="e371a36770e62d0a70631c7a09b9d73e";





    /**
     * 算法提供者 Bouncy Castle
     */
    private static final Provider BC = new BouncyCastleProvider();


    public static void setAccess_key(String access_key){
        ZeroSslUtils.access_key=access_key;
    }

    /**
     * 生成 PKCS#10 证书请求
     *
     * @param isRsaNotEcc {@code true}：使用 RSA 加密算法；{@code false}：使用 ECC（SM2）加密算法
     * @return RSA P10 证书请求 Base64 字符串
     * @throws NoSuchAlgorithmException  当指定的密钥对算法不支持时
     * @throws InvalidAlgorithmParameterException 当采用的 ECC 算法不适用于该密钥对生成器时
     * @throws //OperatorCreationException 当创建签名者对象失败时
     * @throws //IOException               当打印 OpenSSL PEM 格式文件字符串失败时
     */
    public static String generateCsr(boolean isRsaNotEcc, ZeroSslApiCertInfoVo cInfo) {
        try{
            //.out.println("generate_Csr:"+isRsaNotEcc+","+cInfo.getDomains());
            // throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, OperatorCreationException, IOException

            // 使用 RSA/ECC 算法，生成密钥对（公钥、私钥）
            KeyPairGenerator generator = KeyPairGenerator.getInstance(isRsaNotEcc ? "RSA" : "EC", BC);
            if (isRsaNotEcc) {
                // RSA
                generator.initialize(2048);
            } else {
                // ECC
                generator.initialize(new ECGenParameterSpec("sm2p256v1"));
            }
            KeyPair keyPair = generator.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // 打印私钥，注意：请务必保存您的私钥
            String privateKeyStr=   printOpensslPemFormatKeyFileContent(privateKey, isRsaNotEcc);
            cInfo.setPrivateKey(privateKeyStr);

            // 按需添加证书主题项，
            // 有些 CSR 不需要我们在主题项中添加各字段,
            // 如 `C=CN, CN=吴仙杰, E=wuxianjiezh@gmail.com, OU=3303..., L=杭州, S=浙江`，
            // 而是通过额外参数提交，故这里我只简单地指定了国家码
            String name="C=CN";
            if (StringUtils.isNotBlank(cInfo.getDomains())){
                String[] dms=cInfo.getDomains().split(",");
                name="C=CN,CN="+dms[0];
            }
            X500Principal subject = new X500Principal(name);

            // 使用私钥和 SHA256WithRSA/SM3withSM2 算法创建签名者对象
            ContentSigner signer = new JcaContentSignerBuilder(isRsaNotEcc ? "SHA256WithRSA" : "SM3withSM2")
                    .setProvider(BC)
                    .build(privateKey);

            // 创建 CSR
            PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, publicKey);
            PKCS10CertificationRequest csr = builder.build(signer);

            // 打印 OpenSSL PEM 格式文件字符串
            printOpensslPemFormatCsrFileContent(csr);

            // 以 Base64 字符串形式返回 CSR
            String b64csr=Base64.getEncoder().encodeToString(csr.getEncoded());
            cInfo.setCsr(b64csr);
            return b64csr;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 打印 OpenSSL PEM 格式文件字符串的 SSL证书密钥 KEY 文件内容
     *
     * @param privateKey 私钥
     * @param isRsaNotEcc {@code true}：使用 RSA 加密算法；{@code false}：使用 ECC（SM2）加密算法
     */
    private static String printOpensslPemFormatKeyFileContent(PrivateKey privateKey, boolean isRsaNotEcc) {
        try{
            // throws IOException
            PemObject pem = new PemObject(isRsaNotEcc ? "PRIVATE KEY" : "EC PRIVATE KEY", privateKey.getEncoded());
            StringWriter str = new StringWriter();
            PemWriter pemWriter = new PemWriter(str);
            pemWriter.writeObject(pem);
            pemWriter.close();
            str.close();
            // System.out.println(str.toString());
            return str.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 打印 OpenSSL PEM 格式文件字符串的 SSL 证书请求 CSR 文件内容
     *
     * @param csr 证书请求对象
     */
    private static void printOpensslPemFormatCsrFileContent(PKCS10CertificationRequest csr) {
        try{
            PemObject pem = new PemObject("CERTIFICATE REQUEST", csr.getEncoded());
            StringWriter str = new StringWriter();
            PemWriter pemWriter = new PemWriter(str);
            pemWriter.writeObject(pem);
            pemWriter.close();
            str.close();
            //System.out.println(str.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        //throws IOException

    }


    public static R createCert(ZeroSslApiCertInfoVo cInfo){
        String url="https://api.zerossl.com/certificates?access_key="+access_key;
        JSONObject postData=new JSONObject();
        postData.put("certificate_domains",cInfo.getDomains());
        postData.put("certificate_validity_days",cInfo.getCertificate_validity_days());
        ZeroSslUtils.generateCsr(true,cInfo);
        R r= verifyCsr(cInfo.getCsr());
        if (1!=r.getCode()){
            return r;
        }
        postData.put("certificate_csr",cInfo.getCsr());
        String res= HttpRequest.sendPostJsonStr(url,postData.toJSONString());
        //System.out.println(res);
        //System.out.println(cInfo);
        if (StringUtils.isNotBlank(res)){
            JSONObject obj=DataTypeConversionUtil.string2Json(res);
            if (null!=obj && obj.containsKey("id") && obj.containsKey("status")){
                return R.ok().put("data",obj);
            }
        }
        return R.error(res);
    }

    public static R verifyCsr(String csr){
        String url="https://api.zerossl.com/validation/csr?access_key="+access_key;
        JSONObject postData=new JSONObject();
        postData.put("csr",csr);
        String res= HttpRequest.sendPostJsonStr(url,postData.toJSONString());
        System.out.println(res);
        if (StringUtils.isNotBlank(res)){
            JSONObject obj=DataTypeConversionUtil.string2Json(res);
            if (null!=obj && obj.containsKey("valid") && obj.getString("valid").equals("true") ){
                return R.ok().put("data",obj);
            }
        }
        return R.error(res);
    }

    public static R  verifyDomains(String id){
        String url="https://api.zerossl.com/certificates/"+id+"/challenges?access_key="+access_key;
        JSONObject postData=new JSONObject();
        postData.put("validation_method","HTTP_CSR_HASH");
        String res= HttpRequest.sendPostJsonStr(url,postData.toJSONString());
        //System.out.println(res);
        if (StringUtils.isNotBlank(res)){
            JSONObject obj=DataTypeConversionUtil.string2Json(res);
            if (null!=obj && obj.containsKey("status") && obj.getString("status").equals("pending_validation") ){
                return R.ok().put("data",obj);
            }
        }
        return R.error(res);
    }


    public static R getCertStatus(String id){
        String url="https://api.zerossl.com/certificates/"+id+"/?access_key="+access_key;
        String res= HttpRequest.sendGet(url,null);
        //System.out.println(res);
        if (StringUtils.isNotBlank(res)){
            JSONObject obj=DataTypeConversionUtil.string2Json(res);
            if (null!=obj && obj.containsKey("status") ){
                //status: draft, pending_validation, issued, revoked, cancelled, expired
                if(obj.getString("status").equals("issued")){
                    return R.ok().put("data",obj);
                }
            }
        }
        return R.error(res);
    }


    public static R  downloadCert(String id){
        //api.zerossl.com/certificates/{id}/download/return
        String url="https://api.zerossl.com/certificates/"+id+"/download/return?access_key="+access_key+"&include_cross_signed=1";
        //String url="https://api.zerossl.com/certificates/"+id+"/download/return?access_key="+access_key;
        String res= HttpRequest.sendGet(url,null);
        //System.out.println(res);
        if (StringUtils.isNotBlank(res)){
            JSONObject obj=DataTypeConversionUtil.string2Json(res);
            if (null!=obj && obj.containsKey("certificate.crt") && obj.containsKey("ca_bundle.crt")){
                String pemStr=obj.getString("certificate.crt")+obj.getString("ca_bundle.crt");
                return R.ok().put("data",pemStr);
            }
        }
        return R.error(res);
    }

    public static R gEab(){
        String url="https://api.zerossl.com/acme/eab-credentials?access_key="+access_key;
        String res= HttpRequest.sendPost(url,"");
        System.out.println(res);
        //{"success":true,"eab_kid":"oSAhk7AQ4ZD3qJSqsHWn2A","eab_hmac_key":"G7cCvEX6ywa4cLW6tqvMfvBrT22h_Hed5pu4m0nKXpcql18SL1WZIOZcsRyRwV6eEiqsXT4FJB621LAsummB2Q"}
        return R.error(res);
    }

    public static void main(String[] args) {

        ZeroSslApiCertInfoVo cInfo= new ZeroSslApiCertInfoVo() ;
        cInfo.setDomains("c12.cdntest.91hu.top,c14.cdntest.91hu.top,c15.cdntest.91hu.top");
        //R r=createCert(cInfo);
        //System.out.println(r);
        //{"id":"df0fd6eff594138e7096a1d053caea83","type":"1","common_name":"cdntest3.91hu.top","additional_domains":"","created":"2023-12-18 02:47:44","expires":"2024-03-17 00:00:00","status":"draft","validation_type":null,"validation_emails":null,"replacement_for":"","validation":{"email_validation":{"cdntest3.91hu.top":["admin@cdntest3.91hu.top","administrator@cdntest3.91hu.top","hostmaster@cdntest3.91hu.top","postmaster@cdntest3.91hu.top","webmaster@cdntest3.91hu.top","admin@91hu.top","administrator@91hu.top","hostmaster@91hu.top","postmaster@91hu.top","webmaster@91hu.top"]},"other_methods":{"cdntest3.91hu.top":{"file_validation_url_http":"http:\/\/cdntest3.91hu.top\/.well-known\/pki-validation\/C210B173DE643CE76367B9D280447181.txt","file_validation_url_https":"https:\/\/cdntest3.91hu.top\/.well-known\/pki-validation\/C210B173DE643CE76367B9D280447181.txt","file_validation_content":["EA6E695FF58999DB38725EA88E42419F2D774A2D64A7009C1EC79B47FF76BFCA","comodoca.com","b36003e9d55dbae"],"cname_validation_p1":"_C210B173DE643CE76367B9D280447181.cdntest3.91hu.top","cname_validation_p2":"EA6E695FF58999DB38725EA88E42419F.2D774A2D64A7009C1EC79B47FF76BFCA.b36003e9d55dbae.comodoca.com"}}}}
        // nginx conf add:   location ~ "^/.well-known/pki-validation/*"{
        //        return 200 "EA6E695FF58999DB38725EA88E42419F2D774A2D64A7009C1EC79B47FF76BFCA\ncomodoca.com\nb36003e9d55dbae";
        //    }
        //verifyDomains("df0fd6eff594138e7096a1d053caea83");
        //{"id":"df0fd6eff594138e7096a1d053caea83","type":"1","common_name":"cdntest3.91hu.top","additional_domains":"","created":"2023-12-18 02:47:44","expires":"2024-03-17 00:00:00","status":"pending_validation","validation_type":"HTTP_CSR_HASH","validation_emails":"","replacement_for":"","fingerprint_sha1":null,"brand_validation":null,"validation":{"email_validation":{"cdntest3.91hu.top":["admin@cdntest3.91hu.top","administrator@cdntest3.91hu.top","hostmaster@cdntest3.91hu.top","postmaster@cdntest3.91hu.top","webmaster@cdntest3.91hu.top","admin@91hu.top","administrator@91hu.top","hostmaster@91hu.top","postmaster@91hu.top","webmaster@91hu.top"]},"other_methods":{"cdntest3.91hu.top":{"file_validation_url_http":"http:\/\/cdntest3.91hu.top\/.well-known\/pki-validation\/C210B173DE643CE76367B9D280447181.txt","file_validation_url_https":"https:\/\/cdntest3.91hu.top\/.well-known\/pki-validation\/C210B173DE643CE76367B9D280447181.txt","file_validation_content":["EA6E695FF58999DB38725EA88E42419F2D774A2D64A7009C1EC79B47FF76BFCA","comodoca.com","b36003e9d55dbae"],"cname_validation_p1":"_C210B173DE643CE76367B9D280447181.cdntest3.91hu.top","cname_validation_p2":"EA6E695FF58999DB38725EA88E42419F.2D774A2D64A7009C1EC79B47FF76BFCA.b36003e9d55dbae.comodoca.com"}}}}
        //getCertStatus("df0fd6eff594138e7096a1d053caea83");
        //status draft, pending_validation, issued, revoked, cancelled, expired
        //while issued
        //{"id":"df0fd6eff594138e7096a1d053caea83","type":"1","common_name":"cdntest3.91hu.top","additional_domains":"","created":"2023-12-18 02:47:44","expires":"2024-03-17 23:59:59","status":"issued","validation_type":"HTTP_CSR_HASH","validation_emails":"","replacement_for":"","fingerprint_sha1":"12958db6b2791390906427dc6fee3e32e2ac8e1c","brand_validation":null,"validation":{"email_validation":{"cdntest3.91hu.top":["admin@cdntest3.91hu.top","administrator@cdntest3.91hu.top","hostmaster@cdntest3.91hu.top","postmaster@cdntest3.91hu.top","webmaster@cdntest3.91hu.top","admin@91hu.top","administrator@91hu.top","hostmaster@91hu.top","postmaster@91hu.top","webmaster@91hu.top"]},"other_methods":{"cdntest3.91hu.top":{"file_validation_url_http":"http:\/\/cdntest3.91hu.top\/.well-known\/pki-validation\/C210B173DE643CE76367B9D280447181.txt","file_validation_url_https":"https:\/\/cdntest3.91hu.top\/.well-known\/pki-validation\/C210B173DE643CE76367B9D280447181.txt","file_validation_content":["EA6E695FF58999DB38725EA88E42419F2D774A2D64A7009C1EC79B47FF76BFCA","comodoca.com","b36003e9d55dbae"],"cname_validation_p1":"_C210B173DE643CE76367B9D280447181.cdntest3.91hu.top","cname_validation_p2":"EA6E695FF58999DB38725EA88E42419F.2D774A2D64A7009C1EC79B47FF76BFCA.b36003e9d55dbae.comodoca.com"}}}}
        //R r= downloadCert("df0fd6eff594138e7096a1d053caea83");
        //{"certificate.crt":"-----BEGIN CERTIFICATE-----\nMIIGcTCCBFmgAwIBAgIQOt0A3Ibbb7YpQLucDD1TvjANBgkqhkiG9w0BAQwFADBL\nMQswCQYDVQQGEwJBVDEQMA4GA1UEChMHWmVyb1NTTDEqMCgGA1UEAxMhWmVyb1NT\nTCBSU0EgRG9tYWluIFNlY3VyZSBTaXRlIENBMB4XDTIzMTIxODAwMDAwMFoXDTI0\nMDMxNzIzNTk1OVowHDEaMBgGA1UEAxMRY2RudGVzdDMuOTFodS50b3AwggEiMA0G\nCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCvUqiFH7OMJRmqNY8tlFuX9Rak\/mZ1\no4riYPMpQiMJn\/tDg9oRFQIcRegblzpz1aYLOvUPtXHcPskc\/CSayVd01Yk11h1k\nTvYjqLsvuYniYPL4if0\/uH1I4phCV1rTZ0lN6ft4VUF5pHpaWmvjEuYtc3vKYkuY\nomOqeiEkaCMBHDBAvBllu\/qEM9j4o+vSr0Q81j\/5crFBS+HzUTgKFZjZyv3B9\/7l\nMIS+qMM7MGgt1bmd2UaFrVzP9vNfrhAmlsFibZwgWUVAZKDkoS\/B74dOSLGkCuEt\nN2eZQVPKBauIR4ohcLqKuG5l2AVVeakNxI1\/3JuR3N7ol5LaoY3zFyANAgMBAAGj\nggJ+MIICejAfBgNVHSMEGDAWgBTI2XhootkZaNU9ct5fCj7ctYaGpjAdBgNVHQ4E\nFgQU8VIxxXUhWkATyDLvg3CP0Wx4secwDgYDVR0PAQH\/BAQDAgWgMAwGA1UdEwEB\n\/wQCMAAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMEkGA1UdIARCMEAw\nNAYLKwYBBAGyMQECAk4wJTAjBggrBgEFBQcCARYXaHR0cHM6Ly9zZWN0aWdvLmNv\nbS9DUFMwCAYGZ4EMAQIBMIGIBggrBgEFBQcBAQR8MHowSwYIKwYBBQUHMAKGP2h0\ndHA6Ly96ZXJvc3NsLmNydC5zZWN0aWdvLmNvbS9aZXJvU1NMUlNBRG9tYWluU2Vj\ndXJlU2l0ZUNBLmNydDArBggrBgEFBQcwAYYfaHR0cDovL3plcm9zc2wub2NzcC5z\nZWN0aWdvLmNvbTCCAQUGCisGAQQB1nkCBAIEgfYEgfMA8QB2AHb\/iD8KtvuVUcJh\nzPWHujS0pM27KdxoQgqf5mdMWjp0AAABjHrXZSkAAAQDAEcwRQIhAM+i7SvOfNhT\n\/nwZ2WwCqxn4uOobB7sT+ciHLE8oaRhHAiBvSGuBZhAAlHpfg5ClzLP1K8ynOdn7\nTEmu9i7IXdVVigB3ADtTd3U+LbmAToswWwb+QDtn2E\/D9Me9AA0tcm\/h+tQXAAAB\njHrXZVAAAAQDAEgwRgIhAMCIrIc083jvK\/VbZwmg2UbmCvNArNo7dFkpygQVEHCG\nAiEA4WyNiqOs0jJzJ9FjNDwfQ562WdX1OVdEhQ9rkkPdWpwwHAYDVR0RBBUwE4IR\nY2RudGVzdDMuOTFodS50b3AwDQYJKoZIhvcNAQEMBQADggIBAAdWN9MtRxEfMA+5\ntvCFR4USX5E7YqhZISDLTKSaMZ6km2JdAoj8uJ+XZb\/Ir6qrAdkuQfSBtMlcAqJH\nt7MjEaCRSV+7Zrt4NFMQuEQ94WyEla\/rooCajOSGjS2zRg1BMnR3E2\/yGvF+fMmo\nkdcoNw4vZR4j3aBEu3vhMlDJH3XknQnGSCZpVLxZFcjXDYrxNTxsJAq0Mv5xqxec\nBEDHf35BoE4zI9o6mCkhq0O0STi+KAI4AP2DgPG8Uiktvpda413BKZsNMZeYtytT\nEJfROYkpQeVyKdvx5PplN2QYEakgo94ImJnPPRcUr+qU2SWXQu5m7rZV8c1eAJNU\nfDMXnFP7Bxu9g\/GrQdV6\/uEhfHT1Vmmq0aHs1JLggA\/5zjcI8jFzjb0d7vr\/eIv0\nTv1eNn5OcE48rmZUHMJXpRNAOux8jXjL7AX4xw+bJNjJ6S8VtnL\/w0M4Kaw6AF5o\nasOXI4GcfChOVZCHH9hSzR8m9J+v2tNxjsmXY6YVJdHYfSV28h\/FDcQPHUHdIMcn\nyGL\/7yec5pf3nwYhBZoxlo2Lkgu2o2a7id9IeRkhYmMjaXyUbjdHjYQKFhbYzIlW\nU1J8mQPGrp2TBvt3EGMVPeMAMFtySdBO8zMbDy23RdfLzPZJMOhSA4s6CGXH6t7x\ntslZ+0z82AfjBSzTBqSi8I36PBG7\n-----END CERTIFICATE-----\n","ca_bundle.crt":"-----BEGIN CERTIFICATE-----\nMIIG1TCCBL2gAwIBAgIQbFWr29AHksedBwzYEZ7WvzANBgkqhkiG9w0BAQwFADCB\niDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0pl\ncnNleSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNV\nBAMTJVVTRVJUcnVzdCBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMjAw\nMTMwMDAwMDAwWhcNMzAwMTI5MjM1OTU5WjBLMQswCQYDVQQGEwJBVDEQMA4GA1UE\nChMHWmVyb1NTTDEqMCgGA1UEAxMhWmVyb1NTTCBSU0EgRG9tYWluIFNlY3VyZSBT\naXRlIENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAhmlzfqO1Mdgj\n4W3dpBPTVBX1AuvcAyG1fl0dUnw\/MeueCWzRWTheZ35LVo91kLI3DDVaZKW+TBAs\nJBjEbYmMwcWSTWYCg5334SF0+ctDAsFxsX+rTDh9kSrG\/4mp6OShubLaEIUJiZo4\nt873TuSd0Wj5DWt3DtpAG8T35l\/v+xrN8ub8PSSoX5Vkgw+jWf4KQtNvUFLDq8mF\nWhUnPL6jHAADXpvs4lTNYwOtx9yQtbpxwSt7QJY1+ICrmRJB6BuKRt\/jfDJF9Jsc\nRQVlHIxQdKAJl7oaVnXgDkqtk2qddd3kCDXd74gv813G91z7CjsGyJ93oJIlNS3U\ngFbD6V54JMgZ3rSmotYbz98oZxX7MKbtCm1aJ\/q+hTv2YK1yMxrnfcieKmOYBbFD\nhnW5O6RMA703dBK92j6XRN2EttLkQuujZgy+jXRKtaWMIlkNkWJmOiHmErQngHvt\niNkIcjJumq1ddFX4iaTI40a6zgvIBtxFeDs2RfcaH73er7ctNUUqgQT5rFgJhMmF\nx76rQgB5OZUkodb5k2ex7P+Gu4J86bS15094UuYcV09hVeknmTh5Ex9CBKipLS2W\n2wKBakf+aVYnNCU6S0nASqt2xrZpGC1v7v6DhuepyyJtn3qSV2PoBiU5Sql+aARp\nwUibQMGm44gjyNDqDlVp+ShLQlUH9x8CAwEAAaOCAXUwggFxMB8GA1UdIwQYMBaA\nFFN5v1qqK0rPVIDh2JvAnfKyA2bLMB0GA1UdDgQWBBTI2XhootkZaNU9ct5fCj7c\ntYaGpjAOBgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH\/BAgwBgEB\/wIBADAdBgNVHSUE\nFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwIgYDVR0gBBswGTANBgsrBgEEAbIxAQIC\nTjAIBgZngQwBAgEwUAYDVR0fBEkwRzBFoEOgQYY\/aHR0cDovL2NybC51c2VydHJ1\nc3QuY29tL1VTRVJUcnVzdFJTQUNlcnRpZmljYXRpb25BdXRob3JpdHkuY3JsMHYG\nCCsGAQUFBwEBBGowaDA\/BggrBgEFBQcwAoYzaHR0cDovL2NydC51c2VydHJ1c3Qu\nY29tL1VTRVJUcnVzdFJTQUFkZFRydXN0Q0EuY3J0MCUGCCsGAQUFBzABhhlodHRw\nOi8vb2NzcC51c2VydHJ1c3QuY29tMA0GCSqGSIb3DQEBDAUAA4ICAQAVDwoIzQDV\nercT0eYqZjBNJ8VNWwVFlQOtZERqn5iWnEVaLZZdzxlbvz2Fx0ExUNuUEgYkIVM4\nYocKkCQ7hO5noicoq\/DrEYH5IuNcuW1I8JJZ9DLuB1fYvIHlZ2JG46iNbVKA3ygA\nEz86RvDQlt2C494qqPVItRjrz9YlJEGT0DrttyApq0YLFDzf+Z1pkMhh7c+7fXeJ\nqmIhfJpduKc8HEQkYQQShen426S3H0JrIAbKcBCiyYFuOhfyvuwVCFDfFvrjADjd\n4jX1uQXd161IyFRbm89s2Oj5oU1wDYz5sx+hoCuh6lSs+\/uPuWomIq3y1GDFNafW\n+LsHBU16lQo5Q2yh25laQsKRgyPmMpHJ98edm6y2sHUabASmRHxvGiuwwE25aDU0\n2SAeepyImJ2CzB80YG7WxlynHqNhpE7xfC7PzQlLgmfEHdU+tHFeQazRQnrFkW2W\nkqRGIq7cKRnyypvjPMkjeiV9lRdAM9fSJvsB3svUuu1coIG1xxI1yegoGM4r5QP4\nRGIVvYaiI76C0djoSbQ\/dkIUUXQuB8AL5jyH34g3BZaaXyvpmnV4ilppMXVAnAYG\nON51WhJ6W0xNdNJwzYASZYH+tmCWI+N60Gv2NNMGHwMZ7e9bXgzUCZH5FaBFDGR5\nS9VWqHB73Q+OyIVvIbKYcSc2w\/aSuFKGSA==\n-----END CERTIFICATE-----\n-----BEGIN CERTIFICATE-----\nMIIFgTCCBGmgAwIBAgIQOXJEOvkit1HX02wQ3TE1lTANBgkqhkiG9w0BAQwFADB7\nMQswCQYDVQQGEwJHQjEbMBkGA1UECAwSR3JlYXRlciBNYW5jaGVzdGVyMRAwDgYD\nVQQHDAdTYWxmb3JkMRowGAYDVQQKDBFDb21vZG8gQ0EgTGltaXRlZDEhMB8GA1UE\nAwwYQUFBIENlcnRpZmljYXRlIFNlcnZpY2VzMB4XDTE5MDMxMjAwMDAwMFoXDTI4\nMTIzMTIzNTk1OVowgYgxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpOZXcgSmVyc2V5\nMRQwEgYDVQQHEwtKZXJzZXkgQ2l0eTEeMBwGA1UEChMVVGhlIFVTRVJUUlVTVCBO\nZXR3b3JrMS4wLAYDVQQDEyVVU0VSVHJ1c3QgUlNBIENlcnRpZmljYXRpb24gQXV0\naG9yaXR5MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAgBJlFzYOw9sI\ns9CsVw127c0n00ytUINh4qogTQktZAnczomfzD2p7PbPwdzx07HWezcoEStH2jnG\nvDoZtF+mvX2do2NCtnbyqTsrkfjib9DsFiCQCT7i6HTJGLSR1GJk23+jBvGIGGqQ\nIjy8\/hPwhxR79uQfjtTkUcYRZ0YIUcuGFFQ\/vDP+fmyc\/xadGL1RjjWmp2bIcmfb\nIWax1Jt4A8BQOujM8Ny8nkz+rwWWNR9XWrf\/zvk9tyy29lTdyOcSOk2uTIq3XJq0\ntyA9yn8iNK5+O2hmAUTnAU5GU5szYPeUvlM3kHND8zLDU+\/bqv50TmnHa4xgk97E\nxwzf4TKuzJM7UXiVZ4vuPVb+DNBpDxsP8yUmazNt925H+nND5X4OpWaxKXwyhGNV\nicQNwZNUMBkTrNN9N6frXTpsNVzbQdcS2qlJC9\/YgIoJk2KOtWbPJYjNhLixP6Q5\nD9kCnusSTJV882sFqV4Wg8y4Z+LoE53MW4LTTLPtW\/\/e5XOsIzstAL81VXQJSdhJ\nWBp\/kjbmUZIO8yZ9HE0XvMnsQybQv0FfQKlERPSZ51eHnlAfV1SoPv10Yy+xUGUJ\n5lhCLkMaTLTwJUdZ+gQek9QmRkpQgbLevni3\/GcV4clXhB4PY9bpYrrWX1Uu6lzG\nKAgEJTm4Diup8kyXHAc\/DVL17e8vgg8CAwEAAaOB8jCB7zAfBgNVHSMEGDAWgBSg\nEQojPpbxB+zirynvgqV\/0DCktDAdBgNVHQ4EFgQUU3m\/WqorSs9UgOHYm8Cd8rID\nZsswDgYDVR0PAQH\/BAQDAgGGMA8GA1UdEwEB\/wQFMAMBAf8wEQYDVR0gBAowCDAG\nBgRVHSAAMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHA6Ly9jcmwuY29tb2RvY2EuY29t\nL0FBQUNlcnRpZmljYXRlU2VydmljZXMuY3JsMDQGCCsGAQUFBwEBBCgwJjAkBggr\nBgEFBQcwAYYYaHR0cDovL29jc3AuY29tb2RvY2EuY29tMA0GCSqGSIb3DQEBDAUA\nA4IBAQAYh1HcdCE9nIrgJ7cz0C7M7PDmy14R3iJvm3WOnnL+5Nb+qh+cli3vA0p+\nrvSNb3I8QzvAP+u431yqqcau8vzY7qN7Q\/aGNnwU4M309z\/+3ri0ivCRlv79Q2R+\n\/czSAaF9ffgZGclCKxO\/WIu6pKJmBHaIkU4MiRTOok3JMrO66BQavHHxW\/BBC5gA\nCiIDEOUMsfnNkjcZ7Tvx5Dq2+UUTJnWvu6rvP3t3O9LEApE9GQDTF1w52z97GA1F\nzZOFli9d31kWTz9RvdVFGD\/tSo7oBmF0Ixa1DVBzJ0RHfxBdiSprhTEUxOipakyA\nvGp4z7h\/jnZymQyd\/teRCBaho1+V\n-----END CERTIFICATE-----\n"}




    }

}
