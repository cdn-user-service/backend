package io.ants.modules.sys.service;

import io.ants.modules.app.entity.TbSiteMutAttrEntity;
import io.ants.modules.app.entity.TbStreamProxyEntity;

public interface InputAvailableService {

    boolean checkListenIsAvailable(Integer port, Integer areaId, String mode, TbStreamProxyEntity proxy, TbSiteMutAttrEntity mutAttr);

    boolean checkNginxServerNameAndAliasIsValid( String addMode,int id,String name);

}
