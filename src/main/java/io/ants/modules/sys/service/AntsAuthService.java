package io.ants.modules.sys.service;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.form.CdnClientForm;
import io.ants.modules.sys.form.CdnClientQueryForm;
import io.ants.modules.sys.form.ChangeMasterForm;

import java.util.List;
import java.util.Map;

public interface AntsAuthService {

    String getAuthInfo();

    String regNode(String NodeIp);

    String deleteNode(String NodeIp);

    R syncFlushAuthList();

    CdnClientEntity SaveByMainControl(CdnClientForm form);

    PageUtils nodePageList(CdnClientQueryForm param);

    Object feedbackInfo(Integer clientId);

    void deleteAllFeedback(Integer clientId);

    Object operaFeeds(Integer index);

    Object operaDetail(Integer index);

    Map<String, Object> getIndexChartData(String keys);


    R nodeAttrSave(Map param);

    R changeMaster(ChangeMasterForm form);
}
