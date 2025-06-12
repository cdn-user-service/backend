package io.ants.modules.sys.service;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.entity.TbDnsConfigEntity;
import io.ants.modules.utils.vo.DnsRecordItemVo;

import java.util.List;
import java.util.Map;

public interface DnsCApiService {

    PageUtils list(Map params,int userType,long userId);

    TbDnsConfigEntity save (Map params);

    void delete(String ids,int userType,long userId);

    List<TbDnsConfigEntity> allList();

    R getDnsLine(TbDnsConfigEntity dnsConfig,Integer parentId);
    R getDnsLineV2(TbDnsConfigEntity dnsConfig, Integer parentId);


    R getDnsLineByConfigId( Integer dnsConfigId,Integer parentId);

    R getRecordList(TbDnsConfigEntity dnsConfig);

    R getRecordListToMapList(TbDnsConfigEntity dnsConfig);

    R removeRecordByRecordId(TbDnsConfigEntity dnsConfig ,String recordId);

    R removeRecordByRecordIdAndDnsId(Integer dnsId ,String recordId);

    R removeRecordByInfo(Integer dnsConfigId,String top,String recordType,String line,String value,String ttl);
    R removeRecordByInfoWithMainDomain(Integer dnsConfigId,String mainDomain,String top,String recordType,String line,String value,String ttl);

    R addRecordByConfId(Integer dnsConfigId ,String top,String recordType,String line,String value,String ttl);

    R addRecord(TbDnsConfigEntity dnsConfig ,String top,String recordType,String line,String value,String ttl);

    R addRecordByConfIdWithMainDomain(Integer dnsConfigId ,String mainDomain,String top,String recordType,String line,String value,String ttl);

    R addRecordWithMainDomain(TbDnsConfigEntity dnsConfig ,String mainDomain,String top,String recordType,String line,String value,String ttl);

    R modifyRecord(TbDnsConfigEntity dnsConfig, String recordId , String top, String recordType, String line, String value, String ttl);

    R modifyRecordByConfigId(Integer dnsConfigId,String recordId ,String top,String recordType,String line,String value,String ttl);

    List<DnsRecordItemVo> getRecordByInfoToList(Integer dnsConfigId, String top, String type, String line);

    R getRecordByInfo(Integer dnsConfigId,String top,String type,String line);

    DnsRecordItemVo getRecordByValueInfo(Integer dnsConfigId,String top,String type,String line,String value);

    R allList(Integer id, Long userId);

    R verifyMainDomainInDns(Integer dnsConfigId,String mainDomain);
}
