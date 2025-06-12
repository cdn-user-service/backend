package io.ants.common.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class JarResourcesFileUtil {

    public static void extractResource(String resourceName)  {
        try{
            Resource resource = new ClassPathResource(resourceName);
            InputStream  inputStream = resource.getInputStream();

            if (inputStream == null) {
                System.err.println("资源不存在：" + resourceName);
                return;
            }

            // 获取 jar 包所在的目录路径
            String jarParentPath = System.getProperty("user.dir");
            // System.out.println(jarParentPath);
            // 构建目标文件路径
            String targetFilePath = jarParentPath + File.separator + resourceName;
            Path targetPath = new File(targetFilePath).toPath();

            // 将资源拷贝到目标文件路径
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("资源已成功释放到：" + targetFilePath);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<String> getResourceByLine(String resourceName){
        List<String> list=new ArrayList<>();
        try{
            Resource resource = new ClassPathResource(resourceName);
            InputStream  inputStream = resource.getInputStream();
            if (inputStream == null) {
                System.err.println("资源不存在：" + resourceName);
                return list;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 处理每一行数据
                    // System.out.println(line);
                    list.add(line);
                }
            } catch (IOException e) {
                // 处理异常
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    public static void main(String[] args) {
        //extractResource("cdn20_sys_menu.sql");
        //for (String sql: JarResourcesFileUtil.getResourceByLine("cdn20_sys_menu.sql")){
            //System.out.println(sql);
        // }
    }
}
