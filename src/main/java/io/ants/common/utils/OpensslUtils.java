package io.ants.common.utils;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * openssl 证书公钥私钥读取加密解密工具类，主要用于验证上传的openssl 生成的证书和私钥文件是否正确的问题
 *
 * @author yinliang 2018.12.13 add
 * 主要逻辑:
 * 1、根据私钥文件读取私钥
 * 2、根据公钥文件读取公钥
 * 3、根据证书文件读取公钥
 * 4、根据证书公钥加密字符串
 * 5、根据证书私钥解密字符串
 * 6、如果字符串经过证书公钥加密后，再根据证书私钥解密后能后还原，说明上传的证书和私钥是正确的
 */
public class OpensslUtils {
    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);

    private static final String KEY_ALGORITHM = "RSA";
    /**
     * 默认是RSA/NONE/PKCS1Padding
     */
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * RSA密钥长度必须是64的倍数，在512~65536之间。默认是1024
     */
    private static final int KEY_SIZE = 1024;

    /**
     * RSA最大加密明文大小:明文长度(bytes) <= 密钥长度(bytes)-11
     */
    private static final int MAX_ENCRYPT_BLOCK = KEY_SIZE / 8 - 11;

    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = KEY_SIZE / 8;

    public static PrivateKey getPrivateKeyFromECBigIntAndCurve(BigInteger s, String curveName) {

        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);

        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(s, ecParameterSpec);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Logger logger = LoggerFactory.getLogger(OpensslUtils.class);


    private  static PemObject getPrivateKeyObject(String priKeyStr){
        try{
            PemReader pemReader =new PemReader(new StringReader(priKeyStr));
            return  pemReader.readPemObject();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 利用开源的工具类解析openssl私钥，openssl私钥文件格式为pem，需要去除页眉页脚后才能被java读取
     *
     * @param file
     * @return
     */
    public static PrivateKey getPrivateKey(File file) {
        if (file == null) {
            return null;
        }
        PrivateKey privKey = null;
        PemReader pemReader = null;
        try {
            pemReader = new PemReader(new FileReader(file));
            PemObject pemObject = pemReader.readPemObject();
            byte[] pemContent = pemObject.getContent();
            //支持从PKCS#1或PKCS#8 格式的私钥文件中提取私钥
            if (pemObject.getType().equals("RSA PRIVATE KEY") ) {
                // 取得私钥  for PKCS#1
                RSAPrivateKey asn1PrivKey = RSAPrivateKey.getInstance(pemContent);
                RSAPrivateKeySpec rsaPrivKeySpec = new RSAPrivateKeySpec(asn1PrivKey.getModulus(), asn1PrivKey.getPrivateExponent());
                KeyFactory keyFactory= KeyFactory.getInstance("RSA");
                privKey= keyFactory.generatePrivate(rsaPrivKeySpec);
            } else if( pemObject.getType().equals("EC PRIVATE KEY")){
               //logger.debug("--");
            }else if (pemObject.getType().equals("PRIVATE KEY")) {
                //取得私钥 for PKCS#8
                PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pemContent);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                privKey = kf.generatePrivate(privKeySpec);
            }
        } catch (FileNotFoundException e) {
            logger.error("read private key fail,the reason is the file not exist");
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("read private key fail,the reason is :"+e.getMessage());
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            logger.error("read private key fail,the reason is :"+e.getMessage());
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            logger.error("read private key fail,the reason is :"+e.getMessage());
            e.printStackTrace();
        }  finally {
            try {
                if (pemReader != null) {
                    pemReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return privKey;
    }

    /**
     * 利用java自带的方法读取openssl私钥,openssl私钥文件格式为pem，需要去除页眉页脚后，再进行base64位解码才能被java读取
     * 注意该方法有缺陷,只是简单的根据注释将页眉页脚去掉了,不是很完善,如果页眉页脚前面有空格和注释的情况的会有问题,保留此方法是为方便弄清楚openssl私钥解析原理
     *
     * @param file
     * @return
     */
    public static PrivateKey getPrivateKey1(File file) {
        if (file == null) {
            return null;
        }
        PrivateKey privKey = null;
        try {
            BufferedReader privateKey = new BufferedReader(new FileReader(
                    file));
            String line = "";
            String strPrivateKey = "";
            while ((line = privateKey.readLine()) != null) {
                if (line.contains("--")) {//过滤掉首尾页眉页脚
                    continue;
                }
                strPrivateKey += line;
            }
            privateKey.close();
            ;
            //使用base64位解码
            byte[] privKeyByte = Base64.decodeBase64(strPrivateKey);
            //私钥需要使用pkcs8格式编码
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyByte);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privKey = kf.generatePrivate(privKeySpec);
            return privKey;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privKey;
    }


    /**
     * 从证书文件获取公钥
     *
     * @param file
     * @return
     * @throws CertificateException
     * @throws FileNotFoundException
     */
    public static PublicKey getPublicKeyFromCert(File file) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(file);
            Certificate crt = cf.generateCertificate(in);
            PublicKey publicKey = crt.getPublicKey();
            return publicKey;
        } catch (CertificateException e) {
            logger.error("read public key fail,the reason is :"+e.getMessage());
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            logger.error("read public key fail,the reason is the file not exist");
            e.printStackTrace();

        }
        return null;
    }

    /**
     * 从openssl公钥文件中读取公钥
     * @param file
     * @return
     */
    public static PublicKey getPublicKey(File file) {
        if (file == null) {
            return null;
        }
        PublicKey pubKey = null;
        PemReader pemReader = null;
        try {
            pemReader = new PemReader(new FileReader(file));
            PemObject pemObject = pemReader.readPemObject();
            byte[] pemContent = pemObject.getContent();
            //公钥需要使用x509格式编码
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pemContent);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pubKey = kf.generatePublic(pubKeySpec);
            return pubKey;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pemReader != null) {
                    pemReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return pubKey;
    }



    /**
     * 加密
     *
     * @param key
     * @param plainBytes
     * @return
     */
    public static byte[] encrypt(PublicKey key, byte[] plainBytes) {

        ByteArrayOutputStream out = null;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            int inputLen = plainBytes.length;
            if (inputLen <= MAX_ENCRYPT_BLOCK) {
                return cipher.doFinal(plainBytes);
            }
            out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 数据太长对数据分段加密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(plainBytes, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(plainBytes, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            return out.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            logger.error("rencrypt fail,the reason is : no such decryption algorithm,"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            logger.error("rencrypt fail,the reason is :"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            logger.error("rencrypt fail,the reason is : decrypting the private key is illegal, please check,"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IllegalBlockSizeException e) {
            logger.error("rencrypt fail,the reason is : Illegal ciphertext length, please check,"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (BadPaddingException e) {
            logger.error("rencrypt fail,the reason is : ciphertext data is corrupt, please check,"+e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception e2) {
            }
        }
    }

    /**
     * 根据公钥加密字符串
     *
     * @param key
     * @param plainText 需要加密的字符串
     * @return
     */
    public static String encrypt(PublicKey key, String plainText) {
        byte[] encodeBytes = encrypt(key, plainText.getBytes(DEFAULT_CHARSET));
        return Base64.encodeBase64String(encodeBytes);
    }

    /**
     * 解密
     *
     * @param key
     * @param encodedText
     * @return
     */
    public static String decrypt(PrivateKey key, byte[] encodedText) {

        ByteArrayOutputStream out = null;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            int inputLen = encodedText.length;

            if (inputLen <= MAX_DECRYPT_BLOCK) {
                return new String(cipher.doFinal(encodedText), DEFAULT_CHARSET);
            }

            out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(encodedText, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(encodedText, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }
            return new String(out.toByteArray(), DEFAULT_CHARSET);
        } catch (NoSuchAlgorithmException e) {
            logger.error("rencrypt fail,the reason is : no such decryption algorithm,"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            logger.error("rencrypt fail,the reason is :"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            logger.error("rencrypt fail,the reason is : decrypting the private key is illegal, please check,"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IllegalBlockSizeException e) {
            logger.error("rencrypt fail,the reason is : Illegal ciphertext length, please check,"+e.getMessage());
            e.printStackTrace();
            return null;
        } catch (BadPaddingException e) {
            logger.error("rencrypt fail,the reason is : ciphertext data is corrupt, please check,"+e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception e2) {
            }
        }
    }

    /**
     * 根据私钥解密加密过的字符串
     *
     * @param key
     * @param encodedText 加密过的字符串
     * @return 解密后的字符串
     */
    public static String decrypt(PrivateKey key, String encodedText) {
        byte[] bytes = Base64.decodeBase64(encodedText);
        return decrypt(key, bytes);
    }

    /**
     * 验证证书
     * @param cert
     * @return
     */
    public static String validateCert(File cert){
        if (cert == null) {
            return "证书CRT文件不能为空";
        }
        PublicKey publicKey = getPublicKeyFromCert(cert);
        if (publicKey == null) {
            return "无法读取证书公钥,证书CRT文件格式错误";
        }
        return null;
    }

    /**
     * 验证私钥
     * @param privateKey
     * @return
     */
    public static String validatePrivateKey( File privateKey){
        if (privateKey == null) {
            return "证书私钥不能为空";
        }
        PrivateKey privKey = getPrivateKey(privateKey);
        if (privKey == null) {
            return "无法读取证书私钥,证书私钥文件格式错误";
        }
        return null;
    }

    /**
     * 验证证书私钥是否匹配,如果不匹配返回错误消息
     * @param cert
     * @param privateKey
     * @return 错误消息
     */
    public static String validate(File cert, File privateKey) {
        String res = validateCert(cert);//验证证书
        if((res!=null)&&(res.length()>0)){
            return res;//返回错误消息
        }
        res = validatePrivateKey(privateKey);//验证私钥
        if((res!=null)&&(res.length()>0)){
            return res;//返回错误消息
        }
        PublicKey publicKey = getPublicKeyFromCert(cert);
        PrivateKey privKey = getPrivateKey(privateKey);
        String str = "haha";//测试字符串
        String encryptStr = OpensslUtils.encrypt(publicKey, str);//根据证书公钥对字符串进行加密
        String decryptStr = OpensslUtils.decrypt(privKey, encryptStr);//根据证书私钥对加密字符串进行解密
        if(!str.equals(decryptStr)){
            //字符串根据证书公钥加密，私钥解密后不能还原说明证书与私钥不匹配
            return "----证书与私钥不匹配----";
        }
        return null;
    }

    /**
     * 测试主方法
     *
     * @param args
     */
    public static void main(String[] args) {
        File privateKeyFile = new File("D:\\privatekey.pem");
        //        PrivateKey privKey = OpensslUtils.getPrivateKey(privateKeyFile);
        //        System.out.println("privKey1:" + privKey);
        //        privKey = OpensslUtils.getPrivateKey1(privateKeyFile);
        //        System.out.println("privKey2:" + privKey);
        //        File publicKeyFile = new File("src/main/resources/test.public.key");
        //        PublicKey publicKey = OpensslUtils.getPublicKey(publicKeyFile);
        //        System.out.println("publicKey:" + publicKey);
        File certFile = new File("D:\\fullchain.pem");
        //publicKey = OpensslUtils.getPublicKeyFromCert(certFile);
            //        System.out.println("publicKey2:" + publicKey);
            //        String str = "haha";
            //        String encryptStr = OpensslUtils.encrypt(publicKey, str);
            //        System.out.println("encryptStr:" + encryptStr);
            //        String decryptStr = OpensslUtils.decrypt(privKey, encryptStr);
            //        System.out.println("decryptStr:" + decryptStr);
        String validateResult = validate(certFile,privateKeyFile);
        System.out.println("validateResult:" + validateResult);
    }
}
