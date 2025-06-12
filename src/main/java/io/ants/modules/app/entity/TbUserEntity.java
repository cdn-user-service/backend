/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.ants.modules.app.vo.UserModuleVo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 用户
 *
 * @author Mark sunlightcs@gmail.com
 */
@Data
@TableName("tb_user")
public class TbUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId
	private Long userId;
	/**
	 * 用户名
	 */
	private String username;
	/**
	 * 手机号
	 */
	private String mobile;


	private String mail;
	/**
	 * 密码
	 */
	private String password;


	/**
	 * 微信OPENID
	 */
	private String wechatOpenid;


	/*
	 * 注册IP
	 * */
	private String registIp;

	/**
	 * 实名 状态
	 */
	private Integer realnameStatus;

	/**
	 * 实名 姓名
	 */
	private String realnameName;

	/**
	 * 实名 证件类型 0=身份证
	 */
	private Integer realnameCertificatetype;

	/**
	 * 实名 证件ID
	 */
	private String realnameCertificateid;


	private Integer realnameMode;


	private Date realnameTime;

	/**
	 * 财产 余额
	 */
	private Integer propertyBalance=0;


	/**
	 * 财产 积分
	 */
	private Integer propertyScore=0;


	private String propertyPayPassword;

	/**
	 * 备注
	 */
	private String note="";


	/**
	 * 不可用模块 unvalid_model
	 */
	private String unvalidModel;

	private String loginType;


	//可用状态 0禁用 1 可用
	private Integer status=1;


	/**
	 * CDN api token
	 */
	private String uCdnAccessToken="";


	/**
	 * cdn dns token
	 */
	private String uDnsAccessToken="";


	@TableField(exist = false)
	private UserModuleVo userModuleVo;


	//谷歌验证SecretKey
	private String googleAuthSecretKey="";
	//谷歌验证开启状态
	private int googleAuthStatus=0;

	private int whiteIpStatus=0;

	/**
	 * 创建时间
	 */
	private Date createTime=new Date();

}
