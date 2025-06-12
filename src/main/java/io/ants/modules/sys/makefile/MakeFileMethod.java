package io.ants.modules.sys.makefile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.TbSiteAttrDao;
import io.ants.modules.app.dao.TbSiteDao;
import io.ants.modules.app.dao.TbSiteMutAttrDao;
import io.ants.modules.app.dao.TbStreamProxyDao;
import io.ants.modules.app.entity.*;
import io.ants.modules.app.vo.StreamInfoVo;
import io.ants.modules.sys.dao.CdnClientDao;
import io.ants.modules.sys.dao.CdnIpControlDao;
import io.ants.modules.sys.dao.CdnSuitDao;
import io.ants.modules.sys.dao.TbCdnPublicMutAttrDao;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.entity.CdnIpControlEntity;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.vo.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Service
public class MakeFileMethod {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Pattern DOMAIN_F_PATTERN= Pattern.compile("^((?!-)[*A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$");

    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbCdnPublicMutAttrDao publicMutAttrDao;
    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private CdnIpControlDao cdnIpControlDao;
    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private CdnSuitDao cdnSuitDao;



    /**
     * 反射 站点id
     */
    private String Get_site_id(String parameter){
        //{"condition":"status=1","row":{"main_server_name":"text.com","id":1},"fields":"id,main_server_name","table":"tb_site"}
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    return  "site_"+jsonRow.getString("id");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 反射 站点属性
     * @param parameter
     */
    private String Get_site_attr(String parameter){
        return "";
    }

    private String Get_site_attr_int_value(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String variableName=jsonObject.getString("variableName");
                    String pKey=variableName.replace("###site_","");
                    //System.out.println(pKey);
                    pKey=pKey.replace("###","");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            return  attr.getPvalue();
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "0";
    }

    private String Get_site_attr_bool_value(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String variableName=jsonObject.getString("variableName");
                    String pKey=variableName.replace("###site_","");
                    //System.out.println(pKey);
                    pKey=pKey.replace("###","");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                return  "on";
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "off";
    }


    private String Get_site_attr_nameAndValue(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String variableName=jsonObject.getString("variableName");
                    String pKey=variableName.replace("###site_","");
                    //System.out.println(pKey);
                    pKey=pKey.replace("###","");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            return  pKey+" "+attr.getPvalue()+";";
                        }
                    }
                    SiteAttrEnum enumObj=SiteAttrEnum.getObjByName(pKey);
                    if (null!=enumObj){
                        if(StringUtils.isNotBlank(enumObj.getDefaultValue())){
                            return  String.format("%s  %s;",pKey,enumObj.getDefaultValue());
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_master_ip(String parameter){
        if(StringUtils.isNotBlank(StaticVariableUtils.authMasterIp) ){
            return  StaticVariableUtils.authMasterIp;
        }
        return  "127.0.0.1";
    }

    private String Get_nft_white_list(String parameter){
        List<String> nftWhiteList=new ArrayList<>();
        String mIp= QuerySysAuth.getSignAuthIp();
        if (!nftWhiteList.contains(mIp)){
            nftWhiteList.add(mIp);
        }
        if (!nftWhiteList.contains("127.0.0.1")){
            nftWhiteList.add("127.0.0.1");
        }
        if(StringUtils.isNotBlank(StaticVariableUtils.authMasterIp) ){
            if (!nftWhiteList.contains( StaticVariableUtils.authMasterIp)){
                nftWhiteList.add(StaticVariableUtils.authMasterIp);
            }
        }
        return String.join(",",nftWhiteList);
    }

    /**
     */
    private String Get_site_keepalive(Integer siteId){
        TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.SOURCE_KEEP_LIVE.getName())
                .eq("status",1)
                .last("limit 1"));
        if(null!=attr){
            String value=attr.getPvalue();
            if(StringUtils.isNotBlank(value)){
                return "    keepalive "+value+";\n";
            }
        } else if (StringUtils.isNotBlank(SiteAttrEnum.SOURCE_KEEP_LIVE.getDefaultValue())){
            return "    keepalive "+SiteAttrEnum.SOURCE_KEEP_LIVE.getDefaultValue()+";\n";
        }

        return "";
    }

    /**
     * @param siteId
     */
    private String Get_site_keepalive_timeout(Integer siteId){
        TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.SOURCE_KEEP_LIVE_TIMEOUT.getName())
                .eq("status",1)
                .last("limit 1"));
        if(null!=attr){
            String value=attr.getPvalue();
            if(StringUtils.isNotBlank(value)){
                return "    keepalive_timeout "+value+";\n";
            }
        }
        else if (StringUtils.isNotBlank(SiteAttrEnum.SOURCE_KEEP_LIVE_TIMEOUT.getDefaultValue())){
            return "    keepalive_timeout "+SiteAttrEnum.SOURCE_KEEP_LIVE_TIMEOUT.getDefaultValue()+";\n";
        }
        return "";
    }

    public List<CdnClientEntity> getClientList(){
        Date now=new Date();
        return cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
                .eq("status",1)
                .isNotNull("client_ip")
                .isNotNull("reg_info")
                .le("effective_start_time",now)
                .ge("effective_ending_time",now)
        );
    }

    //获取线路优化配置
    private String Get_site_route_optimization(Integer siteId,String port,String outClientIp,Integer mode){
        // TODO: 2022/10/6 功能暂停作用
        try{
            TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey",SiteAttrEnum.NETWORK_ROUTE_OPTIMIZATION.getName())
                    .eq("status",1)
                    .last("limit 1"));
            if(false && null!=attr){
                String value=attr.getPvalue();
                if(StringUtils.isNotBlank(value) && "1".equals(value)){
                    //站点-》套餐-》分组-》clientList
                    TbSiteEntity site=tbSiteDao.selectById(siteId);
                    if(null==site){   return "";      }
                    if(StringUtils.isBlank(site.getSerialNumber())){    return "";     }
                    //List<CdnClientEntity> client_list=this.getClientListBySerialNumber(site.getSerialNumber());
                    List<CdnClientEntity> client_list=this.getClientList();
                    if(0==client_list.size()){return  "";}
                    StringBuilder sb=new StringBuilder();
                    int maxSum=5;
                    for (CdnClientEntity client:client_list){
                        if(maxSum<0){
                            break;
                        }
                        if(!client.getClientIp().equals(outClientIp)){
                            if (1==mode){
                                sb.append(String.format("    server %s:%s weight=1 backup ;\n",client.getClientIp(),port));
                            }else{
                                sb.append(String.format("    server %s:%s ;\n",client.getClientIp(),port));
                            }
                            maxSum--;
                        }
                    }
                    return sb.toString();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }


    private String Get_site_proxy_next_upstream(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if( jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    String[] keys={SiteAttrEnum.NETWORK_NEXT_UPSTREAM.getName(),SiteAttrEnum.NETWORK_NEXT_UPSTREAM_TRIES.getName()};
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .in("pkey",keys)
                            .eq("status",1)
                            .last("limit 1"));
                    if( null!=attr){
                        //proxy_next_upstream
                        TbSiteAttrEntity n_attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                .eq("site_id",siteId)
                                .eq("pkey",SiteAttrEnum.NETWORK_NEXT_UPSTREAM.getName())
                                .eq("status",1)
                                .last("limit 1"));
                        TbSiteAttrEntity t_attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                .eq("site_id",siteId)
                                .eq("pkey",SiteAttrEnum.NETWORK_NEXT_UPSTREAM_TRIES.getName())
                                .eq("status",1)
                                .last("limit 1"));
                        if(null!=n_attr){
                            if (StringUtils.isNotBlank(n_attr.getPvalue())){
                                sb.append("\n");
                                sb.append(String.format("    proxy_next_upstream %s;\n",n_attr.getPvalue()));
                                if (null!=t_attr && StringUtils.isNotBlank(t_attr.getPvalue())){
                                    sb.append(String.format("    proxy_next_upstream_tries %s;\n",t_attr.getPvalue()));
                                }else {
                                    sb.append(String.format("    proxy_next_upstream_tries %s;\n",SiteAttrEnum.NETWORK_NEXT_UPSTREAM_TRIES.getDefaultValue()));
                                }
                            }
                        }
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * ###site_upstream_chunk### ==> 获取UP STEAM 源站 配置块
     */
    private String Get_site_upstream(String parameter){
        //[polling]轮循
        //[hash]ip hash
        //[cookie]cookie 保持
        //[check]http监测
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(!jsonObject.containsKey("row")){
                return sb.toString();
            }
            JSONObject jsonRow=jsonObject.getJSONObject("row");
            String clientIp="";
            if (jsonObject.containsKey("client")){
                JSONObject clientObj=jsonObject.getJSONObject("client");
                if(clientObj.containsKey("clientIp")){
                    clientIp=clientObj.getString("clientIp");
                }
            }
            if(!jsonRow.containsKey("id")){
                return sb.toString();
            }
            Integer siteId=jsonRow.getInteger("id");
            List<TbSiteMutAttrEntity> list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                    .eq("status",1));
            for (TbSiteMutAttrEntity mutAttr:list){
                //{"protocol":"http","port":80,"s_protocol":"$scheme","upstream":"polling","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1},{"ip":"1.2.31.4","domain":"","port":"80","line":1,"weight":1}]}
                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"3.3.3.4","port":"8988","line":2,"weight":1}]}
                JSONObject object=JSONObject.parseObject(mutAttr.getPvalue());
                if(object.containsKey("protocol")&& object.containsKey("line")&& object.containsKey("port")  && object.containsKey("upstream") && object.containsKey("source_set")  && object.containsKey("s_protocol")){
                    //server+="upstream site_"+siteId+"_"+mutAttr.getId()+" {\n" ;
                    sb.append(String.format("upstream site_%d_%d ",siteId,mutAttr.getId()));
                    sb.append("{\n");
                    String protocol=object.getString("protocol");
                    String upstreamMode=object.getString("upstream");
                    String source_set=object.getString("source_set");
                    String s_protocol=object.getString("s_protocol");
                    JSONArray lineArray=object.getJSONArray("line")  ;
                    if (0==lineArray.size()) {
                        logger.error(" lineArray size is zero!");
                        continue;
                    }
                    switch (upstreamMode){
                        case "polling":
                            if (true){
                                for (int i = 0; i <lineArray.size() ; i++) {
                                    JSONObject line_i=lineArray.getJSONObject(i);
                                    if("ip".equals(source_set)){
                                        if(line_i.containsKey("ip") && line_i.containsKey("port") && line_i.containsKey("line") && line_i.containsKey("weight")){
                                            String exist_backup=line_i.getInteger("line")==2?" backup":" ";
                                            String ip=line_i.getString("ip");
                                            String ip_format;
                                            if(IPUtils.isValidIPV4(ip)){
                                                ip_format=ip;
                                            }else if(IPUtils.isValidIPV6(ip)){
                                                ip_format="["+ip+"]";
                                            }else{
                                                logger.error("["+ip+"] is error ip format");
                                                continue;
                                            }
                                            sb.append(String.format("    server %s:%s weight=%s %s ;\n",ip_format,line_i.getString("port"),line_i.getInteger("weight"),exist_backup));
                                        }
                                    }else if("domain".equals(source_set)){
                                        if(line_i.containsKey("domain") && line_i.containsKey("port") && line_i.containsKey("line") ){
                                            String exist_backup=line_i.getInteger("line")==2?" backup":" ";
                                            sb.append(String.format("    server %s:%s %s ;\n",line_i.getString("domain"),line_i.getString("port"),exist_backup));
                                        }else {
                                            logger.info("--todo--");
                                        }
                                    } else {
                                        logger.error("upstream "+mutAttr.getId()+"param is error");
                                    }
                                }
                                sb.append(this.Get_site_route_optimization(siteId,object.getString("port"),clientIp,1));
                            }
                            break;
                        case "hash":
                            if (true){
                                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"hash","source_set":"domain",
                                // "line":[{"ip":"121.62.18.146","domain":"www.91hu.top","port":"80","line":1,"weight":1}]}
                                sb.append("    ip_hash;\n");
                                for (int i = 0; i <lineArray.size() ; i++) {
                                    JSONObject line_i=lineArray.getJSONObject(i);
                                    if("ip".equals(source_set)){
                                        if(line_i.containsKey("ip") && line_i.containsKey("port")  ){
                                            String ip=line_i.getString("ip");
                                            String ip_format;
                                            if(IPUtils.isValidIPV4(ip)){
                                                ip_format=ip;
                                            }else if(IPUtils.isValidIPV6(ip)){
                                                ip_format="["+ip+"]";
                                            }else{
                                                logger.error("["+ip+"] is error ip format");
                                                continue;
                                            }
                                            sb.append(String.format("    server %s:%s ;\n",ip_format,line_i.getString("port")));
                                        }
                                    }else if("domain".equals(source_set)){
                                        if(line_i.containsKey("domain") && line_i.containsKey("port") ){
                                            sb.append(String.format("    server %s:%s  ;\n",line_i.getString("domain"),line_i.getString("port")));
                                        }else{
                                            logger.error("[Get_site_upstream]->"+ line_i);
                                        }
                                    } else {
                                        logger.error("upstream "+mutAttr.getId()+"param is error");
                                    }

                                }
                                sb.append(this.Get_site_route_optimization(siteId,object.getString("port"),clientIp,0));
                                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"hash","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1}]}

                            }
                            break;
                        case "cookie":
                            if (true){
                                sb.append("    sticky name=ngx_cookie expires=6h;\n");
                                for (int i = 0; i <lineArray.size() ; i++) {
                                    JSONObject line_i=lineArray.getJSONObject(i);
                                    if("ip".equals(source_set)){
                                        if(line_i.containsKey("ip") && line_i.containsKey("port")  ){
                                            String ip=line_i.getString("ip");
                                            String ip_format;
                                            if(IPUtils.isValidIPV4(ip)){
                                                ip_format=ip;
                                            }else if(IPUtils.isValidIPV6(ip)){
                                                ip_format="["+ip+"]";
                                            }else{
                                                logger.error("["+ip+"] is error ip format");
                                                continue;
                                            }
                                            sb.append(String.format("    server %s:%s ;\n",ip_format,line_i.getString("port")));
                                        }
                                    }else if("domain".equals(source_set)){
                                        if(line_i.containsKey("domain") && line_i.containsKey("port") ){
                                            sb.append(String.format("    server %s:%s  ;\n",line_i.getString("domain"),line_i.getString("port")));
                                        }
                                    } else {
                                        logger.error("upstream "+mutAttr.getId()+"param is error");
                                    }
                                }
                                sb.append(this.Get_site_route_optimization(siteId,object.getString("port"),clientIp,0));
                                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"cookie","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1}]}

                            }
                            break;
                        case "check":
                            if (true){
                                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"check","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1}]}
                                //ip line
                                for (int i = 0; i <lineArray.size() ; i++) {
                                    JSONObject line_i=lineArray.getJSONObject(i);
                                    if("ip".equals(source_set)){
                                        if(line_i.containsKey("ip") && line_i.containsKey("port")  ){
                                            String ip=line_i.getString("ip");
                                            String ip_format;
                                            if(IPUtils.isValidIPV4(ip)){
                                                ip_format=ip;
                                            }else if(IPUtils.isValidIPV6(ip)){
                                                ip_format="["+ip+"]";
                                            }else{
                                                logger.error("["+ip+"] is error ip format");
                                                continue;
                                            }
                                            sb.append(String.format("    server %s:%s ;\n",ip_format,line_i.getString("port")));
                                        }
                                    }else if("domain".equals(source_set)){
                                        if(line_i.containsKey("domain") && line_i.containsKey("port") ){
                                            sb.append(String.format("    server %s:%s  ;\n",line_i.getString("domain"),line_i.getString("port")));
                                        }
                                    } else {
                                        logger.error("upstream id:"+mutAttr.getId()+" param is error");
                                    }
                                }
                                sb.append(this.Get_site_route_optimization(siteId,object.getString("port"),clientIp,0));
                                if(true){
                                    if ("http".equals(s_protocol)){
                                        // check interval=3000 rise=2 fall=2 timeout=1000 type=http;
                                        //    check_keepalive_requests 100;
                                        //    check_http_send "HEAD / HTTP/1.1\r\nConnection: keep-alive\r\n\r\n";
                                        //    check_http_expect_alive http_2xx http_3xx;
                                        sb.append("    check interval=3000 rise=2 fall=2 timeout=1000 type=http;\n");
                                        sb.append("    check_keepalive_requests 100;\n");
                                        sb.append("    check_http_send \"HEAD / HTTP/1.1\\r\\nConnection: keep-alive\\r\\n\\r\\n\";\n");
                                        sb.append("    check_http_expect_alive http_2xx http_3xx;\n");
                                    }else if("https".equals(s_protocol)){
                                        //check interval=3000 rise=2 fall=5 timeout=1000 type=ssl_hello;
                                        sb.append("    check interval=3000 rise=2 fall=5 timeout=1000 type=ssl_hello;\n");
                                    }else if("$scheme".equals(s_protocol)){
                                        //{"protocol":"http","port":80,"s_protocol":"$scheme","upstream":"cookie","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1}]}
                                        if("http".equals(protocol)){
                                            sb.append("    check interval=3000 rise=2 fall=2 timeout=1000 type=http;\n");
                                            sb.append("    check_keepalive_requests 100;\n");
                                            sb.append("    check_http_send \"HEAD / HTTP/1.1\\r\\nConnection: keep-alive\\r\\n\\r\\n\";\n");
                                            sb.append("    check_http_expect_alive http_2xx http_3xx;\n");
                                        }else if("https".equals(protocol)){
                                            sb.append("    check interval=3000 rise=2 fall=5 timeout=1000 type=ssl_hello;\n");
                                        }
                                    }else {
                                        logger.error("s_protocol unknown type ["+s_protocol+"]");
                                    }
                                }
                            }
                            break;
                        default:
                            logger.error("unknown upstream MOde type ["+upstreamMode+"]");
                            break;
                    }
                    sb.append(this.Get_site_keepalive(siteId));
                    sb.append(this.Get_site_keepalive_timeout(siteId));
                    sb.append("}");
                    sb.append("\n");
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }


    public List<Integer> getSitePorts(Integer siteId){
        List<Integer> posts=new ArrayList<>();
        try{
            List<TbSiteMutAttrEntity> list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                    .eq("status",1));
            for (TbSiteMutAttrEntity mutAttr:list){
                //{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"3.3.3.4","port":"8988","line":2,"weight":1}]}
                JSONObject object=JSONObject.parseObject(mutAttr.getPvalue());
                if(object.containsKey("port")) {
                    Integer p=object.getInteger("port");
                    if(!posts.contains(p)){
                        posts.add(p);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return posts;
    }



    /**
     * 生成listen
     */
    private String Get_site_listen(String parameter){
        StringBuilder listen=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    String http2ConfValue="";
                    //                    List<Integer> posts=this.GetSitePorts(siteId);
                    //                    for (Integer p:posts){
                    //                        listen+="listen "+p+";\n    ";
                    //                    }

                    List<TbSiteMutAttrEntity> list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                            .eq("status",1));
                    TbSiteAttrEntity http2Conf=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.SSL_HTTP2.getName())
                            .eq("status",1)
                            .last("limit 1")
                    );
                    if (null!=http2Conf && StringUtils.isNotBlank(http2Conf.getPvalue())){
                        if ("1".equals(http2Conf.getPvalue())){
                            //http2 2023-05-31 屏蔽http2
                            //http2ConfValue=" http2 ";
                        }
                    }
                    List<Integer> httpPortLs=new ArrayList<>();
                    List<Integer> httpsPortLs=new ArrayList<>();
                    for (TbSiteMutAttrEntity mutAttr:list){
                        //{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"3.3.3.4","port":"8988","line":2,"weight":1}]}
                        JSONObject object=JSONObject.parseObject(mutAttr.getPvalue());
                        if(object.containsKey("port") && object.containsKey("protocol")) {
                            Integer port=object.getInteger("port");
                            String protocol=object.getString("protocol");
                            if ("http".equals(protocol)){
                                if (!httpPortLs.contains(port)){
                                    httpPortLs.add(port);
                                }
                            }else if("https".equals(protocol)){
                                if (!httpsPortLs.contains(port)){
                                    httpsPortLs.add(port);
                                }
                            }else {
                                logger.error("protocol ["+protocol+"] is unknown ");
                            }
                        }
                    }
                    if (httpPortLs.size()>0 || httpsPortLs.size()>0){
                        listen.append("\n");
                    }
                    for (Integer httpPort: httpPortLs){
                        listen.append("    listen "+httpPort+" ;\n");
                        listen.append("    listen [::]:"+httpPort+" ;\n");
                    }
                    if (this.existSslCrtConf(siteId)){
                        for (Integer httpsPort: httpsPortLs){
                            listen.append("    listen "+httpsPort+" ssl "+http2ConfValue+" ;\n");
                            listen.append("    listen [::]:"+httpsPort+" ssl "+http2ConfValue+" ;\n");
                        }
                        if (StringUtils.isNotBlank(http2ConfValue)){
                            listen.append("    http2_max_concurrent_streams 2;\n");
                        }
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(StringUtils.isBlank(listen.toString())){
            listen.append("listen 80;\n    listen [::]:80;");
        }
        return listen+"\n";
    }


    /**
     * 生成 反射代理块
     * @param parameter
     * @return
     */
    private String Get_site_proxy_pass(String parameter){
        /*
        *  set $pp_flag $scheme_$server_port;
        if ( $pp_flag = 'https_443'  )  {
            proxy_pass  https://site_1_5;
        }*/
        StringBuilder sb=new StringBuilder();
        sb.append("set $pp_flag $scheme$server_port;\n");
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    List<TbSiteMutAttrEntity> list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                            .eq("status",1));
                    for (TbSiteMutAttrEntity mutAttr:list){
                        //{"protocol":"http","port":80,"s_protocol":"$scheme","upstream":"polling","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1},{"ip":"1.2.31.4","domain":"","port":"80","line":1,"weight":1}]}
                        //{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"3.3.3.4","port":"8988","line":2,"weight":1}]}
                        JSONObject object=JSONObject.parseObject(mutAttr.getPvalue());
                        if(object.containsKey("port") && object.containsKey("protocol") && object.containsKey("s_protocol")) {
                            Integer p=object.getInteger("port");
                            String  protocol=object.getString("protocol");
                            String  s_protocol=object.getString("s_protocol");
                            if("http".equals(s_protocol)){
                                sb.append(String.format("        if ( $pp_flag = '%s' ) {\n",protocol+p));
                                sb.append(String.format("            proxy_pass %s://site_%s_%d ;\n",s_protocol,siteId,mutAttr.getId()));
                                sb.append("        }\n");
                            }else if("https".equals(s_protocol)){
                                //sb.append(String.format("        proxy_ssl_name $host;\n"));
                                //sb.append(String.format("        proxy_ssl_server_name on;\n"));
                                sb.append(String.format("        if ( $pp_flag = '%s' ) {\n",protocol+p));
                                sb.append(String.format("            proxy_pass %s://site_%s_%d ;\n",s_protocol,siteId,mutAttr.getId()));
                                sb.append("        }\n");
                            }else if("$scheme".equals(s_protocol)){
                                //if ("https".equals(protocol)){
                                //sb.append(String.format("        proxy_ssl_name $host;\n"));
                                //sb.append(String.format("        proxy_ssl_server_name on;\n"));
                                //}
                                sb.append(String.format("        if ( $pp_flag = '%s' ) {\n",protocol+p));
                                sb.append(String.format("            proxy_pass %s://site_%s_%d ;\n",s_protocol,siteId,mutAttr.getId()));
                                sb.append("        }\n");
                            }else {
                                logger.error("unknown s_protocol:"+s_protocol);
                            }

                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb +"\n";
    }




    /**
     * 生成 反射代理块_add_e
     * @param parameter
     * @return
     */
    private String Get_site_proxy_pass_ae(String parameter){
        /*
        *  set $pp_flag $scheme_$server_port;
        if ( $pp_flag = 'https_443'  )  {
            proxy_pass  https://site_1_5;
        }*/
        StringBuilder sb=new StringBuilder();
        sb.append("set $pp_flag $p_pass$scheme$server_port;\n");
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    List<TbSiteMutAttrEntity> list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                            .eq("status",1));
                    for (TbSiteMutAttrEntity mutAttr:list){
                        //{"protocol":"http","port":80,"s_protocol":"$scheme","upstream":"polling","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"80","line":1,"weight":1},{"ip":"1.2.31.4","domain":"","port":"80","line":1,"weight":1}]}
                        //{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"3.3.3.4","port":"8988","line":2,"weight":1}]}
                        JSONObject object=JSONObject.parseObject(mutAttr.getPvalue());
                        if(object.containsKey("port") && object.containsKey("protocol") && object.containsKey("s_protocol")) {
                            Integer p=object.getInteger("port");
                            String  protocol=object.getString("protocol");
                            String  s_protocol=object.getString("s_protocol");
                            if("http".equals(s_protocol)){
                                sb.append(String.format("        if ( $pp_flag = '1%s' ) {\n",protocol+p));
                                sb.append(String.format("            proxy_pass %s://site_%s_%d ;\n",s_protocol,siteId,mutAttr.getId()));
                                sb.append("        }\n");
                            }else if("https".equals(s_protocol)){
                                //sb.append(String.format("        proxy_ssl_name $host;\n"));
                                //sb.append(String.format("        proxy_ssl_server_name on;\n"));
                                sb.append(String.format("        if ( $pp_flag = '1%s' ) {\n",protocol+p));
                                sb.append(String.format("            proxy_pass %s://site_%s_%d ;\n",s_protocol,siteId,mutAttr.getId()));
                                sb.append("        }\n");
                            }else if("$scheme".equals(s_protocol)){
                                //if ("https".equals(protocol)){
                                //sb.append(String.format("        proxy_ssl_name $host;\n"));
                                //sb.append(String.format("        proxy_ssl_server_name on;\n"));
                                //}
                                sb.append(String.format("        if ( $pp_flag = '1%s' ) {\n",protocol+p));
                                sb.append(String.format("            proxy_pass %s://site_%s_%d ;\n",s_protocol,siteId,mutAttr.getId()));
                                sb.append("        }\n");
                            }else {
                                logger.error("unknown s_protocol:"+s_protocol);
                            }

                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb +"\n";
    }


    private String Get_site_spider(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.ADVANCED_CONF_SPIDER.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if(null!=attr && StringUtils.isNotBlank(attr.getPvalue())){
                            return "";
                        }
                    }

                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_site_proxy_cache_key(String parameter){
        String defaultValue="\"'$dev_type''$host$uri'\"";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    // 4258~$uri~
                    defaultValue=siteId+"~$uri~";
                    String defaultUri="$request_uri";
                    String defaultSlice="";
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PERFORMANCE_CACHE_IGNORE_URL_PARAM.getName())
                            .eq("status",1)
                            .last("limit 1"));

                    if(null!=attr && StringUtils.isNotBlank(attr.getPvalue())){
                        if("1".equals(attr.getPvalue())){
                            defaultUri="$uri";
                        }
                    }
                    TbSiteAttrEntity attr_slice=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SOURCE_RANGE.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr_slice){
                        if (StringUtils.isNotBlank(attr_slice.getPvalue())){
                            if("1".equals(attr_slice.getPvalue())){
                                defaultSlice="$slice_range";
                            }
                        }
                    }
                    return "\"'$dev_type''"+siteId+"''"+defaultUri+"''"+defaultSlice+"'\"";
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //[proxy_cache_key $scheme://$host$uri~$slice_range];
        //return "$uri$is_args$args";
        return defaultValue;
    }

    /**
     * 分片缓存
     */
    private String Get_site_slice(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SOURCE_RANGE.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                sb.append("        slice             1m;\n");
                                sb.append("        proxy_set_header  Range $slice_range;\n");
                                sb.append(String.format("        proxy_cache_key   %s;\n",this.Get_site_proxy_cache_key(parameter)));
                                return sb.toString();
                            }
                        }
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sb.append(String.format("        proxy_cache_key %s ;\n" ,Get_site_proxy_cache_key(parameter)));
        return sb.toString();
    }

    private String getCacheConfPath(String parameter){
        JSONObject jsonObject=JSONObject.parseObject(parameter);
        //variableName
        jsonObject.put("variableName","###cache_proxy_cache_path_dir###");
        return Get_cache_conf(jsonObject.toJSONString());
    }

    /**
     * 生成缓存 块
     */
    private String Get_site_cache(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    String HostName="";
                    if(jsonRow.containsKey("main_server_name")){
                        HostName= jsonRow.getString("main_server_name");
                    }else if(jsonRow.containsKey("mainServerName")){
                        HostName= jsonRow.getString("mainServerName");
                    }
                    String proxy_pass=this.Get_site_proxy_pass(parameter);
                    List<TbSiteMutAttrEntity> list1=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.PERFORMANCE_UN_CACHE_TYPE.getName())
                            .eq("status",1));
                    for (TbSiteMutAttrEntity mutAttr:list1){
                        //{"type":1,"content":"jpg|jpeg|gif|ico|png|bmp|webp|psd|tif|tiff|svg|svgz","time":25}\
                        //{"type":2,"content":"/demo/index.js","time":26}
                        //{"type":3,"content":"/demo/","time":26}
                        //{"type":4,"content":"/dir","time":24}
                        //{"type":5,"content":"cookie1","time":25}

                        JSONObject UnCacheObj= DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                        if(UnCacheObj.containsKey("type") && UnCacheObj.containsKey("content") ){
                            Integer type=UnCacheObj.getInteger("type");
                            String content=UnCacheObj.getString("content");
                            if(type.equals(NginxCacheType.FILE_SUFFIX.getId())){
                                sb.append(String.format("location   ~* \\.(%s)$ {\n",content ));
                            }else  if(type.equals(NginxCacheType.PATH_EQUAL.getId())){
                                //cache+="location  "+content+" {\n";
                                sb.append(String.format("location   %s  {\n",content ));

                            }else  if(type.equals(NginxCacheType.PATH_LIKE.getId())){
                                //cache+="location ~* "+content+"  {\n";
                                sb.append(String.format("location ~*  %s  {\n",content ));
                            }else  if(type.equals(NginxCacheType.PATH_INCLUDE.getId())){
                                //cache+="location ~* "+content+"  {\n";
                                sb.append(String.format("location ~*  %s  {\n",content ));

                            }else  if(type.equals(NginxCacheType.VISITOR.getId())){
                                logger.error("VISITOR   type is not follow ");
                                continue;
                            }
                            sb.append("        proxy_cache off;\n");
                            sb.append("        proxy_set_header X-Real-IP $remote_addr;\n");
                            sb.append("        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n");
                            sb.append("        proxy_set_header X-Forwarded-Proto $thescheme;\n");
                            sb.append(String.format("        proxy_set_header Host %s ;\n",this.Get_site_source_host(parameter)));
                            sb.append(String.format("        %s",proxy_pass));
                            sb.append("    }\n    ");
                        }
                    }
                    List<TbSiteMutAttrEntity> list2=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.PERFORMANCE_CACHE_TYPE.getName())
                            .eq("status",1));
                    for (TbSiteMutAttrEntity mutAttr:list2){
                        JSONObject CacheObj=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                        if(CacheObj.containsKey("type") && CacheObj.containsKey("content") && CacheObj.containsKey("time")){
                            //"{\"type\":1,\"content\":\"html|htm|php|json\",\"time\":24,\"unit\":\"h\",\"mode\":\"gho\"}"
                            String mode="cache";
                            if (CacheObj.containsKey("mode")){
                                mode=CacheObj.getString("mode");
                            }
                            Integer type=CacheObj.getInteger("type");
                            String content=CacheObj.getString("content");
                            content.replace("\n","|");
                            String time=CacheObj.getString("time");
                            if(CacheObj.containsKey("unit")){
                                time=time+CacheObj.getString("unit");
                            }
                            if("cache".equals(mode)){
                                if(type.equals(NginxCacheType.FILE_SUFFIX.getId())){
                                    sb.append(String.format("location ~* \\.(%s)$ {\n",content));
                                }else  if(type.equals(NginxCacheType.PATH_EQUAL.getId())){
                                    sb.append(String.format("location   %s  {\n",content ));
                                }else  if(type.equals(NginxCacheType.PATH_LIKE.getId())){
                                    sb.append(String.format("location ~*  %s  {\n",content ));

                                }else  if(type.equals(NginxCacheType.PATH_INCLUDE.getId())){
                                    sb.append(String.format("location ~*  %s  {\n",content ));
                                }else  if(type.equals(NginxCacheType.VISITOR.getId())){
                                    logger.error("VISITOR   type is not follow ");
                                    continue;
                                }
                                //追加分片
                                String slice_chunk=this.Get_site_slice(parameter);
                                sb.append(slice_chunk);
                                //追加缓存配置 //loction块内 配置
                                StringBuilder cacheChunkSb=new StringBuilder();
                                cacheChunkSb.append("        proxy_ignore_headers X-Accel-Expires Expires Cache-Control Set-Cookie;\n");
                                cacheChunkSb.append("        add_header cache-status $upstream_cache_status;\n")   ;
                                cacheChunkSb.append("        proxy_cache cache;\n");
                                cacheChunkSb.append(String.format("        proxy_set_header Host  %s ;\n",this.Get_site_source_host(parameter)) );
                                cacheChunkSb.append(String.format("        proxy_cache_valid any %s ;\n",time));
                                cacheChunkSb.append(String.format("        %s ",proxy_pass));
                                sb.append(cacheChunkSb);
                                sb.append("    }\n    ");
                            }else if("gho".equals(mode)){

                                if(type.equals(NginxCacheType.FILE_SUFFIX.getId())){
                                    sb.append(String.format("location ~* \\.(%s)$ {\n",content));
                                }else  if(type.equals(NginxCacheType.PATH_EQUAL.getId())){
                                    sb.append(String.format("location   %s  {\n",content ));
                                }else  if(type.equals(NginxCacheType.PATH_LIKE.getId())){
                                    sb.append(String.format("location ~*  %s  {\n",content ));

                                }else  if(type.equals(NginxCacheType.PATH_INCLUDE.getId())){
                                    sb.append(String.format("location ~*  %s  {\n",content ));
                                }else  if(type.equals(NginxCacheType.VISITOR.getId())){
                                    logger.error("VISITOR   type is not follow ");
                                    continue;
                                }
                                //loction 块内配置
                                StringBuilder gho_chunk_content=new StringBuilder();

                                //set $p_pass 0;
                                //if ( !-e $request_filename) {
                                //  set $p_pass 1;
                                //}
                                //    if ( !-e $request_filename) {
                                //        proxy_pass https://www.freehao123.com;
                                //    }
                                String ghoPath=getCacheConfPath(parameter);
                                if (StringUtils.isBlank(ghoPath)){
                                    logger.error("获取Cache路径失败[1]");
                                    continue;
                                }
                               //logger.debug(ghoPath);
                                int l_index=ghoPath.lastIndexOf('/');
                                if (-1==l_index){
                                    logger.error("获取Cache路径失败[2]");
                                    continue;
                                }
                                ghoPath=ghoPath.substring(0,l_index);
                                String siteGhoRootPath=ghoPath+"/gho/"+HostName+"/";
                                String siteGhoTempPath=ghoPath+"/gho/"+HostName+"/temp/";
                                //                                String[] dirs={siteGhoRootPath,siteGhoTempPath};
                                //                                for (String dir:dirs){
                                //                                    createDirToNode(dir);
                                //                                }
                                gho_chunk_content.append("        expires ").append(time).append(";\n");
                                gho_chunk_content.append("        aio threads;\n");
                                gho_chunk_content.append("        directio 4m;\n");
                                gho_chunk_content.append("        proxy_set_header Accept-Encoding '';\n");
                                gho_chunk_content.append("        root ").append(siteGhoRootPath).append(";\n");
                                gho_chunk_content.append("        proxy_store on;\n");
                                gho_chunk_content.append("        proxy_set_header Host $host;\n");
                                gho_chunk_content.append("        add_header Access-Control-Allow-Origin \"$http_origin\";\n");
                                gho_chunk_content.append("        proxy_store_access user:rw group:rw all:rw;\n");
                                gho_chunk_content.append("        proxy_temp_path ").append(siteGhoTempPath).append(";\n");
                                gho_chunk_content.append("        set $p_pass 0;\n");
                                gho_chunk_content.append("        if ( !-e $request_filename) {\n");
                                gho_chunk_content.append("            set $p_pass 1;\n");
                                gho_chunk_content.append("        }\n");
                                gho_chunk_content.append(String.format("        %s ",this.Get_site_proxy_pass_ae(parameter)));
                                sb.append(gho_chunk_content);
                                sb.append("    }\n    ");
                            }else{
                                logger.error(mode+"unknown type");
                            }

                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sb.append("\n");
        return sb.toString();
    }


    private String getAddHeadByString(String str){
        if (StringUtils.isBlank(str)){
            return "";
        }
        NgxAddHeadVo vo=DataTypeConversionUtil.string2Entity(str,NgxAddHeadVo.class);
       //  sb.append(String.format("add_header %s \"%s\";\n    ",headjson.getString("header") ,headjson.getString("content")));
       return   "add_header \""+vo.getHeader()+"\"  \""+vo.getContent()+"\";\n";
    }

    private String getAddProxyHeadByString(String str){
        if (StringUtils.isBlank(str)){
            return "";
        }
        // sb.append(String.format("proxy_set_header  %s \"%s\";\n    ",headjson.getString("header"),headjson.getString("content")));
        NgxAddHeadVo vo=DataTypeConversionUtil.string2Entity(str,NgxAddHeadVo.class);
        return   "proxy_set_header \""+vo.getHeader()+"\"  \""+vo.getContent()+"\";\n";
    }

    /**
     * 自定义响应头
     */
    private String Get_site_add_head(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    List<TbSiteMutAttrEntity>  attrList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.ADVANCED_CONF_HTTP_RESPONSE_HEADER.getName())
                            .eq("status",1)
                    );
                    for (TbSiteMutAttrEntity mattr:attrList){
                        //{"type":"Cache-Control","header":"","content":"1111","info":"111"}
                        sb.append(this.getAddHeadByString(mattr.getPvalue()));
                    }

                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * http 自定义头
     * @param parameter
     * @return
     */
    private String Get_http_add_head(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            List<TbCdnPublicMutAttrEntity>  attrList=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey", PublicEnum.HTTP_RESPONSE_HEADER.getName())
                    .eq("status",1)
            );
            for (TbCdnPublicMutAttrEntity mAttr:attrList){
                //{"type":"Cache-Control","header":"","content":"1111","info":"111"}
                sb.append(this.getAddHeadByString(mAttr.getPvalue()));
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 回源设置HEAD
     */
    private String Get_site_proxy_set_header(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            List<TbSiteMutAttrEntity>  attrList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("pkey",SiteAttrEnum.ADVANCED_CONF_HTTP_REQUEST_HEADER.getName())
                    .eq("status",1)
            );
            for (TbSiteMutAttrEntity mAttr:attrList){
                //{"type":"Cache-Control","header":"","content":"1111","info":"111"}
                sb.append(this.getAddProxyHeadByString(mAttr.getPvalue()));
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 回源设置HEAD
     */
    private String Get_http_proxy_set_header(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    List<TbCdnPublicMutAttrEntity>  attrList=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",PublicEnum.HTTP_REQUEST_HEADER.getName())
                            .eq("status",1)
                    );
                    for (TbCdnPublicMutAttrEntity mAttr:attrList){
                        //{"type":"Cache-Control","header":"","content":"1111","info":"111"}
                        sb.append(this.getAddProxyHeadByString(mAttr.getPvalue()));
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sb.append("\n");
        return sb.toString();
    }


    /**
     * 反射 站点主域名
     */
    private String Get_site_main_server_name(String parameter){
        //{"condition":"status=1","row":{"main_server_name":"text.com","id":1},"fields":"id,main_server_name","table":"tb_site"}
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("main_server_name")){
                    return  jsonRow.getString("main_server_name");
                }else if(jsonRow.containsKey("mainServerName")){
                    return  jsonRow.getString("mainServerName");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 强制HTTPS
     */
    private String Get_site_forced_https(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    if (!this.existSslCrtConf(siteId)){
                        return sb.toString();
                    }
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SSL_FORCED_HTTPS.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                // return "if ( $scheme = 'http') { return 302 https://$host$request_uri; }\n";
                                sb.append("if ( $scheme = 'http' ) \n ");
                                sb.append("    {\n");
                                sb.append("        return 301 https://$host$request_uri; \n");
                                sb.append("    }\n");
                                //return "if ( $scheme = 'http') { return 301 https://$host$request_uri; }\n";
                                //return "if ( $scheme = 'http' ){ rewrite ^(/.*)$ https://$host$1 permanent ; }\n";
                            }else if("2".equals(attr.getPvalue())){
                                sb.append("default_type  'text/html';\n");
                                sb.append("    if ( $scheme = 'http' )\n");
                                sb.append("    {\n");
                                sb.append("        return 200 \"<html><script>location.replace('https://'+location.host+location.pathname+location.search)</script><html>\";\n") ;
                                sb.append("    }\n");
                            }else if("3".equals(attr.getPvalue())){
                                sb.append("default_type  'text/html';\n");
                                sb.append("    if ( $scheme = 'http' )\n");
                                sb.append("    {\n");
                                sb.append("        return 200 \"<html><script>var _0x5654=['console','log','debug','search','replace','https://','apply','warn','table','pathname','exception','trace','return\\x20(function()\\x20','{}.constructor(\\x22return\\x20this\\x22)(\\x20)','error'];(function(_0x148b05,_0x5654f0){var _0x3741b3=function(_0x396f38){while(--_0x396f38){_0x148b05['push'](_0x148b05['shift']());}};_0x3741b3(++_0x5654f0);}(_0x5654,0xba));var _0x3741=function(_0x148b05,_0x5654f0){_0x148b05=_0x148b05-0x0;var _0x3741b3=_0x5654[_0x148b05];return _0x3741b3;};var _0xc6f130=function(){var _0x193831=!![];return function(_0xe3bbb9,_0x1f670d){var _0xace33=_0x193831?function(){if(_0x1f670d){var _0x186c12=_0x1f670d[_0x3741('0x0')](_0xe3bbb9,arguments);_0x1f670d=null;return _0x186c12;}}:function(){};_0x193831=![];return _0xace33;};}();var _0x1af9e3=_0xc6f130(this,function(){var _0x18a2e7=function(){};var _0x4c4a1a=function(){var _0x4da621;try{_0x4da621=Function(_0x3741('0x6')+_0x3741('0x7')+');')();}catch(_0x1beecc){_0x4da621=window;}return _0x4da621;};var _0x1ed544=_0x4c4a1a();if(!_0x1ed544[_0x3741('0x9')]){_0x1ed544[_0x3741('0x9')]=function(_0xa08ce1){var _0x238492={};_0x238492[_0x3741('0xa')]=_0xa08ce1;_0x238492[_0x3741('0x1')]=_0xa08ce1;_0x238492[_0x3741('0xb')]=_0xa08ce1;_0x238492['info']=_0xa08ce1;_0x238492[_0x3741('0x8')]=_0xa08ce1;_0x238492[_0x3741('0x4')]=_0xa08ce1;_0x238492[_0x3741('0x2')]=_0xa08ce1;_0x238492[_0x3741('0x5')]=_0xa08ce1;return _0x238492;}(_0x18a2e7);}else{_0x1ed544[_0x3741('0x9')][_0x3741('0xa')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['warn']=_0x18a2e7;_0x1ed544[_0x3741('0x9')][_0x3741('0xb')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['info']=_0x18a2e7;_0x1ed544[_0x3741('0x9')][_0x3741('0x8')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['exception']=_0x18a2e7;_0x1ed544[_0x3741('0x9')][_0x3741('0x2')]=_0x18a2e7;_0x1ed544[_0x3741('0x9')]['trace']=_0x18a2e7;}});_0x1af9e3();location[_0x3741('0xd')](_0x3741('0xe')+location['host']+location[_0x3741('0x3')]+location[_0x3741('0xc')]);</script><html>\";\n") ;
                                sb.append("    }\n");
                            }
                        }
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return sb.toString();
    }

    private boolean checkSSlcrt(Integer siteId){
        TbSiteMutAttrEntity mutAttrKeyEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                .eq("status",1)
                .last("limit 1")
        );
        TbSiteMutAttrEntity mutAttrPemEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                .eq("status",1)
                .last("limit 1")
        );
        if(null!=mutAttrKeyEntity && null!=mutAttrPemEntity){
            return true;
        }
        return  false;
    }

    /**
     * SLL 模块
     */
    private String Get_site_ssl(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String id=jsonRow.getInteger("id").toString();
                    TbSiteMutAttrEntity mutAttrKeyEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",id)
                            .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                            .eq("status",1)
                            .last("limit 1")
                    );
                    TbSiteMutAttrEntity mutAttrPemEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",id)
                            .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                            .eq("status",1)
                            .last("limit 1")
                    );
                    if(null!=mutAttrKeyEntity && null!=mutAttrPemEntity){
                        String cert_name="ssl_"+id;
                        //                        ssl="\n"+
                        //                                "    ssl_certificate conf/ssl/"+cert_name+".crt;\n" +
                        //                                "    ssl_certificate_key  conf/ssl/"+cert_name+".key;\n" +
                        //                                "    ssl_protocols TLSv1.1 TLSv1.2 TLSv1.3;\n" +
                        //                                "    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;\n" +
                        //                                "    ssl_prefer_server_ciphers on;\n" +
                        //                                "    ssl_session_cache shared:SSL:10m;\n";
                        sb.append("\n");
                        sb.append(String.format("    ssl_certificate conf/ssl/%s.crt;\n",cert_name));
                        sb.append(String.format("    ssl_certificate_key  conf/ssl/%s.key;\n",cert_name));
                        String sslProtocols=Get_site_ssl_protocols(parameter);
                        if ("TLSv1 TLSv1.1 TLSv1.2".equals(sslProtocols)){
                            sb.append(String.format("    ssl_protocols %s;\n",sslProtocols));
                            sb.append("    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4:!DH:!DHE;\n");
                        }else if ("TLSv1.1 TLSv1.2 TLSv1.3".equals(sslProtocols)){
                            sb.append(String.format("    ssl_protocols %s;\n",sslProtocols));
                            sb.append("    ssl_ciphers EECDH+CHACHA20:EECDH+CHACHA20-draft:EECDH+AES128:RSA+AES128:EECDH+AES256:RSA+AES256:EECDH+3DES:RSA+3DES:!MD5;\n");
                        }else{
                            //default 1 1.1 1.2
                            sb.append(String.format("    ssl_protocols %s;\n",SiteAttrEnum.SSL_PROTOCOLS.getDefaultValue()));
                            sb.append("    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4:!DH:!DHE;\n");
                        }
                        sb.append("    ssl_prefer_server_ciphers on;\n");
                        sb.append("    ssl_session_cache shared:SSL:10m;\n");
                    }

                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }


    /**
     * 获取站点source host
     * @param parameter
     */
    private String Get_site_source_host(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SOURCE_HOST.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            return attr.getPvalue();
                        }
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return "$http_host ";
    }



    /**
     * gzip 模块
     */
    private String Get_site_gzip(String parameter){
        StringBuilder gzip= new StringBuilder();
        StringBuilder gzip_types= new StringBuilder("gzip_types ");
        int types_sum=0;
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    List<TbSiteAttrEntity> attrs=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .like("pkey","gzip")
                            .eq("status",1));
                    List<String> confs=new ArrayList<>();
                    for (TbSiteAttrEntity attr:attrs){
                        if(attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_GZIP.getName())){
                            if("1".equals(attr.getPvalue())){
                                confs.add("gzip on;\n    ");
                                confs.add("gzip_buffers 32 4K;\n    ");
                                confs.add("gzip_http_version 1.1;\n    ");
                                confs.add("gzip_disable \"MSIE [1-6]\\.\";\n    ");
                            }else  {
                                return "";
                            }
                        }else if(attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_GZIP_MIN_LENGTH.getName())){
                            confs.add("gzip_min_length "+attr.getPvalue()+";\n    ");
                        }else if(attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_GZIP_COMP_LEVEL.getName())){
                            confs.add("gzip_comp_level "+attr.getPvalue()+";\n    ");
                        }else if(attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_GZIP_VARY.getName())){
                            if("1".equals(attr.getPvalue())){
                                confs.add("gzip_vary on;\n    ");
                            }
                        }else if(attr.getPkey().equals(SiteAttrEnum.PERFORMANCE_GZIP_TYPES.getName())){
                            gzip_types.append(attr.getPvalue()).append(" ");
                            types_sum++;
                        }
                    }
                    gzip_types.append(";\n    ");
                    if(types_sum>0){
                        confs.add(gzip_types.toString());
                    }
                    Collections.sort(confs);
                    for (String str:confs) {
                        gzip.append(str);
                    }
                    return gzip+"\n";
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return gzip.toString();
    }

    /**
     * 反射 站点信息
     * @param parameter
     * @return
     */
    private String Get_site_info(String parameter){
        //{"condition":"status=1","variableName":"###site_id###","variableVersion":"2.0.0","row":{"main_server_name":"text.com","id":1},"fields":"id,main_server_name","table":"tb_site"}
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String site_id=jsonRow.getString("id");
                    String variableName=jsonObject.getString("variableName");
                    variableName=variableName.replace("###site_","");
                    variableName=variableName.replace("###","");
                    if(StringUtils.isNotBlank(variableName)){
                        SiteAttrEnum attrObj= SiteAttrEnum.getObjByName(variableName) ;
                        if(null==attrObj)
                        {
                            logger.error("["+variableName+"]未知变量");
                            return "";
                        }
                        String type=attrObj.getType();
                        if("bool".equals(type) || "int".equals(type) || "text".equals(type)){
                            TbSiteAttrEntity tbSiteAttr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                    .eq("site_id",site_id)
                                    .eq("pkey",variableName)
                                    .eq("status",1)
                                    .select("pvalue")
                                    .last("limit 1"));
                            if(null==tbSiteAttr){
                                logger.error("["+variableName+"] 变量未设定值");
                                return attrObj.getDefaultValue();
                            }
                            if("bool".equals(type)){
                                return "1".equals(tbSiteAttr.getPvalue())?"on":"off";
                            }
                            return  tbSiteAttr.getPvalue();
                        }else if("m_text".equals(type)){
                            List<TbSiteAttrEntity> list=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",site_id).eq("pkey",variableName).eq("status",1).select("pvalue"));
                            StringBuilder retStr= new StringBuilder(" ");
                            for (TbSiteAttrEntity attr:list){
                                retStr.append(" ").append(attr.getPvalue()).append(" ");
                            }
                            return retStr.toString();
                        }else if("mm_text".equals(type)){
                            logger.error("--=todo=--");
                            //todo
                        }else{
                            logger.error("["+variableName+"]未知变量 type 类型");
                            return "";
                        }
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  "";
    }

    private String Get_site_client_max_body_size(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PERFORMANCE_LIMIT_UPLOAD_SIZE.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            return "client_max_body_size "+ attr.getPvalue()+"m;";
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_site_limit_rate(String parameter){
        Integer defaultValue=Integer.valueOf(SiteAttrEnum.PERFORMANCE_LIMIT_SPEED.getDefaultValue());
        String defaultString=String.format("limit_rate %dk ;",defaultValue);
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteEntity siteEntity=tbSiteDao.selectById(siteId);
                    if(null==siteEntity){
                        return defaultString;
                    }
                    Integer confValue=defaultValue;
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PERFORMANCE_LIMIT_SPEED.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue()) ){
                            Integer v=Integer.parseInt(attr.getPvalue());
                            if (null!=v && v>0 ){
                                confValue=v;
                            }
                        }
                    }
                    String sn=siteEntity.getSerialNumber();
                    CdnSuitEntity suitEntity=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                            .eq("serial_number",sn)
                            .eq("status",CdnSuitStatusEnum.NORMAL.getId())
                            .orderByDesc("id")
                            .last("limit 1")
                    );
                    if (null==suitEntity){
                        if (confValue<defaultValue){
                           return String.format("limit_rate %dk ;",confValue);
                        }else{
                            return defaultString;
                        }

                    }
                    ProductAttrVo vo=DataTypeConversionUtil.string2Entity(suitEntity.getAttrJson(),ProductAttrVo.class);
                    if (null==vo){
                        if (confValue<defaultValue){
                            return String.format("limit_rate %dk ;",confValue);
                        }else{
                            return defaultString;
                        }
                    }
                    if (null!=vo.getLimit_rate()){
                        if (confValue<vo.getLimit_rate()){
                            return String.format("limit_rate %dk ;",confValue);
                        }else{
                            return String.format("limit_rate %dk ;",vo.getLimit_rate());
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return defaultString;
    }

    private String Get_site_serialNumber(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteEntity siteEntity=tbSiteDao.selectById(siteId);
                    if(null!=siteEntity){
                        String SN=siteEntity.getSerialNumber();
                        if(StringUtils.isNotBlank(SN)){
                            return SN;
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "unknown";
    }

    private String Get_site_websocket(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.ADVANCED_CONF_WEBSOCKET.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            return "proxy_set_header Upgrade $http_upgrade;\n        proxy_set_header Connection \"Upgrade\";";
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }




    private String Get_site_sni(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteMutAttrEntity mutAttrKeyEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                            .eq("status",1)
                            .last("limit 1")
                    );
                    TbSiteMutAttrEntity mutAttrPemEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                            .eq("status",1)
                            .last("limit 1")
                    );
                    if(null!=mutAttrKeyEntity && null!=mutAttrPemEntity){
                        TbSiteAttrEntity SniAttr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                .eq("site_id",siteId)
                                .eq("pkey",SiteAttrEnum.SOURCE_SNI.getName())
                                .eq("status",1)
                                .last("limit 1"));
                        if(null!=SniAttr){
                            if (StringUtils.isNotBlank(SniAttr.getPvalue())){
                                return String.format("proxy_ssl_server_name on;\n    proxy_ssl_name %s ;",SniAttr.getPvalue());
                            }
                        }
                        return "proxy_ssl_server_name on;\n    proxy_ssl_name $host;";
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * HSTS HSTS 为强制客户端（如浏览器）使用 HTTPS 与服务器创建连接，启用 HSTS 前请先启用 HTTPS 协议。
     * @param parameter
     * @return
     */
    private String Get_site_mobile_jump(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PERFORMANCE_MOBILE_JUMP.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            //permanent 301  redirect 302
                            return  "if ($http_user_agent ~* (mobile|nokia|iphone|ipad|android|samsung|htc|blackberry)) {\n" +
                                    "        rewrite  ^(.*)    "+attr.getPvalue()+"$1 redirect ;\n" +
                                    "    }";
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }


    private String Get_site_forced_hsts(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SSL_FORCED_HSTS.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue()) && "1".equals(attr.getPvalue())){
                            return "add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;";
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String error_page(String[] codes,String Html){
        StringBuilder code= new StringBuilder();
        StringBuilder loc_path= new StringBuilder();
        for (String c:codes){
            code.append(c).append(" ");
            loc_path.append(c);
        }
        loc_path.append(".html");
        String s_html=Html.replaceAll("\\s","");
        s_html=s_html.replaceAll("\"", "'");
        return "error_page "+code+" /"+loc_path+";\n" +
                "    location = /"+loc_path+" {\n" +
                "        root /usr/share/nginx/html; 200 \""+s_html+"\";\n" +
                "    }";
    }

    private String error_page_v2(String error_code,String siteId){
        return  String.format("error_page %s  /%s.html;\n    location = /%s.html {\n         root conf/conf/html/%s/;\n    }\n    " ,error_code,error_code,error_code,siteId);
    }

    private String Get_site_error_page_chunk(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    //String variableName=jsonObject.getString("variableName");//###error_page_chunk###
                    //error_page_400
                    //{"type":1,"html_1":"<!DOCTYPE html><html lang=\"zh-CN\"><head></head><body></body></html>","html_2":"<!DOCTYPE html><html lang=\"zh-CN\"><head></head><body></body></html>","html_3":""}
                    //String pKey=variableName.replace("_chunk###","");
                    //pKey=pKey.replace("###","");
                    //String pub_Key=pKey.replace("page_","");
                    List<String> error_page_pkeys=SiteAttrEnum.getAllErrorPage();
                    List<TbSiteMutAttrEntity> mutAttr_list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .in("pkey",error_page_pkeys.toArray())
                            .eq("status",1));
                    if(0==mutAttr_list.size()){
                       //logger.debug("mutAttr_list is empty");
                    }
                    for (TbSiteMutAttrEntity mutAttr:mutAttr_list){
                        String error_code=mutAttr.getPkey().replace("error_page_","");
                        String pv=mutAttr.getPvalue();
                        JSONObject obj=DataTypeConversionUtil.string2Json(pv);
                        if(obj.containsKey("type")){
                            Integer PageType=obj.getInteger("type");
                            if(1==PageType){
                                //error_400
                                //cdn 公共type
                                String P_KEY="error_"+error_code;
                                TbCdnPublicMutAttrEntity publicMutAttrEntity=publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().
                                        eq("pkey",P_KEY)
                                        .eq("status",1)
                                        .select("id")
                                );
                                if(null!=publicMutAttrEntity){
                                    sb.append(error_page_v2(error_code,siteId.toString()));
                                }
                            }else if(2==PageType){
                               //logger.debug("PageType=2 is not define");
                                //源站 ==关闭
                            }else if(3==PageType){
                                // 自定义
                                if(obj.containsKey("html_3") && StringUtils.isNotEmpty(obj.getString("html_3"))){
                                    sb.append(error_page_v2(error_code,siteId.toString()));
                                }
                            }else if(4==PageType){
                               //logger.debug("4==errorPageType");
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            sb.append("\n");
        }
        return sb.toString();
    }

    private String Get_site_ocsp(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.ADVANCED_CONF_OCSP.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                sb.append("ssl_stapling on;\n");
                                sb.append("    ssl_stapling_verify on;\n");
                                // conf/ssl/%s.crt
                                sb.append("    ssl_trusted_certificate conf/ssl/ssl_"+siteId+".crt; \n");
                                sb.append("    resolver 8.8.8.8 8.8.4.4 valid=60s ipv6=off; \n");
                                sb.append("    resolver_timeout 5s;\n");
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String Get_site_accept_encoding_head(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_STATUS.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                return "proxy_set_header Accept-Encoding \"\";";
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_site_sub_filter(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_STATUS.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                String subsFilterTypes="text/html";
                                String subFilterOnce="on";
                                List<String>  subs_filter_list;
                                //types
                                TbSiteAttrEntity attr_types=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("site_id",siteId)
                                        .eq("pkey",SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_TYPES.getName())
                                        .eq("status",1)
                                        .last("limit 1"));
                                if(null!=attr_types && StringUtils.isNotBlank(attr_types.getPvalue())){
                                    //String[] vs=attr_types.getPvalue().split(" ");
                                    //subsFilterTypes=String.join("|",Arrays.asList(vs));
                                    subsFilterTypes=attr_types.getPvalue();
                                }
                                //once
                                TbSiteAttrEntity attr_once=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("site_id",siteId)
                                        .eq("pkey",SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER_ONCE.getName())
                                        .eq("status",1)
                                        .last("limit 1"));
                                if(null!=attr_once && StringUtils.isNotBlank(attr_once.getPvalue())){
                                    if("1".equals(attr_once.getPvalue())){
                                        subFilterOnce="off";
                                    }
                                }
                                // filter
                                List<TbSiteAttrEntity> list=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("site_id",siteId)
                                        .eq("pkey",SiteAttrEnum.CONTENT_SECURITY_SUB_FILTER.getName())
                                        .eq("status",1)
                                );
                                subs_filter_list=list.stream().map(TbSiteAttrEntity::getPvalue).collect(Collectors.toList());
                                if(subs_filter_list.size()>0){
                                    StringBuilder model= new StringBuilder("sub_filter_types   " + subsFilterTypes + ";\n");
                                    model.append("    sub_filter_once     ").append(subFilterOnce).append(";\n");
                                    for (String f:subs_filter_list){
                                        String[] ab=f.split("----");
                                        if(ab.length>=2){
                                            if(!ab[0].contains("'")  && !ab[1].contains("'")){
                                                model.append("    sub_filter    '").append(ab[0]).append("' '").append(ab[1]).append("';\n");
                                            }
                                        }
                                    }
                                    return model.toString();
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_site_ignore_cache(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PERFORMANCE_CACHE_IGNORE_CONTROL_PRAGMA.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                sb.append("proxy_ignore_headers \"X-Accel-Expires\";\n");
                                sb.append( "    proxy_ignore_headers \"Expires\";\n");
                                sb.append( "    proxy_ignore_headers \"Cache-Control\"; \n");
                                sb.append( "    proxy_hide_header \"Expires\";\n");
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String Get_site_proxy_buffering(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PERFORMANCE_CACHE_PROXY_BUFFERING.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("0".equals(attr.getPvalue())){
                                sb.append("proxy_buffering off;\n");
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String Get_site_server_user_custom(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteMutAttrEntity attr=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.ADVANCED_CONF_SERVER_USER_CUSTOM_INFO.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            return  attr.getPvalue();
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_site_ants_waf(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.WAF_STATUS.getName();
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                return "on";
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "off";
    }


    //v1 ants waf CONF
    /*
    private String Get_site_waf_chunk(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.WAF_STATUS.getName();
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                sb.append("ants_waf on;\n");
                                String rule_mode_for="1 403";
                                String rule_mode_sus="1 0";
                                pKey=SiteAttrEnum.WAF_RULE_FORBID_MODE.getName();
                                attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("site_id",siteId)
                                        .eq("pkey",pKey)
                                        .eq("status",1)
                                        .last("limit 1"));
                                if(null!=attr){
                                    if (StringUtils.isNotBlank(attr.getPvalue())){
                                        rule_mode_for=attr.getPvalue();
                                    }
                                }
                                pKey=SiteAttrEnum.WAF_RULE_SUSPICIOUS_MODE.getName();
                                attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("site_id",siteId)
                                        .eq("pkey",pKey)
                                        .eq("status",1)
                                        .last("limit 1"));
                                if(null!=attr){
                                    if (StringUtils.isNotBlank(attr.getPvalue())){
                                        rule_mode_sus=attr.getPvalue();
                                    }
                                }
                                sb.append(String.format("    ants_waf_rule_mode  %s %s;\n",rule_mode_for,rule_mode_sus));
                                TbSiteAttrEntity attr2=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                        .eq("site_id",siteId)
                                        .eq("pkey",SiteAttrEnum.WAF_CHECK_TOKEN_CONFIG.getName())
                                        .eq("status",1)
                                        .last("limit 1"));
                                if(null!=attr2){
                                    if (StringUtils.isNotBlank(attr2.getPvalue())){
                                        sb.append(String.format("    ants_waf_token_set %s;\n",attr2.getPvalue()));
                                    }
                                }


                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  sb.toString();
    }
     */
    /*
    private String Get_site_ants_waf_rule_mode(String parameter){
        String rule_mode="1 0 2 1";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.WAF_RULE_FORBID_MODE.getName();
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            rule_mode=rule_mode.replace("1 0",attr.getPvalue());
                        }
                    }
                    pKey=SiteAttrEnum.WAF_RULE_SUSPICIOUS_MODE.getName();
                    attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if (StringUtils.isNotBlank(attr.getPvalue())){
                            rule_mode=rule_mode.replace("2 1",attr.getPvalue());
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return rule_mode;
    }
    */

    private String pattern_str(String siteId, String pKey){
        String ret="";
        try{
            List<TbSiteMutAttrEntity>  attrList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey",pKey)
                    .eq("status",1)
            );
            for (TbSiteMutAttrEntity attr:attrList){
                if (StringUtils.isNotBlank(attr.getPvalue())){
                    JSONObject pv_obj=DataTypeConversionUtil.string2Json(attr.getPvalue());
                    if(pv_obj.containsKey("value") ){
                        String p_=pv_obj.getString("value");
                        if(StringUtils.isNotBlank(p_)){
                            if(StringUtils.isNotBlank(ret)){
                                ret+="\n";
                            }
                            ret+=p_;
                        }
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return  ret;
    }

    private  String Get_site_pass_pattern(String parameter){
       String ret="";
//        try{
//            JSONObject jsonObject=JSONObject.parseObject(parameter);
//            if(jsonObject.containsKey("row")){
//                JSONObject jsonRow=jsonObject.getJSONObject("row");
//                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
//                    String siteId=jsonRow.getString("id");
//                    String pKey=SiteAttrEnum.PUB_WAF_PASS_SELECTS.getName();
//                    return  pattern_str(siteId,pKey);
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
       return ret;
    }

    private String pub_waf_rule(String siteId,String key){
        StringBuilder sb=new StringBuilder();
        List<TbSiteAttrEntity> list=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",siteId).eq("pkey",key).eq("status",1).like("pvalue","#1").select("pvalue"));
        for (TbSiteAttrEntity item:list){
            String id_select=item.getPvalue();
            if(StringUtils.isNotBlank(id_select)){
                String[] id_s=id_select.split("#");
                if(id_s.length>=2){
                    if("1".equals(id_s[1])){
                        String id=id_s[0];
                        TbCdnPublicMutAttrEntity  publicMutAttrEntity=publicMutAttrDao.selectById(id);
                        if(null!=publicMutAttrEntity && publicMutAttrEntity.getStatus()==1){
                            if(StringUtils.isNotBlank(publicMutAttrEntity.getPvalue())){
                                JSONObject object=DataTypeConversionUtil.string2Json(publicMutAttrEntity.getPvalue());
                                if(object.containsKey("rule")){
                                    sb.append(object.getString("rule"));
                                    sb.append("\n");
                                }
                            }

                        }
                    }
                }
            }
        }
        return  sb.toString();
    }

    /*
    private String Get_site_pub_waf_pass(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.WAF_RULE_PASS.getName();
                    ret=pub_waf_rule(siteId,pKey);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }
    */


    private  String Get_site_forbid_pattern(String parameter){
//        try{
//            JSONObject jsonObject=JSONObject.parseObject(parameter);
//            if(jsonObject.containsKey("row")){
//                JSONObject jsonRow=jsonObject.getJSONObject("row");
//                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
//                    String siteId=jsonRow.getString("id");
//                    String pKey=SiteAttrEnum.PUB_WAF_PASS_SELECTS.getName();
//                    return  pattern_str(siteId,pKey);
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        return "";
    }

    private String Get_site_pub_waf_forbid(String parameter){
        String ret="";
//        try{
//            JSONObject jsonObject=JSONObject.parseObject(parameter);
//            if(jsonObject.containsKey("row")){
//                JSONObject jsonRow=jsonObject.getJSONObject("row");
//                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
//                    String siteId=jsonRow.getString("id");
//                    String pKey=SiteAttrEnum.PUB_WAF_FORBID_SELECTS.getName();
//                    ret=pub_waf_rule(siteId,pKey);
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        return ret;
    }
    /*
    private  String Get_site_suspicious_pattern(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.WAF_RULE_SUSPICIOUS.getName();
                    return  pattern_str(siteId,pKey);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
     */

    private String Get_site_pub_waf_suspicious(String parameter){
        String ret="";
//        try{
//            JSONObject jsonObject=JSONObject.parseObject(parameter);
//            if(jsonObject.containsKey("row")){
//                JSONObject jsonRow=jsonObject.getJSONObject("row");
//                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
//                    String siteId=jsonRow.getString("id");
//                    String pKey=SiteAttrEnum.PUB_WAF_SUSPICIOUS_SELECTS.getName();
//                    ret=pub_waf_rule(siteId,pKey);
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        return ret;
    }


    private String Get_site_access_log(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") ){
                    String name="";
                    if(jsonRow.containsKey("main_server_name")){
                        name= jsonRow.getString("main_server_name");
                    }else if(jsonRow.containsKey("mainServerName")){
                        name= jsonRow.getString("mainServerName");
                    }
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.WAF_ACCESS_LOG.getName();
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr) {
                        if(StringUtils.isNotBlank(attr.getPvalue())){
                            if("1".equals(attr.getPvalue())){
                                return "/logs/"+siteId+"_"+name+".access.log";
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "off";
    }


    /**
     *正则转化工具
     * @param precise_waf_value  规则正则STRING
     * @param mode
     * @return
     */
    private String preciseWafToRuleUtilHandle(String precise_waf_value, String mode){
        String ret="";
        LinkedHashMap linkedHashMap= PreciseWafParamEnum.getTransLinkMap();
        JSONObject obj=DataTypeConversionUtil.string2Json(precise_waf_value);
        String obj_mode="all";
        if(obj.containsKey("mode")){
            obj_mode=obj.getString("mode");
        }
        if( obj.containsKey("rule")){
            if(obj_mode.equals(mode)){
                //提取所有rule
                JSONArray jsonArray=obj.getJSONArray("rule");
                Map<String,JSONObject> map=new HashMap();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject rule_i=jsonArray.getJSONObject(i);
                    if(rule_i.containsKey("type") && rule_i.containsKey("handle") && rule_i.containsKey("content")){
                        String t_type=rule_i.getString("type");
                        String t_typeValue= PreciseWafParamEnum.getValueTypeByName(t_type);
                        if (StringUtils.isBlank(t_typeValue)){
                            continue;
                        }
                        if("object".equals(t_typeValue)){
                            switch (t_type){
                                case "return_code":
                                    if (true){
                                        JSONObject  return_codeObj=rule_i.getJSONObject("content");
                                        Map<String,String> code_map=new HashMap();
                                        code_map.put("2xx","code_0_percentage_2xx");
                                        code_map.put("3xx","code_1_percentage_3xx");
                                        code_map.put("4xx","code_2_percentage_4xx");
                                        code_map.put("5xx","code_3_percentage_5xx");
                                        for (String c_name : code_map.keySet()) {
                                            if(return_codeObj.containsKey(c_name)){
                                                JSONObject code_i_op_obj=return_codeObj.getJSONObject(c_name);
                                                if(code_i_op_obj.containsKey("handle") && code_i_op_obj.containsKey("value")){
                                                    JSONObject rule_build_obj=new JSONObject();
                                                    rule_build_obj.put("type",code_map.get(c_name));
                                                    rule_build_obj.put("handle",code_i_op_obj.getString("handle"));
                                                    rule_build_obj.put("content",code_i_op_obj.getString("value"));
                                                    map.put(code_map.get(c_name),rule_build_obj);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "ants_weight":
                                    if (true){
                                        JSONObject  return_codeObj=rule_i.getJSONObject("content");
                                        Map<String,String> code_map=new HashMap();
                                        code_map.put("AS1","type_0_percentage");
                                        code_map.put("AS2","type_1_percentage");
                                        code_map.put("AS3","type_2_percentage");
                                        code_map.put("AS4","type_3_percentage");
                                        code_map.put("AS5","type_4_percentage");
                                        code_map.put("AS6","type_5_percentage");
                                        code_map.put("AS7","type_6_percentage");
                                        code_map.put("AS8","type_7_percentage");
                                        code_map.put("AS9","type_8_percentage");
                                        code_map.put("AS10","type_9_percentage");
                                        for (String c_name : code_map.keySet()) {
                                            if(return_codeObj.containsKey(c_name)){
                                                JSONObject code_i_op_obj=return_codeObj.getJSONObject(c_name);
                                                if(code_i_op_obj.containsKey("handle") && code_i_op_obj.containsKey("value")){
                                                    JSONObject rule_build_obj=new JSONObject();
                                                    rule_build_obj.put("type",code_map.get(c_name));
                                                    rule_build_obj.put("handle",code_i_op_obj.getString("handle"));
                                                    rule_build_obj.put("content",code_i_op_obj.getString("value"));
                                                    map.put(code_map.get(c_name),rule_build_obj);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "method_weight":
                                    if (true){
                                        JSONObject  return_codeObj=rule_i.getJSONObject("content");
                                        Map<String,String> code_map=new HashMap();
                                        code_map.put("get","ip_get_percentage");
                                        code_map.put("post","ip_post_percentage");
                                        code_map.put("head","ip_head_percentage");
                                        for (String c_name : code_map.keySet()) {
                                            if(return_codeObj.containsKey(c_name)){
                                                JSONObject code_i_op_obj=return_codeObj.getJSONObject(c_name);
                                                if(code_i_op_obj.containsKey("handle") && code_i_op_obj.containsKey("value")){
                                                    JSONObject rule_build_obj=new JSONObject();
                                                    rule_build_obj.put("type",code_map.get(c_name));
                                                    rule_build_obj.put("handle",code_i_op_obj.getString("handle"));
                                                    rule_build_obj.put("content",code_i_op_obj.getString("value"));
                                                    map.put(code_map.get(c_name),rule_build_obj);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                default:
                                    logger.error("unknown type ["+t_typeValue+"]");
                                    break;
                            }
                        }else{
                            map.put(t_type,rule_i);
                        }

                    }
                }
                StringBuilder line_rule= new StringBuilder();//code_0_percentage_2xx  code_0_percentage_2xx
                //重新按导出顺序构造正则
                Iterator<Map.Entry> iterator= linkedHashMap.entrySet().iterator();
                while(iterator.hasNext())
                {
                    Map.Entry entry = iterator.next();
                    //System.out.println(entry.getKey()+":"+entry.getValue());
                    if (map.containsKey(entry.getKey())){
                        String sk=entry.getValue().toString();
                        JSONObject t_rule=map.get(entry.getKey());
                        String handle=t_rule.getString("handle");
                        String content=t_rule.getString("content");
                        line_rule.append(RegExUtils.createJavaPatternRegStr(sk, handle, content));
                    }
                }
                if(StringUtils.isNotBlank(line_rule.toString())){
                    ret+=line_rule+"\n";

                }
            }
        }
        return  ret;
    }

    /**
     * 公共WAF ID 获取生成正则
     * @param selectIndex
     * @param mode
     * @return
     */
    private String pubWafPreciseWafToRegx(String selectIndex, String mode){
        StringBuilder sb=new StringBuilder();
        List<TbCdnPublicMutAttrEntity> pubRulesList=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey", PublicEnum.WEB_RULE_PRECISE_DETAIL.getName())
                .eq("parent_id",selectIndex)
                .eq("status",1)
                .orderByDesc("weight")
        ) ;
        for (TbCdnPublicMutAttrEntity pubAttr:pubRulesList){
            List<String> stringList=this.getRexStrListV300(pubAttr.getPvalue());
            stringList.forEach(item->{
                sb.append(item);
                sb.append("\n");
            });
        }
        return  sb.toString();
    }

    //v2 站点WAF regx
    private String sitePriWafRegx(String siteId){
        StringBuilder sb=new StringBuilder();
        List<TbSiteMutAttrEntity> priWafList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                .eq("status",1)
                .orderByDesc("weight"));
        for (TbSiteMutAttrEntity priWafEntity:priWafList){
            String rule_=this.preciseWafToRuleUtilHandle(priWafEntity.getPvalue(),"all");
            if(StringUtils.isNotBlank(rule_)){
                sb.append(rule_);
                sb.append("\n");
                //ret+=rule_+"\n";
            }
        }
        return  sb.toString();
    }

    // 获取 站点 存储 正则规则集合
    private String sitePreciseWafToRegx(String siteId, String mode){
        StringBuilder sb=new StringBuilder();
        try{
            //list 中每一个包含多个条件；重构顺序构造一条正则
            String selectIndex="0";
            TbSiteAttrEntity siteAttr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",siteId).eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName()).eq("status",1).last("limit 1"));
            if(null!=siteAttr  ){
                selectIndex=siteAttr.getPvalue();
            }

            if("0".equals(selectIndex)){
                //自定义精准
                sb.append(this.sitePriWafRegx(siteId));
            }else {
                //公共精准
                sb.append(this.pubWafPreciseWafToRegx(selectIndex,mode));
            }
            //List<TbSiteAttrEntity> list=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",siteId).eq("pkey",).like("pvalue","#1").eq("status",1));

        }catch (Exception e){
            e.printStackTrace();
        }
        return  sb.toString();
    }

    //V1 (停止使用)通过正则
    private String Get_site_pub_precise_waf_pass(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    ret= sitePreciseWafToRegx(siteId,"pass");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    //V1 (停止使用)封禁正则
    private String  Get_site_pub_precise_waf_forbid(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    ret= sitePreciseWafToRegx(siteId,"forbid");

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }
    //V1 (停止使用)可疑正则
    private String Get_site_pub_precise_waf_suspicious(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    ret= sitePreciseWafToRegx(siteId,"suspicious");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    //V1 鉴权配置
    private String Get_site_ants_waf_token_set(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.WAF_CHECK_TOKEN_CONFIG.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr){
                        if(StringUtils.isNotBlank(attr.getPvalue())){
                            return attr.getPvalue();
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "-1 -1 '' ''";
    }

    public List<Map> getSiteHtmlByIds(String param){
        List<Map> resultList=new ArrayList<>();
        try{
            JSONObject paraJson=DataTypeConversionUtil.string2Json(param);
            String path_model="";
            String variableName="";
            String ClientVersion="";
            String siteId=null;
            if(paraJson.containsKey("path_model")){
                path_model=paraJson.getString("path_model");
            }
            if(paraJson.containsKey("variableName")){
                variableName=paraJson.getString("variableName");
            }
            if(paraJson.containsKey("siteId")){
                siteId=paraJson.getString("siteId");
            }

            List<TbSiteMutAttrEntity>list=new ArrayList<>();
            List<String> errorPages = SiteAttrEnum.getAllErrorPage();
            for (String pkey:errorPages){
                List<TbSiteMutAttrEntity> mutAttrList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq(StringUtils.isNotBlank(siteId),"site_id",siteId)
                        .eq("pkey",pkey)
                        .eq("status",1)
                );
                list.addAll(mutAttrList);
            }
            for (TbSiteMutAttrEntity mutAttr:list){
                JSONObject errorPageObj=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                if (null!=errorPageObj && errorPageObj.containsKey("type")){
                    if(1==errorPageObj.getInteger("type")){
                        //公共
                        String pub_pkey=mutAttr.getPkey().replace("_page","");
                        TbCdnPublicMutAttrEntity publicMutAttr=publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",pub_pkey).last("limit 1"));
                        if(null!=publicMutAttr){
                            Map map=new HashMap();
                            String v_name=mutAttr.getSiteId()+"/"+mutAttr.getPkey().replace("error_page_","");//+"_"+ mutAttr.getId()
                            // home/local/nginx/conf/conf/html/###html_ids###.html
                            // home/local/nginx/conf/conf/html/siteId/404.html
                            map.put("path",path_model.replace(variableName,v_name));
                            JSONObject jsonObject=new JSONObject();
                            jsonObject.put("row",publicMutAttr);
                            map.put("parameter",jsonObject.toJSONString());
                            resultList.add(map);
                        }
                    }else if(3==errorPageObj.getInteger("type")){
                        //自定义
                        Map map=new HashMap();
                        //String v_name=mutAttr.getPkey()+"_"+mutAttr.getSiteId();//+"_"+ mutAttr.getId()
                        String v_name=mutAttr.getSiteId()+"/"+mutAttr.getPkey().replace("error_page_","");//+"_"+ mutAttr.getId()
                        map.put("path",path_model.replace(variableName,v_name));
                        JSONObject jsonObject=new JSONObject();
                        jsonObject.put("row",mutAttr);
                        map.put("parameter",jsonObject.toJSONString());
                        resultList.add(map);
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return resultList;
    }

    //错误码页面_util
    private List<Map> Get_site_html_ids(String param){
        return this.getSiteHtmlByIds(param);
    }

    //错误码页面
    private String Get_site_html_content(String parameter){
        String html="<a> public  error page  is error </a>";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("parentId") && jsonRow.containsKey("pvalue")){
                    return  jsonRow.getString("pvalue");
                }else if(jsonRow.containsKey("pkey") && jsonRow.containsKey("pvalue")) {
                    String pkey = jsonRow.getString("pkey");
                    final String pattern1 = "^error_\\d+$";
                    Pattern r1 = Pattern.compile(pattern1);
                    Matcher m1 = r1.matcher(pkey);
                    if (m1.matches()){
                        return jsonRow.getString("pvalue");
                    }
                    final String pattern2 = "^error_page_\\d+$";
                    Pattern r2 = Pattern.compile(pattern2);
                    Matcher m2 = r2.matcher(pkey);
                    if (m2.matches()){
                        String valueStr=jsonRow.getString("pvalue");
                        if (StringUtils.isBlank(valueStr)){
                            return html;
                        }
                        JSONObject valueObj=DataTypeConversionUtil.string2Json(valueStr);
                        if(null!=valueObj &&valueObj.containsKey("html_3")){
                            return valueObj.getString("html_3");
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return html;
    }

    //v1 final 输出 site 正则 到waf 文件
    private String Get_site_regex_chain_detail(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    ret=this.sitePreciseWafToRegx(siteId,"all");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return  ret;
    }

    //v2处理规则 工具
    private String preciseWafToRuleOpUtil(String preciseWafValue){
        StringBuilder sb=new StringBuilder();
        //LinkedHashMap linkedHashMap=this.preciseWafParam();
        JSONObject obj=DataTypeConversionUtil.string2Json(preciseWafValue);
        String obj_mode="all";
        if(null!=obj && obj.containsKey("mode")){
            obj_mode=obj.getString("mode");
        }
        //{"rule":[{"type":"method","handle":"e","content":"POST|GET"}],
        // "remark":"TEST",
        // "waf_op":{"key":"pass","param":0,"handle":"pass"},
        // "status":1,"update_time":"2022-07-26 18:23:06"}
        if( obj.containsKey("waf_op") && "all".equals(obj_mode)){
            JSONObject waf_op_obj=obj.getJSONObject("waf_op");
            if (waf_op_obj.containsKey("key") && waf_op_obj.containsKey("param")){
                String op_key=waf_op_obj.getString("key");
                Integer waf_mode_value= WafOpEnum.getWafModeValueByKey(op_key);
                Integer op_param_value=Integer.parseInt(waf_op_obj.getString("param")) ;
                //ret.order(ByteOrder.LITTLE_ENDIAN);
                // ret.putInt(waf_mode_value);
                //ret.putInt(op_param_value);
                //ret+=String.format("%04d",waf_mode_value);
                //ret+=String.format("%04d",op_param_value);
                sb.append(String.format("%04d",waf_mode_value));
                sb.append(String.format("%04d",op_param_value));

            }
        }
        return  sb.toString();
    }

    //私有处理规则
    private String pri_waf_rule(String siteId){
        StringBuilder sb=new StringBuilder();
        List<TbSiteMutAttrEntity> pri_waf_list=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                .eq("status",1)
                .orderByDesc("weight")
        );
        for (TbSiteMutAttrEntity priWafEntity:pri_waf_list){
            //String rule_=this.precise_waf_to_rule_util(priWafEntity.getPvalue(),mode);
            String rule_op=this.preciseWafToRuleOpUtil(priWafEntity.getPvalue());
            // ret+= rule_op
            //rule_op.flip();
            //Charset charset = Charset.forName("ISO-8859-1");
            sb.append(rule_op);

        }
        return sb.toString();
    }

    //公共处理规则
    private String pubWafPreciseRule(String selectIndex){
        StringBuilder sb=new StringBuilder();
        List<TbCdnPublicMutAttrEntity> fRulesList=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",PublicEnum.WEB_RULE_PRECISE_DETAIL.getName())
                .eq("parent_id",selectIndex)
                .eq("status",1)
                .orderByDesc("weight")) ;
        List<String>stringList=new ArrayList<>();
        fRulesList.forEach(item->{
            stringList.add(item.getPvalue());
        });
        return  this.getRuleFileV300(stringList);
    }

    //v1 站点处理规则工具
    private String site_precise_waf_op_utils(String siteId,String mode){
        StringBuilder sb=new StringBuilder();
        try{
            //list 中每一个包含多个条件；重构顺序构造一条正则
            String selectIndex="0";
            TbSiteAttrEntity siteAttr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",siteId).eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName()).eq("status",1).last("limit 1"));
            if(null!=siteAttr  ){
                selectIndex=siteAttr.getPvalue();
            }
            //List<Map<String,Object>> f_rules_list=new ArrayList<>();
            if("0".equals(selectIndex)){
                //自定义精准
                sb.append(this.pri_waf_rule(siteId));
            }else {
                //公共精准
                sb.append(this.pubWafPreciseRule(selectIndex));
            }
            //List<TbSiteAttrEntity> list=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",siteId).eq("pkey",).like("pvalue","#1").eq("status",1));

        }catch (Exception e){
            e.printStackTrace();
        }
        return  sb.toString();
    }

    //v1 final 站点处理规则输出 到waf 文件
    private String Get_site_regex_chain_rule_detail(String parameter){
        //String ret="";
        //char[] retArray={0xC9,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x2D,0x01,0x00,0x00,0xC8,0x00,0x00,0x00,0x2E,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x2E,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x2E,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x2E,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x2E,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x2D,0x01,0x00,0x00,0x93,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x64,0x00,0x00,0x00  };
        //return  new String(retArray);
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    return this.site_precise_waf_op_utils(siteId,"all");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    //V2  ants_waf_conf 配置reg rule 路径
    private String Get_site_ants_waf_conf(String parameter){
        //{"condition":"status=1",
        // "variableName":"###ants_waf_conf###",
        // "variableVersion":"2.10",
        // "client":{"area":"1","createtime":1660466058000,"line":"中国移动","stableScore":79,"remark":"1","regInfo":"{\"addtime\":\"1660466070\",\"ip\":\"119.97.137.47\",\"goods\":\"cdn节点\",\"endtime\":\"1670919285\",\"version\":\"2.10\"}","version":"2.10","confInfo":"{\"id\":84,\"proxy_max_temp_file_size\":\"1024m\",\"worker_processes\":\"auto\",\"proxy_cache_path_dir\":\"/data/cache\",\"error_log_level\":\"debug\"}","effectiveStartTime":1660466070000,"clientType":1,"checkTime":1661500656,"clientIp":"119.97.137.47","id":84,"updatetime":1661500656000,"effectiveEndingTime":1670919285000,"status":1},
        // "row":"{\"createtime\":1659921180000,\"serialNumber\":\"1659609845628002\",\"mainServerName\":\"waf.antsxdp.com\",\"id\":1066,\"userId\":61,\"status\":1}",
        // "fields":"id,main_server_name",
        // "table":"tb_site"}
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String siteId=jsonRow.getString("id");
                    TbSiteEntity siteEntity=tbSiteDao.selectById(siteId);
                    if (null !=siteEntity){
                        String mainServerName=siteEntity.getMainServerName();
                        String pubSelectIndex="0";
                        TbSiteAttrEntity siteAttr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                .eq("site_id",siteId)
                                .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_SELECTS.getName())
                                .eq("status",1)
                                .last("limit 1"));
                        if(null!=siteAttr  ){
                            pubSelectIndex=siteAttr.getPvalue();
                        }
                        if (!"0".equals(pubSelectIndex)){
                            // a 选择了公共 //来之 公共
                            // ants_waf_conf '/home/local/nginx/conf/etc/db_255_' '/home/local/nginx/conf/etc/rule_255_';
                            sb.append("ants_waf_conf  ");
                            sb.append(String.format("'/home/local/nginx/conf/etc/reg_%s_'  ",pubSelectIndex));
                            sb.append(String.format("'/home/local/nginx/conf/etc/rule_%s_' ;",pubSelectIndex));
                            return  sb.toString();
                        }
                        // b 选择了用户下的
                        String priSelectIndex="0";
                        TbSiteAttrEntity siteAttr2=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                                .eq("site_id",siteId)
                                .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_USER_SELECTS.getName())
                                .eq("status",1)
                                .last("limit 1"));
                        if(null!=siteAttr2  ){
                            priSelectIndex=siteAttr2.getPvalue();
                        }
                        if (!"0".equals(priSelectIndex)){
                            // c  为用户下其它站 精准
                            //ants_waf_conf '/home/local/nginx/conf/conf/waf/db_1066_waf.abc.com_'  '/home/local/nginx/conf/conf/waf/rule_1066_waf.abc.com_' ;
                            TbSiteEntity userOtherSiteEntity=tbSiteDao.selectById(priSelectIndex);
                            if (null!=userOtherSiteEntity){
                                // e 为其它站
                                sb.append("ants_waf_conf  ");
                                sb.append(String.format("'/home/local/nginx/conf/conf/waf/reg_%d_%s_'  ",userOtherSiteEntity.getId(),userOtherSiteEntity.getMainServerName()));
                                sb.append(String.format("'/home/local/nginx/conf/conf/waf/rule_%d_%s_'; ",userOtherSiteEntity.getId(),userOtherSiteEntity.getMainServerName()));
                                return  sb.toString();
                            }
                        }
                        // d 为自身
                        sb.append("ants_waf_conf  ");
                        sb.append(String.format("'/home/local/nginx/conf/conf/waf/reg_%s_%s_'  ",siteId,mainServerName));
                        sb.append(String.format("'/home/local/nginx/conf/conf/waf/rule_%s_%s_'; ",siteId,mainServerName));
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String Get_site_ants_waf_conf_v2(String parameter){
        String v0=Get_site_ants_waf_conf(parameter);
        String v2= v0.replace("ants_waf_conf","ants_waf_reg_rule_conf");
        System.out.println(v2);
        return v2;
    }


    private String Get_site_access_log_mode(String parameter){
        String ret="set $ants_waf_access_mode "+SiteAttrEnum.SITE_ACCESS_LOG_MODE.getDefaultValue()+";\n";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") ){
                    String siteId=jsonRow.getString("id");
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SITE_ACCESS_LOG_MODE.getName())
                            .eq("status",1)
                            .last("limit 1")
                    );
                    if(null!=attr) {
                        if(StringUtils.isNotBlank(attr.getPvalue())){
                            return  "set $ants_waf_access_mode "+attr.getPvalue()+";\n";
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  ret;
    }

    //v2 生成公共正则
    private String Get_pub_regx_chain_detail(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") ){
                    String selectIndex=jsonRow.getString("id");
                    ret=this.pubWafPreciseWafToRegx(selectIndex,"all");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  ret;
    }

    //waf2.0 生成私有正则文件
    private String Get_pri_regx_chain_detail(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") ){
                    String siteId=jsonRow.getString("id");
                    return this.sitePriWafRegx(siteId);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  ret;
    }

    /**
     * waf3.0 str2reg
     * @param regObj
     * @return
     */
    private List<String> getRexStrListV300(String regObj){
        List<String> list=new ArrayList<>();
        NgxWafRuleVo ruleVo=DataTypeConversionUtil.string2Entity(regObj,NgxWafRuleVo.class);
        ruleVo.getRule().forEach(item->{
            String typeValue= PreciseWafParamEnum.getValueTypeByName(item.getType());
            if (StringUtils.isNotBlank(typeValue)){
                String sk=PreciseWafParamEnum.getSearchKeyByName(item.getType());
                String regStr=  RegExUtils.createJavaPatternRegStrV300(sk, item.getHandle(), item.getContent());
                list.add(regStr);
            }
        });
        return list;
    }

    /**
     *  waf3.0 reg_list
     * @param parameter
     * @return
     */
    private String Get_pri_regx_chain_detail_v2(String parameter){
        String ret="";
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") ){
                    String siteId=jsonRow.getString("id");
                    StringBuilder sb=new StringBuilder();
                    List<TbSiteMutAttrEntity> priWafList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                            .eq("status",1)
                            .orderByDesc("weight"));
                    for (TbSiteMutAttrEntity priWafEntity:priWafList){
                        List<String> stringList=this.getRexStrListV300(priWafEntity.getPvalue());
                        stringList.forEach(item->{
                            sb.append(item);
                            sb.append("\r\n");
                        });
                    }
                    return sb.toString();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  ret;
    }

    // 公共正则 处理规则RULE
    private String Get_pub_regx_chain_rule_detail(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String selectIndex=jsonRow.getString("id");
                    return this.pubWafPreciseRule(selectIndex);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    //waf2.0 私有正则处理规则RULE
    private String Get_pri_regx_chain_rule_detail(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String siteId=jsonRow.getString("id");
                    return this.pri_waf_rule(siteId);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * waf3.0 数据2规则文件
     * @param regObjList
     * @return
     */
    private String getRuleFileV300(List<String> regObjList){
        StringBuilder sb=new StringBuilder();
        StringBuilder mSb=new StringBuilder();
        mSb.append("m:");
        int index=0;
        for (String reg:regObjList){
            StringBuilder rSb=new StringBuilder();
            rSb.append("r:");
            List<String>indexList=new ArrayList<>(64);
            NgxWafRuleVo ruleVo=DataTypeConversionUtil.string2Entity(reg,NgxWafRuleVo.class);
            for (int i = 0; i < ruleVo.getRule().size(); i++) {
                NgxWafRuleVo.RuleInfo  ruleInfo=ruleVo.getRule().get(i);
                mSb.append(RegExUtils.RegexOpTypeEnum.getModeByHandleKey(ruleInfo.getHandle()));
                indexList.add(String.valueOf(index));
                index++;
            }
            rSb.append(String.join(",",indexList));
            rSb.append("#");
            Integer mode=WafOpEnum.getWafModeValueByKey(ruleVo.getWaf_op().getKey());
            rSb.append(String.format("%04d",mode));
            Integer param=Integer.parseInt(ruleVo.getWaf_op().getParam()) ;
            rSb.append(String.format("%04d",param));
            sb.append(rSb.toString());
            sb.append("\r\n");
        }
        return mSb.toString()+"\r\n"+sb.toString();
    }

    /**
     *  waf 3.0处理规则 rule_file
     * @param parameter
     * @return
     */
    private String Get_pri_regx_chain_rule_detail_v2(String parameter){
        //m:10111
        //r:1,2,3#03020403
        //r:4,5#03020403
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String siteId=jsonRow.getString("id");
                    List<TbSiteMutAttrEntity> priWafList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.PRI_PRECISE_WAF_DETAILS.getName())
                            .eq("status",1)
                            .orderByDesc("weight")
                    );
                    List<String> stringList=new ArrayList<>(priWafList.size());
                    priWafList.forEach(item->{
                        stringList.add(item.getPvalue());
                    });
                    return this.getRuleFileV300(stringList);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_site_ssl_protocols(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id") && jsonObject.containsKey("variableName")){
                    String siteId=jsonRow.getString("id");
                    String pKey=SiteAttrEnum.SSL_PROTOCOLS.getName();
                    TbSiteAttrEntity attr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",pKey)
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr) {
                        if(StringUtils.isNotBlank(attr.getPvalue())){
                           return  attr.getPvalue();
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  SiteAttrEnum.SSL_PROTOCOLS.getDefaultValue();
    }

    private String Get_site_cache_purge_chunk(String parameter){
        try{
            //"$host$uri~";
            String sliceV="";
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String siteId=jsonRow.getString("id");
                    TbSiteAttrEntity attr_slice=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.SOURCE_RANGE.getName())
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=attr_slice){
                        if (StringUtils.isNotBlank(attr_slice.getPvalue())){
                            if("1".equals(attr_slice.getPvalue())){
                                sliceV= "        slice    1m;   \n" +
                                        "        proxy_set_header  Range $slice_range;\n";
                            }
                        }
                    }
                }
            }
            String cacheKey=this.Get_site_proxy_cache_key(parameter);
            cacheKey=cacheKey.replace("$uri","$1");
            return  "\n    location ~ /purge(/.*) {\n" +
                    "        allow    127.0.0.1;  \n" +
                    "        deny     all;          \n" +
                    sliceV+
                    "        proxy_cache_purge  cache "+cacheKey+";\n" +
                    "    }\n";
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }







    private String Get_site_more_clear_headers_chunk(String parameter){
        StringBuilder sb=new StringBuilder();
        sb.append("\n");
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    String siteId=jsonRow.getString("id");
                    List<TbSiteMutAttrEntity> clearHeads=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey",SiteAttrEnum.ADVANCED_CONF_MORE_CLEAR_HEADERS.getName())
                            .eq("status",1)
                            .orderByDesc("weight"));
                    for (TbSiteMutAttrEntity  entity:clearHeads){
                        //{"type":"custom","header":"1","content":"1","info":"xx"}
                        JSONObject kvJson=DataTypeConversionUtil.string2Json(entity.getPvalue());
                        //"{\"type\":\"Cache-Control\",\"header\":\"Cache-Control\",\"content\":\"1\",\"info\":\"1\"}"
                        if (null!=kvJson && kvJson.containsKey("header") && kvJson.containsKey("content")){
                            sb.append("    more_clear_headers '"+ kvJson.getString("header") +": "+ kvJson.getString("content") +"';\n");
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }


    private String Get_site_ants_site_cache_purge_chunk(String parameter){
        return "location /cache_uri_set {\n" +
                "        ants_waf_cache_uri_history;\n" +
                "    }";
    }

    private String Get_acme_sh_account_thumbprint_chunk(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            String account= AcmeShUtils.getAcmeAccount();
            if ( StringUtils.isNotBlank(account) ){
                sb.append("location ~ \"^/\\.well-known/acme-challenge/([-_a-zA-Z0-9]+)$\" {\n");
                sb.append("        default_type text/plain;\n");
                sb.append(String.format("        return 200  \"$1.%s\";\n",account));
                sb.append("    }\n");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  sb.toString();
    }



    private String Get_nginx_conf(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            //variableName
            if(jsonObject.containsKey("variableName")){
                String variableName=jsonObject.getString("variableName");
                String key=variableName.replace("###nginx_","");
                key=key.replace("###","");
                if(StringUtils.isNotBlank(key)){
                    if(jsonObject.containsKey("client")){
                        JSONObject jsonClient=jsonObject.getJSONObject("client");
                        if(jsonClient.containsKey("confInfo")){
                            String conf_info_str=jsonClient.getString("confInfo");
                            JSONObject conf_info_json=DataTypeConversionUtil.string2Json(conf_info_str);
                            if(null!=conf_info_json && conf_info_json.containsKey(key)){
                                if (StringUtils.isNotBlank(key)){
                                    return conf_info_json.getString(key);
                                }
                            }
                        }
                    }
                    TbCdnPublicMutAttrEntity publicMutAttrEntity=  publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                            .eq("pkey",key)
                            .last("limit 1")
                    );
                    if(null!=publicMutAttrEntity){
                        String value=publicMutAttrEntity.getPvalue();
                        if(StringUtils.isNotBlank(value)){
                            return  value;
                        }
                    }
                    if(null!=PublicEnum.getObjByName(key)){
                        return Objects.requireNonNull(PublicEnum.getObjByName(key)).getDefaultValue();
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "1";
    }

    private String Get_cache_conf(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            //variableName
            if(jsonObject.containsKey("variableName")){
                String variableName=jsonObject.getString("variableName");
                String key=variableName.replace("###cache_","");
                key=key.replace("###","");
                if(StringUtils.isNotBlank(key)){
                    //优先节点找
                    if(jsonObject.containsKey("client")){
                        JSONObject jsonClient=jsonObject.getJSONObject("client");
                        if(jsonClient.containsKey("confInfo")){
                            String conf_info_str=jsonClient.getString("confInfo");
                            JSONObject conf_info_json=DataTypeConversionUtil.string2Json(conf_info_str);
                            if(conf_info_json!=null && conf_info_json.containsKey(key)){
                                if (StringUtils.isNotBlank(key)){
                                    return conf_info_json.getString(key);
                                }
                            }
                        }
                    }
                    //公共配置
                    TbCdnPublicMutAttrEntity publicMutAttrEntity=  publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",key).last("limit 1"));
                    if(null!=publicMutAttrEntity){
                        String value=publicMutAttrEntity.getPvalue();
                        if(StringUtils.isNotBlank(value)){
                            return  value;
                        }
                    }
                    //默认值
                    if(null!=PublicEnum.getObjByName(key)){
                        return Objects.requireNonNull(PublicEnum.getObjByName(key)).getDefaultValue();
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "1";
    }

    private String Get_http_conf(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            //variableName ###http_default_pass_rule###
            if(jsonObject.containsKey("variableName")){
                String variableName=jsonObject.getString("variableName");
                String key=variableName.replace("###http_","");
                key=key.replace("###","");
                if(StringUtils.isNotBlank(key)){
                    PublicEnum Obj=PublicEnum.getObjByName(key);
                    if (null==Obj){
                        return "";
                    }
                    TbCdnPublicMutAttrEntity publicMutAttrEntity=  publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                            .eq("pkey",key)
                            .last("limit 1"));
                    if(null!=publicMutAttrEntity){
                        String value=publicMutAttrEntity.getPvalue();
                        if(StringUtils.isNotBlank(value)){
                            if ("bool".equals(Obj.getType())){
                                if("0".equals(value)){
                                    return  "off";
                                }else{
                                    return  "on";
                                }
                            }else {
                                return  value;
                            }
                        }
                    }
                    if(null!=PublicEnum.getObjByName(key)){
                        return Objects.requireNonNull(PublicEnum.getObjByName(key)).getDefaultValue();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public void updateGetTbIp(List<String> list, Integer control, Integer pid){
        List<CdnIpControlEntity> ls=cdnIpControlDao.selectList(new QueryWrapper<CdnIpControlEntity>().eq("parent_id",pid).eq("control",control).eq("status",1).select("id,ip"));
        for (CdnIpControlEntity entity:ls){
            if (StringUtils.isNotBlank(entity.getIp())){
                list.add(entity.getIp());
            }else {
                this.updateGetTbIp(list,control,entity.getId());
            }
        }
    }




    private String Get_http_white_ipv4(String parameter){
        StringBuilder sb=new StringBuilder();
        List<String> ipList=new ArrayList<>();
        List<String> ipCidrLIst=new ArrayList<>();
        try{
            //1 公共配置列表中的white ip
            List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
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
        return sb.toString();
    }

    private String Get_http_white_ipv6(String parameter){
        // TODO: 2022/8/29
        StringBuilder sb=new StringBuilder();
        try{
            List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey","white_ip").eq("status",1).select("pvalue"));
            for (TbCdnPublicMutAttrEntity item:list){
                if(StringUtils.isNotBlank(item.getPvalue())){
                    JSONObject object=DataTypeConversionUtil.string2Json(item.getPvalue());
                    if (object.containsKey("ip")){
                        String ip=object.getString("ip");
                        if (IPUtils.isValidIPV6(ip)){
                            sb.append(ip);
                            sb.append("\n");
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String Get_http_black_ipv4(String parameter){
        StringBuilder sb=new StringBuilder();
        try {
            List<String> ipList=new ArrayList<>();
            List<String> ipCidrLIst=new ArrayList<>();

            //1
            List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey","black_ip").eq("status",1).select("pvalue"));
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
        return  sb.toString();
    }

    private String Get_server_white_list(String parameter){
        StringBuilder sb=new StringBuilder();
        try {
            int siteId=0;
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    siteId=jsonRow.getInteger("id");
                }
            }
            List<String> ipList=new ArrayList<>();
            List<String> ipCidrLIst=new ArrayList<>();
            //add 回源IP的加入白名单
            if(true){
                List<TbSiteMutAttrEntity> sourceList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>().eq("pkey",SiteAttrEnum.SOURCE_BASE_INFO.getName()).eq("site_id",siteId).eq("status",1));
                sourceList.forEach(item->{
                    NgxSourceBaseInfoVo ngxSourceInfoVo=DataTypeConversionUtil.string2Entity(item.getPvalue(), NgxSourceBaseInfoVo.class);
                    if (null!=ngxSourceInfoVo && null!=ngxSourceInfoVo.getLine()){
                        ngxSourceInfoVo.getLine().forEach(itm2->{
                            if (StringUtils.isNotBlank(itm2.getIp())){
                                if (IPUtils.isValidIPV4(itm2.getIp())){
                                    if (!ipList.contains(itm2.getIp())){
                                        ipList.add(itm2.getIp());
                                    }
                                }
                            }
                        });
                    }
                });
            }
            //server white ip
            if (true){
                List<TbSiteMutAttrEntity> sourceList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>().eq("pkey",SiteAttrEnum.PUB_WAF_WHITE_IP.getName()).eq("site_id",siteId).eq("status",1));
                sourceList.forEach(item->{
                    NgxIpFormVo ngxIpFormVo =DataTypeConversionUtil.string2Entity(item.getPvalue(), NgxIpFormVo.class);
                    if (null!= ngxIpFormVo && null!= ngxIpFormVo.getIp()){
                        if (StringUtils.isNotBlank(ngxIpFormVo.getIp())){
                            if (IPUtils.isValidIPV4(ngxIpFormVo.getIp())){
                                if (!ipList.contains(ngxIpFormVo.getIp())){
                                    ipList.add(ngxIpFormVo.getIp());
                                }
                            }
                        }
                    }
                });
            }

            //添加masterIp
            if (!ipList.contains(StaticVariableUtils.authMasterIp)){
                ipList.add(StaticVariableUtils.authMasterIp);
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
            String ip_str=String.join("\n",ipList);
            String cidr_str=String.join("\n",ipCidrLIst);
            sb.append(ip_str);
            sb.append("\n");
            sb.append(cidr_str);
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }


    private String Get_server_black_list(String parameter){
        StringBuilder sb=new StringBuilder();
        try {
            int siteId=0;
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    siteId=jsonRow.getInteger("id");
                }
            }
            List<String> ipList=new ArrayList<>();
            List<String> ipCidrLIst=new ArrayList<>();

            //server black ip
            if (true){
                List<TbSiteMutAttrEntity> sourceList=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("pkey",SiteAttrEnum.PUB_WAF_BLACK_IP.getName())
                        .eq("site_id",siteId)
                        .eq("status",1));
                sourceList.forEach(item->{
                    NgxIpFormVo ngxIpFormVo =DataTypeConversionUtil.string2Entity(item.getPvalue(), NgxIpFormVo.class);
                    if (null!= ngxIpFormVo && null!= ngxIpFormVo.getIp()){
                        if (StringUtils.isNotBlank(ngxIpFormVo.getIp())){
                            if (IPUtils.isValidIPV4(ngxIpFormVo.getIp())){
                                if (!ipList.contains(ngxIpFormVo.getIp())){
                                    ipList.add(ngxIpFormVo.getIp());
                                }
                            }
                        }
                    }
                });
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
            String ip_str=String.join("\n",ipList);
            String cidr_str=String.join("\n",ipCidrLIst);
            sb.append(ip_str);
            sb.append("\n");
            sb.append(cidr_str);
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }


    private String Get_http_black_ipv6(String parameter){
        StringBuilder sb=new StringBuilder();
        try {
            List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey","black_ip").eq("status",1).select("pvalue"));
            for (TbCdnPublicMutAttrEntity item:list){
                if(StringUtils.isNotBlank(item.getPvalue())){
                    JSONObject object=DataTypeConversionUtil.string2Json(item.getPvalue());
                    if (object.containsKey("ip")){
                        String ip=object.getString("ip");
                        if (IPUtils.isValidIPV6(ip)){
                            sb.append(ip);
                            sb.append("\n");
                        }

                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  sb.toString();
    }

    //生成公共REG
    private String Get_http_reg(String parameter){
        StringBuffer sb=new StringBuffer();
        try{
            List<TbCdnPublicMutAttrEntity> defList=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey",PublicEnum.HTTP_DEFAULT_RULE.getName())
                    .eq("status",1)
                    .orderByDesc("weight")
            );
            for (TbCdnPublicMutAttrEntity defEntity:defList){
                List<String> stringList=this.getRexStrListV300(defEntity.getPvalue());
                stringList.forEach(item->{
                    sb.append(item);
                    sb.append("\n");
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    //生成公共rule
    private  String Get_http_rule(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            List<TbCdnPublicMutAttrEntity> def_list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey",PublicEnum.HTTP_DEFAULT_RULE.getName())
                    .eq("status",1)
                    .orderByDesc("weight"));
            List<String> stringList=new ArrayList<>(def_list.size());
            def_list.forEach(item->{
                stringList.add(item.getPvalue());
            });
            return  this.getRuleFileV300(stringList);
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    //f_host开关
    private String Get_http_f_host(String parameter){
        if ( StaticVariableUtils.exclusive_modeList.contains("f_host")){
            return "ants_f_host on;";
        }
        return "";
    }

    private String Get_master_domain(String parameter){
        if(StringUtils.isNotBlank(StaticVariableUtils.masterWebSeverName)){
            return  StaticVariableUtils.masterWebSeverName;
        }else {
            return "http://127.0.0.1";
        }
    }

    private String Get_cdn_ssl_server_ip(String parameter){
        try{
            String key=PublicEnum.SSL_SERVER.getName();
            TbCdnPublicMutAttrEntity publicMutAttrEntity=  publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",key).last("limit 1"));
            if(null!=publicMutAttrEntity){
                String value=publicMutAttrEntity.getPvalue();
                if(StringUtils.isNotBlank(value)){
                    return  value;
                }
            }
            return  PublicEnum.SSL_SERVER.getDefaultValue();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "8.8.8.8";
    }

    private String Get_ngx_conf_error_log_level(String parameter){
        try{
            final  String[] ERROR_LEVEL={"debug", "info","notice", "warn", "error", "crit"};
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("client")){
                JSONObject jsonClient=jsonObject.getJSONObject("client");
                if(jsonClient.containsKey("confInfo")){
                    String conf_info_str=jsonClient.getString("confInfo");
                    JSONObject conf_info_json=DataTypeConversionUtil.string2Json(conf_info_str);
                    if(conf_info_json.containsKey("error_log_level")){
                        String v= conf_info_json.getString("error_log_level");
                        if(Arrays.asList(ERROR_LEVEL).contains(v)){
                            return v;
                        }
                    }
                    TbCdnPublicMutAttrEntity pubEntity=publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey","error_log_level").eq("status",1).last("limit 1"));
                    if(null!=pubEntity && StringUtils.isNotBlank(pubEntity.getPvalue())){
                        if(Arrays.asList(ERROR_LEVEL).contains(pubEntity.getPvalue())){
                            return pubEntity.getPvalue();
                        }
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return PublicEnum.ERROR_LOG_LEVEL.getDefaultValue();
    }

    private String Get_sp_id_name(String parameter){
        try{
            //{"condition":"status=1",
            // "variableName":"###sp_id_name###",
            // "variableVersion":"2.0.0",
            // "client":{"effectiveStartTime":1646635542000,"createtime":1649235746000,"clientIp":"119.97.137.47","id":1,"regInfo":"{\"addtime\":\"1646635542\",\"ip\":\"119.97.137.47\",\"goods\":\"AntsCDN节点\",\"endtime\":\"1713145736\",\"version\":\"2.0.0\"}","updatetime":1649301732000,"effectiveEndingTime":1713145736000,"version":"2.0.0","confInfo":"{\"error_log\":\"error_log  logs/error.log  info;\"}","status":1},
            // "row":{"id":6},
            // "fields":"id",
            // "table":"tb_stream_proxy"}
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    return "streamProxy_"+streamProxyId;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 检测SSL证书
     * @param siteId
     * @return
     */
    private boolean existSslCrtConf(Integer siteId){
        TbSiteMutAttrEntity mutAttrKeyEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_KEY.getName())
                .eq("status",1)
                .last("limit 1")
        );
        TbSiteMutAttrEntity mutAttrPemEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteId)
                .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                .eq("status",1)
                .last("limit 1")
        );
        if(null!=mutAttrKeyEntity && null!=mutAttrPemEntity){
            return true;
        }
        return false;
    }

    private String Get_sp_mode(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    TbStreamProxyEntity streamProxy= tbStreamProxyDao.selectById(streamProxyId);
                    if(null!=streamProxy && 1==streamProxy.getStatus()){
                        String conf=streamProxy.getConfInfo();
                        JSONObject confJson=JSONObject.parseObject(conf);
                        if(confJson.containsKey("server_mode")){
                            String sp_mode=confJson.getString("server_mode");
                            sp_mode=sp_mode.trim();
                            if(sp_mode.length()>0){
                                return sp_mode+";";
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_sp_server(String parameter){
        StringBuilder sb=new StringBuilder();
        try{

            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    TbStreamProxyEntity streamProxy= tbStreamProxyDao.selectById(streamProxyId);
                    if(null!=streamProxy && 1==streamProxy.getStatus()){
                        String conf=streamProxy.getConfInfo();
                        JSONObject confJson=JSONObject.parseObject(conf);
                        if(confJson.containsKey("server") && confJson.containsKey("server_mode")){
                            String server_mode=confJson.getString("server_mode");
                            switch (server_mode){
                                case "weight":
                                    if (true){
                                        //{"listen":"12345","server_mode":" ", "proxy_timeout":"30s","proxy_connect_timeout":"60s","server":["121.62.18.146:22 weight=NaN"]}
                                        JSONArray serverArray=confJson.getJSONArray("server");
                                        String pattern = "\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+\\sweight=\\d+";
                                        Pattern r = Pattern.compile(pattern);
                                        for (int i = 0; i <serverArray.size() ; i++) {
                                            String s=serverArray.getString(i);
                                            Matcher m = r.matcher(s);
                                            if(m.matches()){
                                                sb.append(String.format("server %s;\n    ",s));
                                            }else{
                                                logger.error("["+s+"] 格式有误！");
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case "hash":
                                    if(true){
                                        sb.append("hash $remote_addr consistent;\n");
                                        JSONArray serverArray=confJson.getJSONArray("server");
                                        String pattern = "\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+";
                                        Pattern r = Pattern.compile(pattern);
                                        for (int i = 0; i <serverArray.size() ; i++) {
                                            String s=serverArray.getString(i);
                                            Matcher m = r.matcher(s);
                                            if(m.matches()){
                                                sb.append(String.format("    server %s;\n",s));
                                            }else{
                                                logger.error("["+s+"] 格式有误！");
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case "polling":
                                    if (true){
                                        JSONArray serverArray=confJson.getJSONArray("server");
                                        String pattern = "\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+";
                                        Pattern r = Pattern.compile(pattern);
                                        for (int i = 0; i <serverArray.size() ; i++) {
                                            String s=serverArray.getString(i);
                                            Matcher m = r.matcher(s);
                                            if(m.matches()){
                                                sb.append(String.format("    server %s;\n",s));
                                            }else{
                                                logger.error("["+s+"] 格式有误！");
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                default:
                                    logger.error("unknown server_mode["+server_mode+"]");
                                    break;
                            }
                            //"proxy_timeout":"30s","proxy_connect_timeout":"60s",
                            sb.append("\n");
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }



    private String Get_sp_listen(String parameter){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    TbStreamProxyEntity streamProxy= tbStreamProxyDao.selectById(streamProxyId);
                    if(null!=streamProxy && 1==streamProxy.getStatus()){
                        String conf=streamProxy.getConfInfo();
                        StreamInfoVo vo=DataTypeConversionUtil.string2Entity(conf,StreamInfoVo.class);
                        if (null!=vo){
                            sb.append("\n");
                           for (String listen:vo.getListen().split("\\|")){
                               sb.append( "    listen "+listen +";\n");
                           }

                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String Get_sp_proxy_protocol(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    TbStreamProxyEntity streamProxy= tbStreamProxyDao.selectById(streamProxyId);
                    if(null!=streamProxy && 1==streamProxy.getStatus()){
                        String conf=streamProxy.getConfInfo();
                        StreamInfoVo streamInfoVo=DataTypeConversionUtil.string2Entity(streamProxy.getConfInfo(), StreamInfoVo.class);
                        if(null!=streamInfoVo && 1==streamInfoVo.getProxy_protocol()){
                            //return "proxy_protocol on;";
                            //2023 4 11 使用V2
                            return "proxy_protocol v2;";
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_sp_timeout(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    TbStreamProxyEntity streamProxy= tbStreamProxyDao.selectById(streamProxyId);
                    if(null!=streamProxy && 1==streamProxy.getStatus()){
                        String conf=streamProxy.getConfInfo();
                        JSONObject confJson=JSONObject.parseObject(conf);
                        if(confJson.containsKey("proxy_timeout")){
                            return confJson.getString("proxy_timeout");
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "60";
    }

    private String Get_sp_connect_timeout(String parameter){
        try{
            JSONObject jsonObject=JSONObject.parseObject(parameter);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer streamProxyId=jsonRow.getInteger("id");
                    TbStreamProxyEntity streamProxy= tbStreamProxyDao.selectById(streamProxyId);
                    if(null!=streamProxy && 1==streamProxy.getStatus()){
                        String conf=streamProxy.getConfInfo();
                        JSONObject confJson=JSONObject.parseObject(conf);
                        if(confJson.containsKey("proxy_connect_timeout")){
                            return confJson.getString("proxy_connect_timeout");
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "60";
    }


    /**获取 SSL 列表
     * @param param
     * @return
     */
    private List<Map> Get_ssl_ids(String param){
        List<Map> resultList=new ArrayList<>();
        try{
            JSONObject paraJson=DataTypeConversionUtil.string2Json(param);
            String path_model="";
            String variableName="";
            //           String ClientVersion="";
            if(paraJson.containsKey("path_model")){
                path_model=paraJson.getString("path_model");
            }
            if(paraJson.containsKey("variableName")){
                variableName=paraJson.getString("variableName");
            }
            //           if(paraJson.containsKey("ClientVersion")){
            //               ClientVersion=paraJson.getString("ClientVersion");
            //           }
            String t_pkey="";
            if(path_model.contains(".key")){
                t_pkey=SiteAttrEnum.SSL_OTHER_CERT_KEY.getName();
            }else if(path_model.contains(".crt")){
                t_pkey=SiteAttrEnum.SSL_OTHER_CERT_PEM.getName();
            }
            List<TbSiteEntity>siteList=tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>().eq("status",1)   );
            for (TbSiteEntity site:siteList){
                TbSiteMutAttrEntity mutAttr=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                        .eq("site_id",site.getId())
                        .eq("pkey",t_pkey)
                        .eq("status",1)
                        .orderByDesc("id")
                        .last("limit 1")
                ) ;
                if (null==mutAttr){
                    Map map=new HashMap();
                    mutAttr=new TbSiteMutAttrEntity();
                    map.put("path",path_model.replace(variableName,site.getId()+""));
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("row",mutAttr);
                    map.put("parameter",jsonObject.toJSONString());
                    resultList.add(map);
                }else{
                    Map map=new HashMap();
                    map.put("path",path_model.replace(variableName,mutAttr.getSiteId()+""));
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("row",mutAttr);
                    map.put("parameter",jsonObject.toJSONString());
                    resultList.add(map);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return resultList;
    }

    private String Get_ssl_key(String param){
        try{
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(param);
            if(jsonObject.containsKey("row")){
                JSONObject rowJson=jsonObject.getJSONObject("row");
                TbSiteMutAttrEntity mutAttr=DataTypeConversionUtil.json2entity(rowJson, TbSiteMutAttrEntity.class);
                if (null==mutAttr){
                    return "";
                }
                JSONObject valueJson=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                if (null==valueJson){
                    return "";
                }
                if(valueJson.containsKey("value")){
                    return valueJson.getString("value");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }


    //已停用
    private String Get_referer_chunk_v1(String param){

        try{
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(param);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteMutAttrEntity mutAttr=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey","anti_theft_chain")
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=mutAttr){
                        if(StringUtils.isNotBlank(mutAttr.getPvalue())){
                            String model="set $referers_flag 0;\n" +
                                    "    valid_referers {no_blocked_referers} {match_domains};\n" +
                                    "    if ($invalid_referer) {\n" +
                                    "       set $referers_flag \"${referers_flag}1\";\n" +
                                    "    }       \n" +
                                    "    if ( $uri ~ '{match_uri}' ){\n" +
                                    "       set $referers_flag \"${referers_flag}2\";\n" +
                                    "    }\n" +
                                    "    if ( $referers_flag = \"{match_mode}\" ){\n" +
                                    "        return {return_code};    \n" +
                                    "    }";
                            String no_blocked_referers="";
                            String match_domains="";
                            String match_uri="";
                            String match_mode="012";
                            String return_code="444";
                            //Integer match_uri_type=1;//1 后缀 ；2 路径
                            JSONObject object=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                            if(object.containsKey("no_blocked_referers") && 0==object.getInteger("no_blocked_referers")){
                                no_blocked_referers="none  blocked";
                            }
                            if(object.containsKey("match_domains")  ){
                                match_domains=object.getString("match_domains");
                                StringBuilder sbDomain=new StringBuilder();
                                String[] mds=match_domains.split(" ");
                                for (String m:mds){
                                    if (StringUtils.isNotBlank(m)){
                                        if (DOMAIN_F_PATTERN.matcher(m).matches()){
                                            sbDomain.append(" '"+m+"' ");
                                        }
                                    }
                                }
                                match_domains=sbDomain.toString();
                            }
                            if(object.containsKey("match_uri") && object.containsKey("match_uri_type") ){
                                if(1==object.getInteger("match_uri_type")){
                                    match_uri="\\.("+object.getString("match_uri")+")$";
                                }else if(2==object.getInteger("match_uri_type")){
                                    match_uri="("+object.getString("match_uri")+")";
                                }
                            }
                            if(object.containsKey("match_mode")  ){
                                if(0==object.getInteger("match_mode")){
                                    //黑名单
                                    match_mode="02";
                                }else if(1==object.getInteger("match_mode")){
                                    //白名单
                                    match_mode="012";
                                }
                            }
                            if(object.containsKey("return_code") ){
                                return_code=object.getInteger("return_code").toString();
                            }
                            model=model.replace("{no_blocked_referers}",no_blocked_referers);
                            model=model.replace("{match_domains}",match_domains);
                            model=model.replace("{match_uri}",match_uri);
                            model=model.replace("{match_mode}",match_mode);
                            model=model.replace("{return_code}",return_code);
                            return model;
                        }

                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_referer_chunk(String param){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(param);
            if(!jsonObject.containsKey("row")){
                return sb.toString();
            }
            JSONObject jsonRow=jsonObject.getJSONObject("row");
            if(!jsonRow.containsKey("id")){
                return sb.toString();
            }
            Integer siteId=jsonRow.getInteger("id");
            TbSiteMutAttrEntity mutAttr=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("site_id",siteId)
                    .eq("pkey","anti_theft_chain")
                    .eq("status",1)
                    .last("limit 1"));
            if(null==mutAttr){
                return sb.toString();
            }
            if(StringUtils.isBlank(mutAttr.getPvalue())){
                return sb.toString();
            }
            String noBlockedReferers="";
            String match_domains="";
            String match_uri="";
            String match_mode="012";
            String return_code="444";
            //Integer match_uri_type=1;//1 后缀 ；2 路径
            JSONObject object=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
            if(object.containsKey("no_blocked_referers") && 0==object.getInteger("no_blocked_referers")){
                noBlockedReferers="none  blocked";
            }
            //检测域名匹配
            if(object.containsKey("match_domains")  ){
                match_domains=object.getString("match_domains");
                StringBuilder sbDomain=new StringBuilder();
                String[] mds=match_domains.split(" ");
                for (String m:mds){
                    if (StringUtils.isNotBlank(m)){
                        if (DOMAIN_F_PATTERN.matcher(m).matches()){
                            sbDomain.append(" '"+m+"' ");
                        }
                    }
                }
                match_domains=sbDomain.toString();
            }
            //uri
            if(object.containsKey("match_uri") && object.containsKey("match_uri_type") ){
                if(1==object.getInteger("match_uri_type")){
                    match_uri="\\.("+object.getString("match_uri")+")$";
                }else if(2==object.getInteger("match_uri_type")){
                    match_uri="("+object.getString("match_uri")+")";
                }else if (0==object.getInteger("match_uri_type")){
                    match_uri=".*";
                }
            }

            if(object.containsKey("match_mode")  ){
                if(0==object.getInteger("match_mode")){
                    //黑名单
                    match_mode="02";
                }else if(1==object.getInteger("match_mode")){
                    //白名单
                    match_mode="012";
                }
            }

            if(object.containsKey("return_code") ){
                return_code=object.getInteger("return_code").toString();
            }
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

    private String Get_geoip_country_rule_chunk(String param){
        try{
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(param);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    TbSiteMutAttrEntity mutAttr=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey","area_shielding")
                            .eq("status",1)
                            .last("limit 1"));
                    if(null!=mutAttr){
                        if(StringUtils.isNotBlank(mutAttr.getPvalue())){
                            String model= "if ($geoip_country_code {match_mode}  ({match_area}) )  \n" +
                                    "    {\n" +
                                    "        return {return_code};\n" +
                                    "    }";
                            String match_area="";
                            String match_mode="~*";
                            String return_code="444";
                            JSONObject object=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                            if(object.containsKey("match_area")){
                                match_area=object.getString("match_area");
                            }
                            if(object.containsKey("return_code")){
                                return_code=object.getInteger("return_code").toString();
                            }
                            if(object.containsKey("match_mode")){
                                if(0==object.getInteger("match_mode")){
                                    //黑名单方式
                                    match_mode="~*";
                                }else if(1==object.getInteger("match_mode")){
                                    //白名单方式
                                    match_mode="!~*";
                                }
                            }
                            model=model.replace("{match_area}",match_area);
                            model=model.replace("{return_code}",return_code);
                            model=model.replace("{match_mode}",match_mode);
                            return model;
                        }

                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_ssl_pem(String param){
        try{
            JSONObject jsonObject=DataTypeConversionUtil.string2Json(param);
            if(jsonObject.containsKey("row")){
                JSONObject rowJson=jsonObject.getJSONObject("row");
                TbSiteMutAttrEntity mutAttr=DataTypeConversionUtil.json2entity(rowJson, TbSiteMutAttrEntity.class);
                if (null==mutAttr){
                    return "";
                }
                JSONObject valueJson=DataTypeConversionUtil.string2Json(mutAttr.getPvalue());
                if (null==valueJson){
                    return "";
                }
                if(valueJson.containsKey("value")){
                    return valueJson.getString("value");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String Get_rewrite_crt(String param){
        if ( !StaticVariableUtils.exclusive_modeList.contains("rewrite")){
            return "";
        }
        try {
            JSONObject paramObj=DataTypeConversionUtil.string2Json(param);
            TbRewriteEntity rewriteEntity=null;
            CdnClientEntity client=null;
            if (paramObj.containsKey("row")){
                rewriteEntity=DataTypeConversionUtil.json2entity(paramObj.getJSONObject("row"), TbRewriteEntity.class);
            }
            if (paramObj.containsKey("client")){
                client=DataTypeConversionUtil.json2entity(paramObj.getJSONObject("client"), CdnClientEntity.class);
            }
            if (null==rewriteEntity ||null==client){
                logger.error("[Get_rewrite_conf]obj is null");
                return "";
            }
            return rewriteEntity.getCertStr();
        }catch (Exception e){
            e.printStackTrace();
        }
        return  "";
    }

    private String Get_rewrite_key(String param){
        if ( !StaticVariableUtils.exclusive_modeList.contains("rewrite")){
            return "";
        }

        try {
            JSONObject paramObj=DataTypeConversionUtil.string2Json(param);
            TbRewriteEntity rewriteEntity=null;
            CdnClientEntity client=null;
            if (paramObj.containsKey("row")){
                rewriteEntity=DataTypeConversionUtil.json2entity(paramObj.getJSONObject("row"), TbRewriteEntity.class);
            }
            if (paramObj.containsKey("client")){
                client=DataTypeConversionUtil.json2entity(paramObj.getJSONObject("client"), CdnClientEntity.class);
            }
            if (null==rewriteEntity ||null==client){
                logger.error("[Get_rewrite_conf]obj is null");
                return "";
            }
            return rewriteEntity.getKeyStr();
        }catch (Exception e){
            e.printStackTrace();
        }
        return  "";
    }

    private String insertCertConf( TbRewriteEntity rewriteEntity,String cert,String key){
        StringBuilder sb=new StringBuilder();
        sb.append("    listen 443 ssl  ;\n");
        sb.append(String.format("    ssl_certificate      conf/rewrite/%d_%s_.crt;\n",rewriteEntity.getId(),rewriteEntity.getServerName()));
        sb.append(String.format("    ssl_certificate_key  conf/rewrite/%d_%s_.key;\n",rewriteEntity.getId(),rewriteEntity.getServerName()));
        sb.append(
                "    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;\n" +
                "    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4:!DH:!DHE;\n" +
                "    ssl_prefer_server_ciphers on;\n" +
                "    ssl_session_cache shared:SSL:10m;\n");
        return sb.toString();
    }




    /**
     * 生成转发的配置
     * @param param
     * @return
     */
    private String Get_rewrite_conf(String param){
        if ( !StaticVariableUtils.exclusive_modeList.contains("rewrite")){
            return "";
        }
        StringBuilder sb=new StringBuilder();
        try {
            //{"condition":"status=1","variableName":"###rewrite_chunk###","variableVersion":"2.13",
            // "client":{"area":"1","createtime":1660466058000,"line":"中国移动","stableScore":10158,"remark":"1","regInfo":"{\"addtime\":\"1660466070\",\"ip\":\"119.97.137.47\",\"goods\":\"cdn节点\",\"endtime\":\"1670919285\",\"version\":\"2.13\"}","version":"2.13","confInfo":"{\"id\":84,\"proxy_max_temp_file_size\":\"1024m\",\"worker_processes\":\"auto\",\"proxy_cache_path_dir\":\"/data/cache\",\"error_log_level\":\"info\"}","effectiveStartTime":1660466070000,"clientType":1,"checkTime":1663914472,"clientIp":"119.97.137.47","id":84,"updatetime":1663914472000,"effectiveEndingTime":1670919285000,"status":1},
            // "row":{"server_name":"string","scheme":"$scheme","user_id":"88","create_tmp":1663898961000,"alias":",a,b,","serial_number":"string","remark":"string","id":5,"rewrite_mode":0,"target":"1","status":1},
            // "fields":"id,server_name","table":"tb_rewrite"}
            JSONObject paramObj=DataTypeConversionUtil.string2Json(param);
            TbRewriteEntity rewriteEntity=null;
            CdnClientEntity client=null;
            if (paramObj.containsKey("row")){
                rewriteEntity=DataTypeConversionUtil.json2entity(paramObj.getJSONObject("row"), TbRewriteEntity.class);
            }
            if (paramObj.containsKey("client")){
                client=DataTypeConversionUtil.json2entity(paramObj.getJSONObject("client"), CdnClientEntity.class);
            }
            if (null==rewriteEntity ||null==client){
                logger.error("[Get_rewrite_conf]obj is null");
                return sb.toString();
            }
            //            String out=String.format("server {\n" +
            //                    "    server_name  'f.test.abc.com' 'g.test.abc.com'; \n" +
            //                    "    return 301 http://waf.antsxdp$request_uri;\n" +
            //                    "}");
            StringBuilder alias=new StringBuilder();
            for (String as:rewriteEntity.getAlias().split(",")){
                as=as.trim();
                if (StringUtils.isNotBlank(as)){
                    alias.append(" '").append(as).append("' ");
                }
            }
            if (1==rewriteEntity.getRewriteType() || null==rewriteEntity.getRewriteType()){
                // 30xmode
                String followMode="";
                if ("follow".equals(rewriteEntity.getFollowMode())){
                    followMode="$request_uri";
                }
                sb.append("server {\n");
                sb.append("    listen 80  ;\n");
                if (null!=rewriteEntity.getNotAfter() ){
                  sb.append(this.insertCertConf(rewriteEntity,rewriteEntity.getCertStr(),rewriteEntity.getKeyStr()));
                }
                sb.append(String.format("    server_name  '%s' %s; \n",rewriteEntity.getServerName().trim(), alias));
                sb.append(String.format("    return %d %s://%s%s;\n",rewriteEntity.getRewriteMode(),rewriteEntity.getScheme(),rewriteEntity.getTarget().trim(),followMode));
                sb.append(this.Get_acme_sh_account_thumbprint_chunk(param));
                sb.append("}");
            }else if (2==rewriteEntity.getRewriteType()){
                // 处理content
                String content=rewriteEntity.getJsContent();
                content=content.replaceAll("\r|\n"," ");
                content=content.replaceAll("\"","\'");
                sb.append("server {\n");
                sb.append("    listen 80  ;\n");
                if (null!=rewriteEntity.getNotAfter() ){
                    sb.append(this.insertCertConf(rewriteEntity,rewriteEntity.getCertStr(),rewriteEntity.getKeyStr()));
                }
                sb.append(String.format("    server_name  '%s' %s; \n",rewriteEntity.getServerName().trim(), alias));
                sb.append("    gzip on;\n");
                sb.append("    gzip_http_version  1.1;\n");
                sb.append("    gzip_min_length 0;\n");
                sb.append("    gzip_vary on;\n");
                sb.append("    gzip_proxied  any;\n");
                sb.append("    add_header Access-Control-Allow-Origin $http_origin;\n");
                sb.append("    location / { \r");
                sb.append("            more_set_input_headers 'Accept-Encoding: gzip,deflate'; \n");
                sb.append("            rewrite '.*' '/gjs/index.html' redirect ;  \n");
                sb.append("    }\n");
                sb.append("    location /gjs{   \n");
                sb.append("          return 200 \""+content+"\";\n");
                sb.append("    } \n");
                sb.append(this.Get_acme_sh_account_thumbprint_chunk(param));
                sb.append("}");
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return  sb.toString();
    }



    private String Get_site_rewrite_uri(String param){
        StringBuilder sb=new StringBuilder();
        try{
            JSONObject jsonObject=JSONObject.parseObject(param);
            if(jsonObject.containsKey("row")){
                JSONObject jsonRow=jsonObject.getJSONObject("row");
                if(jsonRow.containsKey("id")){
                    Integer siteId=jsonRow.getInteger("id");
                    List<TbSiteMutAttrEntity> list1=tbSiteMutAttrDao.selectList(new QueryWrapper<TbSiteMutAttrEntity>()
                            .eq("site_id",siteId)
                            .eq("pkey", SiteAttrEnum.CONTENT_SECURITY_REWRITE.getName())
                            .eq("status",1));
                    for (TbSiteMutAttrEntity mutAttr:list1){
                        NgxSiteUriRewriteVo sVo=DataTypeConversionUtil.string2Entity(mutAttr.getPvalue(), NgxSiteUriRewriteVo.class);
                        if (null==sVo){
                            continue;
                        }
                        if (StringUtils.isBlank(sVo.getPath()) || StringUtils.isBlank(sVo.getRewritePath())){
                            continue;
                        }
                        //permanent redirect    last
                        //if ($uri = /path/to/file) {
                        //  rewrite ^ http://$server_name$request_uri redirect;
                        //}
                        sb.append("\n");
                        sb.append( String.format("    if ( $request_uri = '%s' ) {\n",sVo.getPath()));
                        sb.append(String.format("         rewrite ^ '%s' %s;\n",sVo.getRewritePath(),sVo.getRewriteMode()));
                        sb.append("    }\n");
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    public List<String> nftDrop3LongCc(int v4v6){
        List<String> tb_ips=new ArrayList<>();
        updateGetTbIp(tb_ips,IpControlEnum.FORBID_3_LONG.getId(), 0);
        List<String> ipv4Ls=new ArrayList<>();
        List<String> ipv6Ls=new ArrayList<>();
        for (String ip:tb_ips){
            if(IPUtils.isValidIPV4(ip) || IPUtils.isCidr(ip)){
               ipv4Ls.add(ip);
            }else if(IPUtils.isValidIPV6(ip)){
              ipv6Ls.add(ip);
            }
        }
       if (4==v4v6){
          ipv4Ls.add("1.1.1.1");
          return   ipv4Ls ;
       }else if (6==v4v6){
          ipv6Ls.add("::100");
          return ipv6Ls;
       }
       return null;
    }

    public List<String> nftNetWaf(){
        List<String> result=new ArrayList<>();
        List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey",PublicEnum.NET_WAF.getName())
                .eq("status",1)
                .orderByDesc("weight")
        );
        /*
        *
            ip saddr 127.0.0.1 accept
            ip6 saddr ::1 accept
            ip saddr 127.0.0.1 udp dport 8080 accept
            ip saddr 127.0.0.1 udp dport 8080 drop
            {"ip":"127.0.0.1","port":"8080","protocol":"all","rule":"accept","remark":"1"}
		* */
        List<String> acceptList=new ArrayList<>();
        List<String> dropList=new ArrayList<>();
        for (TbCdnPublicMutAttrEntity nftObj:list){
            NetNftVo  netVo=DataTypeConversionUtil.string2Entity(nftObj.getPvalue(), NetNftVo.class);
            if (null==netVo){
                continue;
            }
            if (StringUtils.isBlank(netVo.getIp()) || StringUtils.isBlank(netVo.getRule())){
                logger.error("nft obj found error ");
                continue;
            }
            String ipType="ip";
            String protocol="";
            if (IPUtils.isValidIPV6(netVo.getIp())){
                ipType="ip6";
            }
            if ("tcp".equals(netVo.getProtocol())||"udp".equals(netVo.getProtocol())){
                protocol=netVo.getProtocol();
                String v=String.format("%s saddr %s %s dport %s %s",ipType,netVo.getIp(),netVo.getProtocol(),netVo.getPort(),netVo.getRule());
                if ("accept".equals(netVo.getRule())){
                    acceptList.add(v);
                }else{
                    dropList.add(v);
                }
            }else if("all".equals(netVo.getProtocol())){
                String v1=String.format("%s saddr  %s tcp dport %s %s",ipType,netVo.getIp(), netVo.getPort(),netVo.getRule());
                String v2=String.format("%s saddr  %s udp dport %s %s",ipType,netVo.getIp(), netVo.getPort(),netVo.getRule());
                if ("accept".equals(netVo.getRule())){
                    acceptList.add(v1);
                    acceptList.add(v2);
                }else{
                    dropList.add(v1);
                    dropList.add(v2);
                }
            }

            result.addAll(acceptList);
            result.addAll(dropList);
        }
        return  result;
    }



    private String Get_nft_data(String param){
        //{"variableName":"###nft_long_cc###","variableVersion":"2.17","client":{"area":"1","createtime":1660466058000,"line":"中国移动","stableScore":48627,"remark":"1","regInfo":"{\"addtime\":\"1660466070\",\"ip\":\"119.97.137.47\",\"goods\":\"cdn节点\",\"endtime\":\"1862064000\",\"version\":\"2.17\"}","version":"2.17","confInfo":"{\"id\":84,\"proxy_cache_dir_max_size\":\"100G\",\"proxy_max_temp_file_size\":\"1024m\",\"proxy_cache_path_zone\":\"1000m\",\"worker_processes\":\"auto\",\"proxy_cache_path_dir\":\"/data/cache\",\"error_log_level\":\"error\"}","effectiveStartTime":1660466070000,"clientType":1,"checkTime":1666234207,"clientIp":"119.97.137.47","id":84,"updatetime":1666234207000,"effectiveEndingTime":1862064000000,"status":1}}
        StringBuilder sb=new StringBuilder();
        try {
            GetTextParamVo paramVo=DataTypeConversionUtil.string2Entity(param, GetTextParamVo.class);
            if (null==paramVo){
                return sb.toString();
            }
            switch (paramVo.getVariableName()){
                case "###nft_long_cc###":
                     List<String> listv4= this.nftDrop3LongCc(4);
                     if (null!=listv4){
                         sb.append(String.join(",\n",listv4));
                     }
                    break;
                case "###nft_long_cc_v6###":
                    List<String> listv6= this.nftDrop3LongCc(6);
                    if (null!=listv6){
                        sb.append(String.join(",\n",listv6));
                    }
                    break;
                case "###nft_net_white###":
                    //主控 节点
                    List<String> allWhite=new ArrayList<>();
                    allWhite.add("127.0.0.1");
                    allWhite.add(StaticVariableUtils.authMasterIp);
                    for (CdnClientEntity client:this.getClientList()){
                        allWhite.add(client.getClientIp());
                    }
                    sb.append(String.join(",\n",allWhite));
                    break;
                case "###nft_pub_net_waf###":
                    sb.append(String.join("\n",this.nftNetWaf()));
                    break;
                default:
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static boolean hasMethod(Class<?> clazz, String methodName) {
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
     * @param param
     * @return
     */
    public String invokeMethodInFile(String method, String param){
        try{
            //获取类实例
            Class<?>  cls =getClass();
           if (!hasMethod(cls,method)){
               logger.error("error,method:["+method+"],param:"+param);
               return "";
           }
            //获取执行方法
            Method m = cls.getDeclaredMethod(method,String.class);
            return  m.invoke(this,param).toString();
        }
        catch (Exception e){
            logger.error("error,method:["+method+"],param:"+param);
            e.printStackTrace();
        }
        return  "";
    }

    /**
     * @param method
     * @param param
     * @return
     */
    public  List<Map> invokeMethodInFileName(String method, String param){
        try{
            //获取类实例
            Class<?>  cls = getClass();
            if (!hasMethod(cls,method)){
                logger.error("error,method:["+method+"],param:"+param);
                return new ArrayList<>(0);
            }
            //获取执行方法
            Method m = cls.getDeclaredMethod(method,String.class);
            return  (List<Map>)m.invoke(this,param);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return  new ArrayList<>(0);
    }

    //    private Object pushByInfov1(Map<String, String> map){
//        List<Object > result=new ArrayList<>();
//        for(Map.Entry entry:map.entrySet()){
//            String key=entry.getKey().toString();
//            String value=entry.getValue().toString();
//            switch (key){
//                case "all_file":
//                    operationPublicNetWaf();
//                    this.makeAllFile(null);
//                    break;
//                case "all_file_2_node":
//                   this.makeAllFile(Integer.valueOf(value));
//                    break;
//                case "public_chunk":
//                    pushPublicToAllNode();
//                    pushAllPubRegRuleToNode();
//                    break;
//                case "nginx_conf":
//                    this.makeFiles2NodeHandle(null, NGX_CONF_PATH);
//                    break;
//                case "http_conf":
//                    this.makeFiles2NodeHandle(null, HTTP_CONF_PATH);
//                    break;
//                case "cache_conf":
//                    this.makeFiles2NodeHandle(null, CACHE_CONF_PATH);
//                    break;
//                case "nft_waf":
//                    operationPublicNetWaf();
//                    break;
//                case "pub_waf_select":
//                    pushAllPubRegRuleToNode();
//                    break;
//                case "push_etc_vhost_conf":
//                    this.makeFiles2NodeHandle(null, VHOST_PATH_MODE);
//                    break;
//                case "site_select_chunk":
//                    //推送多个站
//                    pushSelectSiteConfToNode(value);
//                    break;
//                case "site_chunk":
//                    pushSiteConfToNode(null,Integer.parseInt(value));
//                    break;
//                case "site_ssl":
//                    pushSiteSslToNodeHandle(Integer.parseInt(value));
//                    break;
//                case "site_waf":
//                    pushSiteWafToNodeHandle(Integer.parseInt(value));
//                    break;
//                case "site_conf":
//                    makeSiteFileToNodeHandle(Integer.parseInt(value));
//                    break;
//                case "command":
//                    pushCommandToNodes(null,Integer.parseInt(value));
//                    break;
//                case "delete_file":
//                    deleteNodeDir(value);
//                    break;
//                case "all_task":
//                    startOperaTask();
//                    break;
//                case "init_node":
//                    Object initInt= pushInitNodeHandle(Integer.parseInt(value));
//                    result.add(initInt);
//                    break;
//                case "ip_trie":
//                    pushIpTrieToAllNode("1,2");
//                    break;
//                case "ip_trie_and_nft":
//                    pushIpTrieToAllNode("1,2,3,4");
//                    break;
//                case "site_html_file":
//                    pushHtmlFile(Integer.parseInt(value));
//                    break;
//                case "stream_conf":
//                    pushStreamToNode(Integer.parseInt(value));
//                    break;
//                case "del_stream_conf":
//                    batDelProxy(null,value);
//                    break;
//                case "init_del_dir":
//                    if(isIntegerOrIntegerList(value)){
//                        pushCommandToNodes(value,CommandEnum.INIT_DEL_DIR_COMMAND.getId());
//                    }
//                    break;
//                case "delete_site":
//                    if(isIntegerOrIntegerList(value)){
//                        TbSiteEntity site=tbSiteDao.selectById(Integer.parseInt(value));
//                        if (null!=site){
//                            this.deleteSiteFileToNodeHandle(site);
//                        }
//                    }
//                    break;
//                case "apply_certificate":
//                    pushApplyCertificate(Integer.parseInt(value));
//                    break;
//                case "apply_certificate_v2":
//                    pushApplyCertificateBySiteId(Integer.parseInt(value));
//                    break;
//                case "initAllNode":
//                    pushInitAllNode();
//                    break;
//                case "clean_short_cc":
//                    //:NODEIP:HOSTNAME:ATTACKiP:OP_TYPE:*:*
//                    cleanShortCcByKey(value);
//                    break;
//                case "close_suit_service":
//                    close_suit_service_handle(value);
//                    break;
//                case "push_suit_service":
//                    push_suit_service_handle(value);
//                    break;
//                case "push_rewrite_conf":
//                    pushRewriteToNode(Integer.parseInt(value));
//                    break;
//                case "delete_rewrite_conf":
//                    deleteRewrite(Integer.parseInt(value));
//                    break;
//                case "create_dir":
//                    createDirToNode(value);
//                    break;
//                case "shell_ants_cmd_1":
//                    Object c1= JavaShellCmd(1,value);
//                    result.add(c1);
//                    break;
//                case "shell_ants_cmd_2":
//                    Object c2= JavaShellCmd(2,value);
//                    result.add(c2);
//                    break;
//                case "http_default_waf":
//                    pushDefault80Waf();
//                    break;
//                case "push_file_by_model":
//                    this.makeFiles2NodeHandle(null, value);
//                    break;
//                default:
//                    logger.error(" unknown op ["+key+"]");
//                    break;
//            }
//        }
//        return result;
//    }


     /*
    private int pushInitNodeHandleV2_1(){
        int ret=0;
        String fileSetKey=LOC_PATH_SET;
        if (StaticVariableUtils.nodeFileSet.containsKey(fileSetKey)){
            List<String> stringList=StaticVariableUtils.nodeFileSet.get(fileSetKey);
            if (null!=stringList){
                String redisKey="allfile-path";
                rdsDeleteKey(redisKey);
                for (String path:stringList){
                    rdsSadd(redisKey,path);
                    ret++;
                }
            }
        }
        return  ret;
    }
     */
}
