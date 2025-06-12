/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有,侵权必究！
 */

package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.datasource.config.DynamicDataSourceConfig;
import io.ants.datasource.properties.DataSourceProperties;
import io.ants.modules.sys.dao.SysConfigDao;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.redis.SysConfigRedis;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.SmsBaoConfig;
import io.ants.modules.utils.config.aliyun.AliyunSmsConfig;
import io.ants.modules.utils.config.tencent.TencentSmsConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<SysConfigDao, SysConfigEntity> implements SysConfigService {

	private Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private SysConfigRedis sysConfigRedis;
	@Autowired
	private DynamicDataSourceConfig dataSourceConfig;


	private void updateDefault(SysConfigEntity configItem){
		if (StringUtils.isBlank(configItem.getParamKey())){return;}
		int cid=ConfigConstantEnum.getConfIdByName(configItem.getParamKey());
		if (!Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).contains(cid)){
			configItem.setStatus(0);
			this.updateById(configItem);
			return;
		}
		if (configItem.getParamKey().equals("SMS_CONFIG_KEY")){
			TencentSmsConfig config= DataTypeConversionUtil.string2Entity(configItem.getParamValue(),TencentSmsConfig.class);
			if (null==config){
				configItem.setParamValue("");
			}else{
				config.updateTempTips();
				configItem.setParamValue(DataTypeConversionUtil.entity2jonsStr(config));
			}
		}else if (configItem.getParamKey().equals("ALIYUN_SMS_CONFIG_KEY")){
			AliyunSmsConfig config= DataTypeConversionUtil.string2Entity(configItem.getParamValue(),AliyunSmsConfig.class);
			if (null==config){
				configItem.setParamValue("");
			}else{
				config.updateTempTips();
				configItem.setParamValue(DataTypeConversionUtil.entity2jonsStr(config));
			}
		}else if (configItem.getParamKey().equals("SMS_BAO")){
			SmsBaoConfig config= DataTypeConversionUtil.string2Entity(configItem.getParamValue(),SmsBaoConfig.class);
			if (null==config){
				configItem.setParamValue("");
			}else{
				config.updateTempTips();
				configItem.setParamValue(DataTypeConversionUtil.entity2jonsStr(config));
			}
		}
	}


	private void insertEmptyConf(){
		for (ConfigConstantEnum item:ConfigConstantEnum.values()){
			if (!item.getStatus()){
				continue;
			}
			SysConfigEntity entity=this.getOne(new QueryWrapper<SysConfigEntity>()
					.eq("param_key",item.getConfKey())
					.last("limit 1")
					.select("id")
			);
			if(null==entity){
				SysConfigEntity newEntity=new SysConfigEntity();
				newEntity.setParamKey(item.getConfKey());
				newEntity.setRemark(item.getName());
				newEntity.setStatus(0);
				this.save(newEntity);
			}
		}
	}

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		this.insertEmptyConf();
		String paramKey=null;
		if(params.containsKey("key") ){
			paramKey=(String)params.get("key");
		}

		IPage<SysConfigEntity> page = this.page(
			new Query<SysConfigEntity>().getPage(params),
			new QueryWrapper<SysConfigEntity>()
				.in("param_key",ConfigConstantEnum.isEnableKeys())
				.like(StringUtils.isNotBlank(paramKey),"param_key", paramKey)
				.like(StringUtils.isNotBlank(paramKey),"remark", paramKey)
				.notIn(!QuerySysAuth.getShowOtherSysConf(),"param_key",ConfigConstantEnum.getOtherConfKey())
				.orderByDesc("weight")
		);
		page.getRecords().forEach(item->{
			updateDefault(item);
		});
		return new PageUtils(page);
	}

	@Override
	public void saveConfig(SysConfigEntity config) {
		this.updateConfAttrs(config);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateConfAttrs(SysConfigEntity config) {
		int cid=ConfigConstantEnum.getConfIdByName(config.getParamKey());

		if (!Arrays.asList(QuerySysAuth.EXISTS_CONFIG_IDS).contains(cid)){
			throw new RRException("配置未能生效");
		}
		SysConfigEntity tConfig =this.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key",config.getParamKey()).last("limit 1"));
		if (null==tConfig){
			//insert
			this.save(config);
			sysConfigRedis.saveOrUpdate(config);
			return;
		}else {
			config.setId(tConfig.getId());
		}
		this.update(null,new UpdateWrapper<SysConfigEntity>()
                .set(StringUtils.isNotBlank(config.getParamValue()), "param_value", config.getParamValue())
                .set(StringUtils.isNotBlank(config.getRemark()), "remark", config.getRemark())
				.set(null!=config.getWeight(), "weight", config.getWeight())
                .set(config.getStatus()!=null, "status", config.getStatus())
                .eq("id", config.getId())
		);
		//this.updateById(config);
		sysConfigRedis.saveOrUpdate(config);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateValueByKey(String key, String value) {
		baseMapper.updateValueByKey(key, value);
		sysConfigRedis.delete(key);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBatch(Long[] ids) {
		for(Long id : ids){
			SysConfigEntity config = this.getById(id);
			sysConfigRedis.delete(config.getParamKey());
		}

		this.removeByIds(Arrays.asList(ids));
	}

	@Override
	public String getValue(String key) {
		SysConfigEntity config = sysConfigRedis.get(key);
		if(config == null){
			config = baseMapper.queryByKey(key);
			sysConfigRedis.saveOrUpdate(config);
		}
		return config == null ? null : config.getParamValue();
	}
	
	@Override
	public <T> T getConfigObject(String key, Class<T> clazz) {
		String value = getValue(key);
		if(StringUtils.isNotBlank(value)){
			try{
				return new Gson().fromJson(value, clazz);
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new RRException("获取参数失败");
		}
	}

	@Override
	public SysConfigEntity getConfigByKey(String key) {
		SysConfigEntity config = sysConfigRedis.get(key);
		if(config == null){
			config = baseMapper.queryByKey(key);
			sysConfigRedis.saveOrUpdate(config);
		}
		return config;
	}

	@Override
    public File dumpMysqlDbFile() {

		try {
			DataSourceProperties ds=dataSourceConfig.dataSourceProperties();
			if (null==ds){
				return null;
			}
			String jdbcUrl=ds.getUrl();
			//jdbc:mysql://aa.aa.com:3306/dns20?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
			//System.out.println(jdbcUrl);
			Integer pIndex=jdbcUrl.indexOf("?");
			//logger.debug(jdbcUrl);
			String  pString=jdbcUrl.substring(0,pIndex);
			//logger.debug(pString);
			String dbName = pString.substring(pString.lastIndexOf("/") + 1);
			//logger.debug(dbName);
			String username =ds.getUsername();
			String password=ds.getPassword();

			String curJarPath= ManagementFactory.getRuntimeMXBean().getClassPath();
			String jarDir = new File(curJarPath).getParent();
			Path pPath= Paths.get(jarDir);
			String dTm= "cdn_db"+ DateUtils.format(new Date(),DateUtils.DATE_PATTERN)+".sql";
			Path fPath=Paths.get(dTm);
			Path outPath=pPath.resolve(fPath);
			//System.out.println("JAR directory path: " + jarDir);

			String executeCmd = "mysqldump -u "+username+" -p"+password+" " + dbName + " -r " + outPath;
			System.out.println(executeCmd);
			Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
			int processComplete = runtimeProcess.waitFor();
			if (processComplete ==  0) {
				//System.out.println("Backup taken successfully!");
				return  outPath.toFile();
			} else {
				System.out.println("Could not take mysql backup");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }

	@Override
	public boolean importDbFile(Reader reader) {
		try {
			DataSourceProperties ds=	dataSourceConfig.dataSourceProperties();
			if (null==ds){
				return false;
			}
			// 建立连接
			Connection conn = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), ds.getPassword());
			// 创建ScriptRunner，用于执行SQL脚本
			ScriptRunner runner = new ScriptRunner(conn);
			runner.setErrorLogWriter(null);
			runner.setLogWriter(null);
			// 遇到错误回滚
			runner.setStopOnError(true);
			Resources.setCharset(Charset.forName("UTF-8"));
			// 执行SQL脚本
			//runner.runScript(new InputStreamReader(new FileInputStream(sqlFilePath), "UTF-8"));
			runner.runScript(reader);
			// 关闭连接
			conn.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}


}
