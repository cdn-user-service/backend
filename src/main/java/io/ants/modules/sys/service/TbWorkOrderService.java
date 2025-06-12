package io.ants.modules.sys.service;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.entity.TbWorkOrderCategoryEntity;
import io.ants.modules.sys.entity.TbWorkOrderListEntity;
import io.ants.modules.sys.entity.TbWorkOrderMessageEntity;

import java.util.Date;
import java.util.List;

public interface TbWorkOrderService {

    List<TbWorkOrderCategoryEntity> workOrderCategoryList(Integer parentId,Integer status);

    TbWorkOrderCategoryEntity saveWorkOrderCategory(TbWorkOrderCategoryEntity categoryEntity);

    void deleteWorkOrderCategory(Integer id);

    PageUtils WOL_PageList(Long userId, Date startDate, Date endDate, Integer urgentLevel, Integer Status, Integer page, Integer limit);

    TbWorkOrderListEntity save_WOL(TbWorkOrderListEntity listEntity);

    TbWorkOrderListEntity change_wol_status(Long userId,Integer id,Integer status,Integer score);

    TbWorkOrderListEntity WOL_detail(Long userId,Integer id);

    void batDelete_WOL(Long userId,String ids);

    TbWorkOrderMessageEntity sendWorkOrderMessage(TbWorkOrderMessageEntity senderEntity);


    R getWolNewMsg();
}


