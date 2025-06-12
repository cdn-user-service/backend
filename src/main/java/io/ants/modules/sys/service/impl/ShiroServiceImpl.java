/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有,侵权必究！
 */

package io.ants.modules.sys.service.impl;


import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.Constant;
import io.ants.modules.sys.dao.SysMenuDao;
import io.ants.modules.sys.dao.SysUserDao;
import io.ants.modules.sys.dao.SysUserTokenDao;
import io.ants.modules.sys.entity.SysMenuEntity;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.entity.SysUserTokenEntity;
import io.ants.modules.sys.enums.PermsEnum;
import io.ants.modules.sys.service.ShiroService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Administrator
 */
@Service
public class ShiroServiceImpl implements ShiroService {
    @Autowired
    private SysMenuDao sysMenuDao;
    @Autowired
    private SysUserDao sysUserDao;
    @Autowired
    private SysUserTokenDao sysUserTokenDao;

    private   List<String> allPermsList(){
        List<String> permsList;
        List<SysMenuEntity> menuList = sysMenuDao.selectList(null);
        permsList = new ArrayList<>(menuList.size());
        for(SysMenuEntity menu : menuList){
            permsList.add(menu.getPerms());
        }
        permsList.addAll(PermsEnum.getAllPerms());
        return permsList;
    }

    @Override
    public Set<String> getUserPermissions(long userId) {
        List<String> permsList=new ArrayList<>();
        //系统管理员,拥有最高权限
        if(userId == Constant.SUPER_ADMIN   ){
            //            List<SysMenuEntity> menuList = sysMenuDao.selectList(null);
            //            permsList = new ArrayList<>(menuList.size());
            //            for(SysMenuEntity menu : menuList){
            //                permsList.add(menu.getPerms());
            //            }
            //            permsList.addAll(PermsEnum.getAllPerms());
            permsList.addAll(this.allPermsList());
        }else{
            if (QuerySysAuth.USER_HAVE_ALL_PERMS){
                permsList.addAll(this.allPermsList());
            }else {
                permsList = sysUserDao.queryAllPerms(userId);
            }
        }
        //用户权限列表
        Set<String> permsSet = new HashSet<>();
        for(String perms : permsList){
            if(StringUtils.isBlank(perms)){
                continue;
            }
            permsSet.addAll(Arrays.asList(perms.trim().split(",")));
        }
        return permsSet;
    }

    @Override
    public SysUserTokenEntity queryByToken(String token) {
        return sysUserTokenDao.queryByToken(token);
    }

    @Override
    public SysUserEntity queryUser(Long userId) {
        return sysUserDao.selectById(userId);
    }
}
