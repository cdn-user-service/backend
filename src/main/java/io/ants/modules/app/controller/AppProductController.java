package io.ants.modules.app.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.ants.common.annotation.UserLog;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.dao.TbOrderDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbOrderEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.QuerySuitBytes;
import io.ants.modules.app.form.SubmitOrderForm;
import io.ants.modules.app.form.SuitListForm;
import io.ants.modules.app.form.UserCertifyForm;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.sys.dao.CdnProductDao;
import io.ants.modules.sys.dao.SysConfigDao;
import io.ants.modules.sys.dao.TbPayRecordDao;
import io.ants.modules.sys.entity.CdnProductEntity;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.entity.TbPayRecordEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.QueryCdnProductForm;
import io.ants.modules.sys.service.CdnSuitService;
import io.ants.modules.sys.service.CommonTaskService;
import io.ants.modules.sys.service.PayService;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.cccpay.CccYunSubmitPayForm;
import io.ants.modules.utils.factory.AllinpayFactory;
import io.ants.modules.utils.factory.CccyunPayFactory;
import io.ants.modules.utils.factory.TokenPayFactory;
import io.ants.modules.utils.factory.alipay.AlipayUserCertifyFactory;
import io.ants.modules.utils.factory.tencent.TencentUserCertifyFactory;
import io.ants.modules.utils.service.alipay.AlipayUserCertifyService;
import io.ants.modules.utils.service.allinpay.AllinpaySybPayService;
import io.ants.modules.utils.service.tencent.TencentUserCertifyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/app/product/")
@Api(tags = "套餐与财务管理")
public class AppProductController {

    private final int ALIPAY_AUTH_MODE=1;
    private final int WECHATPAY_AUTH_MODE=2;
    final long TIME_OUT_VALUE=24L*60*60*1000;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private CdnSuitService cdnSuitService;
    @Autowired
    private TbUserService userService;
    @Autowired
    private SysConfigDao sysConfigDao;
    @Autowired
    private CdnProductDao productDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private CommonTaskService commonTaskService;
    @Autowired
    private PayService payService;
    @Autowired
    private TbPayRecordDao tbRecordDao;
    @Autowired
    private TbUserDao tbUserDao;

    @Login
    @PostMapping("/suit/list")
    @ApiOperation("我购买的套餐[分页]")
    public R suitList(@ApiIgnore @RequestAttribute("userId") Long userId, @RequestBody SuitListForm form){
        form.setUserIds(userId.toString());
        Map params= DataTypeConversionUtil.entity2map(form);
        if (null!=params){
            PageUtils pageUtils=cdnSuitService.suitList(params);
            return R.ok().put("data",pageUtils);
        }
        return R.ok();
    }

    @Login
    @GetMapping("/suit/listbyuser")
    @ApiOperation("我购买的套餐[不分页]")
    public R listByUser(@ApiIgnore @RequestAttribute("userId") Long userId,Integer all){
        return  cdnSuitService.allSuitListByUser(userId,all);
    }

    @GetMapping("/by_access/suit/listbyuser")
    @ApiOperation("access我购买的套餐[不分页]")
    public R listByUser(@RequestParam String access_token){
        if (!StringUtils.isNotBlank(access_token)){
            return R.error("token is empty!");
        }
        TbUserEntity userEntity=tbUserDao.selectOne(new QueryWrapper<TbUserEntity>()
                .eq("u_cdn_access_token",access_token)
                .select("user_id")
                .last("limit 1")
        );
        if (null==userEntity){
            return R.error("token error!");
        }
        return cdnSuitService.allSuitListByUser(userEntity.getUserId(),0);
    }

    @Login
    @GetMapping("/suit/detail")
    @ApiOperation("套餐详情")
    public R suitDetail(@ApiIgnore @RequestAttribute("userId") Long userId, @RequestParam String serialNumber){
        CdnSuitEntity obj=cdnSuitService.getSuitDetailBySerial(userId,serialNumber,true,true);
        if(null==obj){
            return R.error("订单号错误！");
        }
        return R.ok().put("data",obj);
    }

    @Login
    @PostMapping("/order/list")
    @ApiOperation("订单列表")
    public R order_list(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody Map<String, Object> params){
        params.put("userIds",userId.toString());
        return R.ok().put("data",cdnSuitService.orderList(params));
    }

    @Login
    @GetMapping("/order/delete")
    @UserLog("订单删除")
    public R orderDelete(@RequestParam Integer id){
        tbOrderDao.update(null,new UpdateWrapper<TbOrderEntity>().eq("id",id).set("is_delete",1));
        return R.ok();
    }

    @PostMapping("/product/list")
    @ApiOperation("可购套餐列表")
    public R product_list(@RequestBody QueryCdnProductForm form){
        form.setUserType(UserTypeEnum.USER_TYPE.getId());
        form.setVendibility("1");
        form.setProductTypes(String.valueOf(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId()) );
        return R.ok().put("data",cdnSuitService.getProductList(form));

    }


    @GetMapping("/product/all")
    @ApiOperation("根据类型获取所有可购套餐")
    public R product_all(@RequestParam String productTypes){
        return R.ok().put("data",cdnSuitService.getAllProductByType(productTypes));
    }

    @GetMapping("/product/attr/object")
    @ApiOperation("获取套餐属性")
    public R productAttr_all_obj(){
        return  R.ok().put("data",cdnSuitService.getProductAttrObj());
    }

    @Login
    @PostMapping("/order/create")
    @ApiOperation("创建订单")
    @UserLog("创建订单")
    public R order_create(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody SubmitOrderForm form){
        form.setUserId(userId);
        return  cdnSuitService.createOrder(form);
    }

    @Login
    @GetMapping("/stripe/payment/link/create")
    @ApiOperation("创建stripe支付链接")
    @UserLog("创建stripe支付链接")
    public R paymentLinkCreate(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam String serialNumber,@RequestParam String payMethod ){
        return R.error("404");
    }

    @Login
    @GetMapping("/allinpay/pay/create")
    @ApiOperation("创建allinpay")
    @UserLog("创建allinpay")
    public R allinpayCreate(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam String serialNumber,@RequestParam String payMethod ){
        String eMsg="allinpayCreate fail";
        try{
            if (StringUtils.isBlank(serialNumber)){
                return R.error("serialNumber is null");
            }
            TbOrderEntity orderEntity=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("user_id",userId)
                    .eq("serial_number",serialNumber)
                    .last("limit 1")
            );
            if (null==orderEntity){
                return R.error("serialNumber error");
            }

            AllinpaySybPayService service = AllinpayFactory.build();
            //W01: 微信扫码支付       A01: 支付宝扫码支付   S01:数币扫码支付
            String payType="A01";
            if ("wechat".equals(payMethod)){
                payType="W01";
            }
            Map<String, String> map = service.pay(orderEntity.getPayable(),serialNumber,payType);
            logger.info(map.toString());
            return R.ok().put("data",map);
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    @Login
    @GetMapping("/tokenpay/create")
    @ApiOperation("创建tokenpay支付")
    @UserLog("创建tokenpay支付")
    public R tokenpayCreate(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam String serialNumber,@RequestParam String current ){
        if (StringUtils.isBlank(serialNumber)){
            return R.error("serialNumber is null");
        }
        TbOrderEntity orderEntity=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("user_id",userId)
                .eq("serial_number",serialNumber)
                .last("limit 1")
        );
        if (null==orderEntity){
            return R.error("serialNumber error");
        }
        double yun=(double) orderEntity.getPayable()/100.00;
        return TokenPayFactory.build().sendOrder("cdn",yun,serialNumber,current);
    }


    @Login
    @GetMapping("/cccyun/create")
    @ApiOperation("创建彩虹易支付")
    @UserLog("创建彩虹易支付")
    public R cccyunPayCreate(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam String serialNumber,@RequestParam String payMethod){
        if (StringUtils.isBlank(serialNumber)){
            return R.error("serialNumber is null");
        }
        TbOrderEntity orderEntity=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("user_id",userId)
                .eq("serial_number",serialNumber)
                .last("limit 1")
        );
        if (null==orderEntity){
            return R.error("serialNumber error");
        }
        CccYunSubmitPayForm form=new CccYunSubmitPayForm();
        form.setType(payMethod);
        form.setOut_trade_no(serialNumber);
        form.setName("cdn");
        form.setAmount(orderEntity.getPayable());
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip= IPUtils.getIpAddr(request);
        form.setClientip(ip);
        return CccyunPayFactory.build().pay(form);
    }


    private static boolean isJSON(String str) {
        boolean result = false;
        try {
            JSON.parse(str);
            result = true;
        } catch (Exception e) {
            result=false;
        }
        return result;
    }

    private Integer getUserAuthMode(String orderSerialNumber){
        SysConfigEntity sysConfig=sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().and(q->q.eq("param_key",ConfigConstantEnum.ALIPAYUSERCERTIFY_CONFIG_KEY.getConfKey()).or().eq("param_key",ConfigConstantEnum.TENCENTUSERCERTIFY_CONFIG_KEY.getConfKey())).eq("status",1).last("limit 1"));
        if (null==sysConfig){
            throw new RRException("实名未开启--[1]！");
        }

        TbPayRecordEntity record=tbRecordDao.selectOne(new QueryWrapper<TbPayRecordEntity>().eq("serial_number",orderSerialNumber).last("limit 1"));
        if (null!=record){
            if (record.getPayType().equals(PayTypeEnum.PRO_TYPE_alipay.getId())){
                //alipay 支付优先alipay 验证
                if (sysConfig.getParamKey().equals(ConfigConstantEnum.ALIPAYUSERCERTIFY_CONFIG_KEY.getConfKey())){
                    return ALIPAY_AUTH_MODE;
                }else if(sysConfig.getParamKey().equals(ConfigConstantEnum.TENCENTUSERCERTIFY_CONFIG_KEY.getConfKey())){
                    return WECHATPAY_AUTH_MODE;
                }
            }else if(record.getPayType().equals(PayTypeEnum.PRO_TYPE_wechat.getId())){
                //wechat pay 支付优先wechat 验证
                if(sysConfig.getParamKey().equals(ConfigConstantEnum.TENCENTUSERCERTIFY_CONFIG_KEY.getConfKey())){
                    return WECHATPAY_AUTH_MODE;
                }else if (sysConfig.getParamKey().equals(ConfigConstantEnum.ALIPAYUSERCERTIFY_CONFIG_KEY.getConfKey())){
                    return ALIPAY_AUTH_MODE;
                }
            }else{
                if (sysConfig.getParamKey().equals(ConfigConstantEnum.ALIPAYUSERCERTIFY_CONFIG_KEY.getConfKey())){
                    return ALIPAY_AUTH_MODE;
                }else if(sysConfig.getParamKey().equals(ConfigConstantEnum.TENCENTUSERCERTIFY_CONFIG_KEY.getConfKey())){
                    return WECHATPAY_AUTH_MODE;
                }
            }
        }



        return -9;
    }

    //进入ali|wechat 实名
    private R userAuthHandle(String orderSerialNumber, String name, String uid) {
        Map<String,Object> map =new HashMap<>();
        Integer mode=getUserAuthMode(orderSerialNumber);
        if(ALIPAY_AUTH_MODE==mode){
            //使用alipay-AUTH
            AlipayUserCertifyService aliUucs= AlipayUserCertifyFactory.build();
            try{
                R r1=aliUucs.certifyInitialize(orderSerialNumber,name,uid," ");
                if (1==r1.getCode() && r1.containsKey("data")){
                    String certify_id=r1.get("data").toString();
                    if( StringUtils.isNotBlank(certify_id)){
                        R r2 =aliUucs.certify(certify_id);
                        if (1==r2.getCode() && r2.containsKey("data")){
                            String alipay_auth_url=r2.get("data").toString();
                            if(StringUtils.isNotBlank(alipay_auth_url)){
                                map.put("mode",1);
                                map.put("name",name);
                                map.put("uid",uid);
                                map.put("certify_id",certify_id);
                                map.put("certify_url",alipay_auth_url);
                                map.put("Url",alipay_auth_url);
                                return  R.ok().put("data",DataTypeConversionUtil.map2json(map).toJSONString());
                            }else {
                                return R.error("实名失败：获取alipay_auth_url失败");
                            }
                        }else{
                            return r2;
                        }
                    }else {
                        return R.error("实名失败：获取certify_id失败");
                    }
                }else{
                    return r1;
                }
            }catch (Exception e){
                return R.error(e.getMessage());
            }
        }else if(WECHATPAY_AUTH_MODE==mode){
            //使用腾讯云实名
            TencentUserCertifyService tucs=TencentUserCertifyFactory.build();
            R r1=tucs.tencentUserCertify_init(name,uid);
            if (1==r1.getCode() && r1.containsKey("data")){
                map.put("mode",2);
                map.put("name",name);
                map.put("uid",uid);
                map.put("bizcode",r1.get("data"));
                JSONObject jsonObject=DataTypeConversionUtil.string2Json(r1.get("data").toString());
                map.putAll(jsonObject);
                return R.ok().put("data",DataTypeConversionUtil.map2json(map).toJSONString());
            }else{
                return r1;
            }
        }
        return R.error("实名方式未知");
    }

    private List<SysConfigEntity>pay_list(){
        List<SysConfigEntity>list =new ArrayList<>();
        SysConfigEntity sysConfig=sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().eq("param_key", ConfigConstantEnum.PAY_IDS.getConfKey()).eq("status",1).last("limit 1"));
        if(null!=sysConfig){
            String p_value=sysConfig.getParamValue();
            JSONObject pay_select_json=JSON.parseObject(p_value);
            if(pay_select_json.containsKey("ids")){
                String[] ids=pay_select_json.getString("ids").split(",");
                for(String id:ids){
                    SysConfigEntity pay_conf= sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().eq("id",id).eq("status",1).select("id,param_key,remark"));
                    if(null!=pay_conf){
                        pay_conf.setParamValue(null);
                        list.add(pay_conf);
                    }
                }
            }
        }
        return  list;
    }

    private R aliPayQueryAuth(JSONObject payJsonObject,TbUserEntity user,TbOrderEntity alreadyCompleteOrder){
        try{
            AlipayUserCertifyService aliUucs= AlipayUserCertifyFactory.build();
            R r1= aliUucs.certifyQuery(payJsonObject.getString("certify_id"));
            if (1!=r1.getCode()){
                return r1;
            }
            String result=r1.get("data").toString();
            if(StringUtils.isNotBlank(result) && isJSON(result)){
                JSONObject ali_result_json=JSONObject.parseObject(result);
                if(ali_result_json.containsKey("alipay_user_certify_open_query_response")){
                    JSONObject alipay_user_certify_open_query_response_obj=ali_result_json.getJSONObject("alipay_user_certify_open_query_response");
                    if(alipay_user_certify_open_query_response_obj.containsKey("code") && alipay_user_certify_open_query_response_obj.containsKey("passed") ){
                        String passed=alipay_user_certify_open_query_response_obj.getString("passed");
                        //是否通过，通过为T，不通过为F
                        if("T".equals(passed)){
                            if(payJsonObject.containsKey("name") && payJsonObject.containsKey("uid")){
                                String r_name=payJsonObject.getString("name");
                                String r_id=payJsonObject.getString("uid");
                                user.setRealnameCertificateid(r_id);
                                user.setRealnameName(r_name);
                                user.setRealnameStatus(1);
                                user.setRealnameMode(1);
                                user.setRealnameTime(new Date());
                                userService.updateById(user);
                                payJsonObject.putAll(ali_result_json);
                                alreadyCompleteOrder.setPayJson(payJsonObject.toString());
                                tbOrderDao.updateById(alreadyCompleteOrder);
                                return R.ok().put("step",3).put("data",result).put("user",user);
                            }
                        }
                    }
                }
            }
            return R.ok().put("step",2).put("data",payJsonObject);
        }catch (Exception e){
            return R.error(e.getMessage());
        }
    }

    private R tencentQueryAuth(JSONObject payJsonObject,TbUserEntity user,TbOrderEntity alreadyCompleteOrder){
        //String order_extra2=alreadyCompleteOrder.getPayJson();
        //无成功记录--在线查询
        TencentUserCertifyService tucs= TencentUserCertifyFactory.build();
        JSONObject bizcodores=payJsonObject.getJSONObject("bizcode");
        //System.out.println(bizcodores);
        if(bizcodores.containsKey("BizToken")){
            R r2=tucs.tencentUserCertify_query(bizcodores.getString("BizToken"));
            if (1!=r2.getCode()){
                return r2;
            }
            String result=r2.get("data").toString();
            if(StringUtils.isNotBlank(result) && isJSON(result)){
                JSONObject res_json=JSONObject.parseObject(result);
                //System.out.println(res_json);
                JSONObject detectInfo_json= res_json.getJSONObject("DetectInfo");
                //System.out.println(detectInfo_json);
                //System.out.println("-----1>");
                JSONObject text_json=detectInfo_json.getJSONObject("Text");
                //System.out.println(text_json);
                //System.out.println("-----2>");
                if(text_json.containsKey("ErrCode") && text_json.get("ErrCode")!=null &&  0==text_json.getInteger("ErrCode")){
                    String r_name=text_json.getString("Name");
                    String r_id=text_json.getString("IdCard");
                    user.setRealnameCertificateid(r_id);
                    user.setRealnameName(r_name);
                    user.setRealnameStatus(1);
                    user.setRealnameMode(2);
                    user.setRealnameTime(new Date());
                    userService.updateById(user);
                    payJsonObject.put("result",result);
                    alreadyCompleteOrder.setPayObject(payJsonObject.toString());
                    tbOrderDao.updateById(alreadyCompleteOrder);
                    return R.ok().put("step",3).put("data",result);
                }
            }
        }
        return R.ok().put("step",2).put("data",payJsonObject);
    }

    @Login
    @PostMapping("/user/auth")
    @ApiOperation(" 用户实名认证-")
    public R userAuth(@ApiIgnore @RequestAttribute("userId") Long userId, @RequestBody UserCertifyForm form)  {
        final long AUTH_TIME_OUT=5*60*1000l;
        //1 查询当前用户实名状态
        TbUserEntity user=userService.getById(userId);
        if(null==user){
            return R.error("不存在的用户！");
        }
        if(null!=user.getRealnameStatus() &&  1==user.getRealnameStatus()){
            //AppUserEntity userEntity=appUserDao.selectOne(new QueryWrapper<AppUserEntity>().eq("user_id",user.getUserId()).select("realname_status,realname_name,realname_certificatetype,realname_certificateid,realname_mode,realname_time"));
            return R.ok().put("step",4).put("data",user);
        }
        //2 未实名->查询实名订单
        //2.1 实名产品ID
        //2.2 查询实名订单
        Date now =new Date();
        List<TbOrderEntity> auth_order_list=tbOrderDao.selectList(new QueryWrapper<TbOrderEntity>()
                .eq("user_id",userId)
                .eq("order_type", OrderTypeEnum.ORDER_AUTHENTICATION.getTypeId()));
        TbOrderEntity alreadyCompleteOrder=null;
        TbOrderEntity not_pay_order=null;

        for (TbOrderEntity order:auth_order_list){
            if((now.getTime()-order.getCreateTime().getTime())<(AUTH_TIME_OUT) && order.getStatus()== PayStatusEnum.PAY_COMPLETE.getId()){
                alreadyCompleteOrder=order;
            }else if((now.getTime()-order.getCreateTime().getTime())>=(AUTH_TIME_OUT) && order.getStatus()== PayStatusEnum.PAY_COMPLETE.getId()){
               //logger.debug("["+order.getSerialNumber()+"]is timeout");
            } else if((now.getTime()-order.getCreateTime().getTime())<(AUTH_TIME_OUT)){
                not_pay_order=order;
            } else if((now.getTime()-order.getCreateTime().getTime())>(AUTH_TIME_OUT) && order.getStatus()==PayStatusEnum.PAY_NOT_PAY.getId() ) {
                order.setStatus(PayStatusEnum.PAY_OUT_TIME.getId());
                tbOrderDao.updateById(order);
            }
        }
        //3 实名
        if(null!=alreadyCompleteOrder){
            //3.a 存在已支付完成的实名订单-->
            String orderInitStr=alreadyCompleteOrder.getInitJson();
            String authResult=alreadyCompleteOrder.getPayJson();
            //logger.info(orderInitStr);
            //logger.info(authResult);
            //3.a.b 查询实名结果
            JSONObject payJsonObject= DataTypeConversionUtil.string2Json(authResult);
            if(null!=payJsonObject && payJsonObject.containsKey("mode")){
                //存在实名请求数据--查询 实名状态
                if(1==payJsonObject.getInteger("mode") && payJsonObject.containsKey("certify_id") ){
                    //alipay 实名查询
                    return this.aliPayQueryAuth(payJsonObject,user,alreadyCompleteOrder);
                }else if(2==payJsonObject.getInteger("mode") && payJsonObject.containsKey("bizcode")){
                    return this.tencentQueryAuth(payJsonObject,user,alreadyCompleteOrder);
                }else{
                    return R.error("unknown mode");
                }
            }else {
                //3.a.a 无实名请求URL 发起创建实名 URL链接
                if(StringUtils.isBlank(form.getCert_name()) || StringUtils.isBlank(form.getCert_no())){
                    //需要传 实名参数
                    return R.ok().put("step",2).put("data",null).put("describe","需要传递实名和身份ID信息");
                }else {
                    alreadyCompleteOrder.setInitJson(DataTypeConversionUtil.entity2jonsStr(form));
                    tbOrderDao.updateById(alreadyCompleteOrder);
                    R r3= this.userAuthHandle(alreadyCompleteOrder.getSerialNumber(),form.getCert_name(),form.getCert_no());
                    if(1==r3.getCode()){
                        alreadyCompleteOrder.setPayJson(r3.get("data").toString());
                        tbOrderDao.updateById(alreadyCompleteOrder);
                        r3.put("step",2);
                        r3.put("data",DataTypeConversionUtil.string2Json(alreadyCompleteOrder.getPayJson()));
                        return r3;
                    }else {
                        return r3;
                    }
                }
            }
        }else if(null!=not_pay_order) {
            //3.b 存在未支付完成的实名订单-->发起请求支付
            payService.getOrderStatus(userId,not_pay_order.getSerialNumber());
            return  R.ok().put("step",1).put("data",not_pay_order).put("pay_list",pay_list());
        }else {
            //3.c.1 创建实名订单->发起请求支付
            TbOrderEntity n_auth_order=new TbOrderEntity();
            n_auth_order.setSerialNumber(System.currentTimeMillis()+""+ RandomUtil.randomInt(1000,9999));
            n_auth_order.setOrderType(OrderTypeEnum.ORDER_AUTHENTICATION.getTypeId());
            n_auth_order.setUserId(user.getUserId());
            //获取实名价格
            SysConfigEntity sysConfig=sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>()
                    .and(q->q.eq("param_key",ConfigConstantEnum.ALIPAYUSERCERTIFY_CONFIG_KEY.getConfKey()).or().eq("param_key",ConfigConstantEnum.TENCENTUSERCERTIFY_CONFIG_KEY.getConfKey()))
                    .eq("status",1)
                    .last("limit 1")
            );
            if (null==sysConfig){
                throw new RRException("实名未开启！");
            }
            Integer price=200;
            JSONObject AuthConfJsonObject=DataTypeConversionUtil.string2Json(sysConfig.getParamValue());
            if (null!=AuthConfJsonObject && AuthConfJsonObject.containsKey("cost")){
                price=AuthConfJsonObject.getInteger("cost");
            }
            n_auth_order.setPayable(price);
            n_auth_order.setCreateTime(new Date());
            n_auth_order.setStatus(PayStatusEnum.PAY_NOT_PAY.getId());
            tbOrderDao.insert(n_auth_order);
            if (0==price){
                //创建支付
                //增加支付成功记录
                TbPayRecordEntity record=new TbPayRecordEntity();
                record.setSerialNumber(n_auth_order.getSerialNumber());
                record.setPayType(PayTypeEnum.PRO_TYPE_sys.getId());
                record.setPayId(String.valueOf(System.currentTimeMillis()));
                record.setPayPaid(n_auth_order.getPayable());
                record.setStatus(PayRecordStatusEnum.STATUS_UNKNOWN.getId());
                record.setOperateStatus(0);
                tbRecordDao.insert(record);
                payService.getOrderStatus(userId,n_auth_order.getSerialNumber());
                return R.ok().put("step",2).put("data",null).put("describe","需要传递实名和身份ID信息");
            }
            return  R.ok().put("step",1).put("data",n_auth_order).put("pay_list",pay_list());
        }
    }


    @Login
    @PostMapping("/user/suit/bytes/detail")
    @ApiOperation(" 用户套餐流量使用详情")
    public R user_bytes_detail(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody QuerySuitBytes querySuitBytes){
        logger.info("##### app ELK ######");
        return R.ok().put("data",commonTaskService.getBytesDataListBySuitSerialNumber(userId,querySuitBytes.getSerialNumber(), DateUtils.LongStamp2Date(querySuitBytes.getStartTime()) ,DateUtils.LongStamp2Date(querySuitBytes.getEndTime())));
    }


    private Integer getChargingMode(JSONArray jsonArray){
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject attr=jsonArray.getJSONObject(i);
            if(attr.containsKey("attr") && attr.containsKey("value")&& "charging_mode".equals(attr.getString("attr"))){
                return  attr.getInteger("value");
            }
        }
        return 1;
    }

    @Login
    @GetMapping("/suit/create/postpaid")
    @ApiOperation("使用后付费")
    @UserLog("使用后付费")
    public R postpaid_create(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam Integer productId){
        //{"userId":61,"orderType":10,"targetId":1,"initJson":"{\"type\":\"m\",\"sum\":1}"}
        SubmitOrderForm submitOrderForm=new SubmitOrderForm();
        submitOrderForm.setUserId(userId);
        submitOrderForm.setOrderType(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId());
        submitOrderForm.setTargetId(productId);
        submitOrderForm.setInitJson("");
        CdnProductEntity product=productDao.selectById(productId);
        if(null==product){
            return R.error("不存在的产品！");
        }
        JSONArray jsonArray=DataTypeConversionUtil.string2JsonArray(product.getAttrJson());
        Integer chargingModeValue= getChargingMode(jsonArray);
        if(2==chargingModeValue || 3==chargingModeValue){
            return cdnSuitService.createOrder(submitOrderForm);
            //tbLogService.FrontUserWriteLog(userId, LogTypeEnum.PRODUCT_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),"");
        }else {
            return R.error("非后付费产品");
        }
    }

    @Login
    @GetMapping("/suit/cancellation")
    @ApiOperation("注销套餐|删除套餐")
    @UserLog("使用后付费")
    public R cancellationSuit(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam String SerialNumber){
       boolean result= cdnSuitService.cancellationSuit(userId,SerialNumber);
       if (result){
           //tbLogService.FrontUserWriteLog(userId, LogTypeEnum.PRODUCT_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),SerialNumber);
           return R.ok();
       }else {
           return R.error("失败！");
       }
    }

    @Login
    @GetMapping("/suit/liquidation")
    @ApiOperation("清算后付费套餐")
    @UserLog("清算后付费套餐")
    public R liquidationSuit(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestParam String SerialNumber){
       cdnSuitService.liquidationSuit(userId,SerialNumber);
       //tbLogService.FrontUserWriteLog(userId, LogTypeEnum.PRODUCT_LOG.getId(),this.getClass().getName()+"/"+Thread.currentThread().getStackTrace()[1].getMethodName(),SerialNumber);
       return R.ok();
    }

    private JSONObject getAttrObjByJsonArray(JSONArray jsonArray,String key,String keyValue){
        for (int i = 0; i <jsonArray.size() ; i++) {
            JSONObject obj=jsonArray.getJSONObject(i);
            if(obj.containsKey(key) && obj.getString(key).equals(keyValue)){
                return obj;
            }
        }
        return null;
    }

    @Login
    @GetMapping("/product/can/update")
    @ApiOperation("目标套餐可升级的套餐")
    public R canUpdate(@ApiIgnore @RequestAttribute("userId") Long userId, @RequestParam Integer productId){
        List<CdnProductEntity> result=new ArrayList<>();
        CdnProductEntity product=productDao.selectById(productId);
        if (null==product){
           return R.error("["+productId+"]无对应产品");
        }

        if (!ProductStatusEnum.canUpStatus().contains(product.getStatus())){
            return R.error("["+productId+":"+product.getStatus()+"]当前产品不可升级:");
        }
        JSONArray attrArray=DataTypeConversionUtil.string2JsonArray(product.getAttrJson());
        //套餐收费方式
        JSONObject chargingModeObj=getAttrObjByJsonArray(attrArray, "attr",ProductAttrNameEnum.ATTR_CHARGING_MODE.getAttr());
        if(null==chargingModeObj){
            return R.error("["+productId+"]当前产品参数不完整[1]");
        }
        if(chargingModeObj.containsKey("value") && 1!=chargingModeObj.getInteger("value")){
            //R.error("["+productId+"]当前产品参数不可[2]");
            return R.ok().put("data",result);
        }
        //{"m":{"value":10000,"status":1},"s":{"value":30000,"status":1},"y":{"value":100000,"status":1}}
        //以月费价格和其它产品月费价格为基准
        Integer t_m_value=0;
        if (true){
            JSONObject t_priceOBJ=DataTypeConversionUtil.string2Json(product.getProductJson());
            if( t_priceOBJ!=null && t_priceOBJ.containsKey("m")  ){
                JSONObject t_m_obj=t_priceOBJ.getJSONObject("m");
                if (t_m_obj.containsKey("value")){
                    t_m_value=t_m_obj.getIntValue("value");
                }
            }
        }
        if (true){
            Integer[] can_update={ProductStatusEnum.ENABLE.getId()};
            List<CdnProductEntity> productList=productDao.selectList(new QueryWrapper<CdnProductEntity>()
                    .in("status", Arrays.stream(can_update).toArray())
                    .eq("is_delete",0)
                    .eq("product_type",OrderTypeEnum.ORDER_CDN_SUIT.getTypeId()));
            for (CdnProductEntity c_product:productList){
                JSONArray c_attrArray=DataTypeConversionUtil.string2JsonArray(c_product.getAttrJson());
                //套餐收费方式
                JSONObject c_chargingModeObj=getAttrObjByJsonArray(c_attrArray, "attr",ProductAttrNameEnum.ATTR_CHARGING_MODE.getAttr());
                if(null==chargingModeObj){
                   continue;
                }
                if(chargingModeObj.containsKey("value") && 1!=chargingModeObj.getInteger("value")){
                   continue;
                }
                JSONObject c_priceOBJ=DataTypeConversionUtil.string2Json(c_product.getProductJson());
                if (c_priceOBJ!=null && c_priceOBJ.containsKey("m")){
                    JSONObject c_m_obj=c_priceOBJ.getJSONObject("m");
                    if (c_m_obj.containsKey("value")){
                        int c_m_value=c_m_obj.getIntValue("value");
                        if(c_m_value>t_m_value){
                            result.add(c_product);
                        }
                    }
                }
            }
        }

        return R.ok().put("data",result);
    }


    @Login
    @GetMapping("/suit/site/bind")
    @ApiOperation("目标套餐绑定的站点")
    public R suit_site_bind(@ApiIgnore @RequestAttribute("userId") Long userId,String SerialNumber){
        return R.ok().put("data",cdnSuitService.getSuitBind(userId,SerialNumber,1));
    }



}
