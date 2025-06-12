package io.ants.common.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 */
public class ShellUtils {
    /**
     * 运行shell脚本
     * @param shell 需要运行的shell脚本
     */
    public static void execShell(String shell){
        try {
            Runtime.getRuntime().exec(shell);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 运行shell脚本 new String[]方式
     * @param shell 需要运行的shell脚本
     */
    public static void execShellBin(String shell){
        try {
            if(isLinux()){
                Runtime.getRuntime().exec(new String[]{"/bin/sh","-c",shell},null,null);
            }else if(isWin()){
                Runtime.getRuntime().exec(shell);
            }else {
                System.out.println("unknow system!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isWin(){
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            return true;
        }else {
            return false;
        }
    }

    public static boolean isLinux(){
        if(System.getProperty("os.name").toLowerCase().contains("linux")){
            return true;
        }else {
            return false;
        }
    }
    //    private static final boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
    //
    //    private static final boolean isLinux = System.getProperty("os.name").toLowerCase().indexOf("linux`") >= 0;


    public static void createShell(String path, String... strs)  {
        if (strs == null) {
            System.out.println("strs is null");
            return;
        }

        try{
            File sh = new File(path);
            if (sh.exists()) {
                sh.delete();
            }

            sh.createNewFile();
            sh.setExecutable(true);
            FileWriter fw = new FileWriter(sh);
            BufferedWriter bf = new BufferedWriter(fw);

            for (int i = 0; i < strs.length; i++) {
                bf.write(strs[i]);

                if (i < strs.length - 1) {
                    bf.newLine();
                }
            }
            bf.flush();
            bf.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void runnexec(String shPath){
        try{
            List<String> command = new ArrayList<>();
            command.add(shPath);
            // 执行cmd命令
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            Process process = builder.start();
            System.out.println("run_exec-process=>"+process.isAlive());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //执行shell
    public static String runShellByPath(String shpath) throws Exception {
        if (shpath == null || "".equals(shpath)) {
            System.out.println("shpath is empty");
            return "";
        }
        StringBuffer sb = new StringBuffer();
        try {
            Process ps = Runtime.getRuntime().exec(shpath);
            ps.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String createShellAndRun(String path, String... strs){
        try{
            createShell(path,strs);
            return runShellByPath(path);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }





    /**
     * 运行shell并获得结果，注意：如果sh中含有awk,一定要按new String[]{"/bin/sh","-c",shStr}写,才可以获得流
     *
     * @param shStr
     *            需要执行的shell
     */
    public static List<String> runShell(String shStr,boolean printOut) {
        List<String> strList = new ArrayList<String>();
        BufferedReader reader=null;
        try {
            if( isLinux()){
                // Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh","-c",shStr},null,null);
                //                String[] cmds = {"/bin/sh", "-c", shStr};
                //                Process process = (new ProcessBuilder(cmds)).redirectErrorStream(true).start();
                //                InputStreamReader ir = new InputStreamReader(process.getInputStream());
                //                LineNumberReader input = new LineNumberReader(ir);
                //                String line;
                //                process.waitFor();
                //                while ((line = input.readLine()) != null){
                //                    strList.add(line);
                //                }
                String[] cmds = {"/bin/sh", "-c", shStr};
                Process process = (new ProcessBuilder(cmds)).redirectErrorStream(true).start();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                process.waitFor();
                while ((line = reader.readLine()) != null) {
                    if (printOut){
                        System.out.println(line);
                    }
                    strList.add(line);
                }
                reader.close();
            }else if(isWin()){
                Runtime runtime = Runtime.getRuntime();
                reader = new BufferedReader(new InputStreamReader(runtime.exec(shStr).getInputStream()));
                String line=null;
                StringBuilder b=new StringBuilder();
                while ((line=reader.readLine())!=null) {
                    strList.add(line);
                    b.append(line).append("\n");
                }
                //System.out.println(b.toString());
                reader.close();
            }else {
                System.out.println("unknow system !");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return strList;
    }


    public static void runShellRuntimeOut(String shStr,List<String> outBuf){
        try {
            if( isLinux()){
                // Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh","-c",shStr},null,null);
                //                String[] cmds = {"/bin/sh", "-c", shStr};
                //                Process process = (new ProcessBuilder(cmds)).redirectErrorStream(true).start();
                //                InputStreamReader ir = new InputStreamReader(process.getInputStream());
                //                LineNumberReader input = new LineNumberReader(ir);
                //                String line;
                //                process.waitFor();
                //                while ((line = input.readLine()) != null){
                //                    strList.add(line);
                //                }
                String[] cmds = {"/bin/sh", "-c", shStr};
                Process process = (new ProcessBuilder(cmds)).redirectErrorStream(true).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                process.waitFor();
                while ((line = reader.readLine()) != null) {
                    //System.out.println(line);
                    outBuf.add(line);
                }
                reader.close();
            }else if(isWin()){
                Runtime runtime = Runtime.getRuntime();
                BufferedReader br = new BufferedReader(new InputStreamReader(runtime.exec(shStr).getInputStream()));
                String line=null;
                StringBuilder b=new StringBuilder();
                while ((line=br.readLine())!=null) {
                    outBuf.add(line);
                    b.append(line+"\n");
                }
                br.close();
                //System.out.println(b.toString());

            }else {
                System.out.println("unknow system !");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
