package io.ants.modules.app.vo;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TbUserVo implements Serializable {
    private static final long serialVersionUID = 1L;


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
    private Integer propertyBalance;


    /**
     * 财产 积分
     */
    private Integer propertyScore;


    private String propertyPayPassword;
    /**
     * 备注
     */
    private String note;


    /**
     * 不可用模块
     */
    private String unvalidModel;


    private String loginType;

    /**
     * 创建时间
     */
    private Date createTime;
}
