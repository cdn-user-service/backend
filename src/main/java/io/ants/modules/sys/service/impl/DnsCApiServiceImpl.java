package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencentcloudapi.dnspod.v20210323.models.RecordListItem;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.dao.TbDnsConfigDao;
import io.ants.modules.sys.entity.TbDnsConfigEntity;
import io.ants.modules.sys.enums.DnsApiEnum;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.DnsCApiService;
import io.ants.modules.sys.vo.AntsDnsRecordVo;
import io.ants.modules.sys.vo.CfDnsRecordVo;
import io.ants.modules.sys.vo.DnsCommonRecordVo;
import io.ants.modules.sys.vo.GodaddyRecordVo;

import io.ants.modules.utils.vo.AntsDnsRecordItemVo;
import io.ants.modules.utils.vo.DnsRecordItemVo;
import io.ants.modules.utils.service.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DnsCApiServiceImpl implements DnsCApiService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;

    @Override
    public PageUtils list(Map params, int userType, long userId) {
        int page = 1;
        int limit = 10;
        String userIds = "";
        String source = "";
        if (params.containsKey("page")) {
            page = Integer.parseInt(params.get("page").toString());
        }
        if (params.containsKey("limit")) {
            limit = Integer.parseInt(params.get("limit").toString());
        }
        if (userType == UserTypeEnum.USER_TYPE.getId()) {
            userIds = String.valueOf(userId);
        }
        if (params.containsKey("source")) {
            source = params.get("source").toString();
        }
        IPage<TbDnsConfigEntity> ipage = tbDnsConfigDao.selectPage(
                new Page<>(page, limit),
                new QueryWrapper<TbDnsConfigEntity>()
                        .orderByDesc("id")
                        .in(StringUtils.isNotBlank(userIds), "user_id", userIds.split(","))
                        .like(StringUtils.isNotBlank(source), "source", source)
                        .eq("user_type", userType));
        ipage.getRecords().forEach(item -> {
            if (!item.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
                item.setAppUrl(null);
            }
            R rLine = this.getDnsLine(item, 0);
            item.setStatus(0);
            if (null != rLine && 1 == rLine.getCode()) {
                item.setStatus(1);
            }
        });
        return new PageUtils(ipage);

    }

    @Override
    public R allList(Integer userType, Long userId) {
        List<TbDnsConfigEntity> list = tbDnsConfigDao.selectList(new QueryWrapper<TbDnsConfigEntity>()
                .orderByDesc("id")
                .eq(userType == 2, "user_id", userId)
                .eq("user_type", userType));
        return R.ok().put("data", list);
    }

    @Override
    public R verifyMainDomainInDns(Integer dnsConfigId, String mainDomain) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            dnsConfig.setAppDomain(mainDomain);
            return this.verifyMainDomainInDns(dnsConfig);
        }
        return R.error("dnsConfigId error");
    }

    private R verifyMainDomainInDns(TbDnsConfigEntity dnsConfig) {
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.GetRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.getDescribeDomainInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy xxx
            return GodaddyDnsApiService.RecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.getRecordList(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(),
                    dnsConfig.getAppId(), dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.getDomainInZone(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // XDPDNS
            return DnsXdpDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("config is not supported");
    }

    @Override
    public TbDnsConfigEntity save(Map params) {
        TbDnsConfigEntity dnsConfig = DataTypeConversionUtil.map2entity(params, TbDnsConfigEntity.class);
        dnsConfig.setAppId(dnsConfig.getAppId().trim());
        dnsConfig.setAppKey(dnsConfig.getAppKey().trim());
        if (null != dnsConfig.getId()) {
            TbDnsConfigEntity t_dnsConfig = tbDnsConfigDao.selectById(dnsConfig.getId());
            if (null != t_dnsConfig) {
                dnsConfig.setStatus(0);
                // todo 检测DNS 正确性
                if (null != this.getDnsLine(dnsConfig, 0)) {
                    dnsConfig.setStatus(1);
                }
                tbDnsConfigDao.updateById(dnsConfig);
                return dnsConfig;
            }
        }
        // todo 检测DNS 正确性
        dnsConfig.setStatus(0);
        if (null != this.getDnsLine(dnsConfig, 0)) {
            dnsConfig.setStatus(1);
        }
        tbDnsConfigDao.insert(dnsConfig);
        return dnsConfig;
    }

    @Override
    public void delete(String ids, int userType, long userId) {
        String[] id_s = ids.split(",");
        for (String id : id_s) {
            if (userType == UserTypeEnum.USER_TYPE.getId()) {
                Long count = tbDnsConfigDao.selectCount(new QueryWrapper<TbDnsConfigEntity>()
                        .eq("user_id", userId)
                        .eq("user_type", UserTypeEnum.USER_TYPE.getId())
                        .eq("id", id)
                        .last("limit 1"));
                if (0 == count) {
                    logger.info("delete failed:" + id);
                    continue;
                }
            }
            tbDnsConfigDao.deleteById(id);
        }
    }

    @Override
    public List<TbDnsConfigEntity> allList() {
        List<TbDnsConfigEntity> list = tbDnsConfigDao.selectList(new QueryWrapper<TbDnsConfigEntity>()
                .eq("user_type", UserTypeEnum.MANAGER_TYPE.getId())
                .eq("status", 1)
                .orderByDesc("id"));
        return list;
    }

    @Override
    public R getDnsLine(TbDnsConfigEntity dnsConfig, Integer parentId) {
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.getLine(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.getLine(dnsConfig.getAppId(), dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            // Map jsonObject=new HashMap(2);
            // jsonObject.put("default","default");
            // return R.ok().put("data",jsonObject);
            return GodaddyDnsApiService.getDomainLine(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.getLine(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), 0);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.getLine(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.getLine(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(), 0);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // XDPDNS
            return DnsXdpDnsApiService.getLine(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    0);
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("not found！");
    }

    @Override
    public R getDnsLineV2(TbDnsConfigEntity dnsConfig, Integer parentId) {
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.getLineV2(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.getLineV2(dnsConfig.getAppId(), dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            // Map jsonObject=new HashMap(2);
            // jsonObject.put("default","default");
            // return R.ok().put("data",jsonObject);
            return GodaddyDnsApiService.getDomainLineV2(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.getLineV2(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), parentId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.getLineV2(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.getLineV2(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    parentId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // xdp dns
            return DnsXdpDnsApiService.getLineV2(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    parentId);
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("not found！");
    }

    @Override
    public R getDnsLineByConfigId(Integer dnsConfigId, Integer parentId) {

        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            return this.getDnsLine(dnsConfig, parentId);
        }
        return R.error("dnsConfigId error ");
    }

    @Override
    public R getRecordList(TbDnsConfigEntity dnsConfig) {
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.GetRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            return GodaddyDnsApiService.RecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.getRecordList(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(),
                    dnsConfig.getAppId(), dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // xdp dns
            return DnsXdpDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("unknown type");
    }

    @Override
    public R getRecordListToMapList(TbDnsConfigEntity dnsConfig) {
        List<DnsCommonRecordVo> list = new ArrayList<>();
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }

        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            R r = DnspodDnsApiService.GetRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
            if (null == r || 1 != r.getCode()) {
                return r;
            }
            RecordListItem[] rl = (RecordListItem[]) r.get("data");
            if (null != rl) {
                for (RecordListItem item : rl) {
                    DnsCommonRecordVo commonRecordVo = new DnsCommonRecordVo();
                    commonRecordVo.setRecordId(item.getRecordId().toString());
                    commonRecordVo.setTop(item.getName());
                    commonRecordVo.setValue(item.getValue());
                    commonRecordVo.setLine(item.getLine());
                    commonRecordVo.setTtl(item.getTTL().toString());
                    commonRecordVo.setType(item.getType());
                    commonRecordVo.setRecordType(item.getType());
                    list.add(commonRecordVo);
                }
            }
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            // return
            // AliyunDnsApiService.getRecordList(dnsConfig.getAppDomain(),dnsConfig.getAppId(),dnsConfig.getAppKey());
            R r = AliyunDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
            if (null == r || 1 != r.getCode()) {
                return r;
            }
            List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> aliList = (List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord>) r
                    .get("data");
            for (DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord item : aliList) {
                // JSONObject m=
                // DataTypeConversionUtil.entity2jsonObj(com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord.class);
                /*
                 * JSONObject obj=new JSONObject();
                 * obj.put("recordId",item.getRecordId());
                 * obj.put("top",item.getRR());
                 * obj.put("value",item.getValue());
                 * obj.put("line",item.getLine());
                 * obj.put("ttl",item.getTTL());
                 * obj.put("type",item.getType());
                 * obj.put("recordType",item.getType());
                 */
                DnsCommonRecordVo commonRecordVo = new DnsCommonRecordVo();
                commonRecordVo.setRecordId(item.getRecordId());
                commonRecordVo.setTop(item.getRR());
                commonRecordVo.setValue(item.getValue());
                commonRecordVo.setLine(item.getLine());
                commonRecordVo.setTtl(item.getTTL().toString());
                commonRecordVo.setType(item.getType());
                commonRecordVo.setRecordType(item.getType());
                list.add(commonRecordVo);
            }
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            R r = GodaddyDnsApiService.RecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
            if (null == r || 1 != r.getCode()) {
                return r;
            }
            JSONArray jsonArray = (JSONArray) r.get("data");
            ;
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                GodaddyRecordVo item = DataTypeConversionUtil.json2entity(object, GodaddyRecordVo.class);
                DnsCommonRecordVo commonRecordVo = new DnsCommonRecordVo();
                commonRecordVo.setRecordId(item.getCalRecordId());
                commonRecordVo.setTop(item.getName());
                commonRecordVo.setValue(item.getData());
                commonRecordVo.setLine("default");
                commonRecordVo.setTtl(item.getTtl().toString());
                commonRecordVo.setType(item.getType());
                commonRecordVo.setRecordType(item.getType());
                list.add(commonRecordVo);
            }
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())
                || dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())
                || dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // antsdns
            R r = R.error();
            if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
                r = AntsDnsApiService.getRecordList(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(),
                        dnsConfig.getAppId(), dnsConfig.getAppKey());
            } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
                r = Dns99DnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey());
            } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
                r = DnsXdpDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey());
            }
            if (null == r || 1 != r.getCode()) {
                return r;
            }
            List<String> antsDnsList = (List<String>) r.get("data");
            if (null == antsDnsList) {
                return R.error("empty data");
            }
            for (String str : antsDnsList) {
                DnsCommonRecordVo commonRecordVo = new DnsCommonRecordVo();
                AntsDnsRecordVo antsDnsRecordVo = DataTypeConversionUtil.string2Entity(str, AntsDnsRecordVo.class);

                commonRecordVo.setRecordId(antsDnsRecordVo.getRecord_id());
                commonRecordVo.setTop(antsDnsRecordVo.getTop());
                commonRecordVo.setValue(antsDnsRecordVo.getValue());
                commonRecordVo.setTtl(antsDnsRecordVo.getTtl());
                commonRecordVo.setType(antsDnsRecordVo.getRecord_type());
                commonRecordVo.setRecordType(antsDnsRecordVo.getRecord_type());
                commonRecordVo.setLine(antsDnsRecordVo.getRecord_line_name());
                list.add(commonRecordVo);
            }
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            R r = CloudflareDnsApiService.getRecordList(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey());
            if (1 != r.getCode()) {
                return r;
            }
            if (r.containsKey("data")) {
                JSONArray array = (JSONArray) r.get("data");
                if (null == array) {
                    return R.error(r.getMsg());
                }
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    CfDnsRecordVo vo = DataTypeConversionUtil.json2entity(object, CfDnsRecordVo.class);
                    if (null != vo) {
                        DnsCommonRecordVo commonRecordVo = new DnsCommonRecordVo();
                        commonRecordVo.setRecordId(vo.getId());
                        commonRecordVo.setRecordType(vo.getType());
                        commonRecordVo.setValue(vo.getContent());
                        commonRecordVo.setTtl(String.valueOf(vo.getTtl()));
                        commonRecordVo.setLine("default");
                        commonRecordVo.setType(vo.getType());
                        String top = "";
                        if (null != vo.getZone_name()) {
                            top = vo.getName().replace(vo.getZone_name(), "");
                            if (top.endsWith(".")) {
                                top = top.substring(0, top.length() - 1);
                            }
                        } else {
                            top = vo.getName().replace(dnsConfig.getAppDomain(), "");
                            if (top.endsWith(".")) {
                                top = top.substring(0, top.length() - 1);
                            }
                        }
                        commonRecordVo.setTop(top);
                        commonRecordVo.setRecordType(vo.getType());
                        list.add(commonRecordVo);
                    }
                }
            }
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.ok().put("data", list);
    }

    @Override
    public R removeRecordByRecordId(TbDnsConfigEntity dnsConfig, String recordId) {
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.removeRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.removeRecord(dnsConfig.getAppId(), dnsConfig.getAppKey(), recordId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            return GodaddyDnsApiService.removeRecordByRecordId(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.removeRecord(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.removeRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.removeRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // xdpdns
            return DnsXdpDnsApiService.removeRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId);
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("unknown type");
    }

    @Override
    public R removeRecordByRecordIdAndDnsId(Integer dnsId, String recordId) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsId);
        if (null != dnsConfig) {
            return this.removeRecordByRecordId(dnsConfig, recordId);
        }
        return R.error("dnsId is error");
    }

    @Override
    public R removeRecordByInfoWithMainDomain(Integer dnsConfigId, String mainDomain, String top, String recordType,
            String line, String value, String ttl) {

        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null == dnsConfig) {
            return R.error("dnsId is error");
        }
        dnsConfig.setAppDomain(mainDomain.trim());
        logger.info(String.format("confId[%d],dm[%s],top[%s],type[%s],line[%s],value[%s],ttl[%s]", dnsConfigId,
                dnsConfig.getAppDomain(), top, recordType, line, value, ttl));
        return this.removeRecordByInfoByDnsConf(dnsConfig, top, recordType, line, value, ttl);
    }

    private R removeRecordByInfoByDnsConf(TbDnsConfigEntity dnsConfig, String top, String recordType, String line,
            String value, String ttl) {
        if (null == dnsConfig) {
            return R.error("dnsConfig is null");
        }
        R r = this.getRecordList(dnsConfig);
        if (null == r) {
            return R.error("获取记录失败");
        }
        if (1 != r.getCode()) {
            return r;
        }
        List<String> resultList = new ArrayList<String>();
        // dnspod
        // public static final String[]
        // dnsApiTypes={"dnspod","aliyun","godaddy","antsdns"};
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            RecordListItem[] dnsPodList = (RecordListItem[]) r.get("data");
            for (RecordListItem obj : dnsPodList) {
                if (StringUtils.isNotBlank(top) && !obj.getName().equals(top)) {
                    continue;
                }
                if (StringUtils.isNotBlank(value) && !obj.getValue().equals(value)) {
                    continue;
                }
                if (StringUtils.isNotBlank(recordType) && !obj.getType().equals(recordType)) {
                    continue;
                }
                if (StringUtils.isNotBlank(line) && !obj.getLine().equals(line)) {
                    continue;
                }
                String recordId = obj.getRecordId().toString();
                R r2 = this.removeRecordByRecordId(dnsConfig, recordId);
                resultList.add(r2.toJsonString());
            }
            return R.ok().put("data", resultList);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            // AliyunDnsApiService.removeRecordByInfo(dnsConfig.getAppDomain(),dnsConfig.getAppId(),dnsConfig.getAppKey(),top,recordType);
            List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> aliList = (List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord>) r
                    .get("data");
            for (DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord item : aliList) {
                if (StringUtils.isNotBlank(top) && !item.getRR().equals(top)) {
                    continue;
                }
                if (StringUtils.isNotBlank(value) && !item.getValue().equals(value)) {
                    continue;
                }
                if (StringUtils.isNotBlank(recordType) && !item.getType().equals(recordType)) {
                    continue;
                }
                if (StringUtils.isNotBlank(line) && !item.getLine().equals(line)) {
                    continue;
                }
                String recordId = item.getRecordId();
                R r2 = this.removeRecordByRecordId(dnsConfig, recordId);
                resultList.add(r2.toJsonString());
            }
            return R.ok().put("data", resultList);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            GodaddyDnsApiService.removeRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordType, top);
            return R.ok();
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())
                || dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())
                || dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // ants dns || 99dns || XDP DNS
            List<String> antsDnsList = (List<String>) r.get("data");
            for (String str : antsDnsList) {
                JSONObject dnsJsonObject = DataTypeConversionUtil.string2Json(str);
                if (null == dnsJsonObject) {
                    continue;
                }
                if (!dnsJsonObject.containsKey("top") || !dnsJsonObject.containsKey("value")
                        || !dnsJsonObject.containsKey("record_type")
                        || !dnsJsonObject.containsKey("record_line_name")) {
                    continue;
                }
                if (StringUtils.isNotBlank(top) && !dnsJsonObject.getString("top").equals(top)) {
                    continue;
                }
                if (StringUtils.isNotBlank(value) && !dnsJsonObject.getString("value").equals(value)) {
                    continue;
                }
                if (StringUtils.isNotBlank(recordType) && !dnsJsonObject.getString("record_type").equals(recordType)) {
                    continue;
                }
                if (StringUtils.isNotBlank(line) && !dnsJsonObject.getString("record_line_name").equals(line)) {
                    continue;
                }
                if (dnsJsonObject.containsKey("record_id")) {
                    String recordId = dnsJsonObject.getString("record_id");
                    R r2 = this.removeRecordByRecordId(dnsConfig, recordId);
                    resultList.add(r2.toJsonString());
                }
            }
            return R.ok().put("data", resultList);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            JSONArray array = (JSONArray) r.get("data");
            if (null == array) {
                return R.error("empty data");
            }
            for (int i = 0; i < array.size(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (null == object) {
                    continue;
                }
                CfDnsRecordVo vo = DataTypeConversionUtil.json2entity(object, CfDnsRecordVo.class);
                if (null == vo || null == vo.getType()) {
                    continue;
                }
                if (StringUtils.isNotBlank(recordType)) {
                    if (!recordType.equals(vo.getType())) {
                        continue;
                    }
                }
                if (StringUtils.isNotBlank(value)) {
                    if (!value.equals(vo.getContent())) {
                        continue;
                    }
                }
                if (StringUtils.isNotBlank(top)) {
                    String c_top = "";
                    if (null != vo.getZone_name()) {
                        c_top = vo.getName().replace(vo.getZone_name(), "");
                        if (c_top.endsWith(".")) {
                            c_top = c_top.substring(0, c_top.length() - 1);
                        }
                    } else {
                        c_top = vo.getName().replace(dnsConfig.getAppDomain(), "");
                        if (c_top.endsWith(".")) {
                            c_top = top.substring(0, top.length() - 1);
                        }
                    }
                    if (!c_top.equals(top)) {
                        continue;
                    }
                }
                R r2 = this.removeRecordByRecordId(dnsConfig, vo.getId());
                resultList.add(r2.toJsonString());
            }
            return R.ok().put("data", resultList);
        }
        return R.error("unknown source");
    }

    @Override
    public R removeRecordByInfo(Integer dnsConfigId, String top, String recordType, String line, String value,
            String ttl) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            return removeRecordByInfoByDnsConf(dnsConfig, top, recordType, line, value, ttl);
        }
        return R.error("unknown type");
    }

    @Override
    public R addRecordByConfId(Integer dnsConfigId, String top, String recordType, String line, String value,
            String ttl) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            return this.addRecord(dnsConfig, top, recordType, line, value, ttl);
        }
        return R.error("dnsConfigId error");
    }

    @Override
    public R addRecordByConfIdWithMainDomain(Integer dnsConfigId, String mainDomain, String top, String recordType,
            String line, String value, String ttl) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            return this.addRecordWithMainDomain(dnsConfig, mainDomain, top, recordType, line, value, ttl);
        }
        return R.error("dnsConfigId error");
    }

    @Override
    public R addRecord(TbDnsConfigEntity dnsConfig, String top, String recordType, String line, String value,
            String ttl) {
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.addRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.addRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            return GodaddyDnsApiService.addRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    top, recordType, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.addRecord(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.addRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), top, recordType, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.addRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // xdp dns
            return DnsXdpDnsApiService.addRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(), dnsConfig.getAppKey(),
                    top, recordType, line, value, ttl);
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("unknown type");
    }

    @Override
    public R addRecordWithMainDomain(TbDnsConfigEntity dnsConfig, String mainDomain, String top, String recordType,
            String line, String value, String ttl) {
        dnsConfig.setAppDomain(mainDomain);
        return this.addRecord(dnsConfig, top, recordType, line, value, ttl);
    }

    @Override
    public R modifyRecord(TbDnsConfigEntity dnsConfig, String recordId, String top, String recordType, String line,
            String value, String ttl) {
        // if(StringUtils.isNotBlank(dnsConfig.getAppId()) &&
        // StringUtils.isNotBlank(dnsConfig.getAppKey()) &&
        // StringUtils.isNotBlank(dnsConfig.getSource()) ){ }
        if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
            // dnspod
            return DnspodDnsApiService.modifyRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId, top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
            // aliyun
            return AliyunDnsApiService.modifyRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId, top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
            // godaddy
            return R.error("godaddy 修改失败");
        } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
            // antsdns
            return AntsDnsApiService.modifyRecord(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId, top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
            // 99dns
            return Dns99DnsApiService.modifyRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId, top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
            // xdp dns
            return DnsXdpDnsApiService.modifyRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId, top, recordType, line, value, ttl);
        } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
            // cf
            return CloudflareDnsApiService.modifyRecord(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                    dnsConfig.getAppKey(), recordId, top, recordType, value, ttl);
        } else {
            logger.error("[" + dnsConfig.getSource() + "] unknown type ");
        }

        return R.error("未知类型");
    }

    @Override
    public R modifyRecordByConfigId(Integer dnsConfigId, String recordId, String top, String recordType, String line,
            String value, String ttl) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            return this.modifyRecord(dnsConfig, recordId, top, recordType, line, value, ttl);
        }
        return R.error("dnsConfigId error");
    }

    @Override
    public R getRecordByInfo(Integer dnsConfigId, String top, String type, String line) {
        TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
        if (null != dnsConfig) {
            if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
                // dnspod
                return DnspodDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey(), top, type, line);
            } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
                // aliyun
                return AliyunDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey(), top, type, line);
            } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
                // godaddy
                return GodaddyDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey(), top, type);
            } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())) {
                // antsdns
                return AntsDnsApiService.GetRecordByInfo(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(),
                        dnsConfig.getAppId(), dnsConfig.getAppKey(), top, type, line);
            } else if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
                // 99dns
                return Dns99DnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey(), top, type, line);
            } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
                // xdp dns
                return DnsXdpDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey(), top, type, line);
            } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
                // CF
                return CloudflareDnsApiService.getRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                        dnsConfig.getAppKey(), top, type);
            } else {
                logger.error("[" + dnsConfig.getSource() + "] unknown type ");
            }
        }
        return R.error("unknown type");
    }

    @Override
    public DnsRecordItemVo getRecordByValueInfo(Integer dnsConfigId, String top, String type, String line,
            String value) {
        List<DnsRecordItemVo> r_list = this.getRecordByInfoToList(dnsConfigId, top, type, line);
        if (null == r_list) {
            return null;
        }
        for (DnsRecordItemVo obj : r_list) {
            if (obj.getValue().equals(value)) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public List<DnsRecordItemVo> getRecordByInfoToList(Integer dnsConfigId, String top, String type, String line) {
        try {
            TbDnsConfigEntity dnsConfig = tbDnsConfigDao.selectById(dnsConfigId);
            if (null != dnsConfig) {
                if (dnsConfig.getSource().equals(DnsApiEnum.DNSPOD.getName())) {
                    // dnspod
                    R r = DnspodDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                            dnsConfig.getAppKey(), top, type, line);
                    if (null != r && 1 == r.getCode()) {
                        RecordListItem[] res = (RecordListItem[]) r.get("data");
                        if (res.length > 0) {
                            List<DnsRecordItemVo> list = new ArrayList<>();
                            for (RecordListItem item : res) {
                                DnsRecordItemVo obj = new DnsRecordItemVo();
                                obj.setRecordId(item.getRecordId().toString());
                                obj.setTop(item.getName());
                                obj.setValue(item.getValue());
                                obj.setLine(item.getLine());
                                obj.setTtl(item.getTTL().toString());
                                obj.setType(item.getType());
                                obj.setRecordType(item.getType());
                                list.add(obj);
                            }
                            return list;
                        }
                    }
                } else if (dnsConfig.getSource().equals(DnsApiEnum.ALIYUN.getName())) {
                    // aliyun
                    R r = AliyunDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                            dnsConfig.getAppKey(), top, type, line);
                    if (null != r && 1 == r.getCode()) {
                        List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> ali_list = (List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord>) r
                                .get("data");
                        if (ali_list.size() > 0) {
                            List<DnsRecordItemVo> list = new ArrayList<>();
                            for (DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord item : ali_list) {
                                DnsRecordItemVo obj = new DnsRecordItemVo();
                                obj.setRecordId(item.getRecordId());
                                obj.setTop(item.getRR());
                                obj.setValue(item.getValue());
                                obj.setLine(item.getLine());
                                obj.setTtl(item.getTTL().toString());
                                obj.setType(item.getType());
                                obj.setRecordType(item.getType());
                                list.add(obj);
                            }
                            return list;
                        }
                    }
                } else if (dnsConfig.getSource().equals(DnsApiEnum.GODADDY.getName())) {
                    // godaddy
                    R r = GodaddyDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                            dnsConfig.getAppKey(), top, type);
                    if (null != r && 1 == r.getCode()) {
                        JSONArray jsonArray = (JSONArray) r.get("data");
                        List<DnsRecordItemVo> list = new ArrayList<>();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            DnsRecordItemVo itemObj = new DnsRecordItemVo();
                            JSONObject obj = jsonArray.getJSONObject(i);
                            itemObj.setTtl("600");
                            itemObj.setLine("-");
                            if (obj.containsKey("data")) {
                                itemObj.setValue(obj.getString("data"));
                            }
                            if (obj.containsKey("name")) {
                                itemObj.setTop(obj.getString("name"));
                            }
                            if (obj.containsKey("type")) {
                                itemObj.setType(obj.getString("type"));
                                itemObj.setRecordType(obj.getString("type"));
                            }
                            list.add(itemObj);
                        }
                        return list;
                    }
                } else if (dnsConfig.getSource().equals(DnsApiEnum.ANTSDNS.getName())
                        || dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())
                        || dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
                    // antsdns || 99dns
                    R r = R.error();
                    if (dnsConfig.getSource().equals(DnsApiEnum.DNS99DNS.getName())) {
                        r = Dns99DnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                                dnsConfig.getAppKey(), top, type, line);
                    } else if (dnsConfig.getSource().equals(DnsApiEnum.DNSXDP.getName())) {
                        r = DnsXdpDnsApiService.GetRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                                dnsConfig.getAppKey(), top, type, line);
                    } else {
                        r = AntsDnsApiService.GetRecordByInfo(dnsConfig.getAppUrl(), dnsConfig.getAppDomain(),
                                dnsConfig.getAppId(), dnsConfig.getAppKey(), top, type, line);
                    }
                    if (1 != r.getCode()) {
                        return new ArrayList<DnsRecordItemVo>(1);
                    }
                    if (!r.containsKey("data") || null == r.get("data")) {
                        return new ArrayList<DnsRecordItemVo>(1);
                    }
                    // System.out.println(r.get("data").getClass().getName());
                    if (!"java.util.ArrayList".equals(r.get("data").getClass().getName())) {
                        return new ArrayList<DnsRecordItemVo>(1);
                    }
                    List<JSONObject> retObjLs = (List<JSONObject>) r.get("data");
                    if (null != retObjLs) {
                        List<DnsRecordItemVo> list = new ArrayList<>(retObjLs.size());
                        for (JSONObject obj : retObjLs) {
                            DnsRecordItemVo itemVo = new DnsRecordItemVo();
                            AntsDnsRecordItemVo antsRItem = DataTypeConversionUtil.json2entity(obj,
                                    AntsDnsRecordItemVo.class);
                            if (null == antsRItem) {
                                continue;
                            }
                            itemVo.setRecordType(antsRItem.getRecord_type());
                            itemVo.setType(itemVo.getRecordType());
                            itemVo.setLine(antsRItem.getRecord_line_name());
                            itemVo.setRecordId(antsRItem.getRecord_id());
                            itemVo.setTop(antsRItem.getTop());
                            itemVo.setValue(antsRItem.getValue());
                            itemVo.setTtl(String.valueOf(antsRItem.getTtl()));
                            list.add(itemVo);
                        }
                        return list;
                    }
                } else if (dnsConfig.getSource().equals(DnsApiEnum.CLOUDFLARE.getName())) {
                    // cf
                    R r = CloudflareDnsApiService.getRecordByInfo(dnsConfig.getAppDomain(), dnsConfig.getAppId(),
                            dnsConfig.getAppKey(), top, type);
                    if (1 != r.getCode()) {
                        return new ArrayList<DnsRecordItemVo>(1);
                    }
                    if (r.containsKey("data")) {
                        List<JSONObject> jArray = (List<JSONObject>) r.get("data");
                        List<DnsRecordItemVo> list = new ArrayList<>();
                        for (JSONObject obj : jArray) {
                            CfDnsRecordVo vo = DataTypeConversionUtil.json2entity(obj, CfDnsRecordVo.class);
                            if (null != vo) {
                                DnsRecordItemVo rVo = new DnsRecordItemVo();
                                rVo.setRecordId(vo.getId());
                                String ctop = "";
                                if (null != vo.getZone_name()) {
                                    ctop = vo.getName().replace(vo.getZone_name(), "");
                                    if (ctop.endsWith(".")) {
                                        ctop = top.substring(0, ctop.length() - 1);
                                    }
                                } else {
                                    ctop = vo.getName().replace(dnsConfig.getAppDomain(), "");
                                    if (ctop.endsWith(".")) {
                                        ctop = top.substring(0, ctop.length() - 1);
                                    }
                                }
                                rVo.setTop(ctop);
                                rVo.setValue(vo.getContent());
                                rVo.setType(vo.getType());
                                rVo.setRecordType(vo.getType());
                                rVo.setTtl(String.valueOf(vo.getTtl()));
                                list.add(rVo);
                            }
                        }
                        return list;
                    }

                } else {
                    logger.error("[" + dnsConfig.getSource() + "] unknown type ");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<DnsRecordItemVo>(1);
    }

}
