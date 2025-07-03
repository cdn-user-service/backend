package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.app.enums.BatchSearchUpdateSiteAttrEnum;
import io.ants.modules.app.form.*;
import io.ants.modules.app.vo.CreateSingleCreateSiteForm;
import io.ants.modules.app.vo.SysWafRuleConfVo;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.*;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.BatExportForm;
import io.ants.modules.sys.form.BatchAddSiteAttrForm;
import io.ants.modules.sys.form.BuildAiModellingForm;
import io.ants.modules.sys.service.*;
import io.ants.modules.sys.vo.*;
import io.ants.modules.utils.vo.DnsRecordItemVo;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TbSiteServerImpl implements TbSiteServer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // private static boolean buildModelThreadFlag=false;

    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    @Lazy
    private CdnSuitService suitService;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private TbCdnPublicMutAttrDao publicMutAttrDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private InputAvailableService inputAvailableService;
    @Autowired
    private TbCertifyDao tbCertifyDao;
    @Autowired
    private DnsCApiService dnsCApiService;
    @Autowired
    private CdnClientGroupChildConfDao cdnClientGroupChildConfDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private TbSiteGroupDao tbSiteGroupDao;
    @Autowired
    private CdnProductDao cdnProductDao;
    @Autowired
    private TbOrderDao tbOrderDao;

    private final String PUSH_SAVE = "save";
    private final String PUSH_DELETE = "delete";

    private void cleanSiteConfigFile(int siteId) {
        StaticVariableUtils.cacheSiteIdWafWhiteIpv4FileMap.remove(String.valueOf(siteId));
        StaticVariableUtils.cacheSiteIdWafBlockIpv4FileMap.remove(String.valueOf(siteId));
        StaticVariableUtils.cacheSiteIdWafRegFileMap.remove(String.valueOf(siteId));
        StaticVariableUtils.cacheSiteIdWafRuleFileMap.remove(String.valueOf(siteId));
        StaticVariableUtils.cacheSiteIdConfFileMap.remove(String.valueOf(siteId));
    }

    private boolean get_ssl_status(Integer siteId) {
        TbSiteMutAttrEntity mkey = tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id", siteId)
                .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                .eq("status", 1)
                .select("id")
                .last("limit 1"));
        TbSiteMutAttrEntity mpem = tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id", siteId)
                .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                .eq("status", 1)
                .select("id")
                .last("limit 1"));
        return null != mkey && null != mpem;
    }

    private void updateSiteChunk(String mode, TbSiteEntity site) {
        if (null == site || null == site.getId() || null == site.getStatus()) {
            return;
        }
        Map pushMap = new HashMap(8);
        if (PUSH_SAVE.equals(mode) && 1 == site.getStatus()) {
            pushMap.put(PushTypeEnum.SITE_CHUNK.getName(), site.getId().toString());
        } else if (PUSH_DELETE.equals(mode)) {
            this.sendDeleteNodeSiteFile(site, "all");
        }
        cdnMakeFileService.pushByInputInfo(pushMap);
    }

    private int getSiteAcmeJobStatus(Integer siteId) {
        TbSiteAttrEntity job_ssl_applyAttr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id", siteId)
                .eq("pkey", SiteAttrEnum.JOB_SSL_APPLY.getName())
                .eq("pvalue", "1")
                .eq("status", "1")
                .select("id")
                .last("limit 1"));
        if (null != job_ssl_applyAttr) {
            return 1;
        }
        return 0;
    }

    private int getSiteCheckCnameJobStatus(Integer siteId) {
        TbSiteAttrEntity job_ssl_applyAttr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id", siteId)
                .eq("pkey", SiteAttrEnum.JOB_CHECK_SITE_CNAME.getName())
                .select("pvalue")
                .last("limit 1"));
        if (null != job_ssl_applyAttr && StringUtils.isNotBlank(job_ssl_applyAttr.getPvalue())) {
            return Integer.parseInt(job_ssl_applyAttr.getPvalue());
        }
        return 0;
    }

    private void initSiteIdGroupInfo() {
        // siteIdGroupNameMap
        if (!StaticVariableUtils.siteIdGroupNameMap.isEmpty()) {
            return;
        }
        List<TbSiteGroupEntity> groupList = tbSiteGroupDao.selectList(new QueryWrapper<TbSiteGroupEntity>()
                .isNotNull("site_ids")
                .select("id,name,site_ids,remark"));
        for (TbSiteGroupEntity group : groupList) {
            for (String sid : group.getSiteIds().split(",")) {
                SiteGroupMiniVo vo = new SiteGroupMiniVo();
                if (StaticVariableUtils.siteIdGroupNameMap.containsKey(sid)) {
                    vo = StaticVariableUtils.siteIdGroupNameMap.get(sid);
                    String[] ids = vo.getIds().split(",");
                    if (!Arrays.asList(ids).contains(group.getId().toString())) {
                        vo.setIds(vo.getIds() + "," + group.getId());
                        vo.setNames(vo.getNames() + "," + group.getName());
                    }
                } else {
                    vo.setIds(group.getId().toString());
                    vo.setNames(group.getName());
                }
                StaticVariableUtils.siteIdGroupNameMap.put(sid, vo);
            }
        }
    }

    @Override
    public PageUtils querySitePage(QuerySitePageForm form) {
        // logger.info("---querySitePage---");
        form.setAreaId(form.getGroupId());
        String userIds = form.getUserIds();
        List<String> serialNumberList = new ArrayList<>();
        List<String> unInSerialNumberList = new ArrayList<>();
        List<Integer> inSiteIdList = new ArrayList<>();
        List<String> statusList = new ArrayList<>();
        if (StringUtils.isNotBlank(form.getId())) {
            inSiteIdList.add(Integer.parseInt(form.getId()));
        }
        if (StringUtils.isNotBlank(form.getMainServerName())) {
            inSiteIdList.add(0);
            List<TbSiteEntity> l1 = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                    .like("main_server_name", form.getMainServerName())
                    .select("id"));
            if (!l1.isEmpty()) {
                inSiteIdList.addAll(l1.stream().map(TbSiteEntity::getId).collect(Collectors.toList()));
            }
            List<TbSiteAttrEntity> l2 = tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("pkey", SiteAttrEnum.ALIAS.getName())
                    .like("pvalue", form.getMainServerName())
                    .select("site_id"));
            if (!l2.isEmpty()) {
                inSiteIdList.addAll(l2.stream().map(TbSiteAttrEntity::getSiteId).collect(Collectors.toList()));
            }
        }
        if (StringUtils.isNotBlank(form.getStatuss())) {
            statusList = Arrays.stream(form.getStatuss().split(",")).collect(Collectors.toList());
        }
        if (StringUtils.isNotBlank(form.getSourceIp()) || null != form.getSourcePort()
                || null != form.getListenPort()) {
            inSiteIdList.add(0);
            List<TbSiteMutAttrEntity> list = new ArrayList();
            // {"protocol":"https","upstream":"polling","port":443,"line":[{"port":"80","line":1,"domain":"","ip":"3.2.3.2","weight":1}],"s_protocol":"http","source_set":"ip"}
            if (StringUtils.isNotBlank(form.getSourceIp())) {
                // .and(q->q.like(StringUtils.isNotBlank(form.getSourceIp()),"pvalue",form.getSourceIp()).or().li.or().like(null!=form.getSourcePort()
                // ,"pvalue",form.getSourcePort()).or().like(null!=form.getListenPort(),"pvalue",form.getListenPort()))
                form.setSourceIp(form.getSourceIp().trim());
                List<TbSiteMutAttrEntity> list1 = tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                        .like("pvalue", form.getSourceIp())
                        .eq("status", 1)
                        .select("site_id,pvalue"));
                list.addAll(list1);
            }
            if (null != form.getSourcePort()) {
                List<TbSiteMutAttrEntity> list1 = tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                        .like("pvalue", form.getSourcePort().toString())
                        .eq("status", 1)
                        .select("site_id,pvalue"));
                list.addAll(list1);
            }
            if (null != form.getListenPort()) {
                List<TbSiteMutAttrEntity> list1 = tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                        .like("pvalue", form.getListenPort().toString())
                        .eq("status", 1)
                        .select("site_id,pvalue"));
                list.addAll(list1);
            }
            if (!list.isEmpty()) {
                logger.info("search list length: " + list.size());
                for (TbSiteMutAttrEntity item : list) {
                    NgxSourceBaseInfoVo vo = DataTypeConversionUtil.string2Entity(item.getPvalue(),
                            NgxSourceBaseInfoVo.class);
                    if (null != vo) {
                        if (null != vo.getLine() && !vo.getLine().isEmpty()) {
                            for (int i = 0; i < vo.getLine().size(); i++) {
                                NgxSourceBaseInfoVo.LineVo lineVo = vo.getLine().get(i);
                                if (null == lineVo) {
                                    continue;
                                }
                                if (StringUtils.isNotBlank(form.getSourceIp())) {
                                    if (StringUtils.isNotBlank(lineVo.getIp())
                                            && lineVo.getIp().contains(form.getSourceIp())) {
                                        inSiteIdList.add(item.getSiteId());
                                    } else if (StringUtils.isNotBlank(lineVo.getDomain())
                                            && lineVo.getDomain().contains(form.getSourceIp())) {
                                        inSiteIdList.add(item.getSiteId());
                                    }
                                }

                                if (null != form.getSourcePort()) {
                                    if (StringUtils.isNotBlank(lineVo.getPort())
                                            && lineVo.getPort().contains(form.getSourcePort().toString())) {
                                        inSiteIdList.add(item.getSiteId());
                                    }
                                }
                            }
                        }
                        if (null != vo.getPort() && null != form.getListenPort()
                                && vo.getPort().equals(form.getListenPort())) {
                            inSiteIdList.add(item.getSiteId());
                        }
                    }
                }
            }
        }

        // logger.info("---站点分组---");
        this.initSiteIdGroupInfo();
        if (null != form.getGroup_id() && 0 != form.getGroup_id()) {
            inSiteIdList.add(0);
            TbSiteGroupEntity siteGroup = tbSiteGroupDao.selectOne(new QueryWrapper<TbSiteGroupEntity>()
                    .eq("id", form.getGroup_id())
                    .select("site_ids"));
            if (null != siteGroup && StringUtils.isNotBlank(siteGroup.getSiteIds())) {
                String[] gIds = siteGroup.getSiteIds().split(",");
                for (String gid : gIds) {
                    if (StringUtils.isNotBlank(gid)) {
                        inSiteIdList.add(Integer.parseInt(gid));
                    }
                }
            }
        }
        // logger.info("---节点分组---");
        // areaID-->groupList-->
        if (StringUtils.isNotBlank(form.getAreaId())) {
            Integer areaId = Integer.valueOf(form.getAreaId());
            serialNumberList.add("0");
            List<CdnClientGroupEntity> groupEntityList = cdnClientGroupDao
                    .selectList(new QueryWrapper<CdnClientGroupEntity>()
                            .eq("area_id", areaId)
                            .select("id"));
            if (!groupEntityList.isEmpty()) {
                List<Integer> groupIdLs = groupEntityList.stream().map(o -> o.getId()).collect(Collectors.toList());
                List<CdnProductEntity> productEntityList = cdnProductDao.selectList(new QueryWrapper<CdnProductEntity>()
                        .in("server_group_ids", groupIdLs)
                        .select("id"));
                if (!productEntityList.isEmpty()) {
                    List<Integer> productIds = productEntityList.stream().map(o -> o.getId())
                            .collect(Collectors.toList());
                    List<TbOrderEntity> orderEntityList = tbOrderDao.selectList(new QueryWrapper<TbOrderEntity>()
                            .in("target_id", productIds)
                            .select("serial_number"));
                    if (!orderEntityList.isEmpty()) {
                        for (TbOrderEntity tbOrderEntity : orderEntityList) {
                            StaticVariableUtils.serialNumberGroupsMap.put(tbOrderEntity.getSerialNumber(),
                                    areaId.toString());
                            serialNumberList.add(tbOrderEntity.getSerialNumber());
                        }
                        // serialNumberList=orderEntityList.stream().map(o->o.getSerialNumber()).collect(Collectors.toList());
                    }
                }
            }
        }

        if (null != form.getIsAvailable()) {
            List<CdnSuitEntity> asList = cdnSuitDao.getAllAvailableSuit();
            if (1 == form.getIsAvailable()) {
                // 套餐有效的数据
                if (null == asList || 0 == asList.size()) {
                    serialNumberList.add("0");
                } else {
                    if (serialNumberList.isEmpty()) {
                        serialNumberList
                                .addAll(asList.stream().map(o -> o.getSerialNumber()).collect(Collectors.toList()));
                    } else {
                        // 创建一个新的集合，用于存储共有的元素
                        Set<String> commonSet = new HashSet<>(serialNumberList);
                        commonSet.retainAll(asList.stream().map(o -> o.getSerialNumber()).collect(Collectors.toList()));
                        // 将共有的元素转换回列表
                        serialNumberList = new ArrayList<>(commonSet);
                    }
                }
            } else {
                if (null == asList || 0 == asList.size()) {
                    unInSerialNumberList.add("0");
                } else {
                    unInSerialNumberList
                            .addAll(asList.stream().map(o -> o.getSerialNumber()).collect(Collectors.toList()));
                }

            }
        }

        // System.out.println(String.join(",",serialNumberList));
        // System.out.println(String.join(",",unInSerialNumberList));
        // logger.info("---159---");
        IPage<TbSiteEntity> page = tbSiteDao.selectPage(
                new Page<>(form.getPage(), form.getLimit()),
                new QueryWrapper<TbSiteEntity>()
                        .orderByDesc("id")
                        .in(!statusList.isEmpty(), "status", statusList)
                        .in(StringUtils.isNotBlank(userIds), "user_id", userIds.split(","))
                        .in(inSiteIdList.size() > 0, "id", inSiteIdList)
                        .in(null != serialNumberList && serialNumberList.size() > 0, "serial_number", serialNumberList)
                        .notIn(unInSerialNumberList.size() > 0, "serial_number", unInSerialNumberList));

        page.getRecords().forEach(item -> {
            if (0 == form.getSimpleFlag()) {
                item.setUser(this.getSiteUserName(item));
                if (StringUtils.isNotBlank(item.getSuitInfo())) {
                    JSONObject srcSuitObj = DataTypeConversionUtil.string2Json(item.getSuitInfo());
                    if (null != srcSuitObj && srcSuitObj.containsKey("id")) {
                        CdnSuitVoMini svo = DataTypeConversionUtil.json2entity(srcSuitObj, CdnSuitVoMini.class);
                        item.setSuit(svo);
                    }
                }
                if (null == item.getSuit() || StringUtils.isBlank(item.getCname())) {
                    // 自动更新数据到DB
                    CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, item.getSerialNumber(), false,
                            true);
                    if (null != suitObj) {
                        CdnSuitVoMini svo = DataTypeConversionUtil
                                .json2entity(DataTypeConversionUtil.entity2json(suitObj), CdnSuitVoMini.class);
                        if (null != svo) {
                            item.setSuit(svo);
                            if (null != suitObj.getCname()) {
                                String cname = suitObj.getCname().toString().replace("*.", "");
                                String serverCname = IPUtils.domain2domainStr(item.getMainServerName());
                                item.setCname(serverCname + "." + cname);
                                tbSiteDao.update(null, new UpdateWrapper<TbSiteEntity>()
                                        .eq("id", item.getId())
                                        .set("cname", item.getCname())
                                        .set("suit_info", DataTypeConversionUtil.entity2jonsStr(item.getSuit())));
                            }
                        }

                    }
                }
                item.setSuitInfo(null);
                item.setSsl(this.get_ssl_status(item.getId()));
                item.setJob_ssl_apply(this.getSiteAcmeJobStatus(item.getId()));
                item.setJob_check_site_cname(this.getSiteCheckCnameJobStatus(item.getId()));
            } else {
                item.setSuit(null);
                item.setSuitInfo(null);
            }
            // show group info
            if (StaticVariableUtils.siteIdGroupNameMap.containsKey(item.getId().toString())) {
                SiteGroupMiniVo vo = StaticVariableUtils.siteIdGroupNameMap.get(item.getId().toString());
                String[] gIds = vo.getIds().split(",");
                String[] gNames = vo.getNames().split(",");
                vo = new SiteGroupMiniVo();
                vo.setIds(String.join(",", gIds));
                vo.setNames(String.join(",", gNames));
                item.setGroupVo(vo);
            }
        });
        // logger.info("---185---");
        return new PageUtils(page);
    }

    @Override
    public R getAllSiteList(Long userId, Integer igg) {

        List<String> notInSiteIds = new ArrayList<>();
        notInSiteIds.add("0");

        if (null == igg || 0 == igg) {
            // 去除已经分配的
            List<TbSiteGroupEntity> groupsList = tbSiteGroupDao.selectList(new QueryWrapper<TbSiteGroupEntity>()
                    .eq(null != userId, "user_id", userId)
                    .select("site_ids"));
            if (!groupsList.isEmpty()) {
                for (TbSiteGroupEntity groupEntity : groupsList) {
                    if (StringUtils.isNotBlank(groupEntity.getSiteIds())) {
                        notInSiteIds.addAll(Arrays.asList(groupEntity.getSiteIds().split(",")));
                    }
                }
            }
        }

        List<TbSiteEntity> list = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId)
                .notIn("id", notInSiteIds)
                .select("id,main_server_name")

        );
        if (list.isEmpty()) {
            return R.ok().put("data", "");
        }
        Map<String, String> res = new HashMap<>(list.size());
        for (TbSiteEntity site : list) {
            res.put(site.getId().toString(), site.getMainServerName());
        }
        return R.ok().put("data", res);
    }

    @Override
    public R changeSiteStatus(Long userId, Integer siteId, Integer status) {
        String[] postpaidMode = { "2", "3" };
        TbSiteEntity siteEntity = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id", siteId)
                .eq(null != userId, "user_id", userId).last("limit 1"));
        if (null == siteEntity) {
            return R.error("无此站点");
        }
        if (CdnSiteStatusEnum.CLOSE.getId() == status) {
            /*
             * siteEntity.setStatus(status);
             * String
             * pathKey=PushSetEnum.SITE_CONF.getTemplatePath().replace("###site_id_name###",
             * siteEntity.getId()+"_"+siteEntity.getMainServerName()+"_");
             * String cmd="rm -rf "+pathKey;
             * String xid=redisUtils.streamXAdd(RedisStreamType.STREAM_HEAD.getName()+
             * RedisStreamType.COMMAND.getName(),RedisStreamType.STREAM_NORMAL_KEY.getName()
             * ,cmd);
             * if (StringUtils.isNotBlank(xid) && redisUtils.delete(pathKey)){
             * tbSiteDao.updateById(siteEntity);
             * HashMap pushMap=new HashMap<>(8);
             * pushMap.put(PushTypeEnum.COMMAND.getName(),CommandEnum.RELOAD_NGINX.getId().
             * toString());
             * cdnMakeFileService.pushByInputInfo(pushMap);
             * return R.ok();
             * }
             */
            return cdnMakeFileService.deleteSite(siteId, DelConfMode.CLOSE);
        } else if (CdnSiteStatusEnum.CLOSE_AND_LOCKED.getId() == status) {
            return cdnMakeFileService.deleteSite(siteId, DelConfMode.LOCK);
        } else if (CdnSiteStatusEnum.NORMAL.getId() == status) {
            // 开启
            if (null != userId) {
                // 用户态检测
                // 检测套餐 1是否存在 2是否超出流量
                CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(userId, siteEntity.getSerialNumber(), false,
                        true);
                if (null == suitObj) {
                    return R.error("开启失败,套餐数据有误！");
                }
                if (new Date().after(suitObj.getEndTime())) {
                    return R.error("开启失败,套餐过期！");
                }
                if (null == suitObj.getAttr()) {
                    return R.error("开启失败,获取当前套餐权限失败[0]！");
                }
                if (CdnSiteStatusEnum.CLOSE_AND_LOCKED.getId() == siteEntity.getStatus()) {
                    return R.error("开启失败,站点已被锁定！");
                }
                // ProductAttrVo
                // suitAttrVO=DataTypeConversionUtil.json2entity(suitObj.getAttr(),ProductAttrVo.class)
                // ;
                ProductAttrVo suitAttrVO = suitObj.getAttr();
                if (null != suitAttrVO && null != suitObj.getConsume()) {
                    // ProductAttrVo
                    // usedAttrVo=DataTypeConversionUtil.json2entity(suitObj.getConsume(),ProductAttrVo.class);
                    ProductAttrVo usedAttrVo = suitObj.getConsume();
                    if (null != usedAttrVo) {
                        if (!Arrays.asList(postpaidMode).contains(suitAttrVO.getCharging_mode())) {
                            if (suitAttrVO.getFlow() < usedAttrVo.getFlow()) {
                                return R.error("开启失败,【流量】已超出！");
                            }
                        }
                    }

                }
            }
            siteEntity.setStatus(status);
            tbSiteDao.updateById(siteEntity);
            this.updateSiteChunk(PUSH_SAVE, siteEntity);
            return R.ok();
        }
        return R.error("未知状态类型:" + status);
    }

    // init alias
    private void initInsertAlias(Integer siteId, String alias) {
        Map<String, Object> saveAliasMap = new HashMap<>();
        saveAliasMap.put("siteId", siteId.toString());
        JSONArray array = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("id", 0);
        item.put("value", alias);
        array.add(item);
        saveAliasMap.put(SiteAttrEnum.ALIAS.getName(), array);
        // {"siteId":"3169","alias":[{"id":5624,"value":"ytest3.com"}]}
        this.saveSiteAttr(saveAliasMap);
    }

    /**
     *
     * @param p  监听协议
     * @param l  监听协议端口
     * @param sp 回源协议
     * @param si 回源地址
     * @param sl 回源监听端口
     * @return
     */
    private String getDefaultSourceStr(String p, Integer l, String sp, String si, String sl) {
        NgxSourceBaseInfoVo defaultVo = new NgxSourceBaseInfoVo();
        defaultVo.setProtocol(p);
        defaultVo.setPort(l);
        defaultVo.setS_protocol(sp);
        List<NgxSourceBaseInfoVo.LineVo> lineList = new ArrayList<>(2);
        NgxSourceBaseInfoVo.LineVo lineVo = defaultVo.new LineVo();
        lineVo.setIp(si);
        lineVo.setPort(sl);
        lineList.add(lineVo);
        defaultVo.setLine(lineList);
        return DataTypeConversionUtil.entity2jonsStr(defaultVo);
    }

    // init 默认回源IP
    private void initDefaultSource(Integer siteId, List<String> mainSource, List<String> backSource, String sProtocol) {
        if (null == mainSource) {
            return;
        }
        JSONObject schemePortsObj = new JSONObject(4);
        schemePortsObj.put("http", 80);
        schemePortsObj.put("https", 443);
        for (String s : schemePortsObj.keySet()) {
            TbSiteMutAttrEntity httpAttr = new TbSiteMutAttrEntity();
            httpAttr.setSiteId(siteId);
            httpAttr.setPkey(SiteAttrEnum.SOURCE_BASE_INFO.getName());
            // {"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"1.1.1.1","domain":"","port":"80","line":1,"weight":1}]}
            NgxSourceBaseInfoVo vo = new NgxSourceBaseInfoVo();
            vo.setProtocol(s);
            vo.setPort(schemePortsObj.getInteger(s));
            vo.setS_protocol(sProtocol);
            vo.setUpstream("polling");
            List<NgxSourceBaseInfoVo.LineVo> lines = new ArrayList<>();
            for (String ms : mainSource) {
                NgxSourceBaseInfoVo.LineVo lineVo = vo.new LineVo();
                // "1.1.1.1|80|1" "1.14.1.1|80|1"
                String[] split = ms.split("\\|");
                String sourceHost = split[0].trim();
                String sourcePort = "";
                if (2 == split.length || 3 == split.length) {
                    sourcePort = split[1];
                }
                if (IPUtils.isValidIPV4(sourceHost)) {
                    vo.setSource_set("ip");
                    if (IPUtils.isValidIpv4OrIpv6(sourceHost)) {
                        lineVo.setIp(sourceHost);
                    } else {
                        logger.error("sourceHost:" + sourceHost + "is not valid");
                        continue;
                    }
                } else {
                    vo.setSource_set("domain");
                    String sDomain = "";
                    if (sourceHost.startsWith("http://")) {
                        lineVo.setS_protocol("http");
                        sDomain = sourceHost.substring("http://".length());
                    } else if (sourceHost.startsWith("https://")) {
                        lineVo.setS_protocol("https");
                        sDomain = sourceHost.substring("https://".length());
                    } else {
                        sDomain = sourceHost;
                    }
                    if (StringUtils.isNotBlank(sDomain) && DomainUtils.isNormalDomain(sDomain)) {
                        lineVo.setDomain(sDomain);
                    } else {
                        logger.error("sDomain:" + sDomain + "is not valid");
                        continue;
                    }
                }
                if (StringUtils.isNotBlank(sourcePort)) {
                    String[] pss = sourcePort.split(",");
                    if ("http".equals(vo.getS_protocol()) || "https".equals(vo.getS_protocol())) {
                        String sPort = pss[0];
                        if (IPUtils.isValidPort(sPort)) {
                            lineVo.setPort(sPort);
                        } else {
                            logger.error("sPort:" + sPort + "is not valid");
                            continue;
                        }
                    } else if ("$scheme".equals(vo.getS_protocol())) {
                        String tPort = "";
                        if (2 == pss.length) {
                            tPort = s.equals("http") ? pss[0] : pss[1];
                        } else {
                            tPort = s.equals("http") ? "80" : "443";
                        }
                        if (StringUtils.isNotBlank(tPort) && IPUtils.isValidPort(tPort)) {
                            lineVo.setPort(tPort);
                        } else {
                            logger.error("tPort:" + tPort + "is not valid");
                            continue;
                        }
                    } else {
                        logger.error("Unknown type of source port");
                        continue;
                    }
                } else {
                    if ("http".equals(vo.getS_protocol())) {
                        lineVo.setPort("80");
                    } else if ("https".equals(vo.getS_protocol())) {
                        lineVo.setPort("443");
                    } else if ("$scheme".equals(vo.getS_protocol())) {
                        String linePort = s.equals("http") ? "80" : "443";
                        lineVo.setPort(linePort);
                    } else {
                        logger.error("Unknown type of source port");
                        continue;
                    }
                }
                lineVo.setWeight(1);
                lines.add(lineVo);
            }
            if (null != backSource && !backSource.isEmpty()) {
                for (String bs : backSource) {
                    //// "1.1.1.1|80|1" "1.14.1.1|80|1"
                    String[] splitBs = bs.split("\\|");
                    String bsHost = splitBs[0];
                    String bsPort = "";
                    if (2 == splitBs.length || 3 == splitBs.length) {
                        bsPort = splitBs[1];
                    }
                    NgxSourceBaseInfoVo.LineVo lineVo = vo.new LineVo();
                    if (IPUtils.isValidIPV4(bsHost)) {
                        vo.setSource_set("ip");
                        lineVo.setIp(bsHost);
                    } else {
                        vo.setSource_set("domain");
                        String sDomain = "";
                        if (bsHost.startsWith("http://")) {
                            lineVo.setS_protocol("http");
                            sDomain = bsHost.substring("http://".length());
                        } else if (bsHost.startsWith("https://")) {
                            lineVo.setS_protocol("https");
                            sDomain = bsHost.substring("https://".length());
                        } else {
                            sDomain = bsHost;
                        }
                        if (StringUtils.isNotBlank(sDomain) && DomainUtils.isNormalDomain(sDomain)) {
                            lineVo.setDomain(sDomain);
                        } else {
                            logger.error("sDomain:" + sDomain + "is not valid");
                            continue;
                        }
                    }
                    if (StringUtils.isNotBlank(bsPort) && IPUtils.isValidPort(bsPort)) {
                        lineVo.setPort(bsPort);
                    } else {
                        if ("http".equals(sProtocol)) {
                            lineVo.setPort("80");
                        } else if ("https".equals(sProtocol)) {
                            lineVo.setPort("443");
                        } else if ("$scheme".equals(vo.getS_protocol())) {
                            String linePort = s.equals("http") ? "80" : "443";
                            lineVo.setPort(linePort);
                        } else {
                            lineVo.setPort("80");
                        }
                    }
                    lineVo.setWeight(1);
                    lineVo.setLine(2);
                    lines.add(lineVo);
                }
            }
            vo.setLine(lines);
            httpAttr.setPvalue(DataTypeConversionUtil.entity2jonsStr(vo));
            httpAttr.setStatus(1);
            httpAttr.setPType(SiteAttrEnum.getTypeByName(httpAttr.getPkey()));
            tbSiteMutAttrDao.insert(httpAttr);
        }

    }

    // 初始化站点 添加默认值
    private void InitSiteAttr(TbSiteEntity tbSite) {
        if (true) {
            // 添加日志级别,
            final SiteAttrEnum[] initDefaultItem = { SiteAttrEnum.SITE_ACCESS_LOG_MODE };
            for (SiteAttrEnum item : initDefaultItem) {
                TbSiteAttrEntity attr1 = new TbSiteAttrEntity();
                attr1.setSiteId(tbSite.getId());
                attr1.setPkey(item.getName());
                attr1.setPvalue(item.getDefaultValue());
                attr1.setPType(SiteAttrEnum.getTypeByName(item.getName()));
                attr1.setStatus(1);
                this.updateAdditionalAttr(attr1, null);
                tbSiteAttrDao.insert(attr1);
            }
        }

        if (true) {
            // 为站添加默认规则
            // PRI_PRECISE_WAF_SELECTS
            TbCdnPublicMutAttrEntity publicMutAttr = publicMutAttrDao.selectOne(
                    new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey", PublicEnum.WEB_RULE_PRECISE.getName())
                            .orderByDesc("weight").last("limit 1").select("id"));
            if (null != publicMutAttr) {
                TbSiteAttrEntity attr2 = new TbSiteAttrEntity();
                attr2.setSiteId(tbSite.getId());
                attr2.setPkey(SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName());
                attr2.setPvalue(publicMutAttr.getId().toString());
                attr2.setPType(SiteAttrEnum.getTypeByName(attr2.getPkey()));
                attr2.setStatus(1);
                this.updateAdditionalAttr(attr2, null);
                tbSiteAttrDao.insert(attr2);
            }
        }

        if (true) {
            // 为站点添加默认缓存
            TbSiteMutAttrEntity attr3 = new TbSiteMutAttrEntity();
            attr3.setSiteId(tbSite.getId());
            attr3.setPkey(SiteAttrEnum.PERFORMANCE_CACHE_TYPE.getName());
            attr3.setPvalue(
                    "{\"type\":1,\"content\":\"css|txt|iso|img|exe|zip|rar|7z|gz|tar|apk|ipa|dmg|manifest|conf|xml|cab|bin|msi|jpg|jpeg|gif|ico|png|bmp|webp|psd|tif|tiff|svg|svgz|mp3|flv|swf|wma|wav|mp4|mov|mpeg|rm|avi|wmv|mkv|vob|rmvb|asf|mpg|ogg|m3u8|ts|mid|midi|3gp|js\",\"time\":30,\"unit\":\"m\",\"mode\":\"cache\"}");
            attr3.setStatus(1);
            attr3.setPType(SiteAttrEnum.getTypeByName(attr3.getPkey()));
            this.updateAdditionalAttr(null, attr3);
            tbSiteMutAttrDao.insert(attr3);
        }

        if (true) {
            // 为站点添加默认错误页
            NgxErrPageAttrVo vo = new NgxErrPageAttrVo();
            vo.setType(QuerySysAuth.NGX_DEFAULT_ERR_PAGE);
            List<String> allErrPageKeys = SiteAttrEnum.getAllErrorPage();
            if (null != allErrPageKeys) {
                for (String key : allErrPageKeys) {
                    TbSiteMutAttrEntity attr = new TbSiteMutAttrEntity();
                    attr.setSiteId(tbSite.getId());
                    attr.setPkey(key);
                    attr.setPvalue(DataTypeConversionUtil.entity2jonsStr(vo));
                    attr.setPType(SiteAttrEnum.getTypeByName(attr.getPkey()));
                    attr.setStatus(1);
                    this.updateAdditionalAttr(null, attr);
                    tbSiteMutAttrDao.insert(attr);
                }
            }
        }

    }

    /**
     * @param userId
     * @param serialNumber
     * @param mainServerName
     * @param alias
     * @param
     * @param
     * @param sProtocol
     * @param throwError
     * @return
     */
    private SaveSiteResultVo saveSite(Long userId, String serialNumber, String mainServerName, String alias,
            List<String> mainSourceArray, List<String> backSourceArray, String sProtocol, boolean throwError) {
        SaveSiteResultVo resultVo = new SaveSiteResultVo();
        String eMsg = "";
        resultVo.setCode(0);
        String fMainServerName = mainServerName.toLowerCase(Locale.ROOT);
        TbUserEntity user = tbUserDao.selectById(userId);
        if (null == user) {
            if (throwError) {
                throw new RRException("无此用户");
            }
            resultVo.setMsg("无此用户");
            return resultVo;
        }
        CdnSuitEntity suitEntity = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("user_id", userId)
                .eq("serial_number", serialNumber)
                .last("limit 1"));
        if (null == suitEntity) {
            if (throwError) {
                throw new RRException("无此套餐");
            }
            resultVo.setMsg("无此套餐");
            return resultVo;
        }
        this.cleanSiteConfigFile(suitEntity.getId());
        if (!inputAvailableService.checkNginxServerNameAndAliasIsValid("site", 0, fMainServerName)) {
            if (throwError) {
                throw new RRException("添加失败，存在的域名！");
            }
            resultVo.setMsg("添加失败，存在的域名");
            return resultVo;
        }
        CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, serialNumber, false, true);
        if (null == suitObj) {
            if (throwError) {
                throw new RRException("套餐数据有误！");
            }
            resultVo.setMsg("套餐数据有误");
            return resultVo;
        }
        if (null == suitObj.getAttr()) {
            if (throwError) {
                throw new RRException("当前套餐无创建站点权限[0]！");
            }
            resultVo.setMsg("当前套餐无创建站点权限[0]！");
            return resultVo;
        }
        // 套餐中可建站点数量
        // ProductAttrVo
        // maxVo=DataTypeConversionUtil.json2entity(suitObj.getAttr(),ProductAttrVo.class)
        // ;
        ProductAttrVo maxVo = suitObj.getAttr();
        if (null == maxVo || null == maxVo.getSite() || 0 == maxVo.getSite()) {
            if (throwError) {
                throw new RRException("当前套餐无创建站点权限[1]！");
            }
            resultVo.setMsg("当前套餐无创建站点权限[1]！");
            return resultVo;
        }
        // ProductAttrVo
        // useVo=DataTypeConversionUtil.json2entity(suitObj.getConsume(),ProductAttrVo.class)
        // ;
        ProductAttrVo useVo = suitObj.getConsume();
        if (null == useVo) {
            eMsg = "创建失败！获取使用量失败【" + maxVo.getSite() + "】";
            if (throwError) {
                throw new RRException(eMsg);
            }
            resultVo.setMsg(eMsg);
            return resultVo;
        }
        if (useVo.getSite() >= maxVo.getSite()) {
            eMsg = "创建失败！当前套餐创建站点数量已超出限制【" + maxVo.getSite() + "】";
            if (throwError) {
                throw new RRException(eMsg);
            }
            resultVo.setMsg(eMsg);
            return resultVo;
        }

        TbSiteEntity tbSite = new TbSiteEntity();
        tbSite.setUserId(userId);
        tbSite.setSerialNumber(serialNumber);
        tbSite.setMainServerName(fMainServerName);
        tbSite.setStatus(1);
        int insertCount = tbSiteDao.insert(tbSite);
        // 为站点增加默认属性
        if (insertCount > 0) {
            // insert alias
            if (StringUtils.isNotBlank(alias)) {
                for (String a : alias.split(",")) {
                    if (StringUtils.isNotBlank(a)) {
                        this.initInsertAlias(tbSite.getId(), a);
                    }
                }
            }
            // insert sourceIp
            this.initDefaultSource(tbSite.getId(), mainSourceArray, backSourceArray, sProtocol);
            // 添加默认值
            this.InitSiteAttr(tbSite);
            this.updateSiteChunk(PUSH_SAVE, tbSite);
        }
        resultVo.setCode(1);
        resultVo.setSiteEntity(tbSite);
        return resultVo;
    }

    @Override
    public R singleCreateSite(CreateSingleCreateSiteForm form) {
        ValidatorUtils.validateEntity(form);
        String mainServerName = form.getDomains().get(0);
        String alias = null;
        if (form.getDomains().size() > 1) {
            List<String> aliasList = form.getDomains().subList(1, form.getDomains().size());
            alias = StringUtils.join(aliasList, ",");
        }
        SaveSiteResultVo vo = this.saveSite(form.getUserId(), form.getSerialNumber(), mainServerName, alias,
                form.getServerSource(), form.getServerSourceBackup(), form.getSProtocol(), true);
        if (1 == vo.getCode() && null != vo.getSiteEntity()) {
            insertSiteIdToSiteGroup(form.getGroupId(), vo.getSiteEntity().getId());
            return R.ok().put("data", vo.getSiteEntity());
        }
        return R.error(vo.getMsg());
    }

    @Override
    public TbSiteEntity createSite(Long userId, String serialNumber, String main_server_name, String alias,
            String sourceHost, String port, String sProtocol) {
        List<String> mainSourceArray = new ArrayList();
        String ms = String.format("%s|%s|1", sourceHost, port);
        mainSourceArray.add(ms);
        SaveSiteResultVo vo = this.saveSite(userId, serialNumber, main_server_name, alias, mainSourceArray, null,
                sProtocol, true);
        if (1 == vo.getCode() && null != vo.getSiteEntity()) {
            return vo.getSiteEntity();
        }
        return null;
    }

    @Override
    public R batCreateSite(Long userId, String serialNumber, String mainServerNames) {
        String[] servers = mainServerNames.split(",");
        String eMsg = "";
        int success = 0;
        int total = 0;
        if (1 == servers.length) {
            this.saveSite(userId, serialNumber, mainServerNames, null, null, null, null, true);
            total = 1;
            success += 1;
            return R.ok().put("total", total).put("success", success);
        } else {
            if (StringUtils.isNotBlank(mainServerNames)) {
                for (String main_server_name : mainServerNames.split(",")) {
                    SaveSiteResultVo vo = this.saveSite(userId, serialNumber, main_server_name, null, null, null, null,
                            false);
                    if (1 == vo.getCode() && null != vo.getSiteEntity()) {
                        success++;
                    } else if (StringUtils.isNotBlank(vo.getMsg())) {
                        eMsg += main_server_name + ":" + vo.getMsg() + "\n";
                    }
                    total++;
                }
            }

        }
        return R.ok().put("total", total).put("success", success).put("err", eMsg);
    }

    private R insertSiteIdToSiteGroup(int groupId, Integer siteId) {
        if (0 == groupId || null == siteId || 0 == siteId) {
            return R.ok();
        }
        TbSiteGroupEntity group = tbSiteGroupDao.selectById(groupId);
        if (null == group) {
            return R.ok();
        }
        String ids = group.getSiteIds();
        if (StringUtils.isNotBlank(ids)) {
            ids += "," + siteId;
        } else {
            ids = siteId + "";
        }
        group.setSiteIds(ids);
        tbSiteGroupDao.updateById(group);
        cdnMakeFileService.deleteCacheByKey(CacheKeyEnums.cert_apply_proxy_pass.getKeyName());
        return R.ok();
    }

    @Override
    public R batchCreateStdSite(Long userId, BatchCreateSite batchCreateSiteParam) {
        String eMsg = "";
        int success = 0;
        int total = 0;
        for (String infos : batchCreateSiteParam.getServerSource()) {
            total++;
            // domain.com,alias1.com,alias2.com|1.1.1.1
            // domain.com,alias1.com,alias2.com|1.1.1.1|88
            // domain.com,alias1.com,alias2.com|1.1.1.1|88|https
            String[] sas = infos.split("\\|");
            if (2 == sas.length) {
                String maStr = sas[0];
                String[] ma = maStr.split(",");
                String mainServerName = ma[0];
                String alias = maStr.substring(mainServerName.length());
                String sourceHost = sas[1];
                List<String> mainSourceArray = new ArrayList();
                mainSourceArray.add(sourceHost);
                SaveSiteResultVo vo = this.saveSite(userId, batchCreateSiteParam.getSerialNumber(), mainServerName,
                        alias, mainSourceArray, null, batchCreateSiteParam.getSProtocol(), false);
                if (1 == vo.getCode() && null != vo.getSiteEntity()) {
                    insertSiteIdToSiteGroup(batchCreateSiteParam.getGroupId(), vo.getSiteEntity().getId());
                    success++;
                } else {
                    eMsg += mainServerName + ":" + vo.getMsg() + "\n";
                }
            } else if (3 == sas.length) {
                String maStr = sas[0];
                String[] ma = maStr.split(",");
                String mainServerName = ma[0];
                String alias = maStr.substring(mainServerName.length());
                String sourceHost = sas[1];
                String sourcePort = sas[2];
                List<String> mainSourceArray = new ArrayList();
                mainSourceArray.add(sourceHost + "|" + sourcePort);
                SaveSiteResultVo vo = this.saveSite(userId, batchCreateSiteParam.getSerialNumber(), mainServerName,
                        alias, mainSourceArray, null, batchCreateSiteParam.getSProtocol(), false);
                if (1 == vo.getCode() && null != vo.getSiteEntity()) {
                    insertSiteIdToSiteGroup(batchCreateSiteParam.getGroupId(), vo.getSiteEntity().getId());
                    success++;
                } else {
                    eMsg += mainServerName + ":" + vo.getMsg() + "\n";
                }
            } else if (4 == sas.length) {
                String maStr = sas[0];
                String[] ma = maStr.split(",");
                String mainServerName = ma[0];
                String alias = maStr.substring(mainServerName.length());
                String sourceHost = sas[1];
                String sourcePort = sas[2];
                String sProtocol = sas[3];
                List<String> mainSourceArray = new ArrayList();
                mainSourceArray.add(sourceHost + "|" + sourcePort + "|1|" + sProtocol);
                SaveSiteResultVo vo = this.saveSite(userId, batchCreateSiteParam.getSerialNumber(), mainServerName,
                        alias, mainSourceArray, null, sProtocol, false);
                if (1 == vo.getCode() && null != vo.getSiteEntity()) {
                    insertSiteIdToSiteGroup(batchCreateSiteParam.getGroupId(), vo.getSiteEntity().getId());
                    success++;
                } else {
                    eMsg += mainServerName + ":" + vo.getMsg() + "\n";
                }
            }
        }
        return R.ok().put("total", total).put("success", success).put("err", eMsg);
    }

    @Override
    public TbSiteEntity changeSiteSerialNumber(Long userId, Integer siteId, String serialNumber) {
        TbSiteEntity siteEntity = tbSiteDao.selectById(siteId);
        if (null == siteEntity) {
            throw new RRException("[siteId]错误1");
        }
        if (null != userId && !siteEntity.getUserId().equals(userId)) {
            throw new RRException("[siteId]错误2");
        }
        CdnSuitEntity suitEntity = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("user_id", siteEntity.getUserId()).eq("serial_number", serialNumber).last("limit 1"));
        if (null == suitEntity) {
            throw new RRException("[serialNumber]错误1");
        }
        R r1 = cdnMakeFileService.deleteSite(siteId, DelConfMode.CLOSE);
        logger.info("delete_site:" + r1.toJsonString());
        //
        this.cleanSiteConfigFile(suitEntity.getId());
        if (true) {
            String pathKey = PushSetEnum.SITE_CONF.getTemplatePath().replace("###site_id_name###",
                    siteEntity.getId() + "_" + siteEntity.getMainServerName() + "_");
            String cmd = "rm -rf " + pathKey;
            redisUtils.streamXAdd(RedisStreamType.STREAM_HEAD.getName() + RedisStreamType.COMMAND.getName(),
                    RedisStreamType.STREAM_NORMAL_KEY.getName(), cmd);
        }
        siteEntity.setSerialNumber(suitEntity.getSerialNumber());
        tbSiteDao.updateById(siteEntity);

        // --更新套餐信息到SITE-SUIT_INFO
        CdnSuitEntity src_suitObj = suitService.getSuitDetailBySerial(null, siteEntity.getSerialNumber(), false, true);
        if (null != src_suitObj) {
            String cname = src_suitObj.getCname().toString().replace("*.", "");
            String serverCname = IPUtils.domain2domainStr(siteEntity.getMainServerName());
            tbSiteDao.update(null, new UpdateWrapper<TbSiteEntity>()
                    .eq("id", siteId)
                    .set("cname", serverCname + "." + cname)
                    .set("suit_info", DataTypeConversionUtil.entity2jonsStr(src_suitObj)));
        }

        // 更新baseInfo areaId
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("site_id", String.valueOf(siteId));
        params.put("siteId", String.valueOf(siteId));
        params.put(SiteAttrEnum.Label_3.getName(), String.valueOf(siteId));
        R r2 = this.saveSiteAttr(params);
        logger.info("save_change_suit:" + r2.toJsonString());

        // ----
        return siteEntity;
    }

    private String getSiteUserName(TbSiteEntity site) {
        TbUserEntity user = tbUserDao.selectById(site.getUserId());
        if (null != user) {
            if (StringUtils.isNotBlank(user.getUsername())) {
                return user.getUsername();
            } else if (StringUtils.isNotBlank(user.getMobile())) {
                return user.getMobile();
            } else if (StringUtils.isNotBlank(user.getMail())) {
                return user.getMail();
            } else {
                return user.getUserId() + "";
            }
        }
        return "";
    }

    /**
     * 暂无使用
     */
    @Override
    public TbSiteEntity querySiteAllDetailInfo(Long userId, Integer siteId) {
        TbSiteEntity siteEntity = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId).eq("id", siteId).last("limit 1"));
        if (null == siteEntity) {
            return null;
        }

        siteEntity.setUser(this.getSiteUserName(siteEntity));

        // 站点套餐
        siteEntity.setSuit(suitService.getSuitDetailBySerial(null, siteEntity.getSerialNumber(), true, true));

        // 站点attr list
        List<Map<String, Object>> list1 = new ArrayList<>();
        List<Map<String, Object>> list2 = tbSiteAttrDao.selectMaps(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id", siteId)
                .eq("status", 1));
        List<Map<String, Object>> list3 = tbSiteMutAttrDao.selectMaps(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id", siteId)
                .eq("status", 1));
        if (list2.size() > 0) {
            list1.addAll(list2);
        }
        if (list3.size() > 0) {
            list1.addAll(list3);
        }
        siteEntity.setAttr(list1);
        return siteEntity;
    }

    private List<Object> getAttrListByAttrGroup(String group, Integer siteId) {
        List<Object> resultList = new ArrayList<>();
        if (StringUtils.isBlank(group) || null == siteId) {
            return resultList;
        }
        for (SiteAttrEnum item : SiteAttrEnum.values()) {
            if (item.getGroup().equals(group)) {
                List<Map<String, Object>> list1 = tbSiteAttrDao.selectMaps(new QueryWrapper<TbSiteAttrEntity>()
                        .eq("site_id", siteId)
                        .orderByDesc("weight")
                        .eq("pkey", item.getName()));
                List<Map<String, Object>> list2 = tbSiteMutAttrDao.selectMaps(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("site_id", siteId)
                        .orderByDesc("weight")
                        .eq("pkey", item.getName()));
                boolean foundKey = false;
                if (list1.size() > 0) {
                    foundKey = true;
                    resultList.addAll(list1);
                }
                if (list2.size() > 0) {
                    foundKey = true;
                    resultList.addAll(list2);
                }
                if (!foundKey && StringUtils.isNotBlank(item.getDefaultValue())) {
                    Map<String, Object> defValue = new HashMap<>();
                    defValue.put("pkey", item.getName());
                    if ("on".equalsIgnoreCase(item.getDefaultValue())) {
                        defValue.put("pvalue", "1");
                    } else if ("off".equalsIgnoreCase(item.getDefaultValue())) {
                        defValue.put("pvalue", "0");
                    } else {
                        defValue.put("pvalue", item.getDefaultValue());
                    }
                    defValue.put("site_id", siteId);
                    resultList.add(defValue);
                }
            }
        }

        return resultList;
    }

    /**
     * 获取站点详情
     * 
     * @param userId
     * @param siteId
     * @param groups
     * @param key
     * @return
     */
    @Override
    public TbSiteEntity querySiteAttr(Long userId, Integer siteId, String groups, String key) {
        TbSiteEntity siteEntity = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId)
                .eq("id", siteId)
                .last("limit 1"));
        if (null == siteEntity) {
            return null;
        }
        if (StringUtils.isNotBlank(groups)) {
            String[] group_list = groups.split(",");
            List<Object> final_all_group_list = new ArrayList<>();
            for (String group : group_list) {
                switch (group) {
                    case "base":
                    case "suit":
                        if (true) {
                            final_all_group_list.addAll(this.getAttrListByAttrGroup(group, siteId));
                            CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null,
                                    siteEntity.getSerialNumber(), false, true);
                            if (null != suitObj) {
                                siteEntity.setSuit(suitObj);
                                if (null != suitObj.getCname()) {
                                    String cname = suitObj.getCname().toString().replace("*.", "");
                                    String serverCname = IPUtils.domain2domainStr(siteEntity.getMainServerName());
                                    siteEntity.setCname(serverCname + "." + cname);
                                }
                                tbSiteDao.update(null, new UpdateWrapper<TbSiteEntity>()
                                        .eq("id", siteId)
                                        .set("cname", siteEntity.getCname())
                                        .set("suit_info", DataTypeConversionUtil.entity2jonsStr(suitObj)));

                            }
                        }
                        break;
                    case "injPenEnums":
                        final_all_group_list.addAll(NgxInjectioPenetrationEnum.getAllInfo());
                        break;
                    case "pri_precise_waf":
                    case "pub_precise_waf":
                        if (true) {
                            List<Map<String, Object>> listPub = publicMutAttrDao
                                    .selectMaps(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                                            .eq("pkey", "web_rule_precise").eq("status", 1).orderByDesc("weight"));
                            listPub.forEach(map -> {
                                map.put("child", null);
                                if (map.containsKey("id")) {
                                    String parentId = map.get("id").toString();
                                    List<TbCdnPublicMutAttrEntity> c_list = publicMutAttrDao.selectList(
                                            new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("parent_id", parentId));
                                    for (TbCdnPublicMutAttrEntity publicMutAttr : c_list) {
                                        String t_value = publicMutAttr.getId() + "#1";
                                        TbSiteAttrEntity attrEntity = tbSiteAttrDao
                                                .selectOne(new QueryWrapper<TbSiteAttrEntity>().eq("site_id", siteId)
                                                        .eq("pvalue", t_value).eq("pkey", "pub_precise_waf_selects")
                                                        .eq("status", 1).last("limit 1"));
                                        if (null != attrEntity) {
                                            publicMutAttr.setStatus(1);
                                        } else {
                                            publicMutAttr.setStatus(0);
                                        }
                                    }
                                    map.put("child", c_list);
                                }
                            });
                            List<TbSiteEntity> sameSuitSiteList = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                                    .eq("user_id", siteEntity.getUserId())
                                    .eq("serial_number", siteEntity.getSerialNumber()));
                            Iterator<TbSiteEntity> iterator = sameSuitSiteList.iterator();
                            while (iterator.hasNext()) {
                                TbSiteEntity siteBuffer = iterator.next();
                                List<TbSiteMutAttrEntity> listPri = tbSiteMutAttrDao
                                        .selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                                                .eq("site_id", siteBuffer.getId())
                                                .ne("site_id", siteId)
                                                .eq("status", 1)
                                                .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName()));
                                if (null == listPri || listPri.size() == 0) {
                                    iterator.remove();
                                }
                            }
                            Map<String, Object> f_map = new HashMap<>();
                            f_map.put("pub_precise_waf", listPub);
                            f_map.put("pri_precise_waf", sameSuitSiteList);
                            final_all_group_list.add(f_map);
                        }
                        break;
                    case "ssl":
                        if (true) {
                            TbSiteMutAttrEntity ssl_key = tbSiteMutAttrDao
                                    .selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                                            .eq("site_id", siteId)
                                            .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                                            .eq("status", 1)
                                            .last("limit 1"));
                            TbSiteMutAttrEntity ssl_pem = tbSiteMutAttrDao
                                    .selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                                            .eq("site_id", siteId)
                                            .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                                            .eq("status", 1)
                                            .last("limit 1"));
                            List<Object> list1 = new ArrayList<>();
                            if (null != ssl_key && null != ssl_pem) {
                                JSONObject keyObj = DataTypeConversionUtil.string2Json(ssl_key.getPvalue());
                                if (keyObj.containsKey("value")) {
                                    ssl_key.setPvalue(keyObj.getString("value"));
                                }
                                JSONObject pemObj = DataTypeConversionUtil.string2Json(ssl_pem.getPvalue());
                                if (pemObj.containsKey("value")) {
                                    Map res = HashUtils.readCerSubjectToMap(pemObj.getString("value"));
                                    list1.add(res);
                                    ssl_pem.setPvalue(pemObj.getString("value"));
                                }
                                list1.add(ssl_key);
                                list1.add(ssl_pem);
                            }
                            Map map = SiteAttrEnum.getAllByGroupName(group);
                            List<String> listMap = new ArrayList(map.keySet());
                            List<Map<String, Object>> list_other1 = tbSiteAttrDao
                                    .selectMaps(new QueryWrapper<TbSiteAttrEntity>()
                                            .eq("site_id", siteId)
                                            .in("pkey", listMap.toArray()));
                            List<Map<String, Object>> list_other2 = tbSiteMutAttrDao
                                    .selectMaps(new QueryWrapper<TbSiteMutAttrEntity>()
                                            .eq("site_id", siteId)
                                            .ne("pkey", SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                                            .ne("pkey", SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                                            .in("pkey", listMap.toArray()));
                            list1.addAll(list_other1);
                            list1.addAll(list_other2);
                            final_all_group_list.addAll(list1);
                        }
                        break;
                    case "custom_dns":
                        // 自定义DNS组 查看
                        if (true) {
                            // 获取站点的套餐
                            CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null,
                                    siteEntity.getSerialNumber(), false, true);
                            if (null == suitObj) {
                                break;
                            }
                            List<TbSiteMutAttrEntity> t_attr_ls = tbSiteMutAttrDao
                                    .selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                                            .eq("site_id", siteId)
                                            .eq("pkey", SiteAttrEnum.NETWORK_CUSTOM_DNS.getName())
                                            .eq("status", 1));
                            if (null == suitObj.getProductEntity()) {
                                break;
                            }
                            CdnProductEntity suit_product = (CdnProductEntity) suitObj.getProductEntity();
                            if (null == suit_product.getClient_group_list()) {
                                break;
                            }
                            // 从套餐中获取DNS_记录信息 查看
                            Map view_data_map = new HashMap();
                            Map view_line_data_map = new HashMap();
                            List<CdnClientGroupEntity> groupList = (List<CdnClientGroupEntity>) suit_product
                                    .getClient_group_list();
                            List<String> f_ips = new ArrayList<>();
                            Map keyLsMap = new HashMap();
                            for (CdnClientGroupEntity cg : groupList) {
                                Integer d_id = cg.getDnsConfigId();
                                String d_hash = cg.getHash();
                                String d_key = String.format("%d_%s", d_id, d_hash);
                                if (!keyLsMap.containsKey(d_key)) {
                                    List<CdnClientGroupChildConfEntity> ls_client_dns_group = cdnClientGroupChildConfDao
                                            .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                                                    .eq("group_id", cg.getId()).eq("parent_id", 0).eq("status", 1)
                                                    .select("client_id"));
                                    for (CdnClientGroupChildConfEntity cdde : ls_client_dns_group) {
                                        CdnClientEntity client = cdnClientDao.selectById(cdde.getClientId());
                                        if (null != client && 1 == client.getClientType() && 1 == client.getStatus()) {
                                            if (StringUtils.isNotBlank(client.getClientIp())) {
                                                if (!f_ips.contains(client.getClientIp())) {
                                                    f_ips.add(client.getClientIp());
                                                }
                                            }
                                        }
                                    }
                                    keyLsMap.put(d_key, ls_client_dns_group);
                                    String cMainName = siteEntity.getMainServerName().replace(".", "-");
                                    String dns_top = String.format("%s.%s", cMainName, d_hash);
                                    List<DnsRecordItemVo> d_r_ls = dnsCApiService.getRecordByInfoToList(d_id, dns_top,
                                            null, null);
                                    if (null != d_r_ls && d_r_ls.size() > 0) {
                                        view_data_map.put(d_key, d_r_ls);
                                    }
                                    R lineR = dnsCApiService.getDnsLineByConfigId(d_id, 0);
                                    if (1 == lineR.getCode() && lineR.containsKey("data")) {
                                        view_line_data_map.put(d_key, lineR.get("data"));
                                    }

                                }
                            }
                            Map f_v_map = new HashMap();
                            f_v_map.put("dns_infos", view_data_map);
                            String min_server = siteEntity.getMainServerName().replace(".", "-");
                            f_v_map.put("cname", suitObj.getCname().toString().replace("*", min_server));
                            f_v_map.put("dns_lines", view_line_data_map);
                            f_v_map.put("all_ips", f_ips);
                            f_v_map.put("buf", keyLsMap);
                            f_v_map.put("data", t_attr_ls);
                            final_all_group_list.add(f_v_map);
                        }
                        break;
                    case "waf":
                        if (true) {
                            // waf 组内除 PRI_PRECISE_WAF_DETAILS 的数据
                            List<Object> resultList = new ArrayList<>();
                            Map groupKeyMaps = SiteAttrEnum.getAllByGroupName(group);
                            List<String> listMap = new ArrayList(groupKeyMaps.keySet());
                            if (listMap.size() > 0) {
                                for (Object wafKey : groupKeyMaps.keySet()) {
                                    List<Map<String, Object>> list1 = tbSiteAttrDao
                                            .selectMaps(new QueryWrapper<TbSiteAttrEntity>()
                                                    .eq("site_id", siteId)
                                                    .orderByDesc("weight")
                                                    .eq("pkey", wafKey));
                                    List<Map<String, Object>> list2 = tbSiteMutAttrDao
                                            .selectMaps(new QueryWrapper<TbSiteMutAttrEntity>()
                                                    .eq("site_id", siteId)
                                                    .ne("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                                                    .orderByDesc("weight")
                                                    .eq("pkey", wafKey));
                                    if (list1.size() > 0) {
                                        resultList.addAll(list1);
                                    } else {
                                        final String[] oneKeys = { "int", "text" };
                                        String keyType = SiteAttrEnum.getTypeByName(wafKey.toString());
                                        if (Arrays.asList(oneKeys).contains(keyType)) {
                                            TbSiteAttrEntity defaultEntity = new TbSiteAttrEntity();
                                            defaultEntity.setSiteId(siteId);
                                            defaultEntity.setPkey(wafKey.toString());
                                            defaultEntity.setPvalue(SiteAttrEnum.getKeyDefaultValue(wafKey.toString()));
                                            defaultEntity.setStatus(1);
                                            resultList.add(DataTypeConversionUtil.entity2map(defaultEntity));
                                        }
                                    }
                                    if (list2.size() > 0) {
                                        resultList.addAll(list2);
                                    }
                                }

                            }
                            final_all_group_list.addAll(resultList);
                            // 获取公共选择INDEX 和 私有选择INDEX
                            int pubWafSelectId = 0;
                            int priWafSelectSiteID = 0;
                            TbSiteAttrEntity pubSelectEntity = tbSiteAttrDao
                                    .selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                            .eq("site_id", siteId)
                                            .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName()));
                            if (null != pubSelectEntity && StringUtils.isNotBlank(pubSelectEntity.getPvalue())) {
                                pubWafSelectId = Integer.parseInt(pubSelectEntity.getPvalue());
                            }
                            if (0 == pubWafSelectId) {
                                TbSiteAttrEntity priSelectEntity = tbSiteAttrDao
                                        .selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                                .eq("site_id", siteId)
                                                .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_USER_SELECTS.getName()));
                                if (null != priSelectEntity && StringUtils.isNotBlank(priSelectEntity.getPvalue())) {
                                    priWafSelectSiteID = Integer.parseInt(priSelectEntity.getPvalue());
                                }
                            }
                            if (0 == pubWafSelectId && 0 == priWafSelectSiteID) {
                                // 为自身
                                List<TbSiteMutAttrEntity> list2 = tbSiteMutAttrDao
                                        .selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                                                .eq("site_id", siteId)
                                                .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                                                .orderByDesc("weight"));
                                list2.forEach(item -> {
                                    if (1 == item.getPvalue1() && StringUtils.isNotBlank(item.getPvalue())) {
                                        SysWafRuleVo vo = DataTypeConversionUtil.string2Entity(item.getPvalue(),
                                                SysWafRuleVo.class);
                                        if (null != vo) {
                                            if (false && !StaticVariableUtils.demoIp
                                                    .equals(StaticVariableUtils.authMasterIp)) {
                                                // 20240629 所有客户端显示规则
                                                vo.setRule(null);
                                            }
                                            item.setPvalue(DataTypeConversionUtil.entity2jonsStr(vo));
                                        }
                                    }
                                });
                                final_all_group_list.addAll(list2);
                            } else if (0 == pubWafSelectId && priWafSelectSiteID > 0) {
                                // 为 其它站的
                                List<TbSiteMutAttrEntity> list2 = tbSiteMutAttrDao
                                        .selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                                                .eq("site_id", priWafSelectSiteID)
                                                .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                                                .orderByDesc("weight"));
                                final_all_group_list.addAll(list2);
                            }
                            // 追加其它组的数据PUB_PRECISE_WAF_SELECTS PRI_USER_PRECISE_WAF_SET data
                            final_all_group_list.addAll(this
                                    .getAttrListByAttrGroup(SiteAttrEnum.PUB_PRECISE_WAF_SELECTS.getGroup(), siteId));
                            final_all_group_list.addAll(this
                                    .getAttrListByAttrGroup(SiteAttrEnum.PRI_USER_PRECISE_WAF_SET.getGroup(), siteId));

                        }
                        break;
                    default:
                        if (StringUtils.isNotBlank(group)) {
                            final_all_group_list.addAll(this.getAttrListByAttrGroup(group, siteId));
                        }
                        break;
                }
            }
            siteEntity.setAttr(final_all_group_list);
        } else if (StringUtils.isNotBlank(key)) {
            List<Map<String, Object>> list1 = tbSiteAttrDao.selectMaps(
                    new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("pkey", key));
            List<Map<String, Object>> list2 = tbSiteMutAttrDao
                    .selectMaps(new QueryWrapper<TbSiteMutAttrEntity>().eq("site_id", siteId).eq("pkey", key));
            list1.addAll(list2);
            siteEntity.setAttr(list1);
        }
        return siteEntity;
    }

    @Override
    public TbSiteEntity getSiteSuitInfo(Long userId, Integer siteId) {
        return querySiteAttr(userId, siteId, "suit", "");
    }

    private void checkPkeyPvalueRule(String pKey, String pValue) {
        if (StringUtils.isBlank(pKey)) {
            return;
        }
        SiteAttrEnum tEnum = SiteAttrEnum.getObjByName(pKey);
        if (null == tEnum) {
            return;
        }
        if (StringUtils.isBlank(tEnum.getRule())) {
            return;
        }
        Pattern tPattern = Pattern.compile(tEnum.getRule());
        Matcher m = tPattern.matcher(pValue);
        if (!m.matches()) {
            throw new RRException("[" + pKey + "]值不符合规则：" + tEnum.getRule());
        }
    }

    private void check2alias(String pValue) {
        if (StringUtils.isBlank(pValue)) {
            throw new RRException("[]别名格式有误[1]！");
        }
        // m_text
        // {"id":125,"value":"~@wwwww.com"}
        // alias:[{id: 4945, value: "2ef.test.cdn.com"}]==>{id: 4945, value:
        // "2ef.test.cdn.com"}
        NgxSiteAlasVo vo = DataTypeConversionUtil.string2Entity(pValue, NgxSiteAlasVo.class);
        // noinspection AlibabaUndefineMagicConstant
        if (null == vo) {
            throw new RRException("[]别名格式有误[2]！");
        }
        String alias_domain = vo.getValue();
        Integer id = vo.getId();
        final String serverNamePattern = "^(\\*\\.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
        Pattern r = Pattern.compile(serverNamePattern);
        Matcher m = r.matcher(alias_domain);
        if (!m.matches()) {
            throw new RRException("[" + alias_domain + "]别名格式有误！");
        }
        if (!inputAvailableService.checkNginxServerNameAndAliasIsValid("alias", id, alias_domain)) {
            throw new RRException("[" + alias_domain + "]别名冲突！");
        }

    }

    /**
     * 检测 回源 与端口冲突
     * 
     * @param siteId
     * @param attrId
     * @param pValue
     */
    private void check2sourceBaseInfo(Integer siteId, Integer attrId, String pValue) {
        if (StringUtils.isBlank(pValue)) {
            throw new RRException("缺少必要参数【id,protocol,port,line】");
        }
        // {"id":602,"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"1.1.11.1","domain":"","port":"12","line":1,"weight":1}]}
        // {"id":51,"protocol":"https","port":443,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"::1","domain":"","port":"443","line":1,"weight":1}]}
        NgxSourceBaseInfoVo vo = DataTypeConversionUtil.string2Entity(pValue, NgxSourceBaseInfoVo.class);
        if (null == vo) {
            throw new RRException("缺少必要参数【id,protocol,port,line】");
        }
        // 1 https 判定证书
        if (false) {
            if ("https".equals(vo.getProtocol())) {
                Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("site_id", siteId)
                        .eq("status", 1)
                        .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_PEM.getName()));
                if (0 == count) {
                    throw new RRException("无ssl证书,保存失败！");
                }
            }
        }

        // 2 端口冲突判定
        if (true) {
            Integer port = vo.getPort();
            // "{\"pvalue1\":\"listen\",\"pvalue2\":\"areaId\"}"
            TbSiteEntity site = tbSiteDao.selectById(siteId);
            Integer areaId = Integer
                    .parseInt(cdnMakeFileService.getNodeAreaGroupIdBySerialNumber(site.getSerialNumber()));
            TbSiteMutAttrEntity mutAttr = new TbSiteMutAttrEntity();
            mutAttr.setSiteId(siteId);
            if (null != attrId) {
                mutAttr.setId(attrId);
            }
            mutAttr.setPkey(SiteAttrEnum.SOURCE_BASE_INFO.getName());
            mutAttr.setPvalue(pValue);
            mutAttr.setPvalue1(vo.getPort());
            mutAttr.setPvalue2(areaId);
            inputAvailableService.checkListenIsAvailable(port, areaId, "site", null, mutAttr);
        }

        // 3 存在主线路判定
        if (true) {
            String source_set;
            if (null == vo.getSource_set()) {
                throw new RRException("【source_set】不存在！");
            }
            source_set = vo.getSource_set();
            boolean main_line_i_flag = false;
            List<NgxSourceBaseInfoVo.LineVo> line_array = vo.getLine();
            for (int i = 0; i < line_array.size(); i++) {
                NgxSourceBaseInfoVo.LineVo line_obj = line_array.get(i);
                if ("domain".equals(source_set) && null != line_obj.getDomain()) {
                    String domain = line_obj.getDomain();
                    // System.out.println(StringUtils.isNotBlank(domain));
                    if (StringUtils.isNotBlank(domain)) {
                        String pattern = "^[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(domain);
                        if (!m.matches()) {
                            throw new RRException("域名【" + domain + "】格式有误！");
                        }
                    }
                } else if ("ip".equals(source_set)) {
                    String ip = line_obj.getIp();
                    // System.out.println(StringUtils.isNotBlank(ip));
                    if (StringUtils.isNotBlank(ip)) {
                        if (!IPUtils.isValidIpv4OrIpv6(ip)) {
                            throw new RRException("【" + ip + "】不是一个有效的IP地址！");
                        }
                    }
                }
                if ("cookie".equals(vo.getUpstream()) || "hash".equals(vo.getUpstream())) {
                    if (line_obj.getLine().equals(2)) {
                        throw new RRException(vo.getUpstream() + "模式不可存在备用线路");
                    }
                }
                if (1 == line_obj.getLine()) {
                    main_line_i_flag = true;
                }
            }
            if (!main_line_i_flag) {
                throw new RRException("线路中缺少主线路");
            }
        }

    }

    // custom_dns
    private void check2customDns(Integer siteId, String pValue) {
        TbSiteEntity site = tbSiteDao.selectById(siteId);
        if (null == site) {
            throw new RRException("无此站【" + siteId + "】");
        }
        CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, site.getSerialNumber(), false, true);
        if (null == suitObj) {
            throw new RRException("站【" + siteId + "】套餐属性获取失败");
        }
        // ProductAttrVo
        // attrObj=DataTypeConversionUtil.json2entity(suitObj.getAttr(),ProductAttrVo.class)
        // ;
        ProductAttrVo attrObj = suitObj.getAttr();
        if (null == attrObj || null == attrObj.getCustom_dns() || 0 == attrObj.getCustom_dns()) {
            throw new RRException("站【" + site.getMainServerName() + "】的套餐没有开通此服务");
        }
        // {"siteId":"1066",
        // "custom_dns":"{\"handle\":\"create\",\"recordId\":null,\"top\":\"waf_antsxdp_com.7edd32c78e.91hu.top\",\"recordType\":\"A\",\"line\":\"default\",\"value\":\"23.23.23.23\",\"ttl\":600}"}
        JSONObject op_object = DataTypeConversionUtil.string2Json(pValue);
        if (op_object.containsKey("handle") && op_object.containsKey("dnsConfigId")) {
            String handle = op_object.getString("handle");
            String dnsConfigId_ = op_object.getString("dnsConfigId");
            if (StringUtils.isBlank(handle) || StringUtils.isBlank(dnsConfigId_)) {
                throw new RRException("参数不完整！");
            }

            int end_i = dnsConfigId_.indexOf("_");
            Integer dns_conf_id = Integer.parseInt(dnsConfigId_.substring(0, end_i));
            String min_server = site.getMainServerName().replace(".", "-");
            String top_ = suitObj.getCname().toString().replace("*", min_server);
            TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dns_conf_id);
            if (null != dnsConfig) {
                top_ = top_.replace("." + dnsConfig.getAppDomain(), "");
            }
            if ("create".equals(handle) && op_object.containsKey("recordType") && op_object.containsKey("line")
                    && op_object.containsKey("value") && op_object.containsKey("ttl")) {
                String type_ = op_object.getString("recordType");
                String line_ = op_object.getString("line");
                String value_ = op_object.getString("value");
                String ttl_ = op_object.getString("ttl");
                R r = dnsCApiService.addRecordByConfId(dns_conf_id, top_, type_, line_, value_, ttl_);
                if (null == r) {
                    throw new RRException("创建失败！");
                }
                if (1 != r.getCode()) {
                    logger.error(r.toString());
                }
            } else if ("update".equals(handle) && op_object.containsKey("recordId")
                    && op_object.containsKey("recordType") && op_object.containsKey("line")
                    && op_object.containsKey("value") && op_object.containsKey("ttl")) {
                String type_ = op_object.getString("recordType");
                String line_ = op_object.getString("line");
                String value_ = op_object.getString("value");
                String ttl_ = op_object.getString("ttl");
                String recordId = op_object.getString("recordId");
                R r = dnsCApiService.modifyRecordByConfigId(dns_conf_id, recordId, top_, type_, line_, value_, ttl_);
                if (null == r) {
                    throw new RRException("失败！");
                }
                if (1 != r.getCode()) {
                    logger.error(r.toString());
                }
            } else if ("delete".equals(handle) && op_object.containsKey("recordId")) {
                String recordId = op_object.getString("recordId");
                R r = dnsCApiService.removeRecordByRecordIdAndDnsId(dns_conf_id, recordId);
                if (null == r) {
                    throw new RRException("失败！");
                }
                if (1 != r.getCode()) {
                    logger.error(r.toString());
                }
            } else {
                logger.error(pValue + "UNKNOWN DNS OPERATE!");
            }
        }

    }

    // 所有类型检测入参规则与权限
    private void checkValueFormat(Integer siteId, Integer attrId, String pKey, String pValue) {
        checkPkeyPvalueRule(pKey, pValue);
        switch (pKey) {
            case "alias":
                this.check2alias(pValue);
                break;
            case "mobile_jump":
                if (StringUtils.isNotEmpty(pValue)) {
                    String pattern = "(http|https)://([\\w.]+/?)\\S*";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(pValue);
                    if (!m.matches()) {
                        throw new RRException("请以http:// 或 https://开头的正确地址");
                    }
                }
                break;
            case "source_sni":
            case "source_host":
                if (StringUtils.isNotEmpty(pValue)) {
                    String pattern = "^[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(pValue);
                    if (!m.matches()) {
                        throw new RRException("域名格式有误【2】！");
                    }
                }
                break;
            case "source_base_info":
                this.check2sourceBaseInfo(siteId, attrId, pValue);
                break;
            case "search_engines_dns_source":
                if (true) {
                    TbSiteEntity site = tbSiteDao.selectById(siteId);
                    if (null == site) {
                        throw new RRException("无此站【" + siteId + "】");
                    }
                    if (null == site.getSerialNumber()) {
                        throw new RRException("站【" + siteId + "】套餐为空");
                    }
                    CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, site.getSerialNumber(), false,
                            true);
                    if (null == suitObj) {
                        throw new RRException("站【" + siteId + "】套餐属性获取失败");
                    }
                    // ProductAttrVo attrObj=DataTypeConversionUtil.json2entity(
                    // suitObj.getAttr(),ProductAttrVo.class);
                    ProductAttrVo attrObj = suitObj.getAttr();
                    if (null == attrObj || null == attrObj.getSeo_dns() || 0 == attrObj.getSeo_dns()) {
                        throw new RRException("站【" + site.getMainServerName() + "】的套餐没有开通此服务");
                    }
                    if (!"1".equals(pValue)) {
                        this.delSiteCusDns(siteId);
                    }
                }
                break;
            case "add_header":
            case "proxy_set_header":
                if (true) {
                    if (StringUtils.isBlank(pValue)) {
                        throw new RRException("HEAD 不能为空");
                    }
                    SetHttpHeadVo headVo = DataTypeConversionUtil.string2Entity(pValue, SetHttpHeadVo.class);
                    if (null == headVo) {
                        throw new RRException("参数不完整!");
                    }

                    if (headVo.getHeader().contains("'") || headVo.getHeader().contains("\"")) {
                        throw new RRException("header 不能包含 '|\"");
                    }
                    if (headVo.getContent().contains("'") || headVo.getContent().contains("\"")) {
                        throw new RRException("content 不能包含 '|\"");
                    }

                    if (StringUtils.isBlank(headVo.getType())) {
                        throw new RRException("缺少必要参数【type】");
                    }
                    Integer id = headVo.getId();
                    String type = headVo.getType();
                    if (StringUtils.isNotBlank(type) && !type.equals("custom")) {
                        Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                                .eq("site_id", siteId)
                                .eq("status", 1)
                                .ne("id", id)
                                .eq("pkey", pKey)
                                .like("pvalue", "\"type\":\"" + type + "\","));
                        if (count > 0) {
                            throw new RRException("【" + type + "】重复");
                        }
                    }
                }
                break;
            case "other_ssl_pem":
                if (StringUtils.isBlank(pValue)) {
                    throw new RRException("SSL证书不能为空");
                } else {
                    JSONObject object = DataTypeConversionUtil.string2Json(pValue);
                    if (!object.containsKey("value")) {
                        throw new RRException("缺少必要参数【value】");
                    }
                    String certStr = object.getString("value").trim();
                    // logger.debug(certStr);
                    String ssl_host = HashUtils.ReadCerSubjectDN(certStr);
                    // 对1=证书域名检测,2=证书域名不检测
                    int check_ssl_mach_mode = 2;
                    if (1 == check_ssl_mach_mode) {
                        // 1 crt 证书验证
                        TbSiteEntity siteEntity = tbSiteDao.selectById(siteId);
                        if (null == siteEntity) {
                            throw new RRException("无此站【" + siteId + "】");
                        }
                        // CN=www.antsxdp.com *.antsxdp.com
                        Integer i = ssl_host.indexOf("=");
                        if (-1 != i) {
                            i++;
                            ssl_host = ssl_host.substring(i);
                        }
                        String main_server = siteEntity.getMainServerName();
                        String pattern = ssl_host.replace(".", "\\.");
                        // pattern=pattern.replace("*.",".*");
                        pattern = pattern.replace("*\\.", ".*\\.?");
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(main_server);
                        if (m.matches()) {
                            // 保存证书
                        } else {
                            logger.error("证书有误");
                            throw new RRException("证书有误!与当前域名不符！");
                        }
                    } else if (2 == check_ssl_mach_mode) {
                        if (StringUtils.isNotBlank(ssl_host)) {
                            TbSiteEntity siteEntity = tbSiteDao.selectById(siteId);
                            if (null == siteEntity) {
                                throw new RRException("无此站【" + siteId + "】");
                            }
                        } else {
                            throw new RRException("证书有误!");
                        }
                    }
                }
                break;
            case "other_ssl_key":
                break;
            case "ocsp":
            case "forced_ssl":
            case "forced_hsts":
                if (!"0".equals(pValue)) {
                    Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("status", 1)
                            .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_PEM.getName()));
                    if (0 == count) {
                        throw new RRException("无ssl证书,开启失败！");
                    }
                    Long count2 = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("status", 1)
                            .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                            .like("pvalue", "\"protocol\":\"https\","));
                    if (0 == count2) {
                        throw new RRException("未开启HTTPS配置,请先配置HTTPS！");
                    }
                }
                break;
            case "ssl_protocols":
                if (StringUtils.isNotBlank(pValue)) {
                    Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("status", 1)
                            .eq("pkey", SiteAttrEnum.SSL_OTHER_CERT_PEM.getName()));
                    if (0 == count) {
                        throw new RRException("无ssl证书,配置失败！");
                    }
                    Long count2 = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("status", 1)
                            .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                            .like("pvalue", "\"protocol\":\"https\","));
                    if (0 == count2) {
                        throw new RRException("未开启HTTPS配置,请先配置HTTPS！");
                    }
                }
                break;
            case "gzip_min_length":
                if (StringUtils.isNotEmpty(pValue)) {
                    pValue = pValue.trim();
                    String pattern = "^\\d+[k|m]?$";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(pValue);
                    if (!m.matches()) {
                        throw new RRException("【压缩的最小文件的大小】格式有误！");
                    }
                }
                break;
            case "waf_rule_pass":
            case "waf_rule_forbid":
            case "waf_rule_suspicious":
                if (true) {
                    TbSiteEntity siteEntity = tbSiteDao.selectById(siteId);
                    if (null == siteEntity) {
                        throw new RRException("无此站【" + siteId + "】");
                    }
                }
                break;
            case "pub_waf_pass_selects":
            case "pub_waf_forbid_selects":
            case "pub_waf_suspicious_selects":
            case "pub_precise_waf":
                // pValue="15#1";
                if (true) {
                    TbSiteEntity siteEntity = tbSiteDao.selectById(siteId);
                    if (null == siteEntity) {
                        throw new RRException("无此站【" + siteId + "】");
                    }
                    if (StringUtils.isBlank(pValue)) {
                        throw new RRException("缺少必要参数【pvalue】");
                    } else {
                        JSONObject object = DataTypeConversionUtil.string2Json(pValue);
                        if (!object.containsKey("value")) {
                            throw new RRException("缺少必要参数【value】");
                        }
                        String c_id = object.getString("value");
                        String[] s = c_id.split("#");
                        if (s.length >= 2) {
                            String del_value0 = s[0] + "#0";
                            String del_value1 = s[0] + "#1";
                            tbSiteAttrDao.delete(new QueryWrapper<TbSiteAttrEntity>().eq("site_id", siteId)
                                    .and(q -> q.eq("pvalue", del_value0).or().eq("pvalue", del_value1))
                                    .eq("pkey", pKey));
                        }
                    }
                }
                break;
            case "pri_precise_waf_selects":
                // 公有规则
                if (true) {
                    TbSiteEntity site = tbSiteDao.selectById(siteId);
                    if (null == site) {
                        throw new RRException("无此站【" + siteId + "】");
                    }
                    if (null == site.getSerialNumber()) {
                        throw new RRException("站【" + siteId + "】套餐为空");
                    }
                    CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, site.getSerialNumber(), false,
                            true);
                    if (null == suitObj) {
                        throw new RRException("站【" + siteId + "】套餐属性获取失败");
                    }
                    // ProductAttrVo attrObj=DataTypeConversionUtil.json2entity(
                    // suitObj.getAttr(),ProductAttrVo.class);
                    ProductAttrVo attrObj = suitObj.getAttr();
                    if (null == attrObj || null == attrObj.getPublic_waf() || 0 == attrObj.getPublic_waf()) {
                        throw new RRException("站【" + site.getMainServerName() + "】的套餐没有开通此服务");
                    }
                    if (!"0".equals(pValue)) {
                        // 将选用WAF 设置为0
                        tbSiteAttrDao.update(null, new UpdateWrapper<TbSiteAttrEntity>()
                                .eq("site_id", siteId)
                                .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_USER_SELECTS.getName())
                                .set("pvalue", "0")
                                .set("update_time", new Date()));
                    }
                }
            case "pri_precise_waf_user_selects":
                if (true) {
                    // 将公共WAF 设置为0
                    tbSiteAttrDao.update(null, new UpdateWrapper<TbSiteAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName())
                            .set("pvalue", "0")
                            .set("update_time", new Date()));
                }
                break;
            case "pri_precise_waf_details":
                if (true) {
                    // 私有规则
                    TbSiteEntity site = tbSiteDao.selectById(siteId);
                    if (null == site) {
                        throw new RRException("无此站【" + siteId + "】");
                    }
                    CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, site.getSerialNumber(), true, true);
                    if (null == suitObj) {
                        throw new RRException("站【" + siteId + "】套餐属性获取失败");
                    }
                    // ProductAttrVo attrObj=DataTypeConversionUtil.json2entity(
                    // suitObj.getAttr(),ProductAttrVo.class);
                    ProductAttrVo attrObj = suitObj.getAttr();
                    if (null == attrObj || null == attrObj.getPrivate_waf() || 0 == attrObj.getPrivate_waf()) {
                        throw new RRException("站【" + site.getMainServerName() + "】的套餐没有开通此服务");
                    }
                    NgxWafRuleVo ruleVo = DataTypeConversionUtil.string2Entity(pValue, NgxWafRuleVo.class);
                    if (null == ruleVo) {
                        throw new RRException("[" + pValue + "]error");
                    }
                    for (NgxWafRuleVo.RuleInfo ruleInfo : ruleVo.getRule()) {
                        if (ruleInfo.getContent().length() > 240) {
                            throw new RRException("[" + ruleInfo.getContent().substring(0, 16) + "......]字符长度超出");
                        }
                        // check reg
                        if (2 == CheckHyperRegUtils.checkHyper(ruleInfo.getContent())) {
                            throw new RRException("[" + ruleVo.getRemark() + "] 编译规则失败");
                        }
                    }

                }
                break;
            case "custom_dns":
                // 自定义DNS 保存与权限验证
                this.check2customDns(siteId, pValue);
                break;
            case "sub_filter":
                // {id: 4951, value: "123----12312"}
                // m_text
                if (true) {
                    JSONObject obj = DataTypeConversionUtil.string2Json(pValue);
                    // noinspection AlibabaUndefineMagicConstant
                    if (obj.containsKey("value")) {
                        String value = obj.getString("value");
                        // noinspection AlibabaUndefineMagicConstant
                        if (value.contains("\"") || value.contains("'")) {
                            throw new RRException("不能包含\"|'");
                        }
                    }
                }
                break;
            case "proxy_buffering":
                if (!(pValue.toString().equals("0") || pValue.toString().equals("1"))) {
                    throw new RRException(pValue + "取值需为0或1");
                }
                break;
            case "cache_config":
            case "un_cache_config":
                if (true) {
                    // {"type":3,"content":"/","time":24,"unit":"h","mode":"cache"}
                    NgxCacheConfVo ngxCacheConfVo = DataTypeConversionUtil.string2Entity(pValue, NgxCacheConfVo.class);
                    if (StringUtils.isBlank(ngxCacheConfVo.getContent())) {
                        throw new RRException("缺少参数[content]");
                    }
                    String content = ngxCacheConfVo.getContent();
                    // Integer type=cacheObj.getInteger("type");
                    int id = ngxCacheConfVo.getId();
                    String[] systemLocation = { "/purge(/.*)", "/purge1(/.*)",
                            "^/\\.well-known/acme-challenge/([-_a-zA-Z0-9]+)$", "/purge(/.*)" };
                    if (Arrays.asList(systemLocation).contains(content)) {
                        throw new RRException("当前路径已使用【1】！");
                    }
                    String likeLocation = ",\"content\":\"" + content + "\",";
                    Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                            .ne("id", id)
                            .eq("site_id", siteId)
                            .like("pkey", "cache_config")
                            .like("pvalue", likeLocation)
                            .eq("status", 1));
                    if (count > 0) {
                        throw new RRException("缓存路径" + content + "不能重复【2】");
                    }
                    // content 输入字符判定
                    String pattern = "[-|._/a-zA-Z0-9]*";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(content);
                    if (!m.matches()) {
                        throw new RRException("缓存格式不正确！");
                    }

                    if (content.contains("\\|\\|")) {
                        throw new RRException("||错误");
                    }

                    // 输入内重复判定
                    List<String> objLs = new ArrayList<>();
                    String[] contentLS = content.split("\\|");
                    for (String CacheTypeObj : contentLS) {
                        if (StringUtils.isBlank(CacheTypeObj)) {
                            throw new RRException("[" + CacheTypeObj + "] 为空");
                        }
                        if (objLs.contains(CacheTypeObj)) {
                            throw new RRException("[" + CacheTypeObj + "] 重复");
                        }
                        objLs.add(CacheTypeObj);
                    }

                }
                break;
            case "anti_theft_chain":
                if (true) {
                    NgxReferersBlockConfVo ngxReferersBlockConfVo = DataTypeConversionUtil.string2Entity(pValue,
                            NgxReferersBlockConfVo.class);
                    if (null == ngxReferersBlockConfVo) {
                        throw new RRException("解析失败！");
                    }

                    if (StringUtils.isNotBlank(ngxReferersBlockConfVo.getMatch_domains())) {
                        final String serverNamePattern = "^(\\*\\.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
                        Pattern r = Pattern.compile(serverNamePattern);
                        for (String mDomain : ngxReferersBlockConfVo.getMatch_domains().split("\\s+")) {
                            Matcher m = r.matcher(mDomain);
                            if (!m.matches()) {
                                throw new RRException("[" + mDomain + "]格式有误【1】！");
                            }
                            if (false && IPUtils.isValidIPV4(mDomain)) {
                                // 2024-12-13可以ip
                                throw new RRException("[" + mDomain + "]格式有误！ ");
                            }
                        }

                    }
                    if (null == ngxReferersBlockConfVo.getMatch_uri_type()) {
                        throw new RRException("[" + ngxReferersBlockConfVo.getMatch_uri_type() + "]格式有误【1】！");
                    }
                    if (StringUtils.isBlank(ngxReferersBlockConfVo.getMatch_uri())) {
                        throw new RRException("[" + ngxReferersBlockConfVo.getMatch_uri() + "]格式有误【1】！");
                    }
                    if (1 == ngxReferersBlockConfVo.getMatch_uri_type()) {
                        final String matchPattern = "^[a-zA-Z0-9\\|]+$";
                        Pattern r = Pattern.compile(matchPattern);
                        Matcher m = r.matcher(ngxReferersBlockConfVo.getMatch_uri());
                        if (!m.matches()) {
                            throw new RRException("[" + ngxReferersBlockConfVo.getMatch_uri() + "]后缀格式有误【2】！");
                        }

                    } else if (2 == ngxReferersBlockConfVo.getMatch_uri_type()) {
                        final String matchPattern = "^\\/(\\w+\\/?){0,}$";
                        ;
                        Pattern r = Pattern.compile(matchPattern);
                        String[] urls = ngxReferersBlockConfVo.getMatch_uri().split("\\|");
                        for (String dir : urls) {
                            Matcher m = r.matcher(dir);
                            if (!m.matches()) {
                                throw new RRException("[" + dir + "]路径格式有误【2】！");
                            }
                        }
                    }

                }
                break;
            case "server_waf_white_ip":
                if (true) {
                    // eq("site_id",siteId).eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName()).set("pvalue","0").set("updatetime",new
                    // Date());
                    Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id", siteId).eq("pkey", SiteAttrEnum.PUB_WAF_WHITE_IP.getName()));
                    if (count > 50) {
                        throw new RRException("白名单数据超出限制！");
                    }
                    NgxIpFormVo ngxIpFormVo = DataTypeConversionUtil.string2Entity(pValue, NgxIpFormVo.class);
                    if (null != ngxIpFormVo) {
                        Long isExist = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                                .ne(null != ngxIpFormVo.getId(), "id", ngxIpFormVo.getId())
                                .eq("site_id", siteId)
                                .eq("pkey", SiteAttrEnum.PUB_WAF_WHITE_IP.getName())
                                .like("pvalue", "\"ip\":\"" + ngxIpFormVo.getIp() + "\","));
                        if (isExist > 0) {
                            throw new RRException(ngxIpFormVo.getIp() + "重复！");
                        }
                    }
                }
                break;
            case "site_uri_rewrite":
                if (true) {
                    NgxSiteUriRewriteVo rVo = DataTypeConversionUtil.string2Entity(pValue, NgxSiteUriRewriteVo.class);
                    if (null == rVo) {
                        throw new RRException("参数有误！");
                    }
                }
                break;
            case "waf_sys_rule_config":
                if (true) {
                    TbSiteEntity site = tbSiteDao.selectById(siteId);
                    if (null == site) {
                        throw new RRException("无此站【" + siteId + "】");
                    }
                    CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, site.getSerialNumber(), true, true);
                    if (null == suitObj) {
                        throw new RRException("站【" + siteId + "】套餐属性获取失败");
                    }
                    // ProductAttrVo attrObj=DataTypeConversionUtil.json2entity(
                    // suitObj.getAttr(),ProductAttrVo.class);
                    ProductAttrVo attrObj = suitObj.getAttr();
                    if (null == attrObj || null == attrObj.getPrivate_waf() || 0 == attrObj.getPrivate_waf()) {
                        throw new RRException("站【" + site.getMainServerName() + "】的套餐没有开通此服务");
                    }
                    SysWafRuleConfVo vo = DataTypeConversionUtil.string2Entity(pValue, SysWafRuleConfVo.class);
                    if (null == vo) {
                        throw new RRException("参数有误！");
                    }
                    ValidatorUtils.validateEntity(vo);
                    // 自动生成规则
                    // 当前站所有已生成的系统规则
                    JSONObject kvObj = DataTypeConversionUtil.entity2jsonObj(vo);
                    logger.info(kvObj.toString());
                    tbSiteMutAttrDao.delete(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                            .eq("pvalue1", 1)
                            .eq("pvalue2", 0));
                    JSONObject newBuildJsonObj = new JSONObject();
                    for (String key : kvObj.keySet()) {
                        int index = SysWafRuleConfVo.getWafRuleTypeId(key);
                        int v = kvObj.getInteger(key);
                        if (0 == v) {
                            // 关闭 系统规则
                            tbSiteMutAttrDao.update(null, new UpdateWrapper<TbSiteMutAttrEntity>()
                                    .eq("site_id", siteId)
                                    .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                                    .eq("pvalue1", 1)
                                    .eq("pvalue2", index)
                                    .set("status", 0));
                        } else if (1 == v) {
                            // 开启 系统规则
                            Long tCount = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                                    .eq("site_id", siteId)
                                    .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                                    .eq("pvalue1", 1)
                                    .eq("pvalue2", index));
                            if (tCount > 0) {
                                tbSiteMutAttrDao.update(null, new UpdateWrapper<TbSiteMutAttrEntity>()
                                        .eq("site_id", siteId)
                                        .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                                        .eq("pvalue1", 1)
                                        .eq("pvalue2", index)
                                        .set("status", 1));
                            } else {
                                newBuildJsonObj.put(key, 1);
                            }
                        }
                    }
                    // 生成添加系统规则
                    List<SysWafRuleVo> list = RegExUtils.buildSysWafByKey(
                            DataTypeConversionUtil.json2entity(newBuildJsonObj, SysWafRuleConfVo.class));
                    if (null == list || list.isEmpty()) {
                        break;
                    }
                    // logger.info(list.toString());
                    for (SysWafRuleVo r_vo : list) {
                        TbSiteMutAttrEntity mutAttr = new TbSiteMutAttrEntity();
                        mutAttr.setSiteId(siteId);
                        mutAttr.setPkey(SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName());
                        mutAttr.setPvalue(DataTypeConversionUtil.entity2jonsStr(r_vo));
                        mutAttr.setPvalue1(1);
                        mutAttr.setPvalue2(Long.valueOf(String.valueOf(r_vo.getSys_index())));
                        mutAttr.setWeight(999999);
                        mutAttr.setPType(SiteAttrEnum.getTypeByName(mutAttr.getPkey()));
                        tbSiteMutAttrDao.insert(mutAttr);
                    }
                }
                break;
            case "referer_check":
                if (true) {
                    NgxRefererCheckVo vo = DataTypeConversionUtil.string2Entity(pValue, NgxRefererCheckVo.class);
                    if (null == vo) {
                        throw new RRException("参数有误！");
                    }
                    ValidatorUtils.validateEntity(vo);
                }
                break;
            case "pri_waf_url_strings":
                if (true) {
                    if (pValue.contains("&") || pValue.contains(":") || pValue.contains("\r")
                            || pValue.contains("\n")) {
                        throw new RRException("参数不可包含【&:\\r\\n】！");
                    }
                }
                break;
            case "error_code_rewrite":
                if (true) {
                    NgxErrCodeRewriteVo vo = DataTypeConversionUtil.string2Entity(pValue, NgxErrCodeRewriteVo.class);
                    if (null == vo) {
                        throw new RRException("参数有误！");
                    }
                    ValidatorUtils.validateEntity(vo);
                    Integer[] existParamCodes = { 200, 301, 302, 307 };
                    if (Arrays.asList(existParamCodes).contains(Integer.valueOf(vo.getRewriteCode()))) {
                        if (StringUtils.isNotBlank(vo.getRewriteParam())) {
                            if (vo.getRewriteParam().contains("\"")) {
                                throw new RRException("rewriteParam 包含特殊字符(\")！");
                            }
                        } else {
                            throw new RRException("rewriteParam 不能为空！");
                        }
                    }

                }
                break;
            default:
                break;
        }
    }

    /**
     * bool int text save
     * 
     * @param siteId
     * @param pKey
     * @param pValue
     */
    private void singleSaveAttr(Integer siteId, String pKey, String pValue) {
        if (pKey.length() > 255) {
            throw new RRException("取值长度超出！");
        }

        List<TbSiteAttrEntity> list = tbSiteAttrDao.selectList(
                new QueryWrapper<TbSiteAttrEntity>()
                        .eq("site_id", siteId)
                        .eq("pkey", pKey));
        if (0 == list.size()) {
            this.checkValueFormat(siteId, null, pKey, pValue);
            TbSiteAttrEntity attr = new TbSiteAttrEntity();
            attr.setSiteId(siteId);
            attr.setPkey(pKey);
            attr.setPvalue(pValue);
            attr.setStatus(1);
            attr.setPType(SiteAttrEnum.getTypeByName(attr.getPkey()));
            this.updateAdditionalAttr(attr, null);
            tbSiteAttrDao.insert(attr);
        } else {
            // update
            for (TbSiteAttrEntity attr : list) {
                this.checkValueFormat(siteId, attr.getId(), pKey, pValue);
                attr.setPvalue(pValue);
                attr.setUpdateTime(new Date());
                attr.setStatus(1);
                this.updateAdditionalAttr(attr, null);
                tbSiteAttrDao.updateById(attr);
            }
        }
    }

    /**
     * l_text save
     * 
     * @param siteId
     * @param pKey
     * @param pValue text|jsonStr
     */
    private void longSingleSaveAttr(Integer siteId, String pKey, String pValue) {
        TbSiteMutAttrEntity attr = tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id", siteId)
                .eq("pkey", pKey)
                .last("limit 1"));
        if (null == attr) {
            this.checkValueFormat(siteId, null, pKey, pValue);
            attr = new TbSiteMutAttrEntity();
            attr.setSiteId(siteId);
            attr.setPkey(pKey);
            attr.setPvalue(pValue);
            attr.setStatus(1);
            attr.setPType(SiteAttrEnum.getTypeByName(attr.getPkey()));
            this.updateAdditionalAttr(null, attr);
            tbSiteMutAttrDao.insert(attr);
        } else {
            this.checkValueFormat(siteId, attr.getId(), pKey, pValue);
            attr.setPvalue(pValue);
            this.updateAdditionalAttr(null, attr);
            tbSiteMutAttrDao.updateById(attr);
        }
    }

    /**
     * m_text save
     * 
     * @param siteId
     * @param pKey
     * @param pValueArray
     * @return
     */
    private Boolean singleMultipleSaveAttr(Integer siteId, String pKey, JSONArray pValueArray) {
        // [{"id":0,""vlaue":"acom.com"}]
        for (int i = 0; i < pValueArray.size(); i++) {
            JSONObject jsonObject = pValueArray.getJSONObject(i);
            if (jsonObject.containsKey("id") && jsonObject.getInteger("id") > 0) {
                // update
                Integer id = jsonObject.getInteger("id");
                // jsonObject.remove("id");
                checkValueFormat(siteId, id, pKey, jsonObject.toJSONString());
                if (jsonObject.containsKey("value")) {
                    String value = jsonObject.getString("value");
                    if (value.length() > 255) {
                        throw new RRException("长度超出！");
                    }
                    Long count = tbSiteAttrDao.selectCount(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id", siteId)
                            .eq("pkey", pKey)
                            .eq("pvalue", value));
                    if (0 == count) {
                        // 不存在才更新
                        List<TbSiteAttrEntity> list = tbSiteAttrDao.selectList(
                                new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("id", id)
                                        .eq("site_id", siteId)
                                        .eq("pkey", pKey));
                        for (TbSiteAttrEntity attr : list) {
                            attr.setPvalue(jsonObject.getString("value"));
                            attr.setUpdateTime(new Date());
                            this.updateAdditionalAttr(attr, null);
                            tbSiteAttrDao.updateById(attr);
                        }

                    }
                }
            } else {
                // insert
                if (jsonObject.containsKey("id")) {
                    jsonObject.remove("id");
                }
                checkValueFormat(siteId, null, pKey, jsonObject.toJSONString());
                if (jsonObject.containsKey("value")) {
                    String value = jsonObject.getString("value");
                    if (value.length() > 255) {
                        throw new RRException("长度超出！");
                    }
                    Long count = tbSiteAttrDao.selectCount(new QueryWrapper<TbSiteAttrEntity>().eq("site_id", siteId)
                            .eq("pkey", pKey).eq("pvalue", value));
                    if (0 == count) {
                        TbSiteAttrEntity attr = new TbSiteAttrEntity();
                        attr.setSiteId(siteId);
                        attr.setPkey(pKey);
                        attr.setPvalue(jsonObject.getString("value"));
                        attr.setStatus(1);
                        attr.setPType(SiteAttrEnum.getTypeByName(attr.getPkey()));
                        this.updateAdditionalAttr(attr, null);
                        tbSiteAttrDao.insert(attr);
                        // logger.debug(attr.getId()+"");

                    }
                }
            }
        }
        return true;
    }

    // 自动更新附加属性
    private void updateAdditionalAttr(TbSiteAttrEntity attrEntity, TbSiteMutAttrEntity mutAttrEntity) {
        JSONObject siteAttrObj = new JSONObject();
        if (null != attrEntity) {
            siteAttrObj = DataTypeConversionUtil.entity2jsonObj(attrEntity);
        } else if (null != mutAttrEntity) {
            siteAttrObj = DataTypeConversionUtil.entity2jsonObj(mutAttrEntity);
        } else {
            siteAttrObj = new JSONObject();
        }
        if (null == siteAttrObj || !siteAttrObj.containsKey("pkey")
                || StringUtils.isBlank(siteAttrObj.getString("pkey"))) {
            return;
        }
        String pkey = siteAttrObj.getString("pkey");
        // 获取附属性
        JSONObject addKVJsonObject = SiteAttrEnum.getAdditionalByKey(pkey);
        if (null == addKVJsonObject) {
            return;
        }
        if (addKVJsonObject.size() > 0) {
            TbSiteEntity site = tbSiteDao.selectById(siteAttrObj.getLong("siteId"));
            if (null == site) {
                return;
            }
            for (String key : addKVJsonObject.keySet()) {
                String value = addKVJsonObject.getString(key);
                switch (value) {
                    case "areaId":
                        String areaId = cdnMakeFileService.getNodeAreaGroupIdBySerialNumber(site.getSerialNumber());
                        if ("pvalue1" == key || "pvalue2" == key) {
                            siteAttrObj.put(key, Long.parseLong(areaId));
                        }
                        break;
                    case "listen":
                        JSONObject baseVoJson = DataTypeConversionUtil.string2Json(siteAttrObj.getString("pvalue"));
                        if (null == baseVoJson) {
                            continue;
                        }
                        if (baseVoJson.containsKey("port")) {
                            baseVoJson.put("port", Integer.parseInt(baseVoJson.get("port").toString()));
                            siteAttrObj.put(key, Integer.parseInt(baseVoJson.get("port").toString()));
                        } else {
                            if (!baseVoJson.containsKey("pvalue")) {
                                continue;
                            }
                            NgxSourceBaseInfoVo baseVo = DataTypeConversionUtil
                                    .string2Entity(siteAttrObj.getString("pvalue"), NgxSourceBaseInfoVo.class);
                            if (null != baseVo) {
                                if ("pvalue1" == key || "pvalue2" == key) {
                                    siteAttrObj.put(key, baseVo.getPort());
                                }
                            }
                        }
                        break;
                    case "end_time":
                        JSONObject pemObj = JSONObject.parseObject(siteAttrObj.getString("pvalue"));
                        if (pemObj.containsKey("value")) {
                            long endTime = HashUtils.getCertEndTime(pemObj.getString("value"));
                            siteAttrObj.put(key, endTime);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        if (null != attrEntity) {
            TbSiteAttrEntity attrEntityBuf = DataTypeConversionUtil.json2entity(siteAttrObj, TbSiteAttrEntity.class);
            attrEntity.setPvalue(attrEntityBuf.getPvalue());
            attrEntity.setPvalue1(attrEntityBuf.getPvalue1());
            attrEntity.setPvalue2(attrEntityBuf.getPvalue2());
        } else if (null != mutAttrEntity) {
            TbSiteMutAttrEntity mutAttrEntityBuf = DataTypeConversionUtil.json2entity(siteAttrObj,
                    TbSiteMutAttrEntity.class);
            mutAttrEntity.setPvalue(mutAttrEntityBuf.getPvalue());
            mutAttrEntity.setPvalue1(mutAttrEntityBuf.getPvalue1());
            mutAttrEntity.setPvalue2(mutAttrEntityBuf.getPvalue2());
        }
    }

    /**
     * mm_text 类型保存
     * 
     * @param siteId
     * @param pKey
     * @param pValueArray
     * @return
     */
    private boolean multipleMultipleSaveAttr(Integer siteId, String pKey, JSONArray pValueArray) {
        for (int i = 0; i < pValueArray.size(); i++) {
            JSONObject jsonObject = pValueArray.getJSONObject(i);
            if (jsonObject.containsKey("id") && null != jsonObject.get("id") && jsonObject.getInteger("id") > 0) {
                // 更新 update
                Integer id = jsonObject.getInteger("id");
                TbSiteMutAttrEntity attr = tbSiteMutAttrDao.selectById(id);
                if (null == attr) {
                    logger.error(String.format("update,site:%d, attr:%s ,not exist  ! ", siteId, pKey));
                    continue;
                }
                this.checkValueFormat(siteId, id, pKey, jsonObject.toJSONString());
                attr.setPvalue(jsonObject.toJSONString());
                attr.setUpdateTime(new Date());
                this.updateAdditionalAttr(null, attr);
                tbSiteMutAttrDao.updateById(attr);

            } else {
                // 创建一个
                // INSERT
                if (jsonObject.containsKey("id")) {
                    jsonObject.remove("id");
                }
                this.checkValueFormat(siteId, null, pKey, jsonObject.toJSONString());
                Long count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("site_id", siteId).eq("pkey", pKey).eq("pvalue", jsonObject.toJSONString()));
                if (count > 0) {
                    logger.error(String.format("insert,site:%d, attr:%s ,already exist  ! ", siteId, pKey));
                    continue;
                }
                // 不重复创建
                int testUp = 0;
                if (Arrays.asList(StaticVariableUtils.onlyOneAttrGroup).contains(pKey)) {
                    // 对单一多MM_TEXT 去重
                    UpdateWrapper<TbSiteMutAttrEntity> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("site_id", siteId).eq("pkey", pKey).set("pvalue", jsonObject.toJSONString());
                    testUp = tbSiteMutAttrDao.update(null, updateWrapper);
                }
                if (0 == testUp) {
                    TbSiteMutAttrEntity attr = new TbSiteMutAttrEntity();
                    attr.setSiteId(siteId);
                    attr.setPkey(pKey);
                    attr.setPvalue(jsonObject.toJSONString());
                    attr.setStatus(1);
                    attr.setPType(SiteAttrEnum.getTypeByName(attr.getPkey()));
                    this.updateAdditionalAttr(null, attr);
                    tbSiteMutAttrDao.insert(attr);
                    // logger.debug(attr.getId()+"");
                }

            }
        }
        return true;
    }

    // 从params 中获取pem key
    private String getPemOrKey(Map<String, Object> params, String key) {
        JSONObject pJson = DataTypeConversionUtil.map2json(params);
        if (null == pJson) {
            return null;
        }
        JSONArray jsonArray = pJson.getJSONArray(key);
        if (null == jsonArray || jsonArray.size() <= 0) {
            return null;
        }
        JSONObject object = jsonArray.getJSONObject(0);
        if (!object.containsKey("value")) {
            return null;
        }
        return object.getString("value");
    }

    // 保存证书到证书库
    private void saveSiteBySaveCert(Map<String, Object> params, TbSiteEntity site, boolean insertV) {
        logger.info("save_Site_By_Cert ");
        String pem = this.getPemOrKey(params, SiteAttrEnum.SSL_OTHER_CERT_PEM.getName());
        String key = this.getPemOrKey(params, SiteAttrEnum.SSL_OTHER_CERT_KEY.getName());
        if (StringUtils.isBlank(pem) || StringUtils.isBlank(key)) {
            logger.error("save_cert_2_db Invalid SSL certificate");
            return;
        }

        CertSubjectVo vo = HashUtils.readCerSubjectToVo(pem);
        if (null == vo || null == vo.getSubjectDN() || null == vo.getNotAfter()
                || StringUtils.isBlank(vo.getSubjectDN())) {
            logger.error("save_cert_2_db Invalid SSL certificate");
            return;
        }

        if (vo.getSubjectDN().split("=").length < 1) {
            logger.error("save_cert_2_db Invalid SSL certificate host");
            return;
        }
        String sslMainHost = vo.getSubjectDN().split("=")[1];
        TbCertifyObjVo objInfo = new TbCertifyObjVo();
        objInfo.setPem_cert(pem);
        objInfo.setPrivate_key(key);
        objInfo.setNotAfter(vo.getNotAfter().getTime());
        objInfo.setNotBefore(vo.getNotBefore().getTime());
        objInfo.setVersion(vo.getVersion());
        objInfo.setSubjectAlternativeNames(vo.getSubjectAlternativeNames());

        // 去除已经保存的证书
        List<TbCertifyEntity> allCertList = tbCertifyDao
                .selectList(new QueryWrapper<TbCertifyEntity>().eq("common_name", sslMainHost).select("obj_info"));
        boolean existCert = false;
        for (TbCertifyEntity certifyEntity : allCertList) {
            try {
                if (StringUtils.isBlank(certifyEntity.getObjInfo())) {
                    continue;
                }
                TbCertifyObjVo objInfoBfVo = DataTypeConversionUtil.string2Entity(certifyEntity.getObjInfo(),
                        TbCertifyObjVo.class);
                if (null == objInfoBfVo) {
                    continue;
                }
                if (objInfoBfVo.getPem_cert().equals(objInfo.getPem_cert())) {
                    existCert = true;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (false == existCert) {
            if (insertV) {
                TbCertifyEntity certify = new TbCertifyEntity();
                certify.setUserId(site.getUserId());
                certify.setCommonName(sslMainHost);
                certify.setNotAfter(vo.getNotAfter().getTime());
                certify.setObjInfo(DataTypeConversionUtil.entity2jonsStr(objInfo));
                certify.setStatus(TbCertifyStatusEnum.USER.getId());
                certify.setRemark(TbCertifyStatusEnum.USER.getName());
                tbCertifyDao.insert(certify);
            }
        } else {
            logger.info("user SSL certificate exist!");
        }

    }

    // 检测证书合法
    private void verifySignSiteCert(Map<String, Object> params) {
        String pem = this.getPemOrKey(params, SiteAttrEnum.SSL_OTHER_CERT_PEM.getName());
        String key = this.getPemOrKey(params, SiteAttrEnum.SSL_OTHER_CERT_KEY.getName());
        if (StringUtils.isBlank(pem) && StringUtils.isBlank(key)) {
            return;
        }
        if (StringUtils.isBlank(pem) || StringUtils.isBlank(key)) {
            throw new RRException("证书提交不完整");
        }
        if (!SslUtil.getPrivateKeyObject(key).getType().equals("EC PRIVATE KEY")) {
            if (!SslUtil.verifySign(pem, key)) {
                throw new RRException("证书与私钥不匹配!");
            }
        }

    }

    /**
     * 保存属性
     * 
     * @param params
     * @return
     */
    @Override
    public R saveSiteAttr(Map<String, Object> params) {
        // {"siteId":1,"bind_port":"80|443","alias":[{"0":"b.com"},{"12":"a.com"}],"cross":1}
        if (!params.containsKey("siteId")) {
            return R.error("siteId is null");
        }
        JSONObject paramsJson = DataTypeConversionUtil.map2json(params);
        Long userId = null;
        Integer siteId = paramsJson.getInteger("siteId");
        if (paramsJson.containsKey("userId")) {
            userId = paramsJson.getLong("userId");
            paramsJson.remove("userId");
        }
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq("id", siteId)
                .eq(null != userId, "user_id", userId)
                .last("limit 1"));
        if (null == site) {
            return R.error("site is empty");
        }
        this.cleanSiteConfigFile(site.getId());
        try {
            paramsJson.remove("siteId");
        } catch (Exception e) {
            logger.error("remove attr fail:" + e.getMessage());
        }
        int total = paramsJson.size();
        int fail = 0;
        List<String> failMsg = new ArrayList<>();
        for (String key : paramsJson.keySet()) {
            if (StringUtils.isBlank(key)) {
                continue;
            }
            SiteAttrEnum objType = SiteAttrEnum.getObjByName(key);
            if (null == objType) {
                logger.error("[" + key + "]未知属性类型[1]");
                fail++;
                failMsg.add("[" + key + "]未知属性类型[1]");
                continue;
            }
            switch (objType.getType()) {
                case "bool":
                case "int":
                    this.singleSaveAttr(siteId, key, paramsJson.getInteger(key).toString());
                    break;
                case "text":
                    this.singleSaveAttr(siteId, key, paramsJson.getString(key));
                    break;
                case "m_text":
                    JSONArray jsonArray = paramsJson.getJSONArray(key);
                    this.singleMultipleSaveAttr(siteId, key, jsonArray);
                    break;
                case "mm_text":
                    JSONArray jsonArray2 = paramsJson.getJSONArray(key);
                    this.verifySignSiteCert(params);
                    this.multipleMultipleSaveAttr(siteId, key, jsonArray2);
                    break;
                case "l_text":
                    this.longSingleSaveAttr(siteId, key, paramsJson.getString(key));
                    break;
                default:
                    fail++;
                    failMsg.add("[" + key + "]未知属性类型[2]");
                    break;
            }
        }

        // 其它
        // 1 证书保存
        if (params.containsKey(SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                && params.containsKey(SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())) {
            boolean insertV = true;
            if (params.containsKey("ssl_not_insert")) {
                insertV = false;
            }
            this.saveSiteBySaveCert(params, site, insertV);
        }
        if (!params.containsKey("no_need_push")) {
            logger.info("_updateSiteChunk_");
            this.updateSiteChunk(PUSH_SAVE, site);
        }

        // 保存SEO DNS 回源优化
        if (params.containsKey("search_engines_dns_source") || params.containsKey("source_base_info")) {
            R r2 = this.saveSiteSEODns(siteId);
            if (1 != r2.getCode()) {
                logger.error(r2.getMsg());
            }
        }
        if (fail > 0) {
            logger.error(failMsg.toString());
            return R.error("保存失败[" + fail + "]条").put("msg", failMsg);
        }
        return R.ok().put("total", total).put("fail", fail).put("msg", failMsg);
    }

    @Override
    public Integer changeAttrStatus(Long userId, Integer siteId, String pKey, Integer attrId, Integer status) {
        // {"siteId":"2485","pkey":"cache_config","attrId":0}
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId)
                .eq("id", siteId));
        int result = 0;
        if (null == site) {
            throw new RRException("修改失败[无此站]！");
        }
        if (null == SiteAttrEnum.getObjByName(pKey)) {
            throw new RRException("修改失败[位知属性]！");
        }
        this.cleanSiteConfigFile(site.getId());
        String type = SiteAttrEnum.getObjByName(pKey).getType();
        if ("mm_text".equals(type) || "l_text".equals(type)) {
            if (null != attrId && 0 != attrId) {
                // 指定ID 修改
                UpdateWrapper<TbSiteMutAttrEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", attrId).eq("site_id", siteId).eq("pkey", pKey).set("status", status);
                result = tbSiteMutAttrDao.update(null, updateWrapper);
            } else {
                // 没有指定属性ID ,批量修改
                UpdateWrapper<TbSiteMutAttrEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("site_id", siteId).eq("pkey", pKey).set("status", status);
                result = tbSiteMutAttrDao.update(null, updateWrapper);
            }
        } else {
            if (null != attrId && 0 != attrId) {
                // 指定ID 修改
                UpdateWrapper<TbSiteAttrEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", attrId).eq("site_id", siteId).eq("pkey", pKey).set("status", status);
                result = tbSiteAttrDao.update(null, updateWrapper);

            } else {
                // 没有指定属性ID ,批量修改
                UpdateWrapper<TbSiteAttrEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("site_id", siteId).eq("pkey", pKey).set("status", status);
                result = tbSiteAttrDao.update(null, updateWrapper);
            }
        }
        this.updateSiteChunk(PUSH_SAVE, site);
        return result;

    }

    /**
     * @param userId
     * @param siteId
     * @param pKey
     * @param attrId
     * @param opMode 0=置顶; 1=前移一个; -1=后移一个; -999999=置底;
     * @return
     */
    @Override
    public R changeAttrWeight(Long userId, Integer siteId, String pKey, Integer attrId, Integer opMode) {
        boolean haseOp = false;
        int db_index = 0;
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId)
                .eq("id", siteId));
        if (null == site) {
            throw new RRException("修改失败！站点有误！");
        }
        this.cleanSiteConfigFile(site.getId());
        List<Integer> fIdList = new ArrayList<>();
        List<Integer> idList = new ArrayList<>();
        String type = SiteAttrEnum.getObjByName(pKey).getType();
        if ("mm_text".equals(type) || "l_text".equals(type)) {
            List<TbSiteMutAttrEntity> listMM = tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id", siteId)
                    .eq("pkey", pKey)
                    // 2024.1.23 取消限制
                    // .lt("weight",10000)
                    .orderByDesc("weight")
                    .select("id"));
            idList = listMM.stream().map(TbSiteMutAttrEntity::getId).collect(Collectors.toList());
            fIdList.addAll(idList);
            db_index = 1;
        } else {
            List<TbSiteAttrEntity> listMM = tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("site_id", siteId)
                    .eq("pkey", pKey)
                    .orderByDesc("weight")
                    .select("id"));
            idList = listMM.stream().map(TbSiteAttrEntity::getId).collect(Collectors.toList());
            fIdList.addAll(idList);
            db_index = 2;
        }

        if (idList.size() > 0) {
            for (int i = 0; i < idList.size(); i++) {
                if (idList.get(i).equals(attrId)) {
                    if (1 == opMode && i > 0) {
                        // 上移
                        haseOp = true;
                        Integer i_p = idList.get(i - 1);
                        fIdList.set(i, i_p);
                        fIdList.set(i - 1, attrId);
                    } else if (-1 == opMode && i < idList.size() - 1) {
                        // 下移
                        haseOp = true;
                        Integer i_n = idList.get(i + 1);
                        fIdList.set(i, i_n);
                        fIdList.set(i + 1, attrId);
                    } else if (0 == opMode && 0 != i) {
                        // 致顶
                        haseOp = true;
                        List<Integer> buf_id_list = new ArrayList<>();
                        buf_id_list.addAll(fIdList);
                        buf_id_list.remove(attrId);
                        fIdList.clear();
                        fIdList.add(attrId);
                        fIdList.addAll(buf_id_list);
                    }
                }
            }
            if (-999999 == opMode) {
                haseOp = true;
                fIdList.addAll(idList);
                fIdList.add(attrId);
            }
            if (haseOp) {
                for (int i = 0; i < fIdList.size(); i++) {
                    if (1 == db_index) {
                        UpdateWrapper<TbSiteMutAttrEntity> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", fIdList.get(i)).set("weight", fIdList.size() - i);
                        tbSiteMutAttrDao.update(null, updateWrapper);
                    } else if (2 == db_index) {
                        UpdateWrapper<TbSiteAttrEntity> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", fIdList.get(i)).set("weight", fIdList.size() - i);
                        tbSiteAttrDao.update(null, updateWrapper);
                    }

                }
            }
        }

        this.updateSiteChunk(PUSH_SAVE, site);
        return R.ok();
    }

    @Override
    public Integer deleteAttr(Long userId, Integer id, String pkey) {
        int result = 0;
        String type = SiteAttrEnum.getObjByName(pkey).getType();
        if ("mm_text".equals(type) || "l_text".equals(type)) {
            TbSiteMutAttrEntity siteMutAttr = tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("id", id)
                    .eq("pkey", pkey));
            if (null != siteMutAttr) {
                TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                        .eq(null != userId, "user_id", userId)
                        .eq("id", siteMutAttr.getSiteId()));
                if (null != site) {
                    this.cleanSiteConfigFile(site.getId());
                    result = tbSiteMutAttrDao.deleteById(id);
                    this.updateSiteChunk(PUSH_SAVE, site);
                }
            }
        } else {
            TbSiteAttrEntity siteAttr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("id", id)
                    .eq("pkey", pkey));
            if (null != siteAttr) {
                TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                        .eq(null != userId, "user_id", userId)
                        .eq("id", siteAttr.getSiteId()));
                if (null != site) {
                    this.cleanSiteConfigFile(site.getId());
                    result = tbSiteAttrDao.deleteById(id);
                    this.updateSiteChunk(PUSH_SAVE, site);
                }
            }
        }
        return result;
    }

    /**
     * 删除acme申请的证书
     * 
     * @param site
     */
    private void deleteCertifyFile(TbSiteEntity site) {
        TbCertifyEntity certify = tbCertifyDao.selectOne(new QueryWrapper<TbCertifyEntity>()
                .eq("user_id", site.getUserId()).eq("common_name", site.getMainServerName()).last("limit 1"));
        if (null != certify) {
            if (StringUtils.isNotBlank(certify.getCommonName())) {
                String certFilePath = String.format("%s/%s", AcmeShUtils.getAcmeRootDir(), certify.getCommonName());
                logger.warn("[deleteCertifyFile][certFilePath]=" + certFilePath);
                File file = new File(certFilePath);
                if (file.exists()) {
                    ShellUtils.runShell("rm -rf " + certFilePath, false);
                }
            }
            tbCertifyDao.deleteById(certify.getId());
        } else {
            // logger.debug("siteId="+site.getId()+",certify is null");
        }
    }

    private void sendDeleteNodeSiteFile(TbSiteEntity site, String modes) {
        // delFileMode=2
        Map<String, String> pushMap = new HashMap<>(8);
        pushMap.put(PushTypeEnum.CLEAN_DEL_SITE.getName(), site.getId().toString());
        Object o = cdnMakeFileService.pushByInputInfo(pushMap);
        if (null != o) {
            logger.info(o.toString());
        }
    }

    @Override
    public Integer batDeleteSite(Long userId, String siteIds) {
        String[] ids = siteIds.split(",");
        int success = 0;

        for (String id : ids) {
            try {
                TbSiteEntity site = tbSiteDao
                        .selectOne(new QueryWrapper<TbSiteEntity>().eq(null != userId, "user_id", userId).eq("id", id));
                if (null == site) {
                    // logger.debug("["+id+"]无此站点");
                    continue;
                }
                if (QuerySysAuth.FORBID_FREE_CONSUME_DEL_SITES_FLAG && null != userId) {
                    CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, site.getSerialNumber(), false,
                            true);
                    if (null != suitObj && null != suitObj.getProductEntity()
                            && StringUtils.isNotBlank(suitObj.getProductEntity().getProductJson())) {
                        OrderCdnProductVo ovo = DataTypeConversionUtil
                                .string2Entity(suitObj.getProductEntity().getProductJson(), OrderCdnProductVo.class);
                        if (null != ovo) {
                            if (null != ovo.getY() && 1 == ovo.getY().getStatus() && 0 == ovo.getY().getValue()) {
                                throw new RRException("免费套餐不可删除域名!");
                            }
                            if (null != ovo.getS() && 1 == ovo.getS().getStatus() && 0 == ovo.getS().getValue()) {
                                throw new RRException("免费套餐不可删除域名!");
                            }
                            if (null != ovo.getM() && 1 == ovo.getM().getStatus() && 0 == ovo.getM().getValue()) {
                                throw new RRException("免费套餐不可删除域名!");
                            }
                        }
                    }
                }
                // del local-global-data
                this.cleanSiteConfigFile(site.getId());

                // 1 send del redis conf
                this.sendDeleteNodeSiteFile(site, "all");

                // //2 删除site mysql attr
                // QueryWrapper<TbSiteAttrEntity> wrapper1=new QueryWrapper<>();
                // wrapper1.eq("site_id",id);
                // tbSiteAttrDao.delete(wrapper1);
                // //3 删除site mysql mut attr
                // QueryWrapper<TbSiteMutAttrEntity> wrapper2=new QueryWrapper<>();
                // wrapper2.eq("site_id",id);
                // tbSiteMutAttrDao.delete(wrapper2);
                // //4删除证书 cert file
                this.deleteCertifyFile(site);
                //
                // //5 删除site mysql
                // tbSiteDao.deleteById(id);
                // del dns
                this.delSiteCusDns(site.getId());

                site.setStatus(2);
                tbSiteDao.updateById(site);
                success++;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return success;
    }

    private ElkServerVo getElkConfigInfo() {
        TbCdnPublicMutAttrEntity publicMutAttr = publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey", PublicEnum.ELK_CONFIG.getName())
                .eq("status", 1)
                .last("limit 1"));
        if (null == publicMutAttr || StringUtils.isBlank(publicMutAttr.getPvalue())) {
            logger.info("ELK_CONFIG is null ");
            return null;
        }
        return DataTypeConversionUtil.string2Entity(publicMutAttr.getPvalue(), ElkServerVo.class);
    }

    @Override
    public R queryElk(String method, String path, String param) {
        ElkServerVo vo = getElkConfigInfo();
        if (null == vo) {
            return R.error("未配置elk");
        }
        if (vo.getMethod().equals("http")) {
            // String url=
            // jsonObj.get(DnsGlobalConfigEnum.ELASTICSEARCH_METHOD.getConfigKey())+"://"+jsonObj.get(DnsGlobalConfigEnum.ELASTICSEARCH_HOST.getConfigKey())+":"+jsonObj.get(DnsGlobalConfigEnum.ELASTICSEARCH_PORT.getConfigKey())+uri
            // ;
            String url = "http://" + vo.getHost() + ":" + vo.getPort() + "/" + path;
            return R.ok().put("data", HttpRequest.erkHttp(method, url, vo.getPwd(), param));
        } else if (vo.getMethod().equals("https")) {
            String url = "https://" + vo.getHost() + ":" + vo.getPort() + "/" + path;
            return R.ok().put("data", HttpRequest.erkHttpS(url, vo.getPwd(), vo.getCaPath(), param));
        } else {
            return R.error("未知请求方式：" + vo.getMethod());
        }
    }

    private HttpHostTypeVo getSiteIdByHttpHost(Long userId, String httpHost) {
        HttpHostTypeVo resultVo = new HttpHostTypeVo();
        resultVo.setUserId(userId);
        resultVo.setHttpHost(httpHost);
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId)
                .eq("main_server_name", httpHost)
                .last("limit 1"));
        if (null != site) {
            resultVo.setType(1);
            resultVo.setSiteId(site.getId());
            return resultVo;
        }
        TbSiteAttrEntity aliasAttr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey", SiteAttrEnum.ALIAS.getName())
                .select("pvalue")
                .eq("status", 1)
                .last("limit 1"));
        if (null != aliasAttr) {
            resultVo.setType(2);
            resultVo.setSiteId(aliasAttr.getSiteId());
            return resultVo;
        }
        return resultVo;
    }

    @Override
    public List<PurgeVo> parsePurgeCacheUrl(Long userId, String urls) {
        List<PurgeVo> resultVoList = new ArrayList<>();

        final String urlPathPattern = "^http[s]?://[^/]*.*";
        Pattern rUrl = Pattern.compile(urlPathPattern);
        final String serverPattern = "://[^/]*";
        Pattern rServerName = Pattern.compile(serverPattern);
        String[] ursArray = urls.split("\\n");
        Map<String, NgxPurgeCacheVo> cacheMap = new HashMap<>();
        for (String url : ursArray) {
            Matcher mUrl = rUrl.matcher(url);
            if (!mUrl.matches()) {
                // logger.debug(url+" is not a normal url ");
                continue;
            }
            Matcher mServer = rServerName.matcher(url);
            if (!mServer.find()) {
                // logger.debug(url+" get server fail 1 ");
                continue;
            }
            String hostG0 = mServer.group();
            String server = hostG0.replace("://", "");
            if (StringUtils.isBlank(server)) {
                // logger.debug(url+" get server fail 2 ");
                continue;
            }
            HttpHostTypeVo resultVo = this.getSiteIdByHttpHost(userId, server);
            TbSiteAttrEntity attr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("site_id", resultVo.getSiteId())
                    .eq("pkey", SiteAttrEnum.PERFORMANCE_CACHE_IGNORE_URL_PARAM.getName())
                    .last("limit 1"));
            String schema = "http";
            if (url.startsWith("https://")) {
                schema = "https";
            }
            if (!cacheMap.containsKey(server)) {
                NgxPurgeCacheVo vo = new NgxPurgeCacheVo();
                vo.setServerName(server);
                vo.setSchema(schema);
                if (null != attr && StringUtils.isNotBlank(attr.getPvalue())) {
                    vo.setIgnoreUrlParamFlag(Integer.parseInt(attr.getPvalue()));
                }
                cacheMap.put(server, vo);
            }
            String pullUrl = url.substring(url.indexOf(hostG0) + hostG0.length());
            if (StringUtils.isBlank(pullUrl)) {
                continue;
            }
            cacheMap.get(server).getUrlList().add(pullUrl);
            String pPullUrl = pullUrl.replace(".", "\\.");
            pPullUrl = pPullUrl.replace("*", ".*");
            // logger.info("pPullUrl:"+pPullUrl);
            cacheMap.get(server).getUrlPatternList().add(Pattern.compile(pPullUrl));
        }
        for (String serverName : cacheMap.keySet()) {
            // ELK获取所有SERVER URL;构造CMD
            HttpHostTypeVo resultVo = this.getSiteIdByHttpHost(userId, serverName);
            PurgeVo vo = new PurgeVo();
            StringBuilder cleanCmds = new StringBuilder();
            vo.setServerName(serverName);
            logger.info("parse_Purge_CacheUrl Server: " + serverName);
            for (String url : this.getServerUrlHistoryByElk(resultVo)) {
                for (Pattern cUrlPattern : cacheMap.get(serverName).getUrlPatternList()) {
                    if (cUrlPattern.matcher(url).find()) {
                        String purUrl = url;
                        if (1 == cacheMap.get(serverName).getIgnoreUrlParamFlag()) {
                            Integer argIndex = url.indexOf("?");
                            if (-1 != argIndex) {
                                purUrl = purUrl.substring(0, argIndex);
                            }
                        }
                        cleanCmds.append(String.format("%s \n", purUrl));
                        // cleanCmds.append(String.format("curl -H 'Host:%s' http://127.0.0.1/purge1%s
                        // \n",serverName,purUrl)) ;
                    }
                }
            }
            if (cleanCmds.length() > 0) {
                vo.setUrls(cleanCmds.toString());
                resultVoList.add(vo);
            }

        }
        return resultVoList;
    }

    private List<String> getServerUrlHistoryByElk(HttpHostTypeVo resultVo) {
        List<String> list = new ArrayList<>();
        R r1 = this.queryElk("GET", "filebeat-*/_search",
                "{\"size\":0,\"aggs\":{\"unique_uris\":{\"cardinality\":{\"field\":\"k_uri\"}}}}");
        if (null == r1 || 1 != r1.getCode()) {
            logger.error(r1.toJsonString());
            return list;
        } else {
            logger.info("queryElk kUrl:" + r1.toJsonString());
        }
        ElkFilterTotalVo tVo = DataTypeConversionUtil.string2Entity(r1.get("data").toString(), ElkFilterTotalVo.class);
        if (null == tVo) {
            logger.error("Filter total is null:" + r1.toJsonString());
            return list;
        }
        int urlSum = tVo.getFilterSum();
        if (0 == urlSum) {
            logger.error("Filter url sum is 0");
            return list;
        }
        if (urlSum > 65536) {
            urlSum = 65535;
        }
        String param = String.format(
                "{\"size\":0,\"aggs\":{\"uri_total\":{\"filter\":{\"term\":{\"k_host\":\"%s\"}},\"aggs\":{\"total\":{\"terms\":{\"field\":\"k_uri\",\"size\":%d}}}}}}",
                resultVo.getHttpHost(), urlSum);
        if (2 == resultVo.getType()) {
            param = String.format(
                    "{\"size\":0,\"aggs\":{\"uri_total\":{\"filter\":{\"term\":{\"http_host\":\"%s\"}},\"aggs\":{\"total\":{\"terms\":{\"field\":\"k_uri\",\"size\":%d}}}}}}",
                    resultVo.getHttpHost(), urlSum);
        }
        R r2 = this.queryElk("GET", "filebeat-*/_search", param);
        if (1 != Integer.parseInt(r2.get("code").toString())) {
            logger.error("queryElk fail:" + r2.toJsonString());
            return list;
        }
        ElkFilterUriVo vo = DataTypeConversionUtil.string2Entity(r2.get("data").toString(), ElkFilterUriVo.class);
        list.addAll(vo.getKeyList());
        logger.info("get_ServerUrlHistoryByElk len:" + list.size());
        if (0 == list.size()) {
            logger.info(r2.toJsonString());
        }
        return list;
    }

    // 暂停作用
    public Map getInterceptreSultV0(String nodeIp, String serverName, String sourceIp, String interceptMode,
            String date, Integer page, Integer limit) {
        // {"interceptMode":"20*","date":"*","nodeIp":"**","sourceIp":"*171.44.122.129*","serverName":"**","page":1,"limit":50}
        // wafdata:*:*:*171.44.122.129**:20*:*:*
        // wafdata:119.97.137.47:waf.antsxdp.com:171.44.122.129:301:20220808153830:1
        // wafdata:nodeip:servername:souceIp:mode:date:index
        String pKey = String.format("wafdata:%s:%s:%s:%s:%s:*", nodeIp, serverName, sourceIp, interceptMode, date);
        Map r_map = redisUtils.findKeysForPage(pKey, page, limit);
        if (r_map.containsKey("data")) {
            List<String> list_s = (List<String>) r_map.get("data");
            List<JSONObject> r_list = new ArrayList<>();
            for (String k : list_s) {
                String v = redisUtils.get(k);
                int i = k.indexOf(":", "wafdata:".length());
                String t = k.substring("wafdata:".length(), i);
                if (StringUtils.isNotBlank(v)) {
                    JSONObject obj = DataTypeConversionUtil.string2Json(v);
                    if (null != obj) {
                        obj.put("t", t);
                        r_list.add(obj);
                    }

                }
            }
            r_map.put("data", r_list);
        }
        return r_map;
    }

    @Override
    public Map getInterceptResult(String nodeIp, String serverName, String sourceIp, String interceptMode, String date,
            Integer page, Integer limit) {
        // {"interceptMode":"20*","date":"*","nodeIp":"**","sourceIp":"*171.44.122.129*","serverName":"**","page":1,"limit":50}
        // wafdata:*:*:*171.44.122.129**:20*:*:*
        // wafdata:119.97.137.47:waf.antsxdp.com:171.44.122.129:301:20220808153830:1
        // wafdata:nodeIp:servername:souceIp:mode:date:index
        Map resultMap = new HashMap();
        String pKey = String.format("wafdata:%s:%s:%s:%s:%s:*", nodeIp, serverName, sourceIp, interceptMode, date);
        Set<String> all_set = redisUtils.scanAll(pKey);
        if (null == all_set) {
            return null;
        }
        Set<String> sortSet = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] s1 = o1.split(":");
                String[] s2 = o2.split(":");
                if (s1.length >= 7 && s2.length >= 7) {
                    String s1Date = s1[s1.length - 2] + "--" + Arrays.toString(s1);
                    String s2Date = s2[s2.length - 2] + "--" + Arrays.toString(s2);
                    return s2Date.compareTo(s1Date);
                }
                return o2.compareTo(o1);
            }
        });
        sortSet.addAll(all_set);
        List<JSONObject> r_list = new ArrayList<>();
        int i = 0;
        for (String k : sortSet) {
            if (i >= (page - 1) * limit && i < page * limit) {
                String v = redisUtils.get(k);
                if (StringUtils.isNotBlank(v)) {
                    String[] ss = k.split(":");
                    if (ss.length >= 7) {
                        String t_nodeIP = ss[1];
                        String d_date = ss[ss.length - 2];
                        JSONObject obj = DataTypeConversionUtil.string2Json(v);
                        if (null != obj) {
                            obj.put("t", t_nodeIP);
                            obj.put("date", d_date);
                            obj.put("key", k);
                            r_list.add(obj);
                        }
                    }

                }
            }
            i++;
        }
        resultMap.put("data", r_list);
        resultMap.put("page", page);
        resultMap.put("limit", limit);
        resultMap.put("total", all_set.size());
        return resultMap;
    }

    /**
     * 推送清理缓存
     * 
     * @param userId
     * @param urls
     * @param ips
     * @return
     */
    @Override
    public Integer PullCache(Long userId, String urls, String ips) {
        // http://waf.antsxdp.com/download,http://t.test.antxdpm.cn/public/css/
        Integer success = 0;
        String pattern = "://[^\\s^/]*";
        Pattern r = Pattern.compile(pattern);
        // String[] ipList=ips.split(",");
        for (String url : urls.split(",")) {
            Matcher m = r.matcher(url);
            if (m.find()) {
                String sName = m.group(0);
                sName = sName.replace("://", "");
                Long count = tbSiteDao.selectCount(new QueryWrapper<TbSiteEntity>()
                        .eq(null != userId, "user_id", userId).eq("main_server_name", sName));
                if (count > 0) {
                    success++;
                    redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "cache", url);
                }
            }
        }
        return success;
    }

    /**
     * 更新数据库后 做数据更新处理
     */
    @Override
    public void reInitSiteAttr() {
        // AREA 2023 03 24 添加pvalue1 pvalue2
        // SOURCE_BASE_INFO "{\"pvalue1\":\"listen\",\"pvalue2\":\"areaId\"}"
        try {
            List<TbSiteMutAttrEntity> list = tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                    .and(q -> q.eq("pvalue1", 0).or().isNull("pvalue1")));
            for (TbSiteMutAttrEntity mutAttr : list) {
                this.updateAdditionalAttr(null, mutAttr);
                tbSiteMutAttrDao.updateById(mutAttr);
            }

            // 更新pType
            List<TbSiteAttrEntity> list1 = tbSiteAttrDao
                    .selectList(new QueryWrapper<TbSiteAttrEntity>().isNull("p_type").select("id,pkey,p_type"));
            for (TbSiteAttrEntity siteAttr : list1) {
                String pType = SiteAttrEnum.getTypeByName(siteAttr.getPkey());
                if (StringUtils.isNotBlank(pType)) {
                    tbSiteAttrDao.update(null, new UpdateWrapper<TbSiteAttrEntity>()
                            .eq("id", siteAttr.getId())
                            .set("p_type", pType));
                }
            }

            List<TbSiteMutAttrEntity> list2 = tbSiteMutAttrDao
                    .selectList(new QueryWrapper<TbSiteMutAttrEntity>().isNull("p_type").select("id,pkey,p_type"));
            for (TbSiteMutAttrEntity siteMutAttr : list2) {
                String pType = SiteAttrEnum.getTypeByName(siteMutAttr.getPkey());
                if (StringUtils.isNotBlank(pType)) {
                    tbSiteMutAttrDao.update(null, new UpdateWrapper<TbSiteMutAttrEntity>()
                            .eq("id", siteMutAttr.getId())
                            .set("p_type", pType));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            ;
        }

    }

    @Override
    public R querySiteGroupList(Long userId, QuerySiteGroupForm form) {
        StaticVariableUtils.siteIdGroupNameMap.clear();
        IPage<TbSiteGroupEntity> siPage = tbSiteGroupDao.selectPage(
                new Page<>(form.getPage(), form.getLimit()),
                new QueryWrapper<TbSiteGroupEntity>()
                        .eq(null != userId, "user_id", userId)
                        .eq(null != userId, "create_user_type", UserTypeEnum.USER_TYPE.getId())
                        .orderByDesc("weight")
                        .like(StringUtils.isNotBlank(form.getName()), "name", form.getName()));
        siPage.getRecords().forEach(item -> {
            item.setSiteList(new ArrayList<>());
            if (StringUtils.isNotBlank(item.getSiteIds())) {
                for (String id : item.getSiteIds().split(",")) {
                    if (StringUtils.isBlank(id)) {
                        continue;
                    }
                    TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                            .eq("user_id", item.getUserId()).eq("id", id).last("limit 1"));
                    if (null != site) {
                        item.getSiteList().add(site);
                    }
                }
            }
        });
        return R.ok().put("data", new PageUtils(siPage));
    }

    @Override
    public R saveSiteGroupList(Long userId, TbSiteGroupEntity tbSiteGroup) {
        ValidatorUtils.validateEntity(tbSiteGroup);
        if (null == tbSiteGroup.getId() || 0 == tbSiteGroup.getId()) {
            // insert
            tbSiteGroup.setUserId(userId);
            tbSiteGroup.setCreateUserType(UserTypeEnum.USER_TYPE.getId());
            tbSiteGroupDao.insert(tbSiteGroup);
        } else {
            tbSiteGroup.setUserId(userId);
            tbSiteGroup.setCreateUserType(UserTypeEnum.USER_TYPE.getId());
            tbSiteGroupDao.updateById(tbSiteGroup);
            // save
        }
        cdnMakeFileService.deleteCacheByKey(CacheKeyEnums.cert_apply_proxy_pass.getKeyName());
        return R.ok();
    }

    @Override
    public R deleteSiteGroup(Long userId, DeleteIdsForm form) {
        if (StringUtils.isNotBlank(form.getIds())) {
            for (String id : form.getIds().split(",")) {
                TbSiteGroupEntity tbSiteGroup = tbSiteGroupDao.selectOne(new QueryWrapper<TbSiteGroupEntity>()
                        .eq("user_id", userId)
                        .eq("id", id)
                        .last("limit 1")
                        .select("id"));
                if (null != tbSiteGroup) {
                    tbSiteGroupDao.deleteById(id);
                }
            }
        }
        cdnMakeFileService.deleteCacheByKey(CacheKeyEnums.cert_apply_proxy_pass.getKeyName());
        return R.ok();
    }

    /**
     * 生成系统内置WAF规则
     * 
     * @param userId
     * @param form
     * @return
     */
    @Override
    public R buildSysWafRule(Long userId, SysCreateWafRuleForm form) {
        String eMsg = "";
        try {
            TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                    .eq(null != userId, "user_id", userId)
                    .eq("id", form.getSiteId())

            );
            if (null == site) {
                return R.error("siteId error!");
            }
            // 删除当前站所有已生成的系统规则
            tbSiteMutAttrDao.delete(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id", form.getSiteId())
                    .eq("pkey", SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                    .eq("pvalue1", 1));
            // 生成添加系统规则
            List<SysWafRuleVo> list = RegExUtils.getSysWafList(form, 1);
            if (null == list) {
                this.updateSiteChunk(PUSH_SAVE, site);
                return R.ok();
            }
            // logger.info(list.toString());
            for (SysWafRuleVo vo : list) {
                TbSiteMutAttrEntity mutAttr = new TbSiteMutAttrEntity();
                mutAttr.setSiteId(form.getSiteId());
                mutAttr.setPkey(SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName());
                mutAttr.setPvalue(DataTypeConversionUtil.entity2jonsStr(vo));
                mutAttr.setPvalue1(1);
                mutAttr.setWeight(999999);
                mutAttr.setPType(SiteAttrEnum.getTypeByName(mutAttr.getPkey()));
                tbSiteMutAttrDao.insert(mutAttr);
            }
            this.updateSiteChunk(PUSH_SAVE, site);
            return R.ok().put("data", list);
        } catch (Exception e) {
            eMsg = e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    @Override
    public R buildAiWafModelling(BuildAiModellingForm form) {
        if (!StaticVariableUtils.exclusive_modeList.contains("ai_waf")) {
            return R.error("功能暂不可用！");
        }
        TbCdnPublicMutAttrEntity publicMutAttr = publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey", PublicEnum.ELK_CONFIG.getName())
                .last("limit 1"));
        if (null == publicMutAttr) {
            return R.error("未配置elk");
        }
        ElkServerVo vo = DataTypeConversionUtil.string2Entity(publicMutAttr.getPvalue(), ElkServerVo.class);
        if (null == vo) {
            return R.error("配置elk错误");
        }

        String authStr = String.format("_%s_%s_%s_", vo.getHost(), vo.getPort(), vo.getPwd());
        String authCode = HashUtils.md5ofString(authStr);
        String currentDirectory = System.getProperty("user.dir");
        String ants_ai_wafPath = currentDirectory + "/ants_ai_waf";
        String ants_ai_waf_model_Path = currentDirectory + "/ai_waf_model.bin";
        FileUtils.addModeX(ants_ai_wafPath);
        String cmd = String.format("%s -m %d -eh %s -ep %s -es %s -ac %s -bs \"%s\" -be \"%s\" -bm %d", ants_ai_wafPath,
                form.getMode(), vo.getHost(), vo.getPort(), vo.getPwd(), authCode, form.getStartTime(),
                form.getEndTime(), form.getMaxCount());
        logger.info(cmd);
        List<String> list = ShellUtils.runShell(cmd, true);
        logger.info("file byte set " + ants_ai_waf_model_Path);
        if (list.toString().contains("model complete")) {
            redisUtils.byteSet(PushSetEnum.AI_MODEL_CATBOOST_BIN.getTemplatePath(), ants_ai_waf_model_Path);
            Map pushMap = new HashMap(4);
            pushMap.put(PushTypeEnum.AI_MODEL_PUSH.getName(), "");
            cdnMakeFileService.pushByInputInfo(pushMap);
            return R.ok().put("data", list);
        } else {
            return R.error("编译模型失败！").put("data", list);
        }

    }

    @Override
    public TbSiteEntity getSiteEntityStatus(Long userId, Integer siteId) {
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq(null != userId, "user_id", userId)
                .eq("id", siteId)
                .select("status")
                .last("limit 1"));
        return site;
    }

    @Override
    public R batchUpdateSiteAttr(Long userId, BatchModifySiteAttrForm form) {
        if (null == form.getList() || form.getList().isEmpty()) {
            return R.error("list 不能为空！");
        }
        for (String siteId : form.getSiteIds().split(",")) {
            TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                    .eq(null != userId, "user_id", userId)
                    .eq("id", siteId)
                    .select("id,main_server_name")
                    .last("limit 1"));
            if (null == site) {
                continue;
            }
            this.cleanSiteConfigFile(site.getId());
            for (BatchModifySiteAttrForm.KvObj kv : form.getList()) {
                String sitePkey = kv.getKey();
                String pKeyType = SiteAttrEnum.getTypeByName(sitePkey);
                String[] sTypes = { "int", "bool", "text" };
                String[] lTypes = { "l_text" };
                // String[] mmTypes={"mm_text","m_text"};
                if (Arrays.asList(sTypes).contains(pKeyType)) {
                    TbSiteAttrEntity siteAttr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("pkey", sitePkey)
                            .eq("site_id", siteId)
                            .last("limit 1"));
                    if (null == siteAttr) {
                        siteAttr = new TbSiteAttrEntity();
                        siteAttr.setSiteId(Integer.parseInt(siteId));
                        siteAttr.setPkey(sitePkey);
                        siteAttr.setPvalue(kv.getValue());
                        siteAttr.setPType(SiteAttrEnum.getTypeByName(sitePkey));
                        tbSiteAttrDao.insert(siteAttr);
                    } else {
                        siteAttr.setPvalue(kv.getValue());
                        tbSiteAttrDao.updateById(siteAttr);
                    }
                } else if (Arrays.asList(lTypes).contains(pKeyType)) {
                    TbSiteMutAttrEntity siteLAttr = tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("pkey", sitePkey)
                            .eq("site_id", siteId)
                            .last("limit 1"));
                    if (null == siteLAttr) {
                        siteLAttr = new TbSiteMutAttrEntity();
                        siteLAttr.setSiteId(Integer.parseInt(siteId));
                        siteLAttr.setPkey(sitePkey);
                        siteLAttr.setPvalue(kv.getValue());
                        siteLAttr.setPType(SiteAttrEnum.getTypeByName(sitePkey));
                        tbSiteMutAttrDao.insert(siteLAttr);
                    } else {
                        siteLAttr.setPvalue(kv.getValue());
                        tbSiteMutAttrDao.updateById(siteLAttr);
                    }
                } else if ("m_text".equals(pKeyType)) {
                    if (kv.getValue().startsWith("[") && kv.getValue().endsWith("]")) {
                        // 删除后新增
                        tbSiteAttrDao.delete(new QueryWrapper<TbSiteAttrEntity>()
                                .eq("pkey", sitePkey)
                                .eq("site_id", siteId));
                        JSONArray jsonArray = DataTypeConversionUtil.string2JsonArray(kv.getValue());
                        for (int i = 0; i < jsonArray.size(); i++) {
                            Object o = jsonArray.get(i);
                            TbSiteAttrEntity siteAttr = new TbSiteAttrEntity();
                            siteAttr.setSiteId(Integer.parseInt(siteId));
                            siteAttr.setPkey(sitePkey);
                            siteAttr.setPType(SiteAttrEnum.getTypeByName(sitePkey));
                            if (o.getClass().getSimpleName().equals("String")) {
                                siteAttr.setPvalue(o.toString());
                            } else if (o.getClass().getSimpleName().equals("JSONObject")) {
                                JSONObject jo = (JSONObject) o;
                                siteAttr.setPvalue(jo.toJSONString());
                            } else {
                                siteAttr.setPvalue(o.toString());
                            }
                            tbSiteAttrDao.insert(siteAttr);
                        }
                    }

                } else if ("mm_text".equals(pKeyType)) {
                    if (kv.getValue().startsWith("[") && kv.getValue().endsWith("]")) {
                        tbSiteMutAttrDao.delete(new QueryWrapper<TbSiteMutAttrEntity>()
                                .eq("pkey", sitePkey)
                                .eq("site_id", siteId));
                        JSONArray jsonArray = DataTypeConversionUtil.string2JsonArray(kv.getValue());
                        for (int i = 0; i < jsonArray.size(); i++) {
                            Object o = jsonArray.get(i);
                            TbSiteMutAttrEntity siteLAttr = new TbSiteMutAttrEntity();
                            siteLAttr.setSiteId(Integer.parseInt(siteId));
                            siteLAttr.setPkey(sitePkey);
                            siteLAttr.setPType(SiteAttrEnum.getTypeByName(sitePkey));
                            if (o.getClass().getSimpleName().equals("String")) {
                                siteLAttr.setPvalue(o.toString());
                            } else if (o.getClass().getSimpleName().equals("JSONObject")) {
                                JSONObject jo = (JSONObject) o;
                                siteLAttr.setPvalue(jo.toJSONString());
                            } else {
                                siteLAttr.setPvalue(o.toString());
                            }
                            tbSiteMutAttrDao.insert(siteLAttr);
                        }
                    }
                } else if (pKeyType.equals("cmd") && sitePkey.equals(SiteAttrEnum.CMD_CLEAN_CACHE.getName())) {
                    // 批量清理缓存
                    // {urls: "http://ahalimu.publicvm.com/*"}
                    // {"urls":"http://ahalimu.publicvm.com/*\nhttp://ahalimu2.publicvm.com/*"}
                    // String f_cmds= tbSiteServer.parse_PurgeCacheUrl(userId,urls);
                    // (f_cmds);
                    if (StringUtils.isNotBlank(site.getMainServerName())) {
                        String urls = String.format("http://%s/*", site.getMainServerName());
                        cdnMakeFileService.pushPurgeCache(parsePurgeCacheUrl(userId, urls));
                    }
                } else {
                    logger.error("unknown type:" + DataTypeConversionUtil.entity2jonsStr(kv));
                }
            }

        }

        Map pushMap = new HashMap();
        pushMap.put(PushTypeEnum.SITE_CONF.getName(), form.getSiteIds());
        cdnMakeFileService.pushByInputInfo(pushMap);
        return R.ok();
    }

    @Override
    public R getSiteInfo(Long userId, String name) {
        TbSiteEntity tbSite = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq(null != userId, "user_id", userId)
                .eq("main_server_name", name).last("limit 1"));
        if (null != tbSite) {
            return R.ok().put("data", tbSite);
        }
        TbSiteAttrEntity attr = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey", SiteAttrEnum.ALIAS.getName()).eq("pvalue", name).last("limit 1"));
        if (null != attr) {
            tbSite = tbSiteDao.selectById(attr.getSiteId());
            if (null != tbSite) {
                return R.ok().put("data", tbSite);
            }
        }
        return R.error("无此站");
    }

    /**
     * 获取用户免费CDN套餐
     * 
     * @param userId
     * @return
     */
    private String getFreeCdnSuit(Long userId) {
        CdnProductEntity product = cdnProductDao.selectOne(new QueryWrapper<CdnProductEntity>()
                .eq("status", ProductStatusEnum.ONLY_FIRST.getId())
                .eq("product_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                .orderByDesc("id")
                .last("limit 1"));
        if (null == product) {
            return null;
        }
        TbOrderEntity order = tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("user_id", userId)
                .eq("order_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                .eq("target_id", product.getId())
                .orderByDesc("id")
                .last("limit 1"));
        if (null == order) {
            return null;
        }
        return order.getSerialNumber();
    }

    @Override
    public R testCreateSite(Long userId, String mainServerName, String sProtocol, int newUserFlag) {
        int maxFailCount = 15;
        String freeSn = "";
        while (maxFailCount > 0) {
            maxFailCount--;
            freeSn = this.getFreeCdnSuit(userId);
            if (StringUtils.isNotBlank(freeSn)) {
                break;
            } else {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {

                }
            }
        }
        if (StringUtils.isBlank(freeSn)) {
            return R.error("创建站点失败！【无体验套餐】");
        }
        String siteDomain = "";
        String sIp = null;
        String sPort = null;
        String[] dInfos = mainServerName.split("\\|");
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

        TbSiteEntity site = this.createSite(userId, freeSn, mainDomain, alias, sIp, sPort, sProtocol);
        // tbLogService.FrontUserWriteLog(userId,
        // LogTypeEnum.OPERATION_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        return R.ok().put("data", site).put("newUserFlag", newUserFlag);
    }

    @Override
    public R batchSearchUpdateSiteAttr(Long userId, BatchSearchModifySiteAttrForm form) {
        if ("all".equals(form.getSiteIds())) {
            List<TbSiteEntity> list = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>().select("id"));
            List<String> strList = list.stream().map(o -> o.getId().toString()).collect(Collectors.toList());
            String ids = String.join(",", strList);
            form.setSiteIds(ids);
        }
        final int[] findSum = { 0 };
        for (String siteId : form.getSiteIds().split(",")) {
            TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                    .eq(null != userId, "user_id", userId)
                    .eq("id", siteId)
                    .select("id")
                    .last("limit 1"));
            if (null == site) {
                continue;
            }
            this.cleanSiteConfigFile(site.getId());
            String siteKey = BatchSearchUpdateSiteAttrEnum.getSitePkeyByKey(form.getKey());
            switch (siteKey) {
                case "source_base_info":
                    if (true) {
                        List<TbSiteMutAttrEntity> list = tbSiteMutAttrDao
                                .selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                                        .eq("site_id", siteId)
                                        .eq("pkey", "source_base_info")
                                        .eq("status", 1));
                        for (TbSiteMutAttrEntity mutAttr : list) {
                            NgxSourceBaseInfoVo vo = DataTypeConversionUtil.string2Entity(mutAttr.getPvalue(),
                                    NgxSourceBaseInfoVo.class);
                            if (null == vo) {
                                continue;
                            }
                            final boolean[] existUpdateFlag = { false };
                            if ("b_source_ip".equals(form.getKey()) || "b_source_ip_port".equals(form.getKey())) {
                                if (null == vo.getLine()) {
                                    continue;
                                }
                                vo.getLine().forEach(lineVo -> {
                                    if ("b_source_ip".equals(form.getKey())) {
                                        if (lineVo.getIp().equals(form.getS_value())) {
                                            existUpdateFlag[0] = true;
                                            findSum[0]++;
                                            if (IPUtils.isValidIPV4(form.getT_value())) {
                                                lineVo.setIp(form.getT_value());
                                            }
                                        }
                                    } else if ("b_source_ip_port".equals(form.getKey())) {
                                        String sIpPort = lineVo.getIp() + ":" + lineVo.getPort();
                                        if (sIpPort.equals(form.getS_value())) {
                                            findSum[0]++;
                                            String[] ipPorts = form.getT_value().split(":");
                                            if (2 == ipPorts.length) {
                                                existUpdateFlag[0] = true;
                                                lineVo.setIp(ipPorts[0]);
                                                lineVo.setPort(ipPorts[1]);
                                            }
                                        }
                                    }
                                });
                            } else if ("b_source_listen".equals(form.getKey())) {
                                if (vo.getPort().equals(form.getS_value())) {
                                    existUpdateFlag[0] = true;
                                    findSum[0]++;
                                    vo.setPort(Integer.parseInt(form.getT_value()));
                                }
                            }
                            if (0 == form.getTest() && existUpdateFlag[0]) {
                                mutAttr.setPvalue(DataTypeConversionUtil.entity2jonsStr(vo));
                                tbSiteMutAttrDao.updateById(mutAttr);
                                Map pushMap = new HashMap();
                                pushMap.put(PushTypeEnum.SITE_CONF.getName(), String.valueOf(mutAttr.getSiteId()));
                                cdnMakeFileService.pushByInputInfo(pushMap);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return R.ok().put("findSum", findSum[0]);
    }

    @Override
    public R checkSiteName(String name) {
        boolean ret = inputAvailableService.checkNginxServerNameAndAliasIsValid("site", 0, name);
        return R.ok().put("data", ret).put("tips", "true:域名可用;false:域名不可用，系统存在此域名");
    }

    @Data
    private class SiteDnsConfigVo {
        private String top;
        private TbDnsConfigEntity dnsConfig;
    }

    // 1=add 2=save 3=delete
    private SiteDnsConfigVo getSiteDnsApiConf(Integer siteId, int mode) {
        try {
            TbSiteEntity siteEntity = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id", siteId)
                    .select("main_server_name,serial_number").last("limit 1"));
            if (null == siteEntity) {
                return null;
            }
            if (StringUtils.isBlank(siteEntity.getSerialNumber())) {
                return null;
            }
            if (1 == mode || 2 == mode) {
                TbSiteAttrEntity siteAttrEntity = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                        .eq("site_id", siteId).eq("pkey", SiteAttrEnum.SEARCH_ENGINES_DNS_SOURCE.getName())
                        .orderByDesc("id").last("limit 1"));
                if (null == siteAttrEntity || StringUtils.isBlank(siteAttrEntity.getPvalue())
                        || !"1".equals(siteAttrEntity.getPvalue())) {
                    return null;
                }
            }
            Integer[] typeArray = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),
                    OrderTypeEnum.ORDER_CDN_RENEW.getTypeId() };
            CdnSuitEntity suit = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                    .eq("serial_number", siteEntity.getSerialNumber())
                    .in("suit_type", typeArray)
                    // .eq("status",CdnSuitStatusEnum.NORMAL.getId())
                    .orderByDesc("id")
                    .last("limit 1"));
            if (null == suit) {
                return null;
            }
            TbOrderEntity orderEntity = tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("serial_number", suit.getPaySerialNumber()).select("target_id,init_json").last("limit 1"));
            if (null == orderEntity) {
                return null;
            }
            CdnProductEntity product = null;
            String gIds = "";
            if (0 != orderEntity.getTargetId()) {
                product = cdnProductDao.selectOne(new QueryWrapper<CdnProductEntity>()
                        .eq("id", orderEntity.getTargetId()).select("server_group_ids").last("limit 1"));
                if (null != product && StringUtils.isBlank(product.getServerGroupIds())) {
                    gIds = product.getServerGroupIds();
                }
            }
            if (StringUtils.isBlank(gIds)) {
                TbOrderInitVo tbOrderInitVo = DataTypeConversionUtil.string2Entity(orderEntity.getInitJson(),
                        TbOrderInitVo.class);
                if (null != tbOrderInitVo && null != tbOrderInitVo.getProduct_obj()) {
                    product = DataTypeConversionUtil.json2entity(tbOrderInitVo.getProduct_obj(),
                            CdnProductEntity.class);
                }
                if (null != product && StringUtils.isNotBlank(product.getServerGroupIds())) {
                    gIds = product.getServerGroupIds();
                }
            }
            if (StringUtils.isBlank(gIds)) {
                return null;
            }
            CdnClientGroupEntity ccgEntity = cdnClientGroupDao.selectById(gIds);
            if (null == ccgEntity) {
                return null;
            }
            TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(ccgEntity.getDnsConfigId());
            if (null == dnsConfig || StringUtils.isBlank(dnsConfig.getSource())) {
                return null;
            }
            String top = String.format("%s.%s", siteEntity.getMainServerName().replace(".", "-"), ccgEntity.getHash());
            SiteDnsConfigVo vo = new SiteDnsConfigVo();
            vo.setTop(top);
            vo.setDnsConfig(dnsConfig);
            return vo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private R delSiteCusDns(Integer siteId) {
        String eMsg = "";
        try {
            SiteDnsConfigVo vo = getSiteDnsApiConf(siteId, 3);
            if (null == vo || null == vo.getTop() || null == vo.getDnsConfig()) {
                return R.error("delSiteCusDns failed:01");
            }
            if (StringUtils.isBlank(vo.getTop())) {
                return R.error("delSiteCusDns failed:02");
            }
            if (null == vo.getDnsConfig() || null == vo.getDnsConfig().getId() || null == vo.getTop()) {
                return R.error("delSiteCusDns failed:03");
            }
            return dnsCApiService.removeRecordByInfo(vo.getDnsConfig().getId(), vo.getTop(), null, null, null, null);
        } catch (Exception e) {
            eMsg = e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    @Override
    public R saveSiteSEODns(Integer siteId) {
        List<String> eMsg = new ArrayList<>();
        try {
            SiteDnsConfigVo vo = getSiteDnsApiConf(siteId, 1);
            if (null == vo || null == vo.getTop() || null == vo.getDnsConfig() || null == vo.getDnsConfig().getId()) {
                return R.error("saveSiteSEODns failed:01");
            }
            if (StringUtils.isBlank(vo.getTop())) {
                return R.error("saveSiteSEODns failed:02");
            }
            TbDnsConfigEntity dnsConfig = vo.getDnsConfig();
            if (null == dnsConfig || StringUtils.isBlank(dnsConfig.getSource())) {
                return R.error("套餐信息获取失败:06");
            }
            String[] dl = { DnsApiEnum.DNSPOD.getName(), DnsApiEnum.ALIYUN.getName() };
            if (!Arrays.asList(dl).contains(dnsConfig.getSource())) {
                return R.error("仅支持:" + dl.toString());
            }
            // 获取回源信息
            List<TbSiteMutAttrEntity> list = tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id", siteId)
                    .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                    .eq("status", 1));
            Set<String> sIps = new HashSet<>();
            Set<String> sDomains = new HashSet();
            for (TbSiteMutAttrEntity tbSiteAttrEntity : list) {
                NgxSourceBaseInfoVo ngxSourceInfoVo = DataTypeConversionUtil.string2Entity(tbSiteAttrEntity.getPvalue(),
                        NgxSourceBaseInfoVo.class);
                if (null == ngxSourceInfoVo || null == ngxSourceInfoVo.getSource_set()
                        || null == ngxSourceInfoVo.getLine()) {
                    continue;
                }
                if ("ip".equals(ngxSourceInfoVo.getSource_set())) {
                    ngxSourceInfoVo.getLine().forEach(itm2 -> {
                        sIps.add(itm2.getIp());
                    });
                } else if ("domain".equals(ngxSourceInfoVo.getSource_set())) {
                    ngxSourceInfoVo.getLine().forEach(itm2 -> {
                        sDomains.add(itm2.getDomain());
                    });
                }
            }

            String top = vo.getTop();
            R rt = dnsCApiService.removeRecordByInfo(dnsConfig.getId(), top, null, "搜索引擎", null, null);
            if (1 != rt.getCode()) {
                eMsg.add(rt.getMsg());
            }
            for (String ip : sIps) {
                if (StringUtils.isBlank(ip)) {
                    continue;
                }
                if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
                    R r1 = dnsCApiService.addRecord(dnsConfig, top, "A", "搜索引擎", ip, "600");
                    if (1 != r1.getCode()) {
                        eMsg.add(r1.getMsg());
                    }
                } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
                    R r1 = dnsCApiService.addRecord(dnsConfig, top, "A", "search", ip, "600");
                    if (1 != r1.getCode()) {
                        eMsg.add(r1.getMsg());
                    }
                }
            }

            for (String domain : sDomains) {
                if (StringUtils.isBlank(domain)) {
                    continue;
                }
                if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
                    R r1 = dnsCApiService.addRecord(dnsConfig, top, "CNAME", "搜索引擎", domain, "600");
                    if (1 != r1.getCode()) {
                        eMsg.add(r1.getMsg());
                    }
                } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
                    R r1 = dnsCApiService.addRecord(dnsConfig, top, "CNAME", "search", domain, "600");
                    if (1 != r1.getCode()) {
                        eMsg.add(r1.getMsg());
                    }
                }
            }
        } catch (Exception e) {
            eMsg.add(e.getMessage());
            e.printStackTrace();
        }
        return R.ok().put("eMsg", eMsg);
    }

    @Override
    public R batExport(Long userId, BatExportForm form) {
        ValidatorUtils.validateEntity(form);
        List<JSONObject> results = new ArrayList<>();
        String siteGroupMapStr = cdnMakeFileService.getCacheValueByKey(CacheKeyEnums.site_group_info.getKeyName());
        JSONObject siteGroupMap = DataTypeConversionUtil.string2Json(siteGroupMapStr);
        List<TbSiteEntity> list = null;
        if ("all".equals(form.getSiteIds())) {
            list = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                    .eq(null != userId, "user_id", userId)
                    .select("id,main_server_name"));
        } else {
            String[] siteIds = form.getSiteIds().split(",");
            list = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                    .in("id", siteIds)
                    .eq(null != userId, "user_id", userId)
                    .select("id,main_server_name"));
        }
        if (null == list || list.size() == 0) {
            return R.error("没有找到符合条件的站点");
        }
        for (TbSiteEntity siteEntity : list) {
            if (null == siteEntity) {
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("siteId", siteEntity.getId());
            jsonObject.put("siteName", siteEntity.getMainServerName());
            jsonObject.put("groupId", "0");
            jsonObject.put("groupName", "默认分组");
            if (siteGroupMap.containsKey(String.valueOf(siteEntity.getId()))) {
                String groupStr = siteGroupMap.getString(String.valueOf(siteEntity.getId()));
                if (StringUtils.isNotBlank(groupStr)) {
                    String[] gns = groupStr.split(":");
                    if (2 == gns.length) {
                        jsonObject.put("groupId", gns[0]);
                        jsonObject.put("groupName", gns[1]);
                    }
                }
            }
            for (String key : form.getKeys().split(",")) {
                jsonObject.put(key, "");
                SiteAttrEnum attrEnum = SiteAttrEnum.getObjByName(key);
                if (null == attrEnum) {
                    continue;
                }
                int DbType = SiteAttrEnum.getDbType(attrEnum);
                if (1 == DbType) {
                    List<TbSiteAttrEntity> values = tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id", siteEntity.getId())
                            .eq("pkey", key)
                            .select("pvalue"));
                    if (null != values && values.size() > 0) {
                        ;
                        jsonObject.put(key,
                                values.stream().map(q -> q.getPvalue()).collect(Collectors.toList()).toString());
                    }
                } else if (2 == DbType) {
                    List<TbSiteMutAttrEntity> values = tbSiteMutAttrDao
                            .selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                                    .eq("site_id", siteEntity.getId())
                                    .eq("pkey", key)
                                    .select("pvalue"));
                    if (null != values && values.size() > 0) {
                        jsonObject.put(key,
                                values.stream().map(q -> q.getPvalue()).collect(Collectors.toList()).toString());
                    }
                }
            }
            results.add(jsonObject);
        }

        return R.ok().put("data", results);
    }

    @Override
    public R batchAddSiteAttr(Long userId, BatchAddSiteAttrForm form) {
        if ("all".equals(form.getSiteIds())) {
            List<TbSiteEntity> list = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>().select("id"));
            List<String> strList = list.stream().map(o -> o.getId().toString()).collect(Collectors.toList());
            String ids = String.join(",", strList);
            form.setSiteIds(ids);
        }
        int findSum = 0;
        List<String> bf = new ArrayList<>();
        for (String siteId : form.getSiteIds().split(",")) {
            TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                    .eq(null != userId, "user_id", userId)
                    .eq("id", siteId)
                    .select("id")
                    .last("limit 1"));
            if (null == site) {
                continue;
            }
            this.cleanSiteConfigFile(site.getId());
            SiteAttrEnum obj = SiteAttrEnum.getObjByName(form.getKey());
            if (null == obj) {
                continue;
            }
            String[] mTypes = { "m_text", "mm_text" };
            if (!Arrays.asList(mTypes).contains(obj.getType())) {
                continue;
            }
            findSum++;
            Map<String, Object> saveMap = new HashMap<>();
            saveMap.put("site_id", site.getId());
            saveMap.put("siteId", site.getId());
            saveMap.put(form.getKey(), form.getValue());
            R rs = this.saveSiteAttr(saveMap);
            if (1 != rs.getCode()) {
                bf.add(rs.toJsonString());
            }

        }
        return R.ok().put("findSum", findSum).put("err", bf);
    }

}
