package io.ants.modules.sys.service;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.entity.CdnClientGroupEntity;
import io.ants.modules.sys.vo.EditGroupClientDnsVo;

import java.util.List;

public interface CdnGroupService {

    R clientGroupList(Integer page, Integer limit, String key);

    CdnClientGroupEntity addGroupClient(Integer groupId,Integer parentId,String clientIds,String line,Long ttl);

    List<CdnClientEntity> UnUsedClientIpsByGroup(Integer groupId,Integer parentId);

    PageUtils groupNodeStableInfosList(Integer page, Integer limit, Integer groupId, String key);

    R saveCdnGroup(CdnClientGroupEntity inputGroup);

    R deleteCdnGroup(String ids);

    R modifyClientDnsInfo(EditGroupClientDnsVo vo);

    R getGroupDetail(Integer groupId);
}
