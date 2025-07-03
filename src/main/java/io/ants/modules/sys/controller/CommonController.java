package io.ants.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.HttpRequest;
import io.ants.common.utils.R;
import io.ants.common.utils.StaticVariableUtils;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.controller.AppAccountController;
import io.ants.modules.app.enums.UserModelEnum;
import io.ants.modules.app.form.RequestVo;
import io.ants.modules.job.task.JavaJobEnum;
import io.ants.modules.sys.dao.SysConfigDao;
import io.ants.modules.sys.dao.SysUserDao;
import io.ants.modules.sys.dao.TableDao;
import io.ants.modules.sys.dao.TbWorkOrderListDao;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.entity.TbWorkOrderListEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.CertCallbackForm;
import io.ants.modules.sys.service.CommonTaskService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.WebDirConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/common")
public class CommonController extends AbstractController {

    final private String lockFIlePath = "/usr/ants/cdn-api/ants_jar_init_lock";

    @Autowired
    private SysConfigDao sysConfigDao;

    @Resource
    TableDao tableDao;
    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private CommonTaskService commonTaskService;

    @Autowired
    private TbWorkOrderListDao tbWorkOrderListDao;
    @Autowired
    private ApplicationContext applicationContext;

    @PostMapping("/save/cert/callback")
    public R saveCertCallback(@RequestBody CertCallbackForm form) {
        return commonTaskService.saveCertCallback(form);
    }

    @PostMapping("/save/site/attr")
    public R saveSiteAttr(@RequestBody Map<String, Object> params) {
        return commonTaskService.saveSiteAttr(params);
    }

    @GetMapping("/acme/call/back")
    public R acmeCallBack(Integer siteId) {
        commonTaskService.viewLocalCertFileUpdateSslCrt(siteId);
        return R.ok();
    }

    @GetMapping("/enums")
    public R enums() {
        Map<String, Object> map = new HashMap();
        try {
            map.put("PayType", PayTypeEnum.getallpaytype());
            map.put("PayStatus", PayStatusEnum.getAllStatus());
            map.put("PayRecordStatus", PayRecordStatusEnum.getAllStatus());
            map.put("orderType", OrderTypeEnum.GetAllType());
            map.put("ClientStatus", ClientStatusEnum.getAllType());
            Map orderMap = new TreeMap();
            orderMap.put("product_example",
                    "{\"m\":{\"value\":1,\"status\":1},\"s\":{\"value\":100,\"status\":1},\"y\":{\"value\":1000,\"status\":1}}");
            orderMap.put("buy_product_example", "{\"type\":\"m\",\"sum\":1}");
            orderMap.put("renew_product_example", "{\"serialNumber\":\"1650361752071001\",\"sum\":1}");
            orderMap.put("adder_product_example",
                    "{\"serialNumber\":\"1650361752071001\",\"startTime\":\"1650769152\",\"type\":\"m\",\"sum\":2}");
            orderMap.put("upgrade_product_example", "{\"serialNumber\":\"1650361752071001\"}");
            map.put("orderInfo", orderMap);
            map.put("ProductAttrValueType", ProductAttrNameEnum.getAllTypes());
            map.put("ProductAttr", ProductAttrNameEnum.getAll());
            map.put("productAttr_kv", ProductAttrNameEnum.getKeyValues());
            map.put("productAttr_s", ProductAttrNameEnum.getSuffixKeyValues());
            map.put("productStatus", ProductStatusEnum.getAllType());
            map.put("siteAttr", SiteAttrEnum.getAll());

            map.put("dnsApiType", DnsApiEnum.getAllType());
            map.put("dnsUserType", UserTypeEnum.GetAll());
            map.put("mimeTypes", MimeTypesEnum.GetAll());
            map.put("nginxErrLogType", StaticVariableUtils.NGX_ERR_LOG_TYPES);
            map.put("LogTypeEnum", LogTypeEnum.GetAll());
            map.put("JavaJobEnum", JavaJobEnum.GetAll());
            map.put("pushEnum", PushTypeEnum.getAll());
            map.put("staticVariable", StaticVariableUtils.getStaticStaticVariable());
            map.put("workOrder", WorkOrderStatusEnum.getAll());
            map.put("all_valid_model", UserModelEnum.getAll());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok().put("data", map);

    }

    @GetMapping("/index")
    public R webConfig() {
        Map<String, Object> map = new HashMap<>();
        SysConfigEntity sysConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>()
                .eq("param_key", ConfigConstantEnum.WEB_SITE_CONFIG_KEY.getConfKey()).last("limit 1"));
        map.put("web_config", sysConfig);
        SysConfigEntity sysConfig_dir = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>()
                .eq("param_key", ConfigConstantEnum.WEB_DIR_CONF.getConfKey()).last("limit 1"));
        map.put("sysConfig_dir", sysConfig_dir);
        map.put("user_dir", null);
        if (null != sysConfig_dir) {
            String dirStr = sysConfig_dir.getParamValue();
            if (StringUtils.isNotBlank(dirStr)) {
                WebDirConfig webDir = DataTypeConversionUtil.string2Entity(dirStr, WebDirConfig.class);
                if (null != webDir && StringUtils.isNotBlank(webDir.getUserDir())) {
                    /// www/wwwroot/web/users/
                    String[] u_dir_s = webDir.getUserDir().split("/");
                    map.put("user_dir",
                            StaticVariableUtils.masterWebSeverName + "/" + u_dir_s[u_dir_s.length - 1] + "/");
                }
            }
        }

        List<SysConfigEntity> list = sysConfigDao
                .selectList(new QueryWrapper<SysConfigEntity>().select("id,param_key,status"));
        map.put("config_status", list);
        // exclusive_modeList
        if (StaticVariableUtils.exclusive_modeList.size() > 0) {
            for (String modeName : StaticVariableUtils.exclusive_modeList) {
                map.put(modeName, 1);
            }
        }

        Long wkCount = tbWorkOrderListDao.selectCount(new QueryWrapper<TbWorkOrderListEntity>().eq("status", 0));
        map.put("wkCount", wkCount);

        return R.ok().put("data", map);
    }

    @GetMapping("/dispatch")
    public R dispatch() {
        // online|timing
        if (StaticVariableUtils.dispatchThread) {
            return R.error("任务中！");
        }
        if ((System.currentTimeMillis() - StaticVariableUtils.dispatchTemp) < 60 * 1000) {
            return R.error("[dispatch]DispatchDns too fast! exit," + StaticVariableUtils.dispatchTemp);
        }
        StaticVariableUtils.dispatchTemp = System.currentTimeMillis();
        commonTaskService.groupDnsRecordDispatch();
        return R.ok();
    }

    @GetMapping("/check/node/normal")
    public R checkNodeHealth() {
        if (StaticVariableUtils.checkNodeThread) {
            return R.error("任务中！");
        }
        commonTaskService.checkNodeNormalHandle();
        return R.ok();
    }

    @PostMapping("/request")
    public R request(@RequestBody RequestVo requestVo) {
        ValidatorUtils.validateEntity(requestVo);
        return applicationContext.getBean(AppAccountController.class).request(0l, requestVo);
    }

    @GetMapping("/request/bytes/record")
    public R request_bytes_record() {
        if (StaticVariableUtils.bytesThread) {
            return R.error("任务中！");
        }
        if ((System.currentTimeMillis() - StaticVariableUtils.bytesTimeTemp) < (5 * 60 * 1000)) {
            return R.error("/request/bytes/recor too fast！wait...");
        }
        StaticVariableUtils.bytesTimeTemp = System.currentTimeMillis();
        commonTaskService.requestBytesRecordHandle();
        return R.ok();
    }

    @GetMapping("/pre_paid/task")
    public R pre_paid_task() {
        if (StaticVariableUtils.pre_paidThread) {
            return R.error("任务中！");
        }
        if ((System.currentTimeMillis() - StaticVariableUtils.pre_paidTimeTemp) < (5 * 60 * 1000)) {
            return R.error("pre_paid too fast！wait...");
        }
        StaticVariableUtils.pre_paidTimeTemp = System.currentTimeMillis();
        commonTaskService.prePaidTask();
        return R.ok();
    }

    private boolean isCanInit() {
        File file = new File(lockFIlePath);
        if (file.exists()) {
            file.delete();
            return true;
        }
        return false;
    }

    @GetMapping("/install")
    public String init() {
        if (isCanInit()) {
            if (StaticVariableUtils.demoIp.equals(StaticVariableUtils.authMasterIp)) {
                return "error!";
            }

            // 1 truncate table
            // String[] truncate_tables={"sys_captcha"};
            String[] truncate_tables = { "cdn_client", "cdn_client_group", "cdn_client_group_dns_conf", "cdn_consume",
                    "cdn_ip_control", "cdn_product", "cdn_suit", "schedule_job_log", "sys_captcha", "sys_log",
                    "sys_user_token", "tb_cdn_public_mut_attr", "tb_certify", "tb_dns_config", "tb_order",
                    "tb_pay_recory", "tb_site", "tb_site_attr", "tb_site_mut_attr", "tb_stream_proxy", "tb_user",
                    "tb_rewrite" };
            for (String table : truncate_tables) {
                tableDao.update_sql("truncate table " + table);
            }
            // 2 delele not admin
            sysUserDao.delete(new QueryWrapper<SysUserEntity>().ne("username", "admin"));

            // 3 update

            return "ok";
        } else {
            return "error";
        }
    }

}
