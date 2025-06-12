package io.ants.common.utils;

import io.ants.common.exception.RRException;
import io.ants.modules.sys.vo.CertSubjectVo;
import io.ants.modules.sys.vo.TbCertifyObjVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * @author Administrator
 */
public class HashUtils {

    public static final Logger logger = LoggerFactory.getLogger(HashUtils.class);

    public   static String md5OfFile(File file)  {
        try{
            if(!file.isFile()){return  null;}
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fs = new FileInputStream(file);
            if(null==fs){return null;}
            BufferedInputStream bs = new BufferedInputStream(fs);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte bite : digest) {
                sb.append(String.format("%02x", bite & 0xff));
            }
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static  String md5ofString(String data)   {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString().toLowerCase();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static  String bs64OfString(String data){
        try{
            String base64encodedString = Base64.getEncoder().encodeToString(data.getBytes("utf-8"));
            return base64encodedString;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static String ReadCerSubjectDN(String certStr){
        String pattern = "-----[\\W\\D\\S]*-----";
        Pattern r = Pattern.compile(pattern);
        Matcher pem_m = r.matcher(certStr);
        if (!pem_m.matches()){
            logger.error(certStr);
            throw new RRException("证书pem 格式有误！");
        }
        try{
            InputStream is = new ByteArrayInputStream(certStr.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;
            x509Cert.checkValidity();
            return x509Cert.getSubjectDN().toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }




    public static Map readCerSubjectToMap(String certStr){
        try{
            Map resultMap=new HashMap();
            InputStream is = new ByteArrayInputStream(certStr.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;

            // JAVA程序中显示证书指定信息
            //System.out.println("输出证书信息:"+c.toString());
            //System.out.println("版本号:"+x509Cert.getVersion());
            //System.out.println("序列号:"+x509Cert.getSerialNumber().toString(16));
            //System.out.println("主体名："+x509Cert.getSubjectDN());
            //System.out.println("签发者："+x509Cert.getIssuerDN());
            //System.out.println("有效期："+x509Cert.getNotBefore());
            //System.out.println("签名算法："+x509Cert.getSigAlgName());
            //byte [] sig=x509Cert.getSignature();//签名值
            //System.out.println("签名值："+ Arrays.toString(sig));
            //PublicKey pk=x509Cert.getPublicKey();
            //byte [] pkenc=pk.getEncoded();
            //System.out.println("公钥");
            //for (byte b : pkenc)
            //System.out.print(b + ",");
            resultMap.put("infos",c.toString());
            resultMap.put("version",x509Cert.getVersion());
            resultMap.put("SerialNumber",x509Cert.getSerialNumber().toString(16));
            resultMap.put("SubjectDN",x509Cert.getSubjectDN());
            resultMap.put("IssuerDN",x509Cert.getIssuerDN());
            resultMap.put("NotBefore",x509Cert.getNotBefore());
            resultMap.put("NotAfter",x509Cert.getNotAfter());
            resultMap.put("SigAlgName",x509Cert.getSigAlgName());
            byte [] sig=x509Cert.getSignature();//签名值
            resultMap.put("sign",Arrays.toString(sig));
            PublicKey pk=x509Cert.getPublicKey();
            byte [] pkenc=pk.getEncoded();
            resultMap.put("publicKey",Arrays.toString(pkenc));
            try{
                List<String> ls=new ArrayList<>();
                x509Cert.getSubjectAlternativeNames().forEach(item->{
                    ls.add(item.get(1).toString());
                });
                resultMap.put("subjectAlternativeNames",String.join(",",ls));
            }catch (Exception e){

            }
            return resultMap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static CertSubjectVo readCerSubjectToVo(String certStr){
        CertSubjectVo certSubjectVo = new CertSubjectVo();
        try{

            InputStream is = new ByteArrayInputStream(certStr.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;

            certSubjectVo.setInfos(c.toString());
            certSubjectVo.setVersion(x509Cert.getVersion());
            certSubjectVo.setSerialNumber(x509Cert.getSerialNumber().toString(16));
            certSubjectVo.setSubjectDN( x509Cert.getSubjectDN().getName());
            certSubjectVo.setIssuerDN(x509Cert.getIssuerDN());
            certSubjectVo.setNotAfter(x509Cert.getNotAfter());
            certSubjectVo.setNotBefore(x509Cert.getNotBefore());
            certSubjectVo.setSigAlgName(x509Cert.getSigAlgName());

            byte [] sig=x509Cert.getSignature();//签名值
            certSubjectVo.setSign(Arrays.toString(sig));
            PublicKey pk=x509Cert.getPublicKey();
            byte [] pkenc=pk.getEncoded();
            certSubjectVo.setPublicKey(Arrays.toString(pkenc));
            try{
                List<String> ls=new ArrayList<>();
                x509Cert.getSubjectAlternativeNames().forEach(item->{
                    ls.add(item.get(1).toString());
                });
                certSubjectVo.setSubjectAlternativeNames(String.join(",",ls));
            }catch (Exception e){
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return certSubjectVo;
    }


    public static void updateCertVoInfo(TbCertifyObjVo certifyObjVo){
        try{
            if (StringUtils.isBlank(certifyObjVo.getPem_cert())){
                return;
            }
            InputStream is = new ByteArrayInputStream(certifyObjVo.getPem_cert().getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;
            certifyObjVo.setNotAfter(x509Cert.getNotAfter().getTime());
            certifyObjVo.setNotBefore(x509Cert.getNotBefore().getTime());
            certifyObjVo.setVersion(x509Cert.getVersion());
            try{
               List<String> ls=new ArrayList<>();
                x509Cert.getSubjectAlternativeNames().forEach(item->{
                    ls.add(item.get(1).toString());
                });
                certifyObjVo.setSubjectAlternativeNames(String.join(",",ls));
            }catch (Exception e){

            }


        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static long getCertEndTime(String certStr){
        try{
            Map map=smallReadCerSubjectToMap(certStr);
            if (map.containsKey("NotAfter")){
                //Wed Jun 07 07:59:59 CST 2023
                Date aDate= (Date)map.get("NotAfter");
                return aDate.getTime();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0l;
    }

    public static Map smallReadCerSubjectToMap(String certStr){
        try{
            Map resultMap=new HashMap();
            InputStream is = new ByteArrayInputStream(certStr.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;

            // JAVA程序中显示证书指定信息
            //System.out.println("输出证书信息:"+c.toString());
            //System.out.println("版本号:"+x509Cert.getVersion());
            //System.out.println("序列号:"+x509Cert.getSerialNumber().toString(16));
            //System.out.println("主体名："+x509Cert.getSubjectDN());
            //System.out.println("签发者："+x509Cert.getIssuerDN());
            //System.out.println("有效期："+x509Cert.getNotBefore());
            //System.out.println("签名算法："+x509Cert.getSigAlgName());
            //byte [] sig=x509Cert.getSignature();//签名值
            //System.out.println("签名值："+ Arrays.toString(sig));
            //PublicKey pk=x509Cert.getPublicKey();
            //byte [] pkenc=pk.getEncoded();
            //System.out.println("公钥");
            //for (byte b : pkenc)
            //System.out.print(b + ",");
           // resultMap.put("infos",c.toString());
            //resultMap.put("version",x509Cert.getVersion());
            //resultMap.put("SerialNumber",x509Cert.getSerialNumber().toString(16));
            //resultMap.put("SubjectDN",x509Cert.getSubjectDN());
            //resultMap.put("IssuerDN",x509Cert.getIssuerDN());
            resultMap.put("NotBefore",x509Cert.getNotBefore());
            resultMap.put("NotAfter",x509Cert.getNotAfter());
            //resultMap.put("SigAlgName",x509Cert.getSigAlgName());
            //byte [] sig=x509Cert.getSignature();//签名值
            //resultMap.put("sign",Arrays.toString(sig));
            //PublicKey pk=x509Cert.getPublicKey();
            //byte [] pkenc=pk.getEncoded();
            //resultMap.put("publicKey",Arrays.toString(pkenc));
            return resultMap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public static boolean isValidCert(String pem){
        try{
            if (StringUtils.isBlank(pem)){
                return false;
            }
            InputStream is = new ByteArrayInputStream(pem.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;
            Date start=  x509Cert.getNotBefore();
            Date end=x509Cert.getNotAfter();
            Date now=new Date();
            if (now.after(start) &&  now.before(end) ){
                return  true;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private static X509Certificate getX509(String pemStr){
        try{
            InputStream is = new ByteArrayInputStream(pemStr.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate c = cf.generateCertificate(is);
            X509Certificate x509Cert = (X509Certificate) c;
            return x509Cert;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isCanApplyCert(String pem){
        try{
            if (StringUtils.isBlank(pem)){
                return false;
            }
            X509Certificate x509Cert=getX509(pem);
            Date end=x509Cert.getNotAfter();
            Date now=new Date();
            Date lastStartDate=DateUtils.addDateDays(end,-15);
            if (now.after(lastStartDate)){
                return true;
            }

        }catch (Exception e){
            logger.error("isCanApplyCert error:"+e.getMessage());
            //e.printStackTrace();
        }
        return false;
    }


    private static void  test_1(){
        String cert="-----BEGIN CERTIFICATE-----\\n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\\u003d\\u003d\\n-----END CERTIFICATE-----\\n-----BEGIN CERTIFICATE-----\\nMIIG1TCCBL2gAwIBAgIQbFWr29AHksedBwzYEZ7WvzANBgkqhkiG9w0BAQwFADCB\\niDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0pl\\ncnNleSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNV\\nBAMTJVVTRVJUcnVzdCBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMjAw\\nMTMwMDAwMDAwWhcNMzAwMTI5MjM1OTU5WjBLMQswCQYDVQQGEwJBVDEQMA4GA1UE\\nChMHWmVyb1NTTDEqMCgGA1UEAxMhWmVyb1NTTCBSU0EgRG9tYWluIFNlY3VyZSBT\\naXRlIENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAhmlzfqO1Mdgj\\n4W3dpBPTVBX1AuvcAyG1fl0dUnw/MeueCWzRWTheZ35LVo91kLI3DDVaZKW+TBAs\\nJBjEbYmMwcWSTWYCg5334SF0+ctDAsFxsX+rTDh9kSrG/4mp6OShubLaEIUJiZo4\\nt873TuSd0Wj5DWt3DtpAG8T35l/v+xrN8ub8PSSoX5Vkgw+jWf4KQtNvUFLDq8mF\\nWhUnPL6jHAADXpvs4lTNYwOtx9yQtbpxwSt7QJY1+ICrmRJB6BuKRt/jfDJF9Jsc\\nRQVlHIxQdKAJl7oaVnXgDkqtk2qddd3kCDXd74gv813G91z7CjsGyJ93oJIlNS3U\\ngFbD6V54JMgZ3rSmotYbz98oZxX7MKbtCm1aJ/q+hTv2YK1yMxrnfcieKmOYBbFD\\nhnW5O6RMA703dBK92j6XRN2EttLkQuujZgy+jXRKtaWMIlkNkWJmOiHmErQngHvt\\niNkIcjJumq1ddFX4iaTI40a6zgvIBtxFeDs2RfcaH73er7ctNUUqgQT5rFgJhMmF\\nx76rQgB5OZUkodb5k2ex7P+Gu4J86bS15094UuYcV09hVeknmTh5Ex9CBKipLS2W\\n2wKBakf+aVYnNCU6S0nASqt2xrZpGC1v7v6DhuepyyJtn3qSV2PoBiU5Sql+aARp\\nwUibQMGm44gjyNDqDlVp+ShLQlUH9x8CAwEAAaOCAXUwggFxMB8GA1UdIwQYMBaA\\nFFN5v1qqK0rPVIDh2JvAnfKyA2bLMB0GA1UdDgQWBBTI2XhootkZaNU9ct5fCj7c\\ntYaGpjAOBgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUE\\nFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwIgYDVR0gBBswGTANBgsrBgEEAbIxAQIC\\nTjAIBgZngQwBAgEwUAYDVR0fBEkwRzBFoEOgQYY/aHR0cDovL2NybC51c2VydHJ1\\nc3QuY29tL1VTRVJUcnVzdFJTQUNlcnRpZmljYXRpb25BdXRob3JpdHkuY3JsMHYG\\nCCsGAQUFBwEBBGowaDA/BggrBgEFBQcwAoYzaHR0cDovL2NydC51c2VydHJ1c3Qu\\nY29tL1VTRVJUcnVzdFJTQUFkZFRydXN0Q0EuY3J0MCUGCCsGAQUFBzABhhlodHRw\\nOi8vb2NzcC51c2VydHJ1c3QuY29tMA0GCSqGSIb3DQEBDAUAA4ICAQAVDwoIzQDV\\nercT0eYqZjBNJ8VNWwVFlQOtZERqn5iWnEVaLZZdzxlbvz2Fx0ExUNuUEgYkIVM4\\nYocKkCQ7hO5noicoq/DrEYH5IuNcuW1I8JJZ9DLuB1fYvIHlZ2JG46iNbVKA3ygA\\nEz86RvDQlt2C494qqPVItRjrz9YlJEGT0DrttyApq0YLFDzf+Z1pkMhh7c+7fXeJ\\nqmIhfJpduKc8HEQkYQQShen426S3H0JrIAbKcBCiyYFuOhfyvuwVCFDfFvrjADjd\\n4jX1uQXd161IyFRbm89s2Oj5oU1wDYz5sx+hoCuh6lSs+/uPuWomIq3y1GDFNafW\\n+LsHBU16lQo5Q2yh25laQsKRgyPmMpHJ98edm6y2sHUabASmRHxvGiuwwE25aDU0\\n2SAeepyImJ2CzB80YG7WxlynHqNhpE7xfC7PzQlLgmfEHdU+tHFeQazRQnrFkW2W\\nkqRGIq7cKRnyypvjPMkjeiV9lRdAM9fSJvsB3svUuu1coIG1xxI1yegoGM4r5QP4\\nRGIVvYaiI76C0djoSbQ/dkIUUXQuB8AL5jyH34g3BZaaXyvpmnV4ilppMXVAnAYG\\nON51WhJ6W0xNdNJwzYASZYH+tmCWI+N60Gv2NNMGHwMZ7e9bXgzUCZH5FaBFDGR5\\nS9VWqHB73Q+OyIVvIbKYcSc2w/aSuFKGSA\\u003d\\u003d\\n-----END CERTIFICATE-----\\n-----BEGIN CERTIFICATE-----\\nMIIFgTCCBGmgAwIBAgIQOXJEOvkit1HX02wQ3TE1lTANBgkqhkiG9w0BAQwFADB7\\nMQswCQYDVQQGEwJHQjEbMBkGA1UECAwSR3JlYXRlciBNYW5jaGVzdGVyMRAwDgYD\\nVQQHDAdTYWxmb3JkMRowGAYDVQQKDBFDb21vZG8gQ0EgTGltaXRlZDEhMB8GA1UE\\nAwwYQUFBIENlcnRpZmljYXRlIFNlcnZpY2VzMB4XDTE5MDMxMjAwMDAwMFoXDTI4\\nMTIzMTIzNTk1OVowgYgxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpOZXcgSmVyc2V5\\nMRQwEgYDVQQHEwtKZXJzZXkgQ2l0eTEeMBwGA1UEChMVVGhlIFVTRVJUUlVTVCBO\\nZXR3b3JrMS4wLAYDVQQDEyVVU0VSVHJ1c3QgUlNBIENlcnRpZmljYXRpb24gQXV0\\naG9yaXR5MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAgBJlFzYOw9sI\\ns9CsVw127c0n00ytUINh4qogTQktZAnczomfzD2p7PbPwdzx07HWezcoEStH2jnG\\nvDoZtF+mvX2do2NCtnbyqTsrkfjib9DsFiCQCT7i6HTJGLSR1GJk23+jBvGIGGqQ\\nIjy8/hPwhxR79uQfjtTkUcYRZ0YIUcuGFFQ/vDP+fmyc/xadGL1RjjWmp2bIcmfb\\nIWax1Jt4A8BQOujM8Ny8nkz+rwWWNR9XWrf/zvk9tyy29lTdyOcSOk2uTIq3XJq0\\ntyA9yn8iNK5+O2hmAUTnAU5GU5szYPeUvlM3kHND8zLDU+/bqv50TmnHa4xgk97E\\nxwzf4TKuzJM7UXiVZ4vuPVb+DNBpDxsP8yUmazNt925H+nND5X4OpWaxKXwyhGNV\\nicQNwZNUMBkTrNN9N6frXTpsNVzbQdcS2qlJC9/YgIoJk2KOtWbPJYjNhLixP6Q5\\nD9kCnusSTJV882sFqV4Wg8y4Z+LoE53MW4LTTLPtW//e5XOsIzstAL81VXQJSdhJ\\nWBp/kjbmUZIO8yZ9HE0XvMnsQybQv0FfQKlERPSZ51eHnlAfV1SoPv10Yy+xUGUJ\\n5lhCLkMaTLTwJUdZ+gQek9QmRkpQgbLevni3/GcV4clXhB4PY9bpYrrWX1Uu6lzG\\nKAgEJTm4Diup8kyXHAc/DVL17e8vgg8CAwEAAaOB8jCB7zAfBgNVHSMEGDAWgBSg\\nEQojPpbxB+zirynvgqV/0DCktDAdBgNVHQ4EFgQUU3m/WqorSs9UgOHYm8Cd8rID\\nZsswDgYDVR0PAQH/BAQDAgGGMA8GA1UdEwEB/wQFMAMBAf8wEQYDVR0gBAowCDAG\\nBgRVHSAAMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHA6Ly9jcmwuY29tb2RvY2EuY29t\\nL0FBQUNlcnRpZmljYXRlU2VydmljZXMuY3JsMDQGCCsGAQUFBwEBBCgwJjAkBggr\\nBgEFBQcwAYYYaHR0cDovL29jc3AuY29tb2RvY2EuY29tMA0GCSqGSIb3DQEBDAUA\\nA4IBAQAYh1HcdCE9nIrgJ7cz0C7M7PDmy14R3iJvm3WOnnL+5Nb+qh+cli3vA0p+\\nrvSNb3I8QzvAP+u431yqqcau8vzY7qN7Q/aGNnwU4M309z/+3ri0ivCRlv79Q2R+\\n/czSAaF9ffgZGclCKxO/WIu6pKJmBHaIkU4MiRTOok3JMrO66BQavHHxW/BBC5gA\\nCiIDEOUMsfnNkjcZ7Tvx5Dq2+UUTJnWvu6rvP3t3O9LEApE9GQDTF1w52z97GA1F\\nzZOFli9d31kWTz9RvdVFGD/tSo7oBmF0Ixa1DVBzJ0RHfxBdiSprhTEUxOipakyA\\nvGp4z7h/jnZymQyd/teRCBaho1+V\\n-----END CERTIFICATE-----";
        cert="-----BEGIN CERTIFICATE-----\n" +
                "MIIFCDCCA/CgAwIBAgISBHF+PDbRtMECOTIXIGviGReuMA0GCSqGSIb3DQEBCwUA\n" +
                "MDIxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MQswCQYDVQQD\n" +
                "EwJSMzAeFw0yNDA0MDQxMzU4NDVaFw0yNDA3MDMxMzU4NDRaMBkxFzAVBgNVBAMT\n" +
                "Dnd3dy5obGdnb3MuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
                "t1WYAfwBbzRnStCnGunyWbgUU4fL7G4Ou0RFdZTAY0q/iBx32nRLSzxpn9hFMmJ/\n" +
                "Q1X4yLlNu0wSExYslYZ7W5u6BbGuRglIQwEzEJHq9p3hrX6V3Tm1HDKgMtTc/CCg\n" +
                "dCn+M+gjHgVFKOV74bJ8CMWk59Aid2UZkSmEoyWsG/myzViZ29XHYjDMfszBCV17\n" +
                "/XlO1jxxEPij/azK8zr5y5z7z6KZxL2bsg6fCOQjrqXXsjUlt0yYzBk5EqoQoCas\n" +
                "6466BUOs6d354BTjmKz/RMEwh6k2ys8LEUdR/3E0HffYZsiydAydRaXp9kwU6+w8\n" +
                "coyGqPemOENv283p+hAhbwIDAQABo4ICLzCCAiswDgYDVR0PAQH/BAQDAgWgMB0G\n" +
                "A1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1Ud\n" +
                "DgQWBBSTVkNCtFjw6nYkonqychtMxh8/8TAfBgNVHSMEGDAWgBQULrMXt1hWy65Q\n" +
                "CUDmH6+dixTCxjBVBggrBgEFBQcBAQRJMEcwIQYIKwYBBQUHMAGGFWh0dHA6Ly9y\n" +
                "My5vLmxlbmNyLm9yZzAiBggrBgEFBQcwAoYWaHR0cDovL3IzLmkubGVuY3Iub3Jn\n" +
                "LzA5BgNVHREEMjAwgg53d3cuaGxnZ29hLmNvbYIOd3d3LmhsZ2dvcy5jb22CDnd3\n" +
                "dy5obGdnb3QuY29tMBMGA1UdIAQMMAowCAYGZ4EMAQIBMIIBAwYKKwYBBAHWeQIE\n" +
                "AgSB9ASB8QDvAHUAdv+IPwq2+5VRwmHM9Ye6NLSkzbsp3GhCCp/mZ0xaOnQAAAGO\n" +
                "qZ6T9gAABAMARjBEAiBqnI3cGcurEyzeV1tvLCgEBvkqbN/T7GTBDLIK7AmOFgIg\n" +
                "NLEnvEc43tLMDA8nQFzZ82U0RK3vlpGWFC6wG2f2WBYAdgA7U3d1Pi25gE6LMFsG\n" +
                "/kA7Z9hPw/THvQANLXJv4frUFwAAAY6pnpuLAAAEAwBHMEUCIFYIz2FCadrevkJX\n" +
                "ccwLpfeTV+3Y2K7wPND0Y/hDDswOAiEAmkELtYN3402FTdkJQH2jYSTtH5zTOE8I\n" +
                "qtb1tXKN6AAwDQYJKoZIhvcNAQELBQADggEBADuVjUL2FFmmPj56IffZyCMSAiS5\n" +
                "3cXpQY0Z1vqWyJeSOksekLXh3hSPgBrLhodKbT8+nRCcufhrtPECw+rAWAt7UkYf\n" +
                "wo33hFVvXeu/K+ZTws7qrnPRuh/tJC136Xf37/UukmZLK94w4dHWO/sfeaSmMMQZ\n" +
                "briPlUw0lyNZJshVb4EWmiQjM5rTpFb0jiLiikWuPbvNio5RyGgvEGu8IKjgGOts\n" +
                "4QsKMGLnhYD2j93853v8h/FY+6QlKeNXtLOYcFEsaDmiArvLTpo8sCOkjTTA9Upl\n" +
                "7tHOd0F1A7eDgH6tcjkHX1w5rj3eSviNrw69Mhu27LngqL77hfhTEMYMo8o=\n" +
                "-----END CERTIFICATE-----\n" +
                "\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIFFjCCAv6gAwIBAgIRAJErCErPDBinU/bWLiWnX1owDQYJKoZIhvcNAQELBQAw\n" +
                "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
                "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMjAwOTA0MDAwMDAw\n" +
                "WhcNMjUwOTE1MTYwMDAwWjAyMQswCQYDVQQGEwJVUzEWMBQGA1UEChMNTGV0J3Mg\n" +
                "RW5jcnlwdDELMAkGA1UEAxMCUjMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n" +
                "AoIBAQC7AhUozPaglNMPEuyNVZLD+ILxmaZ6QoinXSaqtSu5xUyxr45r+XXIo9cP\n" +
                "R5QUVTVXjJ6oojkZ9YI8QqlObvU7wy7bjcCwXPNZOOftz2nwWgsbvsCUJCWH+jdx\n" +
                "sxPnHKzhm+/b5DtFUkWWqcFTzjTIUu61ru2P3mBw4qVUq7ZtDpelQDRrK9O8Zutm\n" +
                "NHz6a4uPVymZ+DAXXbpyb/uBxa3Shlg9F8fnCbvxK/eG3MHacV3URuPMrSXBiLxg\n" +
                "Z3Vms/EY96Jc5lP/Ooi2R6X/ExjqmAl3P51T+c8B5fWmcBcUr2Ok/5mzk53cU6cG\n" +
                "/kiFHaFpriV1uxPMUgP17VGhi9sVAgMBAAGjggEIMIIBBDAOBgNVHQ8BAf8EBAMC\n" +
                "AYYwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMBIGA1UdEwEB/wQIMAYB\n" +
                "Af8CAQAwHQYDVR0OBBYEFBQusxe3WFbLrlAJQOYfr52LFMLGMB8GA1UdIwQYMBaA\n" +
                "FHm0WeZ7tuXkAXOACIjIGlj26ZtuMDIGCCsGAQUFBwEBBCYwJDAiBggrBgEFBQcw\n" +
                "AoYWaHR0cDovL3gxLmkubGVuY3Iub3JnLzAnBgNVHR8EIDAeMBygGqAYhhZodHRw\n" +
                "Oi8veDEuYy5sZW5jci5vcmcvMCIGA1UdIAQbMBkwCAYGZ4EMAQIBMA0GCysGAQQB\n" +
                "gt8TAQEBMA0GCSqGSIb3DQEBCwUAA4ICAQCFyk5HPqP3hUSFvNVneLKYY611TR6W\n" +
                "PTNlclQtgaDqw+34IL9fzLdwALduO/ZelN7kIJ+m74uyA+eitRY8kc607TkC53wl\n" +
                "ikfmZW4/RvTZ8M6UK+5UzhK8jCdLuMGYL6KvzXGRSgi3yLgjewQtCPkIVz6D2QQz\n" +
                "CkcheAmCJ8MqyJu5zlzyZMjAvnnAT45tRAxekrsu94sQ4egdRCnbWSDtY7kh+BIm\n" +
                "lJNXoB1lBMEKIq4QDUOXoRgffuDghje1WrG9ML+Hbisq/yFOGwXD9RiX8F6sw6W4\n" +
                "avAuvDszue5L3sz85K+EC4Y/wFVDNvZo4TYXao6Z0f+lQKc0t8DQYzk1OXVu8rp2\n" +
                "yJMC6alLbBfODALZvYH7n7do1AZls4I9d1P4jnkDrQoxB3UqQ9hVl3LEKQ73xF1O\n" +
                "yK5GhDDX8oVfGKF5u+decIsH4YaTw7mP3GFxJSqv3+0lUFJoi5Lc5da149p90Ids\n" +
                "hCExroL1+7mryIkXPeFM5TgO9r0rvZaBFOvV2z0gp35Z0+L4WPlbuEjN/lxPFin+\n" +
                "HlUjr8gRsI3qfJOQFy/9rKIJR0Y/8Omwt/8oTWgy1mdeHmmjk7j1nYsvC9JSQ6Zv\n" +
                "MldlTTKB3zhThV1+XWYp6rjd5JW1zbVWEkLNxE7GJThEUG3szgBVGP7pSWTUTsqX\n" +
                "nLRbwHOoq7hHwg==\n" +
                "-----END CERTIFICATE-----";
        //x509Cert.getIssuerDN()
        X509Certificate x=getX509(cert);
        //System.out.println();
        try {
            System.out.println(x.getSubjectAlternativeNames().toString());
            System.out.println(DataTypeConversionUtil.entity2jonsStr(readCerSubjectToVo(cert) ) );
        }catch (Exception e){

        }

        return;
        //        String ssl_host=ReadCerSubjectDN(cert);
        //        Integer i=ssl_host.indexOf("=");
        //        if (-1!=i){
        //            i++;
        //        }
        //        ssl_host=ssl_host.substring(i);
        //        System.out.println(ssl_host);
        //        String main_server="www.0z0.cc";
        //        String pattern=ssl_host.replace(".","\\.");
        //        pattern=pattern.replace("*",".*");
        //        System.out.println(pattern);
        //        Pattern r = Pattern.compile(pattern);
        //        Matcher m = r.matcher(main_server);
        //        System.out.println(m);
        //        if(m.matches()){
        //            System.out.println("xx");
        //        }
    }



    /**
     * 字符串CRC32校验
     * @param value
     * @return
     */
    public static String getCRC32(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes());
        return String.valueOf(crc32.getValue());
    }


    public static void main(String[] args) {
        test_1();
       // String eee="3123_7edd32c78e";
       // Integer ii=eee.indexOf("_");
       // System.out.println(getCRC32(eee));
        //        String v1="1.22";
        //        String v2="1.23";
        //        System.out.println(v1.compareToIgnoreCase(v2)<0);

    }
}
