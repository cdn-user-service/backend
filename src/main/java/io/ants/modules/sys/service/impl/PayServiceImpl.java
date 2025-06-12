package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.TbOrderDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbOrderEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.PayBalanceForm;
import io.ants.modules.app.vo.TokenPayCallBackBodyVo;
import io.ants.modules.sys.dao.CdnSuitDao;
import io.ants.modules.sys.dao.SysLogDao;
import io.ants.modules.sys.dao.TbPayRecordDao;
import io.ants.modules.sys.entity.CdnProductEntity;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.entity.SysLogEntity;
import io.ants.modules.sys.entity.TbPayRecordEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.PayService;
import io.ants.modules.sys.service.SysConfigService;
import io.ants.modules.sys.vo.ProductAttrVo;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.SysLoginKeyConfig;
import io.ants.modules.utils.config.alipay.AlipayBeanConfig;
import io.ants.modules.utils.config.alipay.AlipayConfig;
import io.ants.modules.utils.config.alipay.AlipayNotifyParam;
import io.ants.modules.utils.config.cccpay.CccYunNotifyForm;
import io.ants.modules.utils.config.fuiou.FuiouConfig;
import io.ants.modules.utils.config.wechat.ConfigUtil;
import io.ants.modules.utils.factory.AllinpayFactory;
import io.ants.modules.utils.factory.CccyunPayFactory;
import io.ants.modules.utils.factory.TokenPayFactory;
import io.ants.modules.utils.factory.fuiou.FuiouFactory;
import io.ants.modules.utils.factory.wxchat.WXPayFactory;
import io.ants.modules.utils.service.TokenPayService;
import io.ants.modules.utils.service.alipay.AlipayService;
import io.ants.modules.utils.service.allinpay.AllinpaySybPayService;
import io.ants.modules.utils.service.allinpay.SybUtil;
import io.ants.modules.utils.service.fuiou.FuiouService;
import io.ants.modules.utils.service.fuiou.HttpUtils;
import io.ants.modules.utils.service.fuiou.Utils;
import io.ants.modules.utils.service.wechat.CommonUtil;
import io.ants.modules.utils.service.wechat.WXPayConstants;
import io.ants.modules.utils.service.wechat.WXPayService;
import io.ants.modules.utils.service.wechat.WXPayUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class PayServiceImpl implements PayService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private TbPayRecordDao tbPayRecordDao;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private SysLogDao sysLogDao;
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;

    private void writeLog(String msg){
        SysLogEntity logEntity=new SysLogEntity();
        logEntity.setUserType(UserTypeEnum.MANAGER_TYPE.getId());
        logEntity.setLogType(LogTypeEnum.FINANCE_LOG.getId());
        logEntity.setOperation(msg);
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        //设置IP地址
        logEntity.setIp(IPUtils.getIpAddr(request));
        sysLogDao.insert(logEntity);
    }



    private Integer get_charging_mode( JSONArray attrJSONarray){
        for (int i = 0; i < attrJSONarray.size(); i++) {
            JSONObject attr=attrJSONarray.getJSONObject(i);
            if(attr.containsKey("attr") && attr.containsKey("value")&& "charging_mode".equals(attr.getString("attr"))){
                return  attr.getInteger("value");
            }
        }
        return 1;
    }

    //处理实名
    private void operate_authentication(TbOrderEntity order, TbPayRecordEntity record){
        //更新订单
        order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
        tbOrderDao.updateById(order);

        //更新支付记录
        record.setStatus(PayRecordStatusEnum.STATUS_SUCCESS.getId());
        record.setOperateStatus(1);
        tbPayRecordDao.updateById(record);
    }

    /*
    处理充值
    * */
    private void operate_recharge(TbOrderEntity order,TbPayRecordEntity record){
        Long userId=order.getUserId();
        if(null!=userId){
            TbUserEntity user= tbUserDao.selectById(userId);
            if(null!=user){
                //user.setPropertyBalance(user.getPropertyBalance()+record.getPayPaid());
                //tbUserDao.updateById(user);
                tbUserDao.update(null,new UpdateWrapper<TbUserEntity>().eq("user_id",userId).set("property_balance",user.getPropertyBalance()+order.getPayable()));
            }else{
                logger.error("ORDER_ERROR:"+ DataTypeConversionUtil.entity2jonsStr(record));
            }
        }
        //更新订单
        order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
        tbOrderDao.updateById(order);

        //更新支付记录
        record.setStatus(PayRecordStatusEnum.STATUS_SUCCESS.getId());
        record.setOperateStatus(1);
        tbPayRecordDao.updateById(record);
    }


    private void updateRecord(TbPayRecordEntity record,Integer status){
        // record.setStatus(PayRecordStatusEnum.STATUS_SUCCESS.getId());
        logger.info("updateRecord");
        record.setStatus(status);
        record.setOperateStatus(1);
        tbPayRecordDao.updateById(record);
    }

    /*
    * 处理购买CDN套餐
    * */
    private void operate_cdn_buy(TbOrderEntity order,TbPayRecordEntity record){
        //创建套餐
        logger.info("operate__cdn__buy");
        CdnSuitEntity suitEntity=new CdnSuitEntity();
        suitEntity.setUserId(order.getUserId());
        suitEntity.setSerialNumber(order.getSerialNumber());
        suitEntity.setPaySerialNumber(order.getSerialNumber());
        suitEntity.setSuitType(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId());

        String initJsonStr=order.getInitJson();
        JSONObject initJson= DataTypeConversionUtil.string2Json(initJsonStr);
        if(!initJson.containsKey("product_obj")){
            logger.info("product_obj is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        // //{"buy_obj":"{\"type\":\"y\",\"sum\":1}","product_obj":"{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"}
        JSONObject productJson=initJson.getJSONObject("product_obj");
        if(!productJson.containsKey("attrJson")){
            logger.info("attrJson is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        int charging_mode_value=0;
        //计算套餐的总量
        try{
            JSONArray attrJsonArray=productJson.getJSONArray("attrJson");
            ProductAttrVo productAttrVo=new ProductAttrVo();
            ProductAttrNameEnum.getFinalAttrJson(productAttrVo,attrJsonArray);
            suitEntity.setAttrJson(ProductAttrNameEnum.getFinalAttrJson(attrJsonArray).toJSONString());
            //复制部分属性
            ProductAttrVo.updateSuitAttrByAttrVo(suitEntity,productAttrVo);
            charging_mode_value= get_charging_mode(attrJsonArray);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(1==charging_mode_value){
            //前致付费
            if(!initJson.containsKey("buy_obj")){
                logger.info("buy_obj is null");
                this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
                return;
            }
            //{"buy_obj":"{\"type\":\"y\",\"sum\":1}","product_obj":"{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"}
            JSONObject buy_obj=initJson.getJSONObject("buy_obj");
            if(!buy_obj.containsKey("type") || !buy_obj.containsKey("sum")){
                logger.info("type OR sum is null");
                this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
                return;
            }
            if (buy_obj.getInteger("sum")>99){
                logger.info("sum is > 99");
                this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
                return;
            }
            try{
                Date eDate=ProductUnitEnum.getEndDate(buy_obj.getString("type"),new Date(),buy_obj.getInteger("sum"));
                //logger.info(buy_obj.getString("type"));
                //logger.info(buy_obj.getString("sum"));
                logger.info(eDate.toString());
                if (!DateUtils.isValidUtilDate(eDate)){
                    logger.info("eDate error:"+eDate.toString());
                    this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
                    return;
                }
                suitEntity.setStartTime(new java.sql.Date(new Date().getTime()) );
                suitEntity.setEndTime(new java.sql.Date(eDate.getTime()));
                suitEntity.setStatus(CdnSuitStatusEnum.NORMAL.getId());
                CdnProductEntity product=DataTypeConversionUtil.json2entity(productJson, CdnProductEntity.class);
                if(null!=product){
                    if (product.getStatus().equals(ProductStatusEnum.ONLY_BUY.getId())){
                        suitEntity.setStatus(CdnSuitStatusEnum.UNKNOWN.getId());
                    }
                }
                cdnSuitDao.insert(suitEntity);
            } catch (Exception e){
                e.printStackTrace();
            }
            //更新订单
            order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
            tbOrderDao.updateById(order);
            //更新支付记录
            this.updateRecord(record,PayRecordStatusEnum.STATUS_SUCCESS.getId());
        }else if(2==charging_mode_value ||3 ==charging_mode_value ){
            //后致付费
            Date eDate= DateUtils.addDateYears(new Date(),10);
            suitEntity.setStartTime(new java.sql.Date(new Date().getTime()) );
            suitEntity.setEndTime( new java.sql.Date(eDate.getTime()) );

            suitEntity.setStatus(CdnSuitStatusEnum.UNKNOWN.getId());
            cdnSuitDao.insert(suitEntity);

            //更新订单
            order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
            tbOrderDao.updateById(order);

            //更新支付记录
            this.updateRecord(record,PayRecordStatusEnum.STATUS_SUCCESS.getId());
        }


    }


    /*
    * 处理续费
    * */
    private void operate_cdn_renew(TbOrderEntity order,TbPayRecordEntity record){
        JSONObject order_init_obj=DataTypeConversionUtil.string2Json(order.getInitJson());
        //{\"buy_obj\":\"{\\\"serialNumber\\\":\\\"1650785734919001\\\",\\\"sum\\\":1}\",\"suit_obj\":\"{\\\"value\\\":20100,\\\"status\\\":1}\"}"
        if (!order_init_obj.containsKey("buy_obj")){
            logger.info("buy_obj is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        JSONObject buy_obj=order_init_obj.getJSONObject("buy_obj");
        if(!buy_obj.containsKey("serialNumber")){
            logger.info("serialNumber is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        String serialNumber=buy_obj.getString("serialNumber");
        if(!buy_obj.containsKey("sum")){
            logger.info("sum is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        Integer sum=buy_obj.getInteger("sum");
        if (!order_init_obj.containsKey("type")){
            logger.info("type is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        String type=order_init_obj.getString("type");
        Integer[] in_type_list={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        CdnSuitEntity source_suit=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("user_id",order.getUserId())
                .in("suit_type",in_type_list)
                .eq("serial_number",serialNumber)
                .ne("status",CdnSuitStatusEnum.DISABLE.getId())
                .orderByDesc("end_time")
                .last("limit 1")
        );
        if (null==source_suit){
            logger.info("source_suit is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        try{
            Date now=new Date();
            Date startDate=now;
            if(source_suit.getEndTime().after(now)){
                startDate=source_suit.getEndTime();
            }
            Date endDate=ProductUnitEnum.getEndDate(type,startDate,sum);
            CdnSuitEntity suitEntity=new CdnSuitEntity();
            suitEntity.setUserId(order.getUserId());
            suitEntity.setSerialNumber(serialNumber);
            suitEntity.setPaySerialNumber(order.getSerialNumber());
            suitEntity.setSuitType(OrderTypeEnum.ORDER_CDN_RENEW.getTypeId());
            suitEntity.setAttrJson(source_suit.getAttrJson());

            //从JOSNARRAY 映射为ProductAttrVo
            ProductAttrVo productAttrVo=new ProductAttrVo();
            ProductAttrVo.updateAttrVoFromString(productAttrVo,source_suit.getAttrJson());
            //复制属性
            ProductAttrVo.updateSuitAttrByAttrVo(suitEntity,productAttrVo);

            suitEntity.setStartTime(new java.sql.Date(startDate.getTime() ) );
            suitEntity.setEndTime(new java.sql.Date(endDate.getTime()));
            suitEntity.setStatus(CdnSuitStatusEnum.NORMAL.getId());
            cdnSuitDao.insert(suitEntity);

            //更新过期套餐为正常
            UpdateWrapper<CdnSuitEntity> updateWrapper=new UpdateWrapper<>();
            updateWrapper.eq("serial_number",serialNumber)
                    .in("suit_type",in_type_list)
                    .eq("status",CdnSuitStatusEnum.TIMEOUT.getId())
                    .set("status",CdnSuitStatusEnum.NORMAL.getId());
            cdnSuitDao.update(null,updateWrapper);

            //更新订单
            order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
            tbOrderDao.updateById(order);
            //推送新配置
            Map pushMap=new HashMap(8);
            pushMap.put(PushTypeEnum.PUSH_SUIT_SERVICE.getName(),serialNumber);
            cdnMakeFileService.pushByInputInfo(pushMap);
        }catch (Exception e){
            e.printStackTrace();
        }

        //更新支付记录
        updateRecord(record,PayRecordStatusEnum.STATUS_SUCCESS.getId());
        return;
    }

    /**
     * 获取套餐的最后到期时间
     * @return
     */
    private Date getSuitFinalEndDate(String suitSerialNumber){
        Integer[] s_in_list={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        Integer[] s_in_status_list={CdnSuitStatusEnum.NORMAL.getId()};
        CdnSuitEntity end_suit=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number",suitSerialNumber)
                .in("suit_type",s_in_list)
                .in("status", s_in_status_list)
                .orderByDesc("end_time")
                .last("limit 1")
        );
        if(null!=end_suit){
            return  end_suit.getEndTime();
        }
        return new Date();
    }

    /*
     * 处理增值服务
     */
    private void operate_cdn_adder(TbOrderEntity order,TbPayRecordEntity record){
        //"{\"buy_obj\":\"{\\\"serialNumber\\\":\\\"1650361752071001\\\",\\\"startTime\\\":1650769152,\\\"type\\\":\\\"y\\\",\\\"sum\\\":2}\",
        // \"suit_obj\":\"{\\\"value\\\":20100,\\\"status\\\":1}\",
        // \"product_obj\":\"{\\\"createtime\\\":1650789712000,\\\"productJson\\\":\\\"{\\\\\\\"m\\\\\\\":{\\\\\\\"value\\\\\\\":1,\\\\\\\"status\\\\\\\":0},\\\\\\\"s\\\\\\\":{\\\\\\\"value\\\\\\\":10100,\\\\\\\"status\\\\\\\":0},\\\\\\\"y\\\\\\\":{\\\\\\\"value\\\\\\\":20100,\\\\\\\"status\\\\\\\":1}}\\\",\\\"name\\\":\\\"加油包1\\\",\\\"attrJson\\\":\\\"[{\\\\\\\"id\\\\\\\":3,\\\\\\\"name\\\\\\\":\\\\\\\"defense\\\\\\\",\\\\\\\"unit\\\\\\\":\\\\\\\"G\\\\\\\",\\\\\\\"transferMode\\\\\\\":null,\\\\\\\"valueType\\\\\\\":\\\\\\\"int\\\\\\\",\\\\\\\"value\\\\\\\":10,\\\\\\\"status\\\\\\\":1,\\\\\\\"weight\\\\\\\":2},{\\\\\\\"id\\\\\\\":2,\\\\\\\"name\\\\\\\":\\\\\\\"flow\\\\\\\",\\\\\\\"unit\\\\\\\":\\\\\\\"G\\\\\\\",\\\\\\\"transferMode\\\\\\\":null,\\\\\\\"valueType\\\\\\\":\\\\\\\"int\\\\\\\",\\\\\\\"value\\\\\\\":1001,\\\\\\\"status\\\\\\\":1,\\\\\\\"weight\\\\\\\":1},{\\\\\\\"id\\\\\\\":4,\\\\\\\"name\\\\\\\":\\\\\\\"public_waf\\\\\\\",\\\\\\\"unit\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"transferMode\\\\\\\":null,\\\\\\\"valueType\\\\\\\":\\\\\\\"bool\\\\\\\",\\\\\\\"value\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"status\\\\\\\":1,\\\\\\\"weight\\\\\\\":1}]\\\",\\\"weight\\\":1,\\\"id\\\":4,\\\"productType\\\":12,\\\"status\\\":1}\"}"
        //{"price_obj":"{\"value\":100,\"status\":1}",
        // "buy_obj":{"serialNumber":"1663913563415002","sum":1,"startTime":1664243131367,"type":"m"},
        // "product_obj":"{\"createtime\":1659499295000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":100,\\\"status\\\":1}}\",\"name\":\"流量月包\",\"serverGroupIds\":\"\",\"attrJson\":\"[{\\\"attr\\\":\\\"flow\\\",\\\"id\\\":31,\\\"name\\\":\\\"流量\\\",\\\"valueType\\\":\\\"int\\\",\\\"unit\\\":\\\"G\\\",\\\"value\\\":200000}]\",\"weight\":1,\"id\":18,\"productType\":12,\"status\":1}"}
        JSONObject orderInitJson=DataTypeConversionUtil.string2Json(order.getInitJson());
        if(!orderInitJson.containsKey("buy_obj")){
            logger.info("buy_obj is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        JSONObject buy_obj= orderInitJson.getJSONObject("buy_obj");
        if(!buy_obj.containsKey("serialNumber") || !buy_obj.containsKey("startTime") || !buy_obj.containsKey("type") ||!buy_obj.containsKey("sum")){
            logger.info("serialNumber  startTime type  sum is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        String serialNumber=buy_obj.getString("serialNumber");
        String stm=String.valueOf(buy_obj.getLongValue("startTime")) ;
        Date startDate=new Date();
        if (stm.length()>10){
            startDate=DateUtils.LongStamp2Date(buy_obj.getLong("startTime"));
        }else{
            startDate=DateUtils.stamp2date(buy_obj.getInteger("startTime"));
        }
        String type=buy_obj.getString("type");
        Integer sum=buy_obj.getInteger("sum");
        if(!orderInitJson.containsKey("product_obj")){
            logger.info("product_obj is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        CdnProductEntity product=DataTypeConversionUtil.json2entity(orderInitJson.getJSONObject("product_obj"), CdnProductEntity.class);
        if(null==product){
            logger.info("product is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        try{
            JSONArray productAttrSJsonArray=DataTypeConversionUtil.string2JsonArray(product.getAttrJson());
            //[{"id":3,"name":"defense","unit":"G","transferMode":null,"valueType":"int","value":20,"status":1,"weight":2},{"id":2,"name":"flow","unit":"G","transferMode":null,"valueType":"int","value":1001,"status":1,"weight":1}]

            //JSONObject final_attr_json=ProductAttrNameEnum.getFinalAttrJson(product_attr_s_json_array);

            CdnSuitEntity suitEntity=new CdnSuitEntity();
            suitEntity.setUserId(order.getUserId());
            suitEntity.setSerialNumber(serialNumber);
            suitEntity.setPaySerialNumber(order.getSerialNumber());
            suitEntity.setSuitType(OrderTypeEnum.ORDER_CDN_ADDED.getTypeId());
            ProductAttrVo attrVo=new ProductAttrVo();
            ProductAttrNameEnum.getFinalAttrJson(attrVo,productAttrSJsonArray);
            suitEntity.setAttrJson(ProductAttrNameEnum.getFinalAttrJson(productAttrSJsonArray).toJSONString());
            //复制属性
            ProductAttrVo.updateSuitAttrByAttrVo(suitEntity,attrVo);
            suitEntity.setUsedFlow(BigDecimal.ZERO);
            if (startDate.before(new Date())){
                startDate=new Date();
            }

            for (int i = 0; i <sum ; i++) {
                suitEntity.setStartTime(new java.sql.Date(startDate.getTime()) );
                Date orderEndDate=ProductUnitEnum.getEndDate(type,startDate,1);
                suitEntity.setEndTime(new java.sql.Date(orderEndDate.getTime()));
                //2023 04 12 根据绑定的套餐计算到期时间
                Date suitEndDate= this.getSuitFinalEndDate(serialNumber);
                if (suitEndDate.before(orderEndDate)){
                    suitEntity.setEndTime(new java.sql.Date(suitEndDate.getTime()));
                }
                startDate=suitEntity.getEndTime();
                suitEntity.setStatus(CdnSuitStatusEnum.NORMAL.getId());
                cdnSuitDao.insert(suitEntity);
            }
            //更新订单
            order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
            tbOrderDao.updateById(order);

            //推送新配置
            Map pushMap=new HashMap(8);
            pushMap.put(PushTypeEnum.PUSH_SUIT_SERVICE.getName(),serialNumber);
            cdnMakeFileService.pushByInputInfo(pushMap);
        }catch (Exception e){
            e.printStackTrace();
        }

        //更新支付记录
        this.updateRecord(record,PayRecordStatusEnum.STATUS_SUCCESS.getId());

    }


    /*处理套餐升级*/
    private void operate_cdn_upgrade(TbOrderEntity order,TbPayRecordEntity record){
        // 原套餐为已升级 ；创建新套餐
        //{"buy_obj":{"serialNumber":"1650944278952005","sum":"1","type":"y"},
        // "price_obj":{"value":20100,"status":1},
        // "end_date_time":1903405086000,
        // "product_obj":"{\"createtime\":1650873242000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1000,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":20100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":120100,\\\"status\\\":1}}\",\"name\":\"测试产品三\",\"attrJson\":\"[{\\\"id\\\":5,\\\"name\\\":\\\"live_data\\\",\\\"unit\\\":\\\"\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"bool\\\",\\\"value\\\":\\\"1\\\",\\\"status\\\":1,\\\"weight\\\":1},{\\\"id\\\":6,\\\"name\\\":\\\"site\\\",\\\"unit\\\":\\\"个\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":10,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":1,\"id\":5,\"productType\":10,\"status\":1}"}

        JSONObject source_order_obj=DataTypeConversionUtil.string2Json(order.getInitJson());
        if(!source_order_obj.containsKey("buy_obj")){
            logger.info("serialNumber is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        JSONObject buy_obj=source_order_obj.getJSONObject("buy_obj");
        if(!buy_obj.containsKey("serialNumber")){
            logger.info("serialNumber is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        String source_serialNumber=buy_obj.getString("serialNumber");
        if (!source_order_obj.containsKey("end_date_time")){
            Date suitEndDate= getSuitFinalEndDate(source_serialNumber);
            source_order_obj.put("end_date_time",suitEndDate.getTime());
        }
        if(!source_order_obj.containsKey("product_obj")){
            logger.info("product_obj is null");
            this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
            return;
        }
        try{
            JSONObject productObjJson=source_order_obj.getJSONObject("product_obj");
            CdnProductEntity product=DataTypeConversionUtil.json2entity(productObjJson, CdnProductEntity.class);
            JSONArray productAttrJsonArray=DataTypeConversionUtil.string2JsonArray(product.getAttrJson());


            Long end_date_time=source_order_obj.getLong("end_date_time");
            Date end_date=DateUtils.LongStamp2Date(end_date_time);
            Integer[] in_type_list={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId(),OrderTypeEnum.ORDER_CDN_UPGRADE.getTypeId()};

            //获取最后一个套餐的ATTR
            /*
            CdnSuitEntity last_suit=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                    .eq("user_id",order.getUserId())
                    .in("suit_type",in_type_list)
                    .eq("serial_number",source_serialNumber)
                    .eq("status",CdnSuitStatusEnum.NORMAL.getId())
                    .le("end_time",end_date)
                    .last("limit 1")
            );
             */

            //更新批量时间前的状态为已升级
            UpdateWrapper<CdnSuitEntity> updateWrapper=new UpdateWrapper<>();
            updateWrapper.eq("serial_number",source_serialNumber)
                    .in("suit_type",in_type_list)
                    .le("end_time",end_date)
                    .set("status",CdnSuitStatusEnum.UPGRADE.getId());
            cdnSuitDao.update(null,updateWrapper);


            CdnSuitEntity up_suit =new CdnSuitEntity();
            up_suit.setSerialNumber(source_serialNumber);
            up_suit.setUserId(order.getUserId());
            up_suit.setPaySerialNumber(order.getSerialNumber());
            up_suit.setSuitType(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId());
            ProductAttrVo attrVo=new ProductAttrVo();
            ProductAttrNameEnum.getFinalAttrJson(attrVo,productAttrJsonArray);
            up_suit.setAttrJson(ProductAttrNameEnum.getFinalAttrJson(productAttrJsonArray).toJSONString());
            //复制部分属性
            ProductAttrVo.updateSuitAttrByAttrVo(up_suit,attrVo);
            up_suit.setStartTime( new java.sql.Date(new Date().getTime()) );
            up_suit.setEndTime(new java.sql.Date(end_date.getTime()));
            up_suit.setStatus(CdnSuitStatusEnum.NORMAL.getId());
            cdnSuitDao.insert(up_suit);

            //更新订单
            order.setStatus(PayStatusEnum.PAY_COMPLETE.getId());
            tbOrderDao.updateById(order);

            //推送新配置
            Map pushMap=new HashMap(8);
            pushMap.put(PushTypeEnum.PUSH_SUIT_SERVICE.getName(),source_serialNumber);
            cdnMakeFileService.pushByInputInfo(pushMap);
        }catch (Exception e){
            e.printStackTrace();
        }

        //更新支付记录
        this.updateRecord(record,PayRecordStatusEnum.STATUS_SUCCESS.getId());

    }


    private void operateRecordHandle(){
        logger.info("---------->operate_Record_Handle");
        StaticVariableUtils.inOperatePayRecord=true;
        try{
            List<TbPayRecordEntity> list=tbPayRecordDao.selectList(new QueryWrapper<TbPayRecordEntity>().eq("operate_status",0).last("limit 2"));
            if (null==list || list.isEmpty()){
                logger.info("无待处理订单");
                return;
            }
            for (TbPayRecordEntity record:list ){
                    logger.info( "record id:"+record.getId());
                    TbOrderEntity order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",record.getSerialNumber()).last("limit 1" ));
                    if(null==order){
                        //未知订单
                        logger.error("未知支付订单:"+DataTypeConversionUtil.entity2jonsStr(record));
                        record.setStatus(PayRecordStatusEnum.STATUS_UNKNOWN_ORDER.getId());
                        record.setOperateStatus(1);
                        tbPayRecordDao.updateById(record);
                        continue;
                    }
                    logger.info("order id:"+order.getId());
                    if(order.getPayable()>record.getPayPaid()){
                        //订单价格不符 支付金额小于订单金额
                        logger.error("订单价格不符:"+DataTypeConversionUtil.entity2jonsStr(record));
                        record.setStatus(PayRecordStatusEnum.STATUS_PRICE_UNEQUAL.getId());
                        record.setOperateStatus(1);
                        tbPayRecordDao.updateById(record);
                        continue;
                    }else if(record.getCreatetime().before( DateUtils.addDateHours(new Date(),-24))){
                        //订单超时24小时
                        logger.error("订单超时24小时:"+DataTypeConversionUtil.entity2jonsStr(record));
                        record.setStatus(PayRecordStatusEnum.STATUS_OUT_TIME.getId());
                        record.setOperateStatus(1);
                        tbPayRecordDao.updateById(record);
                        continue;
                    }
                    else if(order.getOrderType()== OrderTypeEnum.ORDER_AUTHENTICATION.getTypeId()){
                        //实名认证 类
                        logger.error("ORDER_AUTHENTICATION");
                        this.operate_authentication(order,record);
                        continue;
                    } else if(order.getOrderType()==OrderTypeEnum.ORDER_RECHARGE.getTypeId()){
                        //处理充值类
                        logger.info("ORDER_RECHARGE");
                        this.operate_recharge(order,record);
                        continue;
                    }
                    else if(order.getOrderType()==OrderTypeEnum.ORDER_CDN_SUIT.getTypeId()){
                        // 处理购买CDN套餐
                        logger.info("ORDER_CDN_SUIT");
                        this.operate_cdn_buy(order,record);
                        continue;
                    }
                    else if(order.getOrderType()==OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()){
                        // 处理购买CDN套餐续费
                        logger.info("ORDER_CDN_RENEW");
                        this.operate_cdn_renew(order,record);
                        continue;
                    }
                    else if(order.getOrderType()==OrderTypeEnum.ORDER_CDN_ADDED.getTypeId()){
                        // 处理购买CDN 增值/加油包
                        logger.info("ORDER_CDN_ADDED");
                        this.operate_cdn_adder(order,record);
                        continue;
                    }
                    else if(order.getOrderType()==OrderTypeEnum.ORDER_CDN_UPGRADE.getTypeId()){
                        //处理购买CDN升级
                        logger.info("ORDER_CDN_UPGRADE");
                        this.operate_cdn_upgrade(order,record);
                        continue;
                    }else{
                        logger.info("Unknown order type:"+order);
                        this.updateRecord(record,PayRecordStatusEnum.STATUS_ERROR.getId());
                    }
                }

        }catch (Exception e){
            e.printStackTrace();
        }
        StaticVariableUtils.inOperatePayRecord=false;
    }

    private static RandomNumberGenerator randomNumberGenerator = new SecureRandomNumberGenerator();


    @Override
    public void operateRecord() {
        if( StaticVariableUtils.inOperatePayRecord ){
            logger.info("inOperatePayRecord=true,线程执行中。。");
            return;
        }
        logger.info("---------->operate_Record_Handle-->");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    operateRecordHandle();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    StaticVariableUtils.inOperatePayRecord=false;
                }
            }
        }).start();
    }

    @Override
    public R getOrderStatus(Long useId, String SerialNumber) {
        //线程处理订单
        this.operateRecord();
        //返回订单信息
        TbOrderEntity order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number",SerialNumber)
                .eq(null!=useId,"user_id",useId)
                .last("limit 1" ));
        return R.ok().put("data",order);

    }

    private boolean userBalancePayPassword(String password,String ip){
        SysLoginKeyConfig apiloginconf=sysConfigService.getConfigObject(ConfigConstantEnum.SYS_APP_LOGIN.getConfKey(), SysLoginKeyConfig.class);
        //cdn22.tb_user.property_pay_password
         if (null==apiloginconf){
             return true;
         }

        if (StringUtils.isBlank(apiloginconf.getAppKey()) ){
            return true;
        }
        if (!apiloginconf.getAppKey().equals(password)){
            return true;
        }
        if (StringUtils.isNotBlank(apiloginconf.getWhiteIps())){
            String[] ips=apiloginconf.getWhiteIps().split(",");
            if (!Arrays.asList(ips).contains(ip)){
                 return true;
            }
        }
        return false;
    }

    /**
     * 余额支付
     */
    @Override
    public R orderPayBalance(PayBalanceForm form) {
        //|| null==form.getPassword()
        if(null==form.getSerialNumber() ){
            return R.error("参数不完整");
        }
        TbOrderEntity order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",form.getSerialNumber()).last("limit 1" ));
        if(null==order){
            return  R.error("订单不存在");
        }
        if(order.getStatus()!=PayStatusEnum.PAY_NOT_PAY.getId()){
            return  R.error("订单状态不可支付");
        }
        if(null==order.getUserId()){
            return R.error("订单所属用户为空");
        }
        if(null!=form.getUserId()){
            if(!form.getUserId().equals(order.getUserId()) ){
                return R.error(String.format("[%d][%d]订单所属用户异常",form.getUserId(),order.getUserId()));
            }
        }
        if(order.getPayable()<0){
            return R.error("订单金额不能小于0");
        }
        TbUserEntity user= tbUserDao.selectById(order.getUserId());
        if(null==user){
            return R.error("用户获取失败");
        }
        if (user.getPropertyBalance()<0){
            return R.error("余额不足[a]");
        }
        if(user.getPropertyBalance()<order.getPayable()){
            return R.error("余额不足[b]");
        }

        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip=IPUtils.getIpAddr(request);

        //        if (this.userBalancePayPassword(form.getPassword(),ip)){
        //            if(null==user.getPropertyPayPassword()){
        //                return R.error("支付密码未设置！");
        //            }
        //            if( !user.getPropertyPayPassword().equals(DigestUtils.sha256Hex(form.getPassword())) ){
        //                return R.error("密码错误");
        //            }
        //        }

        //扣出余额-
        user.setPropertyBalance(user.getPropertyBalance()-order.getPayable());
        tbUserDao.updateById(user);
        //增加支付成功记录
        TbPayRecordEntity record=new TbPayRecordEntity();
        record.setSerialNumber(form.getSerialNumber());
        record.setPayType(PayTypeEnum.PRO_TYPE_sys.getId());
        record.setPayId(String.valueOf(System.currentTimeMillis()));
        record.setPayPaid(order.getPayable());
        JSONObject json_object=new JSONObject();
        json_object.put("ip",ip);
        record.setPayMsg(json_object.toJSONString());
        record.setStatus(PayRecordStatusEnum.STATUS_UNKNOWN.getId());
        record.setOperateStatus(0);
        tbPayRecordDao.insert(record);
        //线程处理订单
        this.operateRecord();
        return R.ok().put("data",order);
    }


    @Override
    public R adminRecharge(Long useId, Integer amount,String remark) {
        TbUserEntity user= tbUserDao.selectById(useId);
        if(null==user){
            return R.error("用户不存在");
        }
        //user.setPropertyBalance(user.getPropertyBalance()+amount);
        // userDao.updateById(user);
        TbOrderEntity order=new TbOrderEntity();
        String serialNumber=System.currentTimeMillis()+"000"+StaticVariableUtils.createOrderIndex;
        order.setOrderType(OrderTypeEnum.ORDER_RECHARGE.getTypeId());
        order.setSerialNumber(serialNumber);
        order.setUserId(useId);
        order.setPayable(amount);
        order.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        order.setRemark(remark);
        order.setCreateTime(new Date());
        tbOrderDao.insert(order);
        TbPayRecordEntity record=new TbPayRecordEntity();
        record.setSerialNumber(serialNumber);
        record.setPayType(PayTypeEnum.PRO_TYPE_admin.getId());
        record.setPayPaid(amount);
        JSONObject json_object=new JSONObject();
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        json_object.put("ip",IPUtils.getIpAddr(request));
        record.setPayMsg(json_object.toJSONString());
        record.setStatus(PayRecordStatusEnum.STATUS_UNKNOWN.getId());
        record.setOperateStatus(0);
        tbPayRecordDao.insert(record);
        this.operateRecord();
        return R.ok().put("data",order);
    }


    //private static SecureRandom random = new SecureRandom();
    @Override
    public void payResultSub(String SerialNumber, Integer payPaid, String out_trade_id, Integer payType, String payMsg){
        try {
            //只记录
            TbPayRecordEntity payRecord =new TbPayRecordEntity();
            payRecord.setSerialNumber(SerialNumber);
            //payRecord.setPayType(PayTypeEnum.PRO_TYPE_alipay.getId());
            payRecord.setPayType(payType);
            //payRecord.setPayId(params.get("trade_no"));
            payRecord.setPayId(out_trade_id);
            //500*7.3
            payRecord.setPayPaid( payPaid);
            payRecord.setPayMsg(payMsg);
            payRecord.setOperateStatus(0);
            payRecord.setStatus(PayRecordStatusEnum.STATUS_UNKNOWN.getId());
            tbPayRecordDao.insert(payRecord);
            this.operateRecord();//更新记录
            logger.info("处理支付---完成");
        } catch (Exception e) {
            logger.error(payType+"回调业务处理报错,params:" + payMsg, e);
        }

    }

    @Override
    public String aliPay(AlipayBeanConfig alipayBean, String mode) {
        try{
            if("pc".equals(mode)){
                return new AlipayService().pcpay(alipayBean);
            }else if("h5".equals(mode)){
                return new AlipayService().h5pay(alipayBean);
            }else {
                return new AlipayService().pcpay(alipayBean);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String aliPayQuery(String out_trade_no)   {
        try{
            return new AlipayService().query(out_trade_no);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public String aliPayNoticeCallback(HttpServletRequest request){
        /**
         * 支付宝回调,{"gmt_create":"2021-12-06 17:26:52",
         * "charset":"utf-8",
         * "gmt_payment":"2021-12-06 17:27:00",
         * "notify_time":"2021-12-06 17:27:00",
         * "subject":"dns test",
         * "sign":"H1hk4q6RtQQpnQDEcP0sEgJxyus1iPl7mCnBG8C0Mk7aj9mBqbFwoQkVTWv1jY3PgceSsUVo93Sw6GVYVhN7AYe7GAqIiv1qJcxzDpkZXAFt4IF2GVLLc1JveHp12pWlogR165M4cL3DxokGTcKyxJB3+olkusXDddSyxMyswXDSD/9iyFvxxf4cs2L6h4WU5U2vSkUWfZ6xjE2PoymPky6lkYzG30E6o7wl+MVv0P8GnNu5msayGGG8IdG+MfSpv6KPVwmVX0G/iLIsej1IwjI4qVMEfymp/uuqVJGwl1/fiIsKPaHn68q77izQbQ33VAbwZcNvddrczv/FBTPkgw==",
         * "buyer_id":"2088002186446584",
         * "body":"alipay test pay",
         * "invoice_amount":"0.01",
         * "version":"1.0",
         * "notify_id":"2021120600222172700046581405990563",
         * "fund_bill_list":"[{\"amount\":\"0.01\",\"fundChannel\":\"PCREDIT\"}]",
         * "notify_type":"trade_status_sync",
         * "out_trade_no":"st1113121",
         * "total_amount":"0.01",   //	交易金额
         * "trade_status":"TRADE_SUCCESS",
         * "trade_no":"2021120622001446581425772663",
         * "auth_app_id":"2021002191683716",
         * "receipt_amount":"0.01",//实收金额
         * "point_amount":"0.00", //使用集分宝付款的金额
         * "buyer_pay_amount":"0.01", //买家付款的金额
         * "app_id":"2021002191683716",
         * "sign_type":"RSA2","
         * seller_id":"2088141384721814"}
         */
        try {
            AlipayConfig alipayConfig = new AlipayService().getAlipayConfig();
            //将异步通知中收到的待验证所有参数都存放到map中
            Map<String, String> params = alipayConvertRequestParamsToMap(request);
            String paramsJson = JSON.toJSONString(params);
            logger.info("支付宝回调,{}", paramsJson);
            String charset=params.get("charset");
            String sign_type=params.get("sign_type");
            // 调用SDK验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayConfig.getAlipayPublicKey(),  charset, sign_type);
            if (signVerified) {
                // 支付宝回调签名认证成功 按照支付结果异步通知中的描述,对支付结果中的业务内容进行1\2\3\4二次校验,校验成功后在response中返回success,校验失败返回failure
                this.alipayCheck(params);
                // 另起线程处理业务
                // threadPool.execute(   () -> {    });
                AlipayNotifyParam param = buildAlipayNotifyParam(params);
                String trade_status = param.getTradeStatus();
                // 支付成功
                if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                    // 处理支付成功逻辑
                    String order_id=params.get("out_trade_no");
                    Float f_paypaid=Float.parseFloat(params.get("total_amount"))*100.0f;
                    Integer paypaid= f_paypaid.intValue();
                    String out_trade_id=params.get("trade_no");
                    Integer payType=PayTypeEnum.PRO_TYPE_alipay.getId();
                    String payMsg=paramsJson;
                    this.payResultSub(order_id,paypaid,out_trade_id,payType,payMsg);
                } else {
                    logger.error("没有处理支付宝回调业务,支付宝交易状态：{},params:{}", trade_status, paramsJson);
                }
                this.writeLog(String.format("%s 支付成功","alipay"));
                return "success";
            } else {
                logger.info("支付宝回调签名认证失败,signVerified=false, paramsJson:{}", paramsJson);
                this.writeLog(String.format("%s 支付失败！支付宝回调签名认证失败","alipay"));
                return "FAIL";
            }
        } catch (AlipayApiException e) {
            //logger.error("支付宝回调签名认证失败 2,paramsJson:{},errorMsg:{}", paramsJson, e.getMessage());
            e.printStackTrace();
            this.writeLog(String.format("%s 支付失败！支付宝回调参数错误！","alipay"));
            return "FAIL";
        }
    }

    private String getPostBody(HttpServletRequest request) {
        try{
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = null;
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(request.getInputStream(), "UTF-8");
                bufferedReader = new BufferedReader(inputStreamReader);
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
            return stringBuilder.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";

    }


    @Override
    public String tokenPayCallback(HttpServletRequest request, HttpServletResponse response) {
        if (!request.getMethod().equalsIgnoreCase("post")){
            response.setStatus(403);
            return "";
        }
        TokenPayService tokenPayService= TokenPayFactory.build();
        //TokenPayConfig tokenPayConfig= tokenPayService.getConfig();
        String payload = this.getPostBody(request);
        if (StringUtils.isBlank(payload)){
            response.setStatus(403);
            return "";
        }
        JSONObject postData=DataTypeConversionUtil.string2Json(payload);
        if (!postData.containsKey("Signature")){
            response.setStatus(403);
            return "";
        }
        String sign=tokenPayService.getSignStr(postData);
        if (!sign.equalsIgnoreCase(postData.getString("Signature"))){
            logger.error(" tokenPayCallback sign error:"+payload);
        }else{
            TokenPayCallBackBodyVo vo=DataTypeConversionUtil.string2Entity(payload,TokenPayCallBackBodyVo.class);
            if (null!=vo){
                logger.info("订单："+payload);
                if (1==vo.getStatus()){
                    // 处理支付成功逻辑
                    String orderId=vo.getOutOrderId();
                    Integer payPaid=Integer.parseInt(vo.getActualAmount()) *100;
                    String outTradeId=vo.getId();
                    Integer payType= PayTypeEnum.PRO_TYPE_TOKENPAY.getId();
                    this.payResultSub(orderId,payPaid,outTradeId,payType,payload);
                    response.setStatus(200);
                    return "ok";
                }
            }
        }

        response.setStatus(403);
        return "";
    }

    @Override
    public String cccYunCallback(HttpServletRequest request, HttpServletResponse response) {
        if (!request.getMethod().equalsIgnoreCase("get")){
            response.setStatus(403);
            return "";
        }
        Map<String,Object> pMap=new HashMap<>();
        // 获取所有请求参数的 Map
        Map<String, String[]> parameterMap = request.getParameterMap();
        // 遍历参数 Map
        for (String paramName : parameterMap.keySet()) {
            String[] paramValues = parameterMap.get(paramName);
            if (null==paramValues){
                continue;
            }
            pMap.put(paramName,paramValues[0]);
        }
        //logger.info("订单："+pMap);
        CccYunNotifyForm form=DataTypeConversionUtil.map2entity(pMap,CccYunNotifyForm.class);
        logger.info("订单："+form);
        R r= CccyunPayFactory.build().parseCallBack(form);
        if (1==r.getCode()){
            // 处理支付成功逻辑
            if (form.getTrade_status().equalsIgnoreCase("TRADE_SUCCESS")){
                String orderId=form.getOut_trade_no();
                double decimal = Double.parseDouble(form.getMoney())*100;
                Integer payPaid= new Double(decimal).intValue();
                String outTradeId=orderId;
                Integer payType= PayTypeEnum.PRO_TYPE_CCCYUN.getId();
                this.payResultSub(orderId,payPaid,outTradeId,payType,DataTypeConversionUtil.entity2jonsStr(form));
            }
            response.setStatus(200);
            return "success";
        }
        return "fail:"+r.getMsg();
    }

    @Override
    public void allinpayCallback(HttpServletRequest request, HttpServletResponse response) {
        try {
            AllinpaySybPayService aService= AllinpayFactory.build();

            request.setCharacterEncoding("UTF-8");//通知传输的编码为GBK
            response.setCharacterEncoding("UTF-8");
            TreeMap<String,String> params = aService.getParams(request);//动态遍历获取所有收到的参数,此步非常关键,因为收银宝以后可能会加字段,动态获取可以兼容
            try {
                logger.info(params.toString());
                String appkey = "";
                if("RSA".equals(params.get("signtype"))){
                    appkey = aService.getConf().getSYB_RSATLPUBKEY();
                }
                else if("SM2".equals(params.get("signtype"))){
                    appkey = aService.getConf().getSYB_SM2TLPUBKEY();
                }
                else{
                    appkey = aService.getConf().getSYB_MD5_APPKEY();
                }
                boolean isSign = SybUtil.validSign(params, appkey, params.get("signtype"));// 接受到推送通知,首先验签
                logger.info("验签结果:"+isSign);
                //todo 验签完毕进行业务处理
                if (isSign){
                   if (params.containsKey("cusorderid") && params.containsKey("trxamt") && params.containsKey("trxid") ) {
                       String sn=params.get("cusorderid");
                       Long  amount=Long.parseLong(params.get("trxamt"));
                       String outTradeId=params.get("trxid");
                       Integer payType= PayTypeEnum.PRO_TYPE_ALLINPAY.getId();
                       this.payResultSub(sn,amount.intValue(),outTradeId,payType,DataTypeConversionUtil.map2json(params).toJSONString());
                   }
                }
            } catch (Exception e) {//处理异常
                // TODO: handle exception
                e.printStackTrace();
            }
            finally{
                //收到通知,返回success
                response.getOutputStream().write("success".getBytes());
                response.flushBuffer();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private static Map<String, String> alipayConvertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> retMap = new HashMap<String, String>();

        Set<Map.Entry<String, String[]>> entrySet = request.getParameterMap().entrySet();

        for (Map.Entry<String, String[]> entry : entrySet) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            int valLen = values.length;

            if (valLen == 1) {
                retMap.put(name, values[0]);
            } else if (valLen > 1) {
                StringBuilder sb = new StringBuilder();
                for (String val : values) {
                    sb.append(",").append(val);
                }
                retMap.put(name, sb.toString().substring(1));
            } else {
                retMap.put(name, "");
            }
        }

        return retMap;
    }

    private AlipayNotifyParam buildAlipayNotifyParam(Map<String, String> params) {
        String json = JSON.toJSONString(params);
        return JSON.parseObject(json, AlipayNotifyParam.class);
    }

    private void alipayCheck(Map<String, String> params) throws AlipayApiException {
        try{
            AlipayConfig alipayConfig = new AlipayService().getAlipayConfig();
            String outTradeNo = params.get("out_trade_no");

            // 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号,
            //logger.info("outTradeNo:"+outTradeNo);
            //        TransFlowEntity order =transflowservice.getOrderByOutTradeNo(outTradeNo);
            //        if (order == null) {
            //            throw new AlipayApiException("out_trade_no错误");
            //        }

            // 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额）,
            //        long total_amount = new BigDecimal(params.get("total_amount")).multiply(new BigDecimal(100)).longValue();
            //        if (total_amount != order.getAmount().multiply(new BigDecimal(100)).longValue()) {
            //            throw new AlipayApiException("error RSAcontent ,["+total_amount +"]-->["+order.getAmount().toString()+"]");
            //        }

            // 3、校验通知中的seller_id（或者seller_email)是否为out_trade_no这笔单据的对应的操作方（有的时候,一个商户可能有多个seller_id/seller_email）,
            // 第三步可根据实际情况省略

            // 4、验证app_id是否为该商户本身。
            //            if (!params.get("app_id").equals(alipayConfig.getAppId())) {
            //                throw new AlipayApiException("app_id不一致");
            //            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private String GetWxpay(HttpServletRequest request, String outTradeNo, String totalAmount, String body,String trade_type){
        try{
            //String code_url="";
            // ConfigUtil config = new ConfigUtil();
            ConfigUtil config = WXPayFactory.build();
            if (null==config){
                logger.error("wxpay config is null");
                return null;
            }
            WXPayService wxpay = new WXPayService(config);
            Map<String, String> data = new HashMap<String, String>();
            data.put("body", body);
            data.put("out_trade_no", outTradeNo);
            data.put("device_info", "");
            data.put("fee_type", "CNY");
            data.put("total_fee", totalAmount);
            data.put("spbill_create_ip", CommonUtil.getIp(request));
            data.put("notify_url",config.getNotifyUrl());
            // 此处指定为扫码支付
            //data.put("trade_type", "NATIVE");
            data.put("trade_type", trade_type);
            data.put("product_id", "12");
            Map<String, String> resp = wxpay.unifiedOrder(data);
            logger.info(resp.toString());
            if(resp.containsKey("code_url")){
                return  resp.get("code_url").toString();
            }else if(resp.containsKey("mweb_url")){
                return resp.get("mweb_url").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public  String wxPayNativePay(HttpServletRequest request, String outTradeNo, String totalAmount, String body){
        return this.GetWxpay(request,outTradeNo,totalAmount,body,"NATIVE");
    }

    @Override
    public String wxPayMWebPay(HttpServletRequest request, String outTradeNo, String totalAmount, String body) {
        return this.GetWxpay(request,outTradeNo,totalAmount,body,"MWEB");
    }


    @Override
    public  String wxPayCallback(HttpServletRequest request, HttpServletResponse response) {
        try{
            Map<String,String> return_data = new HashMap<>();
            //读取参数
            InputStream inputStream ;
            StringBuffer sb = new StringBuffer();
            inputStream = request.getInputStream();
            String s ;
            BufferedReader inBR = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while ((s = inBR.readLine()) != null){
                sb.append(s);
            }
            inBR.close();
            inputStream.close();

            //解析xml成map
            Map<String, String> map = WXPayUtil.xmlToMap(sb.toString());
            //判断签名是否正确\
            ConfigUtil config = WXPayFactory.build();
            if(WXPayUtil.isSignatureValid(map, config.getKey(), WXPayConstants.SignType.HMACSHA256)) {
                if(!"SUCCESS".equals(map.get("return_code"))){
                    return_data.put("return_code", "FAIL");
                    return_data.put("return_msg", "return_code不正确");
                    logger.error("return_code不正确");
                }else{
                    if(!"SUCCESS".equals(map.get("result_code"))){
                        return_data.put("return_code", "FAIL");
                        return_data.put("return_msg", "result_code不正确");
                        logger.error("return_code不正确");
                        return WXPayUtil.mapToXml(return_data);
                    }else{
                        //商户订单号
                        String orderno = map.get("out_trade_no");
                        //微信支付订单号
                        String transaction_id = map.get("transaction_id");
                        //支付完成时间yyyyMMddHHmmss
                        //String time_end = map.get("time_end");
                        Integer total_fee = new BigDecimal(map.get("total_fee")).intValue();
                        //付款完成后,支付宝系统发送该交易状态通知

                        //TODO:
                        //如果支付金额不等于订单金额返回错误
                        String order_id=orderno;
                        Integer paypaid= total_fee;
                        String out_trade_id=transaction_id;
                        Integer payType=PayTypeEnum.PRO_TYPE_wechat.getId();
                        String payMsg=map.toString();
                        this.payResultSub(order_id,paypaid,out_trade_id,payType,payMsg);

                        //更新订单信息
                        logger.info("wx pay success:"+map.toString());
                        return_data.put("return_code", "SUCCESS");
                        return_data.put("return_msg", "OK");
                        this.writeLog(String.format("%s 支付成功！","wechat"));
                        return WXPayUtil.mapToXml(return_data);
                    }

                }
            } else{
                return_data.put("return_code", "FAIL");
                return_data.put("return_msg", "签名错误");
                logger.error("通知签名验证失败"+map.toString()+","+config.getKey());
                this.writeLog(String.format("%s 支付失败！签名错误！","wechat"));
            }
            return WXPayUtil.mapToXml(return_data);
        }  catch (Exception e) {
            this.writeLog(String.format("%s 支付失败！参数错误！","wechat"));
            e.printStackTrace();
            //return_data.put("return_code", "FAIL");
            // return_data.put("return_msg", "更新订单失败");
            // logger.error("wx pay Exception:"+map.toString());
            //return WXPayUtil.mapToXml(return_data);
        }
        return null;
    }

    @Override
    public  Map<String,String>  fuiouPay(String ip,String mchnt_order_no,String order_amt,String goods_des,String order_type)   {
        try{
            FuiouService fuiouService = FuiouFactory.build();
            FuiouConfig fuiouConfig = fuiouService.getFuiouConfig();
            Map<String, String> reqs = new HashMap<>();
            reqs.put("version", "1.0");
            reqs.put("ins_cd", fuiouConfig.ins_cd);
            reqs.put("mchnt_cd", fuiouConfig.mchnt_cd);
            reqs.put("term_id", fuiouConfig.term_id);
            reqs.put("random_str", randomNumberGenerator.nextBytes().toHex());
            reqs.put("sign", "");
            reqs.put("order_type", order_type);
            reqs.put("goods_des", goods_des);
            reqs.put("goods_detail", "");
            reqs.put("addn_inf", "");
            Calendar calendar = Calendar.getInstance();
            //SimpleDateFormat sdf_no = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            //reqs.put("mchnt_order_no", Const.order_prefix+sdf_no.format(calendar.getTime()) + (int)(random.nextDouble() * 100000));
            reqs.put("mchnt_order_no", fuiouConfig.order_prefix+mchnt_order_no);
            reqs.put("curr_type", "");
            reqs.put("order_amt",order_amt);
            reqs.put("term_ip", ip);
            SimpleDateFormat sdf_ts = new SimpleDateFormat("yyyyMMddHHmmss");
            reqs.put("txn_begin_ts", sdf_ts.format(calendar.getTime()));
            reqs.put("goods_tag", "");
            reqs.put("notify_url", fuiouConfig.notify_url);
            reqs.put("reserved_sub_appid", "");
            reqs.put("reserved_limit_pay", "");

            Map<String, String> nvs = new HashMap<>();


            String sign = Utils.getSign(reqs);
            reqs.put("sign", sign);


            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("xml");

            Iterator it=reqs.keySet().iterator();
            while(it.hasNext()){
                String key = it.next().toString();
                String value = reqs.get(key);

                root.addElement(key).addText(value);
            }

            // String reqBody = doc.getRootElement().asXML();
            String reqBody = "<?xml version=\"1.0\" encoding=\"GBK\" standalone=\"yes\"?>" + doc.getRootElement().asXML();

            //System.out.println("==============================待编码字符串==============================\r\n" + reqBody);

            reqBody = URLEncoder.encode(reqBody, FuiouConfig.charset);

            //System.out.println("==============================编码后字符串==============================\r\n" + reqBody);

            nvs.put("req", reqBody);

            StringBuffer result = new StringBuffer("");

            HttpUtils httpUtils = new HttpUtils();
            httpUtils.post(FuiouConfig.fuiou_21_url, nvs, result);
            String rspXml = URLDecoder.decode(result.toString(),FuiouConfig.charset);
            //System.out.println("==============================响应报文==============================\r\n"+rspXml);
            //响应报文验签
            Map<String,String> resMap = Utils.xmlStr2Map(rspXml);
            //String str = resMap.get("sign");
            //System.out.println("sign :"+str);
            //System.out.println("验签结果："+Utils.verifySign(resMap, str));
            return resMap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return  null;

    }


    private   byte[] getRequestPostBytes(HttpServletRequest request)
            throws IOException {
        int contentLength = request.getContentLength();
        if(contentLength<0){
            return null;
        }
        byte[] buffer = new byte[contentLength];
        for (int i = 0; i < contentLength;) {

            int readlen = request.getInputStream().read(buffer, i,
                    contentLength - i);
            if (readlen == -1) {
                break;
            }
            i += readlen;
        }
        return buffer;
    }

    private  String getRequestPostStr(HttpServletRequest request)       {
        try{
            byte[] buffer = getRequestPostBytes(request);
            String charEncoding = request.getCharacterEncoding();
            if (charEncoding == null) {
                charEncoding = "GBK";
            }
            return new String(buffer, charEncoding);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public  void fuiouPayCallBack(HttpServletRequest request,String req)  {
        //noinspection deprecation
        //getRequestPostStr(request)
        try{
            final  String F="req=";
            //String reqstr=req.replace("req=","");
            String reqstr=req.substring(F.length());
            logger.info(reqstr);
            String xml_res= URLDecoder.decode(reqstr,FuiouConfig.charset);
            logger.info(xml_res);
            Map<String,String> resMap = Utils.xmlStr2Map(xml_res);
            String str = resMap.get("sign");
            System.out.println("sign :"+str);
            System.out.println("验签结果："+Utils.verifySign(resMap, str));
            if(Utils.verifySign(resMap, str)){
                //success
                //todo:
                logger.info("pay success");
            }
            return;
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    @Override
    public Map<String,String> fuiouPayQuery(String order_no, String order_type)  {
        try{
            FuiouService fuiouService = FuiouFactory.build();
            FuiouConfig fuiouConfig = fuiouService.getFuiouConfig();
            logger.info("============================"+fuiouConfig);
            Map<String, String> reqs = new HashMap<>();
            reqs.put("version", "1.0");
            reqs.put("ins_cd", fuiouConfig.ins_cd);
            reqs.put("mchnt_cd", fuiouConfig.mchnt_cd);
            reqs.put("term_id", fuiouConfig.term_id);
            reqs.put("order_type", order_type);
            reqs.put("mchnt_order_no", order_no);
            reqs.put("random_str", randomNumberGenerator.nextBytes().toHex());
            reqs.put("sign", "");
            String sign = Utils.getSign(reqs);
            reqs.put("sign", sign);

            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("xml");

            Iterator it=reqs.keySet().iterator();
            while(it.hasNext()){
                String key = it.next().toString();
                String value = reqs.get(key);

                root.addElement(key).addText(value);
            }

            // String reqBody = doc.getRootElement().asXML();
            String reqBody = "<?xml version=\"1.0\" encoding=\"GBK\" standalone=\"yes\"?>" + doc.getRootElement().asXML();


            reqBody = URLEncoder.encode(reqBody, FuiouConfig.charset);

            Map<String, String> nvs = new HashMap<>();
            nvs.put("req", reqBody);

            StringBuffer result = new StringBuffer("");

            HttpUtils httpUtils = new HttpUtils();
            httpUtils.post(FuiouConfig.fuiou_30_url, nvs, result);
            String rspXml = URLDecoder.decode(result.toString(),FuiouConfig.charset);
            //System.out.println("==============================响应报文==============================\r\n"+rspXml);
            //响应报文验签
            Map<String,String> resMap = Utils.xmlStr2Map(rspXml);
            //String str = resMap.get("sign");
            //System.out.println("sign :"+str);
            //System.out.println("验签结果："+Utils.verifySign(resMap, str));
            return resMap;
        }catch (Exception e){
            e.printStackTrace();
        }
       return  null;
    }


}
