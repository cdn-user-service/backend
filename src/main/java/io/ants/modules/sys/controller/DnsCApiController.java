package io.ants.modules.sys.controller;

import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.form.DnsAddRecordForm;
import io.ants.modules.app.form.DnsModifyRecordForm;
import io.ants.modules.app.form.RecordCleanForm;
import io.ants.modules.sys.dao.TbDnsConfigDao;
import io.ants.modules.sys.entity.TbDnsConfigEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.DnsCApiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/dnsapi/")
public class DnsCApiController extends AbstractController {

    @Autowired
    private DnsCApiService dnsCApiService;
    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;

    @PostMapping("/list")
    @PreAuthorize("hasAuthority('sys:dns_api:list')")
    public R list(@RequestBody Map params) {
        return R.ok().put("data", dnsCApiService.list(params, UserTypeEnum.MANAGER_TYPE.getId(), 0));
    }

    @GetMapping("/all/list")
    @PreAuthorize("hasAuthority('sys:dns_api:list')")
    public R allList() {
        return R.ok().put("data", dnsCApiService.allList(UserTypeEnum.MANAGER_TYPE.getId(), 0l));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('sys:dns_api:list')")
    public R all() {
        return R.ok().put("data", dnsCApiService.allList());
        // return
        // R.ok().put("data",dnsCApiService.allList(UserTypeEnum.MANAGER_TYPE.getId(),0l));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('sys:dns_api:save')")
    public R save(@RequestBody Map params) {
        params.put("user_type", UserTypeEnum.MANAGER_TYPE.getId());
        params.put("user_id", 0);
        return R.ok().put("data", dnsCApiService.save(params));
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('sys:dns_api:save')")
    public R delete(@RequestBody Map params) {
        checkDemoModify();
        if (params.containsKey("ids")) {
            String ids = params.get("ids").toString();
            dnsCApiService.delete(ids, UserTypeEnum.MANAGER_TYPE.getId(), 0);
            return R.ok();
        }
        return R.error("ids is null");
    }

    @GetMapping("/record/list")
    @PreAuthorize("hasAuthority('sys:dns_api:list')")
    public R recordList(@RequestParam Integer id) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(id);
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        R r1 = dnsCApiService.getRecordListToMapList(dnsConfig);
        R r2 = dnsCApiService.getRecordList(dnsConfig);
        if (1 == r1.getCode() && 1 == r2.getCode()) {
            return R.ok().put("data", r1.get("data")).put("r_data", r2.get("data")).put("source",
                    dnsConfig.getSource());
        }
        return R.error("").put("r1", r1).put("r2", r2);

    }

    @GetMapping("/line/list")
    public R LineList(@RequestParam Integer id) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(id);
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        return dnsCApiService.getDnsLine(dnsConfig, 0).put("source", dnsConfig.getSource());
    }

    @GetMapping("/line/list/v2")
    public R LineListV2(@RequestParam Integer id, @RequestParam Integer parentId) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(id);
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        return dnsCApiService.getDnsLineV2(dnsConfig, parentId).put("source", dnsConfig.getSource());
    }

    @PostMapping("/record/add")
    @PreAuthorize("hasAuthority('sys:dns_api:save')")
    public R recordAdd(@RequestBody DnsAddRecordForm form) {
        ValidatorUtils.validateEntity(form);
        if (null == form.getId()) {
            return R.error("参数ID缺失！");
        }
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(form.getId());
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        Object rAddObj = dnsCApiService.addRecord(dnsConfig, form.getTop(), form.getRecordType(), form.getLine(),
                form.getValue(), form.getTtl());
        if (null == rAddObj) {
            return R.error("添加失败！");
        }
        return R.ok().put("source", dnsConfig.getSource()).put("data", rAddObj);
    }

    @PostMapping("/record/remove")
    @PreAuthorize("hasAuthority('sys:dns_api:save')")
    public R recordRemove(@RequestBody Map params) {
        if (!params.containsKey("id") || !params.containsKey("recordId")) {
            return R.error("参数缺失！");
        }
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(params.get("id").toString());
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        String recordId = params.get("recordId").toString();
        Object rObj = dnsCApiService.removeRecordByRecordId(dnsConfig, recordId);
        return R.ok().put("source", dnsConfig.getSource()).put("data", rObj);
    }

    @PostMapping("record/modify")
    @PreAuthorize("hasAuthority('sys:dns_api:save')")
    public R recordModify(@RequestBody Map params) {
        DnsModifyRecordForm form = DataTypeConversionUtil.map2entity(params, DnsModifyRecordForm.class);
        if (null == form.getId()) {
            return R.error("参数ID缺失！");
        }
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(form.getId());
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        R r = dnsCApiService.modifyRecord(dnsConfig, form.getRecordId(), form.getTop(), form.getRecordType(),
                form.getLine(), form.getValue(), form.getTtl());
        return r.put("source", dnsConfig.getSource());
    }

    @PostMapping("/record/clean")
    @PreAuthorize("hasAuthority('sys:dns_api:save')")
    public R recordModify(@RequestBody RecordCleanForm params) {
        return dnsCApiService.removeRecordByInfoWithMainDomain(params.getDnsConfigId(), params.getMainDomain(),
                params.getTop(), params.getRecordType(), params.getLine(), params.getValue(), params.getTtl());
    }

    @PostMapping("/record/info")
    @PreAuthorize("hasAuthority('sys:dns_api:list')")
    public R recordInfo(@RequestBody Map params) {
        if (!params.containsKey("id") || !params.containsKey("top") || !params.containsKey("recordType")) {
            return R.error("参数缺失！");
        }
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(params.get("id").toString());
        if (null == dnsConfig) {
            return R.error("无此配置");
        }
        Integer id = (Integer) params.get("id");
        String top = params.get("top").toString();
        String type = params.get("recordType").toString();
        String line = "";
        if (params.containsKey("line")) {
            line = params.get("line").toString();
        }
        return dnsCApiService.getRecordByInfo(id, top, type, line);
    }

}
