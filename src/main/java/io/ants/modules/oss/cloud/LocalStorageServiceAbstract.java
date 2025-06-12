package io.ants.modules.oss.cloud;

import io.ants.common.exception.RRException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalStorageServiceAbstract extends AbstractCloudStorageService {


    @Override
    public String upload(byte[] data, String path) {
        try{
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.indexOf("windows")>=0){
                path=path.replace("/","");
                String dirPath="D:" + File.separator + "upload"  ;
                File testFile = new File(dirPath);
                if (!testFile.exists()) {
                    testFile.mkdirs();
                }
                dirPath=dirPath+ File.separator + path;
                Files.write(Paths.get(dirPath), data);
                return  path;
            }else if(OS.indexOf("linux")>=0){
                path=path.replace("/","");
                String dirPath="/usr/ants/cdn-api/upload";
                File testFile = new File(dirPath);
                if (!testFile.exists()) {
                    testFile.mkdirs();
                }
                dirPath=dirPath+ File.separator + path;
                Files.write(Paths.get(dirPath), data);
                return  path;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath("", suffix));
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            return this.upload(data, path);
        } catch (IOException e) {
            throw new RRException("上传文件失败", e);
        }
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath("", suffix));
    }
}
