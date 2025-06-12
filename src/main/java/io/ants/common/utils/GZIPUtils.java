package io.ants.common.utils;

import java.io.*;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPUtils {
    public static final String GZIP_ENCODE_UTF_8 = "UTF-8";
    public static final String GZIP_ENCODE_ISO_8859_1 = "ISO-8859-1";
    public static final int BUFFER = 2048;

    public static byte[] compress(String str, String encoding) {
        if (str == null || str.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes(encoding));
            gzip.close();
        } catch ( Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    /**
     * 数据压缩
     *
     * @param is
     * @param os
     * @throws Exception
     */
    public static void compress(InputStream is, OutputStream os) {
        try {
            GZIPOutputStream gos = new GZIPOutputStream(os);

            int count;
            byte data[] = new byte[BUFFER];
            while ((count = is.read(data, 0, BUFFER)) != -1) {
                gos.write(data, 0, count);
            }

            gos.finish();

            gos.flush();
            gos.close();
        }catch (Exception e){
            e.printStackTrace();
        }

       return;
    }

    /**
     * 数据压缩
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] compress(byte[] data) {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 压缩
            compress(bais, baos);

            byte[] output = baos.toByteArray();

            baos.flush();
            baos.close();

            bais.close();

            return output;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static byte[] compress(String str)  {
        try{
            return compress(str, GZIP_ENCODE_ISO_8859_1);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static String uncompressToString(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toString(encoding);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String uncompressToString(byte[] bytes) {
        return uncompressToString(bytes, GZIP_ENCODE_ISO_8859_1);
    }

    /**
     * 方法 1：使用 FileWriter 写文件
     * @param filepath 文件目录
     * @param content  待写入内容
     *
     */
    public static void fileWriterMethod(String filepath, String content) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filepath)) {
            fileWriter.append(content);
        }
    }

    public static String compress2Base64(String s){
        byte[] out=compress(s);
        String base64Out= Base64.getEncoder().encodeToString(out);
        return base64Out;
    }


    public static void main(String[] args) throws IOException {
        String s = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        //OutputStream outputStream = new FileOutputStream("C:\\Users\\Administrator\\222.gz");
        //compress(new   ByteArrayInputStream(s.getBytes()),outputStream);
        System.out.println("字符串长度："+s.length());
        String base64Out= compress2Base64(s);
        System.out.println(base64Out);

        OutputStream outputStream = new FileOutputStream("C:\\Users\\Administrator\\333.gz");
        outputStream.write(Base64.getDecoder().decode(base64Out));
        outputStream.flush();
        outputStream.close();


        System.out.println("压缩后：："+compress(s).length);
        System.out.println("解压后："+uncompress(compress(s)).length);
        System.out.println("解压字符串后：："+uncompressToString(compress(s)).length());
    }
}
