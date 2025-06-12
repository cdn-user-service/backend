package io.ants.modules.sys.service;

import io.ants.common.utils.R;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.form.CertCallbackForm;
import io.ants.modules.sys.vo.BytesDateVo;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface CommonTaskService {

    void groupDnsRecordDispatch();

    void checkNodeNormalHandle();

    void requestBytesRecordHandle();

    void prePaidTask();

    void applySslTaskHandle();

    void viewLocalCertFileUpdateSslCrt(int siteId);

    void checkSiteCname();

    BytesDateVo getBytesDataListBySuitSerialNumber(Long userId, String SerialNumber, Date startTime, Date endTime);

    CdnSuitEntity updateThisSuitDetailInfo(CdnSuitEntity suit, boolean usedInfo);

    void updateUsedFlow(CdnSuitEntity suitEntity );

    CdnSuitEntity commGetSuitDetail(Long userId, String serialNumber, List<Integer> typeList,boolean usedInfo);

    CdnSuitEntity stopUsePostpaidSuit(Long userId,String serialNumber);

    R saveCertCallback(CertCallbackForm form);

    R saveSiteAttr(Map<String, Object> params);
}
