package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-09-22 10:11:35
 */
@Data
@TableName("tb_rewrite")
public class TbRewriteEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;

	/**
	 * 
	 */
	private Long userId;
	/**
	 * 
	 */
	@NotBlank(message = "serialNumber不能为空")
	private String serialNumber;
	/**
	 * 
	 */
	@NotBlank(message = "serverName不能为空")
	private String serverName;

	// 1==REWRITE;2==gzip_js
	private Integer rewriteType = 1;

	/**
	 * 
	 */
	private String alias;
	/**
	 * 
	 */
	private Integer rewriteMode = 307;

	/**
	 * 跟随方式
	 */
	private String followMode;

	/**
	 * http|https|$scheme
	 */
	private String scheme;
	/**
	 * 
	 */
	@NotBlank(message = "target不能为空")
	private String target;

	private String jsContent;

	private String remark;

	private Integer status = 1;

	private String certStr;

	private String keyStr;

	private Date notAfter;

	/**
	 * 
	 */
	private Date createTmp;

	@TableField(exist = false)
	private Object suitObj;

	@TableField(exist = false)
	private Object aliasLs;

	@TableField(exist = false)
	private Object user;
}
