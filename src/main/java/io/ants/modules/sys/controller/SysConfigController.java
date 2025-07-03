/**
 * Copyright (c)  rights reserved.
 *
 *
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.internal.util.file.IOUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.sys.vo.NodeInstallShVo;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.ZeroSslConfig;
import io.ants.modules.utils.factory.MailFactory;
import io.ants.modules.utils.factory.ZeroSslFactory;
import io.ants.modules.utils.service.MailService;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统配置信息
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/sys/config")
public class SysConfigController extends AbstractController {
	@Autowired
	private SysConfigService sysConfigService;

	/**
	 * 所有配置列表
	 */
	@PostMapping("/list")
	@PreAuthorize("hasAuthority('sys:config:list')")
	public R list(@RequestBody Map<String, Object> params) {
		PageUtils page = sysConfigService.queryPage(params);
		return R.ok().put("page", page).put("allFields", ConfigConstantEnum.getAllConfKey(null));
	}

	/**
	 * 配置信息
	 */
	@GetMapping("/info/{id}")
	@PreAuthorize("hasAuthority('sys:config:info')")
	public R info(@PathVariable("id") Long id) {
		SysConfigEntity config = sysConfigService.getById(id);
		return R.ok().put("config", config);
	}

	/**
	 * 配置信息
	 */
	@GetMapping("/info2/{paramKey}")
	public R info(@PathVariable("paramKey") String paramKey) {
		SysConfigEntity config = sysConfigService
				.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", paramKey).last("limit 1"));
		return R.ok().put("config", config);
	}

	@SysLog("修改配置状态")
	@GetMapping("/update/status/{id}")
	@PreAuthorize("hasAuthority('sys:config:save')")
	public R updateStatus(@PathVariable("id") Long id) {
		SysConfigEntity config = sysConfigService.getById(id);
		if (null != config && 2 != config.getStatus()) {
			config.setStatus(config.getStatus() == 1 ? 0 : 1);
			sysConfigService.updateConfAttrs(config);
		}
		return R.ok().put("status", config.getStatus());
	}

	@SysLog("修改配置状态")
	@GetMapping("/update/conf/status")
	@PreAuthorize("hasAuthority('sys:config:save')")
	public R updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
		SysConfigEntity config = sysConfigService.getById(id);
		if (null != config) {
			config.setStatus(status);
			sysConfigService.updateConfAttrs(config);
		}
		return R.ok().put("status", config.getStatus());
	}

	private boolean isJSON(String str) {
		boolean result = false;
		try {
			Object obj = JSON.parse(str);
			result = true;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * 保存配置
	 */
	@SysLog("保存配置")
	@PostMapping("/save")
	@PreAuthorize("hasAuthority('sys:config:save')")
	public R save(@RequestBody SysConfigEntity config) {
		// System.out.println(config);
		ValidatorUtils.validateEntity(config);
		config.setSerialVersionUID(1L);
		if (this.isJSON(config.getParamValue())) {
			List<String> list = ConfigConstantEnum.getParamClass(config.getParamKey());
			if (null == list) {
				sysConfigService.saveConfig(config);
				return R.ok();
			} else {
				String err_key = "";
				JSONObject jsonObject = JSONObject.parseObject(config.getParamValue());
				for (String key : list) {
					if ("serialVersionUID".equals(key)) {
						continue;
					}
					if (!jsonObject.containsKey(key)) {
						err_key += "[" + key + "]";
					}

				}
				if (0 == err_key.length()) {
					sysConfigService.saveConfig(config);
					return R.ok();
				} else {
					return R.error("[save]参数缺失" + err_key);
				}
			}
		} else {
			return R.error("参数值JSON 格式有误！");
		}

	}

	private void update_config(SysConfigEntity config) {
		switch (config.getParamKey()) {
			case "TENCENTUSERCERTIFY_CONFIG_KEY":
				break;
			case "ALIPAYUSERCERTIFY_CONFIG_KEY":
				break;
			case "WXPAY_CONFIG_KEY":
				if (true) {
					String pvalue = config.getParamValue();
					JSONObject obj = DataTypeConversionUtil.string2Json(pvalue);
					obj.put("certData", new JSONArray());
					config.setParamValue(obj.toString());
				}
				break;
			case "CDN_NODECHECK_KEY":
				if (true) {
					try {
						HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
						StaticVariableUtils.masterWebSeverName = request.getServerName();
						BufferedWriter out = new BufferedWriter(new FileWriter("/usr/ants/ants_port_scan/config.yaml"));
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("token: '%s'\n", StaticVariableUtils.checkNodeInputToken));
						sb.append(String.format("url: '%s://%s%s'\n", StaticVariableUtils.MasterProtocol,
								StaticVariableUtils.masterWebSeverName, QuerySysAuth.CHECK_NODE_INPUT_PATH));
						out.write(sb.toString());
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						ShellUtils.execShell("pkill port-scan");
						ShellUtils.execShell("pkill ants_port_scan");
					}
				}
				break;
			case "STRIPE_PAY_CONF":
				break;
			case "ZERO_SSL_CONFIG":
				ZeroSslConfig zConf = ZeroSslFactory.build();
				if (null != zConf) {
					if (StringUtils.isNotBlank(zConf.getEab_kid()) && StringUtils.isNotBlank(zConf.getEab_hmac_key())) {
						QuerySysAuth.setZeroSslAccessId(zConf.getEab_kid());
						QuerySysAuth.setZeroSslAccessPwd(zConf.getEab_hmac_key());
					}
					if (StringUtils.isNotBlank(zConf.getApi_key())) {
						ZeroSslUtils.setAccess_key(zConf.getApi_key());
					}
				}
				break;
			default:
				break;
		}
		sysConfigService.updateConfAttrs(config);
	}

	/**
	 * 修改配置
	 */
	@SysLog("修改配置")
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('sys:config:update')")
	public R update(@RequestBody SysConfigEntity config) {
		checkDemoModify();
		ValidatorUtils.validateEntity(config);
		config.setSerialVersionUID(1L);
		if (this.isJSON(config.getParamValue())) {
			List<String> list = ConfigConstantEnum.getParamClass(config.getParamKey());
			if (null == list) {
				this.update_config(config);
				return R.ok();
			} else {
				String err_key = "";
				JSONObject jsonObject = JSONObject.parseObject(config.getParamValue());
				if (null == jsonObject) {
					return R.error("参数值JSON 格式有误！");
				}
				for (String key : list) {
					if (!"serialVersionUID".equals(key)) {
						if (!jsonObject.containsKey(key)) {
							err_key += "[" + key + "]";
						}
					}
				}
				if (0 == err_key.length()) {
					this.update_config(config);
					return R.ok();
				} else {
					return R.error("[update]参数缺失" + err_key);
				}
			}

		} else {
			return R.error("参数值JSON 格式有误！");
		}
	}

	/**
	 * 删除配置
	 */
	@SysLog("删除配置")
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('sys:config:delete')")
	public R delete(@RequestBody Map params) {
		checkDemoModify();
		if (!params.containsKey("ids")) {
			return R.error("[delete]参数缺失");
		}
		String ids_ = params.get("ids").toString();
		String[] id_s = ids_.split(",");
		Long[] ids = new Long[id_s.length];
		for (int i = 0; i < id_s.length; i++) {
			ids[i] = Long.parseLong(id_s[i]);
		}
		sysConfigService.deleteBatch(ids);
		return R.ok();
	}

	@GetMapping("/getParamClass")
	public R getParamClass(@RequestParam String paramKey) {
		return R.ok().put("data", ConfigConstantEnum.getParamClass(paramKey));
	}

	@GetMapping("/ants/conf/view")
	@PreAuthorize("hasAuthority('sys:ants_conf:save')")
	public R get_redis_conf_view() {
		// yum install -y wget && wget -O install.sh
		// http://download.antsxdp.com/nginx20/install.sh && sh install.sh -i
		// 121.62.18.146 -p 8881130py
		NodeInstallShVo nvo = new NodeInstallShVo();
		if (QuerySysAuth.IS_OFFLINE_VERSION) {
			HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
			StaticVariableUtils.authMasterIp = request.getServerName();
		}
		// 1 获取最新ants_agent
		// final String goods="ants_agent";
		// String remote="1.29";
		// Map<String,Object> rtMap=
		// QueryAnts.queryAntsXdpNewestVersionData(goods,"1.10");
		// if (rtMap.containsKey("remote")){
		// if (null!=rtMap.get("remote")){
		// remote=rtMap.get("remote").toString();
		// }
		// }
		// 2
		String requirepass = FileUtils.getRedisPassWord();
		nvo.getRedis_conf().setRequirepass(requirepass);

		// 3 install centos command
		String iCmd = "yum install -y wget && wget -O install.sh " + QuerySysAuth.getDownloadInstallNodeAddressCentos7()
				+ " && sh install.sh";
		nvo.setMaster_ip(StaticVariableUtils.authMasterIp);
		nvo.setInstall_node_command(iCmd);
		nvo.setFull_install_node_command(
				String.format("%s -i %s -p %s ", iCmd, StaticVariableUtils.authMasterIp, requirepass));

		// wget -O - http://download.antsxdp.com/nginx20/3.0/install.sh | sudo bash -s
		// -- -i 121.62.17.153 -p 8881130py
		if (StringUtils.isNotBlank(QuerySysAuth.getDownloadInstallNodeAddressWithUbuntu())) {
			// apt install -y wget && wget -O install.sh
			// http://download.antsxdp.com/nginx20/ubuntu/install.sh && sh install.sh -i
			// 121.62.17.153 -p 8881130py
			String ubuntuICmd = String.format(
					"apt  install -y wget && wget -O install.sh %s && sh install.sh -i %s -p %s",
					QuerySysAuth.getDownloadInstallNodeAddressWithUbuntu(), StaticVariableUtils.authMasterIp,
					requirepass);
			nvo.setFull_install_node_command_with_ubuntu(ubuntuICmd);
		}
		if (!QuerySysAuth.IS_OFFLINE_VERSION) {
			if (StringUtils.isNotBlank(QuerySysAuth.getDownloadInstallWithCentosBbr())) {
				nvo.setFull_install_bbr_centos(QuerySysAuth.getDownloadInstallWithCentosBbr());
			}
			if (StringUtils.isNotBlank(QuerySysAuth.getDownloadInstallWithUbuntuBbr())) {
				nvo.setFull_install_bbr_ubuntu(QuerySysAuth.getDownloadInstallWithUbuntuBbr());
			}
			nvo.setUbuntu_cert_server(QuerySysAuth.getInstallCertificateService());
		}

		return R.ok().put("data", nvo);
	}

	@PostMapping("/test/send-mail")
	public R save_redis_conf(@RequestBody Map<String, Object> params) {
		if (!params.containsKey("toMail")) {
			return R.error("toMail is null");
		}
		MailService mailService = MailFactory.build();
		mailService.sendEmail(params.get("toMail").toString(), "mail test", "mail test");
		return R.ok().put("toMail", params);
	}

	@GetMapping("/export/db")
	@PreAuthorize("hasAuthority('sys:ants_conf:save')")
	@SysLog("导出数据库")
	public ResponseEntity<byte[]> exportDb() {
		File file = sysConfigService.dumpMysqlDbFile();
		if (null == file) {
			return new ResponseEntity("", null, HttpStatus.BAD_REQUEST);
		}
		try {
			InputStream inputStream = new FileInputStream(file);
			byte[] contents = IOUtils.toByteArray(inputStream);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename(file.getName()).build());

			return new ResponseEntity<>(contents, headers, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity("", null, HttpStatus.BAD_REQUEST);

	}

	@PostMapping("/import/db")
	@SysLog("导入数据库")
	
	@PreAuthorize("hasAuthority('sys:ants_conf:save')")
	public R importDb(@RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			throw new RRException("上传文件不能为空");
		}
		if (false) {
			try {
				Reader reader = new InputStreamReader(file.getInputStream(), "UTF-8");
				boolean r = sysConfigService.importDbFile(reader);
				if (r) {
					return R.ok();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return R.error("导入失败！");
	}

}
