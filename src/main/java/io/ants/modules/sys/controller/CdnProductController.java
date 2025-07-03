package io.ants.modules.sys.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.dao.TbOrderDao;
import io.ants.modules.app.entity.TbOrderEntity;
import io.ants.modules.app.form.PageSimpleForm;
import io.ants.modules.app.form.QuerySuitBytes;
import io.ants.modules.app.form.SubmitOrderForm;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.sys.dao.CdnProductDao;
import io.ants.modules.sys.dao.CdnSuitDao;
import io.ants.modules.sys.entity.CdnProductAttrEntity;
import io.ants.modules.sys.entity.CdnProductEntity;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.enums.CdnSuitStatusEnum;
import io.ants.modules.sys.enums.OrderTypeEnum;
import io.ants.modules.sys.enums.ProductAttrNameEnum;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.form.QueryCdnProductForm;
import io.ants.modules.sys.service.CdnSuitService;
import io.ants.modules.sys.vo.ProduceMiniVo;
import io.ants.modules.sys.vo.ProductAttrItemVo;
import io.ants.modules.sys.vo.ProductAttrVo;
import io.ants.modules.sys.vo.TbOrderInitVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/cdn/suit")
public class CdnProductController extends AbstractController {

    @Autowired
    private CdnSuitService cdnSuitService;
    @Autowired
    private CdnProductDao productDao;
    @Autowired
    private TbUserService userService;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private TbOrderDao tbOrderDao;

    @PostMapping("/product/list")
    @PreAuthorize("hasAuthority('sys:product:list')")
    public R productList(@RequestBody QueryCdnProductForm form){
       form.setUserType(UserTypeEnum.MANAGER_TYPE.getId());
       form.setVendibility(null);
       return R.ok().put("data",cdnSuitService.getProductList(form));

    }

    @GetMapping("/product/all")
    public R productAll(@RequestParam String productTypes){
        return R.ok().put("data",cdnSuitService.getAllProductByType(productTypes));
    }

    @PostMapping("/product/save")
    @PreAuthorize("hasAuthority('sys:product:save')")
    public R productSave(@RequestBody Map<String, Object> params){
       return R.ok().put("data",cdnSuitService.saveProduct(params));

    }

    @SysLog("保存产品")
    @GetMapping("/product/delete")
    @PreAuthorize("hasAuthority('sys:product:save')")
    public R productDelete(@RequestParam Integer id){
        return R.ok().put("data",cdnSuitService.deleteProduct(id));
    }

    @PostMapping("/productAttr/list")
    @PreAuthorize("hasAuthority('sys:product:list')")
    public R productAttrList(@RequestBody PageSimpleForm form){
       return R.ok().put("data",cdnSuitService.getProductAttrList(form));
    }

    @GetMapping("/productAttr/all")
    @PreAuthorize("hasAuthority('sys:product:list')")
    public R productAttrAll(){
        return  R.ok().put("data",cdnSuitService.getAllProductAttr());
    }

    @GetMapping("/product/attr/object")
    public R productAttrAllObj(){
        return  R.ok().put("data",cdnSuitService.getProductAttrObj());
    }

    @SysLog("保存产品属性")
    @PostMapping("/productAttr/save")
    @PreAuthorize("hasAuthority('sys:product:save')")
    public R productAttr_save(@RequestBody Map<String, Object> params){
        CdnProductAttrEntity attrEntity= cdnSuitService.SaveProductAttr(params);
        return  R.ok().put("data",attrEntity);
    }

    @SysLog("删除产品属性")
    @GetMapping("/productAttr/delete")
    @PreAuthorize("hasAuthority('sys:product:save')")
    public R productAttrDelete(@RequestParam Integer id){
        return R.ok().put("data",cdnSuitService.deleteProductAttr(id));
    }

    @PostMapping("/order/list")
    @PreAuthorize("hasAuthority('sys:order:list')")
    public R orderList(@RequestBody Map<String, Object> params){
        if(params.containsKey("user")){
            String user=params.get("user").toString();
            if(StringUtils.isNotBlank(user)){
                String  UserIds= userService.key2userIds(user);
                params.put("userIds",UserIds);
            }
        }
        return R.ok().put("data",cdnSuitService.orderList(params));
    }

    @SysLog("删除订单")
    @GetMapping("/order/delete")
    @PreAuthorize("hasAuthority('sys:order:save')")
    public R orderDelete(@RequestParam Integer id){
          tbOrderDao.update(null,new UpdateWrapper<TbOrderEntity>().eq("id",id).set("is_delete",1));
          return R.ok();
    }

    @PostMapping("/order/create")
    @PreAuthorize("hasAuthority('sys:order:save')")
    public R orderCreate(@RequestBody SubmitOrderForm submitOrderForm){
        return  cdnSuitService.createOrder(submitOrderForm);
    }

    @PostMapping("/suit/list")
    @PreAuthorize("hasAuthority('sys:order:list')")
    public R suitList(@RequestBody Map<String, Object> params){
        //{\"page\":1,\"limit\":10,\"serialNumber\":\"1\",\"user\":\"test\",\"startTime\":0,\"endTime\":1950769152,\"mode\":1}
        //SuitListForm
        if (params.containsKey("user")){
            String userIds=userService.key2userIds(params.get("user").toString());
            params.put("userIds",userIds);
        }
        return R.ok().put("data",cdnSuitService.suitList(params));
    }


    @PostMapping("/suit/listbyuser")
    public R listByUser(@RequestBody Map<String, Object> params){
        if(params.containsKey("userId")){
            int all=0;
            if (params.containsKey("all")){
                all=Integer.parseInt(params.get("all").toString());
            }
            return cdnSuitService.allSuitListByUser(Long.parseLong(params.get("userId").toString()),all);
        }
        return R.error("参数缺失[listByUser]！");
    }

    @GetMapping("/suit/detail/id")
    public R getSuitDitail(@RequestParam Integer id) {
        return R.ok().put("data",cdnSuitService.getSuitDetailBySuitId(id));
    }

    @GetMapping("/suit/detail")
    public R suitDetail(@RequestParam String serialNumber){
        //Long userId,String serialNumber
        CdnSuitEntity obj=cdnSuitService.getSuitDetailBySerial(null,serialNumber,true,true);
        if(null==obj){
            return R.error("订单号错误！");
        }
        return R.ok().put("data",obj);
    }


    @SysLog("修改用户套餐属性")
    @PostMapping("/suit/attr/update")
    @PreAuthorize("hasAuthority('sys:order:save')")
    public R userSuitUpdateAttr(@RequestBody Map<String, Object> params){
        String serialNumber=null;
        if (params.containsKey("serialNumber") ){
            serialNumber=params.get("serialNumber").toString();
        }

        Integer suiteId=null;
        if (params.containsKey("id")){
            suiteId=Integer.parseInt(params.get("id").toString());
        }
        if (null==suiteId){
            return R.error("id 不能为空");
        }
        final Integer[] suitTypeLs={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_ADDED.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        CdnSuitEntity suitEntity=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq(null!=suiteId,"id",suiteId)
                .eq(StringUtils.isNotBlank(serialNumber),"serial_number",serialNumber)
                .in("suit_type", suitTypeLs)
                .orderByDesc("id")
                .last("limit 1")
        );
        if (null==suitEntity){
            return R.error("获取套餐失败！");
        }
        JSONObject newSuitAttrJson=null;
        if (params.containsKey("attrJson")){
            //修改套餐属性
            //{"price_obj":"{\"value\":100,\"status\":1}","buy_obj":{"serialNumber":"1679018351806002","sum":1,"startTime":1681272308115,"type":"m"},"product_obj":"{\"createtime\":1659499295000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":100,\\\"status\\\":1}}\",\"name\":\"流量月包\",\"serverGroupIds\":\"\",\"attrJson\":\"[{\\\"attr\\\":\\\"flow\\\",\\\"id\\\":58,\\\"name\\\":\\\"流量\\\",\\\"valueType\\\":\\\"int\\\",\\\"unit\\\":\\\"G\\\",\\\"value\\\":204000}]\",\"weight\":2,\"id\":18,\"productType\":12,\"status\":1}"}
            String attrJson=params.get("attrJson").toString();
            JSONObject oldSuitAttrJson= DataTypeConversionUtil.string2Json(suitEntity.getAttrJson());
            newSuitAttrJson=DataTypeConversionUtil.string2Json(attrJson);
            for (String attrKey: newSuitAttrJson.keySet()){
                ProductAttrNameEnum item= ProductAttrNameEnum.getEnumObjByAttr(attrKey);
                if (null==item){
                    return R.error(attrKey+" is unknown!");
                }
                oldSuitAttrJson.put(attrKey,newSuitAttrJson.get(attrKey));

            }
            ProductAttrVo attrVo=new ProductAttrVo();
            ProductAttrVo.updateAttrVoFromString(attrVo,attrJson);
            suitEntity.setAttrJson(oldSuitAttrJson.toJSONString());
            ProductAttrVo.updateSuitAttrByAttrVo(suitEntity,attrVo);
            cdnSuitDao.updateById(suitEntity);
        }
        if (params.containsKey("endTime")){
            String endDate=params.get("endTime").toString();
            Date newDate= DateUtils.stringToDate(endDate,DateUtils.DATE_TIME_PATTERN);
            suitEntity.setEndTime( new java.sql.Date(newDate.getTime()));
            cdnSuitDao.updateById(suitEntity);
        }
        if (params.containsKey("productJson") && null!=params.get("productJson")){
            String updateProductString =params.get("productJson").toString();
            TbOrderEntity targetOrder=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("serial_number",serialNumber)
                    .last("limit 1")
            );
            if (null!=targetOrder){
                TbOrderInitVo tbOrderInitVo=DataTypeConversionUtil.string2Entity(targetOrder.getInitJson(), TbOrderInitVo.class);
                if (null!=tbOrderInitVo && null!=tbOrderInitVo.getProduct_obj()){
                    String tProductJsonStr=tbOrderInitVo.getProduct_obj().toString();
                    logger.warn(tProductJsonStr);
                    ProduceMiniVo tProductEntity=DataTypeConversionUtil.string2Entity(tProductJsonStr,ProduceMiniVo.class);
                    if (null!=tProductEntity && null!=tProductEntity.getProductJson()){
                        //tProductJson.put("productJson",updateProductString);
                        //更新product
                        tProductEntity.setProductJson(updateProductString);

                    }
                    if(null!=newSuitAttrJson){
                        //更新attrJson
                        JSONArray attrJsonArray=new JSONArray();
                        for (String attrKey: newSuitAttrJson.keySet()){
                            ProductAttrItemVo vo=new ProductAttrItemVo();
                            vo.setAttr(attrKey);
                            ProductAttrNameEnum enumItem=ProductAttrNameEnum.getEnum(attrKey);
                            if (null!=enumItem){
                                vo.setName(enumItem.getName());
                                vo.setValueType(enumItem.getType());
                                vo.setUnit(enumItem.getSuffix());
                                vo.setValue(newSuitAttrJson.get(attrKey));
                            }
                            attrJsonArray.add(DataTypeConversionUtil.entity2json(vo));
                        }
                        tProductEntity.setAttrJson(attrJsonArray.toJSONString());
                    }
                    tbOrderInitVo.setProduct_obj(DataTypeConversionUtil.entity2jsonObj(tProductEntity));
                    String initJsonStr=DataTypeConversionUtil.entity2jonsStr(tbOrderInitVo);
                    if (StringUtils.isNotBlank(initJsonStr)){
                        targetOrder.setInitJson(initJsonStr);
                        tbOrderDao.updateById(targetOrder);
                    }

                }
            }
        }
        return R.ok();
    }


    private Integer getChargingMode(JSONArray attrjsonarray){
        for (int i = 0; i < attrjsonarray.size(); i++) {
            JSONObject attr=attrjsonarray.getJSONObject(i);
            if(attr.containsKey("attr") && attr.containsKey("value")&& "charging_mode".equals(attr.getString("attr"))){
                return  attr.getInteger("value");
            }
        }
        return 1;
    }

    /**
     * 创建后付费套餐
     */
    @PostMapping("/suit/create/postpaid")
    @PreAuthorize("hasAuthority('sys:order:save')")
    public R postpaid_create(@RequestBody Map param){
        if(!param.containsKey("userId") || !param.containsKey("productId")){
            return R.error("缺少参数【userId】【productId】");        }
        //{"userId":61,"orderType":10,"targetId":1,"initJson":"{\"type\":\"m\",\"sum\":1}"}
        SubmitOrderForm submitOrderForm=new SubmitOrderForm();
        submitOrderForm.setUserId(Long.parseLong(param.get("userId").toString()));
        submitOrderForm.setOrderType( OrderTypeEnum.ORDER_CDN_SUIT.getTypeId());
        submitOrderForm.setTargetId(Integer.parseInt(param.get("productId").toString()));
        submitOrderForm.setInitJson("");
        CdnProductEntity product=productDao.selectById(param.get("productId").toString());
        if(null==product){
            return R.error("不存在的产品！");
        }
        JSONArray attrJSONarray= DataTypeConversionUtil.string2JsonArray(product.getAttrJson());
        Integer chargingModeValue= getChargingMode(attrJSONarray);
        if(2==chargingModeValue || 3==chargingModeValue){
            return cdnSuitService.createOrder(submitOrderForm);
        }else {
            return R.error("非后付费产品");
        }
    }

    @SysLog("注销套餐")
    @GetMapping("/suit/cancellation")
    @PreAuthorize("hasAuthority('sys:order:save')")
    public R cancellationSuit( @RequestParam String SerialNumber){
        boolean result= cdnSuitService.cancellationSuit(null,SerialNumber);
        if (result){
            return R.ok();
        }else {
            return R.error("失败！");
        }
    }

    @SysLog("清算后付费套餐")
    @GetMapping("/suit/liquidation")
    @PreAuthorize("hasAuthority('sys:order:save')")
    public R suit_liquidation(@RequestParam String SerialNumber){
       cdnSuitService.liquidationSuit(null,SerialNumber);
       return R.ok();
    }

    @SysLog("修改用户套餐状态")
    @PostMapping("/suit/status/modify")
    public R modifyUserSuit(@RequestBody Map param){
        if (!param.containsKey("SerialNumber") || !param.containsKey("status")){
            return R.error("【SerialNumber】【status】为空");
        }
        Integer[] canModify={CdnSuitStatusEnum.NORMAL.getId(),CdnSuitStatusEnum.UNKNOWN.getId()};
        Integer status= Integer.parseInt(param.get("status").toString());
        if (!Arrays.asList(canModify).contains(status)){
            return R.error("修改失败,仅可修改为【可用】或【不可用】");
        }
        Integer result=cdnSuitService.updateSuitStatus(param.get("SerialNumber").toString(),status);
        if (0==result){
            return R.error("修改失败");
        }
        return R.ok().put("data",result);
    }

    @PostMapping("/user/suit/bytes/detail")
    public R suitBytesDetail(@RequestBody QuerySuitBytes querySuitBytes){
        return cdnSuitService.user_bytes_detail(querySuitBytes);
    }



}
