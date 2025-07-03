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
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.Constant;
import io.ants.common.utils.MapUtils;
import io.ants.modules.sys.dao.SysMenuDao;
import io.ants.modules.sys.entity.SysMenuEntity;
import io.ants.modules.sys.service.SysMenuService;
import io.ants.modules.sys.service.SysRoleMenuService;
import io.ants.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("sysMenuService")
public class SysMenuServiceImpl extends ServiceImpl<SysMenuDao, SysMenuEntity> implements SysMenuService {
	@Autowired
 
	private SysUserService sysUserService;
	@Autowired
	private SysRoleMenuService sysRoleMenuService;

	@Override
	public List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList) {
		List<SysMenuEntity> menuList = queryListParentId(parentId);
		if (menuIdList == null) {
			return menuList;
		}

		List<SysMenuEntity> userMenuList = new ArrayList<>();
		for (SysMenuEntity menu : menuList) {
			if (menuIdList.contains(menu.getMenuId())) {
				userMenuList.add(menu);
			}
		}
		return userMenuList;
	}

	@Override
	public List<SysMenuEntity> queryListParentId(Long parentId) {
		return baseMapper.queryListParentId(parentId);
	}

	@Override
	public List<SysMenuEntity> queryNotButtonList() {
		return baseMapper.queryNotButtonList();
	}

	@Override
	public List<SysMenuEntity> getUserMenuList(Long userId) {
		// 系统管理员,拥有最高权限
		if (userId == Constant.SUPER_ADMIN || QuerySysAuth.USER_HAVE_ALL_PERMS) {
			return getMenuList(null);
		}
		// 用户菜单列表
		List<Long> menuIdList = sysUserService.queryAllMenuId(userId);
		return getMenuList(menuIdList);
	}

	/**
	 * 获取拥有的菜单列表
	 * 
	 * @param menuIdList
	 * @return
	 */
	private List<SysMenuEntity> getMenuList_x(List<Long> menuIdList) {
		// 查询拥有的所有菜单
		List<SysMenuEntity> menus = this.baseMapper.selectList(new QueryWrapper<SysMenuEntity>()
				.in(Objects.nonNull(menuIdList), "menu_id", menuIdList)
				.in("type", 0, 1)
				.orderByDesc("order_num"));
		// 将id和菜单绑定
		HashMap<Long, SysMenuEntity> menuMap = new HashMap<>(12);
		for (SysMenuEntity s : menus) {
			menuMap.put(s.getMenuId(), s);
		}
		// 使用迭代器,组装菜单的层级关系
		Iterator<SysMenuEntity> iterator = menus.iterator();
		while (iterator.hasNext()) {
			SysMenuEntity menu = iterator.next();
			SysMenuEntity parent = menuMap.get(menu.getParentId());
			if (Objects.nonNull(parent)) {
				parent.getList().add(menu);
				// 将这个菜单从当前节点移除
				iterator.remove();
			}
		}

		return menus;
	}

	private SysMenuEntity getMenuInfo(Long menuId, List<Long> menuIdList) {
		SysMenuEntity menu = this.baseMapper.selectById(menuId);
		Integer[] typeDir = { 0, 1 };
		List<SysMenuEntity> menusType1 = this.baseMapper.selectList(new QueryWrapper<SysMenuEntity>()
				.in(Objects.nonNull(menuIdList), "menu_id", menuIdList)
				.eq("parent_id", menuId)
				.in("type", typeDir));
		List<SysMenuEntity> menusType2 = this.baseMapper.selectList(new QueryWrapper<SysMenuEntity>()
				.in(Objects.nonNull(menuIdList), "menu_id", menuIdList)
				.eq("parent_id", menuId)
				.eq("type", 2));
		// if(0==menusType1.size() && 0==menusType2.size()){
		// return menu;
		// }
		menusType1.forEach(item -> {
			SysMenuEntity c = getMenuInfo(item.getMenuId(), menuIdList);
			item.setList(c.getList());
			item.setType2list(c.getType2list());
		});
		if (null != menusType1) {
			menu.setList(menusType1);
		}
		if (null != menusType2) {
			menu.setType2list(menusType2);
		}
		return menu;
	}

	/**
	 * 获取拥有的菜单列表
	 * 
	 * @param menuIdList
	 * @return
	 */
	private List<SysMenuEntity> getMenuList(List<Long> menuIdList) {
		// 查询拥有的所有菜单
		List<SysMenuEntity> menus = this.baseMapper.selectList(new QueryWrapper<SysMenuEntity>()
				.in(Objects.nonNull(menuIdList), "menu_id", menuIdList)
				.eq("parent_id", 0)
				.eq("type", 0)
				.orderByDesc("order_num"));

		for (SysMenuEntity menu : menus) {
			SysMenuEntity c = getMenuInfo(menu.getMenuId(), menuIdList);
			menu.setList(c.getList());
			menu.setType2list(c.getType2list());
		}

		return menus;
	}

	@Override
	public void delete(Long menuId) {
		// 删除菜单
		this.removeById(menuId);
		// 删除菜单与角色关联
		sysRoleMenuService.removeByMap(new MapUtils().put("menu_id", menuId));
	}

	@Override
	public List<Long> queryAllMenuId(Long userId) {
		return sysUserService.queryAllMenuId(userId);
	}

	/**
	 * 获取所有菜单列表
	 */
	private List<SysMenuEntity> getAllMenuList(List<Long> menuIdList) {
		// 查询根菜单列表
		List<SysMenuEntity> menuList = queryListParentId(0L, menuIdList);
		// 递归获取子菜单
		getMenuTreeList(menuList, menuIdList);

		return menuList;
	}

	/**
	 * 递归
	 */
	private List<SysMenuEntity> getMenuTreeList(List<SysMenuEntity> menuList, List<Long> menuIdList) {
		List<SysMenuEntity> subMenuList = new ArrayList<SysMenuEntity>();

		for (SysMenuEntity entity : menuList) {
			// 目录
			if (entity.getType() == Constant.MenuType.CATALOG.getValue()) {
				entity.setList(getMenuTreeList(queryListParentId(entity.getMenuId(), menuIdList), menuIdList));
			}
			subMenuList.add(entity);
		}

		return subMenuList;
	}
}
