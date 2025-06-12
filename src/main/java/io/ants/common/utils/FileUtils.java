package io.ants.common.utils;

import cn.hutool.core.text.StrBuilder;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Administrator
 */
public class FileUtils {

    public static   String readTxt2JsonString(String path)  {
        BufferedReader br=null;
        FileInputStream fis = null;
        try{
            File file = new File(path);
            StringBuilder jsonStr = new StringBuilder();
            InputStreamReader isr;

            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            assert fis != null;
            isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine())!= null){
                jsonStr.append(line);
            }
            //System.out.println(jsonStr);
            isr.close();
            fis.close();
            br.close();
            return jsonStr.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static   String readTxt(String path)  {
        BufferedReader br=null;
        FileInputStream fis = null;
        try{
            File file = new File(path);
            StringBuilder outStr = new StringBuilder();
            InputStreamReader isr;

            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            assert fis != null;
            isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine())!= null){
                outStr.append(line);
            }

            isr.close();
            fis.close();
            br.close();
            return outStr.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] readFileToByteArray(String filePath){
        // 读取文件内容并转换为字节数组
        byte[] fileBytes={};
        try {
            Path path = java.nio.file.Paths.get(filePath);
            fileBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            System.out.println("无法读取文件：" + e.getMessage());
        }
        return fileBytes;
    }

    public static String getStringByPath(String path){
        String result="";
        try{
            File file = new File(path);
            if (file.exists()) {
                InputStream is = new FileInputStream(path);
                result=new String(SslUtil.input2byte(is));
                is.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static void mkdir( String directoryPath){
        //String directoryPath = "/path/to/directory";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (result) {
                System.out.println("Directory created successfully");
            } else {
                System.out.println("Failed to create directory");
                }
             }
        else {
            //System.out.println("[mkdir]["+directoryPath+"]Directory already exists");

        }
    }

    public static R createFileAndParentDir( String filePath){
        String eMsg="";
        //String filePath = "/path/to/directory/newfile.txt";
        File file = new File(filePath);
        // 获取父目录的 File 对象
        File parentDir = file.getParentFile();
        // 创建父目录
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                //System.out.println("Failed to create parent directory.");
                return R.ok();
            }
        }
        // 创建文件
        try {
            boolean created = file.createNewFile();
            if (created) {
                //System.out.println("File created successfully.");
                return R.ok();
            } else {
                eMsg="File creation failed:"+filePath;
            }
        } catch (IOException e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    private void test1(){
        // 获取当前类的ClassLoader
        try {
            Resource resource = new ClassPathResource("ngx_conf_module\\nginx.conf");
            InputStream is=resource.getInputStream();
            // 使用 BufferedReader 逐行读取输入流内容并输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 关闭输入流
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readFileByline(String filePath){
        List<String> list=new ArrayList<>();
        try{
            //String filePath = "path_to_your_file"; // 替换为你的文件路径
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //System.out.println(line); // 对每一行进行处理，这里简单地打印出来
                    list.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }
    public static void  addModeX(String filePath){
        //String filePath = "path/to/your/file";

        // 创建文件路径对象
        Path path = java.nio.file.Paths.get(filePath);
        // 判断文件是否具有执行权限
        boolean isExecutable = Files.isExecutable(path);
        //System.out.println("文件是否具有执行权限：" + isExecutable);
        // 添加可执行权限
        if (!isExecutable) {
            try {
                Set<PosixFilePermission> permissions = new HashSet<>();
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);

                Files.setPosixFilePermissions(path, permissions);
                //System.out.println("已添加可执行权限");
            } catch (IOException e) {
                System.out.println("添加可执行权限失败：" + e.getMessage());
            }
        }
    }


    public static R deleteFile(String filePath) {
        // 创建 File 对象
        File file = new File(filePath);

        // 检查文件是否存在
        if (file.exists()) {
            // 删除文件
            boolean deleted = file.delete();
            if (deleted) {
               return R.ok();
            } else {
                return R.error(filePath+"文件删除失败！");
            }
        }else{
            return R.error(filePath+"：该路径的文件或目录不存在！");
        }
    }

    public static R deleteDirectory(String directoryPath) {
        String eMsg="";
        try{
            File directory = new File(directoryPath);
            // 如果是目录并且不为空
            if (directory.isDirectory() && directory.list().length > 0) {
                // 获取该目录下所有文件和子目录
                File[] files = directory.listFiles();


                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file.getAbsolutePath());
                    } else {
                        file.delete();
                    }
                }
            }
            // 删除该目录
            return deleteFile(directoryPath);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);

    }

    public static R fileWrite(String path, String content) {
        String eMsg="";
        createFileAndParentDir(path);
        // 写入文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println(content);
            return R.ok();
        } catch (IOException e) {
            //System.err.println("文件写入失败: " + e.getMessage());
            eMsg="文件写入失败: " + e.getMessage();
        }
        return R.error(eMsg);

    }

    private  static String getRdsPwdByPath(String RedisDir){
        String pwd="";
        try{
            //Map<String, String> patternMap=new HashMap();
            //patternMap.put("requirepass" ,"^requirepass\\s*[^\\s]*$");
            final  String  pattern="^requirepass\\s*[^\\s]*$";
            String redisConfPath=String.format("%s/redis.conf",RedisDir);
            //按行读取配置
            //logger.debug("[redis_conf]=>"+redisConfPath);
            FileInputStream inputStream = new FileInputStream(redisConfPath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String str = null;
            while((str = bufferedReader.readLine()) != null)
            {
                //System.out.println(str);
                if (StringUtils.isNotBlank(str)){
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(str);
                    if(m.matches()){
                        //logger.debug(str);
                        str=str.replace("requirepass","");
                        str=str.trim();
                        if (str.charAt(0)=='"' && str.charAt(str.length()-1)=='"'){
                            str=str.substring(1,str.length()-1);
                        }
                        pwd=str;
                    }

                }
            }
            //close
            inputStream.close();
            bufferedReader.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return pwd;
    }


    public static String getRedisPassWord(){
        Map<String, String> patternMap=new HashMap();
        patternMap.put("requirepass" ,"^requirepass\\s*[^\\s]*$");

        //获取redis的目录
        // pid=$( ps -ef | grep 'redis' | grep -v grep | awk '{print $2}' ) && ls -l /proc/$pid/cwd
        //pid=$( ps -ef | grep 'redis' | grep -v grep | awk '{print $2}' ) && ls -l /proc/$pid/cwd |grep -v grep |awk '{print $11}'
        //lrwxrwxrwx 1 root root 0 Sep 14 14:08 /proc/18781/cwd -> /usr/ants/redis-6.2.6

        try{
            String RedisDir2="/usr/ants/redis-6.2.6";
            String pwd2=getRdsPwdByPath(RedisDir2);
            if (StringUtils.isNotBlank(pwd2)){
                return pwd2;
            }else{
                String redis_dir_cmd="pid=$( ps -ef | grep 'redis' | grep -v grep | awk '{print $2}' ) && ls -l /proc/$pid/cwd";
                List<String> resultLS=ShellUtils.runShell(redis_dir_cmd,false) ;
                String RedisDir1="";
                for (String str:resultLS){
                    String pattern = "/[^\\s]*/redis.*";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(str);
                    if (m.find()){
                        RedisDir1=m.group();
                    }
                }
                //logger.debug("[RedisDir]=>"+RedisDir1);
                if (StringUtils.isBlank(RedisDir1)){
                    String pwd=getRdsPwdByPath(RedisDir1);
                    //resultMap.put("requirepass",pwd);
                    return pwd;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        //需要查看获取REDIS的kEY
        return "";
    }

    public static void main(String[] args) {
//        byte[] b=  readFileToByteArray("C:\\Users\\Administrator\\Desktop\\ai_waf_model.bin");
//        System.out.println(b.length);
//        for (int i = b.length-1; i >b.length-64 ; i--) {
//            System.out.print(String.format(" %02X ",b[i]));
//        }
        StrBuilder sb=new StrBuilder();
        for (int i = 0; i < 1000; i++) {
            String url="d00"+i+".cdntest.91hu.top";
            sb.append(url+"|");
        }
        System.out.println(sb);
    }
}
