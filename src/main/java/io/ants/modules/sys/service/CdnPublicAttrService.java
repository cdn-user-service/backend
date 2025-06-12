package io.ants.modules.sys.service;

import io.ants.common.utils.R;

import java.util.Map;

public interface CdnPublicAttrService {

    R getPubKeyDetail(Map map);

    R statusChange(Map<String, String> params);

    R pubAttrSave(Map<String, Object> map);

    R changeWeight(Map<String, String> params);

    R deletePubAttr(Map<String, String> map);

    R pubAttrList(Map<String, String> map);
}
