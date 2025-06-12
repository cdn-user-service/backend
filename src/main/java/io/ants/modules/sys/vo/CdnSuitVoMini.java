package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CdnSuitVoMini {

    private Integer id;

    private Long userId;

    //private String SerialNumber;

    private Integer suitType;

    private java.sql.Date startTime;

    private java.sql.Date endTime;

    private  Integer status;

    //"flow","site","charging_mode","custom_dns","private_waf"

    private long flow= 0l;

    private int site=0;

    //private int chargingMode=0;

   // private int customDns=0;

   // private int privateWaf=0;

    //加油包已使用量
   // private BigDecimal usedFlow=BigDecimal.ZERO;

    private Date CreateTime=new Date();


    //当前套餐已用量
    //private  ProductAttrVo consume;


    //当前套餐的产品属性
    //private CdnProductEntity productEntity;
    private CdnProductVoMini product;

    //当前套餐 cname
    //private  String cname;

    //当前套餐 cdnGroupHashList
    private  String hashList;

    // @TableField(exist = false)
    //private Map productAttrMap;

    //当月流量数据
    //private Object currentMonthData;


    private Object username;

    //当前 的suitName数据

    public Object suitName;


    //public CdnConsumeUsedInfoVo usedTypeDetail;


}
