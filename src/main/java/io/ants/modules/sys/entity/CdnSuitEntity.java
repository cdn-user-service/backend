package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.ants.modules.sys.vo.CdnConsumeUsedInfoVo;
import io.ants.modules.sys.vo.ProductAttrVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 套餐，加油包 额定包
 */
@Data
@TableName("cdn_suit")
public class CdnSuitEntity {
    private Integer id;

    private Long userId;

    private String SerialNumber;

    private String paySerialNumber;

    private Integer suitType;

    private String attrJson;

    private java.sql.Date startTime;

    private java.sql.Date endTime;

    private  Integer status;

    //"flow","site","charging_mode","custom_dns","private_waf"

    private long flow= 0l;

    private int site=0;

    private int chargingMode=0;

    private int customDns=0;

    private int privateWaf=0;

    //加油包已使用量
    private BigDecimal usedFlow=BigDecimal.ZERO;

    private Date CreateTime=new Date();

    //当前套餐拥有的属性及量
    @TableField(exist = false)
    private ProductAttrVo attr;


    //当前套餐已用量
    @TableField(exist = false)
    private  ProductAttrVo consume;


    //当前套餐的产品属性
    @TableField(exist = false)
    private CdnProductEntity productEntity;

    //当前套餐的产品属性product-别名
    @TableField(exist = false)
    private Object product;



    //当前套餐 cname
    @TableField(exist = false)
    private  String cname;

    //当前套餐 cdnGroupHashList
    @TableField(exist = false)
    private  String hashList;

    // @TableField(exist = false)
    //private Map productAttrMap;

    //当月流量数据
    @TableField(exist = false)
    private Object currentMonthData;

    @TableField(exist = false)
    private Object username;

    //绑定的站点数据
    @TableField(exist = false)
    private Object bindSiteList;

    //当前 的主套餐数据
    @TableField(exist = false)
    public Object mainSuitObj;

    //当前 的加油包数据
    @TableField(exist = false)
    public Object addedServices;

    //当前 的suitName数据
    @TableField(exist = false)
    public Object suitName;

    @TableField(exist = false)
    public CdnConsumeUsedInfoVo usedTypeDetail;
}
