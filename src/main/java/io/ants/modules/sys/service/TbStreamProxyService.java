package io.ants.modules.sys.service;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbStreamProxyEntity;
import io.ants.modules.app.form.QueryStreamListForm;

public interface TbStreamProxyService {

    PageUtils streamList(QueryStreamListForm params);

    TbStreamProxyEntity saveProxy(TbStreamProxyEntity streamProxy);

    void batchDelete(Long userId,String ids);

    Integer changeProxyStatus(Long userId, Integer streamProxyId,Integer status);

    void reInitDefaultParam();

    R getAllPort(Long userId);

    R getDetailById(Long userId,Integer id);
}
