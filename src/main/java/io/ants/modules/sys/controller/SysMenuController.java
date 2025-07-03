/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.exception.RRException;
import io.ants.common.utils.Constant;
import io.ants.common.utils.R;
import io.ants.modules.sys.dao.SysUserRoleDao;
import io.ants.modules.sys.entity.SysMenuEntity;
import io.ants.modules.sys.entity.SysUserRoleEntity;
import io.ants.modules.sys.enums.PermsEnum;
import io.ants.modules.sys.service.ShiroService;
import io.ants.modules.sys.service.SysMenuService;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统菜单
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/sys/menu")
public class SysMenuController extends AbstractController {
	@Autowired
	private SysMenuService sysMenuService;
	@Autowired
	private ShiroService shiroService;
	@Autowired
	private SysUserRoleDao sysUserRoleDao;

	@GetMapping("allPerms")
	// @Operation(summary = "所有权限")
	public R allPerms() {
		// 如果是超级管理员
		if (isAdmin()) {
			return R.ok().put("data", PermsEnum.getAllPerms());
		}
		return R.error("仅[超级管理员]账户拥有此权限");

	}

	private boolean isAdmin() {
		if (getSysUserId() == Constant.SUPER_ADMIN) {
			return true;
		}
		SysUserRoleEntity ur = sysUserRoleDao
				.selectOne(new QueryWrapper<SysUserRoleEntity>().eq("user_id", getSysUserId()).last("limit 1"));
		if (null != ur && Constant.SUPER_ADMIN == ur.getRoleId()) {
			return true;
		}
		return false;
	}

	/**
	 * 导航菜单
	 */
	@GetMapping("/nav")
	public R nav() {
		List<SysMenuEntity> menuList = sysMenuService.getUserMenuList(getSysUserId());
		Set<String> permissions = shiroService.getUserPermissions(getSysUserId());
		return R.ok().put("menuList", menuList).put("permissions", permissions);
	}

	// /**
	// * 所有菜单列表
	// */
	// @GetMapping("/list")
	// @PreAuthorize("hasAuthority('sys:menu:list')")
	// public List<SysMenuEntity> x_list(){
	// List<SysMenuEntity> menuList = sysMenuService.list();
	// HashMap<Long, SysMenuEntity> menuMap = new HashMap<>(12);
	// for (SysMenuEntity s : menuList) {
	// menuMap.put(s.getMenuId(), s);
	// }
	// for (SysMenuEntity s : menuList) {
	// SysMenuEntity parent = menuMap.get(s.getParentId());
	// if (Objects.nonNull(parent)) {
	// s.setParentName(parent.getName());
	// }
	// List<SysMenuEntity>child_list=menuList.stream().filter(t->t.getParentId()==s.getMenuId()).collect(Collectors.toList());
	// s.setList(child_list);
	// }
	//
	// return menuList;
	// }

	private SysMenuEntity getChildPerms(SysMenuEntity m, List<SysMenuEntity> menuList) {
		List<SysMenuEntity> pid_p_list = menuList.stream().filter(f -> f.getParentId().equals(m.getMenuId()))
				.collect(Collectors.toList());
		pid_p_list.forEach(item -> {
			SysMenuEntity i = getChildPerms(item, menuList);
			item.setList(i.getList());
		});
		m.setList(pid_p_list);
		return m;
	}

	/**
	 * 当有用户拥有的所有菜单列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('sys:menu:list')")
	public R list() {
		// List<SysMenuEntity> menuList = sysMenuService.getUserMenuList(getUserId());
		List<Long> midList = null;

		if (isAdmin()) {
			List<SysMenuEntity> list = sysMenuService.list(new QueryWrapper<SysMenuEntity>().orderByDesc("order_num"));
			midList = list.stream().map(t -> t.getMenuId()).collect(Collectors.toList());
		} else {
			midList = sysMenuService.queryAllMenuId(getSysUserId());
		}
		List<SysMenuEntity> menuList = sysMenuService.list(new QueryWrapper<SysMenuEntity>()
				.in("menu_id", midList.toArray())
				.orderByDesc("order_num")
				.and(q -> q.eq("type", 2).or().like("perms", ":")));

		Map<Long, SysMenuEntity> map = new HashMap<>();
		for (SysMenuEntity m : menuList) {
			map.put(m.getMenuId(), m);
			Long tId = m.getParentId();
			while (true) {
				SysMenuEntity parentM = sysMenuService.getById(tId);
				if (null != parentM) {
					map.put(parentM.getMenuId(), parentM);
					tId = parentM.getParentId();
				} else {
					break;
				}
			}
		}
		List<SysMenuEntity> treeMenuList = new ArrayList<>();
		for (Map.Entry<Long, SysMenuEntity> entry : map.entrySet()) {
			treeMenuList.add(entry.getValue());
		}
		List<SysMenuEntity> menu_root_list = treeMenuList.stream().filter(t -> t.getParentId().equals(0L))
				.collect(Collectors.toList());
		for (SysMenuEntity m : menu_root_list) {
			SysMenuEntity i = this.getChildPerms(m, treeMenuList);
			m.setList(i.getList());
		} // 0<-46<-47<-140<- 141
		return R.ok().put("data", menu_root_list);
	}

	/**
	 * 选择菜单(添加、修改菜单)
	 */
	@GetMapping("/select")
	@PreAuthorize("hasAuthority('sys:menu:select')")
	public R select() {
		// 查询列表数据
		List<SysMenuEntity> menuList = sysMenuService.queryNotButtonList();

		// 添加顶级菜单
		SysMenuEntity root = new SysMenuEntity();
		root.setMenuId(0L);
		root.setName("一级菜单");
		root.setParentId(-1L);
		root.setOpen(true);
		menuList.add(root);

		return R.ok().put("menuList", menuList);
	}

	/**
	 * 菜单信息
	 */
	@GetMapping("/info/{menuId}")
	@PreAuthorize("hasAuthority('sys:menu:info')")
	public R info(@PathVariable("menuId") Long menuId) {
		SysMenuEntity menu = sysMenuService.getById(menuId);
		return R.ok().put("menu", menu);
	}

	/**
	 * 保存
	 */
	@SysLog("保存菜单")
	@PostMapping("/save")
	@PreAuthorize("hasAuthority('sys:menu:save')")
	public R save(@RequestBody SysMenuEntity menu) {
		// 数据校验
		verifyForm(menu);
		sysMenuService.save(menu);

		return R.ok();
	}

	/**
	 * 修改
	 */
	@SysLog("修改菜单")
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('sys:menu:update')")
	public R update(@RequestBody SysMenuEntity menu) {
		// 数据校验
		verifyForm(menu);

		sysMenuService.updateById(menu);

		return R.ok();
	}

	/**
	 * 删除
	 */
	@SysLog("删除菜单")
	@PostMapping("/delete/{menuId}")
	@PreAuthorize("hasAuthority('sys:menu:delete')")
	public R delete(@PathVariable("menuId") long menuId) {
		checkDemoModify();
		// 判断是否有子菜单或按钮
		List<SysMenuEntity> menuList = sysMenuService.queryListParentId(menuId);
		if (menuList.size() > 0) {
			for (SysMenuEntity m : menuList) {
				sysMenuService.delete(m.getMenuId());
			}
		}

		sysMenuService.delete(menuId);

		return R.ok();
	}

	/**
	 * 验证参数是否正确
	 */
	private void verifyForm(SysMenuEntity menu) {
		if (StringUtils.isBlank(menu.getName())) {
			throw new RRException("菜单名称不能为空");
		}

		if (menu.getParentId() == null) {
			throw new RRException("上级菜单不能为空");
		}

		// 菜单
		if (menu.getType() == Constant.MenuType.MENU.getValue()) {
			if (StringUtils.isBlank(menu.getUrl())) {
				throw new RRException("菜单URL不能为空");
			}
		}

		// 上级菜单类型
		int parentType = Constant.MenuType.CATALOG.getValue();
		if (menu.getParentId() != 0) {
			SysMenuEntity parentMenu = sysMenuService.getById(menu.getParentId());
			parentType = parentMenu.getType();
		}

		// 目录、菜单
		// if(menu.getType() == Constant.MenuType.CATALOG.getValue() ||
		// menu.getType() == Constant.MenuType.MENU.getValue()){
		// if(parentType != Constant.MenuType.CATALOG.getValue()){
		// throw new RRException("上级菜单只能为目录类型");
		// }
		// return ;
		// }

		// 按钮
		if (menu.getType() == Constant.MenuType.BUTTON.getValue()) {
			if (parentType != Constant.MenuType.MENU.getValue()) {
				throw new RRException("上级菜单只能为菜单类型");
			}
			return;
		}
	}
}
