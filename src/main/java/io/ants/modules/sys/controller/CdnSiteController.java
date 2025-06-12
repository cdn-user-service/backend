package io.ants.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.utils.StaticVariableUtils;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbSiteCachePrefetchEntity;
import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.*;
import io.ants.modules.app.service.TbSiteCacheService;
import io.ants.modules.app.vo.CreateSingleCreateSiteForm;
import io.ants.modules.sys.enums.PreciseWafParamEnum;
import io.ants.modules.sys.enums.SiteAttrEnum;
import io.ants.modules.sys.enums.WafOpEnum;
import io.ants.modules.sys.form.BatExportForm;
import io.ants.modules.sys.form.BatReissuedForm;
import io.ants.modules.sys.form.BatchAddSiteAttrForm;
import io.ants.modules.sys.form.BuildAiModellingForm;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.TbSiteServer;
import io.ants.modules.sys.vo.PurgeVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/cdn/site")
public class CdnSiteController extends AbstractController {

    private static final Logger logger = LoggerFactory.getLogger(CdnSiteController.class);

    @Autowired
    private TbSiteServer tbSiteServer;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private TbSiteCacheService tbSiteCacheService;

    private String key2userIds(String key) {
        if(StringUtils.isBlank(key)){
            return  "";
        }
        List<TbUserEntity> list= tbUserDao.selectList(new QueryWrapper<TbUserEntity>()
                .like("username",key).or().like("mobile",key).or().like("mail",key)
                .select("user_id")
        );
        List<String> longList=list.stream().map(t->t.getUserId().toString()).collect(Collectors.toList());
        return String.join(",",longList);
    }

    @PostMapping("/list")
    public R siteList(@RequestBody QuerySitePageForm form){
       // logger.info("--1");
        if(StringUtils.isNotBlank(form.getUser())){
            String userIds=this.key2userIds(form.getUser());
            form.setUserIds(userIds);
        }
        PageUtils pageData= tbSiteServer.querySitePage(form);
        if (null==pageData){
            return R.error("获取失败！");
        }
        // logger.info("--2");
        return R.ok().put("data",pageData);
    }

    @SysLog("创建站点")
    @PostMapping("/create")
    public R siteCreate(@RequestBody   Map map ){
        CreateSingleCreateSiteForm form= DataTypeConversionUtil.map2entity(map,CreateSingleCreateSiteForm.class);
        return tbSiteServer.singleCreateSite(form);

    }


    @GetMapping("/site/name/check")
    public R checkSiteName(@RequestParam String name){
        return tbSiteServer.checkSiteName(name);
    }


    @GetMapping("/site/info")
    public R getSiteInfo(@RequestParam  String name){
        return tbSiteServer.getSiteInfo(null,name);
    }

    @SysLog("批量创建站点")
    @PostMapping("/batcreate")
    public R batCreate(@RequestBody Map map){
        BatchCreateSite batchCreateSiteParam=DataTypeConversionUtil.map2entity(map,BatchCreateSite.class);
        ValidatorUtils.validateEntity(batchCreateSiteParam);
        return tbSiteServer.batchCreateStdSite( batchCreateSiteParam.getUserId(),batchCreateSiteParam);
    }


    @SysLog("修改站点状态")
    @GetMapping("/change/status")
    public R changeSiteStatus(@RequestParam Integer siteId, @RequestParam Integer status){
        return tbSiteServer.changeSiteStatus(null,siteId,status);
    }

    @SysLog("修改站点套餐")
    @PostMapping("/change/suit")
    public R changeSuit(@RequestBody Map<String, Object> params){
        if (!params.containsKey("siteId") || !params.containsKey("serialNumber")){
            return R.error("缺少[siteId][serialNumber]");
        }
        Integer siteId=(Integer)params.get("siteId");
        String serialNumber=(String)params.get("serialNumber");
        TbSiteEntity site= tbSiteServer.changeSiteSerialNumber(null,siteId,serialNumber);
         return R.ok().put("data",site);
    }


    @PostMapping("/build/sys/waf/rule")
    @SysLog("生成WAF规则")
    public R createSysWafRule(@RequestBody SysCreateWafRuleForm form){
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.buildSysWafRule(null,form);
    }



    @SysLog("删除站点")
    @PostMapping("/delete")
    public R delete(@RequestBody Map<String, Object> params){
        checkDemoModify();
        if(params.containsKey("ids")){
            String ids=params.get("ids").toString();
            if(StringUtils.isNotBlank(ids)){
                String[] id_str=ids.split(",");
                if (id_str.length>20){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Integer c= tbSiteServer.batDeleteSite(null,ids);
                            System.out.println("delete:count"+c);
                        }
                    }).start();
                    return R.ok("任务执行中");
                }else{
                    Integer c= tbSiteServer.batDeleteSite(null,ids);
                    System.out.println("delete:count"+c);
                }
                return R.ok();
            }
        }
       return R.error("缺少参数");
    }

    @SysLog("删除站点属性")
    @PostMapping("/delete/attr")
    public R delete_attr(@RequestBody Map<String, Object> params){
        checkDemoModify();
        if(!params.containsKey("id") || !params.containsKey("pkey")){
            return R.error("参数缺失[id,,pkey]");
        }
        Integer result= tbSiteServer.deleteAttr(null,(Integer) params.get("id"),params.get("pkey").toString());
        if(1==result){
            return R.ok();
        }else {
            return R.error("删除失败！");
        }

    }


    //站点属性
    @PostMapping("/detail")
    public R detail(@RequestBody Map<String, Object> params){
        QuerySiteDetailForm form=DataTypeConversionUtil.map2entity(params,QuerySiteDetailForm.class);
        if (null==form){
            return R.error("获取失败！");
        }
        TbSiteEntity site= tbSiteServer.querySiteAttr(null,form.getId(),form.getGroup(),form.getKey());
        if(null==site){
            return R.error("站点有误！");
        }
        return R.ok().put("data",site);
    }

    @SysLog("保存站点属性")
    @PostMapping("/SaveSiteAttr")
    public R saveAttr(@RequestBody Map<String, Object> params){
        return tbSiteServer.saveSiteAttr(params);
    }



    @PostMapping("/batch/modify/site_attr")
    @SysLog("批量修改站点属性")
    public R batchModifySiteAttr(@RequestBody BatchModifySiteAttrForm form){
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.batchUpdateSiteAttr(null,form);
    }

    @PostMapping("/batch/search/modify/site_attr")
    @SysLog("批量匹配修改站点属性")
    public R batchSearchModifySiteAttr(@RequestBody BatchSearchModifySiteAttrForm form){
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.batchSearchUpdateSiteAttr(null,form);
    }


    @PostMapping("/batch/add/site_attr")
    @SysLog("批量添加站点属性")
    public R batchAddSiteAttr(@RequestBody BatchAddSiteAttrForm form){
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.batchAddSiteAttr(null,form);
    }

    @SysLog("修改站点属性状态")
    @PostMapping("/ChangeAttrStatus")
    public R changeAttrStatus(@RequestBody Map params){
        ChangeSiteAttrStatusForm form= DataTypeConversionUtil.map2entity(params, ChangeSiteAttrStatusForm.class);
        tbSiteServer.changeAttrStatus(form.getUserId(),form.getSiteId(),form.getPkey(),form.getAttrId(),form.getStatus());
        return R.ok();
    }

    @SysLog("修改站点属性排序")
    @PostMapping("/ChangeAttrWeight")
    public R changeAttrWeight(@RequestBody Map params){
        ChangeSiteAttrWeightForm form= DataTypeConversionUtil.map2entity(params, ChangeSiteAttrWeightForm.class);
        return  tbSiteServer.changeAttrWeight(form.getUserId(),form.getSiteId(),form.getPkey(),form.getAttrId(),form.getOpMode());
    }


    @PostMapping("/query/elk")
    public R queryElk(@RequestBody QueryElkForm params){
        //http://127.0.0.1:9090/api/v1/query_range?query=node_time_seconds&start=1652321843.779&end=1652325443.779&step=14
        // logger.info("##### cdn ELK #####");
        String method=StringUtils.isBlank(params.getMethod())?"GET":params.getMethod().toUpperCase();
        return tbSiteServer.queryElk(method,params.getPath(),params.getParam());
    }

    @SysLog("构建ai waf模型 ")
    @PostMapping("/ai/waf/modelling")
    public R  aiWafModelling(@RequestBody BuildAiModellingForm form){
        ValidatorUtils.validateEntity(form);
        //1 重建 2 增量构建
        return  tbSiteServer.buildAiWafModelling(form);
    }


    @GetMapping("/attr/enums")
    public R attr_enums(){
        Map map=new HashMap();
        for (String s : SiteAttrEnum.allGroup()) {
            map.put(s,SiteAttrEnum.getAllByGroupName(s));
        }
        return  R.ok().put("data", map).put("PreciseWaf", PreciseWafParamEnum.getAllMap()).put("waf_op", WafOpEnum.getAll());
    }

    @PostMapping("/site/clean/cache")
    @SysLog("清理缓存")
    public R purge_cache(@RequestBody Map params){
        if(!params.containsKey("urls")){
            return R.error("urls is empty");
        }
        String urls=params.get("urls").toString();
        StaticVariableUtils.makeFileThreadIndex++;

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<PurgeVo> lsVo= tbSiteServer.parsePurgeCacheUrl(null,urls);
                cdnMakeFileService.pushPurgeCache(lsVo);
            }
        }).start();
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }



    @PostMapping("/site/waf/intercept/result/data")
    public R intercept_result(@RequestBody Map params){
        String nodeIp="*";
        String serverName="*";
        String sourceIp="*";
        String interceptMode="*";
        String date="*";
        Integer page=1;
        Integer limit=100;
        if (params.containsKey("nodeIp") && StringUtils.isNotBlank(params.get("nodeIp").toString())){
            nodeIp=params.get("nodeIp").toString();
        }
        if (params.containsKey("serverName") && StringUtils.isNotBlank(params.get("serverName").toString())){
            serverName=params.get("serverName").toString();
        }
        if (params.containsKey("sourceIp") && StringUtils.isNotBlank(params.get("sourceIp").toString())){
            sourceIp=params.get("sourceIp").toString();
        }
        if (params.containsKey("interceptMode") && StringUtils.isNotBlank(params.get("interceptMode").toString())){
            interceptMode=params.get("interceptMode").toString();
        }
        if (params.containsKey("date") && StringUtils.isNotBlank(params.get("date").toString())){
            date=params.get("date").toString();
        }
        if (params.containsKey("page")&& StringUtils.isNotBlank(params.get("page").toString())){
            page=Integer.parseInt(params.get("page").toString());
        }
        if (params.containsKey("limit")&& StringUtils.isNotBlank(params.get("limit").toString())){
            limit=Integer.parseInt(params.get("limit").toString());
        }
        Map rMap= tbSiteServer.getInterceptResult(nodeIp,serverName,sourceIp,interceptMode,date,page,limit);
        return R.ok().put("data",rMap.get("data")).put("total",rMap.get("total"));
    }

    @SysLog("缓存预热")
    @PostMapping("/pull/cache")
    public R pull_cache(@RequestBody Map param){
        if (!param.containsKey("urls")){
            return R.error("缺少参数[urls]");
        }
        String urls=param.get("urls").toString();
        String ips="";
        if (param.containsKey("ips")&& null!=param.get("ips")){
            ips=param.get("ips").toString();
        }
        Integer count= tbSiteServer.PullCache(null,urls,ips);
        return R.ok().put("data",count);
    }




    @SysLog("批量重签")
    @PostMapping("/cert/bat/re_issued")
    public R batReissued(@RequestBody BatReissuedForm form ){
//        if (CertSrcTypeEnums.LetsencryptDns.getType()==form.getUseMode() || CertSrcTypeEnums.CertServerDnsV2.getType()==form.getUseMode()){
//            return R.error("仅用户端支持该模式证书");
//        }
         return  cdnMakeFileService.batSIteCertReissued(null,form);
    }


    @SysLog("批量导出站点")
    @PostMapping("/bat/export")
    public R batExport(@RequestBody BatExportForm form){
        return  tbSiteServer.batExport(null,form);
    }




    @PostMapping("/prefetch/cache/list")
    public R prefetchCacheList( @RequestBody QuerySiteCachePrefetchPageForm form ){
        return   tbSiteCacheService.getCachePrePageList(0,form);
    }


    @PostMapping("/prefetch/cache/save")
    @SysLog("预取缓存保存")
    public R prefetchCacheSave(@RequestBody TbSiteCachePrefetchEntity form ){
        return   tbSiteCacheService.saveCachePre(0,form);
    }


    @PostMapping("/prefetch/cache/delete")
    @SysLog("预取缓存删除")
    public R prefetchCacheDelete(@RequestBody DeleteIdsForm form ){
        return   tbSiteCacheService.delCachePre(0,form);
    }
}
