package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.TbOrderDao;
import io.ants.modules.app.dao.TbSiteDao;
import io.ants.modules.app.dao.TbStreamProxyDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbOrderEntity;
import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.app.entity.TbStreamProxyEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.*;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.*;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.QueryCdnProductForm;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.CdnSuitService;
import io.ants.modules.sys.service.CommonTaskService;
import io.ants.modules.sys.vo.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CdnSuitServiceImpl implements CdnSuitService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CdnProductDao cdnProductDao;
    @Autowired
    private CdnProductAttrDao cdnProductAttrDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private CdnSuitDao suitDao;
    @Autowired
    private TbSiteDao siteDao;
    @Autowired
    private TbPayRecordDao recordDao;
    @Autowired
    private TbUserDao userDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private CommonTaskService commonTaskService;
    @Autowired
    private TbPayRecordDao tbPayRecordDao;
    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private CdnClientAreaDao cdnClientAreaDao;
    @Autowired
    private CdnConsumeDao cdnConsumeDao;

    private final Integer S_MODE_IN_USE = 1;
    private final Integer S_MODE_NOT_USE = 2;
    private final Integer S_MODE_TIME_OUT = 3;
    private final Integer S_MODE_INVALID = 4;
    private final double ONE_THOUSAND = 1000.00;

    private void updateProductAttr(CdnProductEntity item) {
        Map attrMap = new HashMap();
        JSONArray attrArray = DataTypeConversionUtil.string2JsonArray(item.getAttrJson());
        for (int i = 0; i < attrArray.size(); i++) {
            JSONObject attrObj = attrArray.getJSONObject(i);
            if (attrObj.containsKey("name") && attrObj.containsKey("value")) {
                String name = attrObj.getString("name");
                ProductAttrNameEnum objEnum = ProductAttrNameEnum.getEnum(name);
                if (null == objEnum) {
                    logger.error("name:[" + name + "] is error !");
                    continue;
                }
                attrMap.put(objEnum.getAttr(), attrObj.getString("value"));
                // item.setAttrJson(item.getAttrJson().replace("\""+name+"\"","\""+chName+"\""));

            }
        }
        item.setAttr(attrMap);
    }

    /**
     * 更新产品AREA info
     * 
     * @param item
     */
    private void updateProductAreaInfo(CdnProductEntity item) {
        if (null != item.getServerGroupIds()) {
            List<CdnClientGroupEntity> groupList = new ArrayList<>();
            String[] ids = item.getServerGroupIds().split(",");
            for (String id : ids) {
                CdnClientGroupEntity group = cdnClientGroupDao.selectById(id);
                if (null != group && null != group.getAreaId()) {
                    if (0 == group.getAreaId()) {
                        group.setAreaName("默认");
                    } else {
                        CdnClientAreaEntity areaEntity = cdnClientAreaDao.selectById(group.getAreaId());
                        if (null != areaEntity) {
                            group.setAreaName(areaEntity.getName());
                        }
                    }
                }
                groupList.add(group);
            }
            item.setClient_group_list(groupList);
        }
    }

    @Override
    public PageUtils getProductList(QueryCdnProductForm form) {
        String key = form.getKey();
        String productTypes = form.getProductTypes();
        String vendibility = form.getVendibility();

        IPage<CdnProductEntity> page = cdnProductDao.selectPage(
                new Page<>(form.getPage(), form.getLimit()),
                new QueryWrapper<CdnProductEntity>()
                        .orderByDesc("weight")
                        .ne("is_delete", 1)
                        .ne(StringUtils.isNotBlank(vendibility), "status", ProductStatusEnum.DISABLE.getId())
                        .ne(StringUtils.isNotBlank(vendibility), "status", ProductStatusEnum.ONLY_RENEW.getId())
                        .ne(null != form.getUserType() && UserTypeEnum.USER_TYPE.getId().equals(form.getUserType()),
                                "status", ProductStatusEnum.ONLY_FIRST.getId())
                        .like(StringUtils.isNotBlank(key), "name", key)
                        .in(StringUtils.isNotBlank(productTypes), "product_type", productTypes.split(","))

        );
        page.getRecords().forEach(item -> {
            updateProductAttr(item);
            updateProductAreaInfo(item);

        });
        return new PageUtils(page);
    }

    @Override
    public List<CdnProductEntity> getAllProductByType(String productTypes) {
        Integer[] statusLs = { ProductStatusEnum.ENABLE.getId(), ProductStatusEnum.ONLY_BUY.getId() };
        List<CdnProductEntity> list = cdnProductDao.selectList(new QueryWrapper<CdnProductEntity>()
                .ne("is_delete", 1)
                .orderByDesc("weight")
                .in("status", statusLs)
                .in("product_type", productTypes.split(",")));
        list.forEach(item -> {
            updateProductAttr(item);
        });
        return list;
    }

    @Override
    public CdnProductEntity saveProduct(Map<String, Object> params) {
        // ObjectMapper objectMapper = new ObjectMapper();
        // CdnProductEntity productEntity= objectMapper.convertValue(params,
        // io.cdn.modules.sys.entity.CdnProductEntity.class);
        // JSONObject jsonObject=DataTypeConversionUtil.map2json(params);
        // ProductAttrArrayVo attrArrayVo=
        try {
            JSONArray jsonArray = JSONArray.parseArray(params.get("attrJson").toString());
            JSONObject jsonObject = JSONObject.parseObject(params.get("productJson").toString());
        } catch (Exception e) {
            throw new RRException(e.getMessage());
        }
        CdnProductEntity productEntity = DataTypeConversionUtil.map2entity(params, CdnProductEntity.class);
        if (productEntity.getProductType().equals(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())) {
            // CDN 套餐 业务分组与DNS API 检测
            if (null == productEntity.getServerGroupIds()) {
                throw new RRException("ServerGroupIds is null");
            }
            Integer dnsConfId = null;
            for (String gid : productEntity.getServerGroupIds().split(",")) {
                CdnClientGroupEntity groupEntity = cdnClientGroupDao.selectById(gid);
                if (null == groupEntity) {
                    throw new RRException("分组[" + gid + "]不存在");
                }
                if (null == dnsConfId) {
                    dnsConfId = groupEntity.getDnsConfigId();
                } else {
                    if (!dnsConfId.equals(groupEntity.getDnsConfigId())) {
                        throw new RRException("同一产品对应的节点业务分组DNS需一致！分组ID:[" + gid + "]的DNS接口Id须是[" + dnsConfId
                                + "]！当前的分组ID:[" + gid + "]的DNS接口Id是[" + groupEntity.getDnsConfigId() + "]");
                    }
                }
            }
        }

        // 20230902 注册赠送添加默认价格属性
        if (productEntity.getProductType().equals(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                && productEntity.getStatus().equals(ProductStatusEnum.ONLY_FIRST.getId())) {
            OrderCdnProductVo productVo = DataTypeConversionUtil.string2Entity(params.get("productJson").toString(),
                    OrderCdnProductVo.class);
            if (null == productVo) {
                productVo = new OrderCdnProductVo();

                productVo.getV().setValue(12);
                productVo.getV().setStatus(1);
            }
            productVo.getY().setValue(0);
            productVo.getY().setStatus(1);
            productVo.getS().setValue(0);
            productVo.getS().setStatus(1);
            productVo.getM().setValue(0);
            productVo.getM().setStatus(1);

            productEntity.setProductJson(DataTypeConversionUtil.entity2jonsStr(productVo));
        }

        if (null == productEntity.getId() || 0 == productEntity.getId()) {
            // 新增
            productEntity.setCreatetime(new Date());
            cdnProductDao.insert(productEntity);
        } else {
            // 修改
            cdnProductDao.updateById(productEntity);
        }
        return productEntity;

    }

    @Override
    public boolean deleteProduct(Integer id) {
        cdnProductDao.deleteById(id);
        return true;
    }

    @Override
    public PageUtils getProductAttrList(PageSimpleForm form) {

        IPage<CdnProductAttrEntity> page = cdnProductAttrDao.selectPage(
                new Page<>(form.getPage(), form.getLimit()),
                new QueryWrapper<CdnProductAttrEntity>()
                        .orderByDesc("weight")
                        .like(StringUtils.isNotBlank(form.getKey()), "name", form.getKey())

        );
        page.getRecords().forEach(item -> {
            String name = item.getName();
            ProductAttrNameEnum obj = ProductAttrNameEnum.getEnum(name);
            if (null != obj) {
                item.setName(obj.getName());
                item.setStandard(ProductAttrNameEnum.getInfo(obj));
                item.setAttr(obj.getAttr());
            }

        });
        return new PageUtils(page);
    }

    @Override
    public Map getProductAttrObj() {
        Map resultMap = ProductAttrNameEnum.buildProductAttr();
        Map allAttrMap = (Map) resultMap.get("productAttrObj");
        for (Object key : allAttrMap.keySet()) {
            Map attrItemMap = (Map) allAttrMap.get(key);
            attrItemMap.put("weight", 0);
            String attr = key.toString();
            ProductAttrNameEnum objEnum = ProductAttrNameEnum.getEnum(attr);
            if (null == objEnum) {
                continue;
            }
            String[] names = { objEnum.getAttr(), objEnum.getName() };
            CdnProductAttrEntity attrEntity = cdnProductAttrDao.selectOne(new QueryWrapper<CdnProductAttrEntity>()
                    .in("name", names)
                    .eq("status", 1)
                    .last("limit 1"));
            if (null == attrEntity) {
                continue;
            }
            attrItemMap.put("weight", attrEntity.getWeight());
            allAttrMap.put(key, attrItemMap);
        }
        resultMap.put("productAttrObj", allAttrMap);
        return resultMap;
    }

    @Override
    public Object getAllProductAttr() {
        List<CdnProductAttrEntity> list = cdnProductAttrDao.selectList(new QueryWrapper<CdnProductAttrEntity>()
                .ne("is_delete", 1)
                .orderByDesc("weight")
                .eq("status", 1));
        list.forEach(item -> {
            ProductAttrNameEnum obj = ProductAttrNameEnum.getEnum(item.getName());
            if (null != obj) {
                item.setName(obj.getName());
                item.setStandard(ProductAttrNameEnum.getInfo(obj));
                item.setAttr(obj.getAttr());
            }
        });
        return list;
    }

    @Override
    public CdnProductAttrEntity SaveProductAttr(Map<String, Object> params) {
        // ObjectMapper objectMapper = new ObjectMapper();
        // CdnProductAttrEntity productAttrEntity= objectMapper.convertValue(params,
        // io.cdn.modules.sys.entity.CdnProductAttrEntity.class);
        JSONObject jsonObject = DataTypeConversionUtil.map2json(params);
        CdnProductAttrEntity productAttrEntity = DataTypeConversionUtil.json2entity(jsonObject,
                CdnProductAttrEntity.class);
        if (null != productAttrEntity.getId() && 0 != productAttrEntity.getId()) {
            CdnProductAttrEntity t_product_attr = cdnProductAttrDao.selectById(productAttrEntity.getId());
            if (null != t_product_attr) {
                // 修改后返回
                Long count = cdnProductAttrDao.selectCount(new QueryWrapper<CdnProductAttrEntity>()
                        .ne("id", t_product_attr.getId()).eq("name", t_product_attr.getName()).eq("is_delete", 0));
                if (0 != count) {
                    throw new RRException("id[" + productAttrEntity.getId() + "]已存在");
                } else {
                    ProductAttrNameEnum obj = ProductAttrNameEnum.getEnum(productAttrEntity.getName());
                    if (null != obj) {
                        productAttrEntity.setName(obj.getAttr());
                    }
                    cdnProductAttrDao.updateById(productAttrEntity);
                    return productAttrEntity;
                }
            }

        }
        // 增加
        Long count = cdnProductAttrDao.selectCount(
                new QueryWrapper<CdnProductAttrEntity>().eq("name", productAttrEntity.getName()).eq("is_delete", 0));
        if (0 == count) {
            ProductAttrNameEnum obj = ProductAttrNameEnum.getEnum(productAttrEntity.getName());
            if (null != obj) {
                productAttrEntity.setName(obj.getAttr());
            }
            cdnProductAttrDao.insert(productAttrEntity);
            return productAttrEntity;
        } else {
            throw new RRException("name[" + productAttrEntity.getName() + "]已存在");
        }
    }

    @Override
    public boolean deleteProductAttr(Integer id) {
        cdnProductAttrDao.deleteById(id);
        return true;
    }

    private Date getStartDate(Date entTime, String type) {
        if ("y".equals(type)) {
            return DateUtils.addDateYears(entTime, -1);
        } else if ("s".equals(type)) {
            return DateUtils.addDateMonths(entTime, -3);
        } else if ("m".equals(type)) {
            return DateUtils.addDateMonths(entTime, -1);
        } else if ("d".equals(type)) {
            return DateUtils.addDateDays(entTime, -1);
        }
        return null;
    }

    /*
     * 创建实名订单
     */
    private TbOrderEntity create_auth_order(SubmitOrderForm submitOrderForm) {
        String sid = System.currentTimeMillis() + "00" + StaticVariableUtils.createOrderIndex;
        TbOrderEntity tbOrderEntity = new TbOrderEntity();
        tbOrderEntity.setSerialNumber(sid);
        tbOrderEntity.setUserId(submitOrderForm.getUserId());
        tbOrderEntity.setOrderType(submitOrderForm.getOrderType());
        tbOrderEntity.setTargetId(submitOrderForm.getTargetId());
        // 执行计算 INIT 数据
        // todo 执行计算实名 INIT 数据47
        // tbOrderEntity.setInitJson();
        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        tbOrderEntity.setCreateTime(new Date());
        // todo 执行计算实名价格FUNCTION
        tbOrderDao.insert(tbOrderEntity);
        return tbOrderEntity;
    }

    /*
     * 创建充值订单
     */
    private R create_recharge_order(SubmitOrderForm submitOrderForm) {
        String eMsg = "";
        String sid = System.currentTimeMillis() + "00" + StaticVariableUtils.createOrderIndex;
        TbOrderEntity tbOrderEntity = new TbOrderEntity();
        tbOrderEntity.setSerialNumber(sid);
        tbOrderEntity.setUserId(submitOrderForm.getUserId());
        tbOrderEntity.setOrderType(submitOrderForm.getOrderType());
        // tbOrderEntity.setTargetId(submitOrderForm.getTargetId());
        // 执行计算 INIT 数据
        JSONObject resultJson = new JSONObject();
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        resultJson.put("ip", IPUtils.getIpAddr(request));
        tbOrderEntity.setInitJson(resultJson.toJSONString());
        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        JSONObject jsonObjectInit = JSONObject.parseObject(submitOrderForm.getInitJson());
        if (!jsonObjectInit.containsKey("sum")) {
            eMsg = ("sum is empty");
            return R.error(eMsg);
        }
        if (jsonObjectInit.getInteger("sum") < 1) {
            eMsg = ("sum need ge 1");
            return R.error(eMsg);
        }
        tbOrderEntity.setPayable(jsonObjectInit.getInteger("sum"));
        tbOrderEntity.setCreateTime(new Date());
        tbOrderDao.insert(tbOrderEntity);
        // return tbOrderEntity;
        return R.ok().put("data", tbOrderEntity);
    }

    /**
     * 获取资费类型
     * 
     * @param attrJSONarray
     * @return
     */
    private Integer getChargingMode(JSONArray attrJSONarray) {
        // [{"attr":"charging_mode","name":"charging_mode","value":3,"valueType":"select"},
        // {"attr":"flow","name":"flow","valueType":"int","unit":"G","value":10000},
        // {"attr":"bandwidth","name":"bandwidth","valueType":"price_int","unit":"元/Mbps/月","value":2000},
        // {"attr":"ai_waf","id":27,"name":"AI WAF","valueType":"bool","value":"1"},
        // {"attr":"port_forwarding","id":36,"name":"端口转发","valueType":"int","unit":"个","value":5},
        // {"attr":"site","id":35,"name":"站点","valueType":"int","unit":"个","value":10},
        // {"attr":"sms","id":34,"name":"短信通知","valueType":"int","unit":"条/月","value":100},
        // {"attr":"monitor","id":33,"name":"流量监控","valueType":"bool","value":"1"},
        // {"attr":"private_waf","id":32,"name":"专属WAF","valueType":"bool","value":"1"},
        // {"attr":"live_data","id":30,"name":"实时数据","valueType":"bool","value":"1"},
        // {"attr":"public_waf","id":37,"name":"WAF","valueType":"bool","value":"1"}]
        for (int i = 0; i < attrJSONarray.size(); i++) {
            JSONObject attr = attrJSONarray.getJSONObject(i);
            if (attr.containsKey("attr") && attr.containsKey("value")
                    && "charging_mode".equals(attr.getString("attr"))) {
                return attr.getInteger("value");
            }
        }
        return 1;
    }

    /**
     * 前致付费
     * 
     * @return
     */
    private R buy_pre_payment(OrderCdnNewInitVo initVo, CdnProductEntity product, TbOrderEntity tbOrderEntity,
            SubmitOrderForm submitOrderForm) {
        String eMsg = "";
        String type = initVo.getType();
        // {"m":{"value":1,"status":1},"s":{"value":100,"status":1},"y":{"value":1000,"status":1}}
        JSONObject AllPriceJson = DataTypeConversionUtil.string2Json(product.getProductJson());
        if (null == AllPriceJson || !AllPriceJson.containsKey(type)) {
            eMsg = (" type  is not in product");
            return R.error(eMsg);
        }
        // {"m":{"value":1,"status":0},"s":{"value":10100,"status":0},"y":{"value":20100,"status":1}}
        JSONObject t_type_json = AllPriceJson.getJSONObject(type);
        if (!t_type_json.containsKey("status")) {
            eMsg = (" status is empty");
            return R.error(eMsg);
        }
        if (!t_type_json.containsKey("value")) {
            eMsg = (" value is empty");
            return R.error(eMsg);
        }
        if (1 != t_type_json.getInteger("status")) {
            eMsg = (" status is 0");
            return R.error(eMsg);
        }
        TbOrderInitVo createOrderinitJsonVo = new TbOrderInitVo();
        createOrderinitJsonVo.setPrice_obj(AllPriceJson.getJSONObject(type));
        createOrderinitJsonVo.setBuy_obj(DataTypeConversionUtil.string2Json(submitOrderForm.getInitJson()));
        createOrderinitJsonVo.setProduct_obj(DataTypeConversionUtil.entity2jsonObj(product));
        tbOrderEntity.setInitJson(DataTypeConversionUtil.entity2jonsStr(createOrderinitJsonVo));

        // JSONObject initJson=new JSONObject();
        // initJson.put("price_obj",AllPriceJson.getJSONObject(type).toJSONString());
        // initJson.put("buy_obj",submitOrderForm.getInitJson());
        // initJson.put("product_obj",DataTypeConversionUtil.entity2jonsStr(product));
        // tbOrderEntity.setInitJson(initJson.toJSONString());

        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        // 购买套餐
        // {"m":{"value":1,"status":1},"s":{"value":100,"status":1},"y":{"value":1000,"status":1}}

        // {"type":"m","sum":2}
        Integer sum = initVo.getSum();
        Integer unitPrice = t_type_json.getInteger("value");
        Integer payAble = sum * unitPrice;
        if (payAble < 0) {
            eMsg = (" price is le 0");
            return R.error(eMsg);
        }
        tbOrderEntity.setPayable(payAble);
        tbOrderEntity.setCreateTime(new Date());
        tbOrderDao.insert(tbOrderEntity);
        // return tbOrderEntity;
        return R.ok().put("data", tbOrderEntity);
    }

    /**
     * 后致付费
     * 
     * @return
     */
    private R buy_postpaid(CdnProductEntity product, TbOrderEntity tbOrderEntity, SubmitOrderForm submitOrderForm) {
        String eMsg = "";
        TbOrderInitVo createOrderinitJsonVo = new TbOrderInitVo();
        JSONObject priceObject = new JSONObject();
        createOrderinitJsonVo.setPrice_obj(priceObject);
        createOrderinitJsonVo.setBuy_obj(DataTypeConversionUtil.string2Json(submitOrderForm.getInitJson()));
        createOrderinitJsonVo.setProduct_obj(DataTypeConversionUtil.entity2jsonObj(product));
        tbOrderEntity.setInitJson(DataTypeConversionUtil.entity2jonsStr(createOrderinitJsonVo));

        // JSONObject initJson=new JSONObject();
        // initJson.put("price_obj","0");
        // initJson.put("buy_obj",submitOrderForm.getInitJson());
        // initJson.put("product_obj",DataTypeConversionUtil.entity2jonsStr(product));
        // tbOrderEntity.setInitJson(initJson.toJSONString());

        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        // 购买套餐
        // {"m":{"value":1,"status":1},"s":{"value":100,"status":1},"y":{"value":1000,"status":1}}
        tbOrderEntity.setPayable(0);
        tbOrderEntity.setCreateTime(new Date());
        tbOrderDao.insert(tbOrderEntity);
        TbPayRecordEntity record = new TbPayRecordEntity();
        record.setSerialNumber(tbOrderEntity.getSerialNumber());
        record.setPayType(PayTypeEnum.PRO_TYPE_admin.getId());
        record.setPayPaid(0);
        JSONObject json_object = new JSONObject();
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        json_object.put("ip", IPUtils.getIpAddr(request));
        record.setPayMsg(json_object.toJSONString());
        record.setStatus(PayRecordStatusEnum.STATUS_UNKNOWN.getId());
        record.setOperateStatus(0);
        tbPayRecordDao.insert(record);
        // return tbOrderEntity;
        return R.ok().put("data", tbOrderEntity);
    }

    /*
     * 创建购买CDN套餐
     */
    private R createCdnSuitOrder(SubmitOrderForm submitOrderForm) {
        String eMsg = "";
        String sid = System.currentTimeMillis() + "00" + StaticVariableUtils.createOrderIndex;
        TbOrderEntity tbOrderEntity = new TbOrderEntity();
        tbOrderEntity.setSerialNumber(sid);
        tbOrderEntity.setUserId(submitOrderForm.getUserId());
        tbOrderEntity.setOrderType(submitOrderForm.getOrderType());
        tbOrderEntity.setTargetId(submitOrderForm.getTargetId());
        // 执行计算 INIT 数据
        // 购买套餐 {"type":"m","sum":1}
        CdnProductEntity product = cdnProductDao.selectOne(new QueryWrapper<CdnProductEntity>()
                .eq("product_type", submitOrderForm.getOrderType())
                .eq("id", submitOrderForm.getTargetId())
                .in("status", ProductStatusEnum.canBuyStatus())
                .last("limit 1"));
        if (null == product) {
            eMsg = (" product is null");
            return R.error(eMsg);
        }
        TbOrderEntity order = null;
        OrderCdnNewInitVo initVo = DataTypeConversionUtil.string2Entity(submitOrderForm.getInitJson(),
                OrderCdnNewInitVo.class);
        if (initVo == null || null == initVo.getSum()) {
            return R.error("init is null");
        }
        if (initVo.getSum() > 100) {
            return R.error("sum  max is 100");
        }
        // 产品属性array
        JSONArray attrJSONArray = DataTypeConversionUtil.string2JsonArray(product.getAttrJson());
        // 目标CDN 套餐资费类型
        Integer charging_mode_value = getChargingMode(attrJSONArray);
        switch (charging_mode_value) {
            case 1:
                return buy_pre_payment(initVo, product, tbOrderEntity, submitOrderForm);
            case 2:
            case 3:
                return buy_postpaid(product, tbOrderEntity, submitOrderForm);
            default:
                break;
        }
        // return order;
        return R.error("[" + charging_mode_value + "]未知类型！");
    }

    /* CDN续费 */
    private R createCdnRenewOrder(SubmitOrderForm submitOrderForm) {
        // {"orderType":11,"targetId":63,
        // "initJson":"{\"serialNumber\":\"1660614895383002\",\"sum\":1,\"type\":\"y\"}"}
        String eMsg = "";
        String sid = System.currentTimeMillis() + "00" + StaticVariableUtils.createOrderIndex;
        TbOrderEntity tbOrderEntity = new TbOrderEntity();
        tbOrderEntity.setSerialNumber(sid);
        tbOrderEntity.setUserId(submitOrderForm.getUserId());
        tbOrderEntity.setOrderType(submitOrderForm.getOrderType());
        // 套餐续费 {"serialNumber":"1650361752071001","sum":1}
        // 1 原套餐是否可续费 更新到INFI info
        // JSONObject
        // buyJsonObject=DataTypeConversionUtil.string2Json(submitOrderForm.getInitJson());
        OrderCdnRenewVo cdnRenewVo = DataTypeConversionUtil.string2Entity(submitOrderForm.getInitJson(),
                OrderCdnRenewVo.class);
        if (null == cdnRenewVo) {
            eMsg = "serialNumber or sum is null";
            return R.error(eMsg);
            // throw new RRException("[serialNumber][sum] 为空");
            // return null;
        }
        if (cdnRenewVo == null || null == cdnRenewVo.getSum()) {
            return R.error("init is null");
        }
        if (cdnRenewVo.getSum() > 100) {
            return R.error("sum  max is 100");
        }
        // 20220924 新增续费可选择m s y
        String reNewType = "";
        if (StringUtils.isNotBlank(cdnRenewVo.getType())) {
            reNewType = cdnRenewVo.getType();
        }

        Integer sum = cdnRenewVo.getSum();
        String serialNumber = cdnRenewVo.getSerialNumber();
        // 主套餐信息
        Integer[] canQueryType = { CdnSuitStatusEnum.UNKNOWN.getId(), CdnSuitStatusEnum.NORMAL.getId(),
                CdnSuitStatusEnum.TIMEOUT.getId() };
        CdnSuitEntity source_suit = suitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number", serialNumber)
                .eq("suit_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                .in("status", canQueryType)
                .orderByDesc("id")
                .last("limit 1"));
        if (null == source_suit) {
            eMsg = "source_suit is null";
            // throw new RRException("[source_suit] 为空");
            return R.error(eMsg);
            // return null;
        }

        // 主套餐的购买信息
        TbOrderEntity order = tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number", source_suit.getPaySerialNumber())
                .eq("status", PayStatusEnum.PAY_COMPLETE.getId())
                .last("limit 1"));
        if (null == order) {
            eMsg = ("order is null");
            return R.error(eMsg);
            // return null;
        }
        String sourceInitJsonStr = order.getInitJson();
        // {"price_obj":"{\"value\":20100,\"status\":1}",
        // "buy_obj":"{\"type\":\"y\",\"sum\":1}",
        // "product_obj":"{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"}
        JSONObject sourceOrderInitJson = JSONObject.parseObject(sourceInitJsonStr);
        if (!sourceOrderInitJson.containsKey("product_obj") || !sourceOrderInitJson.containsKey("buy_obj")) {
            eMsg = ("[product_obj] or [buy_obj]  is null");
            return R.error(eMsg);
            // return null;
        }
        // 检测源套餐 的ID 目录是否可续费
        JSONObject oldProductJson = sourceOrderInitJson.getJSONObject("product_obj");
        // {"createtime":1650535530000,"productJson":"{\"m\":{\"value\":1,\"status\":0},\"s\":{\"value\":10100,\"status\":0},\"y\":{\"value\":20100,\"status\":1}}","name":"测试产品1","attrJson":"[{\"id\":3,\"name\":\"defense\",\"unit\":\"G\",\"transferMode\":null,\"valueType\":\"int\",\"value\":20,\"status\":1,\"weight\":2},{\"id\":2,\"name\":\"flow\",\"unit\":\"G\",\"transferMode\":null,\"valueType\":\"int\",\"value\":1001,\"status\":1,\"weight\":1}]","weight":2,"id":2,"productType":10,"status":1}
        CdnProductEntity source_product = DataTypeConversionUtil.json2entity(oldProductJson, CdnProductEntity.class);
        CdnProductEntity current_product = cdnProductDao.selectById(source_product.getId());
        if (null == current_product) {
            eMsg = ("current_product  is null");
            return R.error(eMsg);
            // return null;
        }

        // 从原套餐订单中产品ID 与当前 产品ID状态对比，获取可续费状态
        if (!ProductStatusEnum.canRenewStatus().contains(current_product.getStatus())) {
            eMsg = ("current_product is disable");
            return R.error(eMsg);
            // return null;
        }

        // 如果 没有提交续费周期类型取购买的类型
        if (StringUtils.isBlank(reNewType)) {
            JSONObject buy_obj_json = sourceOrderInitJson.getJSONObject("buy_obj");
            if (!buy_obj_json.containsKey("type")) {
                eMsg = ("type  is null");
                return R.error(eMsg);
            }
            reNewType = buy_obj_json.getString("type");
        }

        // 从原套餐订单中获取购买时的产品价格相关信息
        // 从原套餐订单中产品中获取价格相关数据
        // 原套餐的价格
        // {"m":{"value":1,"status":0},"s":{"value":10100,"status":0},"y":{"value":20100,"status":1}}
        JSONObject allPriceJson = DataTypeConversionUtil.string2Json(source_product.getProductJson());

        // AllPriceJson=={"m":{"value":1000,"status":1},"s":{"value":20100,"status":1},"y":{"value":120100,"status":0}}
        if (null == allPriceJson || !allPriceJson.containsKey(reNewType)) {
            eMsg = ("AllPriceJson  not containsKey " + reNewType);
            return R.error(eMsg);
        }
        JSONObject priceTypeJson = allPriceJson.getJSONObject(reNewType);
        if (priceTypeJson.containsKey("status") && 0 == priceTypeJson.getInteger("status")) {
            eMsg = (priceTypeJson.toJSONString() + " 不可选择");
            return R.error(eMsg);
        }

        TbOrderInitVo createOrderInitJsonVo = new TbOrderInitVo();
        createOrderInitJsonVo.setPrice_obj(priceTypeJson);
        createOrderInitJsonVo.setBuy_obj(DataTypeConversionUtil.string2Json(submitOrderForm.getInitJson()));
        createOrderInitJsonVo.setType(reNewType);
        createOrderInitJsonVo.setProduct_obj(DataTypeConversionUtil.entity2jsonObj(source_product));
        tbOrderEntity.setInitJson(DataTypeConversionUtil.entity2jonsStr(createOrderInitJsonVo));

        // JSONObject initJson=new JSONObject();
        // initJson.put("price_obj",PriceTypeJson.toJSONString());
        // initJson.put("buy_obj",submitOrderForm.getInitJson());//"{\"serialNumber\":\"1660614895383002\",\"sum\":1,\"type\":\"y\"}"
        // initJson.put("type",reNewType);
        // initJson.put("product_obj",DataTypeConversionUtil.entity2jonsStr(source_product));
        // tbOrderEntity.setInitJson(initJson.toJSONString());

        // 续费
        // 1 购买时的套餐的ID在当前状态是否可续费 2 取原套餐购买时的套餐时长类型与价格
        // {"price_obj":"{\"value\":20100,\"status\":1}",
        // "buy_obj":"{\"type\":\"y\",\"sum\":1}",
        // "product_obj":"{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"}
        Integer final_price = -1;
        if (priceTypeJson.containsKey("value")) {
            final_price = priceTypeJson.getInteger("value") * sum;
        }
        if (final_price < 0) {
            eMsg = (" price is le 0");
            return R.error(eMsg);
        }
        tbOrderEntity.setPayable(final_price);
        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        tbOrderEntity.setCreateTime(new Date());
        tbOrderDao.insert(tbOrderEntity);
        // return tbOrderEntity;
        return R.ok().put("data", tbOrderEntity);

    }

    /**
     * 获取套餐的最后到期时间
     * 
     * @return
     */
    private Date getSuitFinalEndDate(String suitSerialNumber) {
        Integer[] s_in_list = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(), OrderTypeEnum.ORDER_CDN_RENEW.getTypeId() };
        Integer[] s_in_status_list = { CdnSuitStatusEnum.NORMAL.getId() };
        CdnSuitEntity end_suit = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number", suitSerialNumber)
                .in("suit_type", s_in_list)
                .in("status", s_in_status_list)
                .orderByDesc("end_time")
                .last("limit 1"));
        if (null != end_suit) {
            return end_suit.getEndTime();
        }
        return new Date();
    }

    /*
     * CDN 增值服务
     */
    private R create_cdn_extra_order(SubmitOrderForm submitOrderForm) {
        String sid = System.currentTimeMillis() + "000" + StaticVariableUtils.createOrderIndex;
        String eMsg = "";
        TbOrderEntity tbOrderEntity = new TbOrderEntity();
        tbOrderEntity.setSerialNumber(sid);
        tbOrderEntity.setUserId(submitOrderForm.getUserId());
        tbOrderEntity.setOrderType(submitOrderForm.getOrderType());
        tbOrderEntity.setTargetId(submitOrderForm.getTargetId());

        // 增值服务|加油包 购买
        // {"serialNumber":"1650361752071001","startTime":"1650769152","type":"m","sum":2}
        // "{\"serialNumber\":\"1673836321266002\",\"sum\":1,\"type\":\"m\",\"startTime\":1681434642996}"
        Integer[] enable_buy_status = { ProductStatusEnum.ENABLE.getId(), ProductStatusEnum.ONLY_BUY.getId() };
        CdnProductEntity product = cdnProductDao.selectOne(new QueryWrapper<CdnProductEntity>()
                .eq("product_type", OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())
                .eq("id", submitOrderForm.getTargetId())
                .in("status", enable_buy_status)
                .last("limit 1"));
        if (null == product) {
            eMsg = ("product is null");
            return R.error(eMsg);
        }
        // JSONObject
        // buyJson=DataTypeConversionUtil.string2Json(submitOrderForm.getInitJson());
        OrderCdnAddedInitVo initVo = DataTypeConversionUtil.string2Entity(submitOrderForm.getInitJson(),
                OrderCdnAddedInitVo.class);
        // {"serialNumber":"1650361752071001","startTime":"1650769152","type":"m","sum":2}
        if (null == initVo || null == initVo.getSum()) {
            eMsg = (" type or serialNumber or  startTime or sum is  null");
            return R.error(eMsg);
        }
        if (initVo.getSum() > 10) {
            return R.error("sum max is 10");
        }
        // 获取绑定的套餐
        CdnSuitEntity source_suit = suitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number", initVo.getSerialNumber())
                .eq("suit_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                .orderByDesc("end_time")
                .last("limit 1"));
        if (null == source_suit) {
            eMsg = ("source_suit is null");
            return R.error(eMsg);
        }

        JSONObject AllPriceJson = DataTypeConversionUtil.string2Json(product.getProductJson());
        if (null == AllPriceJson || !AllPriceJson.containsKey(initVo.getType())) {
            eMsg = (" AllPriceJson not containsKey " + initVo.getType());
            return R.error(eMsg);
        }

        // {"m":{"value":1,"status":0},"s":{"value":10100,"status":0},"y":{"value":20100,"status":1}}
        JSONObject t_type_json = AllPriceJson.getJSONObject(initVo.getType());
        if (!t_type_json.containsKey("status")) {
            eMsg = ("status is null");
            return R.error(eMsg);
        }
        if (!t_type_json.containsKey("value")) {
            eMsg = ("value is null");
            return R.error(eMsg);
        }
        if (1 != t_type_json.getInteger("status")) {
            eMsg = ("status is not 1");
            return R.error(eMsg);
        }

        // 构造下单初始数据
        TbOrderInitVo createOrderInitJsonVo = new TbOrderInitVo();
        createOrderInitJsonVo.setPrice_obj(AllPriceJson.getJSONObject(initVo.getType()));
        createOrderInitJsonVo.setBuy_obj(DataTypeConversionUtil.entity2jsonObj(initVo));
        createOrderInitJsonVo.setProduct_obj(DataTypeConversionUtil.entity2jsonObj(product));
        tbOrderEntity.setInitJson(DataTypeConversionUtil.entity2jonsStr(createOrderInitJsonVo));

        // 加油包 增值服务
        // {"m":{"value":1,"status":1},"s":{"value":100,"status":1},"y":{"value":1000,"status":1}}
        Integer sum = initVo.getSum();
        Integer unitPrice = t_type_json.getInteger("value");
        // 计算完整待付价
        Integer payAble = sum * unitPrice;
        if (payAble < 0) {
            eMsg = (" price is le 0[1]");
            return R.error(eMsg);
        }
        // 计算套餐剩余时长--折算到期时间的价格
        Date suitEndDate = getSuitFinalEndDate(initVo.getSerialNumber());
        String stm = initVo.getStartTime().toString();
        Date startDate = new Date();
        if (stm.length() > 10) {
            startDate = DateUtils.LongStamp2Date(Long.parseLong(stm));
        } else {
            startDate = DateUtils.stamp2date(Integer.parseInt(stm));
        }
        Date orderEndDate = ProductUnitEnum.getEndDate(initVo.getType(), startDate, sum);
        if (suitEndDate.before(orderEndDate)) {
            logger.warn("suitEndDate.before(orderEndDate)");
            // 套餐在增值前到期--折算价格
            payAble = new Long(payAble * (suitEndDate.getTime() - startDate.getTime())
                    / (orderEndDate.getTime() - startDate.getTime())).intValue();
            tbOrderEntity.setPayable(payAble);
        } else {
            // 全价
            tbOrderEntity.setPayable(payAble);
        }
        if (payAble < 0) {
            eMsg = (" price is le 0[2]");
            return R.error(eMsg);
        }
        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        tbOrderEntity.setCreateTime(new Date());
        tbOrderDao.insert(tbOrderEntity);
        // return tbOrderEntity;
        return R.ok().put("data", tbOrderEntity);
    }

    /* CDN 升级套餐 */
    private R createCdnUpgradeOrder(SubmitOrderForm submitOrderForm) {
        String eMsg = "";
        // 1原套餐在有效期间 升级的套餐存在可购买
        String sid = System.currentTimeMillis() + "00" + StaticVariableUtils.createOrderIndex;
        TbOrderEntity tbOrderEntity = new TbOrderEntity();
        tbOrderEntity.setSerialNumber(sid);
        tbOrderEntity.setUserId(submitOrderForm.getUserId());
        tbOrderEntity.setOrderType(submitOrderForm.getOrderType());
        tbOrderEntity.setTargetId(submitOrderForm.getTargetId());
        // 1
        CdnProductEntity product = cdnProductDao.selectOne(new QueryWrapper<CdnProductEntity>()
                .eq("product_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                .eq("id", submitOrderForm.getTargetId())
                .in("status", ProductStatusEnum.canUpStatus())
                .last("limit 1"));
        if (null == product) {
            eMsg = "【create_cdn_upgrade_order】 product is null";
            return R.error(eMsg);
        }
        // {"serialNumber":"1650361752071001"}
        OrderCdnUpgradeVo cdnUpgradeVo = DataTypeConversionUtil.string2Entity(submitOrderForm.getInitJson(),
                OrderCdnUpgradeVo.class);
        // JSONObject
        // buyJsonObject=DataTypeConversionUtil.string2Json(submitOrderForm.getInitJson());
        if (null == cdnUpgradeVo || null == cdnUpgradeVo.getSum()) {
            eMsg = "serialNumber or sum is null";
            return R.error(eMsg);
        }
        if (cdnUpgradeVo.getSum() > 100) {
            return R.error("sum  max is 100");
        }
        // 2 获取原套餐
        String serialNumber = cdnUpgradeVo.getSerialNumber();
        Integer[] enable_up_list = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),
                OrderTypeEnum.ORDER_CDN_UPGRADE.getTypeId() };
        CdnSuitEntity source_suit = suitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number", serialNumber)
                .in("suit_type", enable_up_list)
                .ne("status", CdnSuitStatusEnum.DISABLE.getId())
                .orderByDesc("end_time")
                .last("limit 1"));
        if (null == source_suit) {
            eMsg = "[create_cdn_upgrade_order]source_suit is null";
            return R.error(eMsg);
        }
        // 获取原订单
        TbOrderEntity source_order = tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .in("order_type", enable_up_list)
                .eq("serial_number", source_suit.getPaySerialNumber())
                .eq("status", PayStatusEnum.PAY_COMPLETE.getId())
                .orderByDesc("id")
                .last("limit 1"));
        if (null == source_order) {
            eMsg = "[create_cdn_upgrade_order]source_order  is null";
            return R.error(eMsg);
        }
        // 3
        Integer[] in_type_list = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(), OrderTypeEnum.ORDER_CDN_RENEW.getTypeId(),
                OrderTypeEnum.ORDER_CDN_UPGRADE.getTypeId() };
        CdnSuitEntity suit = suitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("user_id", submitOrderForm.getUserId())
                .in("suit_type", in_type_list)
                .eq("serial_number", serialNumber)
                .ne("status", CdnSuitStatusEnum.DISABLE.getId())
                .orderByDesc("end_time")
                .last("limit 1"));
        if (null == suit) {
            eMsg = "[create_cdn_upgrade_order]suit  is null";
            return R.error(eMsg);
        }
        Date now = new Date();
        if (suit.getEndTime().before(now)) {
            eMsg = "[create_cdn_upgrade_order]suit  is out of time";
            // throw new RRException("过期套餐不可直接升级！");
            return R.error(eMsg);
        }
        //
        // {"price_obj":"{\"value\":20100,\"status\":1}",
        // "buy_obj":"{\"type\":\"y\",\"sum\":1}",
        // "product_obj":"{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"}

        JSONObject sourceOrderInitJson = DataTypeConversionUtil.string2Json(source_order.getInitJson());
        // {"price_obj":"{\"value\":20100,\"status\":1}",
        // "buy_obj":"{\"type\":\"y\",\"sum\":4}",
        // "product_obj":"{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"}
        if (null == sourceOrderInitJson || !sourceOrderInitJson.containsKey("buy_obj")) {
            eMsg = "buy_obj is null";
            return R.error(eMsg);
        }
        JSONObject source_buy_obj = sourceOrderInitJson.getJSONObject("buy_obj");
        if (!source_buy_obj.containsKey("type")) {
            eMsg = "source_buy_obj is null";
            return R.error(eMsg);
        }
        String type = source_buy_obj.getString("type");
        if (!sourceOrderInitJson.containsKey("price_obj")) {
            eMsg = "price_obj is null";
            return R.error(eMsg);
        }
        JSONObject source_price_obj = sourceOrderInitJson.getJSONObject("price_obj");
        if (!source_price_obj.containsKey("value")) {
            eMsg = "price_obj value is null";
            return R.error(eMsg);
        }
        Integer value = source_price_obj.getIntValue("value");

        if (sourceOrderInitJson.containsKey("product_obj")) {
            JSONObject source_product_json = sourceOrderInitJson.getJSONObject("product_obj");
            CdnProductEntity source_product = DataTypeConversionUtil.json2entity(source_product_json,
                    CdnProductEntity.class);
            if (null != source_product && null != source_product.getStatus()) {
                if (source_product.getStatus().equals(ProductStatusEnum.ONLY_BUY.getId())) {
                    eMsg = "ONLY_BUY  value can't update";
                    return R.error(eMsg);
                }
                if (source_product.getStatus().equals(ProductStatusEnum.ONLY_FIRST.getId())) {
                    eMsg = "注册赠送套餐不可升级";
                    return R.error(eMsg);
                }
            }
        }

        TbOrderInitVo createOrderInitJsonVo = new TbOrderInitVo();
        createOrderInitJsonVo.setPrice_obj(source_price_obj);
        cdnUpgradeVo.setType(type);
        cdnUpgradeVo.setSum(0);
        createOrderInitJsonVo.setBuy_obj(DataTypeConversionUtil.entity2jsonObj(cdnUpgradeVo));
        createOrderInitJsonVo.setType(type);
        createOrderInitJsonVo.setProduct_obj(DataTypeConversionUtil.entity2jsonObj(product));
        createOrderInitJsonVo.setEnd_date_time(suit.getEndTime().getTime());
        tbOrderEntity.setInitJson(DataTypeConversionUtil.entity2jonsStr(createOrderInitJsonVo));

        // JSONObject initJson=new JSONObject();
        // buyJsonObject.put("type",type);
        // buyJsonObject.put("sum","0");
        // initJson.put("buy_obj",buyJsonObject);
        // initJson.put("price_obj",source_price_obj);
        // initJson.put("product_obj",DataTypeConversionUtil.entity2jonsStr(product));
        // initJson.put("end_date_time",suit.getEndTime().getTime());
        // tbOrderEntity.setInitJson(initJson.toJSONString());

        // 计算价格： 1购买的原套餐类型 2剩余价格(剩余时间) 3新套餐价格对应补交价格

        Date f_endDate = suit.getEndTime();
        Date f_startDate = this.getStartDate(f_endDate, type);

        // 剩余 时长
        long b_times = f_endDate.getTime() - now.getTime();
        // 一个周期的时长
        long r_times = f_endDate.getTime() - f_startDate.getTime();
        double d_b_price = b_times * value / r_times;
        // 计算剩余价值
        int b_price = (int) (d_b_price);
        // 新套餐补差价
        JSONObject product_price_s_obj = DataTypeConversionUtil.string2Json(product.getProductJson());
        if (null == product_price_s_obj || !product_price_s_obj.containsKey(type)) {
            eMsg = "product_price_s_obj type is null";
            return R.error(eMsg);
        }
        JSONObject product_type_price_obj = product_price_s_obj.getJSONObject(type);
        // {"m":{"value":1,"status":1},"s":{"value":100,"status":1},"y":{"value":1000,"status":1}}
        if (!product_type_price_obj.containsKey("value")) {
            eMsg = "product_type_price_obj value is null";
            return R.error(eMsg);
        }
        Integer p_value = product_type_price_obj.getIntValue("value");
        if (p_value <= value) {
            eMsg = "p_value<=value";
            return R.error(eMsg);
        }
        // 新套餐剩余价格
        int f_price = (int) (b_times * p_value / r_times);
        Integer f_payPaid = f_price - b_price;
        if (f_payPaid < 0) {
            eMsg = "f_payPaid le 0";
            return R.error(eMsg);
        }
        tbOrderEntity.setPayable(f_payPaid);
        // 设置为待支付
        tbOrderEntity.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
        tbOrderEntity.setCreateTime(new Date());
        tbOrderDao.insert(tbOrderEntity);
        // return tbOrderEntity;
        return R.ok().put("data", tbOrderEntity);

    }

    /**
     * 创建订单
     * 
     * @param
     * @return
     */
    @Override
    public R createOrder(SubmitOrderForm submitOrderForm) {
        String eMsg = "";
        if (null == submitOrderForm.getUserId() || null == submitOrderForm.getOrderType()
                || null == submitOrderForm.getTargetId()) {
            eMsg = "CreateOrder fail ,参数不完整";
            return R.error(eMsg);
        }
        StaticVariableUtils.createOrderIndex++;
        if (submitOrderForm.getOrderType().equals(OrderTypeEnum.ORDER_AUTHENTICATION.getTypeId())) {
            // 2022/4/24 实名订单
            // return this.create_auth_order(submitOrderForm);
        } else if (submitOrderForm.getOrderType().equals(OrderTypeEnum.ORDER_RECHARGE.getTypeId())) {
            // 充值
            return this.create_recharge_order(submitOrderForm);
        } else if (submitOrderForm.getOrderType().equals(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())) {
            // 购买CDN
            // {"userId":61,"orderType":10,"targetId":2,"initJson":"{\"type\":\"y\",\"sum\":4}"}
            return this.createCdnSuitOrder(submitOrderForm);
        } else if (submitOrderForm.getOrderType().equals(OrderTypeEnum.ORDER_CDN_RENEW.getTypeId())) {
            // CDN续费
            // {"userId":61,"orderType":11,"targetId":0,"initJson":"{\"serialNumber\":\"1650944278952005\",\"sum\":4}"}
            return this.createCdnRenewOrder(submitOrderForm);
        } else if (submitOrderForm.getOrderType().equals(OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())) {
            // 增值服务
            // {"userId":61,"orderType":12,"targetId":4,"initJson":"{\"serialNumber\":\"1650944278952005\",\"startTime\":1650769152,\"type\":\"y\",\"sum\":2}"}
            return this.create_cdn_extra_order(submitOrderForm);
        } else if (submitOrderForm.getOrderType().equals(OrderTypeEnum.ORDER_CDN_UPGRADE.getTypeId())) {
            // 套餐升级
            // {"userId":61,"orderType":13,"targetId":3,"initJson":"{\"serialNumber\":\"1650954704735002\"}"}
            return this.createCdnUpgradeOrder(submitOrderForm);
        }
        return R.error("--订单创建失败--");
    }

    private String getUserNameByUserId(Long userId) {
        TbUserEntity user = userDao.selectById(userId);
        if (null != user) {
            if (StringUtils.isNotBlank(user.getUsername())) {
                return user.getUsername();
            } else if (StringUtils.isNotBlank(user.getMobile())) {
                return user.getMobile();
            } else if (StringUtils.isNotBlank(user.getMail())) {
                return user.getMail();
            } else {
                return user.getUserId() + "";
            }
        }
        return null;
    }

    private void getOrderProduct(TbOrderEntity orderEntity) {
        JSONObject productObj = new JSONObject();
        switch (orderEntity.getOrderType()) {
            case 1:
                productObj.put("name", "实名认证");
                break;
            case 2:
                productObj.put("name", "充值");
                break;
            case 10:
            case 11:
            case 12:
            case 13:
                if (true) {
                    if (StringUtils.isNotBlank(orderEntity.getInitJson())) {
                        JSONObject init_json = DataTypeConversionUtil.string2Json(orderEntity.getInitJson());
                        if (null != init_json && init_json.containsKey("product_obj")) {
                            JSONObject product_obj = init_json.getJSONObject("product_obj");
                            if (product_obj.containsKey("name")) {
                                productObj.put("name",
                                        "[" + orderEntity.getTargetId() + "]" + product_obj.getString("name"));
                            }
                        }
                    }
                }
                break;
            case 30:
                if (true) {
                    if (StringUtils.isNotBlank(orderEntity.getInitJson())) {
                        JSONObject init_json = DataTypeConversionUtil.string2Json(orderEntity.getInitJson());
                        if (null != init_json && init_json.containsKey("date")
                                && init_json.containsKey("product_obj")) {
                            String date = init_json.getString("date");
                            JSONObject product_obj = init_json.getJSONObject("product_obj");
                            if (product_obj.containsKey("name")) {
                                productObj.put("name", "[" + orderEntity.getTargetId() + "]"
                                        + product_obj.getString("name") + "-" + date);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
        orderEntity.setProduct(productObj);
    }

    @Override
    public PageUtils orderList(Map params) {
        String[] userIds = {};
        String[] status = {};
        String[] orderType = {};
        QueryOrderForm form = DataTypeConversionUtil.map2entity(params, QueryOrderForm.class);
        if (StringUtils.isNotBlank(form.getUserIds())) {
            userIds = form.getUserIds().split(",");
        }
        if (StringUtils.isNotBlank(form.getStatus())) {
            status = form.getStatus().split(",");
        }
        if (StringUtils.isNotBlank(form.getOrderType())) {
            orderType = form.getOrderType().split(",");
        }
        IPage<TbOrderEntity> i_page = tbOrderDao.selectPage(
                new Page<>(form.getPage(), form.getLimit()),
                new QueryWrapper<TbOrderEntity>()
                        .eq("is_delete", 0)
                        .orderByDesc("id")
                        .in(userIds.length > 0, "user_id", userIds)
                        .like(StringUtils.isNotBlank(form.getSerialNumber()), "serial_number", form.getSerialNumber())
                        .in(status.length > 0, "status", status)
                        .in(orderType.length > 0, "order_type", orderType));
        i_page.getRecords().forEach(item -> {
            TbPayRecordEntity record = recordDao.selectOne(
                    new QueryWrapper<TbPayRecordEntity>().eq("serial_number", item.getSerialNumber()).last("limit 1"));
            item.setPayObject(record);
            item.setUser(this.getUserNameByUserId(item.getUserId()));
            this.getOrderProduct(item);
        });
        return new PageUtils(i_page);
    }

    @Override
    public CdnSuitEntity getSuitDetailBySerial(Long userId, String serialNumber, boolean bindSite, boolean usedInfo) {
        Integer[] sType = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(), OrderTypeEnum.ORDER_CDN_RENEW.getTypeId() };
        CdnSuitEntity suit = commonTaskService.commGetSuitDetail(userId, serialNumber, Arrays.asList(sType), usedInfo);
        if (bindSite) {
            if (null != suit.getUserId() && null != suit.getSerialNumber()) {
                suit.setBindSiteList(this.getSuitBind(suit.getUserId(), suit.getSerialNumber(), 1));
            }
        }
        return null == suit ? new CdnSuitEntity() : suit;
    }

    private CdnProductEntity getProductEntityBySuit(CdnSuitEntity suit) {
        CdnProductEntity product = new CdnProductEntity();
        TbOrderEntity order = tbOrderDao.selectOne(
                new QueryWrapper<TbOrderEntity>().eq("serial_number", suit.getPaySerialNumber()).last("limit 1"));
        if (null != order) {
            JSONObject orderInitJson = DataTypeConversionUtil.string2Json(order.getInitJson());
            if (null != orderInitJson && orderInitJson.containsKey("product_obj")) {
                JSONObject productObjJson = orderInitJson.getJSONObject("product_obj");
                product = DataTypeConversionUtil.json2entity(productObjJson, CdnProductEntity.class);
                TbOrderEntity order_main = tbOrderDao.selectOne(
                        new QueryWrapper<TbOrderEntity>().eq("serial_number", suit.getSerialNumber()).last("limit 1"));
                if (null != order_main) {
                    product.setName(String.format("[%d]%s", order_main.getId(), product.getName()));
                }
                return product;
            }
        }
        TbOrderEntity orderMain = tbOrderDao.selectOne(
                new QueryWrapper<TbOrderEntity>().eq("serial_number", suit.getSerialNumber()).last("limit 1"));
        if (null != orderMain) {
            product.setName(String.format("[%d]", orderMain.getId()));
        }
        return product;
    }

    /**
     * 套餐列表
     * 
     * @param params
     * @return
     */
    @Override
    public PageUtils suitList(Map params) {
        SuitListForm form = DataTypeConversionUtil.map2entity(params, SuitListForm.class);
        if (null == form.getPage() || null == form.getLimit()) {
            return null;
        }
        // mode 0:所有 1使用中 2未使用 3已过期 4已失效
        Date now = new Date();
        Date sDate = null;
        Date eDate = null;
        if (null != form.getStartTime() && null != form.getEndTime()) {
            sDate = DateUtils.stamp2date(form.getStartTime());
            eDate = DateUtils.stamp2date(form.getEndTime());
        }
        // mode //mode 0:所有 1使用中 2未使用 3已过期 4已失效
        Integer mode = 0;
        if (null != form.getMode()) {
            mode = form.getMode();
        }
        // 搜索的产品类型
        List<Integer> productTypeList = new ArrayList<>();
        String productTypes = form.getProductTypes();
        if (StringUtils.isNotBlank(productTypes)) {
            for (String pt : productTypes.split(",")) {
                productTypeList.add(Integer.parseInt(pt));
            }
        } else {
            productTypeList.add(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId());
            productTypeList.add(OrderTypeEnum.ORDER_CDN_RENEW.getTypeId());
        }
        // 搜索KEY
        List<String> serial_number_list = new ArrayList<>();
        if (StringUtils.isNotBlank(form.getKey())) {
            // {"price_obj":"{\"value\":1,\"status\":1}","buy_obj":"{\"sum\":1,\"type\":\"m\"}","product_obj":"{\"createtime\":1658542562000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":200,\\\"status\\\":1}}\",\"name\":\"流量套餐\",\"serverGroupIds\":\"1\",\"attrJson\":\"[{\\\"attr\\\":\\\"charging_mode\\\",\\\"name\\\":\\\"charging_mode\\\",\\\"value\\\":1,\\\"valueType\\\":\\\"select\\\"},{\\\"attr\\\":\\\"flow\\\",\\\"name\\\":\\\"flow\\\",\\\"valueType\\\":\\\"int\\\",\\\"unit\\\":\\\"G\\\",\\\"value\\\":100},{\\\"attr\\\":\\\"bandwidth\\\",\\\"name\\\":\\\"bandwidth\\\",\\\"valueType\\\":\\\"price_int\\\",\\\"unit\\\":\\\"元/Mbps/月\\\",\\\"value\\\":200000},{\\\"attr\\\":\\\"ai_waf\\\",\\\"id\\\":27,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"AI
            // WAF\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"public_waf\\\",\\\"id\\\":37,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"公共WAF\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"port_forwarding\\\",\\\"id\\\":36,\\\"unit\\\":\\\"个\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"端口转发\\\",\\\"value\\\":6},{\\\"attr\\\":\\\"site\\\",\\\"id\\\":35,\\\"unit\\\":\\\"个\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"站点\\\",\\\"value\\\":10001},{\\\"attr\\\":\\\"sms\\\",\\\"id\\\":34,\\\"unit\\\":\\\"条\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"短信通知\\\",\\\"value\\\":101,\\\"hiddenUsed\\\":true},{\\\"attr\\\":\\\"monitor\\\",\\\"id\\\":33,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"流量监控\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"private_waf\\\",\\\"id\\\":32,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"专属WAF\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"live_data\\\",\\\"id\\\":30,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"实时数据\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"defense\\\",\\\"id\\\":38,\\\"unit\\\":\\\"QPS\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"防御\\\",\\\"value\\\":1001,\\\"hiddenUsed\\\":true}]\",\"weight\":1,\"id\":14,\"productType\":10,\"status\":1}"}
            List<TbOrderEntity> o_list = tbOrderDao.selectList(new QueryWrapper<TbOrderEntity>()
                    .in(StringUtils.isNotBlank(form.getUserIds()), "user_id", form.getUserIds().split(","))
                    .in("order_type", productTypeList.toArray())
                    .like("init_json", form.getKey())
                    .eq("status", PayStatusEnum.PAY_COMPLETE.getId()));
            serial_number_list.addAll(o_list.stream().map(t -> t.getSerialNumber()).collect(Collectors.toList()));
        }

        IPage<CdnSuitEntity> ipage = new Page<>();
        // mode 0:所有 1使用中 2未使用 3已过期 4已失效
        Integer s_MODE_ALL = 0;
        if (s_MODE_ALL.equals(mode)) {
            ipage = suitDao.selectPage(
                    new Page<>(form.getPage(), form.getLimit()),
                    new QueryWrapper<CdnSuitEntity>()
                            .orderByDesc("id")
                            .in(serial_number_list.size() > 0, "serial_number", serial_number_list.toArray())
                            .in(StringUtils.isNotBlank(form.getUserIds()), "user_id", form.getUserIds().split(","))
                            .like(StringUtils.isNotBlank(form.getSerialNumber()), "serial_number",
                                    form.getSerialNumber())
                            .le("start_time", now)
                            .ge("end_time", now)
                            .ge(null != sDate, "start_time", sDate)
                            .le(null != eDate, "end_time", eDate));
        } else if (S_MODE_IN_USE.equals(mode)) {
            // 使用中
            Integer[] viewStatusLs = { CdnSuitStatusEnum.UNKNOWN.getId(), CdnSuitStatusEnum.NORMAL.getId() }; // suit/list
            ipage = suitDao.selectPage(
                    new Page<CdnSuitEntity>(form.getPage(), form.getLimit()),
                    new QueryWrapper<CdnSuitEntity>()
                            .orderByDesc("id")
                            .in("suit_type", productTypeList.toArray())
                            .in(serial_number_list.size() > 0, "serial_number", serial_number_list.toArray())
                            .in(StringUtils.isNotBlank(form.getUserIds()), "user_id", form.getUserIds().split(","))
                            .like(StringUtils.isNotBlank(form.getSerialNumber()), "serial_number",
                                    form.getSerialNumber())
                            .in("status", viewStatusLs)
                            .le("start_time", now)
                            .ge("end_time", now)
                            .ge(null != sDate, "start_time", sDate)
                            .le(null != eDate, "end_time", eDate));
            ipage.getRecords().forEach(item -> {
                // CdnSuitEntity
                // t_suit=commonTaskService.GetSuitDetail(item.getUserId(),item.getSerialNumber());
                // updateCdnSuit(item,S_MODE_IN_USE);
                simpleUpdateSuit(item);
            });

        } else if (S_MODE_NOT_USE.equals(mode)) {
            // 2未使用
            // Integer[] show_type={ OrderTypeEnum.ORDER_CDN_SUIT.getTypeId()};
            ipage = suitDao.selectPage(
                    new Page<CdnSuitEntity>(form.getPage(), form.getLimit()),
                    new QueryWrapper<CdnSuitEntity>()
                            .orderByDesc("id")
                            .in("suit_type", productTypeList.toArray())
                            .in(StringUtils.isNotBlank(form.getUserIds()), "user_id", form.getUserIds().split(","))
                            .like(StringUtils.isNotBlank(form.getSerialNumber()), "serial_number",
                                    form.getSerialNumber())
                            .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                            .ge("start_time", now)
                            .ge(null != sDate, "start_time", sDate)
                            .le(null != eDate, "end_time", eDate));
            ipage.getRecords().forEach(item -> {
                // updateCdnSuit(item,S_MODE_NOT_USE);
                simpleUpdateSuit(item);
            });

        } else if (S_MODE_TIME_OUT.equals(mode)) {
            // 3已过期
            ipage = suitDao.selectPage(
                    new Page<>(form.getPage(), form.getLimit()),
                    new QueryWrapper<CdnSuitEntity>()
                            .select("MAX(id) AS id", "serial_number") // 🔧 聚合字段
                            .orderByDesc("id")
                            .in("suit_type", productTypeList.toArray())
                            .in(StringUtils.isNotBlank(form.getUserIds()), "user_id", form.getUserIds().split(","))
                            .like(StringUtils.isNotBlank(form.getSerialNumber()), "serial_number",
                                    form.getSerialNumber())
                            .and(q -> q.eq("status", CdnSuitStatusEnum.TIMEOUT.getId()).or().le("end_time", now))
                            .ge(null != sDate, "start_time", sDate)
                            .le(null != eDate, "end_time", eDate)
                            .groupBy("serial_number"));
            List<CdnSuitEntity> ls = ipage.getRecords();
            Iterator<CdnSuitEntity> iterator = ls.iterator();
            while (iterator.hasNext()) {
                CdnSuitEntity item = iterator.next();
                CdnSuitEntity itemCdnEntity = commonTaskService.updateThisSuitDetailInfo(item, true);
                if (itemCdnEntity.getEndTime().after(now)) {
                    iterator.remove();
                } else {
                    item.setBindSiteList(this.getSuitBind(item.getUserId(), item.getSerialNumber(), 1));
                }

            }
            ipage.setRecords(ls);
        } else if (S_MODE_INVALID.equals(mode)) {
            // 4已失效
            ipage = suitDao.selectPage(
                    new Page<CdnSuitEntity>(form.getPage(), form.getLimit()),
                    new QueryWrapper<CdnSuitEntity>()
                            .orderByDesc("id")
                            .in("suit_type", productTypeList.toArray())
                            .in(StringUtils.isNotBlank(form.getUserIds()), "user_id", form.getUserIds().split(","))
                            .like(StringUtils.isNotBlank(form.getSerialNumber()), "serial_number",
                                    form.getSerialNumber())
                            .eq("status", CdnSuitStatusEnum.DISABLE.getId())
                            .ge(null != sDate, "start_time", sDate)
                            .le(null != eDate, "end_time", eDate));
            ipage.getRecords().forEach(item -> {
                ProductAttrVo sAttrVo = DataTypeConversionUtil.string2Entity(item.getAttrJson(), ProductAttrVo.class);
                // item.setAttr(DataTypeConversionUtil.string2Json(item.getAttrJson()) );
                item.setAttr(sAttrVo);
                item.setProduct(getProductEntityBySuit(item));
                item.setProductEntity(getProductEntityBySuit(item));
                item.setBindSiteList(this.getSuitBind(item.getUserId(), item.getSerialNumber(), 1));
            });

        } else {
            System.out.println("unknown mode");
        }
        return new PageUtils(ipage);
    }

    private void updateCdnSuit(CdnSuitEntity item, Integer mode) {
        if (S_MODE_IN_USE.equals(mode)) {
            // 使用中
            if (item.getSuitType().equals(OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())) {
                commonTaskService.updateThisSuitDetailInfo(item, true);
                Integer[] sType = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),
                        OrderTypeEnum.ORDER_CDN_RENEW.getTypeId() };
                CdnSuitEntity main_suit = commonTaskService.commGetSuitDetail(item.getUserId(), item.getSerialNumber(),
                        Arrays.asList(sType), true);
                item.setMainSuitObj(main_suit);
            } else {
                commonTaskService.updateThisSuitDetailInfo(item, true);
                item.setBindSiteList(this.getSuitBind(item.getUserId(), item.getSerialNumber(), 1));
                LocalDate now_date = LocalDate.now();
                LocalDate firstDay = now_date.with(TemporalAdjusters.firstDayOfMonth());// 获取当前月的第一天
                // LocalDate lastDay = now_date.with(TemporalAdjusters.lastDayOfMonth()); //
                // 获取当前月的最后一天
                LocalDate lastDay = now_date;// 最后一天为当天
                Date fd = DateUtils.stringToDate(firstDay.format(DateTimeFormatter.ISO_DATE) + " 00:00:00",
                        DateUtils.DATE_TIME_PATTERN);
                Date ld = DateUtils.stringToDate(lastDay.format(DateTimeFormatter.ISO_DATE) + " 23:59:59",
                        DateUtils.DATE_TIME_PATTERN);
                item.setCurrentMonthData(commonTaskService.getBytesDataListBySuitSerialNumber(item.getUserId(),
                        item.getSerialNumber(), fd, ld));
            }
            TbUserEntity user = userDao.selectById(item.getUserId());
            if (null != user) {
                item.setUsername(user.getUsername());
            }
        } else if (S_MODE_NOT_USE.equals(mode)) {
            ProductAttrVo sAttrVo = DataTypeConversionUtil.string2Entity(item.getAttrJson(), ProductAttrVo.class);
            // item.setAttr(DataTypeConversionUtil.string2Json(item.getAttrJson()));
            item.setAttr(sAttrVo);
            CdnProductEntity i_product = getProductEntityBySuit(item);
            if (item.getSuitType().equals(OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())) {
                //
            } else if (item.getSuitType().equals(OrderTypeEnum.ORDER_CDN_RENEW.getTypeId())) {
                i_product.setName(i_product.getName() + "[续费]");
                item.setBindSiteList(this.getSuitBind(item.getUserId(), item.getSerialNumber(), 1));
            } else if (item.getSuitType().equals(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())) {
                item.setBindSiteList(this.getSuitBind(item.getUserId(), item.getSerialNumber(), 1));
            }
            item.setProductEntity(i_product);
            item.setProduct(item.getProductEntity());
        } else {
            ProductAttrVo sAttrVo = DataTypeConversionUtil.string2Entity(item.getAttrJson(), ProductAttrVo.class);
            // item.setAttr(DataTypeConversionUtil.string2Json(item.getAttrJson()));
            item.setAttr(sAttrVo);
            item.setProduct(getProductEntityBySuit(item));
            item.setProductEntity(getProductEntityBySuit(item));
            item.setBindSiteList(this.getSuitBind(item.getUserId(), item.getSerialNumber(), 1));
        }

    }

    /**
     * 已使用流量
     * 
     * @param suit
     */
    private void alreadyFlowConsumeInfo(CdnSuitEntity suit) {
        ProductAttrVo result = new ProductAttrVo();
        Date now = new Date();
        Integer[] types = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(), OrderTypeEnum.ORDER_CDN_RENEW.getTypeId() };
        Integer[] statusS = { CdnSuitStatusEnum.UNKNOWN.getId(), CdnSuitStatusEnum.NORMAL.getId() };
        CdnSuitEntity suitEntity = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number", suit.getSerialNumber())
                .in("suit_type", types)
                .in("status", statusS)
                .le("start_time", now)
                .ge("end_time", now)
                .last("limit 1"));
        if (null != suitEntity) {
            Date buyStartTm = suitEntity.getStartTime();
            Date buyEndTm = suitEntity.getEndTime();
            // 当前时间处于套餐的阶段
            Date curDateLastStartDate = null;
            Date curDateLastEndDate = null;
            for (int i = 0;; i++) {
                Date next_month_date = DateUtils.addDateMonths(buyStartTm, i);
                if (next_month_date.after(now)) {
                    curDateLastStartDate = DateUtils.addDateMonths(next_month_date, -1);
                    if (next_month_date.before(buyEndTm)) {
                        curDateLastEndDate = next_month_date;
                    } else {
                        curDateLastEndDate = buyEndTm;
                    }
                    break;
                }
            }
            if (null != curDateLastStartDate && null != curDateLastEndDate) {
                List<CdnConsumeEntity> list = cdnConsumeDao.selectList(new QueryWrapper<CdnConsumeEntity>()
                        .eq("serial_number", suit.getSerialNumber())
                        .eq("attr_name", ProductAttrNameEnum.ATTR_FLOW.getAttr())
                        .ge("start_time", new Long(curDateLastStartDate.getTime() / 1000).intValue())
                        .le("end_time", new Long(curDateLastEndDate.getTime() / 1000).intValue())
                        .gt("s_value", 0l)
                        .select("s_value")
                        .eq("status", 1));
                Long f_value = 0L;
                for (CdnConsumeEntity c : list) {
                    f_value += c.getSValue();
                }
                double flow_f = f_value / (ONE_THOUSAND * ONE_THOUSAND * ONE_THOUSAND);
                // result.put(ProductAttrNameEnum.ATTR_FLOW.getAttr(),flow_f);
                result.setFlow(Double.valueOf(flow_f).longValue());
            }

        }
        // suit.setConsume(DataTypeConversionUtil.entity2jsonObj(result) );
        suit.setConsume(result);

    }

    /**
     * 套餐列表更新属性
     * 
     * @param suit
     */
    private void simpleUpdateSuit(CdnSuitEntity suit) {
        // 0 增值流量
        Long addFlow = 0l;
        List<CdnSuitEntity> list = cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number", suit.getSerialNumber())
                .eq("suit_type", OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())
                .le("start_time", new Date())
                .ge("end_time", new Date())
                .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                .select("flow"));
        for (CdnSuitEntity item : list) {
            addFlow += item.getFlow();
        }
        if (null == suit.getAttr() && StringUtils.isNotBlank(suit.getAttrJson())) {
            // suit.setAttr(DataTypeConversionUtil.string2Json(suit.getAttrJson()));
            suit.setAttr(DataTypeConversionUtil.string2Entity(suit.getAttrJson(), ProductAttrVo.class));
        }
        suit.getAttr().setFlow(suit.getAttr().getFlow() + addFlow);
        suit.setFlow(suit.getAttr().getFlow());
        // suit.setFlow(suit.getAttr().getLong(ProductAttrNameEnum.ATTR_FLOW.getAttr()));
        CdnProductEntity suitProduct = getProductEntityBySuit(suit);
        suit.setProductEntity(suitProduct);
        suit.setProduct(suit.getProductEntity());
        // 1 更新已使用量
        this.alreadyFlowConsumeInfo(suit);
        commonTaskService.updateUsedFlow(suit);

        // 2 更新到期时间
        Integer[] sInList = { OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(), OrderTypeEnum.ORDER_CDN_RENEW.getTypeId() };
        if (Arrays.asList(sInList).contains(suit.getSuitType())) {
            Integer[] sInStatusList = { CdnSuitStatusEnum.NORMAL.getId() };
            CdnSuitEntity endSuit = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                    .eq("serial_number", suit.getSerialNumber())
                    .in("suit_type", sInList)
                    .in("status", sInStatusList)
                    .select("end_time")
                    .orderByDesc("end_time")
                    .last("limit 1"));
            if (null != endSuit) {
                suit.setEndTime(endSuit.getEndTime());
            }
        }
        // 3 更新用户名
        TbUserEntity user = userDao
                .selectOne(new QueryWrapper<TbUserEntity>().eq("user_id", suit.getUserId()).select("username"));
        if (null != user) {
            suit.setUsername(user.getUsername());
        }

    }

    @Override
    public CdnSuitEntity getSuitDetailBySuitId(Integer id) {
        CdnSuitEntity suitEntity = suitDao.selectById(id);
        if (null != suitEntity) {
            this.updateCdnSuit(suitEntity, S_MODE_IN_USE);
            return suitEntity;
        }
        return null;
    }

    @Override
    public R allSuitListByUser(Long userId, Integer all) {
        List<SimpleSuitInfoVo> resultList = new ArrayList<>();
        Integer[] statusArray = { CdnSuitStatusEnum.NORMAL.getId() };
        if (null != all && 1 == all) {
            statusArray = CdnSuitStatusEnum.getAllStatus().stream().toArray(Integer[]::new);
        }
        List<CdnSuitEntity> list = suitDao.selectLatestByUser(userId, OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),
                statusArray);
        list.forEach(item -> {
            SimpleSuitInfoVo sVo = new SimpleSuitInfoVo();
            sVo.setId(item.getId());
            simpleUpdateSuit(item);
            sVo.setSerial_number(item.getSerialNumber());
            if (null != item.getProductEntity()) {
                sVo.setProductName(item.getProductEntity().getName());
            }
            if (null != item.getEndTime()) {
                sVo.setEndTime(item.getEndTime().getTime());
            }
            if (null != item.getAttr()) {
                if (null != item.getConsume()) {
                    sVo.setUsedFlow(item.getConsume().getFlow());
                    sVo.setTotalFlow(item.getAttr().getFlow());
                }
                if (null != item.getAttr().getSite()) {
                    Long count = siteDao
                            .selectCount(new QueryWrapper<TbSiteEntity>().eq("serial_number", sVo.getSerial_number()));
                    sVo.setBindSiteSum(count + "/" + item.getAttr().getSite());
                }
            }
            sVo.setStatus(item.getStatus());
            sVo.setStatusMsg(CdnSuitStatusEnum.statusMsg(item.getStatus()));
            resultList.add(sVo);

        });
        return R.ok().put("data", resultList);
    }

    @Override
    public boolean cancellationSuit(Long userId, String serialNumber) {
        // 注销套餐->{套餐 续费 增值}
        Long count = cdnSuitDao.selectCount(new QueryWrapper<CdnSuitEntity>().eq(null != userId, "user_id", userId)
                .eq("serial_number", serialNumber));
        if (0 == count) {
            return false;
        }
        // 套餐绑定有站点 forward 则失败
        Long count1 = siteDao.selectCount(new QueryWrapper<TbSiteEntity>().eq("serial_number", serialNumber));
        if (count1 > 0) {
            throw new RRException("注销失败，已绑定了站点的套餐不可注销");
        }
        Long count2 = tbStreamProxyDao
                .selectCount(new QueryWrapper<TbStreamProxyEntity>().eq("serial_number", serialNumber));
        if (count2 > 0) {
            throw new RRException("注销失败，已绑定了四层转发的套餐不可注销");
        }
        List<CdnSuitEntity> list = cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                .eq(null != userId, "user_id", userId)
                .eq("serial_number", serialNumber)
                .ne("status", CdnSuitStatusEnum.CANCELLATION.getId()));
        for (CdnSuitEntity suit : list) {
            Integer refundAmount = 0;
            if (suit.getEndTime().before(new Date())) {
                suit.setStatus(CdnSuitStatusEnum.DISABLE.getId());
                cdnSuitDao.updateById(suit);
                continue;
            }
            // 1 注销原套餐
            suit.setStatus(CdnSuitStatusEnum.CANCELLATION.getId());
            cdnSuitDao.updateById(suit);
            // 2 获取支付订单
            TbOrderEntity payOrderEntity = tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("serial_number", suit.getPaySerialNumber())
                    .eq("status", PayStatusEnum.PAY_COMPLETE.getId())
                    .last("limit 1"));
            if (null != payOrderEntity) {
                // 3 计算剩余价格
                if (suit.getStartTime().before(new Date())) {
                    // Integer
                    // total_month=DateUtils.getIntervalMonth(suit.getStartTime(),suit.getEndTime());
                    // Integer surplus_month=DateUtils.getIntervalMonth(new
                    // Date(),suit.getEndTime());
                    // float refundAmountF=payOrderEntity.getPayable()*surplus_month/total_month;
                    Long totalTm = suit.getEndTime().getTime() - suit.getStartTime().getTime();
                    Long surplusTm = suit.getEndTime().getTime() - new Date().getTime();
                    if (totalTm < 1) {
                        refundAmount = 0;
                    } else {
                        float refundAmountF = payOrderEntity.getPayable() * surplusTm / totalTm;
                        refundAmount = Math.round(refundAmountF);
                    }

                } else {
                    refundAmount = payOrderEntity.getPayable();
                }
                JSONObject payJson = new JSONObject();
                if (StringUtils.isNotBlank(payOrderEntity.getPayJson())) {
                    payJson.putAll(DataTypeConversionUtil.string2Json(payOrderEntity.getPayJson()));
                }
                // 4 更新订单为已退款
                payJson.put("refund_source_obj", suit);
                payJson.put("refund_date", new Date());
                payJson.put("refund_amount", refundAmount);
                payOrderEntity.setPayJson(payJson.toJSONString());
                payOrderEntity.setStatus(PayStatusEnum.PAY_REFOUND_COMPLETE.getId());
                tbOrderDao.updateById(payOrderEntity);
                // 5 返还
                TbUserEntity userEntity = userDao.selectById(payOrderEntity.getUserId());
                if (null != userEntity) {
                    if (null == userEntity.getPropertyBalance()) {
                        userEntity.setPropertyBalance(0);
                    }
                    userEntity.setPropertyBalance(userEntity.getPropertyBalance() + refundAmount);
                    userDao.updateById(userEntity);
                }
            }

            // 6 清理
            Map<String, String> map = new HashMap<>();
            map.put(PushTypeEnum.CLEAN_CLOSE_SUIT_SERVICE.getName(), suit.getSerialNumber());
            cdnMakeFileService.pushByInputInfo(map);
        }
        return true;
    }

    @Override
    public void liquidationSuit(Long userId, String serialNumber) {
        commonTaskService.stopUsePostpaidSuit(userId, serialNumber);
    }

    @Override
    public R user_bytes_detail(QuerySuitBytes querySuitBytes) {
        return R.ok().put("data",
                commonTaskService.getBytesDataListBySuitSerialNumber(null, querySuitBytes.getSerialNumber(),
                        DateUtils.LongStamp2Date(querySuitBytes.getStartTime()),
                        DateUtils.LongStamp2Date(querySuitBytes.getEndTime())));
    }

    @Override
    public Integer updateSuitStatus(String SerialNumber, Integer status) {
        UpdateWrapper<CdnSuitEntity> updateWrapper = new UpdateWrapper<>();
        Integer[] canModify = { CdnSuitStatusEnum.NORMAL.getId(), CdnSuitStatusEnum.UNKNOWN.getId() };
        updateWrapper.in("status", canModify).eq("serial_number", SerialNumber).set("status", status);
        Integer resInt = suitDao.update(null, updateWrapper);
        if (CdnSuitStatusEnum.UNKNOWN.getId().equals(status)) {
            Map<String, String> map = new HashMap<>();
            map.put(PushTypeEnum.CLEAN_CLOSE_SUIT_SERVICE.getName(), SerialNumber);
            cdnMakeFileService.pushByInputInfo(map);
        } else if (CdnSuitStatusEnum.NORMAL.getName().equals(status)) {
            Map<String, String> map = new HashMap<>();
            map.put(PushTypeEnum.PUSH_SUIT_SERVICE.getName(), SerialNumber);
            cdnMakeFileService.pushByInputInfo(map);
        }
        return resInt;
    }

    @Override
    public List<String> getSuitBind(Long userId, String SerialNumber, Integer mode) {
        List<String> list = new ArrayList<>();
        if (1 == mode) {
            List<TbSiteEntity> ls = siteDao.selectList(new QueryWrapper<TbSiteEntity>()
                    .eq("serial_number", SerialNumber)
                    .eq(null != userId, "user_id", userId)
                    .select("main_server_name"));
            list.addAll(ls.stream().map(t -> t.getMainServerName()).collect(Collectors.toList()));
        } else if (2 == mode) {
            List<TbStreamProxyEntity> ls = tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
                    .eq("serial_number", SerialNumber)
                    .eq(null != userId, "user_id", userId)
                    .select("conf_info"));
            list.addAll(ls.stream().map(t -> t.getConfInfo()).collect(Collectors.toList()));
        }
        return list;
    }

    private String traceName(String str) {
        // String str = "my_variable_name";
        String[] parts = str.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        String camelCaseStr = sb.toString();
        // System.out.println(camelCaseStr);
        return camelCaseStr;
    }

    @Override
    public void reInitDbColumns() {
        final String[] columns = { "flow", "site", "charging_mode", "custom_dns", "private_waf" };
        List<CdnSuitEntity> list = cdnSuitDao.selectList(
                new QueryWrapper<CdnSuitEntity>().isNull("flow").eq("status", CdnSuitStatusEnum.NORMAL.getId()));
        for (CdnSuitEntity suit : list) {
            String attrJsonStr = suit.getAttrJson();
            suit.setFlow(0L);
            if (StringUtils.isNotBlank(attrJsonStr)) {
                try {
                    JSONObject jObj = JSONObject.parseObject(attrJsonStr);
                    // {"live_data":"1","charging_mode":"2","bandwidth":333300,"custom_dns":"1","private_waf":"1","monitor":"1","port_forwarding":5,"public_waf":"1","dd_defense":100,"site":1000,"defense":100,"service":"7x24专属服务","sms":100,"flow":"10000","ai_waf":"1"}
                    for (String c : columns) {
                        if (jObj.containsKey(c)) {
                            String fieldName = DataTypeConversionUtil.traceName(c);
                            Field field = suit.getClass().getDeclaredField(fieldName);
                            if (null != field) {
                                field.setAccessible(true);
                                if (field.getType() == int.class) {
                                    field.setInt(suit, Integer.parseInt(jObj.get(c).toString()));
                                } else if (field.getType() == long.class) {
                                    field.setLong(suit, Long.parseLong(jObj.get(c).toString()));
                                } else if (field.getType() == String.class) {
                                    field.set(suit, jObj.get(c).toString());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cdnSuitDao.updateById(suit);
        }
    }

}
