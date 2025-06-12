package io.ants.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alipay.api.internal.util.file.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SslUtil {


    private static String PKCS1_H="-----BEGIN RSA PRIVATE KEY-----";
    private static String PKCS1_T="-----END RSA PRIVATE KEY-----";
    private static String PKCS8_H="-----BEGIN PRIVATE KEY-----";
    private static String PKCS8_T="-----END  PRIVATE KEY-----";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * 签验，返回签验是否通过
     * @param certificate   证书
     * @param signStr       签名的字符串
     * @param unSignStr     原字符串
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static boolean verifySign(X509Certificate certificate, String signStr, String unSignStr)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] data = Base64.getDecoder().decode(signStr);
        //System.out.println(certificate.getSigAlgName());
        Signature signature = Signature.getInstance(certificate.getSigAlgName());
        signature.initVerify(certificate.getPublicKey());
        signature.update(unSignStr.getBytes(StandardCharsets.UTF_8));
        return signature.verify(data);
    }


    private static PrivateKey readPrivateKeyFromPem(InputStream privateKeyStream) throws IOException, Exception {
        //  privateKeyStream.readAllBytes();
        byte[] keyBytes =IOUtils.toByteArray(privateKeyStream);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }



    public static boolean verifySignSslV2(String certPem,String priPem){
        try{

            String keystorePath = "D:\\fullchain.pem";
            String keystorePassword = "D:\\privatekey.pem";

            // Load the keystore
            KeyStore keystore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            // Find the first certificate and private key entry in the keystore
            String certificateAlias = null;
            PrivateKey privateKey = null;
            X509Certificate certificate=null;

            java.util.Enumeration<String> aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keystore.isCertificateEntry(alias)) {
                    certificate = (X509Certificate) keystore.getCertificate(alias);
                    privateKey = (PrivateKey) keystore.getKey(alias, null);
                    if (certificate != null && privateKey != null) {
                        certificateAlias = alias;
                        break;
                    }
                }
            }
            // Check if the certificate and private key match
            if (certificateAlias != null && privateKey != null && certificate != null) {
                PublicKey publicKey = certificate.getPublicKey();
                byte[] data = "Test data to sign".getBytes();
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initSign(privateKey);
                signature.update(data);
                byte[] signatureBytes = signature.sign();

                signature.initVerify(publicKey);
                signature.update(data);
                boolean verified = signature.verify(signatureBytes);

                if (verified) {
                    System.out.println("Certificate and private key match.");
                } else {
                    System.out.println("Certificate and private key do not match.");
                }
            } else {
                System.out.println("Certificate and private key not found.");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public   static PemObject getPrivateKeyObject(String priKeyStr){
        try{
            PemReader pemReader =new PemReader(new StringReader(priKeyStr));
            return  pemReader.readPemObject();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static String readFile2String(String filePath)  {
        try{
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));

            return new String(bytes, StandardCharsets.UTF_8);
        }catch (Exception e){
            e.printStackTrace();
        }
       return null;
    }

    public static boolean verifySign(String crtStr,String keyStr){
        try{
            final String str="test-crt-key";
            //String algorithm=  SslUtil.getCertificateByString(crtStr).getSigAlgName();
            if (true){
                try{
                    PrivateKey privateKey = SslUtil.getPrivateKeyByString(keyStr,"RSA");
                    X509Certificate certificate = SslUtil.getCertificateByString(crtStr);
                    String signStr = signString(str, privateKey, certificate);
                    //System.out.println("字符串 ["+str+"] ---签名--->["+signStr+"]");
                    boolean vRsa= verifySign(certificate, signStr, str);
                    if (vRsa){
                        return true;
                    }
                }catch (Exception e){
                    System.out.println("verifySign:"+e.getMessage());
                    //e.printStackTrace();
                }
            }
            if (true){
                try{
                    PrivateKey privateKey = SslUtil.getPrivateKeyByString(keyStr,"EC");
                    X509Certificate certificate = SslUtil.getCertificateByString(crtStr);
                    String signStr = signString(str, privateKey, certificate);
                    //System.out.println("字符串 ["+str+"] ---签名--->["+signStr+"]");
                    boolean vRsa= verifySign(certificate, signStr, str);
                    if (vRsa){
                        return true;
                    }
                }catch (Exception e){
                    //e.printStackTrace();
                    System.out.println("verifySign:"+e.getMessage());
                }
            }
            return false;
        }catch (Exception e){
            //e.printStackTrace();
            System.out.println("verifySign:"+e.getMessage());
        }
        return false;
    }


    public static boolean verifySignV3(String cert, String key) {
        try{
            //    openssl x509 -noout -modulus -in /home/local/fullchain.pem | openssl md5
            //   openssl rsa -noout -modulus -in /home/local/privatekey.pem | openssl md5

            // 读取证书文件
            X509Certificate certificate = SslUtil.getCertificateByString(cert);

            PrivateKey privateKey = SslUtil.getPrivateKeyByString(key,"RSA");

            // 提取证书的公钥
            PublicKey publicKey = certificate.getPublicKey();

            // 创建密钥对并比较公钥和私钥
            if (true){
                try{
                    // 获取证书的公钥
                    byte[] publicKeyEncoded = publicKey.getEncoded();
                    // 使用 KeyFactory 获取公钥对象
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyEncoded);
                    PublicKey parsedPublicKey = keyFactory.generatePublic(publicKeySpec);
                    // 获取证书的模数
                    String modulus = ((java.security.interfaces.RSAPublicKey) parsedPublicKey).getModulus().toString(16);
                    String md5M1=HashUtils.md5ofString(modulus);
                    //System.out.println(modulus);
                    System.out.println(md5M1);

                    byte[] privateKeyEncoded = privateKey.getEncoded();
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
                    keyFactory = KeyFactory.getInstance("RSA");
                    PrivateKey r_privateKey = keyFactory.generatePrivate(keySpec);

                    // 获取私钥的算法和参数
                    String algorithm = r_privateKey.getAlgorithm();
                    java.security.spec.RSAPrivateCrtKeySpec rsaPrivateKeySpec = (java.security.spec.RSAPrivateCrtKeySpec) keyFactory.getKeySpec(r_privateKey, java.security.spec.RSAPrivateCrtKeySpec.class);
                    java.math.BigInteger modulus2 = rsaPrivateKeySpec.getModulus();
                    String modulus2Str=modulus2.toString(16);
                    String md5M2=HashUtils.md5ofString(modulus2Str);
                    //System.out.println("私钥的算法为：" + algorithm);
                    //System.out.println(modulus2Str);
                    System.out.println(md5M2);
                    if (md5M1.equals(md5M2)){
                        return true;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            KeyPair keyPair = new KeyPair(publicKey, privateKey);
            if (keyPair.getPublic().equals(publicKey) && keyPair.getPrivate().equals(privateKey)) {
                //System.out.println("证书和私钥匹配");
                return true;
            } else {
                //System.out.println("证书和私钥不匹配");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

 
    /**
     * 使用私钥对字符串进行签名,返回Base64编码后的字符串
     * @param str   原始字符串
     * @param privateKey    私钥
     * @param certificate   公钥
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static String signString(String str, PrivateKey privateKey, X509Certificate certificate) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //System.out.println(certificate.getSigAlgName());
        Signature signature = Signature.getInstance(certificate.getSigAlgName());
        signature.initSign(privateKey);
        signature.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] data = signature.sign();
        return Base64.getEncoder().encodeToString(data);
    }
 
    /**
     * 读取证书文件
     * @param certFile
     * @return
     * @throws CertificateException
     */
    public static X509Certificate getCertificate(String certFile) throws CertificateException, IOException {
        byte[] cert = readCertFile(certFile);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        InputStream inputStream = new ByteArrayInputStream(cert);
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(inputStream);
        return certificate;
    }



    /**
     * 读取证书 string
     * @param certStr
     * @return
     * @throws CertificateException
     */
    public static X509Certificate getCertificateByString(String certStr) {
        try{
            certStr=getTargetRawString(certStr);
            //        certStr=certStr.replace("-----BEGIN CERTIFICATE-----", "")
            //                .replaceAll("\\n", "")
            //                .replace("-----END CERTIFICATE-----", "");
            //System.out.println("getCertificateByString:");
            //System.out.println(certStr);



            byte[] cert=Base64.getDecoder().decode(certStr);
            //byte[] cert = certStr.getBytes();;
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = new ByteArrayInputStream(cert);
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(inputStream);
            return certificate;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取私钥文件
     * @param pkcs8File
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getPrivateKeyV1(String pkcs8File)              {
        try{
            byte[] prikey = readPkcs8File(pkcs8File);
            if (null==prikey){
                return null;
            }
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(prikey);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            return privateKey;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static PrivateKey getPrivateKey(String pkcs8FilePath){
        try{
            String pccsi=readFile(pkcs8FilePath);
            return getPrivateKeyByString(pccsi,"RSA");
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    private static String getTargetRawString(String str){
        final String pattern = "-----[\\w\\s]*-----";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        while (m.find()){
            String target= m.group();
            str=str.replace(target,"");
        }
        str = str.replaceAll("\\r|\\n", "").trim();
        if (str.contains("=")){
            int i=str.indexOf("=");
            str=str.substring(0,i);
        }
//        if (str.length()%4!=0){
//           StringBuilder sb=new StringBuilder();
//            for (int i = 0; i <str.length()%4 ; i++) {
//                sb.append("=");
//            }
//            str+=sb.toString();
//        }
        return  str;
    }

    /**
     * 读取私钥 string
     * @param pkcs8Str
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getPrivateKeyByString(String pkcs8Str,String algorithm) throws Exception {

        pkcs8Str=getTargetRawString(pkcs8Str);
        //MIIEuwIBADANBgkqhkiG9w0BAQEFAASCBKUwggShAgEAAoIBAQDF9Y4yahQAAVz2bxtDU3JbOqouPDf856uUwHzwwyikDlQfC27V5+alrK/kGd6JhqY53jUn8qaopLZcAASjuUvUNxvj4vLzNyzOMKZqw5akB9GhPvUSRiPgIVaGAcmUMQnymjHbiv8dpZ7Oz55ZUmY35TBFUUbZ0IlfhmSSeZ+GZAegOFT+IlW2YRw75+TbiyevGu90jUf5laD5KC1IdRHkfNWeLNyYMV2C1ndmb8Pu9V1cZCOMj7egWWKinc7Bvoikj5jf1X8lLVrLbRF8j3+FxfdygbPH8Xd4zIGWq3JEHp0T+eS/Q+azpunhwkw41e0z/66dEqSkHGawTi1PV0LNAgMBAAECggEAILFyuAnD3tJaz4o+9uoHp2BzbapdVsPOFwWJPNwGUL3bD7t8JRClC3SPIf1RvuXBeyojHkNZWd3GDkCkg4OdLeGiFgtLs3ZHzI/FVDmUHef6q4YxxEBYOXJYD06pQ3Or4Git1kPI6VUKer5YOQk+P3n29GO9b0ic/207G+PBQFjnc859WD7a5galYJmO6k5jNP/V37Qt6je9c1J2m2Kt9yq7Z+2wegLLTgzBZxeotv1efKB139RNUlWYjSRTjZfKcVMhoq5XRxb1/S3w/0f6xIlCGr8UCn8GrBM9HewR1lMKTdhII33FbKAYxYTBaiBFTeiQbdu8W4DlIkD3G7uDQwKBgQDnzP5xnbEtN7aV0D/z7AmMdedXbDU4PupR5Lb3bG0P/PXrQc1MLszeaizfUdkN+pEwibH0lQTE24k6jn26mlJwNlyO4IytI+jxh6ylDJeBmhItFahA+geqoGNMjFBc+dcaxaMvvsonl08nIx+28Q9SdI9J+U3/mBs4EyjJxXO0CwKBgQDaoCB4ZGAZ/KkYrh7OHGEVjjTo2fLo6xTmfPmve54z4p1P+pe6hQ8s5Yxulg9CSqw1Yp8DZ447fwd1TCwT30BKZ6hj/JCkLi1cN5WPwbklj73mAxW4KaupaXLXR3gA86Tij782//sjWfHPaF/uxXup/MjDAI1OIY7C2cm9WnOThwKBgBTlcfqkEJjW8OrIfztqB+JVrqk+4/1SDqWbTLM5XwuV7kVrBSrc/TL/t1PaeQq9j+EkSJqX5HoqoFBbEBKGXvAmtmla7NOe7Rz48iNe4zmXvhO6ZBSXdaF4G/uGmqgHDdEoB/IB7Q9soIIStIU1bgAs6c3tX46vE6UWdRLbbfbJAoGBAIDAtN6yHnavedZzFOlFhVdwfHB1irzpVVG4YEOchbsAxqyHZDwQiLNkLKQj87CS0YBPKF6U6grX8Mh/p6W8YNxw15aq83P9TfF1OruC10rrsGZ5gp0GzXXkCCIIkP/efUiAZ5g4gOp82g/P5E92NjISERnmhVFyKeVO5aSCVHaZAn8Y5DU8lJtL7Mn7JZab153bog9LVmoBlbjF9/y7fhzM/nijVjfeRprGgQqusOf8lSekVxj9cC3677euuk2vaJaCXu5z3s7tWpaTjEWz424jE5kFTZT7GMNyjS5kd4zlA0W+QGiCpHfUle++Yq5V8FQBhQjA22lXPt/SOkXiRIZo
        //        pkcs8Str=pkcs8Str.replace("-----BEGIN PRIVATE KEY-----", "")
        //                .replaceAll("\\n", "")
        //                .replace("-----END PRIVATE KEY-----", "");
        //System.out.println("getPrivateKeyByString:");
        //System.out.println(pkcs8Str);
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );
        byte[] prikey=Base64.getDecoder().decode(pkcs8Str);
        //byte[] prikey =pkcs8Str.getBytes();
        //System.out.println(new String(prikey));
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(prikey);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        return privateKey;
    }




    public static   X509Certificate getSslX509ByPath(String pemFilePath){
        //String pemFilePath = "/path/to/certificate.pem";
        // Load the PEM certificate file
        File pemFile = new File(pemFilePath);
        try (FileInputStream fis = new FileInputStream(pemFile)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(fis);
            System.out.println("Loaded certificate:");
            System.out.println(certificate);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static   X509Certificate getSslX509ByStr(String str){
        try{
            //String pemFilePath = "/path/to/certificate.pem";
            // Load the PEM certificate file
            // 将字符串编码为二进制数据
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            // 使用 FileInputStream 读取数据
            InputStream fis = new ByteArrayInputStream(bytes);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(fis);
            System.out.println("Loaded certificate:");
            System.out.println(certificate);

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public static PrivateKey getPrivateKeyByStringv2(String pkcs8Str){
        try{
            PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pkcs8Str.getBytes())));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());
            return converter.getPrivateKey(privateKeyInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
         * 读取私钥文件，返回字节数组
         * 去掉开头结尾注释行，保留中间的数据
         * @param pkcs8File
         * @return
         * @throws IOException
         */
    private static byte[] readPkcs8File(String pkcs8File)  {
        StringBuilder stringBuilder = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(pkcs8File));
            String line;
            while((line=br.readLine())!=null){
                if(line.startsWith("-")){
                    continue;
                }
                stringBuilder.append(line);
            }
            br.close();
            return Base64.getDecoder().decode(stringBuilder.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static final byte[] input2byte(InputStream inStream)            throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in2b = swapStream.toByteArray();
        return in2b;
    }


    /**
     * 读取Cert证书文件，返回字节数组
     * 把命令生成的证书文件全部读取到内存
     * @param certFile
     * @return
     * @throws IOException
     */
    private static  byte[] readCertFile(String certFile) throws IOException{
        try(InputStream is = new FileInputStream(certFile)){
            return input2byte(is);
        }
        //        try(InputStream is = new FileInputStream(certFile)){
        //            return is.readAllBytes();
        //        }
    }

    /**
     * 一次性读取全部文件数据
     * @param strFile
     */
    public static String readFile(String strFile){
        try{
            InputStream is = new FileInputStream(strFile);
            int iAvail = is.available();
            byte[] bytes = new byte[iAvail];
            is.read(bytes);
            //logger.info("文件内容:\n" + new String(bytes));
            is.close();
            return  new String(bytes);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

   private static String pkcs8Str = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDE2MJQrE51YK8Z\n" +
            "tPW/6XCnhg0d3eCB/iZ7EsFidu7aVqUZIHOKUFh55LeqQNIkMuDfxaR5xVy7P8EA\n" +
            "T53uIgJkjeKSyAvCQXbXjTMvCuP86KgYH0SCXP7k9YCaEyaq3vkfKPhgDgPqdzwI\n" +
            "/xTnkjke4ZtO8psozCi4Zumcczik5vko840jfKiPBuGylexTVgfSbZNC8FWlVdn4\n" +
            "GxRFU0ax31aun6D/wtkPgcLlX8dOXGYc3S6gp3ZDIW63IEWFGZVAY+zDmQyBv21c\n" +
            "3qwRM6VnnuaagXNPVhTqL17V2zGKFR/xzzcCWPSXHKG2WPa4AarE55Qj2fKyyskN\n" +
            "XuTXOB/DAgMBAAECggEBAL3WGRe6BHRYiKVvCi1JUPaOZenux4w18SByBwQRlOoF\n" +
            "83SXAjZao+jx9VqA5ug4FPxZW/R/rJ18r4HARpuYOtyEpQRPXl+5yIW6S6lLhHGs\n" +
            "YifZR7W9P6jJRnvQilLxdClj1py0B3y//qribWOidendQ21QtOgUdBTaUuMpj3Af\n" +
            "COJt91plihKgR1zGb6kDavjq1qWX2x8tkZkiZwuyR8FUq78Q+gU3FLFaExBp9JTb\n" +
            "GtCjIIw/DUb96dx/puShuN4L/T7IgdJYu6wOB/MHmlp7GUxmordV91/hxPpvmhMl\n" +
            "gguy23IYd8wbbwQaMypwmrUbxrH4ydT1DE6JFDBdhUECgYEA+IqEm4RftpHIiOE/\n" +
            "ZQZx66bOOhWbpkRgz10XY0AcDYqYWEuaguxdYhoeFK9373OsSf0AvQ0fevTbtC8F\n" +
            "dVwQpK1fzQemCP51Es62v4LA7oiq/8zUswj0ywMKvX102nO1sjeGcuRB9EsBDXYM\n" +
            "grB1+GLVJgmBX1CKrlLSgi8iXyMCgYEAysEV4z67t3KSPFSsopQIXZd9kcaz52ul\n" +
            "xnQ6NreH7z8h9+MMJQeTpe8OMLKVCTaz+8EfHPgb+oUQ4x6UoOgds0WUktpIteMC\n" +
            "GhXVszjiZUU4cau2TjsXvfDHS+iVwrug3eRCZOdt89DIdJlOY2VRgOdxZLoneA52\n" +
            "po9IuEbsluECgYBTwDmXPjASkWWF2oFGRbm1suvjXsyoZnpI5hGvfDb3yTA08KUM\n" +
            "YSHGoQ5p4gcERXJVLFBHZFirUj+GpCGuvmtNUq7ppB4rAbPptoaWWjUxLBSTRi6W\n" +
            "dfonPssrt07dHgioGVXQ+WHQNEYShykIEPZv0L4Kp0FVIQraW0ZcYA2xCQKBgQCH\n" +
            "3IiZJLFOxNQe3zJrIrzUUi3PYBGvMd/8smdLwQGynGBbpeW+bmxOlXixwwjBCsni\n" +
            "Gc+Kbur4nO/q8NPxWniEZ0yeduygDMSczCLNnIGAELk42jIoC8rl+RDi2bB4s5eg\n" +
            "+FJRIQ75gN7B2vS+/+Z0dSHzuBW4iiCRjZPR/5yLoQKBgAgpj0yzBWs7uFs5awlt\n" +
            "Tq8H0ybL/iN3ZJ8fCIeXxX24/aAGqsJxbE/KyDn/jV4CPTi2MwivSVJ3sCUH5R9Z\n" +
            "7iAOhb+gU1S+LLnEg1wII7mg6HhDI6uTwEwKOAwpPEouAM+JzAwgHoTKQ5cz/KDl\n" +
            "blnYukyopa5THi0RAb45lqW2\n" +
            "-----END PRIVATE KEY-----\n";
    private static String certStr = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFJzCCBA+gAwIBAgISBEd/MfnlEmgp2B/NY2k3W2UEMA0GCSqGSIb3DQEBCwUA\n" +
            "MDIxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MQswCQYDVQQD\n" +
            "EwJSMzAeFw0yMjA1MjAwMjA1NDJaFw0yMjA4MTgwMjA1NDFaMBwxGjAYBgNVBAMT\n" +
            "EXd3dy53N3NvZnQuY29tLmNuMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
            "AQEAxNjCUKxOdWCvGbT1v+lwp4YNHd3ggf4mexLBYnbu2lalGSBzilBYeeS3qkDS\n" +
            "JDLg38WkecVcuz/BAE+d7iICZI3iksgLwkF2140zLwrj/OioGB9Eglz+5PWAmhMm\n" +
            "qt75Hyj4YA4D6nc8CP8U55I5HuGbTvKbKMwouGbpnHM4pOb5KPONI3yojwbhspXs\n" +
            "U1YH0m2TQvBVpVXZ+BsURVNGsd9Wrp+g/8LZD4HC5V/HTlxmHN0uoKd2QyFutyBF\n" +
            "hRmVQGPsw5kMgb9tXN6sETOlZ57mmoFzT1YU6i9e1dsxihUf8c83Alj0lxyhtlj2\n" +
            "uAGqxOeUI9nyssrJDV7k1zgfwwIDAQABo4ICSzCCAkcwDgYDVR0PAQH/BAQDAgWg\n" +
            "MB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0G\n" +
            "A1UdDgQWBBTCPH3zmQ07SV0zztJBb3e+ICfQZDAfBgNVHSMEGDAWgBQULrMXt1hW\n" +
            "y65QCUDmH6+dixTCxjBVBggrBgEFBQcBAQRJMEcwIQYIKwYBBQUHMAGGFWh0dHA6\n" +
            "Ly9yMy5vLmxlbmNyLm9yZzAiBggrBgEFBQcwAoYWaHR0cDovL3IzLmkubGVuY3Iu\n" +
            "b3JnLzAcBgNVHREEFTATghF3d3cudzdzb2Z0LmNvbS5jbjBMBgNVHSAERTBDMAgG\n" +
            "BmeBDAECATA3BgsrBgEEAYLfEwEBATAoMCYGCCsGAQUFBwIBFhpodHRwOi8vY3Bz\n" +
            "LmxldHNlbmNyeXB0Lm9yZzCCAQMGCisGAQQB1nkCBAIEgfQEgfEA7wB2AEHIyrHf\n" +
            "IkZKEMahOglCh15OMYsbA+vrS8do8JBilgb2AAABgN9tlW0AAAQDAEcwRQIgOpcb\n" +
            "qo8mshcoS45qfweNFDF55bvWPcRItsRrOWd+DQACIQCX3wQzV994b9zPrbvuWeD1\n" +
            "Yvcz90Vmin/aopGZc2LR7QB1ACl5vvCeOTkh8FZzn2Old+W+V32cYAr4+U1dJlwl\n" +
            "XceEAAABgN9tl0EAAAQDAEYwRAIgDCQ2XdI1NeaQ0PqVEtdFF88yML5o8hieQgYf\n" +
            "dCbEBagCICyoBMiuBGDZM7Zn1Tv5o9aEe8hA6xfApHwvnbfkYmZQMA0GCSqGSIb3\n" +
            "DQEBCwUAA4IBAQArgyh+lpIMG+qQ4SmtdCayi0KQNEvmU7vvAjYOs3L77D977nNe\n" +
            "i14tklYWesmrZK4C389UAHjd+iu8rHw83LEVW1FSwLUAIdQXcEZPFEQH41nEeHu8\n" +
            "myYAu06cC3n9jFylT74CavP0yHOt8SAgGgRuiUnaSjqM4ks+Tw338BiIiv+3ejwA\n" +
            "D3K/TGjBUhSdcXk+hTIF0/zgXjf/2IRGtf8iPbj4+JEUGO5ueMdmnSC2mk7v2YVm\n" +
            "0JI5vXv3nZ7EDqb9s3qoNAJULCmo/rPT858uoBOk+2QU5QuKjJbVwEgycY9xFrSZ\n" +
            "px+FBPN2F/e97KCnY+Hpc3zW/W1dXvQdWjIz\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
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
            "-----END CERTIFICATE-----\n";

    private static void test1(){
        try{
            //            String str = " ";
            //            PrivateKey privateKey = SslUtil.getPrivateKeyByString(pkcs8Str);
            //            X509Certificate certificate = SslUtil.getCertificateByString(certStr);
            //            String signStr = SslUtil.signString(str, privateKey, certificate);
            //            System.out.println("字符串 ["+str+"] ---签名--->["+signStr+"]");
            //            boolean result = SslUtil.verifySign(certificate, signStr, str);
            //            System.out.println("签验结果："+result);
            //System.out.println(certStr);
            //System.out.println(pkcs8Str);
            System.out.println(SslUtil.verifySignV3(certStr,pkcs8Str));
            //String crt=readFile("C:\\Users\\Administrator\\Desktop\\1.crt");
            //String key=readFile("C:\\Users\\Administrator\\Desktop\\1.key");
            //System.out.println(crt);
            //System.out.println(key);
            //System.out.println(SslUtil.verifySign(crt,key));
            //System.out.println(SslUtil.certStr);
            //            String regFileName = "-----[\\W\\D\\S]*-----";
            //            // 匹配当前正则表达式
            //            Matcher matcher = Pattern.compile(regFileName).matcher(SslUtil.certStr);
            //            if (matcher.find()) {
            //                // 将匹配当前正则表达式的字符串即文件名称进行赋值
            //                System.out.println(matcher.group());
            //            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void test2(){
        try{
            // 获取证书和证书 PRIVATE KEY
            // certificateFile = new File("D:\\fullchain.pem");
            //File privateKeyFile = new File("D:\\privatekey.pem");

            String cert=readFile("D:\\fullchain.pem");
            String key=readFile("D:\\privatekey.pem");

            PrivateKey privateKey = SslUtil.getPrivateKeyByString(key,"RSA");
            X509Certificate certificate = SslUtil.getCertificateByString(cert);
            PublicKey publicKey=certificate.getPublicKey();
            //pub_key=30 8E 01 0A 02 C0 01 01 00  pri_key=30 8E 04 D9 02 01 00 02 C0 01 01
            //pub_key=30 8E 04 D9 02 01 00        pri_key=30 8E 04 D9 02 01
            String data = "abcd1234";
            byte[] sign = SignatureUtils.sign(data.getBytes(),SignatureUtils.SHA256withRSA,privateKey);
            System.out.println("sign:" + Base64.getEncoder().encodeToString(sign));
            System.out.println("verify:" + SignatureUtils.verify(data.getBytes(),sign,SignatureUtils.SHA256withRSA,publicKey));

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void test3(){
        String cert="-----BEGIN CERTIFICATE-----\n" +
                "MIIGdDCCBFygAwIBAgIQB6rhLFzf2rp3HAJnrexCHTANBgkqhkiG9w0BAQwFADBL\n" +
                "MQswCQYDVQQGEwJBVDEQMA4GA1UEChMHWmVyb1NTTDEqMCgGA1UEAxMhWmVyb1NT\n" +
                "TCBSU0EgRG9tYWluIFNlY3VyZSBTaXRlIENBMB4XDTIzMTIxMzAwMDAwMFoXDTI0\n" +
                "MDMxMjIzNTk1OVowHjEcMBoGA1UEAxMTYzQuY2RudGVzdC45MWh1LnRvcDCCASIw\n" +
                "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL8pIZvF5Jn23gbxYpwUG7/iZKuv\n" +
                "jicvgy8pRCOCGn54eM7/v33Rpe5B1mJzrJZGsgh5SlZ/DnSt2ibQ0CBjF4NfsbF6\n" +
                "0H/H/kkyoDRNPx91VM5Baez/hbxz6HSoSCjppsSs8XcpiFOgMC+P9zyFM6vWTlVk\n" +
                "l3wveX77ZdjzJCkKWeGNXBbGhU6qEdDV+NKBDkhWYwUvXPdjKyAglyE0oDRl82AX\n" +
                "Hv9nZrCToxMfCtYyBP2TSMtfkjR2ZSAoSdSy3izWF2zpm/229vS9cQZXK9zgingp\n" +
                "9nrQedDg+XYz1O//pqgPzVf5Kud2A5x++AcVDnGGlFlZgVbrqRfME44ss0ECAwEA\n" +
                "AaOCAn8wggJ7MB8GA1UdIwQYMBaAFMjZeGii2Rlo1T1y3l8KPty1hoamMB0GA1Ud\n" +
                "DgQWBBRfCjMui/HctFTysf9NrlnTOs+omDAOBgNVHQ8BAf8EBAMCBaAwDAYDVR0T\n" +
                "AQH/BAIwADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwSQYDVR0gBEIw\n" +
                "QDA0BgsrBgEEAbIxAQICTjAlMCMGCCsGAQUFBwIBFhdodHRwczovL3NlY3RpZ28u\n" +
                "Y29tL0NQUzAIBgZngQwBAgEwgYgGCCsGAQUFBwEBBHwwejBLBggrBgEFBQcwAoY/\n" +
                "aHR0cDovL3plcm9zc2wuY3J0LnNlY3RpZ28uY29tL1plcm9TU0xSU0FEb21haW5T\n" +
                "ZWN1cmVTaXRlQ0EuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vemVyb3NzbC5vY3Nw\n" +
                "LnNlY3RpZ28uY29tMIIBBAYKKwYBBAHWeQIEAgSB9QSB8gDwAHYAdv+IPwq2+5VR\n" +
                "wmHM9Ye6NLSkzbsp3GhCCp/mZ0xaOnQAAAGMYMa9YwAABAMARzBFAiB6PV0rs6cG\n" +
                "R6VtF+GSZD6ToLgONjUsaObDFIaHowBO1gIhALk9YY+wjfYwDPnT4ulSmc5EzvBT\n" +
                "2tPacgVpGE0qfRkCAHYAO1N3dT4tuYBOizBbBv5AO2fYT8P0x70ADS1yb+H61BcA\n" +
                "AAGMYMa+MwAABAMARzBFAiEAsnY7CKfenh44qIf0lbY7U8QoO3Jmc8OKzB5GglQO\n" +
                "/xMCIDrb/9/qB35YC5vwg32hPK9L+Swmj6/mf3mmci5xM2eqMB4GA1UdEQQXMBWC\n" +
                "E2M0LmNkbnRlc3QuOTFodS50b3AwDQYJKoZIhvcNAQEMBQADggIBAGeFenlnQa/X\n" +
                "abC1xQPwiupffV9gwGynKPTxhGirAVDKDRiLUNd5cUaZvCF+W1TjAjCoM1ljHY/O\n" +
                "5kZr4KGvzEU7BJvjODzORDUzTENMEHip+5vdG9t2j6UDAyOJrjp2cranu7/k3Rw4\n" +
                "ufPSoLgVTHBVjn/y2U2cUFvYqjtN/cHnu6ST/a3m3i3g0+AU2q7exxLf9sugh7tw\n" +
                "/8WCA1Z9eOcxL4qzRUv1Ecrduj4gtg7Pi0a+G7Pnm9RoHu0qRUEkM9YbEsup1aOv\n" +
                "L+E522E6aToAvtlnOfcYs+UBFg2tjofDfiNB6cwG6M0xiXuDyZWUWg5Z8Cpw4IsT\n" +
                "wwlPl89k1MJHuzn9k4V3wtIbDcoDQc/0b6fJyKPj5oTHAxXbj5YrZca87xF5Ei0A\n" +
                "LeKGW66AT9qAAnPTXpiTnuSGRD50aR93l37397NQge6fzcNNkPIa7cZV/Lfeiuxo\n" +
                "AKOq/G+tezRJ63oa5K/a3WzExwck7h0cw+qOgwXcCOlt11LNdVsGH7Mu3Cw1kYlQ\n" +
                "FIJRGtb4fL33deFxyOQsYV+frP38pRPdXqoJicJe21NDPX/wUq1420PwwqxukUpa\n" +
                "EML8C2Q0zU+jY+NCF/+JMSAHhTSaCbTXMQ4Wrjd3RQSHhvMlPjTpfaJu1xtcPZEm\n" +
                "BR170djm844vCnc1jXTyZit89+i4zreu\n" +
                "-----END CERTIFICATE-----\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIG1TCCBL2gAwIBAgIQbFWr29AHksedBwzYEZ7WvzANBgkqhkiG9w0BAQwFADCB\n" +
                "iDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0pl\n" +
                "cnNleSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNV\n" +
                "BAMTJVVTRVJUcnVzdCBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMjAw\n" +
                "MTMwMDAwMDAwWhcNMzAwMTI5MjM1OTU5WjBLMQswCQYDVQQGEwJBVDEQMA4GA1UE\n" +
                "ChMHWmVyb1NTTDEqMCgGA1UEAxMhWmVyb1NTTCBSU0EgRG9tYWluIFNlY3VyZSBT\n" +
                "aXRlIENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAhmlzfqO1Mdgj\n" +
                "4W3dpBPTVBX1AuvcAyG1fl0dUnw/MeueCWzRWTheZ35LVo91kLI3DDVaZKW+TBAs\n" +
                "JBjEbYmMwcWSTWYCg5334SF0+ctDAsFxsX+rTDh9kSrG/4mp6OShubLaEIUJiZo4\n" +
                "t873TuSd0Wj5DWt3DtpAG8T35l/v+xrN8ub8PSSoX5Vkgw+jWf4KQtNvUFLDq8mF\n" +
                "WhUnPL6jHAADXpvs4lTNYwOtx9yQtbpxwSt7QJY1+ICrmRJB6BuKRt/jfDJF9Jsc\n" +
                "RQVlHIxQdKAJl7oaVnXgDkqtk2qddd3kCDXd74gv813G91z7CjsGyJ93oJIlNS3U\n" +
                "gFbD6V54JMgZ3rSmotYbz98oZxX7MKbtCm1aJ/q+hTv2YK1yMxrnfcieKmOYBbFD\n" +
                "hnW5O6RMA703dBK92j6XRN2EttLkQuujZgy+jXRKtaWMIlkNkWJmOiHmErQngHvt\n" +
                "iNkIcjJumq1ddFX4iaTI40a6zgvIBtxFeDs2RfcaH73er7ctNUUqgQT5rFgJhMmF\n" +
                "x76rQgB5OZUkodb5k2ex7P+Gu4J86bS15094UuYcV09hVeknmTh5Ex9CBKipLS2W\n" +
                "2wKBakf+aVYnNCU6S0nASqt2xrZpGC1v7v6DhuepyyJtn3qSV2PoBiU5Sql+aARp\n" +
                "wUibQMGm44gjyNDqDlVp+ShLQlUH9x8CAwEAAaOCAXUwggFxMB8GA1UdIwQYMBaA\n" +
                "FFN5v1qqK0rPVIDh2JvAnfKyA2bLMB0GA1UdDgQWBBTI2XhootkZaNU9ct5fCj7c\n" +
                "tYaGpjAOBgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUE\n" +
                "FjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwIgYDVR0gBBswGTANBgsrBgEEAbIxAQIC\n" +
                "TjAIBgZngQwBAgEwUAYDVR0fBEkwRzBFoEOgQYY/aHR0cDovL2NybC51c2VydHJ1\n" +
                "c3QuY29tL1VTRVJUcnVzdFJTQUNlcnRpZmljYXRpb25BdXRob3JpdHkuY3JsMHYG\n" +
                "CCsGAQUFBwEBBGowaDA/BggrBgEFBQcwAoYzaHR0cDovL2NydC51c2VydHJ1c3Qu\n" +
                "Y29tL1VTRVJUcnVzdFJTQUFkZFRydXN0Q0EuY3J0MCUGCCsGAQUFBzABhhlodHRw\n" +
                "Oi8vb2NzcC51c2VydHJ1c3QuY29tMA0GCSqGSIb3DQEBDAUAA4ICAQAVDwoIzQDV\n" +
                "ercT0eYqZjBNJ8VNWwVFlQOtZERqn5iWnEVaLZZdzxlbvz2Fx0ExUNuUEgYkIVM4\n" +
                "YocKkCQ7hO5noicoq/DrEYH5IuNcuW1I8JJZ9DLuB1fYvIHlZ2JG46iNbVKA3ygA\n" +
                "Ez86RvDQlt2C494qqPVItRjrz9YlJEGT0DrttyApq0YLFDzf+Z1pkMhh7c+7fXeJ\n" +
                "qmIhfJpduKc8HEQkYQQShen426S3H0JrIAbKcBCiyYFuOhfyvuwVCFDfFvrjADjd\n" +
                "4jX1uQXd161IyFRbm89s2Oj5oU1wDYz5sx+hoCuh6lSs+/uPuWomIq3y1GDFNafW\n" +
                "+LsHBU16lQo5Q2yh25laQsKRgyPmMpHJ98edm6y2sHUabASmRHxvGiuwwE25aDU0\n" +
                "2SAeepyImJ2CzB80YG7WxlynHqNhpE7xfC7PzQlLgmfEHdU+tHFeQazRQnrFkW2W\n" +
                "kqRGIq7cKRnyypvjPMkjeiV9lRdAM9fSJvsB3svUuu1coIG1xxI1yegoGM4r5QP4\n" +
                "RGIVvYaiI76C0djoSbQ/dkIUUXQuB8AL5jyH34g3BZaaXyvpmnV4ilppMXVAnAYG\n" +
                "ON51WhJ6W0xNdNJwzYASZYH+tmCWI+N60Gv2NNMGHwMZ7e9bXgzUCZH5FaBFDGR5\n" +
                "S9VWqHB73Q+OyIVvIbKYcSc2w/aSuFKGSA==\n" +
                "-----END CERTIFICATE-----\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIFgTCCBGmgAwIBAgIQOXJEOvkit1HX02wQ3TE1lTANBgkqhkiG9w0BAQwFADB7\n" +
                "MQswCQYDVQQGEwJHQjEbMBkGA1UECAwSR3JlYXRlciBNYW5jaGVzdGVyMRAwDgYD\n" +
                "VQQHDAdTYWxmb3JkMRowGAYDVQQKDBFDb21vZG8gQ0EgTGltaXRlZDEhMB8GA1UE\n" +
                "AwwYQUFBIENlcnRpZmljYXRlIFNlcnZpY2VzMB4XDTE5MDMxMjAwMDAwMFoXDTI4\n" +
                "MTIzMTIzNTk1OVowgYgxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpOZXcgSmVyc2V5\n" +
                "MRQwEgYDVQQHEwtKZXJzZXkgQ2l0eTEeMBwGA1UEChMVVGhlIFVTRVJUUlVTVCBO\n" +
                "ZXR3b3JrMS4wLAYDVQQDEyVVU0VSVHJ1c3QgUlNBIENlcnRpZmljYXRpb24gQXV0\n" +
                "aG9yaXR5MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAgBJlFzYOw9sI\n" +
                "s9CsVw127c0n00ytUINh4qogTQktZAnczomfzD2p7PbPwdzx07HWezcoEStH2jnG\n" +
                "vDoZtF+mvX2do2NCtnbyqTsrkfjib9DsFiCQCT7i6HTJGLSR1GJk23+jBvGIGGqQ\n" +
                "Ijy8/hPwhxR79uQfjtTkUcYRZ0YIUcuGFFQ/vDP+fmyc/xadGL1RjjWmp2bIcmfb\n" +
                "IWax1Jt4A8BQOujM8Ny8nkz+rwWWNR9XWrf/zvk9tyy29lTdyOcSOk2uTIq3XJq0\n" +
                "tyA9yn8iNK5+O2hmAUTnAU5GU5szYPeUvlM3kHND8zLDU+/bqv50TmnHa4xgk97E\n" +
                "xwzf4TKuzJM7UXiVZ4vuPVb+DNBpDxsP8yUmazNt925H+nND5X4OpWaxKXwyhGNV\n" +
                "icQNwZNUMBkTrNN9N6frXTpsNVzbQdcS2qlJC9/YgIoJk2KOtWbPJYjNhLixP6Q5\n" +
                "D9kCnusSTJV882sFqV4Wg8y4Z+LoE53MW4LTTLPtW//e5XOsIzstAL81VXQJSdhJ\n" +
                "WBp/kjbmUZIO8yZ9HE0XvMnsQybQv0FfQKlERPSZ51eHnlAfV1SoPv10Yy+xUGUJ\n" +
                "5lhCLkMaTLTwJUdZ+gQek9QmRkpQgbLevni3/GcV4clXhB4PY9bpYrrWX1Uu6lzG\n" +
                "KAgEJTm4Diup8kyXHAc/DVL17e8vgg8CAwEAAaOB8jCB7zAfBgNVHSMEGDAWgBSg\n" +
                "EQojPpbxB+zirynvgqV/0DCktDAdBgNVHQ4EFgQUU3m/WqorSs9UgOHYm8Cd8rID\n" +
                "ZsswDgYDVR0PAQH/BAQDAgGGMA8GA1UdEwEB/wQFMAMBAf8wEQYDVR0gBAowCDAG\n" +
                "BgRVHSAAMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHA6Ly9jcmwuY29tb2RvY2EuY29t\n" +
                "L0FBQUNlcnRpZmljYXRlU2VydmljZXMuY3JsMDQGCCsGAQUFBwEBBCgwJjAkBggr\n" +
                "BgEFBQcwAYYYaHR0cDovL29jc3AuY29tb2RvY2EuY29tMA0GCSqGSIb3DQEBDAUA\n" +
                "A4IBAQAYh1HcdCE9nIrgJ7cz0C7M7PDmy14R3iJvm3WOnnL+5Nb+qh+cli3vA0p+\n" +
                "rvSNb3I8QzvAP+u431yqqcau8vzY7qN7Q/aGNnwU4M309z/+3ri0ivCRlv79Q2R+\n" +
                "/czSAaF9ffgZGclCKxO/WIu6pKJmBHaIkU4MiRTOok3JMrO66BQavHHxW/BBC5gA\n" +
                "CiIDEOUMsfnNkjcZ7Tvx5Dq2+UUTJnWvu6rvP3t3O9LEApE9GQDTF1w52z97GA1F\n" +
                "zZOFli9d31kWTz9RvdVFGD/tSo7oBmF0Ixa1DVBzJ0RHfxBdiSprhTEUxOipakyA\n" +
                "vGp4z7h/jnZymQyd/teRCBaho1+V\n" +
                "-----END CERTIFICATE-----";
        System.out.println(HashUtils.getCertEndTime(cert));
        System.out.println(SslUtil.getCertificateByString(cert));
    }




    private static void test4(){
        try{
            // 读取证书文件
            FileInputStream certFile = new FileInputStream("D:\\fullchain.pem");
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certFile);


            // 读取私钥文件
            FileInputStream keyFile = new FileInputStream("D:\\psk.pem");
            byte[] privateKeyBytes = new byte[keyFile.available()];
            keyFile.read(privateKeyBytes);
            keyFile.close();

            // 解析私钥并转换为公钥
            String privateKeyContent = new String(privateKeyBytes);
            String privateKeyPEM = getTargetRawString(privateKeyContent);
            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // 提取证书的公钥
            PublicKey publicKey = certificate.getPublicKey();

            // 创建密钥对并比较公钥和私钥
            KeyPair keyPair = new KeyPair(publicKey, privateKey);
            if (keyPair.getPublic().equals(publicKey) && keyPair.getPrivate().equals(privateKey)) {
                System.out.println("证书和私钥匹配");
            } else {
                System.out.println("证书和私钥不匹配");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void test5(){
        // System.out.println(System.getProperty("java.version"));
        //test1();
        //test3();
        //test4();
        //String cert=readFile("D:\\fullchain.pem");
        //String key=readFile("D:\\privatekey.pem");
        // SslUtil.verifySignSslV2(cert, key);
        //System.out.println(getSslX509ByStr(cert));
        //boolean b= SslUtil.verifySignV3(cert,key);
        //System.out.println(b);
        //test2();
        String s="[\"aaa\",\"afasdf\",\"asdfasdf\",{\"aa\":\"\"}]";
        JSONArray jsonArray=DataTypeConversionUtil.string2JsonArray(s);
        for (int i = 0; i < jsonArray.size(); i++) {
            Object o=  jsonArray.get(i);
            System.out.println(o.getClass().getSimpleName());
            System.out.println(o);

        }
    }

    private static void test6(){
        String domain = "asdf.sub.aaa.com";
        String f="*.aaa.com,www.aaa.com,sub.aaa.com";
        for (String patternString:f.split(",")){
            // 将 * 替换为 .*，将 . 替换为 \.
            if (patternString.contains("*")){
                patternString = patternString.replace(".", "\\.").replace("*", ".*");
                System.out.println(patternString);
                Pattern pattern = Pattern.compile(patternString);
                Matcher matcher = pattern.matcher(domain);

                if (matcher.matches()) {
                    System.out.println("域名匹配成功");
                } else {
                    System.out.println("域名匹配失败");
                }
            }else{
                if (patternString.equals(domain)){
                    System.out.println("域名匹配成功");
                }
            }

        }


    }


    private static void test7(){
       final String key ="-----BEGIN PRIVATE KEY-----\n" +
               "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgvvcMvKS/cZzExkq0\n" +
               "bV4QLqD0hQCGEMbnDhCOaMFDcRKhRANCAAQX42DnHKarTo1jSKQUBasaHvGh5Ky0\n" +
               "PCc7zYqJFhgCCo1ey4lTRYnj5eu9kaDlRuGHkhcmxTljumhG8udOJZaV\n" +
               "-----END PRIVATE KEY-----\n";
       final  String cert="-----BEGIN CERTIFICATE-----\n" +
               "MIIDdzCCAv2gAwIBAgISBHY4K4U4JUUSw7H+dP4As/jqMAoGCCqGSM49BAMDMDIx\n" +
               "CzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MQswCQYDVQQDEwJF\n" +
               "NjAeFw0yNDExMTIwNzM3MjJaFw0yNTAyMTAwNzM3MjFaMBYxFDASBgNVBAMMCyou\n" +
               "ZGNmaGhjLmNuMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEF+Ng5xymq06NY0ik\n" +
               "FAWrGh7xoeSstDwnO82KiRYYAgqNXsuJU0WJ4+XrvZGg5Ubhh5IXJsU5Y7poRvLn\n" +
               "TiWWlaOCAg0wggIJMA4GA1UdDwEB/wQEAwIHgDAdBgNVHSUEFjAUBggrBgEFBQcD\n" +
               "AQYIKwYBBQUHAwIwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUxtnXKRs0Xpd8GGOc\n" +
               "mzOqwh5CywgwHwYDVR0jBBgwFoAUkydGmAOpUWiOmNbEQkjbI79YlNIwVQYIKwYB\n" +
               "BQUHAQEESTBHMCEGCCsGAQUFBzABhhVodHRwOi8vZTYuby5sZW5jci5vcmcwIgYI\n" +
               "KwYBBQUHMAKGFmh0dHA6Ly9lNi5pLmxlbmNyLm9yZy8wFgYDVR0RBA8wDYILKi5k\n" +
               "Y2ZoaGMuY24wEwYDVR0gBAwwCjAIBgZngQwBAgEwggEEBgorBgEEAdZ5AgQCBIH1\n" +
               "BIHyAPAAdgCi4wrkRe+9rZt+OO1HZ3dT14JbhJTXK14bLMS5UKRH5wAAAZMfg9Bo\n" +
               "AAAEAwBHMEUCIQCXJ/9jjmW3za91TDuWuev/iBWeBZZ6eih4sPDGWVrb6QIgPWtw\n" +
               "NOoXNEfA8Jloaoow5cpLc2Q1i5bJvpC1M8bhPDMAdgDPEVbu1S58r/OHW9lpLpvp\n" +
               "GnFnSrAX7KwB0lt3zsw7CAAAAZMfg9CKAAAEAwBHMEUCIDTzU6bPXQYjAKHJLuKX\n" +
               "Hrv/W6iLTYoE/MzqrD3a7v0TAiEAhnmDOEzS/vS60nYHi4I0FvD808ksoQAfscn/\n" +
               "VJIfxrcwCgYIKoZIzj0EAwMDaAAwZQIxAJRo/ddcDPHhwbjGspB4vMXs+DbE2Nr2\n" +
               "FpNhwQxLsiVS4rEDobx3eUPdKADDAqnM/gIwJRD+uMLqzejN7CD4SI5hXyRPxobS\n" +
               "D4A7KtSikhPLiOnlA1R+2SkOAh4hQ++1/B/d\n" +
               "-----END CERTIFICATE-----\n" +
               "\n" +
               "-----BEGIN CERTIFICATE-----\n" +
               "MIIEVzCCAj+gAwIBAgIRALBXPpFzlydw27SHyzpFKzgwDQYJKoZIhvcNAQELBQAw\n" +
               "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
               "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMjQwMzEzMDAwMDAw\n" +
               "WhcNMjcwMzEyMjM1OTU5WjAyMQswCQYDVQQGEwJVUzEWMBQGA1UEChMNTGV0J3Mg\n" +
               "RW5jcnlwdDELMAkGA1UEAxMCRTYwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAATZ8Z5G\n" +
               "h/ghcWCoJuuj+rnq2h25EqfUJtlRFLFhfHWWvyILOR/VvtEKRqotPEoJhC6+QJVV\n" +
               "6RlAN2Z17TJOdwRJ+HB7wxjnzvdxEP6sdNgA1O1tHHMWMxCcOrLqbGL0vbijgfgw\n" +
               "gfUwDgYDVR0PAQH/BAQDAgGGMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcD\n" +
               "ATASBgNVHRMBAf8ECDAGAQH/AgEAMB0GA1UdDgQWBBSTJ0aYA6lRaI6Y1sRCSNsj\n" +
               "v1iU0jAfBgNVHSMEGDAWgBR5tFnme7bl5AFzgAiIyBpY9umbbjAyBggrBgEFBQcB\n" +
               "AQQmMCQwIgYIKwYBBQUHMAKGFmh0dHA6Ly94MS5pLmxlbmNyLm9yZy8wEwYDVR0g\n" +
               "BAwwCjAIBgZngQwBAgEwJwYDVR0fBCAwHjAcoBqgGIYWaHR0cDovL3gxLmMubGVu\n" +
               "Y3Iub3JnLzANBgkqhkiG9w0BAQsFAAOCAgEAfYt7SiA1sgWGCIpunk46r4AExIRc\n" +
               "MxkKgUhNlrrv1B21hOaXN/5miE+LOTbrcmU/M9yvC6MVY730GNFoL8IhJ8j8vrOL\n" +
               "pMY22OP6baS1k9YMrtDTlwJHoGby04ThTUeBDksS9RiuHvicZqBedQdIF65pZuhp\n" +
               "eDcGBcLiYasQr/EO5gxxtLyTmgsHSOVSBcFOn9lgv7LECPq9i7mfH3mpxgrRKSxH\n" +
               "pOoZ0KXMcB+hHuvlklHntvcI0mMMQ0mhYj6qtMFStkF1RpCG3IPdIwpVCQqu8GV7\n" +
               "s8ubknRzs+3C/Bm19RFOoiPpDkwvyNfvmQ14XkyqqKK5oZ8zhD32kFRQkxa8uZSu\n" +
               "h4aTImFxknu39waBxIRXE4jKxlAmQc4QjFZoq1KmQqQg0J/1JF8RlFvJas1VcjLv\n" +
               "YlvUB2t6npO6oQjB3l+PNf0DpQH7iUx3Wz5AjQCi6L25FjyE06q6BZ/QlmtYdl/8\n" +
               "ZYao4SRqPEs/6cAiF+Qf5zg2UkaWtDphl1LKMuTNLotvsX99HP69V2faNyegodQ0\n" +
               "LyTApr/vT01YPE46vNsDLgK+4cL6TrzC/a4WcmF5SRJ938zrv/duJHLXQIku5v0+\n" +
               "EwOy59Hdm0PT/Er/84dDV0CSjdR/2XuZM3kpysSKLgD1cKiDA+IRguODCxfO9cyY\n" +
               "Ig46v9mFmBvyH04=\n" +
               "-----END CERTIFICATE-----\n";

        X509Certificate x=  SslUtil.getCertificateByString(cert);
        //System.out.println(x);
        System.out.println(x.getSigAlgName());
        System.out.println(verifySign(cert,key));

    }

    public static void test8(){
        for (Provider provider : Security.getProviders()) {
            System.out.println("Provider: " + provider.getName());
            for (Provider.Service service : provider.getServices()) {
                if ("Signature".equals(service.getType())) {
                    System.out.println("  Algorithm: " + service.getAlgorithm());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //test8();
        test7();
    }


}