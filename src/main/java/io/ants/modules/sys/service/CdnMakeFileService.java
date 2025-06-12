package io.ants.modules.sys.service;

import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.sys.enums.DelConfMode;
import io.ants.modules.sys.form.BatReissuedForm;
import io.ants.modules.sys.form.CertCallbackForm;
import io.ants.modules.sys.vo.ElkServerVo;
import io.ants.modules.sys.vo.PurgeVo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface CdnMakeFileService {

    LinkedHashMap preciseWafParam();
    List<String> getAllCreateFileInvokeMethod();
    void checkSitesInNode(String siteIds);
    boolean getClientNginxVersion();


    void startOperaTask();
    void pushPurgeCache(List<PurgeVo> purgeVoList);

    void applyCertThread();

    Object pushByInputInfo(Map<String,String> map);
    void operaLinkTaskV2();


    boolean cdnPubCheckKeyValueRule(String key, Object value);

     String getNodeAreaGroupIdBySerialNumber(String  serialNumber);

    void setElkConfig2Redis(ElkServerVo elkServerVo);

    R pushApplyCertificateBySiteId(Integer siteId, int reIssuedMode, int useMode, int resId,int dnsConfId);


    R deleteSite(Integer id, DelConfMode opMode);

    String getCacheValueByKey(String key);

    R saveCert2Db(CertCallbackForm form);

    R batSIteCertReissued(Long userId, BatReissuedForm form);

    R getDomainAndAlias(TbSiteEntity tbSite, boolean needVerify);

    void deleteCacheByKey(String key);
}
