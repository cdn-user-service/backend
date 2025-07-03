package io.ants;

import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.modules.sys.dao.TableDao;
import io.ants.modules.sys.service.*;
import io.ants.modules.utils.config.ZeroSslConfig;
import io.ants.modules.utils.factory.ZeroSslFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CdnSysRunner implements ApplicationRunner {

    @Autowired
    private CdnSysAuthService authService;

    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private TbStreamProxyService tbStreamProxyService;
    @Autowired
    private TbSiteServer tbSiteServer;
    @Autowired
    private CdnSuitService cdnSuitService;
    @Autowired
    private TableDao tableDao;

    private void setZeroConfAndGetAcmeAccount() {
        try {
            ZeroSslConfig zConf = ZeroSslFactory.build();
            if (null != zConf) {
                if (StringUtils.isNotBlank(zConf.getEab_kid()) && StringUtils.isNotBlank(zConf.getEab_hmac_key())) {
                    QuerySysAuth.setZeroSslAccessId(zConf.getEab_kid());
                    QuerySysAuth.setZeroSslAccessPwd(zConf.getEab_hmac_key());
                }
                if (StringUtils.isNotBlank(zConf.getApi_key())) {
                    ZeroSslUtils.setAccess_key(zConf.getApi_key());
                }

            }
            AcmeShUtils.getAcmeAccount();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // private void resetMenu(){
    // if(System.getProperty("os.name").toLowerCase().contains("win")){
    // System.out.println("os fail");
    // return ;
    // }
    // try{
    // System.out.println("init sys_menu");
    // String pattern = "^INSERT\\s+INTO\\s+sys_menu\\s+\\.*.*;$";
    // Pattern r = Pattern.compile(pattern);
    // List<String> sqlList=new ArrayList<>();
    // for (String sql:
    // JarResourcesFileUtil.getResourceByLine("cdn20_sys_menu.sql")){
    // if (StringUtils.isBlank(sql)){
    // continue;
    // }
    // //INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon,
    // order_num, meta) VALUES (7, 6, '查看', null,
    // 'sys:schedule:list,sys:schedule:info', 2, null, 0, null);
    // //System.out.println(sql);
    // Matcher m = r.matcher(sql);
    // if(m.matches()){
    // sqlList.add(sql);
    // }
    // }
    // if (sqlList.size()>0){
    // tableDao.clearSysMenuTable();
    // for (String sql:sqlList){
    // tableDao.update_sql(sql);
    // }
    // }
    // }catch (Exception e){
    // e.printStackTrace();
    // }
    // }
    private void resetMenu() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            System.out.println("os fail");
            return;
        }
        try {
            Long count = tableDao.countSysMenuTable();
            if (count != null && count > 0) {
                System.out.println("sys_menu already initialized, skip reset");
                return;
            }

            System.out.println("init sys_menu");
            List<String> sqlList = new ArrayList<>();
            for (String sql : JarResourcesFileUtil.getResourceByLine("cdn20_sys_menu.sql")) {
                if (StringUtils.isNotBlank(sql)) {
                    sqlList.add(sql);
                }
            }
            if (!sqlList.isEmpty()) {
                tableDao.clearSysMenuTable();
                for (String sql : sqlList) {
                    tableDao.update_sql(sql);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCreateSyncConfig() {
        // 启动 生成 sync 配置文件
        try {
            FileUtils.fileWrite("/etc/rsyncd.conf", "uid=root\n" +
                    "gid=root\n" +
                    "max connections=100\n" +
                    "#use chroot=no:\n" +
                    "log file=/var/log/rsyncd.log\n" +
                    "lock file=/var/run/rsyncd.lock\n" +
                    "secrets file=/etc/rsyncd.pwd\n" +
                    "[ants]\n" +
                    "path=/usr/ants/cdn-api/nginx-config/\n" +
                    "ignore errors\n" +
                    "read only = yes\n" +
                    "list = no\n" +
                    "auth users=ants");

            String rdiPwd = String.format("ants:%s", FileUtils.getRedisPassWord());
            // System.out.println(rdiPwd);
            FileUtils.fileWrite("/etc/rsyncd.pwd", rdiPwd);

            ShellUtils.runShell("chmod 600 /etc/rsyncd.pwd", false);
            ShellUtils.runShell("systemctl enable rsyncd", false);
            ShellUtils.runShell("systemctl restart rsyncd", false);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initUpdateDbInfo() {
        // 重新更新部分数据绑定给字段
        try {
            tbStreamProxyService.reInitDefaultParam();
            tbSiteServer.reInitSiteAttr();
            cdnSuitService.reInitDbColumns();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                return;
            }

            if (!AppConfigHelper.isDev()) {
                System.out.println("[initCreateSyncConfig] 走 yml 逻辑，不走initCreateSyncConfig");
                initCreateSyncConfig();
            }

            // claean QRTZ_SCHEDULER_STATE
            authService.getAuthInfo();

            // 获取ACME账号
            setZeroConfAndGetAcmeAccount();

            initUpdateDbInfo();

            // 重置菜单
            resetMenu();

            // 开启推送任务线程
            cdnMakeFileService.operaLinkTaskV2();

            // 继续执行申请证书
            cdnMakeFileService.applyCertThread();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
