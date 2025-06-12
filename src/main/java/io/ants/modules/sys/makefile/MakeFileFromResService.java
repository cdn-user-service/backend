package io.ants.modules.sys.makefile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.*;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.vo.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class MakeFileFromResService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static  final String SET_SUFFIX=":file-path";
    //AGENT 重新启动
    private static final String ALL_GROUP_SET ="allfile-path:";
    //
    private static final int WAF_REG_MODE=5;

    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private TbCdnPublicMutAttrDao cdnPublicMutAttrDao;
    @Autowired
    private CdnIpControlDao cdnIpControlDao;
    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private TbRewriteDao tbRewriteDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private CdnProductDao cdnProductDao;

    static String VALUE_PATTERN = "###[\\w\\d]+###";
    static Pattern VALUE_PATTERN_R = Pattern.compile(VALUE_PATTERN);

    static String OBJECT_PATTERN="#f#[\\w\\d]+#f#";
    static Pattern OBJECT_PATTERN_R = Pattern.compile(OBJECT_PATTERN);

    public R sentNginxConf(String clientIds){
       List<TbCdnPublicMutAttrEntity> ls=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .in("pkey", PublicEnum.getAllNameByGroup(PublicEnum.WORKER_PROCESSES.getGroup()))
                .eq("status",1)
                .select("pkey,pvalue")
       );
       MakeFileParamVo mv=new MakeFileParamVo();
       mv.setModulePath("ngx_conf_module\\nginx.conf");
       mv.setNgxConfPath(PushSetEnum.NGINX_CONF.getTemplatePath());
       //1 默认
       mv.getValuesMap().putAll(PublicEnum.getAllKeyDefault(PublicEnum.WORKER_PROCESSES.getGroup()));
       //2 pub config
       for (TbCdnPublicMutAttrEntity attrEntity:ls){
            mv.getValuesMap().put(attrEntity.getPkey(),attrEntity.getPvalue());
       }

       List<Integer> clientTypes=ClientTypeEnum.getClientTypes("node");

       StringBuilder cacheUpstream=new StringBuilder();
       List<CdnClientEntity>l2=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",ClientTypeEnum.CACHE_LEVEL_2.getId())
                .select("area_id,client_ip"));
       List<CdnClientEntity>l3=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",ClientTypeEnum.CACHE_LEVEL_3.getId())
                .select("area_id,client_ip"));
       if (QuerySysAuth.IS_USE_CACHE_NODE){
            cacheUpstream.append("\n");
            cacheUpstream.append("    upstream site_cache_12 {\n");
            if (l2.size()>0){
                for (CdnClientEntity clc2:l2){
                    cacheUpstream.append(String.format("        server %s:80 weight=1 max_fails=0 fail_timeout=60 ;\n",clc2.getClientIp()));
                }
            }else{
                cacheUpstream.append(String.format("        server %s:80 weight=1 max_fails=0 fail_timeout=60 ;\n","127.0.0.1"));
            }
            cacheUpstream.append("        keepalive 1 ;\n");
            cacheUpstream.append("        keepalive_timeout 60;\n");
            cacheUpstream.append("    }\n");
            cacheUpstream.append("\n");
            cacheUpstream.append("    upstream site_cache_23 {\n");
            if (l3.size()>0){
                for (CdnClientEntity clc3:l3){
                    cacheUpstream.append(String.format("        server %s:80 weight=1 max_fails=0 fail_timeout=60 ;\n",clc3.getClientIp()));
                }
            }else {
                cacheUpstream.append(String.format("        server %s:80 weight=1 max_fails=0 fail_timeout=60 ;\n","127.0.0.1"));
            }
            cacheUpstream.append("        keepalive 1 ;\n");
            cacheUpstream.append("        keepalive_timeout 60;\n");
            cacheUpstream.append("    }\n");
        }

       List<CdnClientEntity> clientList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
               .in("client_type",clientTypes)
               .in(StringUtils.isNotBlank(clientIds),"id",clientIds.split(","))
               .select("id,area_id,client_type,conf_info,client_ip")
       );
       //3 client conf
       for (CdnClientEntity cl:clientList){
           if (StringUtils.isNotBlank(cl.getConfInfo())){
               Map nodeConfMap= PublicEnum.getNodeConfKeys();
               if (null!=nodeConfMap){
                   JSONObject m2= DataTypeConversionUtil.string2Json(cl.getConfInfo()) ;
                   if (null!=m2){
                       for (Object nodeDefineKey:nodeConfMap.keySet()){
                           if (m2.containsKey(nodeDefineKey.toString())){
                               mv.getValuesMap().put(nodeDefineKey.toString(),m2.get(nodeDefineKey.toString()));
                           }
                       }
                   }
               }
           }
           if (StringUtils.isBlank(cl.getClientIp())){
               logger.info(cl.getId()+" ip is null ");
               continue;
           }


           StringBuilder httpModelConf=new StringBuilder();
           httpModelConf.append("\n");
           if (true){
               httpModelConf.append(cacheUpstream);
               httpModelConf.append("    map \"\" $up_flag_str {\n");
               int clientType=null!=cl.getClientType()?cl.getClientType():1;
               String upFlagStr="00";
               if (l2.size()>0 && l3.size()>0){
                   switch (clientType){
                       case 1:
                           upFlagStr="12";
                           break;
                       case 2:
                           upFlagStr="23";
                           break;
                       default:
                           upFlagStr="00";
                           break;
                   }
               }else if (l2.size()>0 && l3.size()==0){
                   switch (clientType){
                       case 1:
                           upFlagStr="12";
                           break;
                       default:
                           upFlagStr="00";
                           break;
                   }
               }
               httpModelConf.append(String.format("       default \"%s\";\n",upFlagStr));
               httpModelConf.append("    }\n");
           }
           httpModelConf.append("    include      etc/http.conf;\n");
           httpModelConf.append("    include      cache.conf;\n");
           httpModelConf.append("    include      conf/site/*.conf;\n");
           httpModelConf.append("    include      conf/rewrite/*.conf;\n");
           if (mv.getValuesMap().containsKey(PublicEnum.HTTP_MODEL_FLAG.getName()) && "1".equals(mv.getValuesMap().get(PublicEnum.HTTP_MODEL_FLAG.getName()))){
               mv.getValuesMap().put("http_model_chunk",httpModelConf);
           }else{
               logger.info( "HTTP_MODEL_FLAG: not found");
           }
           String streamModelConf="\n    include      conf/forward/*.conf;\n";
           if (mv.getValuesMap().containsKey(PublicEnum.STREAM_MODEL_FLAG.getName()) && "1".equals(mv.getValuesMap().get(PublicEnum.STREAM_MODEL_FLAG.getName()))){
               mv.getValuesMap().put("stream_model_chunk",streamModelConf);
           }else{
               logger.info( "STREAM_MODEL_FLAG:not found");
           }
           R rc=this.tranceNginxConfContent(mv);
           if (1==rc.getCode()){
               String content=rc.get("data").toString();
               //logger.info(content);
               //this.saveFilePathConf2Redis(cl.getClientIp()+":"+mv.getNgxConfPath(),cl.getClientIp(),content);
               this.saveFilePathConf2Local(PushSetEnum.NGINX_CONF.getId(),cl.getClientIp(),null,null,null,content);
           }
           //System.out.println(content);
       }
       return R.ok();

    }



    public R sentCacheConf( String clientIds){
        List<TbCdnPublicMutAttrEntity> ls=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .in("pkey", PublicEnum.getAllNameByGroup(PublicEnum.PROXY_BUFFER_SIZE.getGroup()))
                .eq("status",1)
                .select("pkey,pvalue")
        );
        MakeFileParamVo mv=new MakeFileParamVo();
        mv.setModulePath("ngx_conf_module\\cache.conf");
        mv.setNgxConfPath(PushSetEnum.CACHE_CONF.getTemplatePath());
        for (TbCdnPublicMutAttrEntity attrEntity:ls){
            mv.getValuesMap().put(attrEntity.getPkey(),attrEntity.getPvalue());
        }
        List<CdnClientEntity> clientList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
                .in(StringUtils.isNotBlank(clientIds),"id",clientIds.split(","))
                .select("id,conf_info,client_ip")
        );
        for (CdnClientEntity cl:clientList){
            if (StringUtils.isNotBlank(cl.getConfInfo())){
                JSONObject m2= DataTypeConversionUtil.string2Json(cl.getConfInfo()) ;
                mv.getValuesMap().putAll(m2);
            }
            if (StringUtils.isBlank(cl.getClientIp())){
                logger.info(cl.getId()+" ip is null ");
                continue;
            }

            R rc=this.tranceNginxConfContent(mv);
            if (1==rc.getCode()){
                String content=rc.get("data").toString();
                //this.saveFilePathConf2Redis(cl.getClientIp()+":"+mv.getNgxConfPath(),cl.getClientIp(),content);
                this.saveFilePathConf2Local(PushSetEnum.CACHE_CONF.getId(),cl.getClientIp(),null,null,null,content);
            }
            //System.out.println(content);
        }
        return R.ok();

    }


    public R sentEtcHttpConf(){
        final String groupName="http_conf";
        String[] headKeys={PublicEnum.HTTP_RESPONSE_HEADER.getName(),PublicEnum.HTTP_REQUEST_HEADER.getName()};
        List<TbCdnPublicMutAttrEntity> ls=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .in("pkey", PublicEnum.getAllNameByGroup(groupName))
                .notIn("pkey",headKeys)
                .eq("status",1)
                .select("pkey,pvalue")
        );
        MakeFileParamVo mv=new MakeFileParamVo();
        mv.setModulePath("ngx_conf_module\\http.conf");
        mv.setNgxConfPath(PushSetEnum.HTTP_CONF.getTemplatePath());
        //先追加默认key值
        mv.getValuesMap().putAll(PublicEnum.getAllKeyDefault(groupName));
        //修改配置后的key值
        for (TbCdnPublicMutAttrEntity attrEntity:ls){
            mv.getValuesMap().put(attrEntity.getPkey(),attrEntity.getPvalue());
        }
        //other_key
        mv.getValuesMap().put("master_ip", StaticVariableUtils.authMasterIp);
        String acmeAccount= AcmeShUtils.getAcmeAccount();
        if (StringUtils.isBlank(acmeAccount)){
            mv.getValuesMap().put("acme_account","-");
        }else{
            mv.getValuesMap().put("acme_account",acmeAccount);
        }

        //add head
        StringBuilder httpHeadSb=new StringBuilder();
        List<TbCdnPublicMutAttrEntity>  attrList=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .in("pkey",headKeys)
                .eq("status",1)
        );
        for (TbCdnPublicMutAttrEntity mAttr:attrList){
            //{"type":"Cache-Control","header":"","content":"1111","info":"111"}
            if (mAttr.getPkey().equals(PublicEnum.HTTP_RESPONSE_HEADER.getName())){
                httpHeadSb.append(this.getAddHeadByString(mAttr.getPvalue(),1));
            }else if(mAttr.getPkey().equals(PublicEnum.HTTP_REQUEST_HEADER.getName())){
                httpHeadSb.append(this.getAddHeadByString(mAttr.getPvalue(),2));
            }

        }
        mv.getValuesMap().put("add_http_header_chunk","\n"+httpHeadSb.toString());
        mv.getValuesMap().put("cdn_sys_apply_cert_chunk", buildApplyCertConf(0, mv));

        //f-host module
        if (StaticVariableUtils.exclusive_modeList.contains("f_host")){
            String f_host_str="map \"\" $ants_f_host_flag {\n" +
                    "    default \"1\";\n" +
                    "}\n";
            mv.getValuesMap().put("f_host_flag_map",f_host_str);
        }

        //sys_flag_chunk
        Set<String> sysIps=new HashSet<>();
        sysIps.add("127.0.0.1");
        sysIps.add(StaticVariableUtils.authMasterIp);
        StringBuilder SFCsb=new StringBuilder();
        SFCsb.append("\n");
        for (String sip:sysIps){
            SFCsb.append(String.format("    \"%s\" \"1\";\n",sip));
        }
        mv.getValuesMap().put("sys_flag_chunk",SFCsb);

        R rc=this.tranceNginxConfContent(mv);
        if (1==rc.getCode()){
            String content=rc.get("data").toString();
            //this.saveFilePathConf2Redis(mv.getNgxConfPath(),"",content);
            this.saveFilePathConf2Local(PushSetEnum.HTTP_CONF.getId(),null,null,null,null,content);
        }

        //System.out.println(content);

        return R.ok();
    }

    public R sentEtcCertVerifyConf(){
        TbCdnPublicMutAttrEntity verifyAddrEntity=cdnPublicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",PublicEnum.CERT_APPLY_PROXY_PASS.getName())
                .last("limit 1")
        );
        StringBuilder sb=new StringBuilder();
        sb.append("\nproxy_set_header Host $host;\n");
        String content="proxy_pass http://148.135.100.118:80;";
        if (null!=verifyAddrEntity && StringUtils.isNotBlank(verifyAddrEntity.getPvalue())){
            String scheme="http://";
            String ipPort=verifyAddrEntity.getPvalue();
            if (verifyAddrEntity.getPvalue().startsWith("http://")){
                scheme="http://";
                ipPort=verifyAddrEntity.getPvalue().substring(scheme.length());
            }else if (verifyAddrEntity.getPvalue().startsWith("https://")){
                scheme="https://";
                ipPort=verifyAddrEntity.getPvalue().substring(scheme.length());
            }
            //logger.info("Setting s:"+scheme+",ipPort:"+ipPort+", ");
            String[] ipPortValues=StringUtils.split(ipPort,":");
            if (2==ipPortValues.length){
                String ip = ipPortValues[0];
                int port = Integer.parseInt(ipPortValues[1]);
                content=String.format("proxy_pass %s%s:%d;",scheme,ip,port+1);
            }else if (1==ipPortValues.length){
                content=String.format("proxy_pass %s%s;",scheme,ipPort);
            }
        }
        sb.append(content);
        this.saveFilePathConf2Local(PushSetEnum.CERT_VERIFY_CONF.getId(),null,null,null,null,sb.toString());
        return R.ok();
    }

    public R sentNginxDefaultIndexHtml(){
        TbCdnPublicMutAttrEntity mutIndex=cdnPublicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",PublicEnum.DEFAULT_INDEX_HTML.getName())
                .last("limit 1")
        );
        String indexHtml=PublicEnum.DEFAULT_INDEX_HTML.getDefaultValue();
        String addValue="";
        if (null!=mutIndex){
            indexHtml=mutIndex.getPvalue();
            if (QuerySysAuth.SHOW_NODE_REMARK){
                List<CdnClientEntity> list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>().isNull("parent_id").select("id,client_ip,parent_id,remark"));
                StringBuilder sb=new StringBuilder();
                sb.append("  \n var l_key=CryptoJS.MD5(location.hostname).toString();\n");
                for (CdnClientEntity client:list){
                    if ( StringUtils.isBlank(client.getRemark())){
                        continue;
                    }
                    sb.append("\n if ( l_key== '"+HashUtils.md5ofString(client.getClientIp()) +"') {  return \""+client.getRemark()+"\"; }; \n");
                    List<CdnClientEntity> childList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>().eq("parent_id",client.getId()).select("client_ip"));
                    for (CdnClientEntity client1:childList){
                        sb.append("\n if ( l_key == '"+HashUtils.md5ofString(client1.getClientIp())+"') {  return  \""+client.getRemark()+"\"; }; \n");
                    }

                }
                String rmkHtml="\n<html><script src=\"https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.0.0/crypto-js.min.js\"></script><p id=\"sys_ip_remark\" style=\" position: fixed;bottom: 10px;left: 48%;\"> </p></html>";
                String rmkJs="\n<script>var ip_remark_txt =function() {"+sb+";  \n return \"\"; \n};\ndocument.getElementById(\"sys_ip_remark\").textContent = ip_remark_txt();</script>";
                addValue=rmkHtml+rmkJs;
            }
        }

        //this.saveFilePathConf2Redis(PushSetEnum.INDEX_HTML.getTemplatePath(),"",indexHtml+addValue);
        this.saveFilePathConf2Local(PushSetEnum.INDEX_HTML.getId(),null,null,null,null,indexHtml+addValue);
        return R.ok();
    }


    public R sentNginxDefaultErrPageHtml(){
        List<String>keyList=PublicEnum.getAllNameByGroup("error_page");
        if (keyList.isEmpty()){
            return R.ok();
        }
        List<TbCdnPublicMutAttrEntity> mutList=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .in("pkey",keyList)
                .eq("status",1)
                .ne("pkey","default_index_html")
        );
        if (null==mutList){
            return R.ok();
        }
        for (TbCdnPublicMutAttrEntity publicMutAttr:mutList){
            switch (publicMutAttr.getPkey()){
                case "error_400":
                   // this.saveFilePathConf2Redis(PushSetEnum.ERR_400_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_400_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_403":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_403_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_403_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_404":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_404_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_404_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_410":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_410_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_410_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_500":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_500_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_500_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_502":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_502_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_502_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_503":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_503_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_503_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "error_504":
                    //this.saveFilePathConf2Redis(PushSetEnum.ERR_504_HTML.getTemplatePath(),"",publicMutAttr.getPvalue());
                    this.saveFilePathConf2Local(PushSetEnum.ERR_504_HTML.getId(),null,null,null,null,publicMutAttr.getPvalue());
                    break;
                case "site_suit_exp_html":
                    if (true){
                        String htmlValue=PublicEnum.INDEX_SITE_SUIT_EXP_HTML.getDefaultValue();
                        if (StringUtils.isNotBlank(publicMutAttr.getPvalue())){
                            htmlValue=publicMutAttr.getPvalue();
                        }
                        //this.saveFilePathConf2Redis(PushSetEnum.INDEX_SITE_SUIT_EXP_HTML.getTemplatePath(),"",htmlValue);
                        this.saveFilePathConf2Local(PushSetEnum.INDEX_SITE_SUIT_EXP_HTML.getId(),null,null,null,null,htmlValue);
                    }
                    break;
                default:
                    break;
            }
        }
        return R.ok();
    }

    public R sentHttpDefaultWafConf(){
        List<TbCdnPublicMutAttrEntity> defList=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",PublicEnum.HTTP_DEFAULT_RULE.getName())
                .eq("status",1)
                .orderByDesc("weight")
        );
        if (3==WAF_REG_MODE){
            StringBuilder regSb=new StringBuilder();
            List<String> stringRuleList=new ArrayList<>(defList.size());
            for (TbCdnPublicMutAttrEntity regRuleDet:defList){
                List<String> stringList=this.getRexStrListV300(regRuleDet.getPvalue());
                stringList.forEach(item->{
                    regSb.append(item);
                    regSb.append("\n");
                });
                stringRuleList.add(regRuleDet.getPvalue());
            }
            String regContent=regSb.toString();
            //String ngxDefRegPath=PushSetEnum.REG_HTTP.getTemplatePath();
            //this.saveFilePathConf2Redis(ngxDefRegPath,"",regContent);
            this.saveFilePathConf2Local(PushSetEnum.REG_HTTP.getId(),null,null,null,null,regContent);
            //String ruleContent=this.getRuleFileV300(stringRuleList);
            //String ngxDefRulePath=PushSetEnum.RULE_HTTP.getTemplatePath();
            //this.saveFilePathConf2Redis(ngxDefRulePath,"",ruleContent);
        }else if (5==WAF_REG_MODE){
            String ngxDefRegPath=PushSetEnum.REG_HTTP.getTemplatePath();
            List<String> pVList=new ArrayList<>(defList.size());
            for (TbCdnPublicMutAttrEntity regRuleDet:defList){
                pVList.add(regRuleDet.getPvalue());
            }
            String  regContent=  this.getRegRuleV500(ngxDefRegPath,"",pVList);
            //this.saveFilePathConf2Redis(ngxDefRegPath,"",regContent);
            this.saveFilePathConf2Local(PushSetEnum.REG_HTTP.getId(),null,null,null,null,regContent);
        }
        return R.ok();
    }

    public R sentEtcWhiteIpv4Conf(){
        StringBuilder sb=new StringBuilder();
        List<String> ipList=new ArrayList<>();
        List<String> ipCidrLIst=new ArrayList<>();
        try{
            //1 公共配置列表中的white ip
            List<TbCdnPublicMutAttrEntity> list=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey",PublicEnum.WHITE_IP.getName())
                    .eq("status",1)
                    .select("pvalue"));
            for (TbCdnPublicMutAttrEntity item:list){
                if(StringUtils.isNotBlank(item.getPvalue())){
                    JSONObject object=DataTypeConversionUtil.string2Json(item.getPvalue());
                    if (object.containsKey("ip")){
                        String ip=object.getString("ip");
                        if (IPUtils.isValidIPV4(ip)){
                            if (!ipList.contains(ip)){
                                ipList.add(ip);
                            }
                        }else if(IPUtils.isCidr(ip)){
                            if (!ipCidrLIst.contains(ip)){
                                ipCidrLIst.add(ip);
                            }
                        }else{
                            logger.error("["+ip+"]unknown type to http_white_ipv4!" );
                        }
                    }
                }
            }

            //2 节点列表中节点加入white ip
            List<CdnClientEntity> client_list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                    .eq("client_type",1)
            );
            for (CdnClientEntity client:client_list){
                if (!ipList.contains(client.getClientIp())){
                    ipList.add(client.getClientIp());
                }
            }

            //3 添加masterIp
            if (!ipList.contains(StaticVariableUtils.authMasterIp)){
                ipList.add(StaticVariableUtils.authMasterIp);
            }

            //4 add 127.0.0.1
            if(!ipList.contains("127.0.0.1")){
                ipList.add("127.0.0.1");
            }

            //5 来之IP表的ipv4
            List<String> tb_ips=new ArrayList<>();
            this.updateGetTbIp(tb_ips, IpControlEnum.PASS_7.getId(), 0);
            for (String ip:tb_ips){
                if (IPUtils.isValidIPV4(ip)){
                    if (!ipList.contains(ip)){
                        ipList.add(ip);
                    }
                }else if(IPUtils.isCidr(ip)){
                    if (!ipCidrLIst.contains(ip)){
                        ipCidrLIst.add(ip);
                    }
                }else{
                    logger.error("["+ip+"]tb_ip unknown type to white_ipv!" );
                }
            }

            //6 去除冲突IP
            for (String cidr:ipCidrLIst){
                Iterator<String> it=ipList.iterator();
                while (it.hasNext()){
                    String ip=it.next();
                    if (IPUtils.isInRange(ip,cidr)){
                        it.remove();
                    }
                }
            }

            //7 更新WHITE IP
            String ip_str=String.join("\n",ipList);
            String cidr_str=String.join("\n",ipCidrLIst);
            sb.append(ip_str);
            sb.append("\n");
            sb.append(cidr_str);
        }catch (Exception e){
            e.printStackTrace();
        }
        String content=sb.toString();
        String buildPath=PushSetEnum.HTTP_WHITE_IPV4.getTemplatePath();
        //this.saveFilePathConf2Redis(buildPath,"",content);
        this.saveFilePathConf2Local(PushSetEnum.HTTP_WHITE_IPV4.getId(),null,null,null,null,content);
        //System.out.println(content);
        return R.ok();
    }

    public R sentEtcWhiteIpv6Conf(){
        String content="";
        String buildPath=PushSetEnum.HTTP_WHITE_IPV6.getTemplatePath();
        //this.saveFilePathConf2Redis(buildPath,"",content);
        this.saveFilePathConf2Local(PushSetEnum.HTTP_WHITE_IPV6.getId(),null,null,null,null,content);
        //System.out.println(content);
        return R.ok();
    }

    public R sentEtcBlackIpv4Conf(){
        StringBuilder sb=new StringBuilder();
        try {
            List<String> ipList=new ArrayList<>();
            List<String> ipCidrLIst=new ArrayList<>();

            //1
            List<TbCdnPublicMutAttrEntity> list=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey",PublicEnum.BLACK_IP.getName())
                    .eq("status",1)
                    .select("pvalue"));
            for (TbCdnPublicMutAttrEntity item:list){
                if(StringUtils.isNotBlank(item.getPvalue())){
                    JSONObject object=DataTypeConversionUtil.string2Json(item.getPvalue());
                    if (object.containsKey("ip")){
                        String ip=object.getString("ip");
                        if (IPUtils.isValidIPV4(ip)){
                            if (!ipList.contains(ip)){
                                ipList.add(ip);
                            }
                        }else if(IPUtils.isCidr(ip)){
                            if (!ipCidrLIst.contains(ip)){
                                ipCidrLIst.add(ip);
                            }
                        }else{
                            logger.error("ip["+ip+"] is not valid IPV4 to black ipv4!");
                        }
                    }
                }
            }

            //2   来之IP表的ipv4
            List<String> tb_ips=new ArrayList<>();
            this.updateGetTbIp(tb_ips,IpControlEnum.FORBID_7.getId(), 0);
            for (String ip:tb_ips){
                if (IPUtils.isValidIPV4(ip)){
                    if (!ipList.contains(ip)){
                        ipList.add(ip);
                    }
                }else if(IPUtils.isCidr(ip)){
                    if (!ipCidrLIst.contains(ip)){
                        ipCidrLIst.add(ip);
                    }
                }else{
                    logger.error("["+ip+"]unknown type to nginx  black_ipv4!" );
                }
            }

            //3去冲突
            for (String cidr:ipCidrLIst){
                Iterator it=ipList.iterator();
                while (it.hasNext()){
                    String ip=it.next().toString();
                    if (IPUtils.isInRange(ip,cidr)){
                        it.remove();
                    }
                }
            }

            //4 更新 IP
            String ip_str=String.join("\n",ipList);
            String cidr_str=String.join("\n",ipCidrLIst);
            sb.append(ip_str);
            sb.append("\n");
            sb.append(cidr_str);
        }catch (Exception e){
            e.printStackTrace();
        }
        String content=sb.toString();
        //String buildPath=PushSetEnum.HTTP_BLACK_IPV4.getTemplatePath();
        //this.saveFilePathConf2Redis(buildPath,"",content);
        this.saveFilePathConf2Local(PushSetEnum.HTTP_BLACK_IPV4.getId(),null,null,null,null,content);
        //System.out.println(content);
        return R.ok();
    }

    public R sentEtcBlackIpv6Conf(){
        String content="";
        //String buildPath=PushSetEnum.HTTP_BLACK_IPV6.getTemplatePath();
        //this.saveFilePathConf2Redis(buildPath,"",content);
        this.saveFilePathConf2Local(PushSetEnum.HTTP_BLACK_IPV6.getId(),null,null,null,null,content);
        //System.out.println(content);
        return R.ok();
    }

    public R sentEtcPubRegRuleMConf(){
        try{
            List<TbCdnPublicMutAttrEntity> regGroupList=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey",PublicEnum.WEB_RULE_PRECISE.getName())
                    .eq("status",1)
                    .select("id")
                    .orderByDesc("weight")
            );
            if (3==WAF_REG_MODE){
                for (TbCdnPublicMutAttrEntity pubRegGroup:regGroupList){
                    List<TbCdnPublicMutAttrEntity> regRuleDetailList=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                            .eq("pkey",PublicEnum.WEB_RULE_PRECISE_DETAIL.getName())
                            .eq("status",1)
                            .eq("parent_id",pubRegGroup.getId())
                            .orderByDesc("weight"));
                    StringBuilder sb=new StringBuilder();
                    List<String> stringRuleList=new ArrayList<>(regRuleDetailList.size());
                    for (TbCdnPublicMutAttrEntity regRuleDet:regRuleDetailList){
                        List<String> stringList=this.getRexStrListV300(regRuleDet.getPvalue());
                        stringList.forEach(item->{
                            sb.append(item);
                            sb.append("\n");
                        });
                        stringRuleList.add(regRuleDet.getPvalue());
                    }
                    String regContent=sb.toString();
                    String ruleContent=this.getRuleFileV300(stringRuleList);
                    String buildRegPath=PushSetEnum.REG_PUB_WAF_SELECT_ID.getTemplatePath();
                    buildRegPath=buildRegPath.replace("###pub_waf_select_id###",pubRegGroup.getId().toString());
                    //this.saveFilePathConf2Redis(buildRegPath,"",regContent);
                    this.saveFilePathConf2Local(PushSetEnum.REG_PUB_WAF_SELECT_ID.getId(),null,null,pubRegGroup.getId().toString(),null,regContent);

                    //String buildRulePath=PushSetEnum.RULE_PUB_WAF_SELECT_ID.getTemplatePath();
                    //buildRulePath=buildRulePath.replace("###pub_waf_select_id###",pubRegGroup.getId().toString());
                    //this.saveFilePathConf2Redis(buildRulePath,"",ruleContent);
                    this.saveFilePathConf2Local(PushSetEnum.RULE_PUB_WAF_SELECT_ID.getId(),null,null,pubRegGroup.getId().toString(),null,ruleContent);
                    //System.out.println(regContent+"===="+ruleContent);
                }
            }else if (5==WAF_REG_MODE){
                for (TbCdnPublicMutAttrEntity pubRegGroup:regGroupList){
                    List<TbCdnPublicMutAttrEntity> regRuleDetailList=cdnPublicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                            .eq("pkey",PublicEnum.WEB_RULE_PRECISE_DETAIL.getName())
                            .eq("status",1)
                            .eq("parent_id",pubRegGroup.getId())
                            .orderByDesc("weight"));
                    List<String> pVList=new ArrayList<>();
                    for (TbCdnPublicMutAttrEntity regRuleDet:regRuleDetailList){
                        pVList.add(regRuleDet.getPvalue());
                    }
                    String buildRegPath=PushSetEnum.REG_PUB_WAF_SELECT_ID.getTemplatePath();
                    buildRegPath=buildRegPath.replace("###pub_waf_select_id###",pubRegGroup.getId().toString());
                    String  regContent= this.getRegRuleV500(buildRegPath,"",pVList);
                    //logger.info(buildRegPath,regContent);
                    //this.saveFilePathConf2Redis(buildRegPath,"",regContent);
                    this.saveFilePathConf2Local(PushSetEnum.REG_PUB_WAF_SELECT_ID.getId(),null,null,pubRegGroup.getId().toString(),null,regContent);

                    //String buildRulePath=PushSetEnum.RULE_PUB_WAF_SELECT_ID.getTemplatePath();
                    //buildRulePath=buildRulePath.replace("###pub_waf_select_id###",pubRegGroup.getId().toString());
                    //this.saveFilePathConf2Redis(buildRulePath,"","");
                }
            }
            return R.ok();
        }catch (Exception e){
            e.printStackTrace();
        }
        return R.error("推送cdn公共规则失败");

    }

    public R sendEtcInjRegx(){
        //String buildRulePath=PushSetEnum.PUB_INJ_WAF_REGX.getTemplatePath();
        // this.saveFilePathConf2Redis(buildRulePath,"",NgxInjectioPenetrationEnum.getAllRegx());
        this.saveFilePathConf2Local(PushSetEnum.PUB_INJ_WAF_REGX.getId(),null,null, null,null,NgxInjectioPenetrationEnum.getAllRegx());
        return R.ok();
    }

    public R sendPubIndexWafTemplate(){
        List<String> fileNameList=PublicEnum.getAllNameByGroup("waf_verify_template");
        if (fileNameList.isEmpty()){
            return R.ok();
        }
        for (String fileName : fileNameList){
          //String path=PushSetEnum.PUB_WAF_TEMPLATE.getTemplatePath();
          //path=path.replace("###file_name###",fileName);
          //    PUB_WAF_TEMPLATE(21,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/###file_name###.html","add","-1","{fn}.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
          String content=String.format("<div>empty_template_%s</div>",fileName);
          TbCdnPublicMutAttrEntity mutWafTemplate=cdnPublicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",fileName)
                .last("limit 1")
          );
          if (null==mutWafTemplate || StringUtils.isBlank(mutWafTemplate.getPvalue()) ){
              //default value
              String templateFilePath = "ngx_waf_templates/"+fileName+".html";
              Resource resource = new ClassPathResource(templateFilePath);
              try (InputStream is = resource.getInputStream();
                   BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                  StringBuilder contentBuilder = new StringBuilder();
                  String line;
                  while ((line = reader.readLine()) != null) {
                      contentBuilder.append(line).append(System.lineSeparator());
                  }
                  content = contentBuilder.toString();
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }else{
              content=mutWafTemplate.getPvalue();
          }
         //  this.saveFilePathConf2Redis(path,"",content);
         this.saveFilePathConf2Local(PushSetEnum.PUB_WAF_TEMPLATE.getId(),null,null,fileName,null,content);
        }
        return R.ok();
    }

    public R sentConfForwardMConf(String forwardIds){
        List<TbStreamProxyEntity>list=tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
                .in(StringUtils.isNotBlank(forwardIds),"id",forwardIds.split(","))
                .eq("status",1)
        );
        for(TbStreamProxyEntity streamEntity:list){
            StaticVariableUtils.streamIdSerNumMap.put(streamEntity.getId().toString(),streamEntity.getSerialNumber());
            MakeFileParamVo mv=new MakeFileParamVo();
            String stmId=streamEntity.getId().toString();
            mv.setModulePath("ngx_conf_module\\stream#.conf");
            mv.getValuesMap().put("stream_id","stm_id"+stmId);
            mv.getValuesMap().put("s_id",stmId);

            mv.getValuesMap().put("s_uid",streamEntity.getUserId().toString());
            mv.getValuesMap().put("s_sn",streamEntity.getSerialNumber());
            JSONObject confJsonObject=DataTypeConversionUtil.string2Json(streamEntity.getConfInfo());
            mv.getValuesMap().putAll(confJsonObject);
            NgxStreamConfVo vo=DataTypeConversionUtil.json2entity(confJsonObject,NgxStreamConfVo.class);
            //LISTEN LIST
            if (StringUtils.isNotBlank(vo.getListen())){
                StringBuilder lsb=new StringBuilder();
                for (String l:vo.getListen().split("\\|")){
                    lsb.append("\n");
                    lsb.append("    listen  ");
                    lsb.append(l);
                    if (vo.getProtocol().equalsIgnoreCase("udp")){
                        lsb.append(" udp reuseport ");
                    }
                    lsb.append(";\n");
                }
                mv.getValuesMap().put("listen_list",lsb.toString());
            }
            mv.getValuesMap().put("protocol",vo.getProtocol());
            //proxy_protocol
            if (QuerySysAuth.PPV2_FLAG && null!=vo && null!=vo.getProxy_protocol() ){
                if (1==vo.getProxy_protocol()){
                    //ppv1
                    //proxy_protocol_version        2;
                    //    proxy_protocol    on;
                    mv.getValuesMap().put("proxy_protocol_chunk","proxy_protocol on;\n    proxy_protocol_version    1;\n");
                }else if (2==vo.getProxy_protocol()){
                    //ppv2
                    mv.getValuesMap().put("proxy_protocol_chunk","proxy_protocol on;\n    proxy_protocol_version   2;\n");
                }
            }
            //server_list
            if (null!=vo.getServer() && !vo.getServer().isEmpty()){
                StringBuilder lsv=new StringBuilder();
                for (String l:vo.getServer()){
                    lsv.append("\n");
                    lsv.append("    server ");
                    lsv.append(l);
                    lsv.append(";\n");
                }
                mv.getValuesMap().put("server_list",lsv.toString());
            }
            R rc=this.tranceNginxConfContent(mv);
            if (1==rc.getCode()){
                String content=rc.get("data").toString();
                //String ngxPath=PushSetEnum.STREAM_CONF.getTemplatePath().replace("###sp_id###",stmId);
                // this.saveFilePathConf2Redis(ngxPath,stmId,content);
                this.saveFilePathConf2Local(PushSetEnum.STREAM_CONF.getId(),null,getNodeAreaGroupIdBySerialNumber(streamEntity.getSerialNumber()),stmId,null,content);
            }

            //System.out.println(content);
        }
        return R.ok();
    }


    public R sentConfRewriteMConf(String rewriteIds){
        List<TbRewriteEntity>list=tbRewriteDao.selectList(new QueryWrapper<TbRewriteEntity>()
                .in(StringUtils.isNotBlank(rewriteIds),"id",rewriteIds.split(","))
                .eq("status",1)
        );
        for (TbRewriteEntity rewrite:list){
            StaticVariableUtils.rewriteIdSerNumMap.put(rewrite.getId().toString(),rewrite.getSerialNumber());
            MakeFileParamVo mv=new MakeFileParamVo();
            String rId=rewrite.getId().toString();
            mv.setModulePath("ngx_conf_module\\rewrite#.conf");
            mv.getValuesMap().put("server_name",rewrite.getServerName());
            mv.getValuesMap().put("rewrite_id",rId);
            Map rMap=DataTypeConversionUtil.entity2map(rewrite);
            if (null!=rMap){
                mv.getValuesMap().putAll(rMap);
            }
            mv.getValuesMap().put("follow_mode","$1");
            if (StringUtils.isNotBlank(rewrite.getFollowMode()) &&  rewrite.getFollowMode().equals("assign")){
                mv.getValuesMap().put("follow_mode","");
            }
            mv.getValuesMap().put("jsContent",rewrite.getJsContent().replace("\"","'"));
            if (true){
                mv.getValuesMap().put("alias_list","");
                if (StringUtils.isNotBlank(rewrite.getAlias())){
                    StringBuilder asb=new StringBuilder();
                    String[] alias=rewrite.getAlias().split(",");
                    for (String a:alias){
                        if (StringUtils.isNotBlank(a)){
                            asb.append(" ");
                            asb.append(a);
                            asb.append(" ");
                        }
                    }
                    mv.getValuesMap().put("alias_list",asb.toString());
                }

            }
            R rc=this.tranceNginxConfContent(mv);
            if (1==rc.getCode()){
                String content=rc.get("data").toString();
                //String ngxPath=PushSetEnum.REWRITE_CONF.getTemplatePath().replace("###rewrite_id_name###",rId);
                //this.saveFilePathConf2Redis(ngxPath,rId,content);
                this.saveFilePathConf2Local(PushSetEnum.REWRITE_CONF.getId(),null,getNodeAreaGroupIdBySerialNumber(rewrite.getSerialNumber()),rId,null,content);
            }

            //System.out.println(content);
        }
        return R.ok();
    }


    public R sentConfSiteMConf(String siteIds){
        //final int WAF_MODE=3;
        long start_tm=0;
        logger.info("Sending-start_tm:"+start_tm+",siteIds:"+siteIds);
        List<TbSiteEntity>list=tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                .in(StringUtils.isNotBlank(siteIds),"id",siteIds.split(","))
                .select("id,serial_number,user_id,main_server_name")
                .eq("status",CdnSiteStatusEnum.NORMAL.getId())
        );
        logger.info("Sending total size:"+list.size());
        List<String> errCodeList= SiteAttrEnum.getAllErrorCode();
        final String[] sslKeys={SiteAttrEnum.SSL_OTHER_CERT_KEY.getName(),SiteAttrEnum.SSL_OTHER_CERT_PEM.getName(),SiteAttrEnum.SSL_PROTOCOLS.getName(),SiteAttrEnum.ADVANCED_CONF_OCSP.getName(),SiteAttrEnum.SOURCE_SNI.getName()};
        final String[] wafRegRuleKeys={SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName(),SiteAttrEnum.PRI_PRECISE_WAF_USER_SELECTS.getName(),SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName()};

        String wafRegPath="";
        String wafRulePath="";
        for (TbSiteEntity siteEntity:list){
            if (StaticVariableUtils.siteIdSerNumMap.containsKey(siteEntity.getId().toString())){
                StaticVariableUtils.siteIdSerNumMap.put(siteEntity.getId().toString(),siteEntity.getSerialNumber());
            }
            start_tm=System.currentTimeMillis();
            logger.info("buildSiteMConf start:id["+siteEntity.getId()+"]----["+siteEntity.getMainServerName());
            String SITE_ID=siteEntity.getId().toString();
            //IF NAME INCLUDE * DELETE FAIL ,SO USE ID
            //String SITE_ID_NAME=siteEntity.getId()+"_"+siteEntity.getMainServerName()+"_";
            String SITE_ID_NAME=SITE_ID;
            List<String> siteUserCusErrorHtmlKeys =new ArrayList<>(16);

            MakeFileParamVo mv=new MakeFileParamVo();
            Map<Object,Object> forceReplaceMap=new HashMap<>();
            mv.setModulePath("ngx_conf_module\\site#.conf");
            mv.getValuesMap().put("site_id",siteEntity.getId());
            mv.getValuesMap().put("user_id",siteEntity.getUserId().toString());
            mv.getValuesMap().put("main_server_name",siteEntity.getMainServerName());
            mv.getValuesMap().put("master_ip", StaticVariableUtils.authMasterIp);
            boolean siteSslVaildFlag=false;
            String  http2Conf="";
            if (QuerySysAuth.SITE_HTTP2_FLAG){
                TbSiteAttrEntity tbSiteAttrEntity=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                        .eq("site_id",siteEntity.getId())
                        .eq("pkey", SiteAttrEnum.SSL_HTTP2.getName())
                        .eq("status",1)
                        .last("limit 1")
                );
                if (null!=tbSiteAttrEntity && StringUtils.isNotBlank(tbSiteAttrEntity.getPvalue()) && "1".equals(tbSiteAttrEntity.getPvalue())){
                    http2Conf="http2";
                }
            }
            //====a sent ssl
            if (true){
                start_tm=System.currentTimeMillis();
                List<TbSiteMutAttrEntity> sslMInfos=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .in("pkey",sslKeys)
                        .eq("site_id",siteEntity.getId())
                        .eq("status",1)
                        .orderByDesc("weight")
                );
                List<TbSiteAttrEntity> sslSInfos=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                        .in("pkey",sslKeys)
                        .eq("site_id",siteEntity.getId())
                        .eq("status",1)
                        .orderByDesc("weight")
                );
                String sslCrt="";
                String sslKey="";
                String sslProtocols=SiteAttrEnum.SSL_PROTOCOLS.getDefaultValue();
                String ocspStatus="";
                String sniStatus="";
                if (!sslMInfos.isEmpty() && sslMInfos.size()>=2){
                    for (TbSiteMutAttrEntity sslInfo:sslMInfos){
                        if (sslInfo.getPkey().equals(SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())){
                            sslKey=DataTypeConversionUtil.string2Entity(sslInfo.getPvalue(), NgxAttrValueVo.class).getValue();
                        }else  if (sslInfo.getPkey().equals(SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())){
                            sslCrt=DataTypeConversionUtil.string2Entity(sslInfo.getPvalue(), NgxAttrValueVo.class).getValue();
                        }
                    }
                    for (TbSiteAttrEntity sslInfo:sslSInfos){
                        if (sslInfo.getPkey().equals(SiteAttrEnum.SSL_PROTOCOLS.getName())){
                            sslProtocols=sslInfo.getPvalue();
                        }else if(sslInfo.getPkey().equals(SiteAttrEnum.ADVANCED_CONF_OCSP.getName())){
                            ocspStatus=sslInfo.getPvalue();
                        }  else if(sslInfo.getPkey().equals(SiteAttrEnum.SOURCE_SNI.getName())){
                            sniStatus=sslInfo.getPvalue();
                        }
                    }
                    if (StringUtils.isNotBlank(sslCrt) && StringUtils.isNotBlank(sslKey) ){
                        siteSslVaildFlag=true;
                        if (true){
                            //ssl_chunk
                            StringBuilder sslProSb=new StringBuilder();
                            sslProSb.append("\n");
                            sslProSb.append("    ssl_certificate conf/ssl/"+SITE_ID_NAME+".crt;\n");
                            sslProSb.append("    ssl_certificate_key conf/ssl/"+SITE_ID_NAME+".key;\n");
                            // ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256';
                            // ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';

                            if ( "TLSv1.1 TLSv1.2 TLSv1.3".equals(sslProtocols)){
                                sslProSb.append("    ssl_protocols TLSv1.1 TLSv1.2 TLSv1.3;\n");
                                sslProSb.append("    ssl_ciphers EECDH+CHACHA20:EECDH+CHACHA20-draft:EECDH+AES128:RSA+AES128:EECDH+AES256:RSA+AES256:EECDH+3DES:RSA+3DES:!MD5;\n");

                            }else{
                                sslProSb.append("    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;\n");
                                sslProSb.append("    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4:!DH:!DHE;\n");
                            }
                            sslProSb.append("    ssl_prefer_server_ciphers on;\n");
                            sslProSb.append("    ssl_session_cache shared:SSL:10m;\n");
                            mv.getValuesMap().put("ssl_chunk",sslProSb.toString());
                        }
                        if (true){
                            //ocsp && sni
                            StringBuilder ocspChunkSb=new StringBuilder();
                            ocspChunkSb.append("\n");
                            if("1".equals(ocspStatus)){
                                ocspChunkSb.append("    ssl_stapling on;\n");
                                ocspChunkSb.append("    ssl_stapling_verify on;\n");
                                ocspChunkSb.append("    ssl_trusted_certificate conf/ssl/"+SITE_ID_NAME+".crt; \n");
                                ocspChunkSb.append("    resolver 8.8.8.8 8.8.4.4 valid=60s ipv6=off; \n");
                                ocspChunkSb.append("    resolver_timeout 5s;\n");

                            }
                            //sni
                            if (true){
                                ocspChunkSb.append("    proxy_ssl_server_name on;\n");
                                ocspChunkSb.append("    proxy_ssl_name $sni_name;\n");
                            }
                            mv.getValuesMap().put("ocsp_chunk",ocspChunkSb.toString());
                        }
                       // String ngxSslPathCrt=PushSetEnum.SITE_SSL_CRT.getTemplatePath().replace("###ssl_ids###",siteEntity.getId().toString());
                       // String ngxSslPathKey=PushSetEnum.SITE_SSL_KEY.getTemplatePath().replace("###ssl_ids###",siteEntity.getId().toString());
                        // this.saveFilePathConf2Redis(ngxSslPathCrt,SITE_ID,sslCrt);
                        //this.saveFilePathConf2Redis(ngxSslPathKey,SITE_ID,sslKey);
                        this.saveFilePathConf2Local(PushSetEnum.SITE_SSL_CRT.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,null,sslCrt);
                        this.saveFilePathConf2Local(PushSetEnum.SITE_SSL_KEY.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,null,sslKey);
                        //System.out.println(sslCrt+"\n"+sslKey);
                    }
                }

                if (StringUtils.isNotBlank(http2Conf)){
                    StringBuilder h2Sb=new StringBuilder();
                    h2Sb.append("\n");
                    h2Sb.append("    keepalive_requests 1000;\n");
                    mv.getValuesMap().put("http2_conf_chunk",h2Sb.toString());
                }
                //4 ms
                //logger.info("sentConfSiteMConf_ssl_used_tm:"+(System.currentTimeMillis()-start_tm));
            }



            //====b waf  [ white_ip  black_ip  waf_rule]
            if (true){
                start_tm=System.currentTimeMillis();

                //白名单IPV4
                if (true){
                    String ngxSiteWhitePath=PushSetEnum.SITE_WAF_WHITE_IP.getTemplatePath().replace("###site_id_name###" ,SITE_ID_NAME);
                    String whiteIpv4Content="";
                    if (StaticVariableUtils.cacheSiteIdWafWhiteIpv4FileMap.containsKey(SITE_ID)){
                        whiteIpv4Content= StaticVariableUtils.cacheSiteIdWafWhiteIpv4FileMap.get(SITE_ID);
                    }else{
                        whiteIpv4Content=this.getSiteWhiteIpList(siteEntity.getId());
                        StaticVariableUtils.cacheSiteIdWafWhiteIpv4FileMap.put(SITE_ID,whiteIpv4Content);
                    }
                    if (StringUtils.isNotBlank(whiteIpv4Content)){
                        //this.saveFilePathConf2Redis(ngxSiteWhitePath,SITE_ID,whiteIpv4Content);
                        this.saveFilePathConf2Local(PushSetEnum.SITE_WAF_WHITE_IP.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,null,whiteIpv4Content);
                    }
                }


                //黑名单IPV4
                if (true){
                    String blackIpv4Content="";
                    if (StaticVariableUtils.cacheSiteIdWafBlockIpv4FileMap.containsKey(SITE_ID)){
                        blackIpv4Content=StaticVariableUtils.cacheSiteIdWafBlockIpv4FileMap.get(SITE_ID);
                    }else{
                        blackIpv4Content=this.getSiteBlackIpList(siteEntity.getId());
                        StaticVariableUtils.cacheSiteIdWafBlockIpv4FileMap.put(SITE_ID,blackIpv4Content);
                    }
                    if (StringUtils.isNotBlank(blackIpv4Content)){
                        String ngxSiteBlackPath=PushSetEnum.SITE_WAF_BLACK_IP.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                        // this.saveFilePathConf2Redis(ngxSiteBlackPath,SITE_ID,blackIpv4Content);
                        this.saveFilePathConf2Local(PushSetEnum.SITE_WAF_BLACK_IP.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,null,blackIpv4Content);
                    }
                }



                //REG WAF PATH 1检测是否使用了公共WAF ；2检测使用引用WAF ;3 自定义WAF
                NgxSiteWafVo wafVo=new NgxSiteWafVo();
                List<TbSiteAttrEntity> pubWafSelect=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                        .eq("site_id",siteEntity.getId())
                        .in("pkey",wafRegRuleKeys)
                        .eq("status",1)
                        .orderByDesc("weight")
                        .orderByAsc("id")
                );
                for (TbSiteAttrEntity attr:pubWafSelect){
                    if (attr.getPkey().equals(SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName())){
                        wafVo.setPubWafSelect(Integer.parseInt(attr.getPvalue()));
                    }else if(attr.getPkey().equals(SiteAttrEnum.PRI_PRECISE_WAF_USER_SELECTS.getName())){
                        wafVo.setPriWafSelect(Integer.parseInt(attr.getPvalue()));
                    }
                }
                if (0!=wafVo.getPubWafSelect()){
                    //选择了公共WAF
                    wafRegPath="/home/local/nginx/conf/etc/reg_"+wafVo.getPubWafSelect();
                    //wafRulePath="/home/local/nginx/conf/etc/rule_"+wafVo.getPubWafSelect();
                    //
                    TbCdnPublicMutAttrEntity pub_rule=cdnPublicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                            .eq("pkey",PublicEnum.WEB_RULE_PRECISE.getName())
                            .eq("id",wafVo.getPubWafSelect())
                            .last("limit 1")
                    );
                    if (null!=pub_rule){
                        PubWebRulePreciseDescribeVo pwrVo=DataTypeConversionUtil.string2Entity(pub_rule.getPvalue(),PubWebRulePreciseDescribeVo.class);
                        if (null!=pwrVo ){
                            forceReplaceMap.put("bchsc",String.valueOf(pwrVo.getBotCheckHttpStatusCode()));
                        }
                    }
                }else if (0!=wafVo.getPriWafSelect()){
                    //引用了私有WAF
                    TbSiteEntity selectSiteEntity=tbSiteDao.selectById(wafVo.getPriWafSelect());
                    if (null!=selectSiteEntity){
                        String selV=selectSiteEntity.getId().toString();//selectSiteEntity.getId()+"_"+selectSiteEntity.getMainServerName()+"_";
                        wafRegPath="/home/local/nginx/conf/conf/waf/reg_"+selV;
                        //wafRulePath="/home/local/nginx/conf/conf/waf/rule_"+selV;
                    }
                }else{
                    //使用了自定义私有WAF规则
                    wafRegPath="/home/local/nginx/conf/conf/waf/reg_"+SITE_ID_NAME;
                    //wafRulePath="/home/local/nginx/conf/conf/waf/rule_"+SITE_ID_NAME;
                }

                //====c waf reg_rule
                String regStr="";
                String ruleStr="";
                if (StaticVariableUtils.cacheSiteIdWafRegFileMap.containsKey(SITE_ID) && StaticVariableUtils.cacheSiteIdWafRuleFileMap.containsKey(SITE_ID)){
                    regStr=StaticVariableUtils.cacheSiteIdWafRegFileMap.get(SITE_ID);
                    //ruleStr=StaticVariableUtils.siteIdWafRuleFileMap.get(SITE_ID);
                }else{
                    List<TbSiteMutAttrEntity> priWafList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteEntity.getId())
                            .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                            .eq("status",1)
                            .orderByDesc("weight")
                            .orderByAsc("id")
                    );
                    if (!priWafList.isEmpty()){
                        if (3==WAF_REG_MODE){
                            StringBuilder regSbContent=new StringBuilder();
                            List<String> stringRuleList=new ArrayList<>(priWafList.size());
                            for (TbSiteMutAttrEntity wafInfo:priWafList){
                                List<String> stringList=this.getRexStrListV300(wafInfo.getPvalue());
                                stringList.forEach(item->{
                                    regSbContent.append(item);
                                    regSbContent.append("\r\n");
                                });
                                stringRuleList.add(wafInfo.getPvalue());
                            }
                            String ruleContent= this.getRuleFileV300(stringRuleList) ;
                            //String regPath=PushSetEnum.SITE_WAF_REG.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                            //String rulePath=PushSetEnum.SITE_WAF_RULE.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                            regStr=regSbContent.toString();
                            ruleStr=ruleContent;
                        }else if (5==WAF_REG_MODE){
                            String regPath=PushSetEnum.SITE_WAF_REG.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                            List<String> pVList=new ArrayList<String>();
                            for (TbSiteMutAttrEntity wafInfo:priWafList){
                                pVList.add(wafInfo.getPvalue());
                            }
                            regStr=this.getRegRuleV500(regPath,SITE_ID,pVList);
                        }
                    }
                    StaticVariableUtils.cacheSiteIdWafRegFileMap.put(SITE_ID,regStr);
                    StaticVariableUtils.cacheSiteIdWafRuleFileMap.put(SITE_ID,ruleStr);
                }
                //String regPath=PushSetEnum.SITE_WAF_REG.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                //String rulePath=PushSetEnum.SITE_WAF_RULE.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                //this.saveFilePathConf2Redis(regPath,SITE_ID,regStr);
                //this.saveFilePathConf2Redis(rulePath,SITE_ID,ruleStr);
                this.saveFilePathConf2Local(PushSetEnum.SITE_WAF_REG.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,null,regStr);
                //System.out.println(regSbContent+"----"+ruleContent);
                //logger.info("sentConfSiteMConf_waf_used_tm:"+(System.currentTimeMillis()-start_tm));
            }


            //====d site html
            if (true){
                start_tm=System.currentTimeMillis();
                List<TbSiteMutAttrEntity>errorPageList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .like("pkey","error_page_")
                        .eq("site_id",siteEntity.getId())
                        .eq("status",1)
                        .orderByDesc("weight")
                );
                StringBuilder errPageSb=new StringBuilder();
                for (TbSiteMutAttrEntity errPageEntity:errorPageList){
                    if (siteUserCusErrorHtmlKeys.contains(errPageEntity.getPkey())){
                        continue;
                    }
                    siteUserCusErrorHtmlKeys.add(errPageEntity.getPkey());
                    String htmlContent="";
                    NgxErrPageAttrVo vo=DataTypeConversionUtil.string2Entity(errPageEntity.getPvalue(),NgxErrPageAttrVo.class);
                    if (1==vo.getType()){
                        //来之公共 配置的错误页
                        //htmlContent=vo.getHtml_1();
                        //error_page_chunk
                        //error_400
                        String errCode=errPageEntity.getPkey().replace("error_page_","");
                        if (!errCodeList.contains(errCode)){
                            logger.warn("errCode["+errCode+"]不能定义！"+String.join(",",errCodeList));
                            continue;
                        }
                        errPageSb.append("\n");
                        errPageSb.append("    error_page "+errCode+"  /"+errCode+".html;\n");
                        errPageSb.append("    location = /"+errCode+".html {\n");
                        errPageSb.append("         root /home/local/nginx/conf/etc/html/;\n");
                        errPageSb.append("    }\n");
                    }else if (3==vo.getType()){
                        //来之SITE 自定义--生成文件
                        htmlContent=vo.getHtml_3();
                        if (StringUtils.isNotBlank(htmlContent)){
                            String errCode=errPageEntity.getPkey().replace("error_page_","");
                            // /home/local/nginx/conf/conf/html/####site_id####/###error_code###.html
                            //String ngxPath=PushSetEnum.SITE_HTMLS.getTemplatePath().replace("###error_code###",errCode);
                            //ngxPath=ngxPath.replace("###site_id###",siteEntity.getId().toString());
                            //this.saveFilePathConf2Redis(ngxPath,SITE_ID,htmlContent);
                            this.saveFilePathConf2Local(PushSetEnum.SITE_HTMLS.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,errCode, htmlContent);
                            //System.out.println(errCode+"----"+htmlContent);
                        }
                        String errCode=errPageEntity.getPkey().replace("error_page_","");
                        if (!errCodeList.contains(errCode)){
                            logger.warn("errCode["+errCode+"]不能定义！"+String.join(",",errCodeList));
                            continue;
                        }
                        errPageSb.append("\n");
                        errPageSb.append("    error_page "+errCode+"  /"+errCode+".html;\n");
                        errPageSb.append("    location = /"+errCode+".html {\n");
                        errPageSb.append("         root conf/conf/html/"+SITE_ID_NAME+"/;\n");
                        errPageSb.append("    }\n");
                    }
                }
                mv.getValuesMap().put("error_page_chunk",errPageSb.toString());
                //logger.info("sentConfSiteMConf_html_used_tm:"+(System.currentTimeMillis()-start_tm));
            }


            //====e site conf=====================
            if (true){
                //default key value
                start_tm=System.currentTimeMillis();
                String siteConfFile="";
                if (StaticVariableUtils.cacheSiteIdConfFileMap.containsKey(SITE_ID)){
                    siteConfFile=StaticVariableUtils.cacheSiteIdConfFileMap.get(SITE_ID);
                }else{
                    mv.getValuesMap().putAll(SiteAttrEnum.getAllKeyDefault());
                    mv.getValuesMap().put("suit_sn",siteEntity.getSerialNumber());
                    mv.getValuesMap().put("suit_exp",this.getSuitLastEndTimeTemp(siteEntity.getSerialNumber()));

                    //single
                    List<TbSiteAttrEntity>siteSingleAttrList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteEntity.getId())
                            .notIn("pkey",wafRegRuleKeys)
                            .isNotNull("pvalue")
                            .eq("status",1)
                            .orderByDesc("weight")
                    );

                    //mut
                    List<String> unIncludeKeys=new ArrayList<>(128);
                    unIncludeKeys.addAll(Arrays.asList(sslKeys));
                    unIncludeKeys.addAll(siteUserCusErrorHtmlKeys);
                    unIncludeKeys.addAll(Arrays.asList(wafRegRuleKeys));
                    List<TbSiteMutAttrEntity> siteMutAttrList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteEntity.getId())
                            .notIn("pkey",unIncludeKeys)
                            .isNotNull("pvalue")
                            .eq("status",1)
                            .orderByDesc("weight")
                    );
                    final String[] sKeys={"bool","int","text"};
                    StringBuilder aliasListSb=new StringBuilder();
                    Map<String,String> siteSingleMap=new HashMap<String,String>();
                    Map<String,List<String>> siteMutMap=new HashMap<String,List<String>>();
                    for (TbSiteAttrEntity attr:siteSingleAttrList){
                        SiteAttrEnum attrEnum=SiteAttrEnum.getObjByName(attr.getPkey());
                        if (null==attrEnum){
                            continue;
                        }
                        if (SiteAttrEnum.SITE_ATTR_IPV6.getName().equals(attr.getPkey())){
                            siteSingleMap.put(attr.getPkey(),attr.getPvalue());
                            mv.getValuesMap().put(attr.getPkey(),attr.getPvalue());
                        } else if (Arrays.asList(sKeys).contains(attrEnum.getType())  ){
                            siteSingleMap.put(attr.getPkey(),attr.getPvalue());
                            //"bool","int","text" 类
                        }else if ("m_text".equals(attrEnum.getType())){
                            //alias
                            if (siteMutMap.containsKey(attr.getPkey())){
                                siteMutMap.get(attr.getPkey()).add(attr.getPvalue());
                            }else{
                                List<String> attrMutList=new ArrayList<>();
                                attrMutList.add(attr.getPvalue());
                                siteMutMap.put(attr.getPkey(),attrMutList);
                            }
                        }else{
                            logger.error(attrEnum.getType()+" type not supported");
                        }
                    }
                    StringBuilder cAddHeadSb=new StringBuilder();
                    StringBuilder siteUrlRewriteSb=new StringBuilder();;
                    StringBuilder unCacheLocation=new StringBuilder();
                    StringBuilder cacheLocation=new StringBuilder();
                    StringBuilder cacheKeyValueBuf=new StringBuilder();
                    StringBuilder errCodeRewriteSb=new StringBuilder();
                    boolean cacheEnabled=false;

                    for (TbSiteMutAttrEntity mutAttr:siteMutAttrList){
                        SiteAttrEnum attrEnum=SiteAttrEnum.getObjByName(mutAttr.getPkey());
                        if (null==attrEnum){
                            continue;
                        }
                        if ("l_text".equals(attrEnum.getType()) || Arrays.asList(sKeys).contains(attrEnum.getType())  ){
                            siteSingleMap.put(mutAttr.getPkey(),mutAttr.getPvalue());
                        }else if (  "m_text".equals(attrEnum.getType()) ||  "mm_text".equals(attrEnum.getType())){
                            if (siteMutMap.containsKey(mutAttr.getPkey())){
                                siteMutMap.get(mutAttr.getPkey()).add(mutAttr.getPvalue());
                            }else{
                                List<String> attrMutList=new ArrayList<>();
                                attrMutList.add(mutAttr.getPvalue());
                                siteMutMap.put(mutAttr.getPkey(),attrMutList);
                            }
                        }else{
                            logger.error(attrEnum.getType()+" type not supported");
                        }
                    }

                    //cache key
                    if (true){
                        cacheKeyValueBuf=cacheKeyValueBuf.append(this.getCacheKey(siteEntity.getId()));
                    }
                    //###site_upstream_chunk### ###site_listen_chunk###
                    NgxBaseInfoConfResultVO baseInfoConfResultVO=this.getParseSiteBaseInfo(mv,siteEntity.getId(),siteSslVaildFlag,http2Conf);
                    if (null!=baseInfoConfResultVO){
                        Map bMap=DataTypeConversionUtil.entity2map(baseInfoConfResultVO);
                        if (null!=bMap){
                            mv.getValuesMap().putAll(bMap);
                        }
                    }
                    StringBuilder rangeSb=new StringBuilder();

                    //single attr
                    if (true){
                        //single attr
                        for (Map.Entry<String, String> entry : siteSingleMap.entrySet()) {
                            switch (entry.getKey()) {
                                case "source_host":
                                    mv.getValuesMap().put("source_host","$host");
                                    if (StringUtils.isNotBlank(entry.getValue())){
                                        mv.getValuesMap().put("source_host",entry.getValue());
                                    }
                                    break;
                                case "cache_url_param":
                                    mv.getValuesMap().put("cache_url_param","$request_uri");
                                    if ("1".equals(entry.getValue())){
                                        mv.getValuesMap().put("cache_url_param","$uri");
                                    }
                                    break;
                                case "slice":
                                    mv.getValuesMap().put("slice_range_flag","");
                                    if ("1".equals(entry.getValue())){
                                        mv.getValuesMap().put("slice_range_flag","$slice_range");
                                        mv.getValuesMap().put("proxy_range_chunk","slice  5m;\n");
                                        if (true){
                                            rangeSb.append("\n");
                                            rangeSb.append("        more_set_headers  \"Accept-Ranges: bytes\";\n");
                                            rangeSb.append("        proxy_set_header  Range $slice_range;\n");
                                        }
                                    }
                                    break;
                                case "forced_hsts":
                                    mv.getValuesMap().put("forced_hsts_chunk","\n");
                                    if ("1".equals(entry.getValue())){
                                        mv.getValuesMap().put("forced_hsts_chunk","add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;\n") ;
                                    }
                                    break;
                                case "mobile_jump":
                                    if(StringUtils.isNotBlank(entry.getValue())){
                                        StringBuilder mjSb=new StringBuilder();
                                        mjSb.append("\n");
                                        mjSb.append("    if ($dev_type = '1' ) {\n");
                                        mjSb.append("        rewrite  ^(.*)    "+entry.getValue()+"$1 redirect ;\n");
                                        mjSb.append("    }\n");
                                        mv.getValuesMap().put("mobile_jump_chunk",mjSb.toString());
                                    }
                                    break;
                                case "proxy_buffering":
                                    if ("0".equals(entry.getValue())){
                                        mv.getValuesMap().put("proxy_buffering","off");
                                    }else{
                                        mv.getValuesMap().put("proxy_buffering","on");
                                    }
                                    break;
                                case "source_sni":
                                    mv.getValuesMap().put("source_sni","$host");
                                    if (StringUtils.isNotBlank(entry.getValue())){
                                        mv.getValuesMap().put("source_sni",entry.getValue());
                                    }
                                    break;
                                case "websocket":
                                    if ("1".equals(entry.getValue())){
                                        int wsTMoutValue=3600;
                                        if (siteSingleMap.containsKey("ws_proxy_read_timeout")){
                                            if (StringUtils.isNotBlank(siteSingleMap.get("ws_proxy_read_timeout"))){
                                                wsTMoutValue=Integer.parseInt(siteSingleMap.get("ws_proxy_read_timeout"));
                                            }
                                        }
                                        StringBuilder wsSb=new StringBuilder();
                                        wsSb.append("\n");
                                        wsSb.append("        proxy_set_header Upgrade $http_upgrade;\n");
                                        wsSb.append("        proxy_set_header Connection \"Upgrade\";\n");
                                        wsSb.append(String.format("        proxy_read_timeout %d;\n",wsTMoutValue));
                                        mv.getValuesMap().put("websocket_chunk",wsSb.toString());
                                        //**** Upgrade 首字母强制大写 ****
                                    }
                                    break;
                                case "proxy_redirect":
                                    if ("1".equals(entry.getValue()) || SiteAttrEnum.PROXY_REDIRECT.getDefaultValue().equals(entry.getValue())   ){
                                        mv.getValuesMap().put("proxy_redirect",SiteAttrEnum.PROXY_REDIRECT.getDefaultValue() );
                                    }  else if ("0".equals(entry.getValue()) || "off".equals(entry.getValue())){
                                        mv.getValuesMap().put("proxy_redirect","off" );
                                    }else if (false && StringUtils.isNotBlank(entry.getValue())){
                                        mv.getValuesMap().put("proxy_redirect",entry.getValue() );
                                    }
                                    break;
                                case "x_robots_tag":
                                    if (!"0".equals(entry.getValue())){
                                        mv.getValuesMap().put("well_known_is_sys_chunk","if ($uri ~* \"/.well-known/\" ){  set $sys_flag 1;}");
                                        mv.getValuesMap().put("x_robots_tag_chunk","more_set_headers \"X-Robots-Tag:noindex,nofollow\" ;\n" );
                                        mv.getValuesMap().put("forbid_code_requests_chunk",getForbidCodeRequests(Integer.parseInt(entry.getValue())));
                                        mv.getValuesMap().put("default_type_text_html","default_type text/html;");
                                    }
                                    break;
                                case "forbid_code_requests":
                                    if ("1".equals(entry.getValue())){
                                        mv.getValuesMap().put("well_known_is_sys_chunk","if ($uri ~* \"/.well-known/\" ){  set $sys_flag 1;}");
                                        mv.getValuesMap().put("forbid_code_requests_chunk",getForbidCodeRequests(1));
                                        mv.getValuesMap().put("default_type_text_html","default_type text/html;");
                                    }
                                    break;
                                case "access_control_allow_origin":
                                    if ("1".equals(entry.getValue())){
                                        mv.getValuesMap().put("access_control_allow_origin_chunk","more_set_headers \"Access-Control-Allow-Origin: $http_origin\"; ");
                                    }
                                    break;
                                case "pri_waf_url_strings":
                                    if (StringUtils.isNotBlank(entry.getValue())){
                                        String[] attrs=entry.getValue().split(",");
                                        List<String> apiUrl=new ArrayList<>(attrs.length);
                                        for (String u:attrs){
                                            String uri=u.replace(" ","");
                                            if (StringUtils.isNotBlank(uri)){
                                                apiUrl.add(uri);
                                            }
                                        }
                                        mv.getValuesMap().put(entry.getKey(),String.join(" ",apiUrl));
                                    }
                                    break;
                                case "anti_theft_chain":
                                    //referersChunkSb.append();
                                    mv.getValuesMap().put("referers_chunk",this.getSiteRefererBlockConf(entry.getValue()));
                                    break;
                                case "server_user_custom_info":
                                    NgxCusAttrVo ncVO=DataTypeConversionUtil.string2Entity(entry.getValue(),NgxCusAttrVo.class);
                                    if (null!=ncVO){
                                        mv.getValuesMap().put(entry.getKey(),ncVO.getContent());
                                    }
                                    break;
                                case "follow_30x":
                                    Ngx30xFollowVo vo=DataTypeConversionUtil.string2Entity(entry.getValue(),Ngx30xFollowVo.class);
                                    if (null!=vo){
                                        this.buildFollowConf(mv,siteEntity.getId(),vo,cacheKeyValueBuf.toString());
                                    }
                                    break;
                                default:
                                    mv.getValuesMap().put(entry.getKey(),entry.getValue());
                            }
                        }
                    }

                    //mut attr
                    if (true){
                        for (Map.Entry<String,List<String>> entry : siteMutMap.entrySet()) {
                            switch (entry.getKey()){
                                case "alias":
                                    if (true) {
                                        for (String str:entry.getValue()){
                                            aliasListSb.append(" ");
                                            aliasListSb.append(str);
                                            aliasListSb.append(" ");
                                        }
                                    }
                                    break;
                                case "add_header":
                                    if (true){
                                        for (String str:entry.getValue()){
                                            cAddHeadSb.append(this.getAddHeadByString(str,1));
                                        }
                                    }
                                    break;
                                case "proxy_set_header":
                                    if (true){
                                        for (String str:entry.getValue()){
                                            cAddHeadSb.append(this.getAddHeadByString(str,2));
                                        }
                                    }
                                    break;
                                case "more_clear_headers":
                                    if (true){
                                        for (String str:entry.getValue()){
                                            cAddHeadSb.append(this.getAddHeadByString(str,3));
                                        }
                                    }
                                    break;
                                case "un_cache_config":
                                    if (true){
                                        for (String str:entry.getValue()){
                                            NgxCacheConfVo vo=DataTypeConversionUtil.string2Entity(str,NgxCacheConfVo.class);
                                            unCacheLocation.append(this.getCacheLocationConf(vo,0,siteEntity,baseInfoConfResultVO.getSite_proxy_pass_chunk(),cacheKeyValueBuf.toString(),rangeSb.toString()));
                                        }
                                    }
                                    break;
                                case "cache_config":
                                    if (true){
                                        cacheEnabled=true;
                                        for (String str:entry.getValue()){
                                            NgxCacheConfVo vo=DataTypeConversionUtil.string2Entity(str,NgxCacheConfVo.class);
                                            cacheLocation.append(this.getCacheLocationConf(vo,1,siteEntity,baseInfoConfResultVO.getSite_proxy_pass_chunk(),cacheKeyValueBuf.toString(),rangeSb.toString()));
                                        }
                                    }
                                    break;
                                case "site_uri_rewrite":
                                    if (true){
                                        for (String str:entry.getValue()){
                                            siteUrlRewriteSb.append(this.getSiteRewrite(str));
                                        }
                                    }
                                    break;
                                case "error_code_rewrite":
                                    if (true){
                                        for (String str:entry.getValue()){
                                            //errCodeRewriteSb
                                            errCodeRewriteSb.append(this.getSiteErrCodeRewrite(str));
                                        }
                                    }
                                    break;
                                default:
                                    mv.getValuesMap().put(entry.getKey(),entry.getValue());
                                    break;
                            }
                        }

                    }




                    mv.getValuesMap().put("alias_list",aliasListSb.toString());
                    if (mv.getValuesMap().containsKey(SiteAttrEnum.DEFAULT_LOCATION_CACHE_RULE.getName())){
                        mv.getValuesMap().put("default_location_cache_rule_chunk",this.getSiteDefaultCacheConf( mv.getValuesMap().get(SiteAttrEnum.DEFAULT_LOCATION_CACHE_RULE.getName()).toString(),cacheKeyValueBuf.toString()));
                    }
                    mv.getValuesMap().put("set_pc_h5_cache_flag_chunk","set $dev_type '0';");
                    if (mv.getValuesMap().containsKey(SiteAttrEnum.PERFORMANCE_PC_H5_CACHE.getName())){
                        if ("1".equals(mv.getValuesMap().get(SiteAttrEnum.PERFORMANCE_PC_H5_CACHE.getName()).toString())){
                            mv.getValuesMap().put("set_pc_h5_cache_flag_chunk","");
                        }
                    }
                    if (mv.getValuesMap().containsKey(SiteAttrEnum.SSL_CDN_APPLY_CERT.getName()) && null!=mv.getValuesMap().get(SiteAttrEnum.SSL_CDN_APPLY_CERT.getName())){
                        if (!"1".equals(mv.getValuesMap().get(SiteAttrEnum.SSL_CDN_APPLY_CERT.getName()).toString())){
                            mv.getValuesMap().put("cdn_sys_apply_cert_chunk", buildApplyCertConf(siteEntity.getId(), mv));
                        }
                    }
                    mv.getValuesMap().put("ants_waf_reg_path",wafRegPath);
                    mv.getValuesMap().put("ants_waf_rule_path",wafRulePath);

                    //forced_ssl_chunk
                    if (mv.getValuesMap().containsKey("forced_ssl")){
                        StringBuilder fsSb=new StringBuilder();
                        fsSb.append("\n");
                        mv.getValuesMap().put("well_known_is_sys_chunk","if ($uri ~* \"/.well-known/\" ){  set $sys_flag 1;}");
                        if ("1".equals(mv.getValuesMap().get("forced_ssl").toString())){
                            fsSb.append("\n");
                            fsSb.append("    set $forced_https \"1$scheme$sys_flag\";\n");
                            fsSb.append("    if ($forced_https = '1http0'){\n" );
                            fsSb.append("        return 301 https://$host$request_uri;\n");
                            fsSb.append("    }\n");
                            mv.getValuesMap().put("forced_ssl_chunk",fsSb.toString());
                        }else  if ("x".equals(mv.getValuesMap().get("forced_ssl").toString())){
                            mv.getValuesMap().put("default_type_text_html","default_type text/html;");
                            fsSb.append("\n");
                            //fsSb.append("    default_type  \"text/html\";\n");
                            fsSb.append("    set $forced_https \"2$scheme$sys_flag\";\n");
                            fsSb.append("    if ($forced_https = '2http0'){\n" );
                            fsSb.append("        return 200 \"<html><script>location.replace('https://'+location.host+location.pathname+location.search)</script></html>\";\n");
                            fsSb.append("    }\n");
                            mv.getValuesMap().put("forced_ssl_chunk",fsSb.toString());
                        }else  if ("3".equals(mv.getValuesMap().get("forced_ssl").toString())){
                            mv.getValuesMap().put("default_type_text_html","default_type text/html;");
                            fsSb.append("\n");
                            //fsSb.append("    default_type  \"text/html\";\n");
                            fsSb.append("    set $forced_https \"3$scheme$sys_flag\";\n");
                            fsSb.append("    if ($forced_https = '3http0'){\n" );
                            fsSb.append("        return "+QuerySysAuth.FORCED_SSL_MODE_3_STATUS_CODE);
                            fsSb.append(" \"<html><script>var _0x5654=['console','log','debug','search','replace','https://','apply','warn','table','pathname','exception','trace','return\\\\x20(function()\\\\x20','{}.constructor(\\\\x22return\\\\x20this\\\\x22)(\\\\x20)','error'];(function(_0x148b05,_0x5654f0){var _0x3741b3=function(_0x396f38){while(--_0x396f38){_0x148b05['push'](_0x148b05['shift']());}};_0x3741b3(++_0x5654f0);}(_0x5654,0xba));var _0x3741=function(_0x148b05,_0x5654f0){_0x148b05=_0x148b05-0x0;var _0x3741b3=_0x5654[_0x148b05];return _0x3741b3;};var _0xc6f130=function(){var _0x193831=!![];return function(_0xe3bbb9,_0x1f670d){var _0xace33=_0x193831?function(){if(_0x1f670d){var _0x186c12=_0x1f670d[_0x3741('0x0')](_0xe3bbb9,arguments);_0x1f670d=null;return _0x186c12;}}:function(){};_0x193831=![];return _0xace33;};}();var _0x1af9e3=_0xc6f130(this,function(){var _0x18a2e7=function(){};var _0x4c4a1a=function(){var _0x4da621;try{_0x4da621=Function(_0x3741('0x6')+_0x3741('0x7')+');')();}catch(_0x1beecc){_0x4da621=window;}return _0x4da621;};var _0x1ed544=_0x4c4a1a();if(!_0x1ed544[_0x3741('0x9')]){_0x1ed544[_0x3741('0x9')]=function(_0xa08ce1){var _0x238492={};_0x238492[_0x3741('0xa')]=_0xa08ce1;_0x238492[_0x3741('0x1')]=_0xa08ce1;_0x238492[_0x3741('0xb')]=_0xa08ce1;_0x238492['info']=_0xa08ce1;_0x238492[_0x3741('0x8')]=_0xa08ce1;_0x238492[_0x3741('0x4')]=_0xa08ce1;_0x238492[_0x3741('0x2')]=_0xa08ce1;_0x238492[_0x3741('0x5')]=_0xa08ce1;return _0x238492;}(_0x18a2e7);}else{_0x1ed544[_0x3741('0x9')][_0x3741('0xa')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['warn']=_0x18a2e7;_0x1ed544[_0x3741('0x9')][_0x3741('0xb')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['info']=_0x18a2e7;_0x1ed544[_0x3741('0x9')][_0x3741('0x8')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['exception']=_0x18a2e7;_0x1ed544[_0x3741('0x9')][_0x3741('0x2')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['trace']=_0x18a2e7;}});_0x1af9e3();location[_0x3741('0xd')](_0x3741('0xe')+location['host']+location[_0x3741('0x3')]+location[_0x3741('0xc')]);</script></html>\";\n");
                            fsSb.append("    }\n");
                            mv.getValuesMap().put("forced_ssl_chunk",fsSb.toString());
                        }else if("2".equals(mv.getValuesMap().get("forced_ssl").toString())){
                            mv.getValuesMap().put("default_type_text_html","default_type text/html;");
                            mv.getValuesMap().put("well_known_is_sys_chunk","if ($uri ~* \"/.well-known/\" ){  set $sys_flag 1;}");
                            mv.getValuesMap().put("forbid_code_requests_chunk",getForbidCodeRequests(1));
                            fsSb.append("    set $forced_https \"4$scheme$sys_flag\";\n");
                            fsSb.append("    if ($forced_https = '4http0'){\n" );
                            fsSb.append("        return 301 https://$host$request_uri;\n");
                            fsSb.append("    }\n");
                            mv.getValuesMap().put("forced_ssl_chunk",fsSb.toString());
                        }

                    }


                    if (forceReplaceMap.size() > 0) {
                        mv.getValuesMap().putAll(forceReplaceMap);
                    }

                    //StringBuilder referersChunkSb=new StringBuilder();
                    mv.getValuesMap().put("un_cache_chunk",unCacheLocation.toString());
                    mv.getValuesMap().put("cache_chunk",cacheLocation.toString());
                    mv.getValuesMap().put("add_u_r_head_chunk",cAddHeadSb.toString());
                    mv.getValuesMap().put("gzip_chunk",this.getSiteGzipChunk(siteSingleMap));
                    mv.getValuesMap().put("sub_filter_chunk",this.getSiteSubFilter(siteEntity.getId()));
                    mv.getValuesMap().put("site_rewrite_chunk",siteUrlRewriteSb.toString());
                    mv.getValuesMap().put("cache_key_value",String.format("\"%s\"",cacheKeyValueBuf) );
                    mv.getValuesMap().put("site_id",SITE_ID);
                    mv.getValuesMap().put("set_site_id",String.format("set $site_id \"%s\";",SITE_ID));
                    mv.getValuesMap().put("err_code_rewrite_chunk",errCodeRewriteSb);
                    R rc=this.tranceNginxConfContent(mv);
                    if (1==rc.getCode()){
                        siteConfFile=rc.get("data").toString();
                        StaticVariableUtils.cacheSiteIdConfFileMap.put(SITE_ID,siteConfFile);
                    }
                }
                if (StringUtils.isNotBlank(siteConfFile)){
                    //String siteConfPath=PushSetEnum.SITE_CONF.getTemplatePath().replace("###site_id_name###",SITE_ID_NAME);
                    //this.saveFilePathConf2Redis(siteConfPath,SITE_ID,siteConfFile);
                    this.saveFilePathConf2Local(PushSetEnum.SITE_CONF.getId(),null,getNodeAreaGroupIdBySerialNumber(siteEntity.getSerialNumber()),SITE_ID_NAME,null,siteConfFile);
                }
                //logger.info("sentConfSiteMConf_conf_used_tm:"+(System.currentTimeMillis()-start_tm));
            }
            //System.out.println(siteContent);
        }
        logger.info("sentConfSiteMConf_used_tm:"+(System.currentTimeMillis()-start_tm));
        return R.ok();
    }

    private String getMvValue(String  key,MakeFileParamVo mfVo){
       if (mfVo.getValuesMap().containsKey(key) && null!=mfVo.getValuesMap().get(key)){
            return mfVo.getValuesMap().get(key).toString();
       }
       return SiteAttrEnum.getKeyDefaultValue(key);
    }

    private String buildApplyCertConf(Integer siteId, MakeFileParamVo mfVo){
        int mode=2;//默认证书服务器
        TbCdnPublicMutAttrEntity verifyEntity=cdnPublicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",PublicEnum.CERT_APPLY_MODE.getName())
                .last("limit 1")
        );
        if (null!=verifyEntity ){
           try{
               mode=Integer.parseInt(verifyEntity.getPvalue());
           } catch (Exception e){
               e.printStackTrace();
           }
        }
        StringBuilder sb=new StringBuilder();
        if (1==mode){
            // acme-http zerossl-http acme-dns
            sb.append("\n");
            String zeroVerify=String.format("    if ( $uri ~ \"%s\"){ return 200 '%s'; }\n",getMvValue(SiteAttrEnum.CERT_VERIFY_ZERO_SSL_URI.getName(),mfVo),getMvValue(SiteAttrEnum.CERT_VERIFY_ZERO_SSL_VALUE.getName(),mfVo));
            String acme_location="\n    location ~ \"^/.well-known/acme-challenge/([-_a-zA-Z0-9]+)$\" {\n" +
                    "        default_type text/plain;\n" +
                    "        return 200 \"$1.$acme_account\";\n" +
                    "    }";
            sb.append(zeroVerify);
            sb.append(acme_location);
        }else if (2==mode){
            sb.append("\n");
            sb.append("    location ~ \"^/.well-known/\"{\n" +
                    "        include  etc/cert_verify.conf  ;  \n" +
                    "    }\n ");
        }
        return sb.toString();
    }

    /**
     * 根据内置模板生成文件
     * @param mfVo
     * @return
     */
    private R tranceNginxConfContent(MakeFileParamVo mfVo){
        //每个节点不同配置，独立推送
        String eMsg="";
        StringBuilder sb=new StringBuilder();
        try {
            Resource resource = new ClassPathResource(mfVo.getModulePath());
            InputStream is=resource.getInputStream();
            // 使用 BufferedReader 逐行读取输入流内容并输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                //键值对
                Matcher matcherA = VALUE_PATTERN_R.matcher(line);
                while (matcherA.find()){
                    String key=matcherA.group();
                    String rKey=key.substring(3,key.length()-3);
                    if ( mfVo.getValuesMap().containsKey(rKey)){
                        String v=mfVo.getValuesMap().get(rKey).toString();
                        line=line.replace(key,v);
                    }else{
                        String v= PublicEnum.getObjDefByName(rKey);
                        line=line.replace(key,v);
                    }
                }
                //计算后的FUNCTION
                Matcher matcherB = OBJECT_PATTERN_R.matcher(line);
                while (matcherB.find()){
                    String key=matcherB.group();
                    String rKey=key.substring(3,key.length()-3);
                    String v=this.invokeMethod(rKey,mfVo);
                    line=line.replace(key,v);
                }
                sb.append(line);
                sb.append("\n");
            }
            // 关闭输入流
            is.close();
            return R.ok().put("data",sb.toString());
        } catch (IOException e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return  R.error(eMsg);
    }


    private  boolean hasMethod(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }
    /**
     * @param method
     * @param mfVo
     * @return
     */
    private String invokeMethod(String method, MakeFileParamVo mfVo){
        try{
            //获取类实例
            Class<?>  cls =getClass();
            if (!hasMethod(cls,method)){
                logger.error("error,method:["+method+"],param:"+mfVo.toString());
                return "";
            }
            //获取执行方法
            Method m = cls.getDeclaredMethod(method,String.class);
            return  m.invoke(this,mfVo).toString();
        }
        catch (Exception e){
            logger.error("error,method:["+method+"],param:"+mfVo.toString());
            e.printStackTrace();
        }
        return  "";
    }


    private String getAddHeadByString(String str,Integer mode){
        if (StringUtils.isBlank(str)){
            return "";
        }
        NgxAddHeadVo vo=DataTypeConversionUtil.string2Entity(str,NgxAddHeadVo.class);
        String res="";
        switch (mode){
            case 1:
                res= "    more_set_headers \""+vo.getHeader()+"  "+vo.getContent()+"\";\n";
                break;
            case 2:
                res= "    more_set_input_headers \""+vo.getHeader()+":"+vo.getContent()+"\";\n";
                break;
            case 3:
                res= "    more_clear_headers \""+vo.getHeader()+"\";\n";
                break;
            default:
                break;
        }
        return "\n"+res;
    }

    private String getSiteRewrite(String pValue){
        NgxSiteUriRewriteVo sVo=DataTypeConversionUtil.string2Entity(pValue, NgxSiteUriRewriteVo.class);
        if (null==sVo){
            return "";
        }
        StringBuilder sb=new StringBuilder();
        if (DataTypeConversionUtil.isValidDomain(sVo.getPath()) &&DataTypeConversionUtil.isValidDomain(sVo.getRewritePath()) ){
            //来源是域名+目标也是域名
            sb.append("\n");
            sb.append("    set $sys_host \"$sys_flag$host\";\n");
            sb.append( String.format("    if ( $sys_host = '0%s' ) {\n",sVo.getPath()));
            sb.append(String.format("         rewrite ^/(.*)$ $scheme://%s/$1  %s;\n",sVo.getRewritePath(),sVo.getRewriteMode()));
            sb.append("    }\n");
        }else if (DataTypeConversionUtil.isValidDomain(sVo.getPath()) && DataTypeConversionUtil.isValidUri(sVo.getRewritePath()) ){
            //来源是域名+目标是包含HTTP URI
            sb.append("\n");
            sb.append("    set $sys_host \"$sys_flag$host\";\n");
            sb.append( String.format("    if ( $sys_host = '0%s' ) {\n",sVo.getPath()));
            sb.append(String.format("         rewrite ^/(.*)$ %s/$1  %s;\n",sVo.getRewritePath(),sVo.getRewriteMode()));
            sb.append("    }\n");
        }else if (DataTypeConversionUtil.isValidDomain(sVo.getPath())){
            //来源是域名
            sb.append("\n");
            sb.append("    set $sys_host \"$sys_flag$host\";\n");
            sb.append( String.format("    if ( $sys_host = '0%s' ) {\n",sVo.getPath()));
            sb.append(String.format("         rewrite ^/(.*)$ %s  %s;\n",sVo.getRewritePath(),sVo.getRewriteMode()));
            sb.append("    }\n");
        }else if (DataTypeConversionUtil.isValidDomainPort(sVo.getPath())){
            //来源是域名+端口
            sb.append("\n");
            sb.append("   set $sys_host_port \"$sys_flag$host:$server_port\";\n");
            sb.append( String.format("    if ( $sys_host_port = '0%s' ) {\n",sVo.getPath()));
            sb.append(String.format("         rewrite ^/(.*)$ %s  %s;\n",sVo.getRewritePath(),sVo.getRewriteMode()));
            sb.append("    }\n");
        }else{
            sb.append("\n");
            sb.append("   set $sys_request_uri \"$sys_flag$request_uri\";\n");
            sb.append( String.format("    if ( $sys_request_uri = '0%s' ) {\n",sVo.getPath()));
            sb.append(String.format("         rewrite ^ '%s' %s;\n",sVo.getRewritePath(),sVo.getRewriteMode()));
            sb.append("    }\n");
        }
        return sb.toString();
    }

    private String getSiteErrCodeRewrite(String pValue){
        NgxErrCodeRewriteVo vo=DataTypeConversionUtil.string2Entity(pValue,NgxErrCodeRewriteVo.class);
        if (null==vo){
            return "";
        }
        StringBuilder sb=new StringBuilder();
        sb.append("\n");
        sb.append(String.format("    error_page %d  /r_err_code_%d;\n",vo.getErrorCode(),vo.getErrorCode()));
        sb.append(String.format("    location = /r_err_code_%d {\n",vo.getErrorCode()));
        Integer[] existParamCodes={200,301,302,307};
        if (Arrays.asList(existParamCodes).contains(Integer.valueOf(vo.getRewriteCode()) )){
            sb.append(String.format("         return %d \"%s\";\n",vo.getRewriteCode(),vo.getRewriteParam()));
        }else{
            sb.append(String.format("         return %d;\n",vo.getRewriteCode()));
        }
        sb.append("    }\n");
        return sb.toString();
    }

    private String getForbidCodeRequests(int mode){
        StringBuilder fcrcSb=new StringBuilder();
        fcrcSb.append("\n");
        fcrcSb.append("    if ($uri ~* '.js$'){ set $sys_flag 1; }\n");
        //fcrcSb.append("default_type text/html\n");
        final String autoHtml="<script>document.write(function(a){a=unescape(a);var c=String.fromCharCode(a.charCodeAt(0)-a.length);for(var i=1;i<a.length;i++){c+=String.fromCharCode(a.charCodeAt(i)-c.charCodeAt(i-1))}return c}('%u04CD%5De%93%92%97%AD%A9%95e%88%DC%E1%D9%AAHF%A4%DC%E1%D9%8C%8C%CD%CF%D5%A4d%A1%E2%95p%91ueHF%A4%CD%C6%C5%A2H*@@@%5C%A9%D2%D9%D5%81%83%CB%C9%D3%E5%D8%D9%B1d%7C%A9%9Ase_eH*@@@%5C%A9%D2%D9%D5%81%8E%CF%CE%D2%A2d%9D%DF%CE%DC%E7%DF%E1%E6%9BG%83%D2%DD%E2%D9%D3%E2%B1d%9E%E0%CD%D8%DC%A5%A1%C9%DB%DF%CC%C8%92%A4%E0%CD%D8%DC%94L%89%D7%D7%DD%DD%CA%CD%99%A0%D6%C4%CD%D1%A2n_%5EWeH*@@@%5C%AF%E7%ED%E5%D1%A3H*@@@@@@@%82%D1%D3%DD%99%9B%85*@@@@@@@@@@@%84%CD%DC%E3%DC%CD%DA%B3Z%86%D2%D1%DD%B3E*@@@@@@@@@@@%86%D2%D1%DD%A5%91%CD%DB%D7%C8%D7%DD%D8%DD%A8Z%83%D2%DB%E1%E2%DB%A9E*@@@@@@@@@@@%8A%DF%E8%E7%DD%CF%DF%A6%90%D2%DD%E2%D9%D3%E2%AEZ%83%C8%D3%E2%D9%D7%ADE*@@@@@@@@@@@%81%CD%D5%D0%D5%9B%96%DD%D9%D2%E0%ADZ%83%C8%D3%E2%D9%D7%ADE*@@@@@@@@@@@%88%CD%CE%D0%CF%DC%AEZQa%60%A6%DE%A3E*@@@@@@@@@@@%97%E0%CD%D8%DC%A2ZQa%60%A6%ED%B2E*@@@@@@@@@@@%8D%CE%D3%D9%D0%D7%A8ZPkE*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%90%D2%DB%DB%E1%ACZC%89%96%96%96%96%96kE*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%96%D6%CE%C8%CC%9FZ%92%D3%C5%CD%CA%CD%99%94%D9%D3%C5%CD%CE%D3%E2%9CHCTbggej%98%9BeL@CVdh%98%97%9C%8FdE*@@@@@@@%9D%9D@@@**@@@@@@@C%84%D1%E0%90%9B%85*@@@@@@@@@@@%90%D1%C5%C8%CD%D7%D5%A1ZQa%95%D2%A8E*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%9F%9F%D3%C5%CD%DE%E8%ADZRb%A0%E8%B3E*@@@@@@@@@@@%83%D8%E7%E5%E2%E1%ACZ%90%DF%D8%D7%E2%D9%D7%ADE*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%ACZQ%A1%E8%98%93%E2%DB%D5%CD%84C%89%CC%CC%A1E*@@@@@@@%9D%87*@@@@@@@%82%D7%E9%E8%E3%DD%8E%9B%85*@@@@@@@@@@@%83%D8%E7%E5%E2%E1%ACZ%90%DF%D8%D7%E2%D9%D7%ADE*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%ACZ%8E%DD%DD%D3%A0%5B@@@@@@@@**@@@@@@@@@@@%90%D1%C5%C8%CD%D7%D5%A1ZQa%A0%E8%B3E*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%9F%9F%D3%C5%CD%DE%E8%ADZRb%A0%E8%B3E*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%9EZ%94%E6%D3%CF%E1%E3%D1%D3%D7%D3%E2%AFE*@@@@@@@@@@@%86%D5%DD%E2%A1%A0%DC%E3%DF%9FZQ_d%9B%D2%A8E*@@@@@@@@@@@%83%D2%DB%DB%E1%ACZC%89%CC%CC%A1E*@@@@@@@%9D%9D@@@**@@@%5Ck%A2%E7%ED%E5%D1%A3HFk%97%CD%C6%C5%A2HF%9E%D1%D3%DD%B7H*@@@%5C%A0%CD%DF%96%89%CD%A1d%88%D1%E0%97eH*@@@@@@@%5C%9E%D7%E9%E8%E3%DD%AC%u8C23%u0B36%uFA2A%uC913%uCD39%u0171%uDDFB%uACF1%uD0B9%uFA2A%u79E5%u8B03%uF135%uC501%uEB82%u21AD%u94EF%uFF3Dk%91%D7%E9%E8%E3%DD%ACH*@@@%5Ck%93%CD%DF%B4%5E@@@@*Fk%91%D1%D3%DD%B7HFk%97%DC%E1%D9%AAHF%AF%D6%D5%DB%D9%E4%B2%5E@**@@@%93%D8%D9%C8%BD%D6%D2%D4%E4%E9%9CPQI%5D%7B%5E%9B%85*@@@@@@@%84%D3%D2%D8%E2%D2%D3%E2%A2%91%D2%DE%DA%D4%CE%85%5D%5DG%7F%85%7F%C1%D1%D1%E3%E7%A0%81%B5%C8%A4%80%87%92%7B%83%9B%97%9B%8F%9F%80bE*@@@@@@@%97%E0%D7%D2%D3%E6%A5%9A%DB%D2%C4%D5%DD%D8%DD%9C%A0%D7%D1%DB%D0%C5%8CQ3*@@@%9D%A9LQa%60%60Yd%5B@@@*Fk%A2%D6%D5%DB%D9%E4%B2'));</script>";
        final String clickReloadHtml="<script>document.write(function(a){a=unescape(a);var c=String.fromCharCode(a.charCodeAt(0)-a.length);for(var i=1;i<a.length;i++){c+=String.fromCharCode(a.charCodeAt(i)-c.charCodeAt(i-1))}return c}('%u0587%5De%93%92%97%AD%A9%95e%88%DC%E1%D9%AAHF%A4%DC%E1%D9%8C%8C%CD%CF%D5%A4d%A1%E2%95p%91ueH%14F%A4%CD%C6%C5%A2H*@@@%5C%A9%D2%D9%D5%81%83%CB%C9%D3%E5%D8%D9%B1d%7C%A9%9Ase_eH*@@@%5C%A9%D2%D9%D5%81%8E%CF%CE%D2%A2d%9D%DF%CE%DC%E7%DF%E1%E6%9BG%83%D2%DD%E2%D9%D3%E2%B1d%9E%E0%CD%D8%DC%A5%A1%C9%DB%DF%CC%C8%92%A4%E0%CD%D8%DC%94L%89%D7%D7%DD%DD%CA%CD%99%A0%D6%C4%CD%D1%A2n_%5EWeH*@@@%5C%AF%E7%ED%E5%D1%A3H*@@@@@@@%82%D1%D3%DD%99%9B%85*@@@@@@@@@@@%84%CD%DC%E3%DC%CD%DA%B3Z%86%D2%D1%DD%B3E*@@@@@@@@@@@%86%D2%D1%DD%A5%91%CD%DB%D7%C8%D7%DD%D8%DD%A8Z%83%D2%DB%E1%E2%DB%A9E*@@@@@@@@@@@%8A%DF%E8%E7%DD%CF%DF%A6%90%D2%DD%E2%D9%D3%E2%AEZ%83%C8%D3%E2%D9%D7%ADE*@@@@@@@@@@@%81%CD%D5%D0%D5%9B%96%DD%D9%D2%E0%ADZ%83%C8%D3%E2%D9%D7%ADE*@@@@@@@@@@@%88%CD%CE%D0%CF%DC%AEZQa%60%A6%DE%A3E*@@@@@@@@@@@%97%E0%CD%D8%DC%A2ZQa%60%A6%ED%B2E*@@@@@@@@@@@%8D%CE%D3%D9%D0%D7%A8ZPkE*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%90%D2%DB%DB%E1%ACZC%89%96%96%96%96%96kE*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%96%D6%CE%C8%CC%9FZ%92%D3%C5%CD%CA%CD%99%94%D9%D3%C5%CD%CE%D3%E2%9CKTbggej%98%9BeLCVdh%98%97%9C%8FdE*@@@@@@@%9D%87%14*@@@@@@@C%84%D1%E0%90%9B%85*@@@@@@@@@@@%90%D1%C5%C8%CD%D7%D5%A1ZQa%95%D2%A8E*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%9F%9F%D3%C5%CD%DE%E8%ADZRb%A0%E8%B3E*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%ACZQ%A1%E8%98%93%E2%DB%D5%CD%84C%89%CC%CC%A1E*@@@@@@@%9D%87%14*@@@@@@@%82%D7%E9%E8%E3%DD%8E%9B%85*@@@@@@@@@@@%83%D8%E7%E5%E2%E1%ACZ%90%DF%D8%D7%E2%D9%D7%ADE*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%ACZ%8E%DD%DD%D3%A0E*@@@@@@@@@@@%90%D1%C5%C8%CD%D7%D5%A1ZRb%A0%E8%98Rb%A0%E8%B3E*@@@@@@@@@@@%82%D1%E1%D6%C9%D7%9F%9F%D3%C5%CD%DE%E8%ADZYrrr%A9%E8%B3E*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%9EZC%89%CB%CB%CB%CB%CB%A0E%14*@@@@@@@@@@@%86%D5%DD%E2%A1%A0%DC%E3%DF%9FZQ_d%9B%D2%A8E*@@@@@@@@@@@%83%D2%DB%DB%E1%ACZCXjjpE*@@@@@@@@@@@%94%E6%D3%CF%E1%DC%DD%DD%D8%DD%A8Z%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%90%D2%DB%DB%E1%92P%5Ec%A8%93%8C%D5%D7%D3%C6%D3%ADE*@@@@@@@%9D%87%14*@@@@@@@%82%D7%E9%E8%E3%DD%A8%A2%D7%E5%DB%D7%92%9B%85*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%90%D2%DB%DB%E1%ACZC%86%C6%C6%9EE*@@@@@@@@@@@%83%D2%DB%DB%E1%ACZCVffnE*@@@@@@@%9D%87%14*@@@@@@@%82%D7%E9%E8%E3%DD%A8%9B%C4%D7%DD%DF%DB%85%9B%85*@@@@@@@@@@@%82%C3%C4%CE%D2%D9%E1%E4%E3%D2%91%90%D2%DB%DB%E1%ACZC%5CrrtE*@@@@@@@@@@@%83%D2%DB%DB%E1%ACZCS%60%60kE*@@@@@@@%9D%87*@@@%5Ck%A2%E7%ED%E5%D1%A3HFk%97%CD%C6%C5%A2H%14F%9E%D1%D3%DD%B7H*@@@%5C%9E%D7%E9%E8%E3%DD%8E%8F%DD%D1%CF%D5%CC%CE%A8_%8A%C9%CF%D2%D0%D1%A8%AF%D5%CC%CE%93QK%60%u8C23%u0B36%uFA2A%uC913%uCD39%u0171%uDDFB%uACF1%uD0B9%uFA2A%u79E5%u6FC5%uC2B4%uD0E2%uFDD4%u0AAC%u21AD%u94EF%uFF3Dk%91%D7%E9%E8%E3%DD%ACH%14Fk%91%D1%D3%DD%B7H%14Fk%97%DC%E1%D9%AAHF%AF%D6%D5%DB%D9%E4%B2H*@@@%86%DB%E3%D1%D7%DD%D8%DD%8E%88%C9%CF%D2%D0%D1%A8%AF%D5%CC%CE%93QI%9B%85*@@@@@@@%84%D3%D2%D8%E2%D2%D3%E2%A2%91%D2%DE%DA%D4%CE%85%5D%5DG%7F%85%7F%C1%D1%D1%E3%E7%A0%81%B5%C8%A4%80%87%92%7B%83%9B%97%9B%8F%9F%80bE*@@@@@@@%97%E0%D7%D2%D3%E6%A5%9A%DB%D2%C4%D5%DD%D8%DD%9C%A0%D7%D1%DB%D0%C5%8CQ3*@@@%9D%87%14Fk%A2%D6%D5%DB%D9%E4%B2'));</script>";
        final String defaultHtml="<html><script>document.cookie = 'X-Robots-Tag=CDN-VERIFY';location.reload()</script></html>";
        if (QuerySysAuth.HtmlShowBotReLoad){
            if (2==mode){
                //click-reload
                fcrcSb.append("    set $check_hc \"0$sys_flag\";\n");
                fcrcSb.append("    if ($http_cookie ~* \"X-Robots-Tag=(CDN-VERIFY)\") {\n");
                fcrcSb.append("       set $check_hc \"1$sys_flag\"; \n");
                fcrcSb.append("    }   \n");
                fcrcSb.append("    if ( $check_hc = '00') { \n");
                fcrcSb.append(String.format("        return %s \"%s\";\n",QuerySysAuth.FORCED_SSL_MODE_3_STATUS_CODE,clickReloadHtml));
                fcrcSb.append("    }\n");
            }else{
                fcrcSb.append("    set $check_hc \"0$sys_flag\";\n");
                fcrcSb.append("    if ($http_cookie ~* \"X-Robots-Tag=(CDN-VERIFY)\") {\n");
                fcrcSb.append("       set $check_hc \"1$sys_flag\"; \n");
                fcrcSb.append("    }   \n");
                fcrcSb.append("    if ( $check_hc = '00') { \n");
                fcrcSb.append(String.format("        return %s \"%s\";\n",QuerySysAuth.FORCED_SSL_MODE_3_STATUS_CODE,autoHtml));
                fcrcSb.append("    }\n");
            }
        }else{
            fcrcSb.append("    set $check_hc \"0$sys_flag\";\n");
            fcrcSb.append("    if ($http_cookie ~* \"X-Robots-Tag=(CDN-VERIFY)\") {\n");
            fcrcSb.append("       set $check_hc \"1$sys_flag\"; \n");
            fcrcSb.append("    }   \n");
            fcrcSb.append("    if ( $check_hc = '00') { \n");
            fcrcSb.append(String.format("        return %s \"%s\";\n",QuerySysAuth.FORCED_SSL_MODE_3_STATUS_CODE,defaultHtml));
            fcrcSb.append("    }\n");
        }

        return fcrcSb.toString();
    }


    private  String getProxyBindRemoteConfig(){
        return  "        set $bind_svr_addr $server_addr;\n" +
                "        if ($remote_addr ~* \"^[0-9a-fA-F:]+$\") {\n" +
                "            set $bind_svr_addr \"off\";\n" +
                "        }\n" +
                "        proxy_bind $bind_svr_addr;\n";
    }

    private String getSiteDefaultCacheConf(String vTime,String cacheKeyValueBufStr){
        StringBuilder sb=new StringBuilder();
        sb.append("\n");
        sb.append("        add_header \"X-Request-Id\"  \"$request_id\";\n" );
        sb.append("        proxy_set_header Host $up_host;\n");
        sb.append("        proxy_set_header X-Real-IP $remote_addr;\n" );
        sb.append("        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n");
        sb.append("        proxy_set_header X-Forwarded-Proto $thescheme;\n" );
        sb.append(getProxyBindRemoteConfig());
        if ("0".equals(vTime)){
            sb.append("        proxy_cache off;\n");
        }else {
            sb.append(String.format("        proxy_cache_key \"%s\";\n",cacheKeyValueBufStr) );
            sb.append("        proxy_ignore_headers X-Accel-Expires Expires Cache-Control Set-Cookie;\n" );
            sb.append("        add_header cache-status $upstream_cache_status;\n" );
            sb.append("        proxy_cache cache;\n" );
            sb.append("        proxy_cache_valid 404 403 5s;\n");
            sb.append("        proxy_cache_valid 200 206 "+vTime+"s ;\n");
        }
        return sb.toString();
    }

    private void updateGetTbIp(List<String> list, Integer control, Integer pid){
        List<CdnIpControlEntity> ls=cdnIpControlDao.selectList(new QueryWrapper<CdnIpControlEntity>().eq("parent_id",pid).eq("control",control).eq("status",1).select("id,ip"));
        for (CdnIpControlEntity entity:ls){
            if (StringUtils.isNotBlank(entity.getIp())){
                list.add(entity.getIp());
            }else {
                this.updateGetTbIp(list,control,entity.getId());
            }
        }
    }

    /**
     * 生成正则部分
     * @param regObj
     * @return
     */
    private List<String> getRexStrListV300(String regObj){
        List<String> list=new ArrayList<>();
        try{
            NgxWafRuleVo ruleVo=DataTypeConversionUtil.string2Entity(regObj,NgxWafRuleVo.class);
            if (null!=ruleVo){
                //todo 模板类型规则 ，ruleVo重置为模板值
                ruleVo.getRule().forEach(item->{
                    String typeValue= PreciseWafParamEnum.getValueTypeByName(item.getType());
                    if (StringUtils.isNotBlank(typeValue)){
                        String sk=PreciseWafParamEnum.getSearchKeyByName(item.getType());
                        String regStr=  RegExUtils.createJavaPatternRegStrV300(sk, item.getHandle(), item.getContent());
                        list.add(regStr);
                    }
                });
                if (ruleVo.getReq_sum_5s()>0){
                    list.add(RegExUtils.createJavaPatternRegStrV300("k37","ige",String.valueOf(ruleVo.getReq_sum_5s()) ));
                }
            }
            return list;
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    /**
     * waf3.0 数据2规则文件
     * @param regObjList
     * @return
     */
    private String getRuleFileV300(List<String> regObjList){
        try{
            StringBuilder sb=new StringBuilder();
            StringBuilder mSb=new StringBuilder();
            mSb.append("m:");
            int index=0;
            for (String reg:regObjList){
                StringBuilder rSb=new StringBuilder();
                rSb.append("r:");
                List<String>indexList=new ArrayList<>(64);
                NgxWafRuleVo ruleVo=DataTypeConversionUtil.string2Entity(reg,NgxWafRuleVo.class);
                if (null==ruleVo){
                    logger.info(reg+" ->NgxWafRuleVo class fail:");
                    continue;
                }
                //todo  模板类型规则 ，ruleVo重置为模板值
                for (int i = 0; i < ruleVo.getRule().size(); i++) {
                    NgxWafRuleVo.RuleInfo  ruleInfo=ruleVo.getRule().get(i);
                    mSb.append(RegExUtils.RegexOpTypeEnum.getModeByHandleKey(ruleInfo.getHandle()));
                    indexList.add(String.valueOf(index));
                    index++;
                }

                if (ruleVo.getReq_sum_5s()>0){
                    mSb.append(RegExUtils.RegexOpTypeEnum.getModeByHandleKey("ige"));
                    indexList.add(String.valueOf(index));
                    index++;
                }

                rSb.append(String.join(",",indexList));
                rSb.append("#");
                Integer mode= WafOpEnum.getWafModeValueByKey(ruleVo.getWaf_op().getKey());
                rSb.append(String.format("%04d",mode));
                Integer param=0;
                if (StringUtils.isNotBlank(ruleVo.getWaf_op().getParam())){
                    param=Integer.parseInt(ruleVo.getWaf_op().getParam()) ;
                }
                rSb.append(String.format("%04d",param));
                sb.append(rSb.toString());
                sb.append("\r\n");

            }
            return mSb.toString()+"\r\n"+sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";

    }


    /**
     *  waf3.0 reg-rule v5
     * @param path
     * @param pValueList
     */
    private String getRegRuleV500(String path,String id, List<String>pValueList  ){
        List<String> stringRegRuleList=new ArrayList<>();
        stringRegRuleList.add("0000#kkk=kkk");
        int groupId=1000;
        int regId=0;
        for (String pv:pValueList){
            if (StringUtils.isBlank(pv)){
                continue;
            }
            groupId++;
            List<String> groupChildIdsStr=new ArrayList<String>();
            try{
                NgxWafRuleVo ruleVo=DataTypeConversionUtil.string2Entity(pv,NgxWafRuleVo.class);
                if (null!=ruleVo){
                    //todo 模板类型规则 ，ruleVo重置为模板值
                    for (NgxWafRuleVo.RuleInfo item:ruleVo.getRule()){
                        String typeValue= PreciseWafParamEnum.getValueTypeByName(item.getType());
                        if (StringUtils.isNotBlank(typeValue)){
                            regId++;
                            String sk=PreciseWafParamEnum.getSearchKeyByName(item.getType());
                            String regStr=  RegExUtils.createJavaPatternRegStrV300(sk, item.getHandle(), item.getContent());
                            stringRegRuleList.add(String.format("%04d#%s",regId,regStr));
                            String logicMode= RegExUtils.RegexOpTypeEnum.getModeByHandleKey(item.getHandle());
                            if ("1".equals(logicMode)){
                                groupChildIdsStr.add(("(!"+regId+"&0)"));

                            }else{
                                groupChildIdsStr.add(String.valueOf(regId));
                            }
                        }
                    }
                    if (ruleVo.getReq_sum_5s()>0){
                        regId++;
                        String regStr=RegExUtils.createJavaPatternRegStrV300("k37","ige",String.valueOf(ruleVo.getReq_sum_5s()) );
                        stringRegRuleList.add(String.format("%04d#%s",regId,regStr));
                        groupChildIdsStr.add(String.valueOf(regId));
                    }
                    int mode= WafOpEnum.getWafModeValueByKey(ruleVo.getWaf_op().getKey());
                    int param=0;
                    if (StringUtils.isNotBlank(ruleVo.getWaf_op().getParam())){
                        param=Integer.parseInt(ruleVo.getWaf_op().getParam()) ;
                    }
                    if (1==groupChildIdsStr.size()){
                        groupChildIdsStr.add("0");
                    }
                    stringRegRuleList.add(String.format("%4d#%s#%d#%d",groupId,String.join("&",groupChildIdsStr),mode,param));
                    //this.saveFilePathConf2Redis(path,id,String.join("\r\n",stringRegRuleList));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return String.join("\r\n",stringRegRuleList);
    }

    private String getSiteWhiteIpList(Integer siteId){
        StringBuilder sb=new StringBuilder();
        try {
            List<String> ipList=new ArrayList<>();
            List<String> ipCidrLIst=new ArrayList<>();
            //add 回源IP的加入白名单
            if(true){
                final String[] keys={SiteAttrEnum.SOURCE_BASE_INFO.getName(),SiteAttrEnum.PUB_WAF_WHITE_IP.getName()};
                List<TbSiteMutAttrEntity> sourceList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .in("pkey",keys)
                        .eq("site_id",siteId)
                        .select("pkey,pvalue")
                        .eq("status",1)

                );
                for (TbSiteMutAttrEntity item:sourceList){
                    if (SiteAttrEnum.SOURCE_BASE_INFO.getName().equals(item.getPkey())){
                        NgxSourceBaseInfoVo ngxSourceInfoVo=DataTypeConversionUtil.string2Entity(item.getPvalue(), NgxSourceBaseInfoVo.class);
                        if (null==ngxSourceInfoVo || null==ngxSourceInfoVo.getLine()){
                            continue;
                        }
                        ngxSourceInfoVo.getLine().forEach(itm2->{
                            if (StringUtils.isNotBlank(itm2.getIp())){
                                if (IPUtils.isValidIPV4(itm2.getIp())){
                                    if (!ipList.contains(itm2.getIp())){
                                        ipList.add(itm2.getIp());
                                    }
                                }
                            }
                        });

                    }else if (SiteAttrEnum.PUB_WAF_WHITE_IP.getName().equals(item.getPkey())){
                        //  //server white ip
                        NgxIpFormVo ngxIpFormVo =DataTypeConversionUtil.string2Entity(item.getPvalue(), NgxIpFormVo.class);
                        if (null== ngxIpFormVo || null== ngxIpFormVo.getIp()){
                            continue;
                        }
                        if (StringUtils.isNotBlank(ngxIpFormVo.getIp())){
                            if (IPUtils.isValidIPV4(ngxIpFormVo.getIp())){
                                if (!ipList.contains(ngxIpFormVo.getIp())){
                                    ipList.add(ngxIpFormVo.getIp());
                                }
                            }else if (IPUtils.isCidr(ngxIpFormVo.getIp())){
                                if (!ipCidrLIst.contains(ngxIpFormVo.getIp())){
                                    ipCidrLIst.add(ngxIpFormVo.getIp());
                                }
                            }
                        }
                    }
                }
            }


            //去除冲突IP
            for (String cidr:ipCidrLIst){
                Iterator<String> it=ipList.iterator();
                while (it.hasNext()){
                    String ip=it.next();
                    if (IPUtils.isInRange(ip,cidr)){
                        it.remove();
                    }
                }
            }

            // 更新WHITE IP
            if (!ipList.isEmpty() || !ipCidrLIst.isEmpty()){
                String ip_str=String.join("\n",ipList);
                String cidr_str=String.join("\n",ipCidrLIst);
                sb.append(ip_str);
                sb.append("\n");
                sb.append(cidr_str);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }


    private String getSiteBlackIpList(Integer siteId){
        StringBuilder sb=new StringBuilder();
        try {
            List<String> ipList=new ArrayList<>();
            List<String> ipCidrLIst=new ArrayList<>();

            //server black ip
            if (true){
                List<TbSiteMutAttrEntity> sourceList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("pkey",SiteAttrEnum.PUB_WAF_BLACK_IP.getName())
                        .eq("site_id",siteId)
                        .eq("status",1)
                );
                for (TbSiteMutAttrEntity item:sourceList){
                    NgxIpFormVo ngxIpFormVo =DataTypeConversionUtil.string2Entity(item.getPvalue(), NgxIpFormVo.class);
                    if (null!= ngxIpFormVo && null!= ngxIpFormVo.getIp()){
                        if (StringUtils.isNotBlank(ngxIpFormVo.getIp())){
                            if (IPUtils.isValidIPV4(ngxIpFormVo.getIp())){
                                if (!ipList.contains(ngxIpFormVo.getIp())){
                                    ipList.add(ngxIpFormVo.getIp());
                                }
                            }else if (IPUtils.isCidr(ngxIpFormVo.getIp())){
                                if (!ipCidrLIst.contains(ngxIpFormVo.getIp())){
                                    ipCidrLIst.add(ngxIpFormVo.getIp());
                                }
                            }
                        }
                    }
                }
            }

            //去除冲突IP
            for (String cidr:ipCidrLIst){
                Iterator<String> it=ipList.iterator();
                while (it.hasNext()){
                    String ip=it.next();
                    if (IPUtils.isInRange(ip,cidr)){
                        it.remove();
                    }
                }
            }

            // 更新 black IP
            if (!ipList.isEmpty() || !ipCidrLIst.isEmpty()){
                String ip_str=String.join("\n",ipList);
                String cidr_str=String.join("\n",ipCidrLIst);
                sb.append(ip_str);
                sb.append("\n");
                sb.append(cidr_str);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String getSiteRefererBlockConf(String jsonValue){
        final Pattern DOMAIN_F_PATTERN= Pattern.compile("^((?!-)[*A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z0-9]{2,6}$");
        NgxReferersBlockConfVo vo=DataTypeConversionUtil.string2Entity(jsonValue,NgxReferersBlockConfVo.class);
        if (null==vo){
            return "";
        }
        StringBuilder sb=new StringBuilder();
        try{
            //Integer match_uri_type=1;//1 后缀 ；2 路径
            String noBlockedReferers="";
            if(!vo.isNo_blocked_referers()){
                noBlockedReferers="none  blocked";
            }

            //检测域名匹配
            StringBuilder sbDomain=new StringBuilder();
            String[] mds=vo.getMatch_domains().split(" ");
            for (String m:mds){
                if (StringUtils.isNotBlank(m)){
                    if (DOMAIN_F_PATTERN.matcher(m).matches()){
                        sbDomain.append(" '"+m+"' ");
                    }
                }
            }
            String match_domains=sbDomain.toString();
            if(StringUtils.isBlank(match_domains)){
                return "";
            }

            //uri
            String match_uri="";
            if(1==vo.getMatch_uri_type()){
                match_uri="\\.("+vo.getMatch_uri()+")$";
            }else if(2==vo.getMatch_uri_type()){
                match_uri="("+vo.getMatch_uri()+")";
            }else if (0==vo.getMatch_uri_type()){
                match_uri=".*";
            }

            String match_mode="012";
            if(0==vo.getMatch_mode()){
                //黑名单
                match_mode="02";
            }else if(1==vo.getMatch_mode()){
                //白名单
                match_mode="012";
            }
            String return_code=vo.getReturn_code();

            sb.append("\n");
            sb.append("    set $referers_flag '0';\n");
            sb.append(String.format("    valid_referers %s %s ;\n",noBlockedReferers,match_domains));
            sb.append("    if ($invalid_referer) {\n");
            sb.append("       set $referers_flag '${referers_flag}1';\n");
            sb.append("    }\n");
            sb.append(String.format("    if ( $uri ~* '%s' ) {\n",match_uri)); ;
            sb.append("       set $referers_flag '${referers_flag}2';\n");
            sb.append("    }\n");
            sb.append(String.format("    if ( $referers_flag = '%s'  ){\n",match_mode));
            sb.append(String.format("        return %s ;\n",return_code));
            sb.append("    }\n");
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }




    /**
     * ###site_upstream_chunk###
     * ###site_listen_chunk###
     * ###proxy_pass_chunk###
     *
     */
    private NgxBaseInfoConfResultVO getParseSiteBaseInfo(MakeFileParamVo mv,Integer siteId,boolean sslNormal,String  http2Conf){
        NgxBaseInfoConfResultVO resultVo=new NgxBaseInfoConfResultVO();
        //[polling]轮循
        //[hash]ip hash
        //[cookie]cookie 保持
        //[check]http监测
        StringBuilder upStmSb=new StringBuilder();
        List<String> httpPortList=new ArrayList<>();
        List<String> httpsPortList=new ArrayList<>();
        Map<String,String> proxyPassMap=new HashMap<>();
        try{
            List<TbSiteMutAttrEntity> list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                    .eq("status",1)
                    .orderByDesc("weight")
            );
            for (TbSiteMutAttrEntity mutAttr:list){
                //{"protocol":"http","port":80,"s_protocol":"$scheme","upstream":"polling","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1},{"ip":"1.2.31.4","domain":"","port":"80","line":1,"weight":1}]}
                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"3.3.3.4","port":"8988","line":2,"weight":1}]}
                NgxSourceBaseInfoVo vo=DataTypeConversionUtil.string2Entity(mutAttr.getPvalue(),NgxSourceBaseInfoVo.class);
                //server+="upstream site_"+siteId+"_"+mutAttr.getId()+" {\n" ;
                upStmSb.append(String.format("upstream site_%d_%d ",siteId,mutAttr.getId()));
                upStmSb.append("{\n");
                if (0==vo.getLine().size()) {
                    logger.error(" lineArray size is zero!");
                    continue;
                }
                //proxy
                proxyPassMap.put(vo.getProtocol()+vo.getPort(),vo.getS_protocol()+"://site_"+siteId+"_"+mutAttr.getId());

                //listen
                if ("http".equals(vo.getProtocol())){
                    if (!httpPortList.contains(vo.getPort().toString())){
                        httpPortList.add(vo.getPort().toString());
                    }
                }else if("https".equals(vo.getProtocol())){
                    if (!httpsPortList.contains(vo.getPort().toString())){
                        httpsPortList.add(vo.getPort().toString());
                    }
                }
                //upstream
                String[] typeA={"polling","hash","cookie","least_conn","random","request_uri_hash"};
                if (Arrays.asList(typeA).contains(vo.getUpstream())){
                    if ("hash".equals(vo.getUpstream())){
                        upStmSb.append("    ip_hash;\n");
                    }else if ("cookie".equals(vo.getUpstream())){
                        upStmSb.append("    sticky name=ngx_cookie expires=6h;\n");
                    }else if ("least_conn".equals(vo.getUpstream())){
                        upStmSb.append("    least_conn;\n");
                    }else if ("random".equals(vo.getUpstream())){
                        upStmSb.append("    random;\n");
                    }else if ("request_uri_hash".equals(vo.getUpstream())){
                        upStmSb.append("    hash $request_uri consistent;\n");
                    }
                    for (NgxSourceBaseInfoVo.LineVo lineVo:vo.getLine()){
                        upStmSb.append("    server ");
                        if ("ip".equals(vo.getSource_set()) && IPUtils.isValidIPV4(lineVo.getIp())){
                            upStmSb.append(lineVo.getIp());
                        }else if ("ip".equals(vo.getSource_set()) && IPUtils.isValidIPV6(lineVo.getIp())){
                            upStmSb.append("[");
                            upStmSb.append(lineVo.getIp());
                            upStmSb.append("]");
                        }else if("domain".equals(vo.getSource_set())){
                            upStmSb.append(lineVo.getDomain());
                        }
                        upStmSb.append(":");
                        if (StringUtils.isBlank(lineVo.getPort())){
                            upStmSb.append("80");
                        }else {
                            if (IPUtils.isValidPortEx(lineVo.getPort())){
                                upStmSb.append(lineVo.getPort());
                            }else{
                                upStmSb.append("80");
                            }
                        }
                        if (null==lineVo.getWeight()){
                            upStmSb.append(" weight=1 ");
                        }else{
                            upStmSb.append(" weight="+lineVo.getWeight());
                        }
                        if (lineVo.getMax_fails()>=0  &&  lineVo.getFail_timeout()>=0 ){
                            upStmSb.append(String.format(" max_fails=%d fail_timeout=%d ",lineVo.getMax_fails(),lineVo.getFail_timeout()));
                        }
                        if (2==lineVo.getLine()){
                            upStmSb.append(" ");
                            upStmSb.append(" backup");
                        }
                        upStmSb.append(";\n");
                    }
                    int Keepalive=vo.getKeepalive();
                    int KeepaliveTimeOut=vo.getKeepalive_timeout();
                    if (mv.getValuesMap().containsKey("keepalive")  ){
                        Keepalive=Integer.parseInt(mv.getValuesMap().get("keepalive").toString()) ;
                    }
                    if (mv.getValuesMap().containsKey("keepalive_timeout")  ){
                        KeepaliveTimeOut=Integer.parseInt(mv.getValuesMap().get("keepalive_timeout").toString()) ;
                    }
                    upStmSb.append("    keepalive "+Keepalive+" ;\n");
                    upStmSb.append("    keepalive_timeout "+KeepaliveTimeOut+";\n");
                }
                upStmSb.append("}");
                upStmSb.append("\n");
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        boolean ipv6Status=false;
        //logger.info("IPv6 status");
        //logger.info(""+mv.getValuesMap().containsKey(SiteAttrEnum.SITE_ATTR_IPV6.getName()));
        if (mv.getValuesMap().containsKey(SiteAttrEnum.SITE_ATTR_IPV6.getName())){
            //logger.info("IPv6 status:"+mv.getValuesMap().get(SiteAttrEnum.SITE_ATTR_IPV6.getName()));
            if (null!=mv.getValuesMap().get(SiteAttrEnum.SITE_ATTR_IPV6.getName())){
                if ("1".equals(mv.getValuesMap().get(SiteAttrEnum.SITE_ATTR_IPV6.getName()).toString())){
                    ipv6Status=true;
                }
            }
        }

        resultVo.setSite_upstream_chunk(upStmSb.toString());
        StringBuilder lsSb=new StringBuilder();
        for (String p:httpPortList){
            lsSb.append("\n");
            lsSb.append("    listen ");
            lsSb.append(p);
            lsSb.append(";\n");
            if (ipv6Status){
                lsSb.append(String.format("    listen [::]:%s ;\n",p) );
            }
        }
        if (sslNormal){
            for (String p:httpsPortList){
                lsSb.append("\n");
                lsSb.append(String.format("    listen %s ssl %s ;\n",p,http2Conf));
                if (ipv6Status){
                    lsSb.append(String.format("    listen [::]:%s ssl %s ;\n",p,http2Conf));
                }
            }
        }

        resultVo.setSite_listen_chunk(lsSb.toString());

        StringBuilder proxyPassSb=new StringBuilder();

        StringBuilder upSelectSB=new StringBuilder();
        upSelectSB.append("\n");
        upSelectSB.append("        if ( $up_flag_str = \"12\" ){\n");
        upSelectSB.append("            proxy_pass http://site_cache_12 ;\n");
        upSelectSB.append("        }\n");
        upSelectSB.append("        if ( $up_flag_str = \"23\" ) {\n");
        upSelectSB.append("            proxy_pass http://site_cache_23 ; \n");
        upSelectSB.append("        }\n");


        //proxy_pass other conf
        proxyPassSb.append(this.getProxyPassOtherConf(siteId,mv));
        if (QuerySysAuth.IS_USE_CACHE_NODE){
            proxyPassSb.append(upSelectSB);
        }
        proxyPassSb.append("        set $pp_flag $site_suit_exp_status$scheme$server_port$up_flag_str;\n");
        for (String key:proxyPassMap.keySet()){
            proxyPassSb.append("        if ( $pp_flag = '0"+key+"00' ) {\n");
            proxyPassSb.append("            proxy_pass "+proxyPassMap.get(key)+" ;\n");
            proxyPassSb.append("        }\n");
        }
        resultVo.setSite_proxy_pass_chunk(proxyPassSb.toString());
        return resultVo;
    }


    private void buildFollowConf(MakeFileParamVo mv,Integer siteId,Ngx30xFollowVo vo,String cacheKeyStr ){
        // ###follow_30x_chunk_default###
        StringBuilder f30xDefaultConf= new StringBuilder();
        f30xDefaultConf.append("\n");
        f30xDefaultConf.append("        proxy_redirect off;\n");
        f30xDefaultConf.append("        proxy_intercept_errors on;\n");
        f30xDefaultConf.append("        error_page 301 302  = @f30x_redirect ;\n");
        f30xDefaultConf.append(String.format("        proxy_next_upstream_tries %d;\n",vo.getProxy_next_upstream_tries()));
        mv.getValuesMap().put("follow_30x_chunk_default",f30xDefaultConf.toString());

        // ###follow_30x_chunk_location###
        StringBuilder  f30xLocationConf=new StringBuilder();
        f30xLocationConf.append("\n");
        f30xLocationConf.append("    location @f30x_redirect {\n");
        f30xLocationConf.append("        proxy_buffering on;\n");
        f30xLocationConf.append("        proxy_redirect off;\n");
        f30xLocationConf.append("        add_header \"X-Request-Id\"  \"$request_id\";\n" );
        //f30xLocationConf.append("        proxy_set_header Host $up_host;\n");
        f30xLocationConf.append("        proxy_set_header X-Real-IP $remote_addr;\n" );
        f30xLocationConf.append("        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n");
        f30xLocationConf.append("        proxy_set_header X-Forwarded-Proto $thescheme;\n" );
        if (1==vo.getCache_flag()){
            f30xLocationConf.append("        add_header cache-status $upstream_cache_status;\n")   ;
            f30xLocationConf.append("        proxy_cache cache;\n");
            f30xLocationConf.append(String.format("        proxy_cache_key \"%s\";\n",cacheKeyStr) );
        }
        if (1==vo.getFollow_args()){
            f30xLocationConf.append("        set $saved_redirect_location '$upstream_http_location?$args';\n");
        }else{
            f30xLocationConf.append("        set $saved_redirect_location '$upstream_http_location';\n");
        }

        f30xLocationConf.append("        proxy_pass $saved_redirect_location;\n");
        f30xLocationConf.append("    }\n");
        mv.getValuesMap().put("follow_30x_chunk_location",f30xLocationConf.toString());
    }

    private String getCacheLocationConf(NgxCacheConfVo vo,Integer cacheMode,TbSiteEntity siteEntity,String proxyChunk,String cacheKeyStr,String rangeRes){
        //cacheMode 0=un Cache  1=cache
        StringBuilder sb=new StringBuilder();
        String locationStr="";
        switch (vo.getType()){
            case 1:
                //文件后缀
                locationStr=(String.format("location   ~* \\.(%s)$ {\n",vo.getContent() ));
                break;
            case 2:
                //精准匹配
                locationStr=(String.format("location  =  %s  {\n",vo.getContent()  ));
                break;
            case 3:
                //模糊匹配
                locationStr=(String.format("location ~*  %s  {\n",vo.getContent() ));
                break;
            case 4:
                locationStr=String.format("location ~*  %s  {\n",vo.getContent() );
                //包含
                break;
            case 5:
                // 所有  ~* /
                locationStr="location ~*  / {\n";
            default:
                break;
        }
        if (StringUtils.isBlank(locationStr)){
            return "";
        }
        sb.append("\n");
        sb.append("    "+locationStr);


        if (vo.getMode().equals("cache")){
            StringBuilder ngxCacheSb=new StringBuilder();
            ngxCacheSb.append("\n"+
                    "        add_header \"X-Request-Id\"  \"$request_id\";\n" +
                    "        proxy_set_header \"X-Request-Id\"  \"$request_id\";\n" +
                    "        proxy_set_header X-Real-IP $remote_addr;\n" +
                    "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
                    "        proxy_set_header X-Forwarded-Proto $thescheme;\n");
            ngxCacheSb.append(getProxyBindRemoteConfig());
            if (StringUtils.isNotBlank(rangeRes)){
                ngxCacheSb.append(rangeRes);
            }
            if (0==cacheMode){
                ngxCacheSb.append("        proxy_cache off;\n");
            }else{
                ngxCacheSb.append("        proxy_buffering on;\n");
                ngxCacheSb.append(String.format("        proxy_cache_key \"%s\";\n",cacheKeyStr));
                ngxCacheSb.append("        proxy_ignore_headers X-Accel-Expires Expires Cache-Control Set-Cookie;\n");
                ngxCacheSb.append("        add_header cache-status $upstream_cache_status;\n")   ;
                ngxCacheSb.append("        proxy_cache cache;\n");
                ngxCacheSb.append(String.format("        expires %s%s;\n",vo.getTime().toString(),vo.getUnit()));
                ngxCacheSb.append(String.format("        proxy_cache_valid 200 206 %s%s ;\n",vo.getTime().toString(),vo.getUnit()));
            }
            ngxCacheSb.append("        proxy_set_header Host $up_host;\n" );
            ngxCacheSb.append(proxyChunk);
            sb.append(ngxCacheSb);
            sb.append("    }\n");
        }else if (vo.getMode().equals("gho")){
            StringBuilder ngxGhoSb=new StringBuilder();
            String tTempPath="/data/gho/"+siteEntity.getMainServerName()+"/temp/";
            ngxGhoSb.append("\n"+
                    "        add_header \"X-Request-Id\"  \"$request_id\";\n" +
                    "        proxy_set_header \"X-Request-Id\"  \"$request_id\";\n" +
                    "        proxy_set_header X-Real-IP $remote_addr;\n" +
                    "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
                    "        proxy_set_header X-Forwarded-Proto $thescheme;\n");
            ngxGhoSb.append(getProxyBindRemoteConfig());
            ngxGhoSb.append(String.format("        expires %s%s;\n",vo.getTime().toString(),vo.getUnit()));
            ngxGhoSb.append(""+
                    "        proxy_buffering on;\n"+
                    "        proxy_cache off;\n"+
                    "        aio threads;\n" +
                    "        directio 4m;\n" +
                    "        proxy_set_header Accept-Encoding '';\n" +
                    "        root /data/gho/"+siteEntity.getMainServerName()+"/;\n" +
                    "        proxy_store on;\n" +
                    "        proxy_set_header Host $up_host;\n" +
                    "        add_header Access-Control-Allow-Origin \"$http_origin\";\n" +
                    "        proxy_store_access user:rw group:rw all:rw;\n" +
                    "        proxy_temp_path /data/gho/"+siteEntity.getMainServerName()+"/temp/;\n");
            ngxGhoSb.append(proxyChunk);
            sb.append(ngxGhoSb);
            sb.append("    }\n");
            //创建目录
            this.pushMutCmds("mkdir -p "+tTempPath);
        }
        return  sb.toString();
    }


    private String getCacheKey(Integer siteId){
        //"'$dev_type'###custom_cache_key_prefix###'###cache_url_param###''###slice_range_flag###'"
        //    set $cache_key "'$dev_type''$site_id''$request_uri''$slice_range'";
        //    set $cache_key "'$dev_type''$site_id''$request_uri'''";
        String custom_cache_key_prefix=SiteAttrEnum.PERFORMANCE_CUSTOM_CACHE_KEY_PREFIX.getDefaultValue();
        String cache_url_param=  SiteAttrEnum.PERFORMANCE_CACHE_IGNORE_URL_PARAM.getDefaultValue();
        String slice_range_flag="";
        String dev_type="$dev_type";
        final String[] keys={SiteAttrEnum.PERFORMANCE_CUSTOM_CACHE_KEY_PREFIX.getName(),SiteAttrEnum.PERFORMANCE_CACHE_IGNORE_URL_PARAM.getName(),SiteAttrEnum.SOURCE_RANGE.getName()};
        List<TbSiteAttrEntity> cList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id",siteId)
                .in("pkey",keys)
                .eq("status",1)
        );
        TbSiteAttrEntity cckp=null;
        TbSiteAttrEntity cup=null;
        TbSiteAttrEntity srf=null;
        for (TbSiteAttrEntity attr:cList){
            if (attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_CUSTOM_CACHE_KEY_PREFIX.getName())){
                cckp=attr;
            }else if (attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_CACHE_IGNORE_URL_PARAM.getName())){
                cup=attr;
            }else if (attr.getPkey().equals(SiteAttrEnum.SOURCE_RANGE.getName())){
                srf=attr;
            }
        }
        if (null!=cckp && null!=cckp.getPvalue()){
            custom_cache_key_prefix=cckp.getPvalue();
            custom_cache_key_prefix=custom_cache_key_prefix.replace("$site_id",String.valueOf(siteId));

        }
        if(null!=cup && null!=cup.getPvalue() && cup.getPvalue().equals("1")){
            cache_url_param="$uri";
        }
        if (null!=srf && null!=srf.getPvalue() && "1".equals(srf.getPvalue())){
            slice_range_flag="$slice_range";
        }
        return String.format("'%s''%s''%s''%s'",dev_type,custom_cache_key_prefix,cache_url_param,slice_range_flag);

    }

    /**
     * gzip 模块
     */
    private String getSiteGzipChunk( Map<String,String> singleMap){

        if (!singleMap.containsKey("gzip") || !"1".equals(singleMap.get("gzip"))) {
            return "";
        }
        StringBuilder gzipBf = new StringBuilder();
        gzipBf.append("gzip on;\n    ");
        gzipBf.append("gzip_buffers 32 4K;\n    ");
        gzipBf.append("gzip_http_version 1.1;\n    ");
        gzipBf.append("gzip_disable \"MSIE [1-6]\\.\";\n    ");
        if (StringUtils.isNotBlank(singleMap.get("gzip_min_length"))){
            gzipBf.append("gzip_min_length " + singleMap.get("gzip_min_length") + ";\n    ");
        }
        if (StringUtils.isNotBlank(singleMap.get("gzip_comp_level"))){
            gzipBf.append("gzip_comp_level " + singleMap.get("gzip_comp_level") + ";\n    ");
        }
        if ("1".equals(singleMap.get("gzip_vary"))) {
            gzipBf.append("gzip_vary on;\n    ");
        }
        if (StringUtils.isNotBlank(singleMap.get(SiteAttrEnum.PERFORMANCE_GZIP_TYPES.getName()))){
            gzipBf.append("gzip_types "+singleMap.get(SiteAttrEnum.PERFORMANCE_GZIP_TYPES.getName()) +";\n    ");
        }
        return gzipBf.toString();
    }






    private String getProxyPassOtherConf(Integer siteId,MakeFileParamVo mv){
        StringBuilder ops=new StringBuilder();
        ops.append("\n");
        TbSiteAttrEntity rangeAttr= tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.CONTENT_SECURITY_OPTIONS_RETURN_200.getName())
                .eq("status",1)
                .last("limit 1")
        );
        if (null!=rangeAttr && null!=rangeAttr.getPvalue()){
            if ("1".equals(rangeAttr.getPvalue())){
                ops.append("        if ( $request_method = OPTIONS ){\n");
                ops.append("            add_header A1low \"GET,POST,OPTIONS\";\n");
                ops.append("            add_header Access-Control-Allow-Headers \"origin,X-Requested-With, Content-Type, Accept\";\n");
                ops.append("            add_header Access-Control-Allow-Origin \"$http_origin\";\n");
                ops.append("            return 200;\n");
                ops.append("        }\n");

            }
        }
        if (true){
            ops.append("        if ( $site_suit_exp = '1' ){\n");
            //ops.append("           proxy_set_header Host \"127.0.0.1\"; \n");
            ops.append("           set $site_suit_exp_status '1';\n");
            ops.append("           set $out_html_url   \"http://127.0.0.1/site_suit_exp.html\";\n");
            ops.append("           proxy_pass  $out_html_url;\n");
            ops.append("        }\n");
        }
        boolean proxy_cache_lock_flag=true;
        if (true) {
            //mv.getValuesMap().containsKey(SiteAttrEnum.SOURCE_PROXY_CACHE_LOCK)
            TbSiteAttrEntity cv=tbSiteAttrDao.selectOne(
                    new QueryWrapper<TbSiteAttrEntity>() .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SOURCE_PROXY_CACHE_LOCK.getName())
            );
            if (null!=cv && null!=cv.getPvalue()){
                if ("0".equals(cv.getPvalue())){
                    proxy_cache_lock_flag=false;
                }
            }
        }
        if (true){
            //proxy_next_upstream
            TbSiteMutAttrEntity f301=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey",SiteAttrEnum.FOLLOW_30X.getName())
                    .eq("status",1)
                    .last("limit 1"));
            if (null==f301) {
                //f301 为NULL( 关闭)才可以开50x
                final String[] inKeys={SiteAttrEnum.NETWORK_NEXT_UPSTREAM.getName(),SiteAttrEnum.NETWORK_NEXT_UPSTREAM_TRIES.getName()};
                List<TbSiteAttrEntity> nAttrList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                        .eq("site_id",siteId)
                        .in("pkey",inKeys)
                        .eq("status",1)
                );
                Map<String,String> attrMap=new HashMap<>();
                for (TbSiteAttrEntity attr : nAttrList){
                    attrMap.put(attr.getPkey(),attr.getPvalue());
                }
                if (attrMap.containsKey("proxy_next_upstream") && attrMap.containsKey("proxy_next_upstream_tries")  ){
                    //proxy_next_upstream :"error timeout  http_503 http_504 http_500 http_502 non_idempotent"
                    String[] http_e_codes={"http_500","http_502","http_503","http_504"};
                    Set<String> hecs=new HashSet<>();
                    hecs.add("http_504");
                    for (String hec : http_e_codes){
                       if (attrMap.get("proxy_next_upstream").contains(hec)){
                           hecs.add(hec);
                       }
                    }
                    if (proxy_cache_lock_flag){
                            ops.append("        proxy_cache_lock on;\n");
                            ops.append("        proxy_cache_lock_age 10s;\n");
                    }
                    ops.append(String.format("        proxy_cache_use_stale error timeout updating %s ;\n",String.join(" ",hecs)));
                    ops.append(String.format("        proxy_next_upstream  error timeout %s non_idempotent;\n",String.join(" ",hecs)));
                    ops.append(String.format("        proxy_next_upstream_tries %s;\n",attrMap.get("proxy_next_upstream_tries")));
                }else{
                    //默认开启
                    //error timeout  http_503 http_504 non_idempotent
                    if (proxy_cache_lock_flag){
                        ops.append("        proxy_cache_lock on;\n");
                        ops.append("        proxy_cache_lock_age 10s;\n");
                    }
                    ops.append("        proxy_cache_use_stale error timeout updating http_503 http_504;\n");
                    ops.append(String.format("        proxy_next_upstream %s;\n",SiteAttrEnum.NETWORK_NEXT_UPSTREAM.getDefaultValue()));
                    ops.append(String.format("        proxy_next_upstream_tries %s;\n",SiteAttrEnum.NETWORK_NEXT_UPSTREAM_TRIES.getDefaultValue()));
                }
            }
        }
        return  ops.toString();
    }

    /**
     * Filter
     * @param siteId
     * @return
     */
    private String getSiteSubFilter(Integer siteId){
        try{
            String[] fKeys={SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_STATUS.getName(),SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_TYPES.getName(),SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_ONCE.getName(),SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER.getName()};
            List<TbSiteAttrEntity>  attrList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("site_id",siteId)
                    .in("pkey",fKeys)
                    .eq("status",1)
            );
            if(attrList.isEmpty()){
              return "";
            }
            NgxSubFilterVo vo=new NgxSubFilterVo();
            vo.setFilterContent(new ArrayList<>(attrList.size()));
            for (TbSiteAttrEntity attr:attrList){
                 switch (attr.getPkey()){
                     case "sub_filter_status":
                         vo.setStatus(Integer.parseInt(attr.getPvalue()));
                         break;
                     case "sub_filter_once":
                         vo.setOnceFlag(Integer.parseInt(attr.getPvalue()));
                        break;
                     case "sub_filter_types":
                         vo.setFileTypeS(attr.getPvalue());
                         break;
                     case "sub_filter":
                         vo.getFilterContent().add(attr.getPvalue());
                         break;
                     default:
                         break;
                 }
            }
            if (1!=vo.getStatus()){
                return "";
            }
            if (vo.getFilterContent().isEmpty()){
                return "";
            }
            StringBuilder sb=new StringBuilder();
            String subFilterOnce=1==vo.getOnceFlag()?"off":"on";
            sb.append("\n");
            sb.append("    sub_filter_types "+vo.getFileTypeS()+";\n");
            sb.append("    sub_filter_once "+subFilterOnce+";\n");
            // filter
            for (String fStr:vo.getFilterContent()){
                String[] ab=fStr.split("----");
                if(ab.length>=2){
                    if(!ab[0].contains("'")  && !ab[1].contains("'")){
                        sb.append(String.format("    sub_filter  '%s' '%s' ;\n",ab[0],ab[1]));
                    }
                }
            }
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }







    private R saveFilePathConf2Local(int configId, String ip, String groupId, String fn,String fc, String fileContent){
        String eMsg="";
        try{

            PushSetEnum item= PushSetEnum.getItemById(configId);
            if (null==item){
                logger.error(configId+" to set fail!");
                return R.error(configId+" to set fail!");
            }
            String parentDir=item.getLocalParentDirectory();
            String fileName=item.getFileName();
            if (StringUtils.isNotBlank(ip)){
                parentDir=parentDir.replace("{ip}",ip);
                fileName=fileName.replace("{ip}",ip);
            }
            if (StringUtils.isNotBlank(groupId)){
                parentDir=parentDir.replace("{gid}",groupId);
                fileName=fileName.replace("{gid}",groupId);
            }
            if (StringUtils.isNotBlank(fn)){
                parentDir=parentDir.replace("{fn}",fn);
                fileName=fileName.replace("{fn}",fn);
            }
            if (StringUtils.isNotBlank(fc)){
                parentDir=parentDir.replace("{fc}",fc);
                fileName=fileName.replace("{fc}",fc);
            }
            String locPath=parentDir+fileName;
            //logger.info("path:"+locPath);
            R r= FileUtils.fileWrite(locPath,fileContent);
            if (1!=r.getCode()){
               logger.info(r.toJsonString());
            }
            return r;
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public String getGroupIdById(String siteId,String streamId,String rewriteId){
        String groupId="0";
        String serNum="";
        if (StringUtils.isNotBlank(siteId) ){
             if (StaticVariableUtils.siteIdSerNumMap.contains(siteId)){
                 serNum=StaticVariableUtils.siteIdSerNumMap.get(siteId);
             }else{
                 TbSiteEntity siteEntity=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("status",1).eq("id",siteId).select("serial_number"));
                 if (null!=siteEntity){
                     serNum=siteEntity.getSerialNumber();
                 }
             }
        }else if (StringUtils.isNotBlank(streamId) ){
            if (StaticVariableUtils.streamIdSerNumMap.contains(streamId)){
                serNum=StaticVariableUtils.streamIdSerNumMap.get(streamId);
            }else{
                TbStreamProxyEntity streamProxyEntity=tbStreamProxyDao.selectOne(new QueryWrapper<TbStreamProxyEntity>().eq("status",1).eq("id",streamId).select("serial_number"));
                if (null!=streamProxyEntity){
                    serNum=streamProxyEntity.getSerialNumber();
                }
            }
        }else if (StringUtils.isNotBlank(rewriteId) ){
            if (StaticVariableUtils.rewriteIdSerNumMap.contains(rewriteId)){
                serNum=StaticVariableUtils.rewriteIdSerNumMap.get(rewriteId);
            }else{
                TbRewriteEntity rewriteEntity=tbRewriteDao.selectOne(new QueryWrapper<TbRewriteEntity>().eq("status",1).eq("id",streamId).select("serial_number"));
                if (null!=rewriteEntity){
                    serNum=rewriteEntity.getSerialNumber();
                }
            }
        }
        if (StringUtils.isBlank(serNum)){
            return groupId;
        }
        if (StaticVariableUtils.serialNumberGroupsMap.contains(serNum)){
            groupId=StaticVariableUtils.serialNumberGroupsMap.get(serNum);
        }else{
            groupId=this.getNodeAreaGroupIdBySerialNumber(serNum);
            StaticVariableUtils.serialNumberGroupsMap.put(serNum,groupId);
        }
        return groupId;
    }





    /**
     * 多线程推CMD
     * @param cmds
     */
    private void pushMutCmds(String cmds){
        redisUtils.streamXAdd("public"+RedisStreamType.MULTI_PURGE_COMMAND.getName(), "cmd",cmds);
    }


    private String getSuitLastEndTimeTemp(String sn){
       try{
            Integer[] sInList={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
            Integer[] sInStatusList={CdnSuitStatusEnum.NORMAL.getId()};
            CdnSuitEntity endSuit=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                    .eq("serial_number",sn)
                    .in("suit_type",sInList)
                    .in("status", sInStatusList)
                    .select("end_time")
                    .orderByDesc("end_time")
                    .last("limit 1")
            );
            if(null!=endSuit){
                //延迟12小时关停
               double etm= (endSuit.getEndTime().getTime()/1000.0)+12*3600*1000;
               //logger.info("etm:"+endSuit.getEndTime().getTime()+"----"+etm+"----"+Math.round(etm));
               return String.valueOf(Math.round(etm));
            }
        }catch (Exception e){

        }
        return "1";
    }

    /**
     * 根据serialNumber获取GROUPiD
     * @param serialNumber
     * @return
     */
    private String getNodeAreaGroupIdBySerialNumber(String  serialNumber){
        if (StringUtils.isBlank(serialNumber)){
            return "0";
        }
        Date now=new Date();
        final Integer[] suitTypeLs={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        CdnSuitEntity suitEntity=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number",serialNumber)
                .in("suit_type", suitTypeLs)
                .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                .orderByDesc("id")
                .le("start_time",now)
                .ge("end_time",now)
                .select("pay_serial_number")
                .last("limit 1"));
        if(null==suitEntity){
            //logger.error("[GetClientListBySite] suit ["+serialNumber+"] is null");
            return "0";
        }
        TbOrderEntity order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq("serial_number",suitEntity.getPaySerialNumber())
                .select("init_json,target_id")
                .last("limit 1"));
        if(null==order){
            //logger.error("[GetClientListBySite]order is null");
            return  "0";
        }
        JSONObject  initJson= DataTypeConversionUtil.string2Json(order.getInitJson());
        CdnProductEntity product=null;
        if (initJson.containsKey("product_obj")){
            try{
                product=DataTypeConversionUtil.json2entity(initJson.getJSONObject("product_obj"), CdnProductEntity.class);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(null==product) {
            product=cdnProductDao.selectById(order.getTargetId());
        }
        if (null==product){
            //logger.error("[GetClientListBySite]product is null!");
            return "0";
        }
        String serverGroupIds=product.getServerGroupIds();
        if(StringUtils.isBlank(serverGroupIds)){
            //logger.error("[GetClientListBySite]serverGroupIds is null");
            return "0";
        }
        //可以多个分组，目前业务取第0个分组ID
        String[] gids=serverGroupIds.split(",");
        String gid=gids[0];
        CdnClientGroupEntity groupEntity=cdnClientGroupDao.selectById(gid);
        if (null==groupEntity){
            return "0";
        }
        if (null==groupEntity.getAreaId()){
            return "0";
        }
        return  groupEntity.getAreaId().toString();
    }


}
