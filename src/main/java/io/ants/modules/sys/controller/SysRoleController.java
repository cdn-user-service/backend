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
import io.ants.common.utils.Constant;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.sys.dao.SysUserRoleDao;
import io.ants.modules.sys.entity.SysMenuEntity;
import io.ants.modules.sys.entity.SysRoleEntity;
import io.ants.modules.sys.entity.SysRoleMenuEntity;
import io.ants.modules.sys.entity.SysUserRoleEntity;
import io.ants.modules.sys.service.SysMenuService;
import io.ants.modules.sys.service.SysRoleMenuService;
import io.ants.modules.sys.service.SysRoleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/sys/role")
public class SysRoleController extends AbstractController {
	@Autowired
	private SysRoleService sysRoleService;
	@Autowired
	private SysRoleMenuService sysRoleMenuService;
	@Autowired
	private SysMenuService sysMenuService;
	@Autowired
	private SysUserRoleDao sysUserRoleDao;


	private boolean isAdmin(){
		if(getSysUserId() == Constant.SUPER_ADMIN ){
			return true;
		}
		SysUserRoleEntity ur=sysUserRoleDao.selectOne(new QueryWrapper<SysUserRoleEntity>().eq("user_id",getSysUserId()).last("limit 1"));
		if (null!=ur && Constant.SUPER_ADMIN==ur.getRoleId()){
			return  true;
		}
		return  false;
	}

	/**
	 * 角色列表
	 */
	@PostMapping("/list")
	@RequiresPermissions("sys:role:list")
	public R list(@RequestBody Map<String, Object> params){
		//如果不是超级管理员，则只查询自己创建的角色列表
		if(isAdmin()){
			params.put("createUserId", getSysUserId());
		}

		PageUtils page = sysRoleService.queryRolePage(params);

		return R.ok().put("page", page);
	}
	
	/**
	 * 角色列表
	 */
	@GetMapping("/select")
	@RequiresPermissions("sys:role:select")
	public R select(){
		Map<String, Object> map = new HashMap<>();
		//如果不是超级管理员，则只查询自己所拥有的角色列表
		if(isAdmin()){
			map.put("create_user_id", getSysUserId());
		}
		List<SysRoleEntity> list = (List<SysRoleEntity>) sysRoleService.listByMap(map);
		if (null==list || list.isEmpty()){
			return R.ok();
		}
		list.forEach(item->{
			List<SysRoleMenuEntity>rmList=sysRoleMenuService.list(new QueryWrapper<SysRoleMenuEntity>()
					.eq("role_id",item.getRoleId())
					.isNotNull("menu_id")
					.select("menu_id")
			);
			if (!rmList.isEmpty()){
				List<Long> midList=rmList.stream().map(t->t.getMenuId()).collect(Collectors.toList());
				item.setMenuIdList(midList);
				List<String>mNameList=new ArrayList<>();
				midList.forEach(menuId->{
					SysMenuEntity sysMenu=sysMenuService.getById(menuId);
					if (null!=sysMenu){
						mNameList.add(sysMenu.getName());
					}
				});
				item.setMenuNameList(mNameList);
			}
		});
		return R.ok().put("list", list);
	}
	
	/**
	 * 角色信息
	 */
	@GetMapping("/info/{roleId}")
	@RequiresPermissions("sys:role:info")
	public R info(@PathVariable("roleId") Long roleId){
		SysRoleEntity role = sysRoleService.getById(roleId);
		
		//查询角色对应的菜单
		List<Long> menuIdList = sysRoleMenuService.queryMenuIdList(roleId);
		role.setMenuIdList(menuIdList);
		
		return R.ok().put("role", role);
	}
	
	/**
	 * 保存角色
	 */
	@SysLog("保存角色")
	@PostMapping("/save")
	@RequiresPermissions("sys:role:save")
	public R save(@RequestBody SysRoleEntity role){
		ValidatorUtils.validateEntity(role);
		
		role.setCreateUserId(getSysUserId());
		sysRoleService.saveRole(role);
		
		return R.ok();
	}
	
	/**
	 * 修改角色
	 */
	@SysLog("修改角色")
	@PostMapping("/update")
	@RequiresPermissions("sys:role:update")
	public R update(@RequestBody SysRoleEntity role){
		ValidatorUtils.validateEntity(role);
		
		role.setCreateUserId(getSysUserId());
		sysRoleService.update(role);
		
		return R.ok();
	}
	
	/**
	 * 删除角色
	 */
	@SysLog("删除角色")
	@PostMapping("/delete")
	@RequiresPermissions("sys:role:delete")
	public R delete(@RequestBody Map params){
		if(!params.containsKey("ids")){
			return R.error("参数缺失");
		}
		String ids=params.get("ids").toString();
		String[]  id_s=ids.split(",");
		Long[] roleIds = new Long[id_s.length];
		for(int i=0;i<id_s.length;i++){
			long ii=Long.parseLong(id_s[i]);
			if (1l!=ii){
				roleIds[i] =ii;
			}

		}
		sysRoleService.deleteBatch(roleIds);
		
		return R.ok();
	}
}
