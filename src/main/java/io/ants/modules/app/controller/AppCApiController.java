package io.ants.modules.app.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.ants.common.annotation.UserLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.DomainUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.form.DnsAddRecordForm;
import io.ants.modules.app.form.DnsModifyRecordForm;
import io.ants.modules.sys.controller.AbstractController;
import io.ants.modules.sys.dao.TbDnsConfigDao;
import io.ants.modules.sys.entity.TbDnsConfigEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.DnsCApiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
 
import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/app/dnsapi/")
@Tag(name = "dnsapi")
public class AppCApiController extends AbstractController {

    @Autowired
    private DnsCApiService dnsCApiService;
    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;

    @Login
    @PostMapping("/list")
    @Operation(summary = "dns api list")
    public R list(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map params){
        return R.ok().put("data",dnsCApiService.list(params, UserTypeEnum.USER_TYPE.getId(),userId));
    }


    @Login
    @GetMapping("/all")
    @Operation(summary = "dns api all")
    public R allList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId){
        return R.ok().put("data",dnsCApiService.allList(UserTypeEnum.USER_TYPE.getId(),userId));
    }


    @Login
    @PostMapping("/save")
    @Operation(summary = "dns api save")
    @UserLog("dns api save")
    public R save(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map params){
        params.put("user_type",UserTypeEnum.USER_TYPE.getId());
        params.put("user_id",userId);
        return R.ok().put("data",dnsCApiService.save(params));
    }

    @Login
    @PostMapping("/delete")
    @Operation(summary = "dns api delete")
    @UserLog("dns api delete")
    public R delete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestBody Map params){
        checkDemoModify();
        if(params.containsKey("ids")){
            String ids=params.get("ids").toString();
            dnsCApiService.delete(ids,UserTypeEnum.USER_TYPE.getId(),userId);
            return R.ok();

        }
        return R.error("ids is null");
    }

    @Login
    @GetMapping("/record/list")
    @Operation(summary = "dns record list")
    public R  recordList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestParam Integer id){
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(id);
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        R r1=dnsCApiService.getRecordListToMapList(dnsConfig);
        R r2=dnsCApiService.getRecordList(dnsConfig);
        if (1==r1.getCode() && 1==r2.getCode()){
            return R.ok().put("data",r1.get("data")).put("r_data",r2.get("data")).put("source",dnsConfig.getSource());
        }
        return R.error("").put("r1",r1).put("r2",r2);

    }



    @Login
    @GetMapping("/record/list/v2")
    @Operation(summary = "dns record list v2")
    public R  recordListV2(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestParam Integer id,@RequestParam String domain){
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(id);
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        String mainDomain= DomainUtils.getMainTopDomain(domain);
        dnsConfig.setAppDomain(mainDomain);
        R r1=dnsCApiService.getRecordListToMapList(dnsConfig);
        R r2=dnsCApiService.getRecordList(dnsConfig);
        if (1==r1.getCode() && 1==r2.getCode()){
            return R.ok().put("data",r1.get("data")).put("r_data",r2.get("data")).put("source",dnsConfig.getSource());
        }
        return R.error("").put("r1",r1).put("r2",r2);

    }

    @Login
    @GetMapping("/line/list")
    @Operation(summary = "dns line  list")
    public R  LineList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestParam Integer id){
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(id);
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }

        return dnsCApiService.getDnsLine(dnsConfig,0).put("source",dnsConfig.getSource());
    }

    @Login
    @GetMapping("/line/list/v1")
    @Operation(summary = "dns line  list")
    public R  LineList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestParam Integer id,@RequestParam String domain){
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(id);
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        String mainDomain= DomainUtils.getMainTopDomain(domain);
        dnsConfig.setAppDomain(mainDomain);
        return dnsCApiService.getDnsLine(dnsConfig,0).put("source",dnsConfig.getSource());
    }

    @Login
    @GetMapping("/line/list/v2")
    @Operation(summary = "dns line  list")
    public R  LineListV2(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestParam Integer id,@RequestParam Integer parentId){
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(id);
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        return dnsCApiService.getDnsLineV2(dnsConfig,parentId).put("source",dnsConfig.getSource());
    }

    @Login
    @PostMapping("/record/add")
    @UserLog("dns record add")
    public R recordAdd(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestBody DnsAddRecordForm form){
        ValidatorUtils.validateEntity(form);
        if(null==form.getId()){
            return R.error("参数ID缺失！");
        }
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(form.getId());
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        if (StringUtils.isNotBlank(form.getDomain())){
            String mainDomain= DomainUtils.getMainTopDomain(form.getDomain());
            dnsConfig.setAppDomain(mainDomain);
        }
        Object rAddObj=dnsCApiService.addRecord(dnsConfig,form.getTop(),form.getRecordType(),form.getLine(),form.getValue(),form.getTtl());
        if (null==rAddObj){
            return R.error("添加失败！");
        }
        return R.ok().put("source",dnsConfig.getSource()).put("data",rAddObj);
    }

    @Login
    @PostMapping("/record/remove")
    @UserLog("dns record remove")
    public R recordRemove(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestBody Map params){
        if(!params.containsKey("id") || !params.containsKey("recordId")){
            return R.error("参数缺失！");
        }
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(params.get("id").toString());
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        if (params.containsKey("domain") && null!=params.get("domain")){
            String mainDomain= DomainUtils.getMainTopDomain(params.get("domain").toString());
            dnsConfig.setAppDomain(mainDomain);
        }
        String recordId=params.get("recordId").toString();
        Object rObj=dnsCApiService.removeRecordByRecordId(dnsConfig,recordId);
        return R.ok().put("source",dnsConfig.getSource()).put("data",rObj);
    }

    @Login
    @PostMapping("record/modify")
    @UserLog("dns record modify")
    public R recordModify(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestBody Map params){
        DnsModifyRecordForm form= DataTypeConversionUtil.map2entity(params, DnsModifyRecordForm.class);
        if(null==form.getId()){
            return R.error("参数ID缺失！");
        }
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(form.getId());
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        if (params.containsKey("domain") && null!=params.get("domain")){
            String mainDomain= DomainUtils.getMainTopDomain(params.get("domain").toString());
            dnsConfig.setAppDomain(mainDomain);
        }
        R r=dnsCApiService.modifyRecord(dnsConfig,form.getRecordId(),form.getTop(),form.getRecordType(),form.getLine(),form.getValue(),form.getTtl());
        return r.put("source",dnsConfig.getSource());

    }



    @Login
    @PostMapping("/record/info")
    @UserLog("dns record info")
    public R recordInfo(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,@RequestBody Map params){
        if(!params.containsKey("id") || !params.containsKey("top") || !params.containsKey("recordType")){
            return R.error("参数缺失！");
        }
        TbDnsConfigEntity dnsConfig=tbDnsConfigDao.selectById(params.get("id").toString());
        if(null==dnsConfig || !dnsConfig.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) || !dnsConfig.getUserId().equals(userId) ){
            return R.error("无此配置");
        }
        Integer id=(Integer) params.get("id");
        String top=params.get("top").toString();
        String type=params.get("recordType").toString();
        String line="";
        if(params.containsKey("line")){
            line =params.get("line").toString();
        }
        return dnsCApiService.getRecordByInfo(id,top,type,line);
    }


}
