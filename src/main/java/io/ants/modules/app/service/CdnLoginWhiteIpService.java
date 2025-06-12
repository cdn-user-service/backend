package io.ants.modules.app.service;

import io.ants.common.utils.R;

import io.ants.modules.app.entity.CdnLoginWhiteIpEntity;
import io.ants.modules.app.form.QueryLoginWhiteIpForm;

public interface CdnLoginWhiteIpService {

    R loginWhiteIPList(int type, long uid, QueryLoginWhiteIpForm form);

    R saveLoginWhiteIp(int type, long uid, CdnLoginWhiteIpEntity entity);

    R deleteLoginWhiteIps(int type, long uid, String ids);

    R checkCanLoginInCurrentIp(int type, long uid, String ip);
}
