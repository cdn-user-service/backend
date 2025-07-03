package io.ants.modules.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.annotation.UserLog;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbSiteCachePrefetchEntity;
import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.app.entity.TbSiteGroupEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.*;
import io.ants.modules.app.service.TbSiteCacheService;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.app.vo.CreateSingleCreateSiteForm;
import io.ants.modules.sys.enums.CdnSiteStatusEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.form.BatExportForm;
import io.ants.modules.sys.form.BatReissuedForm;
import io.ants.modules.sys.form.BatchAddSiteAttrForm;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.CdnPublicAttrService;
import io.ants.modules.sys.service.TbSiteServer;
import io.ants.modules.sys.vo.PurgeVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/site/")
@Tag(name = "站点管理")
public class AppSiteController {

    private static final Logger logger = LoggerFactory.getLogger(AppSiteController.class);

    @Autowired
    private TbSiteServer tbSiteServer;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private CdnPublicAttrService cdnPublicAttrService;
    @Autowired
    private TbUserService userService;
    @Autowired
    private TbSiteCacheService tbSiteCacheService;

    @Login
    @PostMapping("/list")
    @Operation(summary = "站点列表")
    public R siteList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QuerySitePageForm form) {
        form.setUserIds(userId.toString());
        PageUtils pageData = tbSiteServer.querySitePage(form);
        return R.ok().put("data", pageData);
    }

    @Login
    @GetMapping("/list/all")
    @Operation(summary = "所有站点列表【仅SiteName】")
    public R siteListAll(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestParam Integer igg) {
        // igg=0 去除已经分配的
        return tbSiteServer.getAllSiteList(userId, igg);

    }

    @Login
    @PostMapping("/group/list")
    @Operation(summary = "获取域名分组")
    public R siteGroupList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QuerySiteGroupForm form) {
        return tbSiteServer.querySiteGroupList(userId, form);
    }

    @Login
    @PostMapping("/group/save")
    @Operation(summary = "保存域名分组")
    public R saveSiteGroup(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody TbSiteGroupEntity tbSiteGroup) {
        return tbSiteServer.saveSiteGroupList(userId, tbSiteGroup);
    }

    @Login
    @PostMapping("/group/delete")
    @Operation(summary = "删除域名分组")
    public R deleteSiteGroup(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody DeleteIdsForm form) {
        return tbSiteServer.deleteSiteGroup(userId, form);
    }

    @Login
    @GetMapping("/site/change/status")
    @Operation(summary = "修改站点状态")
    @UserLog("修改站点状态")
    public R changeSiteStatus(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, Integer siteId,
            Integer status) {
        TbSiteEntity site = tbSiteServer.getSiteEntityStatus(userId, siteId);
        if (null != site && null != site.getStatus() && 2 == site.getStatus()) {
            return R.error("已被管理员锁定！请联系管理员");
        }
        return tbSiteServer.changeSiteStatus(userId, siteId, status);
    }

    @Login
    @GetMapping("/site/info")
    @Operation(summary = "获取站点")
    public R getSiteInfo(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestParam String name) {
        return tbSiteServer.getSiteInfo(userId, name);
    }

    @Login
    @GetMapping("/site/name/check")
    @Operation(summary = "检查站点是否可用")
    public R checkSiteName(@RequestParam String name) {
        return tbSiteServer.checkSiteName(name);
    }

    @Login
    @PostMapping("/site/change/suit")
    @Operation(summary = "修改套餐")
    @UserLog("修改套餐")
    public R change_suit(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        if (!params.containsKey("siteId") || !params.containsKey("serialNumber")) {
            return R.error("缺少[siteId][serialNumber]");
        }
        Integer siteId = (Integer) params.get("siteId");
        String serialNumber = (String) params.get("serialNumber");
        TbSiteEntity site = tbSiteServer.changeSiteSerialNumber(userId, siteId, serialNumber);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return R.ok().put("data", site);
    }

    @Login
    @PostMapping("/create")
    @Operation(summary = "创建站点")
    @UserLog("创建站点")
    public R siteCreate(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map map) {
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        CreateSingleCreateSiteForm form = DataTypeConversionUtil.map2entity(map, CreateSingleCreateSiteForm.class);
        form.setUserId(userId);
        return tbSiteServer.singleCreateSite(form);

    }

    @PostMapping("/create/site/by/test")
    @Operation(summary = "创建站点【测试用户-免费套餐-】")
    @UserLog("创建站点【测试用户-免费套餐】")
    public R testCreateSite(@RequestBody CreateSiteByTestUserForm form) {
        ValidatorUtils.validateEntity(form);
        String userName = "test_user_" + form.getUserLabel();
        TbUserEntity userEntity = tbUserDao
                .selectOne(new QueryWrapper<TbUserEntity>().eq("username", userName).last("limit 1"));
        int newUserFlag = 0;
        if (null == userEntity) {
            userEntity = new TbUserEntity();
            userEntity.setUsername(userName);
            userEntity.setPassword(DigestUtils.sha256Hex("123456"));
            userService.save(userEntity);
            newUserFlag = 1;
        }
        return tbSiteServer.testCreateSite(userEntity.getUserId(), form.getMainServerName(), form.getSProtocol(),
                newUserFlag);
    }

    @PostMapping("/by_access/create")
    @Operation(summary = "access创建站点")
    @UserLog("access创建站点")
    public R siteCreate(@RequestBody CreateSiteByAccessForm form) {
        ValidatorUtils.validateEntity(form);
        TbUserEntity userEntity = tbUserDao.selectOne(new QueryWrapper<TbUserEntity>()
                .eq("u_cdn_access_token", form.getAccess_token())
                .select("user_id")
                .last("limit 1"));
        if (null == userEntity) {
            return R.error("token error!");
        }
        String siteDomain = "";
        String sIp = null;
        String sPort = null;
        String[] dInfos = form.getMainServerName().split("\\|");
        if (1 == dInfos.length) {
            siteDomain = dInfos[0];
        } else if (2 == dInfos.length) {
            siteDomain = dInfos[0];
            sIp = dInfos[1];
        } else if (3 == dInfos.length) {
            siteDomain = dInfos[0];
            sIp = dInfos[1];
            sPort = dInfos[2];
        }
        String mainDomain = siteDomain;
        String alias = null;
        if (siteDomain.contains(",")) {
            String[] dms = siteDomain.split(",");
            mainDomain = dms[0];
            alias = siteDomain.replace(mainDomain, "");
        }
        TbSiteEntity site = tbSiteServer.createSite(userEntity.getUserId(), form.getSerialNumber(), mainDomain, alias,
                sIp, sPort, form.getSProtocol());
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return R.ok().put("data", site);
    }

    @Login
    @PostMapping("/batcreate")
    @Operation(summary = "批量创建站点")
    @UserLog("批量创建站点")
    public R batcreate(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map map) {
        BatchCreateSite batchCreateSiteParam = DataTypeConversionUtil.map2entity(map, BatchCreateSite.class);
        batchCreateSiteParam.setUserId(userId);
        ValidatorUtils.validateEntity(batchCreateSiteParam);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return tbSiteServer.batchCreateStdSite(userId, batchCreateSiteParam);
    }

    @Login
    @PostMapping("/delete")
    @Operation(summary = "删除站点")
    @UserLog("删除站点")
    public R delete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        if (params.containsKey("ids")) {
            String ids = params.get("ids").toString();
            if (StringUtils.isNotBlank(ids)) {
                String[] id_str = ids.split(",");
                for (String siteId : id_str) {
                    TbSiteEntity site = tbSiteServer.getSiteEntityStatus(userId, Integer.parseInt(siteId));
                    if (null != site && null != site.getStatus()
                            && CdnSiteStatusEnum.CLOSE_AND_LOCKED.getId() == site.getStatus()) {
                        return R.error("站点[" + siteId + "]已被管理员锁定！请联系管理员");
                    }
                }
                if (id_str.length > 20) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Integer c = tbSiteServer.batDeleteSite(userId, ids);
                            System.out.println("delete:count" + c);
                        }
                    }).start();
                    return R.ok("任务执行中");
                } else {
                    Integer c = tbSiteServer.batDeleteSite(userId, ids);
                    System.out.println("delete:count" + c);
                }
                // tbLogService.FrontUserWriteLog(userId,
                // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
                return R.ok("");
            }
        }
        return R.error("缺少参数");
    }

    @Login
    @PostMapping("/delete/attr")
    @Operation(summary = "删除站点属性")
    @UserLog("删除站点属性")
    public R delete_attr(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        if (!params.containsKey("id") || !params.containsKey("pkey")) {
            return R.error("参数缺失[id,,pkey]");
        }
        Integer result = tbSiteServer.deleteAttr(userId, (Integer) params.get("id"), params.get("pkey").toString());
        if (1 == result) {
            // tbLogService.FrontUserWriteLog(userId,
            // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
            return R.ok();
        } else {
            return R.error("删除失败！");
        }
    }

    @Login
    @PostMapping("/build/sys/waf/rule")
    @Operation(summary = "生成规则")
    @UserLog("生成规则")
    public R createSysWafRule(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody SysCreateWafRuleForm form) {
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.buildSysWafRule(userId, form);
    }

    @Login
    @PostMapping("/detail")
    @Operation(summary = "站点详情")
    public R detail(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        QuerySiteDetailForm form = DataTypeConversionUtil.map2entity(params, QuerySiteDetailForm.class);
        ValidatorUtils.validateEntity(form);
        if (null == form) {
            return R.error("获取失败！");
        }
        TbSiteEntity site = tbSiteServer.querySiteAttr(userId, form.getId(), form.getGroup(), form.getKey());
        if (null == site) {
            return R.error("站点有误！");
        }
        return R.ok().put("data", site);
    }

    @Login
    @PostMapping("/SaveSiteAttr")
    @Operation(summary = "保存站点属性")
    @UserLog("保存站点属性")
    public R saveAttr(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params) {
        params.put("userId", userId);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return tbSiteServer.saveSiteAttr(params);
    }

    @Login
    @PostMapping("/batch/modify/site_attr")
    @Operation(summary = "批量修改站点属性")
    @UserLog("批量修改站点属性")
    public R batchModifySiteAttr(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody BatchModifySiteAttrForm form) {
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.batchUpdateSiteAttr(userId, form);
    }

    @Login
    @PostMapping("/batch/add/site_attr")
    @Operation(summary = "批量添加站点属性")
    @UserLog("批量添加站点属性")
    public R batchAddSiteAttr(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody BatchAddSiteAttrForm form) {
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.batchAddSiteAttr(userId, form);
    }

    @Login
    @PostMapping("/batch/search/modify/site_attr")
    @Operation(summary = "批量匹配修改站点属性")
    @UserLog("批量匹配修改站点属性")
    public R batchSearchModifySiteAttr(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody BatchSearchModifySiteAttrForm form) {
        ValidatorUtils.validateEntity(form);
        return tbSiteServer.batchSearchUpdateSiteAttr(userId, form);
    }

    @Login
    @PostMapping("/ChangeAttrStatus")
    @Operation(summary = "修改站点属性状态")
    @UserLog("修改站点属性状态")
    public R ChangeAttrStatus(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody ChangeSiteAttrStatusForm form) {
        form.setUserId(userId);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        Integer result = tbSiteServer.changeAttrStatus(form.getUserId(), form.getSiteId(), form.getPkey(),
                form.getAttrId(), form.getStatus());
        return R.ok().put("data", result);
    }

    @Login
    @PostMapping("/ChangeAttrWeight")
    @UserLog("修改站点属性排序")
    @Operation(summary = "修改站点属性排序")
    public R ChangeAttrWeight(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody ChangeSiteAttrWeightForm form) {
        form.setUserId(userId);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return tbSiteServer.changeAttrWeight(form.getUserId(), form.getSiteId(), form.getPkey(), form.getAttrId(),
                form.getOpMode());
    }

    @Login
    @PostMapping("/query/elk")
    @Operation(summary = "query elk")
    public R queryElk(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QueryElkForm params) {
        // http://127.0.0.1:9090/api/v1/query_range?query=node_time_seconds&start=1652321843.779&end=1652325443.779&step=14
        // 测试服务概览数据
        logger.info("##### app ELK ######");

        String fakeJson = "{\n" +
                "  \"aggregations\": {\n" +
                "    \"0\": {\n" +
                "      \"buckets\": [\n" +
                "        {\n" +
                "          \"key_as_string\": \"2025-04-14T09:00:00.000+08:00\",\n" +
                "          \"key\": 1713066000000,\n" +
                "          \"doc_count\": 100,\n" +
                "          \"1\": {\n" +
                "            \"2\": {\n" +
                "              \"flow-in\": { \"value\": 12345678 },\n" +
                "              \"flow-out\": { \"value\": 23456789 },\n" +
                "              \"request-count\": { \"value\": 9876 }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        // logger.info("fakejson: ", fakeJson);

        // logger.info("Path: " + params.getPath());
        // Path: metricbeat-8.9.1/_search
        // logger.info("Param: " + params.getParam());
        // Param:
        // {"size":0,"query":{"bool":{"must":[{"range":{"@timestamp":{"gte":"now-1h","lte":"now"}}},{"match":{"event.dataset":"http.server_io_p"}},{"nested":{"path":"http.server_io_p.v","query":{"terms":{"http.server_io_p.v.s":["www.cdxyspa.com","m.qwerhao.com","y.qwerhao.com","m.qwerhao2.com","y.sdffthgfb.com","mcs.qwerhao3.com","pingtui.cc","wosha.top"]}},"inner_hits":{}}}]}},"aggs":{"0":{"date_histogram":{"field":"@timestamp","fixed_interval":"1m","time_zone":"Asia/Shanghai","min_doc_count":0},"aggs":{"1":{"nested":{"path":"http.server_io_p.v"},"aggs":{"2":{"filter":{"terms":{"http.server_io_p.v.s":["www.cdxyspa.com","m.qwerhao.com","y.qwerhao.com","m.qwerhao2.com","y.sdffthgfb.com","mcs.qwerhao3.com","pingtui.cc","wosha.top"]}},"aggs":{"flow-in":{"sum":{"field":"http.server_io_p.v.i"}},"flow-out":{"sum":{"field":"http.server_io_p.v.o"}},"request-count":{"sum":{"field":"http.server_io_p.v.r"}}}}}}}}}}
        // String
        // method=StringUtils.isBlank(params.getMethod())?"GET":params.getMethod().toUpperCase();
        // return tbSiteServer.queryElk(method,params.getPath(),params.getParam());
        return R.ok().put("data", fakeJson);
    }

    /**
     * 更新公共信息 至 REDIS
     */
    private void recordInfo(RedisUtils redisUtils) {
        if (StringUtils.isBlank(StaticVariableUtils.masterWebSeverName)) {
            HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
            if (null != request) {
                if ("localhost".equals(request.getServerName())) {
                    return;
                }
                if ("127.0.0.1".equals(request.getServerName())) {
                    return;
                }
                StaticVariableUtils.masterWebSeverName = request.getServerName();
                StaticVariableUtils.MasterWebPort = request.getServerPort();
                redisUtils.set("public:master:info:servername", StaticVariableUtils.masterWebSeverName, -1);
                redisUtils.set("public:master:info:port", StaticVariableUtils.MasterWebPort, -1);
                String f_str = StaticVariableUtils.masterWebSeverName + ":" + StaticVariableUtils.MasterWebPort
                        + "/antsxdp";
                redisUtils.set("public:master:api:addNodePath", f_str + StaticVariableUtils.ADD_NODE_PATH, -1);
                redisUtils.set("public:master:api:errorFeedbacks", f_str + StaticVariableUtils.FEEDBACKS, -1);
                StaticVariableUtils.MasterProtocol = HttpRequest
                        .getSchemeByServer(StaticVariableUtils.masterWebSeverName);
                if (StringUtils.isBlank(StaticVariableUtils.checkNodeInputToken)
                        && StringUtils.isNotBlank(StaticVariableUtils.authMasterIp)) {
                    StaticVariableUtils.checkNodeInputToken = HashUtils
                            .md5ofString("check_" + StaticVariableUtils.authMasterIp);
                }
            }
        }
    }

    @Login
    @PostMapping("/site/clean/cache")
    @Operation(summary = "缓存清理")
    @UserLog("缓存清理")
    public R purge_cache(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map params) {
        if (!params.containsKey("urls")) {
            return R.error("urls is empty");
        }
        String urls = params.get("urls").toString();
        StaticVariableUtils.makeFileThreadIndex++;
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<PurgeVo> lsVo = tbSiteServer.parsePurgeCacheUrl(userId, urls);
                cdnMakeFileService.pushPurgeCache(lsVo);
            }
        }).start();
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }

    /**
     * 解封IP
     */
    @Login
    @PostMapping("/release/short_cc/intercept/ips")
    @Operation(summary = "解封IP")
    public R releaseInterceptIps(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map map) {
        if (!map.containsKey("ips")) {
            return R.error("ips is empty");
        }
        map.put("userId", userId);
        String ips = map.get("ips").toString();
        Map pushmap = new HashMap();
        pushmap.put("release_intercept_ip", ips);
        cdnMakeFileService.pushByInputInfo(pushmap);
        return R.ok();
    }

    @Login
    @PostMapping("/pull/cache")
    @Operation(summary = "缓存预热")
    public R pullCache(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map param) {
        if (!param.containsKey("urls") || !param.containsKey("ips")) {
            return R.error("缺少参数[urls][ips]");
        }
        String urls = param.get("urls").toString();
        String ips = param.get("ips").toString();
        Integer count = tbSiteServer.PullCache(userId, urls, ips);
        return R.ok().put("data", count);
    }

    @Login
    @PostMapping("/pushByInputInfo")
    @Operation(summary = "缓存预热")
    public R pushByInputInfo(@RequestBody Map params) {
        String[] keys = { "site_select_chunk", "apply_certificate", "apply_certificate_v2",
                PushTypeEnum.SHELL_ANTS_CMD_TO_MAIN.getName(), PushTypeEnum.SHELL_ANTS_CMD_TO_NODE.getName() };
        for (Object key : params.keySet()) {
            if (!Arrays.asList(keys).contains(key)) {
                return R.error("不支持此操作！");
            }
        }
        this.recordInfo(redisUtils);
        StaticVariableUtils.makeFileThreadIndex++;
        Object res = cdnMakeFileService.pushByInputInfo(params);
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex).put("res", res);
    }

    @Login
    @GetMapping("/pub/web_rule_precise")
    @Operation(summary = "公共WAF")
    public R getPubWafPrecise() {
        Map map = new HashMap(8);
        map.put("key", "web_rule_precise");
        return cdnPublicAttrService.getPubKeyDetail(map);
    }

    @Login
    @PostMapping("/cert/bat/re_issued")
    @Operation(summary = "批量重签")
    public R batReissued(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody BatReissuedForm form) {
        return cdnMakeFileService.batSIteCertReissued(userId, form);
    }

    @Login
    @PostMapping("/bat/export")
    @Operation(summary = "批量导出")
    public R batExport(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody BatExportForm form) {
        return tbSiteServer.batExport(userId, form);
    }

    @Login
    @PostMapping("/prefetch/cache/list")
    @Operation(summary = "预取缓存")
    public R prefetchCacheList(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QuerySiteCachePrefetchPageForm form) {
        return tbSiteCacheService.getCachePrePageList(userId, form);
    }

    @Login
    @PostMapping("/prefetch/cache/save")
    @Operation(summary = "预取缓存保存")
    public R prefetchCacheSave(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody TbSiteCachePrefetchEntity form) {
        return tbSiteCacheService.saveCachePre(userId, form);
    }

    @Login
    @PostMapping("/prefetch/cache/delete")
    @Operation(summary = "预取缓存删除")
    public R prefetchCacheDelete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody DeleteIdsForm form) {
        return tbSiteCacheService.delCachePre(userId, form);
    }
}
