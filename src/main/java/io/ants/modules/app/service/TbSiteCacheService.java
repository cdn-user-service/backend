package io.ants.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbSiteCachePrefetchEntity;
import io.ants.modules.app.form.DeleteIdsForm;
import io.ants.modules.app.form.QuerySiteCachePrefetchPageForm;

public interface TbSiteCacheService extends IService<TbSiteCachePrefetchEntity> {

    R  getCachePrePageList(long userId, QuerySiteCachePrefetchPageForm form);


    R  saveCachePre(long userId,TbSiteCachePrefetchEntity entity);


     R delCachePre(long userId, DeleteIdsForm idsForm);
}
