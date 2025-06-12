package io.ants.modules.sys.service;


import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;

import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.app.entity.TbSiteGroupEntity;
import io.ants.modules.app.form.*;
import io.ants.modules.app.vo.CreateSingleCreateSiteForm;
import io.ants.modules.sys.form.BatExportForm;
import io.ants.modules.sys.form.BatchAddSiteAttrForm;
import io.ants.modules.sys.form.BuildAiModellingForm;
import io.ants.modules.sys.vo.PurgeVo;

import java.util.List;
import java.util.Map;

public interface TbSiteServer {

    PageUtils querySitePage(QuerySitePageForm params);

    R getAllSiteList(Long userId,Integer igg);

    R changeSiteStatus(Long userId, Integer siteId,Integer status);

    R singleCreateSite(CreateSingleCreateSiteForm form);

    TbSiteEntity createSite(Long userId, String serialNumber, String main_server_name, String alias, String sourceIp, String port, String sProtocol);

    R batCreateSite(Long userId,String serialNumber,String mainServerNames);

    R batchCreateStdSite(Long userId, BatchCreateSite batchCreateSiteParam);

    TbSiteEntity changeSiteSerialNumber(Long userId,Integer siteId,String serialNumber);

    /**
     * 暂无使用
     * @param userId
     * @param siteId
     * @return
     */
    TbSiteEntity querySiteAllDetailInfo(Long userId, Integer siteId);

    TbSiteEntity querySiteAttr(Long userId, Integer siteId, String groups, String key);


    TbSiteEntity getSiteSuitInfo(Long userId, Integer siteId);

    R saveSiteAttr(Map<String, Object> params);

    Integer changeAttrStatus(Long userId, Integer siteId,String pKey,Integer attrId,Integer status);

    R changeAttrWeight(Long userId, Integer siteId,String pKey,Integer attrId,Integer opMode);

    Integer deleteAttr(Long userId,Integer id,String pkey);

    Integer  batDeleteSite(Long userId,String siteIds);



    R queryElk(String method,String path,String param);

    List<PurgeVo> parsePurgeCacheUrl(Long userId, String urls);



    Map getInterceptResult(String nodeIp, String serverName, String sourceIp, String interceptMode, String date, Integer page, Integer limit);

    Integer PullCache(Long userId,String urls,String ips);

    void reInitSiteAttr();

    R querySiteGroupList(Long userId, QuerySiteGroupForm form);

    R saveSiteGroupList(Long userId, TbSiteGroupEntity tbSiteGroup);

    R deleteSiteGroup(Long userId, DeleteIdsForm form);

    R buildSysWafRule(Long userId, SysCreateWafRuleForm form);

    R buildAiWafModelling(BuildAiModellingForm form);

    TbSiteEntity getSiteEntityStatus(Long userId, Integer siteId);

    R batchUpdateSiteAttr(Long userId, BatchModifySiteAttrForm form);


    R getSiteInfo(Long userId, String name);

    R testCreateSite(Long userId,String  mainServerName,String sProtocol,int newUserFlag);

    R batchSearchUpdateSiteAttr(Long userId, BatchSearchModifySiteAttrForm form);

    R checkSiteName(String name);

    R saveSiteSEODns(Integer siteId);

    R batExport(Long userId, BatExportForm form);

    R batchAddSiteAttr(Long userId, BatchAddSiteAttrForm form);
}
