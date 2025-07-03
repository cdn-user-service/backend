package io.ants.modules.sys.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.annotation.SysLog;
import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.CdnClientAreaEntity;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.entity.CdnClientGroupChildConfEntity;
import io.ants.modules.sys.entity.CdnClientGroupEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.*;
import io.ants.modules.sys.service.*;
import io.ants.modules.sys.vo.EditGroupClientDnsVo;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.NodeCheckConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/cdnsys/auth")
public class AntsCdnAuthController extends AbstractController {

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private CdnSysAuthService cdnSysAuthService;
    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private CdnClientGroupChildConfDao cdnClientGroupChildConfDao;
    @Autowired
    private DnsCApiService dnsCApiService;

    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private CdnGroupService groupService;
    @Autowired
    private CdnClientAreaDao cdnClientAreaDao;

    // 获取授权信息
    @GetMapping("/info")
    public R authInfo() {
        String info = cdnSysAuthService.getAuthInfo();
        if (StringUtils.isNotBlank(info)) {
            return R.ok().put("data", info);
        }
        return R.error("获取失败！");
    }

    private String minNodeAgentVersion() {
        Date now = new Date();
        String v = "9999.9999";
        List<CdnClientEntity> clientList = cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type", 1)
                .eq("status", 1)
                .isNotNull("reg_info")
                .le("effective_start_time", now)
                .ge("effective_ending_time", now));
        for (CdnClientEntity client : clientList) {
            if (StringUtils.isNotBlank(client.getAgentVersion())) {
                String av = client.getAgentVersion();
                if (av.compareToIgnoreCase(v) < 0) {
                    v = av;
                }
            }
        }
        return v;
    }

    // 与Auth 同步 授权节点信息
    @GetMapping("/node/sync")
    public R nodeList() {
        StaticVariableUtils.minNodeVersion = minNodeAgentVersion();
        cdnMakeFileService.getClientNginxVersion();
        return cdnSysAuthService.syncFlushAuthList();
    }

    @GetMapping("/node/conf/keys")
    public R nodeConfKeys() {
        return R.ok().put("data", PublicEnum.getNodeConfKeys());
    }

    @GetMapping("/make/file/invoke/method")
    public R invoke_method() {
        return R.ok().put("data", cdnMakeFileService.getAllCreateFileInvokeMethod());
    }

    @PostMapping("/node/save/conf/info")
    @SysLog("保存指定节点高级配置")
    public R nodeSaveConfInfo(@RequestBody Map<String, Object> params) {
        if (!params.containsKey("id")) {
            return R.error("缺少必要参数[id]");
        }
        CdnClientEntity client = cdnClientDao.selectById(params.get("id").toString());
        if (null == client) {
            return R.error("无此站");
        }
        if (null != client.getParentId()) {
            return R.error("节点的备用IP不可添加配置！");
        }
        for (String key : params.keySet()) {
            PublicEnum targetPubObj = PublicEnum.getObjByName(key);
            if (null == targetPubObj) {
                logger.error("[" + key + "] is error type");
                continue;
            }
            Object value = params.get(key);
            cdnMakeFileService.cdnPubCheckKeyValueRule(key, value);
        }
        client.setConfInfo(DataTypeConversionUtil.map2json(params).toJSONString());
        if (params.containsKey("proxy_cache_path_dir") && null != params.get("proxy_cache_path_dir")) {
            if (StringUtils.isNotBlank(params.get("proxy_cache_path_dir").toString())) {

                Map map = new HashMap(8);
                map.put(PushTypeEnum.COMMAND_CREATE_DIR.getName(), params.get("proxy_cache_path_dir").toString());
                cdnMakeFileService.pushByInputInfo(map);

                Map pushMap = new HashMap(8);
                pushMap.put(PushTypeEnum.NODE_RESTART_NGINX.getName(), client.getClientIp());
                cdnMakeFileService.pushByInputInfo(pushMap);
            }
        }
        Map pushMap = new HashMap();
        pushMap.put(PushTypeEnum.NODE_CUSTOM.getName(), client.getClientIp());
        cdnMakeFileService.pushByInputInfo(pushMap);
        cdnClientDao.updateById(client);
        return R.ok().put("data", client);
    }

    @PostMapping("/node/add/backup/ip")
    public R nodeAddBackupIp(@RequestBody Map params) {
        if (!params.containsKey("id") || !params.containsKey("clientIps")) {
            return R.error("缺少必要参数[id][clientIs]");
        }
        return cdnSysAuthService.nodeAddBackupIp(params);

    }

    @PostMapping("/node/add/ip")
    public R nodeAddIp(@RequestBody AddClientIpForm form) {
        ValidatorUtils.validateEntity(form);
        return cdnSysAuthService.nodeAddIp2Db(form);
    }

    @PostMapping("/node/page/list")
    public R nodePageList(@RequestBody CdnClientQueryForm params) {
        // cdnMakeFileService.getClientVersion(null);
        return R.ok().put("data", cdnSysAuthService.nodePageList(params)).put("curTask",
                StaticVariableUtils.curPushTaskName);
    }

    @GetMapping("/node/push/feedback/detail")
    public R feedbackDetail(@RequestParam Integer id) {
        return R.ok().put("data", cdnSysAuthService.feedbackInfo(id));
    }

    @GetMapping("/node/push/feedback/clean")
    public R feedbackClean(@RequestParam Integer id) {
        cdnSysAuthService.deleteAllFeedback(id);
        return R.ok();
    }

    // 节点向服务器发起注册
    @GetMapping("/node/addByNodeRequest")
    public R nodeAdd() {
        return cdnSysAuthService.nodeAddByNodeHttpRequest();
    }

    /**
     * NODE -SEVER 反馈
     */
    @PostMapping("/nginx/conf/feedbacks")
    public String feedbacks(@RequestBody String info) {
        // logger.debug(info);
        Map param = DataTypeConversionUtil.urlParams2Map(info);
        assert param != null;
        if (param.containsKey("recordId") && param.containsKey("info")) {
            String recordId = param.get("recordId").toString();
            String log = param.get("info").toString();
            if (StringUtils.isBlank(log)) {
                log = "-";
            }
            if (StaticVariableUtils.alreadyPushTaskRecordMap.containsKey(recordId)) {
                String f_value = StaticVariableUtils.alreadyPushTaskRecordMap.get(recordId).toString() + "|" + log;
                StaticVariableUtils.alreadyPushTaskRecordMap.put(recordId, f_value);
            } else {
                StaticVariableUtils.alreadyPushTaskRecordMap.put(recordId, log);
            }

        }
        return "1";
    }

    /**
     * 解封IP
     * 
     * @param map
     * @return
     */
    @PostMapping("/release/short_cc/intercept/ips")
    public R releaseInterceptIps(@RequestBody Map map) {
        if (!map.containsKey("ips")) {
            return R.error("ips is empty");
        }
        String ips = map.get("ips").toString();
        Map pushmap = new HashMap(8);
        pushmap.put("release_intercept_ip", ips);
        cdnMakeFileService.pushByInputInfo(pushmap);
        return R.ok();
    }

    @GetMapping("/push/enum")
    public R get_enum() {
        return R.ok().put("pushEnum", PushTypeEnum.getAll()).put("command_index_enum", CommandEnum.getAll());
    }

    @GetMapping("/push/feedbacks/detail")
    public R push_feedbacks(@RequestParam String types, @RequestParam Integer index) {
        R r = R.ok();
        if (types.contains("1")) {
            r.put("feed", cdnSysAuthService.operaFeeds(index));
        }
        if (types.contains("2")) {
            r.put("data_push", cdnSysAuthService.operaDetail(index));
        }
        if (types.contains("3")) {
            r.put("data_push_all", StaticVariableUtils.alreadyPushTaskRecordMap);
        }
        return r;
    }

    @PostMapping("/node/SaveByMainControl")
    public R nodeAdd2(@RequestBody Map param) {
        CdnClientForm form = DataTypeConversionUtil.map2entity(param, CdnClientForm.class);
        return R.ok().put("data", cdnSysAuthService.SaveByMainControl(form));
    }

    @PostMapping("/node/attr/save")
    public R nodeAttrSave(@RequestBody Map param) {
        return cdnSysAuthService.nodeAttrSave(param);
    }

    @PostMapping("/node/ssh/save")
    public R nodeSshSave(@RequestBody CdnClientSshSaveForm form) {
        ValidatorUtils.validateEntity(form);
        CdnClientEntity client = cdnClientDao.selectById(form.getId());
        if (null == client) {
            return R.error("client is null");
        }
        client.setSshPort(form.getSshPort());
        client.setSshUser(form.getSshUser());
        client.setSshPwd(form.getSshPwd());
        cdnClientDao.updateById(client);
        return R.ok();
    }

    @GetMapping("/node/enable")
    public R nodeEnable(@RequestParam Integer id) {
        CdnClientEntity r_client = cdnClientDao.selectById(id);
        if (null == r_client) {
            return R.error("无此节点");
        }
        if (null == r_client.getAreaId() || null == r_client.getArea() || null == r_client.getLine()) {
            return R.error("请先配置节点！");
        }
        if (QuerySysAuth.IS_OFFLINE_VERSION) {
            return R.ok();
        }
        // http add ip to remote node list
        String ret = cdnSysAuthService.regNode(r_client.getClientIp());
        if (StringUtils.isNotBlank(ret)) {
            return R.ok();
        } else {
            return R.error("注册失败！");
        }
    }

    @GetMapping("/node/delete")
    public R nodeDelete(@RequestParam Integer id) {
        // 删除无头节点
        List<CdnClientEntity> child_list = cdnClientDao
                .selectList(new QueryWrapper<CdnClientEntity>().eq("client_type", 3));
        for (CdnClientEntity c : child_list) {
            if (null == c.getParentId()) {
                continue;
            }
            CdnClientEntity p_client = cdnClientDao.selectById(c.getParentId());
            if (null == p_client) {
                cdnClientDao.deleteById(c.getParentId());
            }
        }

        CdnClientEntity r_client = cdnClientDao.selectById(id);
        if (null == r_client) {
            return R.error("无此节点ip");
        }
        Long count = cdnClientGroupChildConfDao
                .selectCount(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("client_id", id));
        if (count > 0) {
            return R.error("分组中存在当前节点！删除失败！");
        }
        redisUtils.delete("version_" + r_client.getClientIp());
        redisUtils.delete(r_client.getClientIp() + "_groupId");
        if (null == r_client.getEffectiveEndingTime()) {
            if (r_client.getClientType().equals(1)) {
                cdnClientDao.delete(new QueryWrapper<CdnClientEntity>().eq("parent_id", id));
            }
            cdnClientDao.deleteById(id);
            return R.ok();
        }
        if (!ClientStatusEnum.ALREADY_REGISTER.getId().equals(r_client.getStatus())) {
            if (r_client.getClientType().equals(1)) {
                cdnClientDao.delete(new QueryWrapper<CdnClientEntity>().eq("parent_id", id));
            }
            cdnClientDao.deleteById(id);
            return R.ok();
        }
        if (QuerySysAuth.IS_OFFLINE_VERSION) {
            redisUtils.set("deleteNodeTime", System.currentTimeMillis(), 3);

            cdnClientDao.delete(new QueryWrapper<CdnClientEntity>().eq("parent_id", id));

            cdnClientDao.deleteById(id);
            return R.ok("");
        }
        if (null != r_client.getEffectiveEndingTime()) {
            String deleteNodeTime = redisUtils.get("deleteNodeTime");
            if (StringUtils.isNotBlank(deleteNodeTime)) {
                return R.error("请稍侯再试！");
            }
            // 发送删除到Auth
            String ret = cdnSysAuthService.deleteNode(r_client.getClientIp());
            if (StringUtils.isNotBlank(ret)) {
                redisUtils.set("deleteNodeTime", System.currentTimeMillis(), 3);
                if (r_client.getClientType().equals(1)) {
                    cdnClientDao.delete(new QueryWrapper<CdnClientEntity>().eq("parent_id", id));
                }
                cdnClientDao.deleteById(id);
                return R.ok();
            }

        }
        return R.error("删除失败");
    }

    @GetMapping("/node/info")
    public R nodeInfo() {
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IPUtils.getIpAddr(request);
        CdnClientEntity rClient = cdnClientDao.selectOne(new QueryWrapper<CdnClientEntity>()
                .eq("client_ip", ip).last("limit 1"));
        return R.ok().put("data", rClient);
    }

    @PostMapping("/group/save")
    @SysLog("保存分组")
    public R groupSave(@RequestBody Map param) {
        CdnClientGroupEntity inputGroup = DataTypeConversionUtil.map2entity(param, CdnClientGroupEntity.class);
        if (null == inputGroup) {
            return R.error("参数有误！");
        }
        return groupService.saveCdnGroup(inputGroup);
    }

    @PostMapping("/group/create")
    @SysLog("创建分组")
    public R groupCreate(@RequestBody Map param) {
        CdnClientGroupEntity group = DataTypeConversionUtil.map2entity(param, CdnClientGroupEntity.class);
        if (null == group) {
            return R.error("参数有误！");
        }
        return groupService.saveCdnGroup(group);
    }

    @PostMapping("/group/delete")
    public R groupDelete(@RequestBody Map param) {
        if (!param.containsKey("ids")) {
            return R.error("参数[ids]缺失！");
        }
        String ids = param.get("ids").toString();
        return groupService.deleteCdnGroup(ids);

    }

    @PostMapping("/client/area/list")
    public R getClientAreaList(@RequestBody Map param) {
        int page = 1;
        int limit = 10;
        String name = null;
        if (param.containsKey("page")) {
            page = Integer.parseInt(param.get("page").toString());
        }
        if (param.containsKey("limit")) {
            limit = Integer.parseInt(param.get("limit").toString());
        }
        if (param.containsKey("name")) {
            name = param.get("name").toString();
        }
        IPage<CdnClientAreaEntity> ipage = cdnClientAreaDao.selectPage(
                new Page<>(page, limit),
                new QueryWrapper<CdnClientAreaEntity>().eq(StringUtils.isNotBlank(name), "name", name));
        List<CdnClientAreaEntity> fList = new ArrayList<>();
        CdnClientAreaEntity defalutEntity = new CdnClientAreaEntity();
        defalutEntity.setId(0);
        defalutEntity.setName("默认");
        defalutEntity.setRemark("默认");
        fList.add(defalutEntity);
        fList.addAll(ipage.getRecords());
        fList.forEach(item -> {
            Long count = cdnClientDao.selectCount(new QueryWrapper<CdnClientEntity>()
                    .eq("area_id", item.getId())
                    .eq("client_type", ClientTypeEnum.MAIN_NODE.getId()));
            item.setNodeNum(count);
        });
        ipage.setRecords(fList);

        return R.ok().put("data", new PageUtils(ipage));
    }

    @PostMapping("/client/area/save")
    public R updateClientAreaList(@RequestBody Map param) {
        CdnClientAreaEntity areaEntity = DataTypeConversionUtil.map2entity(param, CdnClientAreaEntity.class);
        if (null == areaEntity.getId()) {
            // insert
            cdnClientAreaDao.insert(areaEntity);
        } else if (0 != areaEntity.getId()) {
            // update
            cdnClientAreaDao.updateById(areaEntity);
        }
        return R.ok();
    }

    @PostMapping("/client/area/delete")
    public R deleteClientAreaList(@RequestBody Map param) {
        String ids = null;
        if (param.containsKey("ids")) {
            for (String id : param.get("ids").toString().split(",")) {
                Long count = cdnClientDao.selectCount(new QueryWrapper<CdnClientEntity>().eq("area_id", id));
                if (count > 0) {
                    throw new RRException("分组[" + id + "]下存在节点，不可删除！");
                }
                cdnClientAreaDao.deleteById(id);
            }
        }
        return R.ok();
    }

    @PostMapping("/group/list")
    public R groupList(@RequestBody Map param) {
        int page = 1;
        int limit = 10;
        String key = null;
        if (param.containsKey("page")) {
            page = Integer.parseInt(param.get("page").toString());
        }
        if (param.containsKey("limit")) {
            limit = Integer.parseInt(param.get("limit").toString());
        }
        if (param.containsKey("key")) {
            key = param.get("key").toString();
        }
        return groupService.clientGroupList(page, limit, key);
    }

    @GetMapping("/group/all")
    public R groupAll() {
        List<CdnClientGroupEntity> list = cdnClientGroupDao.selectList(new QueryWrapper<CdnClientGroupEntity>()
                .eq("status", 1)
                .orderByDesc("weight"));
        return R.ok().put("data", list);
    }

    /**
     * 分组添加节点
     * 
     * @param params
     * @return
     */
    @PostMapping("/group/add_client_ids")
    public R addClientIds(@RequestBody Map params) {
        if (!params.containsKey("id") || !params.containsKey("ids") || !params.containsKey("parentId")) {
            return R.error("缺少参数[groupId][ids][parentId]");
        }
        // {"id":1,"parentId":63,"ids":"84,112","line":""}
        Integer groupId = Integer.parseInt(params.get("id").toString());
        String add_ids = params.get("ids").toString();
        int parentId = Integer.parseInt(params.get("parentId").toString());
        String line = null;
        Long ttl = 600L;
        if (0 == parentId) {
            if (!params.containsKey("line")) {
                return R.error("缺少参数[line]");
            }
            line = params.get("line").toString();
            ttl = 600l;
        } else {
            // 备用线路
            CdnClientGroupChildConfEntity groupClient = cdnClientGroupChildConfDao.selectById(groupId);
            if (null != groupClient) {
                line = groupClient.getLine();
                ttl = groupClient.getTtl();
            }
        }
        CdnClientGroupEntity group = groupService.addGroupClient(groupId, parentId, add_ids, line, ttl);

        return R.ok().put("data", group);
    }

    // 修改分组中的解析记录
    @PostMapping("/group/modify_client_dns_info")
    public R modifyClientDnsInfo(@RequestBody EditGroupClientDnsVo vo) {
        ValidatorUtils.validateEntity(vo);
        return groupService.modifyClientDnsInfo(vo);

    }

    /**
     * 删除分组中的节点
     * 
     * @param id
     */
    private void removeClientGroupId(String id) {
        CdnClientGroupChildConfEntity dnsConfEntity = cdnClientGroupChildConfDao.selectById(id);
        if (null != dnsConfEntity) {
            // 删除子
            List<CdnClientGroupChildConfEntity> list = cdnClientGroupChildConfDao
                    .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("parent_id", id));
            for (CdnClientGroupChildConfEntity c_clientGDC : list) {
                String s_id = c_clientGDC.getId().toString();
                // 递归删除子
                this.removeClientGroupId(s_id);
            }
            CdnClientEntity client = cdnClientDao.selectById(dnsConfEntity.getClientId());
            CdnClientGroupEntity group = cdnClientGroupDao.selectById(dnsConfEntity.getGroupId());
            if (null != client && null != group) {
                dnsCApiService.removeRecordByInfo(group.getDnsConfigId(), "*." + group.getHash(), "A",
                        dnsConfEntity.getLine(), client.getClientIp(), null);
            }
            cdnClientGroupChildConfDao.deleteById(id);
        }
    }

    /**
     * 分组删除节点
     * 
     * @param params
     * @return
     */
    @PostMapping("/group/delete_client_ids")
    public R deleteClientIds(@RequestBody Map params) {
        if (!params.containsKey("ids")) {
            return R.error("缺少参数[ids]");
        }
        String del_ids = params.get("ids").toString();
        if (StringUtils.isNotBlank(del_ids)) {
            for (String d_id : del_ids.split(",")) {
                this.removeClientGroupId(d_id);
            }
        }
        return R.ok();
    }

    /**
     * 分组中未使用的节点
     * 
     * @param params
     * @return
     */
    @PostMapping("/group/get/unuesd/client")
    public R getUnUsedClient(@RequestBody Map params) {
        if (!params.containsKey("groupId") || !params.containsKey("parentId")) {
            return R.error("缺少参数[groupId][parentId]");
        }
        Integer groupId = Integer.parseInt(params.get("groupId").toString());
        Integer parentId = Integer.parseInt(params.get("parentId").toString());
        return R.ok().put("data", groupService.UnUsedClientIpsByGroup(groupId, parentId));
    }

    @GetMapping("/group/get_detail")
    public R getDetail(@RequestParam Integer groupId) {
        return groupService.getGroupDetail(groupId);
    }

    @PostMapping("/group/set_first_client")
    public R setFirstClient(@RequestBody Map params) {
        if (!params.containsKey("groupId") || !params.containsKey("clientId")) {
            return R.error("参数缺失！[groupId][clientId]");
        }
        Integer groupId = (Integer) params.get("groupId");
        String clientId = params.get("clientId").toString();
        CdnClientGroupEntity groupEntity = cdnClientGroupDao.selectById(groupId);
        if (null == groupEntity) {
            return R.error("无此分组！");
        }
        CdnClientEntity client = cdnClientDao.selectById(clientId);
        if (null == client) {
            return R.error("无此节点！");
        }
        String c_ids = groupEntity.getClientIds();
        List<String> cList = new ArrayList<>(Arrays.asList(c_ids.split(",")));
        cList.remove(clientId);
        LinkedList<String> f_cList = new LinkedList<>();
        f_cList.addAll(cList);
        f_cList.addFirst(clientId);
        String join = String.join(",", f_cList);
        groupEntity.setClientIds(join);
        cdnClientGroupDao.updateById(groupEntity);
        return R.ok().put("data", groupEntity);
    }

    /**
     * 将所有配置推送到节点
     * 
     * @return
     */
    @SysLog("推送所有数据到节点")
    @GetMapping("/pushDataToNode")
    public R pushDataToNode() {
        this.recordInfo(redisUtils);
        if (StaticVariableUtils.makeFileThread) {
            // logger.debug("makeFileThread 任务中。。。");
            return R.error("推送任务后台执行中，请稍侯！");
        }
        StaticVariableUtils.makeFileThreadIndex++;
        Map pushmap = new HashMap(8);
        pushmap.put("all_file", "null");
        cdnMakeFileService.pushByInputInfo(pushmap);
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }

    /**
     * 根据站点推送配置到节点
     * 
     * @param siteId
     * @return
     */
    @SysLog("生成站点配置块文件到节点")
    @GetMapping("/pushSiteConfToNode")
    public R pushSiteConfToNode(@RequestParam Integer siteId) {
        this.recordInfo(redisUtils);
        if (StaticVariableUtils.makeFileThread) {
            // logger.debug("makeFileThread 任务中。。。");
            return R.error("推送任务后台执行中，请稍侯！");
        }
        StaticVariableUtils.makeFileThreadIndex++;
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }

    /**
     * 获取当前任务状态【停用】
     * 
     * @return
     */
    @GetMapping("/pushTaskStatus")
    public R pushTaskStatus(@RequestParam(required = false, defaultValue = "0") Integer type) {
        // nginx.*syntax is ok.* test is successful
        // nginx.*\sin\s.*\stest\sfailed
        // StaticVariableUtils.AlreadyPushTaskRecordMap
        if (0 == type) {
            return R.ok().put("in_task", StaticVariableUtils.makeFileThread)
                    .put("task_size", StaticVariableUtils.taskMap.size())
                    .put("data", StaticVariableUtils.taskMap)
                    .put("index", StaticVariableUtils.makeFileThreadIndex);
        } else if (1 == type) {
            return R.ok().put("task_size", StaticVariableUtils.taskMap.size());
        } else if (2 == type) {
            return R.ok().put("in_task", StaticVariableUtils.makeFileThread);
        } else {
            return R.ok().put("task_size", StaticVariableUtils.taskMap.size());
        }

    }

    /**
     * 执行任务
     * 
     * @return
     */
    @SysLog("执行待完成任务")
    @GetMapping("/runTask")
    public R runTask() {
        this.recordInfo(redisUtils);
        StaticVariableUtils.makeFileThreadIndex++;
        cdnMakeFileService.startOperaTask();
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }

    /**
     * 推送指令到节点
     * 
     * @param params
     * @return
     */
    @PostMapping("/pushCommandToNodes")
    public R pushCommandToNodes(@RequestBody Map params) {
        this.recordInfo(redisUtils);
        if (!params.containsKey("ids") || !params.containsKey("cid")) {
            return R.error("参数缺失");
        }
        StaticVariableUtils.makeFileThreadIndex++;
        String clientIds = params.get("ids").toString();
        Integer cmdId = Integer.parseInt(params.get("cid").toString());
        Map pushmap = new HashMap();
        pushmap.put("command", cmdId);
        cdnMakeFileService.pushByInputInfo(pushmap);
        // cdnMakeFileService.pushCommandToNodes(clientIds,cmdId);
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }

    // 所有推送方式
    @SysLog("根据输入条件推送数据到节点")
    @PostMapping("/pushByInputInfo")
    public R pushByInputInfo(@RequestBody Map params) {
        String[] keys = { PushTypeEnum.SHELL_ANTS_CMD_TO_MAIN.getName(),
                PushTypeEnum.SHELL_ANTS_CMD_TO_NODE.getName() };
        for (Object key : params.keySet()) {
            if (!Arrays.asList(keys).contains(key)) {
                HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
                logger.info("!!!! ip:" + IPUtils.getIpAddr(request) + ",cmd:" + params.get(key));
            }
        }
        this.recordInfo(redisUtils);
        StaticVariableUtils.makeFileThreadIndex++;
        Object res = cdnMakeFileService.pushByInputInfo(params);
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex).put("res", res);
    }

    /**
     * 检测站点在节点的状态【暂停使用】
     * 
     * @param params
     * @return
     */
    @PostMapping("/checkSitesInNode")
    public R checkSitesInNode(@RequestBody Map params) {
        if (!params.containsKey("ids")) {
            return R.error("参数缺失");
        }
        StaticVariableUtils.makeFileThreadIndex++;
        String siteIds = params.get("ids").toString();
        cdnMakeFileService.checkSitesInNode(siteIds);
        return R.ok().put("data", StaticVariableUtils.makeFileThreadIndex);
    }

    // index 图表
    @GetMapping("/GetIndexChartData")
    public R getIndexChartData(String keys) {
        return R.ok().put("data", cdnSysAuthService.getIndexChartData(keys));
    }

    @PostMapping("/update/static/variable")
    public R updateStaticVariable(@RequestBody Map param) {
        if (param.containsKey("protocol") && StringUtils.isNotBlank(param.get("protocol").toString())) {
            StaticVariableUtils.MasterProtocol = param.get("protocol").toString().replace(":", "");
        }
        if (param.containsKey("port") && StringUtils.isNotBlank(param.get("port").toString())) {
            StaticVariableUtils.MasterWebPort = Integer.parseInt(param.get("port").toString());
        }
        if (param.containsKey("server") && StringUtils.isNotBlank(param.get("server").toString())) {
            StaticVariableUtils.masterWebSeverName = param.get("server").toString();
        }
        return R.ok();
    }

    /**
     * //检测接入TOKEN与url
     * 
     * @return
     */
    @GetMapping("/check/key/show")
    public R checkKeyShow() {
        this.recordInfo(redisUtils);
        if (StringUtils.isBlank(StaticVariableUtils.checkNodeInputToken)) {
            if (StringUtils.isBlank(StaticVariableUtils.authMasterIp)
                    || StringUtils.isBlank(StaticVariableUtils.masterWebSeverName)) {
                return R.error("获取失败");
            }
            StaticVariableUtils.checkNodeInputToken = HashUtils
                    .md5ofString("check_" + StaticVariableUtils.authMasterIp);
        }
        StaticVariableUtils.MasterProtocol = StaticVariableUtils.MasterProtocol.replace(":", "");
        String url = String.format("url: '%s://%s%s'\n", StaticVariableUtils.MasterProtocol,
                StaticVariableUtils.masterWebSeverName, QuerySysAuth.CHECK_NODE_INPUT_PATH);
        try {
            final String cPath = "/usr/ants/port-scan/config.yaml";
            File file = new File(cPath);
            if (!file.exists()) {
                FileUtils.createFileAndParentDir(cPath);
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(cPath));
            String sb = String.format("token: '%s'\n", StaticVariableUtils.checkNodeInputToken) + url;
            out.write(sb);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok().put("token", StaticVariableUtils.checkNodeInputToken).put("importUrl", url);
    }

    // 更新节点健康状态
    private void updateNodeStableScoreByImportString(String sourceIp, String confStr, String nodeCheckResult) {
        List<String> normalNode = new ArrayList<>();
        List<String> unNormalNode = new ArrayList<>();
        NodeCheckConfig config = DataTypeConversionUtil.string2Entity(confStr, NodeCheckConfig.class);
        if (StringUtils.isBlank(sourceIp) || StringUtils.isBlank(confStr) || null == config) {
            logger.error("sourceIp or confStr is error");
            return;
        }
        if (config.getFailPercent() < 1 || config.getFailPercent() > 100) {
            config.setFailPercent(1);
        }

        if (StringUtils.isNotBlank(nodeCheckResult)) {
            // 1.1.1.1#1,2.2.2.2#0,3.3.3.3#1
            for (String ipCheck : nodeCheckResult.split(",")) {
                String[] info = ipCheck.split("#");
                if (2 == info.length) {
                    String ip = info[0];
                    String healthValue = info[1];
                    if (!StaticVariableUtils.nodeCheckMap.containsKey(ip)) {
                        Map map = new HashMap();
                        map.put(sourceIp, System.currentTimeMillis() + "#" + healthValue);
                        StaticVariableUtils.nodeCheckMap.put(ip, map);
                    } else {
                        StaticVariableUtils.nodeCheckMap.get(ip).put(sourceIp,
                                System.currentTimeMillis() + "#" + healthValue);
                    }
                }
            }
        }

        // statistics
        // logger.info(DataTypeConversionUtil.map2json(StaticVariableUtils.nodeCheckMap).toJSONString());
        for (String nodeIp : StaticVariableUtils.nodeCheckMap.keySet()) {
            int total_sum = 0;
            int fail_sum = 0;
            int success_sum = 0;
            if (null != StaticVariableUtils.nodeCheckMap.get(nodeIp)) {
                for (String timeStatusStr : StaticVariableUtils.nodeCheckMap.get(nodeIp).values()) {
                    // 137000000#1
                    String[] info = timeStatusStr.split("#");
                    if (2 != info.length) {
                        continue;
                    }
                    if ((System.currentTimeMillis() - Long.parseLong(info[0])) > 3 * config.getCheckNodeFrequency()
                            * 1000) {
                        // check date Invalid
                        continue;
                    }
                    total_sum++;
                    if ("1".equals(info[1])) {
                        success_sum++;
                    } else if ("0".equals(info[1])) {
                        fail_sum++;
                    }
                }
                double result = (double) fail_sum / total_sum;
                if ((result) * 100 >= config.getFailPercent()) {
                    // logger.error("node score update:"+nodeIp+",node-fault-result:"+result);
                    unNormalNode.add(nodeIp);
                    continue;
                }
                if (success_sum > 0) {
                    normalNode.add(nodeIp);
                }
            }
        }

        if (normalNode.size() > 0) {
            UpdateWrapper<CdnClientEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.in("client_ip", normalNode.toArray()).set("check_time", System.currentTimeMillis() / 1000)
                    .setSql(" stable_score= stable_score+1 ");
            cdnClientDao.update(null, updateWrapper);
        }
        if (unNormalNode.size() > 0) {
            UpdateWrapper<CdnClientEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.in("client_ip", unNormalNode.toArray()).set("check_time", System.currentTimeMillis() / 1000)
                    .setSql(" stable_score=0 ");
            cdnClientDao.update(null, updateWrapper);
        }
    }

    private String getConfigValueByKey(String key) {
        return sysConfigService.getValue(key);
    }

    private void updateNodeIpList() {
        if (Math.abs(System.currentTimeMillis() - StaticVariableUtils.synNodeIpTimeTemp) > 5 * 60 * 1000) {
            if (StaticVariableUtils.NodeIpList.isEmpty()) {
                StaticVariableUtils.NodeIpList.add("0.0.0.0");
            }
            StaticVariableUtils.synNodeIpTimeTemp = System.currentTimeMillis();
            List<CdnClientEntity> list = cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                    .select("client_ip")
                    .notIn("client_ip", StaticVariableUtils.NodeIpList));
            list.forEach(item -> {
                if (StringUtils.isNotBlank(item.getClientIp())) {
                    // logger.info(item.getClientIp());
                    if (!StaticVariableUtils.NodeIpList.contains(item.getClientIp())) {
                        StaticVariableUtils.NodeIpList.add(item.getClientIp());
                    }
                }
            });
        }
    }

    /**
     * 节点监测接口
     * 
     * @param map
     * @return
     */
    @RequestMapping("/check/import")
    public R checkExport(@RequestBody Map map) {
        CheckExportForm form = DataTypeConversionUtil.map2entity(map, CheckExportForm.class);
        // this.recordInfo(redisUtils);
        ValidatorUtils.validateEntity(form);
        if (!form.getToken().equals(StaticVariableUtils.checkNodeInputToken)) {
            return R.error("token is error");
        }
        String confValue = getConfigValueByKey(ConfigConstantEnum.CDN_NODECHECK_KEY.getConfKey());
        R r = R.ok();
        switch (form.getType()) {
            case "NodeList":
                updateNodeIpList();
                r.put("NodeList", StaticVariableUtils.NodeIpList);
                r.put("url", QuerySysAuth.CHECK_NODE_INPUT_PATH);
                if (StringUtils.isNotBlank(confValue)) {
                    JSONObject obj = DataTypeConversionUtil.string2Json(confValue);
                    r.put("CheckConf", obj);
                }
                break;
            case "NodeResult":
                if (StringUtils.isBlank(form.getNodeCheckResult())) {
                    return R.error("NodeCheckResult is not found! ");
                } else {
                    HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
                    String ip = IPUtils.getIpAddr(request);
                    updateNodeStableScoreByImportString(ip, confValue, form.getNodeCheckResult());
                }
                break;
            default:
                break;
        }
        return r;
    }

    @PostMapping("/group/node/stable/infos")
    public R groupNodeStableInfosList(@RequestBody Map params) {
        int page = 1;
        int limit = 10;
        String key = null;
        Integer groupId = null;
        if (params.containsKey("page")) {
            page = Integer.parseInt(params.get("page").toString());
        }
        if (params.containsKey("limit")) {
            limit = Integer.parseInt(params.get("limit").toString());
        }
        if (params.containsKey("groupId")) {
            groupId = Integer.parseInt(params.get("groupId").toString());
        }
        if (params.containsKey("key")) {
            key = params.get("key").toString();
        }
        return R.ok().put("data", groupService.groupNodeStableInfosList(page, limit, groupId, key));
    }

    @PostMapping("/migrate/master")
    @SysLog("迁移主控")
    @PreAuthorize("hasAuthority('sys:ants_conf:save')")
    public R changeMaster(@RequestBody ChangeMasterForm form) {
        ValidatorUtils.validateEntity(form);
        return cdnSysAuthService.changeMaster(form);
    }

}
