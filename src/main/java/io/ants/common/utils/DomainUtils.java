package io.ants.common.utils;

import io.ants.common.exception.RRException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainUtils {
    //public_suffix_list 下载地址
    //https://publicsuffix.org/list/public_suffix_list.dat
    //private final static Logger logger = LoggerFactory.getLogger(getClass());
    private static File SUFFIX_FILE;
    private static List<String> rules = new ArrayList<>();
    static {
        try {
            Resource resource = new ClassPathResource("public_suffix_list.dat");
            File inuModel = new File("public_suffix_list.dat");
            FileUtils.copyToFile(resource.getInputStream(), inuModel);
            SUFFIX_FILE = inuModel;
            //System.out.println("-->copy2file");
            // 读取规则文件
            BufferedReader reader = new BufferedReader(new FileReader(SUFFIX_FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                // 忽略注释和空行
                if (line.startsWith("//") || line.trim().isEmpty()) {
                    continue;
                }
                rules.add(line);
            }
            // 逆序规则，保证能够先匹配到最具体的规则
            Collections.reverse(rules);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取域名的主域名 eg:aa.bb.cc.w7soft.com.cn -->w7soft.com.cn
     * @param domain
     * @return
     */
    public static String getMainTopDomain(String domain) {
        if (StringUtils.isBlank(domain)){
            return "";
        }
        try {
            String topDomain="";
            // 将域名拆分成多个部分
            String[] domainParts = domain.split("\\.");
            String currentDomain="";
            for (int i = domainParts.length - 1; i >= 0; i--) {
                // 从右往左逐个读取域名部分
                currentDomain = domainParts[i] + (i < domainParts.length - 1 ? "." : "") + currentDomain;
                //System.out.println(currentDomain);
                if (rules.contains(currentDomain)) {
                    //System.out.println(currentDomain);
                    //System.out.println("The TLD of " + domain + " is " + domainParts[i+1] + "." + domainParts[i]);
                    if (i>=1){
                        topDomain=domainParts[i-1]+"."+currentDomain;
                    }else {
                        topDomain="";
                        //logger.error("domain:["+domain+"]error");
                        break;
                    }
                }
            }
            return topDomain.trim();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public static boolean isTopDomain(String domain){
        String topDomain= getMainTopDomain(domain);
        if (topDomain.equals(domain)){
            return true;
        }else {
            return false;
        }
    }

    public static boolean isNormalDomain(String domain){
        try{
            final  String serverNamePattern = "^(\\*\\.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
            Pattern r = Pattern.compile(serverNamePattern);
            Matcher m = r.matcher(domain);
            if(m.matches()){
                return  true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        //System.out.println(getTopDomain("com.cn"));
        System.out.println(getMainTopDomain("aa.bb.cc.w7soft.com.cn"));
        //w7soft.com.cn
    }





}
