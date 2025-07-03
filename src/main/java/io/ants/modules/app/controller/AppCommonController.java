package io.ants.modules.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.ip2area.IPSeeker;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.TbMessageDao;
import io.ants.modules.app.entity.TbMessageEntity;
import io.ants.modules.app.form.PageForm;
import io.ants.modules.job.task.JavaJobEnum;
import io.ants.modules.sys.dao.CdnProductDao;
import io.ants.modules.sys.dao.SysConfigDao;
import io.ants.modules.sys.dao.TbCdnPublicMutAttrDao;
import io.ants.modules.sys.entity.CdnProductEntity;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.vo.DnsRewriteConfVo;
import io.ants.modules.utils.ConfigConstantEnum;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/app/common/")
@Tag(name = "APP  公共 接口")
public class AppCommonController {

    @Autowired
    private TbMessageDao tbMessageDao;

    @Autowired
    private CdnProductDao cdnProductDao;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private TbCdnPublicMutAttrDao tbCdnPublicMutAttrDao;

    @PostMapping("/notice/list")
    @Operation(summary = "获取公告消息[key1=titile,key2=content]")
    public R noticeList(@RequestBody PageForm form) {
        if (null == form.getPagenum() || null == form.getPagesize()) {
            return R.error("分页参数缺失");
        }
        IPage<TbMessageEntity> page = tbMessageDao.selectPage(
                new Page<TbMessageEntity>(form.getPagenum(), form.getPagesize()),
                new QueryWrapper<TbMessageEntity>()
                        .eq("type", 2)
                        .eq("status", 1)
                        .orderByDesc("id")
                        .like(StringUtils.isNotBlank(form.getSearch_key1()), "title", form.getSearch_key1())
                        .like(StringUtils.isNotBlank(form.getSearch_key2()), "content", form.getSearch_key2()));

        return R.ok().put("data", new PageUtils(page));
    }

    @PostMapping("/product/list")
    @Operation(summary = "获取CDN产品列表[key1=product_name]")
    public R productList(@RequestBody PageForm form) {
        if (null == form.getPagenum() || null == form.getPagesize()) {
            return R.error("分页参数缺失");
        }
        IPage<CdnProductEntity> page = cdnProductDao.selectPage(
                new Page<CdnProductEntity>(form.getPagenum(), form.getPagesize()),
                new QueryWrapper<CdnProductEntity>()
                        .eq("status", 1)
                        .ne("is_delete", 1)
                        .eq("product_type", OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                        .orderByDesc("weight")
                        .eq(StringUtils.isNotBlank(form.getSearch_key1()), "product_name", form.getSearch_key1()));
        return R.ok().put("data", new PageUtils(page));
    }

    @GetMapping("/web/config")
    @Operation(summary = "获取web 参数")
    public R getWebConf() {
        Map<String, Object> allMap = new HashMap<>(1024);
        allMap.put("data", sysConfigDao.queryByKey("WEB_SITE_CONFIG_KEY"));

        List<SysConfigEntity> list = sysConfigDao
                .selectList(new QueryWrapper<SysConfigEntity>().select("id,param_key,status"));
        allMap.put("list", list);

        SysConfigEntity dnsSiteConf = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>()
                .eq("param_key", ConfigConstantEnum.DNS_USER_API_ROOT_URI.getConfKey()).last("limit 1"));
        if (null != dnsSiteConf && StringUtils.isNotBlank(dnsSiteConf.getParamValue())) {
            allMap.put("dnsConf", DataTypeConversionUtil.string2Json(dnsSiteConf.getParamValue()));
        } else {
            allMap.put("dnsConf", "");
        }

        Map<String, Object> extraMap = new HashMap<>(1024);
        if (StaticVariableUtils.exclusive_modeList.size() > 0) {
            for (String modeName : StaticVariableUtils.exclusive_modeList) {
                extraMap.put(modeName, 1);
            }
        }
        allMap.put("extra", extraMap);

        TbCdnPublicMutAttrEntity publicMutAttr = tbCdnPublicMutAttrDao
                .selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                        .eq("pkey", PublicEnum.DNS_REWRITE_CONF.getName())
                        .last("limit 1"));
        if (null == publicMutAttr) {
            DnsRewriteConfVo vo = new DnsRewriteConfVo();
            allMap.put("dns_rewrite", vo);
        } else {
            DnsRewriteConfVo vo = DataTypeConversionUtil.string2Entity(publicMutAttr.getPvalue(),
                    DnsRewriteConfVo.class);
            allMap.put("dns_rewrite", vo);
        }

        // allMap.put("master_ip",StaticVariableUtils.authMasterIp);
        return R.ok(allMap);
    }

    @GetMapping("/web/agent")
    @Operation(summary = "获取注册协议")
    public R getAgent() {
        // SysConfigEntity web_agent_config=sysConfigDao.selectOne(new
        // QueryWrapper<SysConfigEntity>().eq("param_key","WEB_AGREEMENT").last("limit
        // 1") );
        return R.ok().put("data", sysConfigDao.queryByKey("WEB_AGREEMENT"));
    }

    @GetMapping("/web/update/log")
    @Operation(summary = "cdn 更新日志")
    public R getUpdateLog(@RequestParam String pagenum, @RequestParam String pagesize) {
        String docStr = "";
        try {
            docStr = QuerySysAuth.getUpdateInfoList("3", pagenum, pagesize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok().put("data", docStr);

    }

    @GetMapping("/query/ip/area")
    @Operation(summary = "获取IP区域")
    public R getIpArea(String ip) {
        if (StringUtils.isNotBlank(ip) && IPUtils.isValidIPV4(ip)) {
            return R.ok().put("data", IPSeeker.getIpAreaByNew(ip));
        }
        return R.ok("ip is error!");
    }

    @GetMapping("/web/enum")
    @Operation(summary = "获取公共enum")
    public R getWebEnum() {
        Map<String, Object> map = new HashMap();
        map.put("PayType", PayTypeEnum.getallpaytype());
        map.put("PayStatus", PayStatusEnum.getAllStatus());
        map.put("PayRecordStatus", PayRecordStatusEnum.getAllStatus());
        map.put("orderType", OrderTypeEnum.GetAllType());
        map.put("ClientStatus", ClientStatusEnum.getAllType());
        Map orderMap = new TreeMap();
        orderMap.put("product_example",
                "{\"m\":{\"value\":1,\"status\":1},\"s\":{\"value\":100,\"status\":1},\"y\":{\"value\":1000,\"status\":1}}");
        orderMap.put("buy_product_example", "{\"type\":\"m\",\"sum\":1}");
        orderMap.put("renew_product_example", "{\"serialNumber\":\"1650361752071001\",\"sum\":1}");
        orderMap.put("adder_product_example",
                "{\"serialNumber\":\"1650361752071001\",\"startTime\":\"1650769152\",\"type\":\"m\",\"sum\":2}");
        orderMap.put("upgrade_product_example", "{\"serialNumber\":\"1650361752071001\"}");
        map.put("orderInfo", orderMap);
        map.put("ProductAttrValueType", ProductAttrNameEnum.getAllTypes());
        map.put("ProductAttr", ProductAttrNameEnum.getAll());
        map.put("productAttr_kv", ProductAttrNameEnum.getKeyValues());
        map.put("productAttr_s", ProductAttrNameEnum.getSuffixKeyValues());
        map.put("productStatus", ProductStatusEnum.getAllType());
        map.put("siteAttr", SiteAttrEnum.getAll());

        map.put("dnsApiType", DnsApiEnum.getAllType());
        map.put("dnsUserType", UserTypeEnum.GetAll());
        map.put("mimeTypes", MimeTypesEnum.GetAll());
        map.put("nginxErrLogType", StaticVariableUtils.NGX_ERR_LOG_TYPES);
        map.put("LogTypeEnum", LogTypeEnum.GetAll());
        map.put("JavaJobEnum", JavaJobEnum.GetAll());
        return R.ok().put("data", map);
    }

}
