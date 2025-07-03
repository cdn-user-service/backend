/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.oss.controller;

import com.google.gson.Gson;
import io.ants.common.exception.RRException;
import io.ants.common.utils.ConfigConstant;
import io.ants.common.utils.Constant;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.common.validator.group.AliyunGroup;
import io.ants.common.validator.group.QcloudGroup;
import io.ants.common.validator.group.QiniuGroup;
import io.ants.modules.oss.cloud.CloudStorageConfig;
import io.ants.modules.oss.cloud.OSSFactory;
import io.ants.modules.oss.entity.SysOssEntity;
import io.ants.modules.oss.service.SysOssService;
import io.ants.modules.sys.service.SysConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * 文件上传
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("sys/oss")
public class SysOssController {
	@Autowired
	private SysOssService sysOssService;
	@Autowired
	private SysConfigService sysConfigService;

	private final static String KEY = ConfigConstant.CLOUD_STORAGE_CONFIG_KEY;

	/**
	 * 列表
	 */
	@PostMapping("/list")
	@PreAuthorize("hasAuthority('sys:oss:all')")
	public R list(@RequestBody Map<String, Object> params) {
		PageUtils page = sysOssService.queryPage(params);

		return R.ok().put("page", page);
	}

	/**
	 * 云存储配置信息
	 */
	@GetMapping("/config")
	@PreAuthorize("hasAuthority('sys:oss:all')")
	public R config() {
		CloudStorageConfig config = sysConfigService.getConfigObject(KEY, CloudStorageConfig.class);

		return R.ok().put("config", config);
	}

	/**
	 * 保存云存储配置信息
	 */
	@PostMapping("/saveConfig")
	@PreAuthorize("hasAuthority('sys:oss:all')")
	public R saveConfig(@RequestBody CloudStorageConfig config) {
		// 校验类型
		ValidatorUtils.validateEntity(config);

		if (config.getType() == Constant.CloudService.QINIU.getValue()) {
			// 校验七牛数据
			ValidatorUtils.validateEntity(config, QiniuGroup.class);
		} else if (config.getType() == Constant.CloudService.ALIYUN.getValue()) {
			// 校验阿里云数据
			ValidatorUtils.validateEntity(config, AliyunGroup.class);
		} else if (config.getType() == Constant.CloudService.QCLOUD.getValue()) {
			// 校验腾讯云数据
			ValidatorUtils.validateEntity(config, QcloudGroup.class);
		}

		sysConfigService.updateValueByKey(KEY, new Gson().toJson(config));

		return R.ok();
	}

	/**
	 * 上传文件

	 */
	@PostMapping("/upload")
	public R upload(@RequestParam("file") MultipartFile file) throws Exception {
		if (file.isEmpty()) {
			throw new RRException("上传文件不能为空");
		}
		// 上传文件
		String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
		String url = OSSFactory.build().uploadSuffix(file.getBytes(), suffix);

		// 保存文件信息
		SysOssEntity ossEntity = new SysOssEntity();
		ossEntity.setUrl(url);
		ossEntity.setCreateDate(new Date());
		sysOssService.save(ossEntity);
		return R.ok().put("url", url).put("path", "sys/oss/get/image/" + url);
	}

	/**
	 * 删除
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('sys:oss:all')")
	public R delete(@RequestBody Map params) {
		if (!params.containsKey("ids")) {
			return R.error("参数缺失");
		}
		String ids_ = params.get("ids").toString();
		String[] id_s = ids_.split(",");
		Long[] ids = new Long[id_s.length];
		for (int i = 0; i < id_s.length; i++) {
			ids[i] = Long.parseLong(id_s[i]);
		}
		sysOssService.removeByIds(Arrays.asList(ids));

		return R.ok();
	}

	// "获取图片-以ImageIO流形式写回"
	@GetMapping(value = "/get/image/{img}")
	public void getImage(HttpServletResponse response, @PathVariable("img") String img) {
		try {
			OutputStream os = null;
			try {
				// 读取图片
				String path = String.format("/usr/ants/cdn-api/upload/%s", img);
				BufferedImage image = ImageIO.read(new FileInputStream(new File(path)));
				response.setContentType("image/png");
				os = response.getOutputStream();
				if (image != null) {
					ImageIO.write(image, "png", os);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					os.flush();
					os.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
