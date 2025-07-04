/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.modules.sys.entity.SysConfigEntity;

import java.io.File;
import java.io.Reader;
import java.util.Map;

/**
 * 系统配置信息
 *
 * @author Mark sunlightcs@gmail.com
 */
public interface SysConfigService extends IService<SysConfigEntity> {

	PageUtils queryPage(Map<String, Object> params);
	
	/**
	 * 保存配置信息
	 */
	public void saveConfig(SysConfigEntity config);
	
	/**
	 * 更新配置信息
	 */
	public void updateConfAttrs(SysConfigEntity config);
	
	/**
	 * 根据key，更新value
	 */
	public void updateValueByKey(String key, String value);
	
	/**
	 * 删除配置信息
	 */
	public void deleteBatch(Long[] ids);
	
	/**
	 * 根据key，获取配置的value值
	 * 
	 * @param key           key
	 */
	public String getValue(String key);
	
	/**
	 * 根据key，获取value的Object对象
	 * @param key    key
	 * @param clazz  Object对象
	 */
	public <T> T getConfigObject(String key, Class<T> clazz);


	public SysConfigEntity getConfigByKey(String key);

	/**
	 * DB导出
	 * @return
	 */
	File dumpMysqlDbFile();

	/**
	 * DB导入
	 * @param reader
	 * @return
	 */
	boolean importDbFile(Reader reader);


}
