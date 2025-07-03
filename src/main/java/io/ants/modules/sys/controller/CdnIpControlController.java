package io.ants.modules.sys.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.exception.RRException;
import io.ants.common.ip2area.IPSeeker;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;

import io.ants.modules.sys.dao.CdnIpControlDao;
import io.ants.modules.sys.dao.TbCdnPublicMutAttrDao;
import io.ants.modules.sys.entity.CdnIpControlEntity;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.IpControlEnum;
import io.ants.modules.sys.enums.PublicEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.enums.RedisStreamType;
import io.ants.modules.sys.service.CdnMakeFileService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

/**
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/cdnsys/ip/control/")
public class CdnIpControlController extends AbstractController {

    @Autowired
    private CdnIpControlDao cdnIpControlDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private TbCdnPublicMutAttrDao publicMutAttrDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;

    private void getChild(CdnIpControlEntity item) {
        if (cdnIpControlDao
                .selectCount(new QueryWrapper<CdnIpControlEntity>().eq("parent_id", item.getId()).isNull("ip")) > 0) {
            List<CdnIpControlEntity> ls = cdnIpControlDao.selectList(
                    new QueryWrapper<CdnIpControlEntity>()
                            .eq("parent_id", item.getId())
                            .isNull("ip"));
            ls.forEach(itemC -> {
                getChild(itemC);
            });
            item.setChild(ls);
            item.setChildCount(Integer.toUnsignedLong(ls.size()));
        }
    }

    @PostMapping("/list")
    public R list(@RequestBody Map params) {
        List<Integer> controlLs = IpControlEnum.getAllControl();
        Integer page = 1;
        Integer limit = 20;
        int parentId = 0;
        Integer control = null;
        String ip = null;
        CdnIpControlEntity parentEntity = null;
        Integer group = 1;
        if (params.containsKey("page")) {
            page = (Integer) params.get("page");
        }
        if (params.containsKey("limit")) {
            limit = (Integer) params.get("limit");
        }
        if (params.containsKey("group")) {
            group = (Integer) params.get("group");
        }
        if (params.containsKey("control") && StringUtils.isNotBlank(params.get("control").toString())) {
            control = Integer.parseInt(params.get("control").toString());
            if (!controlLs.contains(control)) {
                control = null;
            }
        }
        if (params.containsKey("parentId") && StringUtils.isNotBlank(params.get("parentId").toString())) {
            parentId = Integer.parseInt(params.get("parentId").toString());
            parentEntity = cdnIpControlDao.selectById(parentId);
        }
        if (params.containsKey("ip") && StringUtils.isNotBlank(params.get("ip").toString())) {
            ip = params.get("ip").toString();
        }
        if (true) {
            // 1停用保存在公共里的数据
            publicMutAttrDao.update(null, new UpdateWrapper<TbCdnPublicMutAttrEntity>().and(
                    q -> q.eq("pkey", PublicEnum.WHITE_IP.getName()).or().eq("pkey", PublicEnum.BLACK_IP.getName()))
                    .set("status", 1));
            // 2 清理过期的临时封数据
            if (control.equals(IpControlEnum.FORBID_3_LITTLE.getId())) {
                Date tDate = DateUtils.addDateHours(new Date(), -1);
                cdnIpControlDao.delete(new QueryWrapper<CdnIpControlEntity>()
                        .eq("control", IpControlEnum.FORBID_3_LITTLE.getId()).le("create_time", tDate));
            }
        }
        if (1 == group) {
            List<CdnIpControlEntity> ls = cdnIpControlDao.selectList(new QueryWrapper<CdnIpControlEntity>()
                    .eq(null != control, "control", control).eq("parent_id", 0).isNull("ip"));
            ls.forEach(item -> {
                getChild(item);
            });
            return R.ok().put("data", new PageUtils(ls, ls.size(), 20, 1));
        } else {
            IPage<CdnIpControlEntity> iPage = cdnIpControlDao.selectPage(
                    new Page<>(page, limit),
                    new QueryWrapper<CdnIpControlEntity>()
                            .eq("parent_id", parentId)
                            .isNotNull("ip")
                            .eq(null != control, "control", control)
                            .orderByAsc("ip_source")
                            .like(StringUtils.isNotBlank(ip), "ip", ip));
            CdnIpControlEntity finalParentEntity = parentEntity;
            iPage.getRecords().forEach(item -> {
                item.setArea(IPSeeker.getIpAreaByNew(item.getIp()));
                if (null != finalParentEntity) {
                    item.setRemark(finalParentEntity.getRemark());
                }
                if (StringUtils.isBlank(item.getIp())) {
                    Long count = cdnIpControlDao
                            .selectCount(new QueryWrapper<CdnIpControlEntity>().eq("parent_id", item.getId()));
                    item.setChildCount(count);
                }
            });
            return R.ok().put("data", new PageUtils(iPage));
        }

    }

    private void opIpToNode(Integer parentId, String ip, Integer control, String mode) {
        if (null == parentId || 0 == parentId) {
            return;
        }
        if (StringUtils.isBlank(ip)) {
            return;
        }
        // mode: 1=add;2=delete
        switch (control) {
            case 1:
                // 7层加白
            case 2:
                // 7 层拉黑
                Map pushmap = new HashMap(8);
                pushmap.put(PushTypeEnum.IP_TRIE.getName(), "");
                cdnMakeFileService.pushByInputInfo(pushmap);
                break;
            case 3:
                // FORBID_3(3,"3层临时封禁")
                if (true) {
                    if ("add".equals(mode)) {
                        // add
                        if (IPUtils.isValidIPV4(ip) || IPUtils.isCidr(ip)) {
                            // String cmd="/usr/sbin/nft add element inet filter short_cc { "+ip+" }";
                            String cmd = "/usr/sbin/ipset add  short_cc  " + ip;
                            // logger.info(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "short_cc", cmd);
                        } else if (IPUtils.isValidIPV6(ip)) {
                            String cmd = "/usr/sbin/nft add element inet filter short_cc_v6 { " + ip + "   }";
                            // String cmd="/usr/sbin/ipset add short_cc { "+ip+" }";
                            // logger.info(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "short_cc", cmd);
                        } else {
                            logger.error("error_type[" + ip + "]add to 3层临时封禁");
                        }
                    } else if ("del".equals(mode)) {
                        // del
                        if (IPUtils.isValidIPV4(ip) || IPUtils.isCidr(ip)) {
                            // String cmd="/usr/sbin/nft delete element inet filter short_cc {"+ip+"}";
                            String cmd = "/usr/sbin/ipset del short_cc " + ip;
                            // logger.debug(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "short_cc", cmd);
                        } else if (IPUtils.isValidIPV6(ip)) {
                            String cmd = "/usr/sbin/nft delete  element inet filter short_cc_v6  {" + ip + "}";
                            // logger.debug(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "short_cc", cmd);
                        } else {
                            logger.error("error_type[" + ip + "]del to 3层临时封禁");
                        }
                    } else {
                        logger.error("[op_ip_to_node][1]unknown type");
                    }
                }
                break;
            case 4:
                // FORBID_3_LONG(4,"3层永久封禁"),
                if (true) {
                    if ("add".equals(mode)) {
                        // add
                        if (IPUtils.isValidIPV4(ip) || IPUtils.isCidr(ip)) {
                            // nft add element inet filter long_cc {2.2.2.1-2.2.2.5}
                            redisUtils.setAdd("long_cc", ip);
                            // String cmd="/usr/sbin/nft add element inet filter long_cc { "+ip+" }";
                            String cmd = "/usr/sbin/ipset add short_cc " + ip;
                            // logger.info(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "long_cc", cmd);
                        } else if (IPUtils.isValidIPV6(ip)) {
                            redisUtils.setAdd("long_cc_v6", ip);
                            String cmd = "/usr/sbin/nft add element inet filter long_cc_v6 { " + ip + " }";
                            // logger.info(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "long_cc", cmd);
                        } else {
                            logger.error("error_type[" + ip + "]add to 3层持久封禁");
                        }
                    } else if ("del".equals(mode)) {
                        // del
                        if (IPUtils.isValidIPV4(ip) || IPUtils.isCidr(ip)) {
                            redisUtils.setDel("long_cc", ip);
                            // String cmd="/usr/sbin/nft delete element inet filter long_cc {"+ip+"}";
                            String cmd = "/usr/sbin/ipset del short_cc " + ip;
                            // logger.debug(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "long_cc", cmd);
                        } else if (IPUtils.isValidIPV6(ip)) {
                            redisUtils.setDel("long_cc_v6", ip);
                            String cmd = "/usr/sbin/nft delete  element inet filter long_cc_v6  {" + ip + "}";
                            // logger.debug(cmd);
                            redisUtils.streamXAdd("public" + RedisStreamType.COMMAND.getName(), "long_cc", cmd);
                        } else {
                            logger.error("error_type[" + ip + "]dell to 3层持久封禁");
                        }
                    } else {
                        logger.error("[op_ip_to_node][2]unknown type");
                    }
                }
                break;
            default:
                break;
        }

    }

    private boolean saveEntity(CdnIpControlEntity ipControl, boolean sync) {
        if (IPUtils.isValidIPV4(ipControl.getIp())) {
            long[] ipLongStartEndVal = IPUtils.getLowerAndUpper(ipControl.getIp());
            if (null != ipLongStartEndVal && 2 == ipLongStartEndVal.length) {
                ipControl.setIpStart(ipLongStartEndVal[0]);
                ipControl.setIpEnd(ipLongStartEndVal[1]);
            } else {
                logger.error("ip is error");
                return false;
            }
            long ipLong = IPUtils.ip2long(ipControl.getIp());
            // 不在段内
            if (cdnIpControlDao.selectCount(new QueryWrapper<CdnIpControlEntity>()
                    .isNotNull("ip")
                    .eq("control", ipControl.getControl())
                    .le("ip_start", ipLong)
                    .ge("ip_end", ipLong)
                    .ne(null != ipControl.getId(), "id", ipControl.getId())) > 0) {
                // logger.debug(ipControl.getIp()+" is exist [1]");
                return false;
            }
        } else if (IPUtils.isCidr(ipControl.getIp())) {
            // ip段 类型IP
            long[] ipLongStartEndVal = IPUtils.getLowerAndUpper(ipControl.getIp());
            if (null != ipLongStartEndVal && 2 == ipLongStartEndVal.length) {
                ipControl.setIpStart(ipLongStartEndVal[0]);
                ipControl.setIpEnd(ipLongStartEndVal[1]);
            } else {
                logger.error("ip is error");
                return false;
            }
            // 删除段内IP
            cdnIpControlDao.delete(new QueryWrapper<CdnIpControlEntity>()
                    .isNotNull("ip")
                    .eq("control", ipControl.getControl())
                    .ge("ip_start", ipControl.getIpStart())
                    .le("ip_end", ipControl.getIpEnd())
                    .ne(null != ipControl.getId(), "id", ipControl.getId()));
        }

        if (sync) {
            cdnIpControlDao.insert(ipControl);
            // op_ip_to_node(ipControl.getParentId(),ipControl.getIp(),ipControl.getControl(),"add");
            return true;
        }
        if (StringUtils.isNotBlank(ipControl.getIp())) {
            if (null == ipControl.getIp()) {
                // logger.debug(ipControl.getIp()+" is empty [1]");
                return false;
            }
            CdnIpControlEntity parentEntity = cdnIpControlDao.selectById(ipControl.getParentId());
            ;
            if (null != parentEntity) {
                ipControl.setRemark(parentEntity.getRemark());
                ipControl.setControl(parentEntity.getControl());
            }
            boolean iPv4LiteralAddress = IPUtils.isValidIPV4(ipControl.getIp());
            boolean iPv6LiteralAddress = IPUtils.isValidIPV6(ipControl.getIp());
            boolean ipCidrAddress = IPUtils.isCidr(ipControl.getIp());
            if (!iPv4LiteralAddress && !iPv6LiteralAddress && !ipCidrAddress) {
                // logger.debug(ipControl.getIp()+" not a ip value [1]");
                return false;
            }
        }
        if (null == ipControl.getId() || 0 == ipControl.getId()) {
            // insert
            cdnIpControlDao.insert(ipControl);
            opIpToNode(ipControl.getParentId(), ipControl.getIp(), ipControl.getControl(), "add");
        } else {
            // modify
            CdnIpControlEntity old_ipEntity = cdnIpControlDao.selectById(ipControl.getId());
            if (old_ipEntity.getStatus().equals(ipControl.getStatus())) {
                // 状态不变的修改
                cdnIpControlDao.updateById(ipControl);
                // todo update
            } else {
                // 修改状态的修改
                cdnIpControlDao.updateById(ipControl);
                if (0 == ipControl.getStatus()) {
                    // 关闭状态的修改
                    opIpToNode(old_ipEntity.getParentId(), old_ipEntity.getIp(), old_ipEntity.getControl(), "del");
                } else {
                    // 打开状态的修改
                    opIpToNode(old_ipEntity.getParentId(), old_ipEntity.getIp(), old_ipEntity.getControl(), "add");
                }
            }

            Map pushMap = new HashMap(8);
            pushMap.put(PushTypeEnum.IP_TRIE.getName(), "");
            cdnMakeFileService.pushByInputInfo(pushMap);

        }
        return true;
    }

    @PostMapping("/save")
    public R save(@RequestBody Map params) {
        CdnIpControlEntity ipControl = DataTypeConversionUtil.map2entity(params, CdnIpControlEntity.class);
        ipControl.setIpSource(0);
        ipControl.setStatus(1);
        if (StringUtils.isBlank(ipControl.getIp()) && StringUtils.isBlank(ipControl.getIps())) {
            ipControl.setParentId(0);
            if (null == ipControl.getId() || 0 == ipControl.getId()) {
                cdnIpControlDao.insert(ipControl);
            } else {
                cdnIpControlDao.updateById(ipControl);
            }
            return R.ok().put("data", 1);
        }
        // save_entity(ipControl,false);
        if (null == ipControl.getParentId() || 0 == ipControl.getParentId()) {
            CdnIpControlEntity parentEntity = cdnIpControlDao.selectOne(new QueryWrapper<CdnIpControlEntity>()
                    .eq("parent_id", 0)
                    .eq("ip_source", 0)
                    .eq("control", ipControl.getControl())
                    .eq("status", 1)
                    .last("limit 1"));
            if (null != parentEntity) {
                ipControl.setParentId(parentEntity.getId());
            } else {
                throw new RRException("无默认分组！");
            }
        }
        Integer count = 0;
        if (StringUtils.isNotBlank(ipControl.getIps())) {
            // 多IP
            for (String ip : ipControl.getIps().split(",")) {
                CdnIpControlEntity ipControl_i = new CdnIpControlEntity();
                ipControl_i.setId(0);
                ipControl_i.setParentId(ipControl.getParentId());
                ipControl_i.setIp(ip);
                ipControl_i.setIpSource(ipControl.getIpSource());
                ipControl_i.setControl(ipControl.getControl());
                ipControl_i.setRemark(ipControl.getRemark());
                ipControl_i.setStatus(ipControl.getStatus());
                if (this.saveEntity(ipControl_i, false)) {
                    count++;
                }
            }
        } else if (StringUtils.isNotBlank(ipControl.getIp())) {
            // 单个保存
            if (this.saveEntity(ipControl, false)) {
                count++;
            }
        }

        return R.ok().put("data", count);
    }

    @PostMapping("/bat/child/save")
    public R batSave(@RequestBody Map params) {
        Integer parentId = 0;
        String ips = null;
        if (!params.containsKey("parentId") && !params.containsKey("ips")) {
            return R.error("参数不完整！【parentId】【ips】");
        }
        parentId = Integer.parseInt(params.get("parentId").toString());
        ips = params.get("ips").toString();

        CdnIpControlEntity p_IpControlEntity = cdnIpControlDao.selectById(parentId);
        if (null == p_IpControlEntity) {
            return R.error("不存在的分组");
        }

        int success = 0;
        for (String ip : ips.split(",")) {
            CdnIpControlEntity entity = new CdnIpControlEntity();
            entity.setId(0);
            entity.setParentId(parentId);
            entity.setControl(p_IpControlEntity.getControl());
            entity.setIpSource(p_IpControlEntity.getIpSource());
            entity.setRemark(p_IpControlEntity.getRemark());
            entity.setStatus(1);
            entity.setIp(ip);
            boolean saveStatus = this.saveEntity(entity, false);
            if (saveStatus) {
                success++;
            }
        }
        return R.ok().put("success", success);
    }

    private void syncTargetHandle(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            if (obj.containsKey("id") && obj.containsKey("remark") && obj.containsKey("type")) {
                String local_remark = String.format("[%d]%s", obj.getInteger("id"), obj.getString("remark"));
                Integer c_parent_id = obj.getInteger("parent_id");
                CdnIpControlEntity parentEntity = null;
                if (0 != c_parent_id) {
                    String p_remark = String.format("[%d]", obj.getInteger("parent_id"));
                    parentEntity = cdnIpControlDao
                            .selectOne(new QueryWrapper<CdnIpControlEntity>().like("remark", p_remark).last("limit 1"));
                }
                CdnIpControlEntity t_entity = new CdnIpControlEntity();
                if (null == parentEntity) {
                    t_entity.setParentId(0);
                } else {
                    t_entity.setParentId(parentEntity.getId());
                }
                if (StringUtils.isNotBlank(obj.getString("ip"))) {
                    t_entity.setIp(obj.getString("ip").trim());
                }
                t_entity.setIpSource(1);
                t_entity.setControl(obj.getInteger("type"));
                t_entity.setRemark(local_remark);
                t_entity.setStatus(1);
                this.saveEntity(t_entity, true);
            }
        }
    }

    private void ipEntityDeleteById(Integer id) {
        List<CdnIpControlEntity> ls = cdnIpControlDao
                .selectList(new QueryWrapper<CdnIpControlEntity>().eq("parent_id", id));
        for (CdnIpControlEntity item : ls) {
            ipEntityDeleteById(item.getId());
        }
        CdnIpControlEntity old_ipEntity = cdnIpControlDao.selectById(id);
        if (null != old_ipEntity) {
            opIpToNode(old_ipEntity.getParentId(), old_ipEntity.getIp(), old_ipEntity.getControl(), "del");
            cdnIpControlDao.deleteById(id);
        }
    }

    private void syncIpHandle(Integer id) {
        if (null == id || 0 == id) {
            // 1 删除所有同步源数据
            cdnIpControlDao.delete(new QueryWrapper<CdnIpControlEntity>().eq("ip_source", 1));
            // 2
            String url = QuerySysAuth.getSyncIpAddress(null);
            try {
                String res = HttpRequest.curlHttpGet(url);
                JSONArray jsonArray = DataTypeConversionUtil.string2JsonArray(res);
                this.syncTargetHandle(jsonArray);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            CdnIpControlEntity p_IpControlEntity = cdnIpControlDao.selectById(id);
            if (null == p_IpControlEntity) {
                return;
            }
            if (0 != p_IpControlEntity.getParentId()) {
                return;
            }
            if (1 != p_IpControlEntity.getIpSource()) {
                return;
            }
            try {
                // 递归 删除所有子
                List<CdnIpControlEntity> ls = cdnIpControlDao
                        .selectList(new QueryWrapper<CdnIpControlEntity>().eq("parent_id", id));
                for (CdnIpControlEntity item : ls) {
                    this.ipEntityDeleteById(item.getId());
                }

                // 远程更新
                String p_remark = p_IpControlEntity.getRemark();
                Pattern pattern = compile("\\[\\d+\\]");
                Matcher m = pattern.matcher(p_remark);
                String str = "";
                if (m.find()) {
                    str = m.group(0);
                }
                if (StringUtils.isNotBlank(str)) {
                    str = str.replace("[", "");
                    str = str.replace("]", "");
                    String url = QuerySysAuth.getSyncIpAddress(str);
                    String res = HttpRequest.curlHttpGet(url);
                    JSONArray jsonArray = DataTypeConversionUtil.string2JsonArray(res);
                    this.syncTargetHandle(jsonArray);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void syncHandle(Integer id) {
        if (StaticVariableUtils.sync_ip_data_handle) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                StaticVariableUtils.sync_ip_data_handle = true;
                syncIpHandle(id);
                Map map = new HashMap();
                map.put(PushTypeEnum.IP_TRIE.getName(), "null");
                cdnMakeFileService.pushByInputInfo(map);
                StaticVariableUtils.sync_ip_data_handle = false;
            }
        }).start();
    }

    @GetMapping("/sync/ants/ip/data")
    public R sync_ants_ip_data(@RequestParam Integer id) {
        if (StaticVariableUtils.sync_ip_data_handle) {
            return R.error("IP 正在后台同步中！请稍候");
        }
        this.syncHandle(id);
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok();
    }

    @PostMapping("/batch/delete")
    public R delete(@RequestBody Map params) {
        if (!params.containsKey("ids")) {
            return R.error("参数缺失！");
        }
        String ids = params.get("ids").toString();
        for (String id : ids.split(",")) {
            CdnIpControlEntity ipControl = cdnIpControlDao.selectById(id);
            if (null != ipControl) {
                if (0 != ipControl.getIpSource()) {
                    // 删除 云IP
                    throw new RRException("删除失败！");
                } else if (0 != ipControl.getParentId() && 0 == ipControl.getIpSource()) {
                    // 删除自定义分组内的IP
                    if (StringUtils.isNotBlank(ipControl.getIp())) {
                        opIpToNode(ipControl.getParentId(), ipControl.getIp(), ipControl.getControl(), "del");
                    }
                    cdnIpControlDao.deleteById(id);
                } else if (0 == ipControl.getParentId() && 0 == ipControl.getIpSource()) {
                    // 删除自定义分组
                    List<CdnIpControlEntity> c_list = cdnIpControlDao
                            .selectList(new QueryWrapper<CdnIpControlEntity>().eq("parent_id", id));
                    for (CdnIpControlEntity ipControlEntity : c_list) {
                        opIpToNode(ipControlEntity.getParentId(), ipControlEntity.getIp(), ipControlEntity.getControl(),
                                "del");
                        cdnIpControlDao.deleteById(ipControlEntity.getId());
                    }
                    cdnIpControlDao.deleteById(id);
                }
            }
        }
        return R.ok();
    }
}
