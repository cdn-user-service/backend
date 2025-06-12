package io.ants.common.utils;

import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.app.vo.AcmeDnsVo;
import io.ants.modules.app.vo.ApplyCertVo;
import io.ants.modules.sys.enums.TbCertifyStatusEnum;
import io.ants.modules.sys.vo.TbCertifyObjVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcmeShUtils {
    public static final Logger logger = LoggerFactory.getLogger(AcmeShUtils.class);

    private static final Long TIMEOUT=10*60*1000L;
    public static String ACCOUNT_THUMBPRINT=null;
    public static boolean PUSH_ACCOUNT_FLAG=false;
    public static boolean get_account_handle=false;
    public static boolean apply_thread_handle=false;
    //public static Set<Integer>apply_site_id_handle_set=new HashSet<>();
    public static Set<Integer> apply_site_id_handle_set = new ConcurrentSkipListSet<>();
    public static ConcurrentHashMap<Integer,ApplyCertVo>applyCertVoSet =new ConcurrentHashMap<>();
    //public static Set<String>apply_site_name_handle_set=new HashSet<>();
    public static long apply_reg_time=0L;

    public static ConcurrentHashMap<Integer,Integer> apply_status_map =new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,Long> apply_time_map =new ConcurrentHashMap<>();
    public static HashMap<String,String > apply_infos_map=new HashMap<>();

    public static  String acmeRootDir="";

    public static String getAcmeRootDir(){
        if (StringUtils.isBlank(acmeRootDir)){
            final  String path1 = "/root/.acme.sh/README.md";
            final  String path2 = "/root/acme.sh/README.md";
            final  String path3 = "/root/acme.sh/acme.sh/README.md";
            String[] fPath={path1,path2,path3};
            for(String path:fPath){
                try{
                    File file = new File(path);
                    boolean fileExists = file.exists();
                    if (fileExists){
                        String directoryPath = file.getParent();
                        acmeRootDir=directoryPath;
                        return acmeRootDir;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return acmeRootDir;
    }

    public static R applyCertAndNotice(Integer siteId,  String domainList,String  noticeCallBackCmd){
        String eMsg="";
        // //-1=待申请；0= 申请中; 1=申请成功;2=申请失败；3=自有证书;4=证书过期
        apply_status_map.put(siteId, TbCertifyStatusEnum.APPLYING.getId());
        getAcmeAccount();
        try {
            if (apply_site_id_handle_set.contains(siteId)){
                //logger.debug("handle is true");
                apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                return R.error("申请中");
            }
            apply_site_id_handle_set.add(siteId);
            if (ShellUtils.isWin()){
                logger.error("is win,AcmeShUtils fail!");
                apply_site_id_handle_set.remove(siteId);
                apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                return R.error("WIN 系统不支持");
            }
            if (apply_site_id_handle_set.size()>2){
                apply_site_id_handle_set.remove(siteId);
                apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                return R.error("任务中，请稍候再试");
            }
            //logger.debug("[Acme_Sh_Utils.apply_cert]siteId="+siteId);
            if (StringUtils.isBlank(ACCOUNT_THUMBPRINT)){
                apply_site_id_handle_set.remove(siteId);
                logger.error("ACCOUNT_THUMBPRINT is null");
                apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                return R.error("ACCOUNT_THUMBPRINT is empty!");
            }
            if (apply_time_map.containsKey(siteId.toString())){
                if (System.currentTimeMillis()- apply_time_map.get(siteId)<180*1000){
                    apply_site_id_handle_set.remove(siteId);
                    logger.error("to fast is null");
                    apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                    return R.error("申请频繁，请稍候再试！");
                }
            }

            apply_time_map.put(siteId.toString(),System.currentTimeMillis());
            //logger.debug("[Acme_Sh_Utils.apply_cert]===>siteId="+siteId);
            //1 dList
            StringBuilder dList=new StringBuilder();
            for (String dName:domainList.split(",")){
                   String tUrl="http://"+dName;
                   if (HttpRequest.isNormalReturnAcmeAccount(tUrl)) {
                       dList.append(" -d "+dName+" ");
                   }else{
                       logger.error("return_acme_error："+tUrl);
                   }
            }
            if (StringUtils.isBlank(dList.toString())){
                apply_site_id_handle_set.remove(siteId);
                logger.error("验证域名失败！【2】,dList is empty:"+dList);
                apply_time_map.remove(siteId.toString());
                apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                return R.error("申请的证书域名为空");
            }
            R r0=getApplyCertInfos(siteId,"");
            if (1==r0.getCode()){
                if (r0.containsKey("notAfter")){
                    long notAfter=new Long(r0.get("notAfter").toString());
                    if (0l!=notAfter &&  DateUtils.addDateMonths(DateUtils.LongStamp2Date(notAfter),-1).after(new Date())){
                        apply_site_id_handle_set.remove(siteId);
                        logger.info("证书有效期大于1个月，不可重签");
                        apply_time_map.remove(siteId.toString());
                        apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                        if (StringUtils.isNotBlank(noticeCallBackCmd)){
                            runShellRuntimeOutToMap(noticeCallBackCmd,siteId.toString());
                        }
                        return R.error("证书有效期大于1个月，不可重签");
                    }
                }
            }
            FileUtils.mkdir(getAcmeRootDir()+"/ants/");
            //String pem_path=String.format(" --cert-file /root/.acme.sh/ants/%d.crt",siteId) ;
            String pem_path=String.format(" --fullchain-file %s/ants/%d.crt",getAcmeRootDir(),siteId) ;
            String key_path=String.format(" --key-file %s/ants/%d.key",getAcmeRootDir(),siteId);
            //2 执行申请
            //String noticeCallBackCmd="\"php /home/antscdn/web/manager/manager/api/acmeRenewNotice.php site_id={$site_id} src=apply \"";
            //String apply_command=String.format("%s/acme.sh  --server zerossl  --install-cert %s  %s --issue %s --stateless --force  --reloadcmd %s",getAcmeRootDir(),pem_path,key_path,dList,noticeCallBackCmd);
            String apply_command=String.format("%s/acme.sh  --server letsencrypt  --install-cert %s  %s --issue %s --stateless --force  --reloadcmd %s",getAcmeRootDir(),pem_path,key_path,dList,noticeCallBackCmd);

            //acme.sh --issue -d waf.cdn.com  --stateless --force
            // /root/.acme.sh/acme.sh  --install-cert --fullchain-file /root/.acme.sh/ants/22.crt   --key-file /root/.acme.sh/ants/22.key --issue -d api.tianzhikui.com --stateless --force  --reloadcmd "curl http://127.0.0.1:8080/cdn/sys/common/acme/call/back?siteId=22"
            logger.warn("[AcmeShUtils.apply_cert]"+apply_command);
            R r= runShellRuntimeOutToMap(apply_command,siteId.toString());
            logger.warn("[AcmeShUtils.apply_cert],code="+r.getCode());
            apply_time_map.remove(siteId.toString());
            apply_site_id_handle_set.remove(siteId);
            if (1==r.getCode() && r.containsKey("data")){
                String data=r.get("data").toString();
                if (data.contains(" Cert success.")){
                    if (StringUtils.isNotBlank(noticeCallBackCmd)){
                       String resp=  HttpRequest.httpGetFromCmd(noticeCallBackCmd);
                       logger.info(resp);
                    }
                    return r;
                }
                return R.error().put("data",data);
            }
            return r;
        }catch (Exception e){
            eMsg=e.getMessage();
            apply_site_id_handle_set.remove(siteId);
            e.printStackTrace();
        }finally {
            apply_site_id_handle_set.remove(siteId);
            apply_time_map.remove(siteId.toString());
        }
        return R.error(eMsg);
    }


    /**
     * 获取证书证书
     * @param siteId
     * @param minServerName
     * @return
     */
    public static R getApplyCertInfos(Integer siteId,String minServerName){
        //-1=待申请；0= 申请中; 1=申请成功;2=申请失败；3=自有证书;4=证书过期
        //logger.debug("[AcmeShUtils.get_apply_cert_Infos]siteId="+siteId);
        //Map<String ,Object> map=new HashMap();
        try{
             if (apply_site_id_handle_set.contains(siteId)){
                //在 申请任务中
                Long create_tm= apply_time_map.getOrDefault(siteId,0L);
                if ((System.currentTimeMillis()-create_tm)>TIMEOUT ){
                    //超时
                    apply_status_map.put(siteId, TbCertifyStatusEnum.FAIL.getId());
                    //apply_map.remove(siteId);
                    return R.error("3").put("status",TbCertifyStatusEnum.FAIL.getId()).put("log",apply_infos_map.get(siteId));
                }
                //申请中
                return R.ok("0").put("status",TbCertifyStatusEnum.APPLYING.getId());
            }else {
                //申请任务无
                //1 查看证书
                // /root/.acme.sh/waf.cdn.com/waf.cdn.com.cer
                if (StringUtils.isNotBlank(minServerName)){
                    String cer_path=String.format("%s/%s/%s.cer",getAcmeRootDir(),minServerName,minServerName);
                    String cerStr=FileUtils.getStringByPath(cer_path);
                    if (HashUtils.isValidCert(cerStr)){
                        //申请成功
                        TbCertifyObjVo vo=new TbCertifyObjVo();
                        vo.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
                        vo.setPem_cert(cerStr.trim());
                        String key_Path=String.format("%s/%s/%s.key",getAcmeRootDir(),minServerName,minServerName);
                        String keyStr=FileUtils.getStringByPath(key_Path);
                        vo.setPrivate_key(keyStr.trim());
                        HashUtils.updateCertVoInfo(vo);
                        return R.ok(DataTypeConversionUtil.entity2map(vo));
                    }
                }
                if (null!=siteId){
                    String cer_site_path=String.format("%s/ants/%d.crt",getAcmeRootDir(),siteId);
                    String cer_site_Str=FileUtils.getStringByPath(cer_site_path);
                    if (HashUtils.isValidCert(cer_site_Str)){
                        //申请成功
                        TbCertifyObjVo vo=new TbCertifyObjVo();
                        vo.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
                        vo.setPem_cert(cer_site_Str.trim());
                        String key_Path=String.format("%s/ants/%d.key",getAcmeRootDir(),siteId);
                        String keyStr=FileUtils.getStringByPath(key_Path);
                        vo.setPrivate_key(keyStr.trim());
                        HashUtils.updateCertVoInfo(vo);
                        return R.ok(DataTypeConversionUtil.entity2map(vo));
                    }
                }
                return R.error("-1").put("status",TbCertifyStatusEnum.NEED_APPLY.getId());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return R.error("-1").put("status",TbCertifyStatusEnum.NEED_APPLY.getId());
    }

    private static int randomInt(  int min,int max){
        // min  定义随机数的最小值
        // max  定义随机数的最大值
        return (int) min + (int) (Math.random() * (max - min));
    }

    public static String getAcmeAccount(){
        if (get_account_handle){
            return ACCOUNT_THUMBPRINT;
        }
        if (StringUtils.isNotBlank(ACCOUNT_THUMBPRINT) ){
            return ACCOUNT_THUMBPRINT;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ShellUtils.isWin()){
                    logger.error("is win,AcmeShUtils fail!");
                    return;
                }
                apply_reg_time=System.currentTimeMillis();
                List<String> outBufList=new ArrayList<>();
                try{
                    get_account_handle=true;
                    // root/.acme.sh/acme.sh/acme.sh --register-account -m 123@qq.com
                    //ZERO SSL
                    /*
                    String cmd=String.format("%s/acme.sh --server zerossl --register-account -m %d@qq.com",getAcmeRootDir(),randomInt(1000000,100000000));
                    if (StringUtils.isNotBlank(QueryAnts.zeroSslAccessId)  && StringUtils.isNotBlank(QueryAnts.zeroSslAccessPwd)){
                        cmd=String.format("%s/acme.sh --register-account  --server zerossl  --eab-kid %s --eab-hmac-key %s ",getAcmeRootDir(),QueryAnts.zeroSslAccessId,QueryAnts.zeroSslAccessPwd);
                    }*/
                    String cmd=String.format("%s/acme.sh  --server  letsencrypt --register-account -m %d@qq.com",getAcmeRootDir(),randomInt(1000000,100000000));
                    logger.warn(cmd);
                    ShellUtils.runShellRuntimeOut(cmd,outBufList);
                    //logger.debug(String.join("\r\n",outBufList));
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    // ACCOUNT_THUMBPRINT='PdXEIxWbpro2UDm_QMbPOj4MpW0M3nlRRSppRVR4L-o'
                    //ACCOUNT_THUMBPRINT='PdXEIxWbpro2UDm_QMbPOj4MpW0M3nlRRSppRVR4L-o'
                    String pattern = "ACCOUNT_THUMBPRINT='[-_A-Za-z0-9]+'";
                    Pattern r = Pattern.compile(pattern);
                    for (String s:outBufList){
                        //System.out.println("--->"+s);
                        Matcher m = r.matcher(s);
                        if (m.find()){
                            String account_s=m.group();
                            account_s=account_s.replace("ACCOUNT_THUMBPRINT=","");
                            ACCOUNT_THUMBPRINT=account_s.replace("'","");
                            //logger.debug("final====>ACCOUNT_THUMBPRINT="+ACCOUNT_THUMBPRINT);
                        }
                    }
                    if (StringUtils.isBlank(ACCOUNT_THUMBPRINT)){
                        logger.error(outBufList.toString());
                    }
                    get_account_handle=false;
                }

            }
        }).start();
        //acme_sh_account_thumbprint
        return ACCOUNT_THUMBPRINT;
    }

    private static R runShellRuntimeOutToMap(String shStr,String mapKey){
        String eMsg="";
        StringBuilder sb=new StringBuilder();
        BufferedReader reader=null;
        try {
            String[] cmds = {"/bin/sh", "-c", shStr};
            Process process = (new ProcessBuilder(cmds)).redirectErrorStream(true).start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            process.waitFor();
            while ((line = reader.readLine()) != null) {
                //logger.debug("[Acm sh tToMap]"+line);
                sb.append("\n");
                sb.append(line);
                AcmeShUtils.apply_infos_map.put(mapKey,sb.toString());
            }
            reader.close();
            return R.ok().put("data",sb.toString());
        } catch (Exception e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

//    // -1=待申请；0= 申请中; 1=申请成功;2=申请失败；3=自有证书;4=证书过期
//    public static R applyCert(String HostNameList){
//        String eMsg="";
//        getAcmeAccount();
//        try {
//            if (apply_site_name_handle_set.contains(HostNameList)){
//                return R.error("申请中");
//            }
//            apply_site_name_handle_set.add(HostNameList);
//            if (ShellUtils.isWin()){
//                logger.error("is win,AcmeShUtils fail!");
//                apply_site_name_handle_set.remove(HostNameList);
//                return R.error("is win,AcmeShUtils fail!");
//            }
//           //logger.debug("[AcmeShUtils.apply_cert]HostNameList="+HostNameList);
//            if (StringUtils.isBlank(ACCOUNT_THUMBPRINT)){
//                apply_site_name_handle_set.remove(HostNameList);
//                //logger.error("ACCOUNT_THUMBPRINT is null");
//                apply_infos_map.put(HostNameList,"验证域名失败【0】");
//                return R.error("ACCOUNT_THUMBPRINT is empty");
//            }
//            apply_time_map.put(HostNameList,System.currentTimeMillis());
//            //logger.debug("[AcmeShUtils.apply_cert]===>apply_cert");
//            //1 执行申请
//            String[] hArray=HostNameList.split(",");
//            String m0dir=hArray[0];
//            //acme.sh --issue -d waf.cdn.com  --stateless --force
//            StringBuilder applyCmd=new StringBuilder();
//            boolean existHost=false;
//            applyCmd.append(getAcmeRootDir()+"/acme.sh --issue");
//            for (String h:hArray){
//                String tUrl="http://"+h;
//                if (HttpRequest.isNormalReturnAcmeAccount(tUrl)) {
//                    existHost=true;
//                    applyCmd.append(" -d ");
//                    applyCmd.append( h );
//                }
//
//            }
//            if (!existHost){
//                //logger.error("ACCOUNT_THUMBPRINT is null");
//                apply_infos_map.put(HostNameList,"验证域名失败【1】!");
//                return R.error("验证域名失败,CNAME未解析");
//            }
//            //2 清理原有证书文件目录
//            String m0path=String.format("%s/%s",getAcmeRootDir(),m0dir);
//            ShellUtils.execShell(String.format("rm -rf %s",m0path));
//
//            applyCmd.append(" --stateless --force");
//            //logger.debug("[AcmeShUtils.apply_cert]"+applyCmd.toString());
//            R r= runShellRuntimeOutToMap(applyCmd.toString(),HostNameList);
//            // /root/.acme.sh/waf.cdn.com/waf.cdn.com.cer
//            String cer_path=String.format("%s/%s/%s.cer",getAcmeRootDir(),hArray[0],hArray[0]);
//            String cerStr=FileUtils.getStringByPath(cer_path);
//            if (HashUtils.isValidCert(cerStr)){
//                apply_time_map.remove(HostNameList);
//            }else {
//                apply_time_map.put(HostNameList,0L);
//            }
//            apply_site_name_handle_set.remove(HostNameList);
//            return r;
//        }catch (Exception e){
//            eMsg=e.getMessage();
//            apply_site_name_handle_set.remove(HostNameList);
//            e.printStackTrace();
//        }finally {
//            apply_site_name_handle_set.remove(HostNameList);
//        }
//        return R.error(eMsg);
//    }


    public static void deleteCertFileBySiteId(Integer siteId,String certName){
        if (StringUtils.isNotBlank(certName)){
            String certFilePath=String.format("%s/%s",AcmeShUtils.getAcmeRootDir(),certName);
            //logger.debug("[certFilePath]="+certFilePath);
            File file = new File(certFilePath);
            if (file.exists()) {
                ShellUtils.runShell("rm -rf "+certFilePath,false);
            }
        }
        if (null!=siteId){
            String cer_site_path=String.format("%s/ants/%s.crt",AcmeShUtils.getAcmeRootDir(),siteId);
            File file = new File(cer_site_path);
            if (file.exists()) {
                ShellUtils.runShell("rm -rf "+cer_site_path,false);
            }
        }

    }

    public static R getApplyDomainDnsInfo(AcmeDnsVo applyVo){
        String serverName="letsencrypt";
        if (3==applyVo.getMode()){
            serverName="zerossl";
        }
        StringBuilder dList=new StringBuilder();
        for (String dm : applyVo.getTvMap().keySet()) {
            dList.append(" -d "+dm+" ");
        }
        if (StringUtils.isBlank(dList.toString())){
            String msg="验证域名失败！【2】,dList is empty:"+dList;
            logger.error(msg);
            applyVo.setStatus(0);
            return R.error(msg);
        }
        FileUtils.mkdir(getAcmeRootDir()+"/ants/");
        //String pem_path=String.format(" --fullchain-file %s/dns_ssl/%s.crt",getAcmeRootDir(),orderId) ;
        //String key_path=String.format(" --key-file %s/dns_ssl/%s.key",getAcmeRootDir(),orderId);
        //2 执行申请
        //String noticeCallBackCmd="\"php /home/antscdn/web/manager/manager/api/acmeRenewNotice.php site_id={$site_id} src=apply \"";
        // /root/.acme.sh/acme.sh  --issue    -d  cdntest.91hu.top  --challenge-alias 165668.com --dns --yes-I-know-dns-manual-mode-enough-go-ahead-please --stateless --force
        //String fName="";
        //if (StringUtils.isNotBlank(challengeAliasDomain)){
        //fName=String.format(" --challenge-alias %s ",challengeAliasDomain);
        //}
        String applyCommand=String.format("%s/acme.sh  --server %s  --issue %s --dns  --yes-I-know-dns-manual-mode-enough-go-ahead-please --stateless --force --debug ",getAcmeRootDir(),serverName,dList);
        //acme.sh --issue -d waf.antsxdp.com  --stateless --force
        // /root/.acme.sh/acme.sh  --install-cert --fullchain-file /root/.acme.sh/ants/22.crt   --key-file /root/.acme.sh/ants/22.key --issue -d api.tianzhikui.com --dns  --yes-I-know-dns-manual-mode-enough-go-ahead-please --stateless --force  --reloadcmd "curl http://127.0.0.1:8080/antsxdp/sys/common/acme/call/back?siteId=22"
        //   /root/.acme.sh/acme.sh  --server xgtk.com  --issue -d xgtk.com --dns   --yes-I-know-dns-manual-mode-enough-go-ahead-please --stateless --force
        logger.warn("[AcmeShUtils.get_ApplyDomainDnsInfo]"+applyCommand);
        String res=  runShellCmd(applyCommand);
        if (StringUtils.isBlank(res)){
            logger.error(res);
            applyVo.setStatus(0);
            return R.error(applyCommand);
        }
        if (res.contains(":Verify error:")){
            logger.info(res);
            applyVo.setStatus(0);
            return R.error(res);
        }
        if (res.contains("Cert success.")  || res.contains("_on_issue_success")) {
            String pem_path=String.format(" --fullchain-file %s/ants/%s.crt",getAcmeRootDir(),applyVo.getSiteId()) ;
            String key_path=String.format(" --key-file %s/ants/%s.key",getAcmeRootDir(),applyVo.getSiteId());
            String installCmd=String.format("%s/acme.sh  --server %s  --install-cert %s  %s  %s  --reloadcmd %s ",getAcmeRootDir(),serverName,dList,pem_path,key_path,applyVo.getNoticeCallBackCmd());
            logger.warn("[AcmeShUtils.get_ApplyDomainDnsInfo.installCmd]"+installCmd);
            //  /root/.acme.sh/www.4935.com/fullchain.cer
            //  /root/acme.sh/acme.sh  --server   4935.com --install-cert  -d  4935.com --fullchain-file /root/acme.sh/dns_ssl/17114601538389763.crt  --key-file  /root/acme.sh/ants/17114601538389763.key   --reloadcmd ''
            res+=  runShellCmd(installCmd);
            logger.info(res);
            //0=fail 1==applying 2=success  3=需要添加TXT记录 4=需要添加CNAME记录
            applyVo.setStatus(2);
            return R.ok(res);
        }
        Pattern pattern = Pattern.compile("Domain: '(.*?)'\n.*TXT value: '(.*?)'");
        Matcher matcher = pattern.matcher(res);
        while (matcher.find()) {
            AcmeDnsVo.TxtDomainValue tdv=applyVo.new TxtDomainValue();
            String domain = matcher.group(1);
            String txtValue = matcher.group(2);
            tdv.setDomain(domain);
            tdv.setType("TXT");
            tdv.setValue(txtValue);
            String mainDomain=DomainUtils.getMainTopDomain(domain);
            String top=tdv.getDomain().replace(mainDomain,"");
            if (StringUtils.isNotBlank(top)){
                //去除最后一个.
                if (".".equals(top.substring(top.length()-1))){
                    top=top.substring(0,top.length()-1);
                }
            }
            tdv.setMainDomain(mainDomain);
            tdv.setTop(top);
            applyVo.getTvMap().put(domain,tdv);
            //0=fail 1==applying 2=success  3=需要添加TXT记录 4=需要添加CNAME记录
            applyVo.setStatus(3);
            //System.out.println("Domain: " + domain);
            //System.out.println("TXT value: " + txtValue);
        }
        if (applyVo.getTvMap().size()==0){
            applyVo.setStatus(0);
            return R.error("tv map is empty");
        }
        return R.ok(res);
    }

    public static R renewApplyDomainDnsInfo(AcmeDnsVo applyVo,String needForce){
        String serverName="letsencrypt";
        if (3==applyVo.getMode()){
            serverName="zerossl";
        }
        StringBuilder dList=new StringBuilder();
        for (String dName:applyVo.getTvMap().keySet()){
            dList.append(" -d "+dName+" ");
        }
        if (StringUtils.isBlank(dList.toString())){
            String msg="验证域名失败！【2】,dList is empty:"+dList;
            logger.error(msg);
            applyVo.setStatus(0);
            return R.error(msg);
        }
        FileUtils.mkdir(getAcmeRootDir()+"/ants/");

        //2 执行申请
        //String noticeCallBackCmd="\"php /home/antscdn/web/manager/manager/api/acmeRenewNotice.php site_id={$site_id} src=apply \"";

        //"%s/acme.sh  --server %s  --renew  %s  --dns  %s  --yes-I-know-dns-manual-mode-enough-go-ahead-please  --force
        String applyCommand=String.format("%s/acme.sh  --server %s  --renew  %s  --dns   --yes-I-know-dns-manual-mode-enough-go-ahead-please  %s --debug",getAcmeRootDir(),serverName,dList,needForce);
        //acme.sh --issue -d waf.antsxdp.com  --stateless --force

        logger.warn("[AcmeShUtils.renew_ApplyDomainDnsInfo]"+applyCommand);
        String res=  runShellCmd(applyCommand);
        if (StringUtils.isBlank(res)){
            logger.error(applyCommand);
            applyVo.setStatus(0);
            return R.error(applyCommand);
        }
        logger.warn(res);
        //[Mon Dec 11 09:07:42 CST 2023] 'testfurl3.my1314.asia' is not an issued domain, skip
        if (res.contains(" is not an issued domain, skip")){
            //重新下订单
            logger.error("AcmeShUtils.renew_ApplyDomainDnsInfo,is not an issued domain, skip,get_ApplyDomainDnsInfo");
            getApplyDomainDnsInfo(applyVo);
        }
        // Cert success.
        else if (res.contains("Cert success.")  || res.contains("_on_issue_success")){
            String pem_path=String.format(" --fullchain-file %s/ants/%s.crt",getAcmeRootDir(),applyVo.getSiteId()) ;
            String key_path=String.format(" --key-file %s/ants/%s.key",getAcmeRootDir(),applyVo.getSiteId());
            String installCmd=String.format("%s/acme.sh  --server %s  --install-cert %s  %s  %s  --reloadcmd %s ",getAcmeRootDir(),serverName,dList,pem_path,key_path,applyVo.getNoticeCallBackCmd());
            logger.warn("[AcmeShUtils.renew_ApplyDomainDnsInfo.installCmd]"+installCmd);
            //  /root/.acme.sh/www.4935.com/fullchain.cer
            //  /root/acme.sh/acme.sh  --server   37550.vip --install-cert  -d  37550.vip --fullchain-file /root/acme.sh/dns_ssl/17099557634976038.crt  --key-file  /root/acme.sh/dns_ssl/17099557634976038.key   --reloadcmd ''
            String  installRes=  runShellCmd(installCmd);
            res+=installRes;
            logger.info(res);
            applyVo.setStatus(2);
            return R.ok(res);
        }else if (res.contains("Add '--force' to force to renew")){
            return renewApplyDomainDnsInfo(applyVo,"--force");
        }
        Pattern pattern = Pattern.compile("Domain: '(.*?)'\n.*TXT value: '(.*?)'");
        Matcher matcher = pattern.matcher(res);
        while (matcher.find()) {
            String domain = matcher.group(1);
            String txtValue = matcher.group(2);
            AcmeDnsVo.TxtDomainValue tdv=applyVo.new TxtDomainValue();
            tdv.setDomain(domain);
            tdv.setType("TXT");
            tdv.setValue(txtValue);

            String mainDomain=DomainUtils.getMainTopDomain(domain);
            String top=tdv.getDomain().replace(mainDomain,"");
            if (StringUtils.isNotBlank(top)){
                //去除最后一个.
                if (".".equals(top.substring(top.length()-1))){
                    top=top.substring(0,top.length()-1);
                }
            }
            tdv.setMainDomain(mainDomain);
            tdv.setTop(top);
            applyVo.getTvMap().put(domain, tdv);
            //System.out.println("Domain: " + domain);
            //System.out.println("TXT value: " + txtValue);
            //0=fail 1==applying 2=success  3=需要添加TXT记录 4=需要添加CNAME记录
            applyVo.setStatus(3);
        }
        return R.ok();
    }


    private static String runShellCmd(String shStr){
        StringBuilder sb=new StringBuilder();
        BufferedReader reader=null;
        try {
            String[] cmds = {"/bin/sh", "-c", shStr};
            Process process = (new ProcessBuilder(cmds)).redirectErrorStream(true).start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            process.waitFor();
            while ((line = reader.readLine()) != null) {
                //logger.debug("[AcmeShUtils.]"+line);
                sb.append("\n");
                sb.append(line);
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String filePath = "/root/.acme.sh/README.md";

        //        File file = new File(filePath);
        //        String directoryPath = file.getParent();
        //
        //        System.out.println("Directory: " + directoryPath);
        //1711610733
        //1711610774
        Integer exp=new Long(System.currentTimeMillis()/1000l).intValue() ;
        System.out.println(exp);
    }

}
