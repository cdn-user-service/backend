package io.ants.common.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class CheckHyperRegUtils {
    private final static Logger logger = LoggerFactory.getLogger(CheckHyperRegUtils.class);

    public static boolean isWin(){
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            return true;
        }else {
            return false;
        }
    }

    public static int checkHyper(String regString ){
        final  String resourcePath = "check_hyper"; // JAR 中资源的路径
        final  String targetPath = "/usr/ants/cdn-api/check_hyper"; // 解压到的目标路径

        try {
            if (isWin()){
                return -1;
            }
            // 从 JAR 文件中提取资源
            Resource resource = new ClassPathResource(resourcePath);
            InputStream  is = resource.getInputStream();
            if (is == null) {
                logger.error("Resource file not found: " + resourcePath);
                return -1;
            }

            // 将资源写入目标路径
            Files.copy(is, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);

            // 关闭输入流
            is.close();

            // 赋予执行权限
            File file = new File(targetPath);
            if (!file.setExecutable(true)) {
                logger.error("Failed to set execute permission for: " + targetPath);
                return -1;
            }

            // 构建并执行命令
            List<String> list= ShellUtils.runShell(String.format("./%s -r  \"%s\"",resourcePath,regString),false);
            if (String.join("",list).trim().equals("correct")){
                return 1;
            }else {
                logger.error(regString+":"+String.join("",list));
                return 2;
            }
            // 等待进程结束
            //int exitCode = process.waitFor();
            //System.out.println("Process exited with code " + exitCode);

        } catch (Exception e){
            logger.error("Exception while:"+e.getMessage());
        }
        return -1;
    }
}
