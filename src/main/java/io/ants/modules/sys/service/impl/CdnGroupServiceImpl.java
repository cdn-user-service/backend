package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.*;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.CdnGroupService;
import io.ants.modules.sys.service.DnsCApiService;
import io.ants.modules.sys.vo.EditGroupClientDnsVo;
import io.ants.modules.utils.vo.DnsRecordItemVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Service
public class CdnGroupServiceImpl implements CdnGroupService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CdnProductDao cdnProductDao;
    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;
    @Autowired
    private CdnClientGroupChildConfDao cdnClientGroupChildConfDao;
    @Autowired
    private CdnClientAreaDao cdnClientAreaDao;
    @Autowired
    private DnsCApiService dnsCApiService;

    private String GetGroupClientIdsCount(Integer groupId) {
        List<CdnClientGroupChildConfEntity> list = cdnClientGroupChildConfDao
                .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("group_id", groupId)
                        .select("client_id").eq("parent_id", 0));
        List<Integer> list1 = list.stream().map(t -> t.getClientId()).collect(Collectors.toList());
        return list1.size() + "";
    }

    @Override
    public R clientGroupList(Integer page, Integer limit, String key) {
        IPage<CdnClientGroupEntity> ipage = cdnClientGroupDao.selectPage(
                new Page<CdnClientGroupEntity>(page, limit),
                new QueryWrapper<CdnClientGroupEntity>()
                        .orderByDesc("weight")
                        .like(StringUtils.isNotBlank(key), "name", key));
        ipage.getRecords().forEach(item -> {
            item.setClientInfos(null);
            item.setClientIds(GetGroupClientIdsCount(item.getId()));
            TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(item.getDnsConfigId());
            item.setStatus(0);
            if (null != dnsConfig) {
                if (1 == dnsConfig.getStatus()) {
                    item.setCname("*." + item.getHash() + "." + dnsConfig.getAppDomain());
                    item.setStatus(1);
                }
                if (null == item.getAreaId() || 0 == item.getAreaId()) {
                    if (null == item.getAreaId()) {
                        item.setAreaId(0);
                    }
                    item.setAreaName("默认");
                } else {
                    CdnClientAreaEntity areaEntity = cdnClientAreaDao.selectById(item.getAreaId());
                    if (null != areaEntity) {
                        item.setAreaName(areaEntity.getName());
                    }
                }
            }
            cdnClientGroupDao.updateById(item);

        });
        // --
        List<CdnClientGroupChildConfEntity> allDCList = cdnClientGroupChildConfDao
                .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                        .groupBy("group_id")
                        .select("group_id"));
        List<Integer> gList = allDCList.stream().map(o -> o.getGroupId()).collect(Collectors.toList());
        return R.ok().put("data", new PageUtils(ipage)).put("groupArray", gList);
    }

    @Override
    public CdnClientGroupEntity addGroupClient(Integer groupId, Integer parentId, String clientIds, String line,
            Long ttl) {
        CdnClientGroupEntity group = cdnClientGroupDao.selectById(groupId);
        if (null == group) {
            throw new RRException("分组" + groupId + "为空");
        }
        if (1 == group.getRecordMode()) {
            throw new RRException("GTM 分组不可添加节点");
        }
        for (String client_id : clientIds.split(",")) {
            Long count = cdnClientGroupChildConfDao.selectCount(
                    new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id", groupId)
                            .eq("client_id", client_id)
                            .eq("parent_id", parentId));
            if (0 == count) {
                CdnClientGroupChildConfEntity groupDnsConfEntity = new CdnClientGroupChildConfEntity();
                groupDnsConfEntity.setGroupId(groupId);
                groupDnsConfEntity.setClientId(Integer.parseInt(client_id));
                groupDnsConfEntity.setParentId(parentId);
                groupDnsConfEntity.setLine(line);
                groupDnsConfEntity.setTtl(ttl);
                groupDnsConfEntity.setStatus(1);
                cdnClientGroupChildConfDao.insert(groupDnsConfEntity);
            }
        }
        group.setClientIds(GetGroupClientIdsCount(groupId));
        return group;
    }

    private Map<String, Integer> getIpsUsedCount() {
        Map<String, Integer> map = new HashMap();
        List<CdnClientGroupEntity> list = cdnClientGroupDao
                .selectList(new QueryWrapper<CdnClientGroupEntity>().eq("status", 1));
        list.forEach(item -> {
            String client_ids = item.getClientIds();
            if (StringUtils.isNotBlank(client_ids)) {
                for (String id : client_ids.split(",")) {
                    if (map.containsKey(id)) {
                        Integer count = (Integer) map.get(id);
                        map.put(id, count + 1);
                    } else {
                        map.put(id, 1);
                    }
                }
            }
        });
        return map;
    }

    @Override
    public List<CdnClientEntity> UnUsedClientIpsByGroup(Integer groupId, Integer parentId) {
        CdnClientGroupEntity groupEntity = cdnClientGroupDao.selectById(groupId);
        if (null == groupEntity) {
            throw new RRException("无此分组");
        }
        Map<String, Integer> map = getIpsUsedCount();
        List<CdnClientEntity> clientIdsList = cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .isNull("parent_id")
                .eq("area_id", groupEntity.getAreaId())
                .eq("client_type", 1));
        // List<String>
        // allClientIds=clientIdsList.stream().map(t->t.getId().toString()).collect(Collectors.toList());
        // allClientIds.removeAll(uedIdList);
        clientIdsList.forEach(item -> {
            item.setInGroupCount(map.get(item.getId().toString()));
            Long c_count = cdnClientGroupChildConfDao.selectCount(new QueryWrapper<CdnClientGroupChildConfEntity>()
                    .eq("group_id", groupId)
                    .eq("parent_id", parentId)
                    .eq("client_id", item.getId()));
            if (c_count > 0) {
                item.setStatus(0);
            }
            List<CdnClientEntity> list = cdnClientDao
                    .selectList(new QueryWrapper<CdnClientEntity>().eq("parent_id", item.getId()));
            list.forEach(item2 -> {
                item2.setInGroupCount(map.get(item.getId().toString()));
                item2.setArea(item.getArea());
                item2.setLine(item.getLine());
                item2.setRemark(item.getRemark());
                item2.setStatus(1);
                Long c_c_count = cdnClientGroupChildConfDao
                        .selectCount(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("group_id", groupId)
                                .eq("parent_id", parentId).eq("client_id", item2.getId()));
                if (c_c_count > 0) {
                    item.setStatus(0);
                }
            });
            item.setChildBackupIpList(list);
        });
        return clientIdsList;
    }

    private void recursionGetList(CdnClientGroupChildConfEntity groupDetailEntity) {
        CdnClientEntity clientEntity = cdnClientDao.selectById(groupDetailEntity.getClientId());
        groupDetailEntity.setClientEntity(clientEntity);
        List<CdnClientGroupChildConfEntity> nodeList = cdnClientGroupChildConfDao
                .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                        .eq("group_id", groupDetailEntity.getGroupId()).eq("parent_id", groupDetailEntity.getId()));
        for (CdnClientGroupChildConfEntity groupConf : nodeList) {
            recursionGetList(groupConf);
        }
        groupDetailEntity.setChild(nodeList);
    }

    @Override
    public PageUtils groupNodeStableInfosList(Integer page, Integer limit, Integer groupId, String key) {
        IPage<CdnClientGroupEntity> ipage = cdnClientGroupDao.selectPage(
                new Page<CdnClientGroupEntity>(page, limit),
                new QueryWrapper<CdnClientGroupEntity>()
                        .orderByDesc("weight")
                        .eq(null != groupId, "id", groupId)
                        .like(StringUtils.isNotBlank(key), "name", key));
        ipage.getRecords().forEach(item -> {
            List<CdnClientGroupChildConfEntity> parentNodeList = cdnClientGroupChildConfDao.selectList(
                    new QueryWrapper<CdnClientGroupChildConfEntity>().eq("group_id", item.getId()).eq("parent_id", 0));
            for (CdnClientGroupChildConfEntity groupDetailEntity : parentNodeList) {
                recursionGetList(groupDetailEntity);
            }
            item.setClientInfos(parentNodeList);
        });
        return new PageUtils(ipage);
    }

    @Override
    public R saveCdnGroup(CdnClientGroupEntity inputGroup) {

        if (null == inputGroup.getAreaId()) {
            return R.error("areaId id is empty！");
        }
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectOne(new QueryWrapper<TbDnsConfigEntity>()
                .eq("user_type", UserTypeEnum.MANAGER_TYPE.getId())
                .eq("id", inputGroup.getDnsConfigId())
                .eq("status", 1).last("limit 1"));
        if (null == dnsConfig) {
            return R.error("dnsConfigId 暂不可用");
        }
        if (null != inputGroup.getId() && 0 != inputGroup.getId()) {
            // update
            CdnClientGroupEntity sourceGroup = cdnClientGroupDao.selectById(inputGroup.getId());
            if (null == sourceGroup) {
                return R.error("无此 DNS  配置");
            }

            // delete__gtm--cname---ADD
            boolean delFlag = false;
            if (1 == sourceGroup.getRecordMode()) {
                if (1 != inputGroup.getRecordMode()) {
                    delFlag = true;
                } else if (1 == inputGroup.getRecordMode() && !sourceGroup.getRdata().equals(inputGroup.getRdata())) {
                    delFlag = true;
                }
            } else if (0 == sourceGroup.getRecordMode() && 1 == inputGroup.getRecordMode()) {
                delFlag = true;
            }
            if (delFlag) {
                String sTop = String.format("*.%s", sourceGroup.getHash());
                dnsCApiService.removeRecordByInfo(sourceGroup.getDnsConfigId(), sTop, "CNAME", "",
                        sourceGroup.getRdata(), "600");
                if (1 == inputGroup.getRecordMode() && StringUtils.isBlank(inputGroup.getRdata())) {
                    return R.error("rdata is null!");
                }
                String nTop = String.format("*.%s", inputGroup.getHash());
                dnsCApiService.addRecordByConfId(inputGroup.getDnsConfigId(), nTop, "CNAME", "", inputGroup.getRdata(),
                        "600");
            }

            Long count = cdnClientGroupChildConfDao
                    .selectCount(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("group_id", inputGroup.getId()));
            if (count > 0) {
                if (!sourceGroup.getAreaId().equals(inputGroup.getAreaId())) {
                    return R.error("当前业务分组中存在节点， 不可修改节点分组！！");
                }
            }
            Map newObjMap = DataTypeConversionUtil.entity2map(inputGroup);
            if (null != newObjMap) {
                DataTypeConversionUtil.updateEntity(DataTypeConversionUtil.entity2map(sourceGroup), newObjMap);
                inputGroup = DataTypeConversionUtil.map2entity(newObjMap, CdnClientGroupEntity.class);
                cdnClientGroupDao.updateById(inputGroup);
            }
        } else {
            String hash = HashUtils.md5ofString(System.currentTimeMillis() + "0").substring(0, 10);
            inputGroup.setHash(hash);
            cdnClientGroupDao.insert(inputGroup);
            String nTop = String.format("*.%s", inputGroup.getHash());
            if (1 == inputGroup.getRecordMode() && StringUtils.isBlank(inputGroup.getRdata())) {
                return R.error("rdata is null!");
            }
            dnsCApiService.addRecordByConfId(inputGroup.getDnsConfigId(), nTop, "CNAME", "", inputGroup.getRdata(),
                    "600");
        }
        return R.ok().put("data", inputGroup);
    }

    @Override
    public R deleteCdnGroup(String ids) {
        List<String> msgList = new ArrayList<>();
        Integer success = 0;
        Integer fail = 0;
        for (String id : ids.split(",")) {
            Long count = cdnProductDao.selectCount(new QueryWrapper<CdnProductEntity>()
                    .likeRight("server_group_ids", id + ",")
                    .or()
                    .likeLeft("server_group_ids", "," + id)
                    .or()
                    .like("server_group_ids", "," + id + ",")
                    .or()
                    .eq("server_group_ids", id)
                    .eq("is_delete", 0));
            if (0 == count) {
                cdnClientGroupChildConfDao.delete(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("group_id", id));
                cdnClientGroupDao.deleteById(id);
                success++;
            } else {
                throw new RRException("组[" + id + "]已关联产品套餐，删除失败！");
                // fail++;
                // msgList.add("组["+id+"]已关联产品套餐，删除失败！");
            }
        }
        return R.ok().put("success", success).put("fail", fail).put("msg", msgList);
    }

    // 从记录中获取IP的解析记录
    private DnsRecordItemVo getRecord(List<DnsRecordItemVo> r_list, String value) {
        if (null == r_list) {
            return null;
        }
        for (DnsRecordItemVo obj : r_list) {
            if (IPUtils.ipCompare(obj.getValue(), value)) {
                return obj;
            }
        }
        return null;
        // 2607:f130:0:1a0::b461:3bd3
        // 2607:f130:0000:01a0:0000:0000:b461:3bd3
    }

    /**
     * //从网络dns api 中获取DNS info 保存本地
     * 
     * @param groupClient
     * @param allRecordList 所有解析记录列表
     * @return
     */
    private CdnClientGroupChildConfEntity getNetRecordInfo2Entity(CdnClientGroupChildConfEntity groupClient,
            List<DnsRecordItemVo> allRecordList) {
        CdnClientEntity client = cdnClientDao.selectById(groupClient.getClientId());
        if (null == client) {
            groupClient.setClientEntity(new CdnClientEntity());
            groupClient.setStatus(0);
            groupClient.setLine(null);
            groupClient.setTtl(null);
            groupClient.setDnsType(0);
            return groupClient;
        }
        if (null != client) {
            groupClient.setClientIp(client.getClientIp());
            groupClient.setClientEntity(client);
            // 从记录中获取IP的解析记录
            DnsRecordItemVo r_info = this.getRecord(allRecordList, client.getClientIp());
            if (null != r_info) {
                groupClient.setLine(r_info.getLine());
                groupClient.setTtl(Long.valueOf(r_info.getTtl()));
                groupClient.setRecordInfos(r_info);
                groupClient.setDnsType(1);
            }
            return groupClient;
        }
        // groupClient.setTtl(null);
        groupClient.setRecordInfos(null);
        return groupClient;
    }

    /**
     * 获取业务分组中节点信息
     * 
     * @param group
     * @return
     */
    private List<CdnClientGroupChildConfEntity> getClientInfosV2(CdnClientGroupEntity group) {
        if (null == group) {
            return null;
        }
        String cname = "";
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(group.getDnsConfigId());
        if (null != dnsConfig) {
            if (1 == dnsConfig.getStatus()) {
                cname = "*." + group.getHash() + "." + dnsConfig.getAppDomain();
            }
        }
        // 从API 中获取远程解析数据
        List<DnsRecordItemVo> dnsRemoteServerRecordInfoList = dnsCApiService
                .getRecordByInfoToList(group.getDnsConfigId(), "*." + group.getHash(), "A", null);
        List<DnsRecordItemVo> dnsRemoteServerRecordInfoListV6 = dnsCApiService
                .getRecordByInfoToList(group.getDnsConfigId(), "*." + group.getHash(), "AAAA", null);
        dnsRemoteServerRecordInfoList.addAll(dnsRemoteServerRecordInfoListV6);
        if (null == dnsRemoteServerRecordInfoList) {
            dnsRemoteServerRecordInfoList = new ArrayList<>();
        }

        // 从表中获取本地数据
        List<CdnClientGroupChildConfEntity> localDbClientRecordInfoList = cdnClientGroupChildConfDao
                .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                        .eq("group_id", group.getId())
                        .eq("parent_id", 0));
        for (CdnClientGroupChildConfEntity parentGroupClientItem : localDbClientRecordInfoList) {
            // 从网络dns api 中获取DNS info 保存本地
            getNetRecordInfo2Entity(parentGroupClientItem, dnsRemoteServerRecordInfoList);

            // 获取子
            List<CdnClientGroupChildConfEntity> childList = cdnClientGroupChildConfDao
                    .selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id", group.getId())
                            .eq("parent_id", parentGroupClientItem.getId()));
            Integer finalParentGroupClientDnsType = parentGroupClientItem.getDnsType();
            CdnClientGroupChildConfEntity finalParentGroupClientItem = parentGroupClientItem;
            for (CdnClientGroupChildConfEntity childIpItem : childList) {
                childIpItem.setLine(finalParentGroupClientItem.getLine());
                getNetRecordInfo2Entity(childIpItem, dnsRemoteServerRecordInfoList);
                if (null != finalParentGroupClientDnsType && 1 != finalParentGroupClientDnsType) {
                    if (1 == childIpItem.getDnsType()) {
                        finalParentGroupClientDnsType = 2;
                    }
                }
                childIpItem.setCname(cname);
            }
            parentGroupClientItem.setDnsType(finalParentGroupClientDnsType);
            parentGroupClientItem.setChild(childList);
            parentGroupClientItem.setCname(cname);
        }
        return localDbClientRecordInfoList;
    }

    @Override
    public R modifyClientDnsInfo(EditGroupClientDnsVo vo) {

        CdnClientGroupChildConfEntity groupChildConf = cdnClientGroupChildConfDao.selectById(vo.getId());
        if (null == groupChildConf) {
            return R.error("修改失败！ID is error");
        }
        // size = 7
        // {"recordId":"778592869473490944","top":"*.7edd32c78e","line":"default","recordType":"A","type":"A","value":"119.97.137.47","ttl":601}
        if (1 == vo.getStatus()) {
            // 开启
            CdnClientGroupEntity group = cdnClientGroupDao.selectById(groupChildConf.getGroupId());
            if (null == group) {
                return R.error("修改失败！groupId is error");
            }
            TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(group.getDnsConfigId());
            if (null == dnsConfig) {
                return R.error("修改失败！dnsConfig is error");
            }
            // 获取当前分组中的所有dns记录
            List<DnsRecordItemVo> remoteRecordList = dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),
                    "*." + group.getHash(), "A", null);
            // 从网络dns api 中获取DNS info 保存本地
            CdnClientGroupChildConfEntity newGroupClientDns = this.getNetRecordInfo2Entity(groupChildConf,
                    remoteRecordList);
            if (null == newGroupClientDns || null == newGroupClientDns.getRecordInfos()) {
                UpdateWrapper<CdnClientGroupChildConfEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", vo.getId())
                        .set(null != vo.getTtl(), "ttl", Integer.parseInt(vo.getTtl().toString()))
                        .set(null != vo.getLine(), "line", vo.getLine())
                        .set(null != vo.getStatus(), "status", vo.getStatus());
                cdnClientGroupChildConfDao.update(null, updateWrapper);
                return R.ok().put("data", remoteRecordList).put("msg", "new");
                // return R.error("修改失败！获取dns api 数据 失败[1]！");
            }
            DnsRecordItemVo recordObj = newGroupClientDns.getRecordInfos();
            if (StringUtils.isBlank(recordObj.getTop()) || StringUtils.isBlank(recordObj.getValue())) {
                return R.error("修改失败！获取dns api 数据异常[2]！");
            }
            if (!recordObj.getTtl().equals(vo.getTtl()) || !recordObj.getLine().equals(vo.getLine())) {
                // 对线路 或 TTL 发生了修改
                R rModify = dnsCApiService.modifyRecord(dnsConfig, recordObj.getRecordId(), recordObj.getTop(), "A",
                        vo.getLine(), recordObj.getValue(), vo.getTtl().toString());
                if (null == rModify || 0 == rModify.getCode()) {
                    logger.error(String.format("recordId:%s,top:%s,line:%s,value:%s,ttl:%d", recordObj.getRecordId(),
                            recordObj.getTop(), vo.getLine(), recordObj.getValue(), vo.getTtl()));
                    return rModify;
                }
                // logger.debug("modify-->"+ rModify);
                UpdateWrapper<CdnClientGroupChildConfEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", vo.getId())
                        .set(null != vo.getTtl(), "ttl", vo.getTtl())
                        .set(null != vo.getLine(), "line", vo.getLine())
                        .set(null != vo.getStatus(), "status", vo.getStatus());
                cdnClientGroupChildConfDao.update(null, updateWrapper);
                // 同步修改备线路
                cdnClientGroupChildConfDao.update(null, new UpdateWrapper<CdnClientGroupChildConfEntity>()
                        .eq("parent_id", vo.getId())
                        .set(null != vo.getTtl(), "ttl", vo.getTtl())
                        .set(null != vo.getLine(), "line", vo.getLine()));
                return R.ok().put("msg", "update dns record success");

            }
        } else if (0 == vo.getStatus()) {
            // 关闭解析
            if (StringUtils.isNotBlank(vo.getLine())) {
                groupChildConf.setLine(vo.getLine());
            }
            groupChildConf.setStatus(0);
            cdnClientGroupChildConfDao.updateById(groupChildConf);
            CdnClientGroupEntity group = cdnClientGroupDao.selectById(groupChildConf.getGroupId());
            if (null == group) {
                return R.error("Status=0,group=null");
            }
            TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(group.getDnsConfigId());
            if (null == dnsConfig) {
                return R.error("Status=0,dnsConfig=null");
            }
            List<DnsRecordItemVo> readNetRList = dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),
                    "*." + group.getHash(), "A", null);
            /// 从网络dns api 中获取DNS info 保存本地
            CdnClientGroupChildConfEntity newGroupClientDns = this.getNetRecordInfo2Entity(groupChildConf,
                    readNetRList);
            if (null == newGroupClientDns.getRecordInfos()) {
                return R.ok();
            }
            DnsRecordItemVo rItem = newGroupClientDns.getRecordInfos();
            return dnsCApiService.removeRecordByRecordIdAndDnsId(group.getDnsConfigId(), rItem.getRecordId());
        } else {
            logger.error("unknown status value!");
        }
        return R.ok();
    }

    @Override
    public R getGroupDetail(Integer groupId) {
        CdnClientGroupEntity groupEntity = cdnClientGroupDao.selectById(groupId);
        if (null == groupEntity) {
            return R.error("分组不存在！");
        }
        // List<CdnClientEntity> list=new ArrayList<>();
        groupEntity.setClientIds(null);
        groupEntity.setClientInfos(this.getClientInfosV2(groupEntity));

        return R.ok().put("data", groupEntity);
    }

}
