package io.ants.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.modules.app.entity.TbRewriteEntity;


import java.util.Map;

/**
 * 
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-09-22 10:11:35
 */
public interface TbRewriteService extends IService<TbRewriteEntity> {

    PageUtils queryPage(Map<String, Object> params);

    TbRewriteEntity saveObj(Long userId,TbRewriteEntity rewrite);

    Integer removeByIds(Long userId, String ids);



}

