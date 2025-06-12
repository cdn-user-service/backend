/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有,侵权必究！
 */

package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.modules.sys.dao.SysMenuDao;
import io.ants.modules.sys.dao.SysRoleMenuDao;
import io.ants.modules.sys.entity.SysMenuEntity;
import io.ants.modules.sys.entity.SysRoleMenuEntity;
import io.ants.modules.sys.service.SysRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



/**
 * 角色与菜单对应关系
 *
 * @author Mark sunlightcs@gmail.com
 */
@Service("sysRoleMenuService")
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuDao, SysRoleMenuEntity> implements SysRoleMenuService {

	@Autowired
	private SysMenuDao sysMenuDao;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrUpdate(Long roleId, List<Long> menuIdList) {
		//先删除角色与菜单关系
		deleteBatch(new Long[]{roleId});

		if(menuIdList.size() == 0){
			return ;
		}

		//保存角色与菜单关系
		for(Long menuId : menuIdList){
			SysRoleMenuEntity sysRoleMenuEntity = new SysRoleMenuEntity();
			sysRoleMenuEntity.setMenuId(menuId);
			sysRoleMenuEntity.setRoleId(roleId);
			this.save(sysRoleMenuEntity);
			List<SysMenuEntity> menuEntityList=sysMenuDao.selectList(new QueryWrapper<SysMenuEntity>()
					.eq("parent_id",menuId)
					.eq("type",2)
			);
			for (SysMenuEntity entity:menuEntityList){
				sysRoleMenuEntity = new SysRoleMenuEntity();
				sysRoleMenuEntity.setMenuId(entity.getMenuId());
				sysRoleMenuEntity.setRoleId(roleId);
				this.save(sysRoleMenuEntity);
			}
		}
	}

	@Override
	public List<Long> queryMenuIdList(Long roleId) {
		return baseMapper.queryMenuIdList(roleId);
	}

	@Override
	public int deleteBatch(Long[] roleIds){
		return baseMapper.deleteBatch(roleIds);
	}

}
