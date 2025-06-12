package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.ants.common.exception.RRException;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.app.vo.AcmeDnsVo;
import io.ants.modules.app.vo.ApplyCertVo;
import io.ants.modules.app.vo.ZeroSslAPiCreateCertForm;
import io.ants.modules.app.vo.ZeroSslApiCertInfoVo;
import io.ants.modules.sys.controller.CdnSiteController;

import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.*;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.BatReissuedForm;
import io.ants.modules.sys.form.CertCallbackForm;
import io.ants.modules.sys.form.CertServerApplyForm;
import io.ants.modules.sys.makefile.MakeFileFromResService;
import io.ants.modules.sys.makefile.MakeFileMethod;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.DnsCApiService;
import io.ants.modules.sys.vo.*;
import io.ants.modules.utils.config.ZeroSslConfig;
import io.ants.modules.utils.factory.ZeroSslFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.ants.modules.sys.enums.PushTypeEnum.*;


@Service
public class CdnMakeFileImpl implements CdnMakeFileService {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    private static  final String SET_SUFFIX=":file-path";
    private static final String  GROUP_SET="allfile-path:";
    //private static final  String LOC_PATH_SET="_fileSet";
    private static final  long EXPIRE_TIME =3600*6;


    //"add";//add-all//add-nocheck//add-nft
    //向所有IP推送
    //private static final String RDS_ADD_MODE_ADD="add";
    //private static final String RDS_ADD_MODE_ADD_NOCHECK="add-nocheck";
    //向所有IP推送
    //private static final String RDS_ADD_MODE_ADD_ALL="add-all";
    //向节点IP推送
    //private static final String RDS_ADD_MODE_ADD_NODE="add-node";
    //private static final String RDS_ADD_MODE_ADD_NFT="add-nft";
    private static final String RDS_DEL_MODE_DEL="del";
    //private static final String RDS_DEL_MODE_CLEAN_CACHE="clean-cache";



    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()  .setNameFormat("ants_make_file_serer-pool-%d").build();
    private static ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());




    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private TbStreamProxyAttrDao tbStreamProxyAttrDao;
    @Autowired
    private  TbCertifyDao tbcertifyDao;
    @Autowired
    private CdnProductDao cdnProductDao;
    @Autowired
    private TbRewriteDao tbRewriteDao;
    @Autowired
    private MakeFileMethod methodHandleTool;
    @Autowired
    private ServerProperties serverProperties;
    @Autowired
    private MakeFileFromResService makeFileFromResService;
    @Autowired
    private SysLogDao sysLogDao;
    @Autowired
    private TbCdnPublicMutAttrDao publicMutAttrDao;
    @Autowired
    private CdnClientAreaDao cdnClientAreaDao;
    @Autowired
    private DnsCApiService dnsCApiService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TbSiteGroupDao tbSiteGroupDao;

    private static final Pattern NUMBER_PATTERN= Pattern.compile("^[-+]?[\\d,]*$");

    private  boolean isIntegerOrIntegerList(String str) {
        return NUMBER_PATTERN.matcher(str).matches();
    }



    private void rdsInsertGroupSet(String groupIdS,String content){
        redisUtils.setUnionAndStore(content+SET_SUFFIX,GROUP_SET+groupIdS);
    }

    private String rdsXAddConfigByGroupIds(String key, String groupIdS, String content){
        //public::config-stream
        //add:0
        //site_id_set
        // headKey+RedisStreamType.CONFIG.getName();
        String streamKey=String.format("%s%s","public", RedisStreamType.CONFIG.getName()) ;
        String fKey=String.format("%s:%s",key,groupIdS);
        try{
            String xid=  redisUtils.streamXAdd(streamKey, fKey,content);
            if (StringUtils.isNotBlank(xid)){
                this.rdsInsertGroupSet(groupIdS,content);
                StaticVariableUtils.lastSendXAddTaskStreamIdMap.put(streamKey,xid);
                return  xid;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.error("stream xadd  keys fail:"+streamKey);
        StaticVariableUtils.pushFailTask.put(streamKey+"_"+content, DateUtils.getLocationDate());
        return null;
    }





//    private String pushCmd2TargetNodeRedis(String clientIp, String key, String cmds){
//        String taskKey= "cmd:"+clientIp+":"+HashUtils.getCRC32(key+cmds);
//        try{
//            String xid=  redisUtils.streamXAdd(clientIp+RedisStreamType.COMMAND.getName(), key,cmds);
//            if (StringUtils.isNotBlank(xid)){
//                StaticVariableUtils.pushFailTask.remove(taskKey);
//                return  xid;
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        logger.error("stream xadd  keys fail:"+clientIp+":"+key+":"+cmds);
//        StaticVariableUtils.pushFailTask.put(taskKey,DateUtils.getLocationDate());
//        return null;
//    }

    private String pushCmd2AllNode(String key, String cmds){
        final  String tKey="public";
        String taskKey= "cmd:public:"+ HashUtils.getCRC32(key+cmds);
        try{
            //public：command-stream
            //normal
            //rm -rf path
            logger.info(taskKey+":"+cmds);
            String xid=  redisUtils.streamXAdd(tKey+RedisStreamType.COMMAND.getName(), key,cmds);
            if (StringUtils.isNotBlank(xid)){
                StaticVariableUtils.pushFailTask.remove(taskKey);
                return  xid;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.error("stream xadd  keys fail:"+tKey+":"+key+":"+cmds);
        StaticVariableUtils.pushFailTask.put(taskKey,DateUtils.getLocationDate());
        return null;
    }



//    private String push2NodeCmd(String ip,String cmd){
//        final  String tKey="public";
//        try{
//            String xid=  redisUtils.streamXAdd(tKey+RedisStreamType.COMMAND.getName(), ip,cmd);
//            return xid;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return "";
//    }

    private String push2nodeCmdV2(String ip,String cmd){
        final  String tKey="public";
        try{
            String xid=  redisUtils.streamXAdd(tKey+RedisStreamType.COMMAND.getName(), "node_cmd:"+ip,cmd);
            return xid;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String getWsSpecialPortsResetValue(){
        try {
            TbCdnPublicMutAttrEntity attr= publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",PublicEnum.COMMON_SPECIAL_PORTS.getName()).last("limit 1"));
            Set<String>ports=new HashSet<>();
            ports.add("80");
            ports.add("443");
            if (null!=attr && StringUtils.isNotBlank(attr.getPvalue())){
                ports.addAll(Arrays.asList(attr.getPvalue().split("\\|")));
            }
            StringBuilder cmdBf=new StringBuilder();
            cmdBf.append("iptables -F OUTPUT && ");
            cmdBf.append(String.format("iptables -I OUTPUT -p tcp -m multiport --sports %s --tcp-flags SYN,RST,ACK,FIN,PSH SYN,ACK -j NFQUEUE --queue-num 8899",String.join(",",ports)));

            return cmdBf.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
       return "";
    }

    private String pushNodeSysWsStatus(String ip,int mode){
        try{
            if (0==mode){
                //关闭
                this.push2nodeCmdV2(ip,CommandEnum.SYS_WS_OFF.getContent());
                this.push2nodeCmdV2(ip,CommandEnum.IP_TABLE_OUTPUT_F.getContent());
            }else if (1==mode){
                //开启
                this.push2nodeCmdV2(ip,CommandEnum.SYS_WS_ON.getContent());
                this.push2nodeCmdV2(ip,this.getWsSpecialPortsResetValue());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String pushWsSpecialPortsReset(){
        try{
            List<CdnClientEntity>list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                    .eq("sys_ws_status",1)
                    .eq("client_type",1)
                    .eq("status",1)
                    .select("client_ip")
                    .and(q->q.eq("parent_id",0).or().isNull("parent_id"))
            );
            final   String key="normal";
            String value=this.getWsSpecialPortsResetValue();
            for (CdnClientEntity client:list){
                String clientIp=client.getClientIp();
                push2nodeCmdV2(clientIp,value);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private String pusCmdUserMutThreadPurge2AllNode(String key, String cmds){
        //public:purge-stream
        //normal
        //curl 0000
        final  String tKey="public";
        String taskKey= "cmd:"+tKey+":"+HashUtils.getCRC32(key+cmds);
        try{
            String xid=  redisUtils.streamXAdd(tKey+RedisStreamType.MULTI_PURGE_COMMAND.getName(), key,cmds);
            if (StringUtils.isNotBlank(xid)){
                StaticVariableUtils.pushFailTask.remove(taskKey);
                return  xid;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.error("stream xadd  keys fail:"+tKey+":"+key+":"+cmds);
        StaticVariableUtils.pushFailTask.put(taskKey,DateUtils.getLocationDate());
        return null;
    }



    private Boolean rdsDeleteKey(String key){
        try{
            if (redisUtils.delete(key)){
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.warn("delete key fail:"+key);
       return null;
    }



    private  List<MapRecord<String, Object, Object>> rdsStreamRead(String key){
        try{
            return redisUtils.streamReadLimitSize(key,StaticVariableUtils.maxFeedBackInfoSize);
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.warn("stream read keys fail:"+key);
        return null;
    }

    //----------------------------------------------------------------------------------------
    //------------------------------------push method-----------------------------------------
    //----------------------------------------------------------------------------------------




    /**
     * 获取NGINX version by curl
     * @param client
     */
    private boolean getNginxVersionByCurl(CdnClientEntity client){
            try{
               //logger.debug("curl-->获取远程版本号");
                String res= HttpRequest.sendGet("http://"+client.getClientIp()+":8181/version","");
                if(StringUtils.isNotBlank(res)){
                    JSONObject versionJson=DataTypeConversionUtil.string2Json(res);
                    if(null!=versionJson && versionJson.containsKey("ants_waf")){
                        String clientVersion=versionJson.getString("ants_waf");
                       //logger.debug("curl-->get target versionJson:"+client.getClientIp()+",version==>"+clientVersion);
                        if(null==client.getVersion()){
                            client.setVersion(clientVersion);
                            cdnClientDao.updateById(client);
                            return  true;
                        }else if(null!=client.getVersion() && !client.getVersion().equals(clientVersion)){
                            if (client.getVersion().compareToIgnoreCase(clientVersion)<0){
                                client.setVersion(clientVersion);
                                cdnClientDao.updateById(client);
                                return  true;
                            }
                        }
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
            return false;
    }

    /**
     * 获取NGINX VERSION BY REDIS
     * @param client
     */
    private void getNginxVersionByRedisStream(CdnClientEntity client){
            try {
               //logger.debug("-->redis 获取远程版本号");

                String streamKey=client.getClientIp()+":feedback-stream";
                JSONObject versionJson=new JSONObject();
                List<MapRecord<String, Object, Object>>    feedBackInfoList=null;
                for (int i = 0; i <6 ; i++) {
                    feedBackInfoList= rdsStreamRead(streamKey);
                    if (null==feedBackInfoList  || 0==feedBackInfoList.size()){
                        logger.warn("read feedback-stream is empty!");
                        continue;
                    }
                    try{Thread.sleep(2000);}catch (Exception e){e.printStackTrace();}
                }
                if (null==feedBackInfoList){
                   //logger.debug("获取失败！");
                    return;
                }
                logger.info("-->redis->feedback-stream_size:"+feedBackInfoList.size());
                for (MapRecord mapRecord:feedBackInfoList){
                    LinkedHashMap stmKVMap= (LinkedHashMap)mapRecord.getValue();
                    if(stmKVMap.containsKey("version") ){
                        String recordId= mapRecord.getId().getValue();
                        String res2=stmKVMap.get("version").toString();
                        if(StringUtils.isNotBlank(res2)){
                            versionJson=DataTypeConversionUtil.string2Json(res2);
                           //logger.debug("->redis->versionJsonStr:"+res2);
                            break;
                        }
                        redisUtils.streamDel(streamKey,recordId);
                    }
                }
                if(null!=versionJson && versionJson.containsKey("ants_waf")){
                    String client_version=versionJson.getString("ants_waf");
                   //logger.debug(">redis->get target versionJson:"+client.getClientIp()+"==>"+client_version);
                    if(null==client.getVersion()){
                        client.setVersion(client_version);
                        cdnClientDao.updateById(client);

                    }else  if(null!= client.getVersion() && !client.getVersion().equals(client_version)){
                        client.setVersion(client_version);
                        cdnClientDao.updateById(client);
                    }
                }

            }
            catch (Exception e){
                e.printStackTrace();
               //logger.debug("get version error!");
            }

    }

    private void getNodeAgentVersionByRedis(CdnClientEntity client){
        String v= redisUtils.get(String.format("version_%s",client.getClientIp()));
       //logger.debug("------"+v);
        if (StringUtils.isNotBlank(v)){
            NodeVersionVo vo=NodeVersionVo.getVersionObj(v);
            if (null!=vo.getNginxWafVersion() || null!=vo.getAgentVersion()){
                if ( !vo.getNginxWafVersion().equals(client.getVersion())){
                        client.setNgxVersion(vo.getNginxVersion());
                        client.setVersion(vo.getNginxWafVersion());
                }
                if(!vo.getAgentVersion().equals(client.getAgentVersion())){
                    client.setAgentVersion(vo.getAgentVersion());

                }
                cdnClientDao.updateById(client);
            }
        }

    }







    /**
     * 更新客户端版本
     */
    private void updateClientNginxVersionHandle(){
       //logger.debug("update_client_nginx_version");
        //1 redis-get-nginx version
        String cmd=CommandEnum.getContentById(CommandEnum.NGINX_VERSION.getId());
        pushCmd2AllNode("normal:"+CommandEnum.getCommandIdByContent(cmd),cmd);

        //更新反馈
        List<CdnClientEntity> clientList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
        );
        for(CdnClientEntity client:clientList) {
            //1获取远程版本号
            if (true) {
                getNginxVersionByCurl(client);
            }
            if (false) {
                getNginxVersionByRedisStream(client);
            }
            if (true){
                getNodeAgentVersionByRedis(client);
            }
        }
    }



    /**
     * 获取NGINX 版本
     * @param
     * @return
     */
    @Override
    public boolean getClientNginxVersion() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateClientNginxVersionHandle( );
                }catch (Exception e){
                    //System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }

    @Override
    public List<String> getAllCreateFileInvokeMethod() {
        List<String> list=new ArrayList<>();
        Class<?>  cls = getClass();
        Method[] ms=cls.getDeclaredMethods();
        for (Method method:ms){
            if(2==method.getModifiers()){
                if(method.getName().contains("Get")){
                    list.add(method.getName());
                }
            }
        }
        return list;
    }


    /**
     * 解封IP
     * @param ips
     */
    public void releaseInterceptIp(String ips) {
        if(StringUtils.isBlank(ips)){
            return;
        }
        if (ips.equals("all")){
            this.pushCmd2AllNode("normal",CommandEnum.CLEAN_SHORT_CC.getContent());
            return;
        }
        List<String> IPV4List=new ArrayList<>();
        List<String> IPV6List=new ArrayList<>();
        for (String ip:ips.split(",")){
            StaticVariableUtils.deleteNftMap.put(ip,System.currentTimeMillis());
            if(IPUtils.isValidIPV4(ip)){
                IPV4List.add(ip);
            }else if(IPUtils.isValidIPV6(ip)){
                IPV6List.add(ip);
            }
       }
       if (IPV4List.size()>0){
          //String cmd="/usr/sbin/nft delete  element inet filter short_cc {"+String.join(",",IPV4List)+"}";
          for (String ip:IPV4List){
              String cmd="/usr/sbin/ipset del short_cc "+ip;
              //logger.info(cmd);
              this.pushCmd2AllNode("short_cc",cmd);
          }

       }
       if (IPV6List.size()>0){
           ///usr/sbin/ipset create short6_cc hash:ip family inet6 maxelem 3000000 timeout 172800
           for (String ipv6:IPV6List){
               String cmd="/usr/sbin/ipset del short6_cc "+ipv6;
               this.pushCmd2AllNode("short_cc",cmd);
           }
           //String cmd="/usr/sbin/ipset del  short6_cc {"+String.join(",",IPV6List)+"}";
           // String cmd="/usr/sbin/nft delete  element inet filter short_cc6 {"+String.join(",",IPV6List)+"}";
           //this.pushCmd2AllNode("short_cc",cmd);
       }


    }



    private List<CdnClientEntity> getClientList(){
         return  methodHandleTool.getClientList();
    }


    /**
     * 根据serialNumber获取GROUPiD
     * @param serialNumber
     * @return
     */
    @Override
    public String getNodeAreaGroupIdBySerialNumber(String  serialNumber){
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
                .last("limit 1"));
        if(null==suitEntity){
            logger.error("[GetClientListBySite] suit ["+serialNumber+"] is null");
            return "0";
        }
        TbOrderEntity order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",suitEntity.getPaySerialNumber()).last("limit 1"));
        if(null==order){
            logger.error("[GetClientListBySite]order is null");
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
            logger.error("[GetClientListBySite]product is null!");
            return "0";
        }
        String serverGroupIds=product.getServerGroupIds();
        if(StringUtils.isBlank(serverGroupIds)){
            logger.error("[GetClientListBySite]serverGroupIds is null");
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

    @Override
    public void setElkConfig2Redis(ElkServerVo elkServerVo) {
        ElkRedisKeyVo vo=new ElkRedisKeyVo();
        vo.setElasticsearchhost(elkServerVo.getHost());
        vo.setElasticsearchport(elkServerVo.getPort());
        vo.setElasticsearchmethod(elkServerVo.getMethod());
        vo.setElasticsearchpwd(elkServerVo.getPwd());
        if (StringUtils.isNotBlank(elkServerVo.getCaPath())){
           String content= FileUtils.getStringByPath(elkServerVo.getCaPath());
           vo.setElasticsearchca_path(content);
        }
        Map voMap=DataTypeConversionUtil.entity2map(vo);
        if (null!=voMap){
            for (Object key:voMap.keySet()){
                redisUtils.longSet(key.toString(),voMap.get(key).toString());
            }
        }
    }




    private void deleteNodeDir(String dir){
       if(!dir.contains("/home/local/nginx/conf/")){
           throw new RRException("路径有误！");
       }
       // Date now=new Date();
       pushCmd2AllNode("normal","rm -rf "+dir);

   }


   private void pushSuitServerHandler(String serialNumber){
        // 1 site
       List<TbSiteEntity> sites = tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
               .eq("serial_number",serialNumber)
               .eq("status",1)
               .select("id")
       );
       if (sites.size()>0){
           List<String>ids=sites.stream().map(t->t.getId().toString()).collect(Collectors.toList());
           if (ids.size()>0){
               Map pushMap=new HashMap(8);
               pushMap.put(SITE_CHUNK.getName(),String.join(",",ids));
               this.opTaskV2Handle(pushMap);
           }
       }


       // 2 streams
       List<TbStreamProxyEntity> streamProxyEntityList=tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
               .eq("serial_number",serialNumber)
               .eq("status",1)
               .select("id")
       );
       if (streamProxyEntityList.size()>0){
           List<String>ids2=streamProxyEntityList.stream().map(t->t.getId().toString()).collect(Collectors.toList());
           if (ids2.size()>0){
               Map pushMap=new HashMap(8);
               pushMap.put(STREAM_CONF.getName(),String.join(",",ids2));
               this.opTaskV2Handle(pushMap);
           }
       }

   }

    /**
     * 关闭指定套餐相关产品
     * @param SerialNumber
     */
   private  void closeSuitServiceHandle(String SerialNumber){
       //清理相关产品
       //1 site
       List<TbSiteEntity> siteList=tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
               .eq("serial_number",SerialNumber));
       for (TbSiteEntity site:siteList){
           //String addKey="SITE_"+site.getId()+"_SET";
           //String delKey="SITE_"+site.getId()+"_DEL_SET";
           //this.pushDeleteRdsKey("site",Integer.valueOf(site.getId()),addKey,delKey,DelConfMode.CLOSE);
           this.deleteSite(Integer.valueOf(site.getId()),DelConfMode.CLOSE);
       }

       //2 proxy
       List<TbStreamProxyEntity> proxyList=tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
               .eq("serial_number",SerialNumber));
       for (TbStreamProxyEntity proxy:proxyList){
           //String addKey="STREAM_"+proxy.getId()+"_SET";
           //String delKey="STREAM_"+proxy.getId()+"_DEL_SET";
           //this.pushDeleteRdsKey("stream",Integer.valueOf(proxy.getId()),addKey,delKey,DelConfMode.CLOSE);
           this.deleteStream(Integer.valueOf(proxy.getId()),DelConfMode.CLOSE);
       }

       //3 rewrite
       List<TbRewriteEntity> rewriteList= tbRewriteDao.selectList(new QueryWrapper<TbRewriteEntity>()
               .eq("serial_number",SerialNumber)) ;
       for (TbRewriteEntity rewrite:rewriteList){
           //String addKey="REWRITE_"+rewrite.getId()+"_SET";
          // String delKey="REWRITE_"+rewrite.getId()+"_DEL_SET";
           //this.pushDeleteRdsKey("rewrite",Integer.valueOf(rewrite.getId()),addKey,delKey,DelConfMode.CLOSE);
           this.deleteRewrite(Integer.valueOf(rewrite.getId()),DelConfMode.CLOSE);
       }

   }


    private void pushCmdToNode(String cmd ){
        this.pushCmd2AllNode("normal:"+CommandEnum.getCommandIdByContent(cmd),cmd);
    }

    private void checkSiteHandle(String siteId){
        TbSiteEntity site =tbSiteDao.selectById(siteId);
        if(null==site){
           //logger.debug("site is null");
            return;
        }
        if(StringUtils.isBlank(site.getMainServerName())){
           //logger.debug("MainServerName is empty");
            return;
        }
        List<CdnClientEntity> client_list=this.getClientList();
        //this.getClientListBySerialNumber(site.getSerialNumber());
        List<Integer>ports=methodHandleTool.getSitePorts(Integer.parseInt(siteId));
        if(0==ports.size()){
           //logger.debug("ports is empty");
            return;
        }
        String port=ports.get(0).toString();
        for (CdnClientEntity client:client_list){
            String cmd="curl -s -x "+client.getClientIp()+":"+port+" http://"+site.getMainServerName();
            //this.pushCmd2TargetNodeRedis(client.getClientIp(),"normal",cmd);
            push2nodeCmdV2(client.getClientIp(),cmd);
        }
    }



    //添加日志到SYSLOG
    private void InsertTaskLog(String operation,String method){
        SysLogEntity logEntity=new SysLogEntity();
        logEntity.setUserType(UserTypeEnum.MANAGER_TYPE.getId());
        logEntity.setLogType(LogTypeEnum.OTHER_LOG.getId());
        logEntity.setOperation(operation);
        if (method.length()>64){
            logEntity.setMethod(method.substring(0,64));
        }else{
            logEntity.setMethod(method);
        }

        //logEntity.setIp("127.0.0.1");
        sysLogDao.insert(logEntity);
        //
    }

    private void InsertPushLog(String operation,String method){
        SysLogEntity logEntity=new SysLogEntity();
        logEntity.setUserType(UserTypeEnum.MANAGER_TYPE.getId());
        logEntity.setLogType(LogTypeEnum.DISPATCH_LOG.getId());
        logEntity.setUserId(1l);
        logEntity.setUsername("admin");
        logEntity.setIp("127.0.0.1");
        logEntity.setOperation(operation);
        if (method.length()>64){
            logEntity.setMethod(method.substring(0,64));
        }else{
            logEntity.setMethod(method);
        }

        //logEntity.setIp("127.0.0.1");
        sysLogDao.insert(logEntity);
        //
    }



    @Override
    public R getDomainAndAlias( TbSiteEntity tbSite,boolean needVerify){
       if(null==tbSite){
           return R.error("No domain") ;
       }
        StringJoiner domainListJoiner = new StringJoiner(",");
        domainListJoiner.add(tbSite.getMainServerName());
        List<TbSiteAttrEntity> aliasList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey", SiteAttrEnum.ALIAS.getName())
                .eq("site_id",tbSite.getId())
                .eq("status",1)
        );
        for (TbSiteAttrEntity attr:aliasList){
            if(StringUtils.isBlank(attr.getPvalue())){
                continue;
            }
            if (needVerify){
                String url="http://"+attr.getPvalue();
                if (HttpRequest.isNormalReturnAcmeAccount(url)){
                    String eMsg="申请失败！别名："+url+",验证失败,别名CNAME未解析至系统，请稍候后再试！";
                    return R.error(eMsg);
                }
            }
            domainListJoiner.add(attr.getPvalue());
        }
        return R.ok().put("data",domainListJoiner.toString()) ;
    }

    private int getDomainAndAliasMainDomainSum(TbSiteEntity tbSite){
        if(null==tbSite){
            return 0 ;
        }
        Set<String>domainList = new HashSet<>();
        domainList.add(DomainUtils.getMainTopDomain(tbSite.getMainServerName()) );
        List<TbSiteAttrEntity> aliasList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey", SiteAttrEnum.ALIAS.getName())
                .eq("site_id",tbSite.getId())
                .eq("status",1)
        );
        for (TbSiteAttrEntity attr:aliasList){
            if(StringUtils.isBlank(attr.getPvalue())){
                continue;
            }
            domainList.add(DomainUtils.getMainTopDomain(attr.getPvalue()) );
        }
        return domainList.size();
    }

    @Override
    public void deleteCacheByKey(String key) {
        try {
            if (!CacheKeyEnums.isExistKey(key)) {
                return;
            }
            redisUtils.delete(key);
        }catch (Exception e) {
            logger.info(e.getMessage());
        }

    }


    //certServer apply cert
    private R certServerApply(TbSiteEntity tbSite, TbCertifyEntity tbCertify,int useMode,int dnsConfId){
        if (null==tbSite){
           return R.error("No domain") ;
        }
        R r1=this.getDomainAndAlias(tbSite,false);
        if (1!=r1.getCode()){
            return  r1;
        }
        //构建apply param
        CertServerApplyForm caf=new CertServerApplyForm();
        caf.setId(tbSite.getId());
        caf.setDomains(r1.get("data").toString());
        caf.setUsemode(useMode);
        caf.setCallback(this.getCacheValueByKey(CacheKeyEnums.cert_apply_callback_url.getKeyName()));
        if (useMode==CertSrcTypeEnums.CertServerLetV2.getType() || useMode==CertSrcTypeEnums.CertServerZeroHttpV2.getType()){
            //3 | 5
            //continue
        }else if (useMode==CertSrcTypeEnums.CertServerLetDnsV2.getType() || useMode==CertSrcTypeEnums.CertServerZeroDnsV2.getType() ){
            //4 | 6
            String mainDomain=DomainUtils.getMainTopDomain(tbSite.getMainServerName());
            String top=tbSite.getMainServerName().replace(mainDomain,"");
            if (top.endsWith(".")){
                top=top.substring(0,top.length()-1);
            }
            if ("".equals(top)){
                top="@";
            }
            if(!top.equals("*")){
                if (!(top.replace("*","")).startsWith(".")){
                    top="."+top;
                }
            }
            caf.setTop(top);
            caf.setDnsconfigid(dnsConfId);
        }
        String vUrl=  this.getCacheValueByKey(CacheKeyEnums.cert_apply_proxy_pass.getKeyName());
        if (null==vUrl || StringUtils.isBlank(vUrl)){
            return R.error("No certServer proxy_pass config") ;
        }
        String url=vUrl;
        String scheme="http://";
        String ipPort=vUrl;
        if (vUrl.startsWith("http://")){
            scheme="http://";
            ipPort=vUrl.substring(scheme.length());
        }else if (vUrl.startsWith("https://")){
            scheme="https://";
            ipPort=vUrl.substring(scheme.length());
        }


        String[] values=StringUtils.split(ipPort,":");
        if (2==values.length){
            int port=Integer.parseInt(values[1]);
            url=String.format("%s%s:%d/http_task",scheme,values[0],port);
        }else if (1==values.length){
            url=String.format("%s%s:%d/http_task",scheme,values[0],"81");
        }
        //logger.info(String.format("scheme:%s,ip:port:%s,url:%s",scheme,ipPort,url));
        //insert record
        if (null==tbCertify){
            tbCertify=new TbCertifyEntity();
            tbCertify.setUserId(tbSite.getUserId());
            tbCertify.setCommonName(tbSite.getMainServerName());
            tbCertify.setStatus(TbCertifyStatusEnum.APPLYING.getId());
            tbCertify.setSiteId(tbSite.getId());
            tbCertify.setSrcType(useMode);
            tbCertify.setApplyInfo(DataTypeConversionUtil.entity2jonsStr(caf));
            tbcertifyDao.insert(tbCertify);
        }else{
            tbCertify.setStatus(TbCertifyStatusEnum.APPLYING.getId());
            tbCertify.setSiteId(tbSite.getId());
            tbCertify.setSrcType(useMode);
            tbCertify.setApplyInfo(DataTypeConversionUtil.entity2jonsStr(caf));
            tbcertifyDao.updateById(tbCertify);
        }
        //send record
        logger.info("param:"+DataTypeConversionUtil.entity2jonsStr(caf));
        String result= HttpRequest.sendPostJsonStr(url,DataTypeConversionUtil.entity2jonsStr(caf));
        logger.info("result:"+result);
        return R.ok().put("data",result);
    }


    /**
     * 重签证书
     * @param siteId
     * @param reIssuedMode 0==普通；1=强制
     */
    @Override
    public   R pushApplyCertificateBySiteId(Integer siteId,int reIssuedMode,int useMode,int resId,int dnsConfId) {

        //删除mysql record
        tbcertifyDao.delete(new QueryWrapper<TbCertifyEntity>()
                .and(q->q.eq("site_id",siteId).or().eq("id",resId))
        );
        TbSiteEntity tbSite=tbSiteDao.selectById(siteId);
        if (null==tbSite){
            return R.error("Site not found");
        }
        if (CertSrcTypeEnums.getIsNeedVerifyDnsByType(useMode) ){
            // verify_domain in dns
            if (0==dnsConfId){
                return R.error("dnsConfigId不能为空");
            }
            //verify main sum
            int mSum= getDomainAndAliasMainDomainSum(tbSite);
            if (mSum!=1){
                return R.error("DNS 申请方式主域名不能多个");
            }
            //verify not found
            R r1=dnsCApiService.verifyMainDomainInDns(dnsConfId,DomainUtils.getMainTopDomain(tbSite.getMainServerName()));
            if (1!=r1.getCode()){
                return r1;
            }
        }
        String applyCertServerMode="l";
        String uMode= getCacheValueByKey(CacheKeyEnums.cert_apply_mode.getKeyName());
        if (StringUtils.isNotBlank(uMode) && "2".equals(uMode)){
            applyCertServerMode="r";
        }
        boolean v=CertSrcTypeEnums.getIsCanApply(applyCertServerMode,useMode);
        if (!v){
            return R.error("Apply mode error");
        }
        if (0==reIssuedMode){
            // 删除acme_site_file
            AcmeShUtils.deleteCertFileBySiteId(siteId,null);
            TbCertifyEntity  tbCertify=new TbCertifyEntity();
            tbCertify.setUserId(tbSite.getUserId());
            tbCertify.setCommonName(tbSite.getMainServerName());
            tbCertify.setStatus(TbCertifyStatusEnum.APPLYING.getId());
            tbCertify.setSiteId(siteId);
            tbCertify.setSrcType(useMode);
            tbcertifyDao.insert(tbCertify);
            this.pushApplyCertificate(siteId,tbCertify.getId(),useMode,resId,dnsConfId);
        }else if (1==reIssuedMode){
            TbCertifyEntity tbCertify= new TbCertifyEntity();
            tbCertify.setCommonName(tbSite.getMainServerName());
            tbCertify.setUserId(tbSite.getUserId());
            tbCertify.setStatus(TbCertifyStatusEnum.APPLYING.getId());
            tbCertify.setSiteId(siteId);
            tbCertify.setSrcType(useMode);
            tbcertifyDao.insert(tbCertify);
            this.pushApplyCertificate(siteId,tbCertify.getId(),useMode,resId,dnsConfId);
        }else{
            return R.error("Reissued mode error");
        }
        return R.ok();
    }


    @Override
    public LinkedHashMap preciseWafParam() {
        return PreciseWafParamEnum.getTransLinkMap();
    }




    /**
     * 初始化 clean
     * @param ClientIds
     */
    private void initCleanRdsData(String ClientIds){
        //删除任务流
        if (true){
            List<String> keys=RedisStreamType.getAll();
            for (String key:keys){
                if (!key.contains("stream")){
                    continue;
                }
                redisUtils.scanAllDelete("*"+key+"*");
            }
        }

        //1 clean  rds
        if(false){
            final String clKey= "redis_clean_Time";
            String cleanTmp=redisUtils.get(clKey);
            long cl=0l;
            if(StringUtils.isNotBlank(cleanTmp)) {
                cl=Long.valueOf(cleanTmp);
            }
            // 5 分钟内不重复删除
            if ((System.currentTimeMillis()-cl)>5*60*1000) {
                redisUtils.set(clKey,String.valueOf(System.currentTimeMillis()));
                String[] delKeys={"/home/local/nginx/conf*","*file-path","*-stream","allfile-path*"};
                for (String p:delKeys){
                    for (String key:redisUtils.scanAll(p)){
                        redisUtils.delete(key);
                    }
                }
            }
        }



        //0 clean feedback AND RED_SET
        if (true){
            StaticVariableUtils.pushCallBackMap.clear();
            StaticVariableUtils.pushCallBackErrorMap.clear();
            StaticVariableUtils.pushCallBackUserErrorMap.clear();
            List<LinkedHashMap<String,Object>> clientList=cdnClientDao.queryOnlineClient() ;
            for(LinkedHashMap clientMap:clientList){
                CdnClientEntity client=DataTypeConversionUtil.map2entity(clientMap, CdnClientEntity.class);
                rdsDeleteKey(client.getClientIp()+":feedback-stream");
            }
        }

        if (false){
            this.pushCmd2AllNode("normal","rm -rf /home/local/nginx/conf/conf/html/*");
            this.pushCmd2AllNode("normal","rm -rf /home/local/nginx/conf/conf/waf/*");
        }
        logger.info("initCleanData complete");
    }

    private void initCleanNodeConf(){
        if (false){
            //20231107 删除文件
            //202311 10 第天删除一次
            boolean delFlag=false;
            if (true) {
                final String initDelFlagKey="init_del_flag";
                String curDate= DateUtils.format(new Date());
                String delFlagStr= redisUtils.get(initDelFlagKey);
                if (StringUtils.isNotBlank(delFlagStr) && delFlagStr.equals(curDate)){
                    return;
                }
                redisUtils.set(initDelFlagKey,curDate);
                delFlag=true;
            }

            //3 delete conf
            final String delConf="rm -rf /home/local/nginx/conf/conf/*";
            final String delEtcCmd="cd /home/local/nginx/conf/etc && find . ! -name \"http.conf\" -type f -exec rm {} \\;";
            String[] cmds={delConf,delEtcCmd} ;
            for (String cmd:cmds){
                this.pushCmd2AllNode("normal",cmd);
            }



            if (delFlag) {
                try{
                    Thread.sleep(30*1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            logger.info("init clean node conf complete ");
        }
    }

    @Override
    public void checkSitesInNode(String siteIds) {
        if(StringUtils.isBlank(StaticVariableUtils.authMasterIp)){
            logger.error("AuthMasterIp is null");
            throw new RRException("主控IP未知");
        }
        if(DateUtils.stamp2date(StaticVariableUtils.authEndTime).before(new Date())){
            logger.error("auth out of time");
            throw new RRException("主控授权未知");
        }
        try{
            StaticVariableUtils.makeFileThread=true;
            String[] id_s=siteIds.split(",");
            for (String id:id_s){
                checkSiteHandle(id);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            StaticVariableUtils.makeFileThread=false;
        }

    }


    private void applyFunc(int index){
        logger.info("applyCertThread start "+index);
        List<TbCertifyEntity> list=tbcertifyDao.getApplyingListByStatus(index,TbCertifyStatusEnum.APPLYING.getId());
        //logger.info(list.toString());
        for (TbCertifyEntity tbCertify:list){
            try{
                if (StringUtils.isBlank(tbCertify.getApplyInfo())){
                    tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                            .eq("id",tbCertify.getId())
                            .set("status",TbCertifyStatusEnum.FAIL.getId())
                    );
                    logger.info("getApplyInfo is empty ");
                    continue;
                }
                if (tbCertify.getStatus()==TbCertifyStatusEnum.APPLYING.getId() && (tbCertify.getCreateTime().getTime()+24*3600*1000)<System.currentTimeMillis() ){
                    logger.info("apply task is expected ");
                    tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                            .eq("id",tbCertify.getId())
                            .set("acme_log",R.error("time_out").toJsonString())
                            .set("status",TbCertifyStatusEnum.FAIL.getId())
                    );
                    continue;
                }
                // -----start--apply----
                if (tbCertify.getSrcType()==CertSrcTypeEnums.LetsencryptDns.getType()){
                    //2 acme-dns
                    try{
                        AcmeDnsVo vo = DataTypeConversionUtil.string2Entity(tbCertify.getApplyInfo(),AcmeDnsVo.class) ;
                        if (null==vo || 0==vo.getDnsConfigId()){
                            tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                    .eq("id",tbCertify.getId())
                                    .set("status",TbCertifyStatusEnum.FAIL.getId())
                            );
                            logger.info("AcmeDnsVo is empty ");
                            continue;
                        }
                        R r=AcmeDnsApplyCertHandle(tbCertify,vo);
                        logger.info("Acme_DnsApplyCert_Handle:"+r.toJsonString());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else  if (tbCertify.getSrcType()==CertSrcTypeEnums.LetsencryptHttp.getType()){
                    //1 acme_http01
                    try{
                        Thread.sleep(30*1000);
                        ApplyCertVo acVo=DataTypeConversionUtil.string2Entity(tbCertify.getApplyInfo(),ApplyCertVo.class) ;
                        if (null==acVo || null==acVo.getSiteId() || null==acVo.getDomainList() || null==acVo.getNoticeCallBackCmd()){
                            tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                    .eq("id",tbCertify.getId())
                                    .set("status",TbCertifyStatusEnum.FAIL.getId())
                            );
                            logger.info("acVo is empty ");
                            continue;
                        }
                        R r=AcmeShUtils.applyCertAndNotice(acVo.getSiteId(),acVo.getDomainList(),acVo.getNoticeCallBackCmd());
                        tbCertify.setAcmeLog(r.toJsonString());
                        tbCertify.setStatus(TbCertifyStatusEnum.FAIL.getId());
                        if (1==r.getCode()){
                            tbCertify.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
                        }
                        tbcertifyDao.updateById(tbCertify);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else if (tbCertify.getSrcType()==CertSrcTypeEnums.ZeroSslHttp.getType()){
                    logger.error("zeroSsl is not use thread apply:"+tbCertify.getId());
                }else if (tbCertify.getSrcType()==CertSrcTypeEnums.CertServerLetV2.getType()){
                    logger.error("CertServer is not use thread apply:"+tbCertify.getId());
                }else if (tbCertify.getSrcType()==CertSrcTypeEnums.CertServerLetDnsV2.getType()){
                    logger.error("CertServer is not use thread apply:"+tbCertify.getId());
                }else if (tbCertify.getSrcType()==CertSrcTypeEnums.CertServerZeroDnsV2.getType()){
                    logger.error("CertServer is not use thread apply:"+tbCertify.getId());
                }else{
                    logger.info("not found src_type,apply exit,src_type="+tbCertify.getSrcType());
                    continue;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }


    private R  AcmeDnsApplyCertHandle(TbCertifyEntity tbCertify,AcmeDnsVo vo){
        String  eMsg="";
        try{
            //下订单
            logger.info("001: Acme_DnsApplyCertHandle");
            R r=AcmeShUtils.getApplyDomainDnsInfo(vo);
            logger.info("001: "+r.toJsonString());
            tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                    .eq("id",tbCertify.getId())
                    .set("apply_info",DataTypeConversionUtil.entity2jonsStr(vo))
                    .set("acme_log",r.toJsonString())
            );
            //tbOrderDao.updateById(orderEntity);
            if (0==r.getCode()){
                logger.info("001 fail: ",r.toJsonString());
                return r;
            }
            if (1==r.getCode() && 2==vo.getStatus()){
                logger.info("001 success:"+ r.toJsonString());
                logger.info(DataTypeConversionUtil.entity2jonsStr(vo));
                return r;
            }
            if (null==vo.getTvMap() || vo.getTvMap().isEmpty()){
                logger.info("001 fail: getTvMap is empty",r.toJsonString());
                return r;
            }
            //  //0=fail 1==applying 2=success  3=需要添加TXT记录 4=需要添加CNAME记录
            if (3==vo.getStatus()){
                logger.info("002: add_txt");
                R r2=addAcmeTxtRecord2DnsSys(vo);
                logger.info("002: "+r2.toJsonString());
                r2.put("r1",r.toJsonString());
                tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                        .eq("id",tbCertify.getId())
                        .set("apply_info",DataTypeConversionUtil.entity2jonsStr(vo))
                        .set("acme_log",r2.toJsonString())
                );
                if(0==r2.getCode()){
                    logger.info("add record fail");
                    return r2;
                }
                try{
                    //暂停1分钟后查询
                    Thread.sleep(30*1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                logger.info("003: renew");
                R r3= renewAcmeFreeCert(vo ,3);
                logger.info("003: renew"+r2.toJsonString());
                r3.put("r2",r2.toJsonString());
                tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                        .eq("id",tbCertify.getId())
                        .set("apply_info",DataTypeConversionUtil.entity2jonsStr(vo))
                        .set("acme_log",r3.toJsonString())
                );
                return r3;
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    private R renewAcmeFreeCert( AcmeDnsVo vo,int maxDeep){
        //renew
        R r=AcmeShUtils.renewApplyDomainDnsInfo(vo,"");
        // orderEntity.setOrderResult();
        //logger.info(r2.toString());
        tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                .eq("id",vo.getCertId())
                .set("acme_log",r.toJsonString())
        );
        if (1==r.getCode() && 2==vo.getStatus()){
            return R.ok();
        }
        logger.info("renewAcmeFreeCert,maxDeep:"+maxDeep);
        //添加DNS记录
        if ( maxDeep>0 ){
            if (!vo.getTvMap().isEmpty()){
                addAcmeTxtRecord2DnsSys(vo);
            }
            try{
                Thread.sleep(30*1000);
            }catch (Exception e){
                e.printStackTrace();
            }
            return  renewAcmeFreeCert(vo,maxDeep-1);
        }
        logger.info("renew-AcmeFreeCert over!");
        return R.error("renew-AcmeFreeCert out 3 times");
    }

    private R addAcmeTxtRecord2DnsSys(AcmeDnsVo rVo){
        logger.info("add_AcmeTxtRecord2Dns Sys "+DataTypeConversionUtil.entity2jonsStr(rVo));
        for (AcmeDnsVo.TxtDomainValue tdv:rVo.getTvMap().values()){
            String mainDomain=DomainUtils.getMainTopDomain(tdv.getDomain());
            String top=tdv.getTop();
            if (StringUtils.isBlank(top)){
                top=tdv.getDomain().replace(mainDomain,"");
                if (StringUtils.isNotBlank(top)){
                    //去除最后一个.
                    if (".".equals(top.substring(top.length()-1))){
                        top=top.substring(0,top.length()-1);
                    }
                }
            }
            try{
                // ADD CAA
                 dnsCApiService.addRecordByConfIdWithMainDomain(rVo.getDnsConfigId(),mainDomain,top,"CAA","","letsencrypt.org","600");
                 if (tdv.getType().equals("TXT") && StringUtils.isNotBlank(tdv.getValue())){
                    //remove txt
                    dnsCApiService.removeRecordByInfoWithMainDomain(rVo.getDnsConfigId(),mainDomain,top,tdv.getType(),"","","");
                    // ADD TXT
                    Object rAddObj=dnsCApiService.addRecordByConfIdWithMainDomain(rVo.getDnsConfigId(),mainDomain,top,tdv.getType(),"",tdv.getValue(),"600");
                    if (null==rAddObj){
                        logger.info("添加失败  " +DataTypeConversionUtil.entity2jonsStr(tdv));
                        //return R.error("添加失败！" +DataTypeConversionUtil.entity2jonsStr(tdv) );
                        continue;
                    }
                }
                logger.info("add_Record_ByConfId:"+DataTypeConversionUtil.entity2jonsStr(tdv) );
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        return R.ok();
    }


    @Override
    public void applyCertThread(){
        if (!StaticVariableUtils.apply_ssl_thread1_flag){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StaticVariableUtils.apply_ssl_thread1_flag=true;
                    applyFunc(0);
                    StaticVariableUtils.apply_ssl_thread1_flag=false;
                }
            }).start();
        }

        if (!StaticVariableUtils.apply_ssl_thread2_flag){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StaticVariableUtils.apply_ssl_thread2_flag=true;
                    applyFunc(1);
                    StaticVariableUtils.apply_ssl_thread2_flag=false;
                }
            }).start();
        }
    }

    /**
     * @param type
     * @param certId
     * @param useMode -1=auto;0=let;1=zeroSsl
     */
    private void applyCertHandle(ApplyTypeEnums type,Integer siteId,  Integer certId,int useMode,int resId,int dnsConfId){
        String eMsg="";
        TbCertifyEntity tbCertify=tbcertifyDao.selectById(certId);
        if(null==tbCertify){
            eMsg="申请失败，当前Id有误！";
            logger.info(eMsg+","+type);
            throw new RRException(eMsg);
        }
        if (0l==tbCertify.getNotAfter() && StringUtils.isNotBlank(tbCertify.getObjInfo()) ){
            JSONObject jsonObject= DataTypeConversionUtil.string2Json(tbCertify.getObjInfo());
            if (jsonObject.containsKey("pem_cert") && StringUtils.isNotBlank(jsonObject.getString("pem_cert"))){
                String pemStr=jsonObject.getString("pem_cert");
                tbCertify.setNotAfter(HashUtils.getCertEndTime(pemStr));
            }
        }
        if ( 0!=tbCertify.getNotAfter() && DateUtils.addDateMonths(DateUtils.LongStamp2Date(tbCertify.getNotAfter()),-1).after(new Date())){
            eMsg="证书["+certId+"]有效期大于1个月，不可重签";
            logger.info(eMsg);
            throw new RRException(eMsg);
        }
        //LinkedList<String> domainList=new LinkedList<>();
        TbSiteEntity tbSite=null;
        //获取域名+别名
        if (null!=siteId && siteId>0){
            tbSite=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>() .eq("id",siteId) .eq("status",CdnSiteStatusEnum.NORMAL.getId()) .last("limit 1"));
        }else{
            tbSite=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>() .eq("main_server_name",tbCertify.getCommonName()).eq("status",CdnSiteStatusEnum.NORMAL.getId()) .last("limit 1"));
        }
        if (null==tbSite){
            eMsg="申请失败，无此站点或站点关闭！";
            tbCertify.setAcmeLog(R.error(eMsg).toJsonString());
            tbCertify.setStatus(TbCertifyStatusEnum.FAIL.getId());
            tbcertifyDao.updateById(tbCertify);
            throw new RRException(eMsg);
        }
        //---init---acme--account
        if (StringUtils.isBlank(AcmeShUtils.ACCOUNT_THUMBPRINT)){
            //logger.error("ACCOUNT_THUMBPRINT is null");
            AcmeShUtils.getAcmeAccount();
            eMsg="申请失败，证书申请组件初始化未完成，请稍后再试！！";
            tbCertify.setAcmeLog(R.error(eMsg).toJsonString());
            tbCertify.setStatus(TbCertifyStatusEnum.FAIL.getId());
            tbcertifyDao.updateById(tbCertify);
            throw new RRException(eMsg);
        }
        if (!AcmeShUtils.PUSH_ACCOUNT_FLAG){
            //更新ACCOUNT
            Map pushMap=new HashMap(8);
            pushMap.put(PUBLIC_HTTP_CONF.getName(),"");
            this.opTaskV2Handle(pushMap);
            AcmeShUtils.PUSH_ACCOUNT_FLAG=true;
        }

        //----验证是否解析----   //test curl
        String  domainListJoiner="";
        boolean needVerifyInSys=CertSrcTypeEnums.isNeedVerifyByType(useMode);
        if (needVerifyInSys){
            R ra= getDomainAndAlias(tbSite,needVerifyInSys);
            if (1!=ra.getCode()){
                eMsg=ra.getMsg();
                tbCertify.setAcmeLog(R.error(eMsg).toJsonString());
                tbCertify.setStatus(TbCertifyStatusEnum.FAIL.getId());
                tbcertifyDao.updateById(tbCertify);
                throw new RRException(eMsg);
            }else{
                domainListJoiner=ra.get("data").toString();
            }
        }

        //更新acme_exp
        if (needVerifyInSys){
            logger.info("update acme_exp timestamp timeout=600");
            Integer exp=new Long(System.currentTimeMillis()/1000l).intValue()+600;
            TbSiteAttrEntity siteAcmeExp=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",tbSite.getId()).eq("pkey",SiteAttrEnum.SITE_ACME_EXP.getName()).eq("status",1));
            if (null==siteAcmeExp){
                siteAcmeExp=new TbSiteAttrEntity();
                siteAcmeExp.setSiteId(tbSite.getId());
                siteAcmeExp.setStatus(1);
                siteAcmeExp.setPkey(SiteAttrEnum.SITE_ACME_EXP.getName());
                siteAcmeExp.setPvalue(String.valueOf(exp) );
                siteAcmeExp.setPType(SiteAttrEnum.SITE_ACME_EXP.getType());
                tbSiteAttrDao.insert(siteAcmeExp);
            }else{
                siteAcmeExp.setPvalue(String.valueOf(exp)  );
                tbSiteAttrDao.updateById(siteAcmeExp);
            }
            StaticVariableUtils.cacheSiteIdConfFileMap.remove(tbSite.getId().toString());
            Map pushMap=new HashMap(8);
            pushMap.put(SITE_CHUNK.getName(),tbSite.getId().toString());
            this.opTaskV2Handle(pushMap);
        }


        //----apply_mode---
        int finalCreatUseMode=0; //0==Letsencrypt;1==zeroSsl
        if (-1!=useMode){
            finalCreatUseMode=useMode;
        }else{
            // 根据配置自动 选择默认
            String uMode= getCacheValueByKey(CacheKeyEnums.cert_apply_mode.getKeyName());
            if (StringUtils.isNotBlank(uMode) && "2".equals(uMode)){
                finalCreatUseMode=CertSrcTypeEnums.CertServerLetV2.getType();
            }else{
                ZeroSslConfig zConf= ZeroSslFactory.build();
                if (null!=zConf && 1==zConf.getConfigId() && StringUtils.isNotBlank(zConf.getApi_key())){
                    finalCreatUseMode=1;
                }else {
                    finalCreatUseMode=0;
                }
            }
        }
        //update_src_type
        tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>().eq("id",certId) .set("src_type",finalCreatUseMode));
        logger.info(String.format("certId=%d,resId=%d",certId,resId));
        if (finalCreatUseMode==CertSrcTypeEnums.LetsencryptHttp.getType()){
            //0==Letsencrypt
            String noticeCallBackCmd="\"/usr/bin/curl http://127.0.0.1:"+serverProperties.getPort()+ serverProperties.getServlet().getContextPath()+ "/sys/common/acme/call/back?siteId="+tbSite.getId()+"\"";
            ApplyCertVo acVo=new ApplyCertVo();
            acVo.setSiteId(tbSite.getId());
            acVo.setCertId(certId);
            acVo.setMode(finalCreatUseMode);
            acVo.setDomainList(domainListJoiner);
            acVo.setNoticeCallBackCmd(noticeCallBackCmd);
            tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                    .eq("id",certId)
                    .set("status",TbCertifyStatusEnum.APPLYING.getId())
                    .set("acme_log","")
                    .set("site_id",tbSite.getId())
                    .set("remark","Letsencrypt")
                    .set("src_type",finalCreatUseMode)
                    .set("apply_info",DataTypeConversionUtil.entity2jonsStr(acVo))
            );
            if ( resId>0 && resId!=certId){
                tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>().eq("id",resId).set("status",TbCertifyStatusEnum.RE_APPLY.getId()));
            }
            this.InsertTaskLog("certId:"+certId,"申请Lets_encrypt证书[certId:"+certId+"]");
            applyCertThread();
        }else if (finalCreatUseMode==CertSrcTypeEnums.ZeroSslHttp.getType()){
            //;1==zeroSsl
            //tbCertifyDao.deleteById(certId);
            ZeroSslAPiCreateCertForm form=new ZeroSslAPiCreateCertForm();
            form.setDomains(domainListJoiner);
            form.setCertificate_validity_days(90);
            form.setSiteId(tbSite.getId());
            form.setCertId(certId);
            String postUrl="http://127.0.0.1:"+serverProperties.getPort()+ serverProperties.getServlet().getContextPath()+"/sys/certify/zero/api/create/cert";
            String r= HttpRequest.sendPostJsonStr(postUrl,DataTypeConversionUtil.entity2jonsStr(form));
            if ( resId>0 && resId!=certId){
                tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>().eq("id",resId)  .set("src_type",finalCreatUseMode)  .set("status",TbCertifyStatusEnum.RE_APPLY.getId()));
            }
            logger.info(r);
            this.InsertTaskLog("certId:"+certId,"申请zeroSsl证书[certId:"+certId+"]");
        }else if (finalCreatUseMode==CertSrcTypeEnums.LetsencryptDns.getType()){
            // 2 Letsencrypt dns
            AcmeDnsVo applyVo=new AcmeDnsVo();
            String noticeCallBackCmd="\"/usr/bin/curl http://127.0.0.1:"+serverProperties.getPort()+ serverProperties.getServlet().getContextPath()+ "/sys/common/acme/call/back?siteId="+tbSite.getId()+"\"";
            applyVo.setSiteId(tbSite.getId());
            applyVo.setCertId(certId);
            applyVo.setMode(finalCreatUseMode);
            applyVo.setDnsConfigId(dnsConfId);
            // 去掉前后方括号并按逗号分割
            String[] domainArray = domainListJoiner.split(",");
            // 输出数组内容
            for (String domain : domainArray) {
                AcmeDnsVo.TxtDomainValue tdv= applyVo.new TxtDomainValue();
                tdv.setDomain(domain);
                applyVo.getTvMap().put(domain,tdv);
            }
            applyVo.setNoticeCallBackCmd(noticeCallBackCmd);
            tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                    .eq("id",certId)
                    .set("status",TbCertifyStatusEnum.APPLYING.getId())
                    .set("acme_log","")
                    .set("site_id",tbSite.getId())
                    .set("remark","Letsencrypt-dns")
                    .set("src_type",finalCreatUseMode)
                    .set("apply_info",DataTypeConversionUtil.entity2jonsStr(applyVo))
            );
            if ( resId>0 && resId!=certId){
                tbcertifyDao.update(null,new UpdateWrapper<TbCertifyEntity>().eq("id",resId).set("status",TbCertifyStatusEnum.RE_APPLY.getId()));
            }
            this.InsertTaskLog("certId:"+certId,"申请Lets_encrypt证书[certId:"+certId+"]");
            applyCertThread();
        }else if (finalCreatUseMode==CertSrcTypeEnums.CertServerLetV2.getType() || finalCreatUseMode==CertSrcTypeEnums.CertServerZeroHttpV2.getType() ){
            //3 Cert Server http ||  // 5 Cert Server Zero
            R r1= this.certServerApply(tbSite,tbCertify,finalCreatUseMode,dnsConfId);
            logger.info("cert Server Apply 3|5 :"+r1.toJsonString());
        }else if (finalCreatUseMode==CertSrcTypeEnums.CertServerLetDnsV2.getType() || finalCreatUseMode==CertSrcTypeEnums.CertServerZeroDnsV2.getType()){
            //4 Cert Server Dns || 6  zero_dns
            R r1= this.certServerApply(tbSite,tbCertify,finalCreatUseMode,dnsConfId);
            logger.info("cert Server Apply 4|6 :"+r1.toJsonString());
        }else{
            logger.error(String.format("creatMode %d not define",finalCreatUseMode) );
        }

    }


    private R zeroSslCreateOrder(ZeroSslAPiCreateCertForm form){
        //检测是否重复提交
        TbCertifyEntity certify=null;
        if (null!=form.getSiteId()){
            //-1 0:待申请 1:成功 2失败 3自有
            Integer[] statusList={-1,0,2};
            certify=tbcertifyDao.selectOne(new QueryWrapper<TbCertifyEntity>()
                    .eq(null!=form.getCertId(),"id",form.getCertId())
                    .eq(null!=form.getId(),"id",form.getId())
                    .eq("site_id",form.getSiteId())
                    .eq("src_type",1)
                    .in("status",statusList)
                    .orderByDesc("id")
                    .select("id")
                    .last("limit 1")
            );
        }
        ZeroSslApiCertInfoVo cInfo= new ZeroSslApiCertInfoVo() ;
        cInfo.setDomains(form.getDomains());
        cInfo.setCertificate_validity_days(form.getCertificate_validity_days());

        R r=ZeroSslUtils.createCert(cInfo);
        if (null!=certify){
            certify.setApiOrderInfo(r.toJsonString());
            tbcertifyDao.updateById(certify);
        }
        logger.info("zero ssl order:"+r.toJsonString());
        return r;
    }

    /**
     * 申请 证书
     * @param certId
     */
    private void pushApplyCertificate(Integer siteId,Integer certId,int useMode,int resId,int dnsConfId) {
       this.applyCertHandle(ApplyTypeEnums.SITE,siteId,certId,useMode,resId,dnsConfId);
   }


    private Object JavaShellCmd(Integer mode,String cmd){
        if (1==mode){
            return  ShellUtils.runShell(cmd,false);
        }else if(2==mode){
             pushCmd2AllNode("normal",cmd);
        }
        return null;
    }


    private void pushCommandToNodeHand(Integer cmdId){
        try{
           pushCmdToNode(CommandEnum.getContentById(cmdId));
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //执行待完成任务
    @Override
    public void startOperaTask() {
        Map map=new HashMap();
        map.put("all_task","");
        pushByInputInfo(map);
    }

    @Override
    public void pushPurgeCache(List<PurgeVo> purgeVoList) {
        //Map map=new HashMap();
        //map.put("purge_cache",cmds);
        //pushByInputInfo(map);
        if (null==purgeVoList){
            return;
        }
        for (PurgeVo vo:purgeVoList){
            String targetServerName = vo.getServerName();
            if (targetServerName.contains("*")){
                targetServerName=targetServerName.replace("*",String.valueOf(System.currentTimeMillis()));
            }
            //logger.info(String.format("purge:%s",targetServerName)+":"+vo.getUrls());
            this.pusCmdUserMutThreadPurge2AllNode(String.format("purge:%s",targetServerName),vo.getUrls());
        }

    }

    @Override
    public boolean cdnPubCheckKeyValueRule(String key, Object value) {
        boolean useCheck=true;
        switch (key){
            case "net_waf":
                break;
            case "ssl_server":
                String attr_pvalue=value.toString();
                if(!IPUtils.isValidIPV4(attr_pvalue)){
                    throw  new RRException(key+"不是IPv4地址形式");
                }
                break;

            case "default_rule":
            case "web_rule_precise_detail":
                if (true){
                   List<LinkedHashMap> list=(List<LinkedHashMap>)value;
                    list.forEach(item->{
                        if (item.containsKey("value")){
                            SysWafRuleVo vo=DataTypeConversionUtil.string2Entity(item.get("value").toString(),SysWafRuleVo.class);
                            if (null==vo){
                                throw  new RRException("规则"+key+"格式有误");
                            }
                            vo.getRule().forEach(rule->{
                                if (rule.getContent().length()>240){
                                    throw  new RRException(rule.getContent().substring(0,16)+"......，字符长度超出");
                                }
                                //check reg
                                if (2==CheckHyperRegUtils.checkHyper(rule.getContent())){
                                    throw new RRException("["+vo.getRemark()+"] 编译规则失败");
                                }
                            });
                            if (null!= vo.getWaf_op().getParam() && vo.getWaf_op().getParam().length()>5){
                                throw  new RRException(vo.getWaf_op().getParam()+"，参数格式有误");
                            }
                        }
                    });
                }
                break;
            case "proxy_cache_path_dir":
                //PATH /data/cache
                if (useCheck){
                    String pattern = "[-_./a-zA-Z0-9]+";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                    if (!value.toString().endsWith("/cache")){
                        throw  new RRException(key+"格式有误,路径需以 /cache 结尾");
                    }
                    Map map=new HashMap();
                    map.put(COMMAND_CREATE_DIR.getName(),value);
                    pushByInputInfo(map);
                }
                break;
            case "worker_cpu_affinity":
            case "worker_processes":
                if (useCheck){
                    String pattern = "(auto)|(\\d+)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "resolver_timeout":
            case "worker_shutdown_timeout":
                if (useCheck){
                    String pattern = "\\d+s";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "error_log_level":
                if (useCheck){
                    String pattern = "debug|info|notice|warn|error|crit";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "resolver":
                if (useCheck){
                    String pattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s+\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s+valid=\\d+\\s+ipv6=(off|on)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "client_max_body_size":
            case "ants_waf_export_memory_buffer":
            case "proxy_cache_dir_max_size":
            case "proxy_cache_path_zone":
            case "proxy_max_temp_file_size":
            case "proxy_busy_buffers_size":
            case "proxy_buffer_size":
                if (useCheck){
                    String pattern = "\\d+(k|K|m|M|g|G)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "proxy_buffers":
            case "large_client_header_buffers":
                if (useCheck){
                    String pattern = "\\d+\\s+\\d+(k|m)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "proxy_cache_dir_levels":
                if (useCheck){
                    String pattern = "\\d+:\\d+";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "proxy_http_version":
                if (useCheck){
                    String pattern = "(1.0)|(1.1)|(2.0)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "proxy_buffering":
                if ( !(value.toString().equals("0") || value.toString().equals("1"))){
                    throw  new RRException(key+"取值为0或1");
                }
                break;
            case "vhost_server_url":
                if (useCheck){
                    String pattern = "http://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:9090";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "forbid_port":
            case "special_ports":
                if (useCheck){
                    String pattern = "[\\d\\|]+";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(value.toString());
                    if(!m.matches()){
                        throw  new RRException(key+"格式有误");
                    }
                }
                break;
            case "auto_pull_cache":
                if (true){
                    AutoPullCacheVo vo=DataTypeConversionUtil.string2Entity(value.toString(),AutoPullCacheVo.class);
                    if (null==vo){ throw  new RRException(key+"格式有误");}
                }
                break;
            default:
                break;
        }
        return true;
    }


    private R pushUpStatus2Stream(){
        String eMsg="";
        try{
            String streamKey=String.format("%s",RedisStreamType.UP_CONFIG.getName()) ;
            String fKey=String.format("%s","up");
            String xid=  redisUtils.streamXAdd(streamKey, fKey,"1");
            if (StringUtils.isNotBlank(xid)){
                StaticVariableUtils.lastSendXAddTaskStreamIdMap.put(streamKey,xid);
                return  R.ok();
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    private List<Integer> getAllGroupsIds(){
        List<Integer> result=new ArrayList<Integer>();
        result.add(0);
        List<CdnClientAreaEntity> list = cdnClientAreaDao.selectList(new QueryWrapper<CdnClientAreaEntity>().select("id"));
        if (null==list || list.isEmpty()){
            return result;
        }
        for (CdnClientAreaEntity entity : list) {
            result.add(entity.getId());
        }
        return result;
    }

    private R deleteLocalFile(int configId, String ip, String groupId, String fn ){
        String eMsg="";
        try{
            List<Integer> gIds=new ArrayList<>();
            if (StringUtils.isNotBlank(groupId)){
                gIds.addAll(getAllGroupsIds());
            }else{
                gIds.add(0);
            }
            for (Integer gId : gIds) {
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
                String gidStr=String.valueOf(gId);
                if (StringUtils.isNotBlank(gidStr)){
                    parentDir=parentDir.replace("{gid}",gidStr);
                    fileName=fileName.replace("{gid}",gidStr);
                }
                if (StringUtils.isNotBlank(fn)){
                    parentDir=parentDir.replace("{fn}",fn);
                    fileName=fileName.replace("{fn}",fn);
                }
                String locPath=parentDir+fileName;
                R r= FileUtils.deleteFile(locPath);
                if (1!=r.getCode()){
                    logger.error("delete file fail:"+locPath);
                }
            }
            return R.ok();
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }




    public R deleteSite(Integer id,DelConfMode opMode){
        if (id==null){
            return R.error("id is null");
        }
        TbSiteEntity siteEntity= tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",id).select("id,main_server_name").last("limit 1"));
        if (null!=siteEntity && StringUtils.isNotBlank(siteEntity.getMainServerName())){
           // String fn=String.format("%d_%s_",id,siteEntity.getMainServerName());
            String fn=String.format("%d",id);
            String groupId=makeFileFromResService.getGroupIdById(id.toString(),null,null);
            for (Integer cid:PushSetEnum.getSetIdsByGroup(PushSetEnum.SITE_CONF.getGroup())){
                R rd= deleteLocalFile(cid,null,groupId,fn);
                if (1!=rd.getCode()){
                    logger.info("Error deleting"+rd);
                }
            }
        }
        if (opMode.equals(DelConfMode.DELETE)){
            //2 删除site mysql attr
            tbSiteAttrDao.delete(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",id));
            //3 删除site mysql mut attr
            tbSiteMutAttrDao.delete(new QueryWrapper<TbSiteMutAttrEntity>().eq("site_id",id));
            //4删除证书 cert file
            //..
            //5 删除site mysql
            tbSiteDao.deleteById(id);
            logger.info("deleted  site " + id + " complete");
        }else if (opMode.equals(DelConfMode.CLOSE)){
            tbSiteDao.update(null,new UpdateWrapper<TbSiteEntity>().eq("id",id).set("status",0));
        }else if (opMode.equals(DelConfMode.LOCK)){
            tbSiteDao.update(null,new UpdateWrapper<TbSiteEntity>().eq("id",id).set("status",2));
        }else {
            logger.error("unknown "+opMode+"");
        }
        return pushUpStatus2Stream();
    }

    @Override
    public String getCacheValueByKey(String key) {
        String result="";
        try{
            result =  redisUtils.get(key);
            if (StringUtils.isNotBlank(result)){
                return  result;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        CacheKeyEnums item=  CacheKeyEnums.getGroupByKey(key);
        if (item==null){
            logger.error("can't find cache key:"+key);
            return result;
        }
        try{
            if (item.getGroup().equals("TbCdnPublicMutAttrEntity") ){
                TbCdnPublicMutAttrEntity publicMutAttr= publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                        .eq("pkey",item.getKeyName())
                        .last("limit 1")
                );
                if (null!=publicMutAttr && null!=publicMutAttr.getPvalue()){
                    result=publicMutAttr.getPvalue();
                    redisUtils.set(key,result,item.getExpireTime());
                }
            }else if (item.getGroup().equals("site_group_info")){
                JSONObject jsonObject=new JSONObject();
                List<TbSiteGroupEntity> siteGroupList=tbSiteGroupDao.selectList(new QueryWrapper<>());
                if (null!=siteGroupList && !siteGroupList.isEmpty()){
                     for (TbSiteGroupEntity siteGroupEntity : siteGroupList){
                         if (StringUtils.isNotBlank(siteGroupEntity.getSiteIds())){
                             for (String siteId:siteGroupEntity.getSiteIds().split(",")){
                                 jsonObject.put(siteId,String.format("%d:%s",siteGroupEntity.getId(),siteGroupEntity.getName()));
                             }
                         }
                     }
                }
                redisUtils.set(key,jsonObject.toJSONString(),item.getExpireTime());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public R saveCert2Db(CertCallbackForm form) {
        String eMsg="";
        try{
            //更新tb_certify
            TbSiteEntity site=tbSiteDao.selectById(form.getSiteId());
            if (null==site){
                logger.error("site ["+form.getSiteId()+"] is empty");
                return R.error("site ["+form.getSiteId()+"] is empty");
            }
            String pem=form.getPem().trim();
            String  key=form.getKey().trim();
            List<TbCertifyEntity> certifyList=tbcertifyDao.selectList(new QueryWrapper<TbCertifyEntity>()
                    .and(q->q.eq("site_id",form.getSiteId()).or().eq("common_name",site.getMainServerName()))
                    .orderByDesc("id")
            );
            if (null!=certifyList && certifyList.size()>0){
                for (int i = 0; i <certifyList.size() ; i++) {
                    TbCertifyEntity certify=certifyList.get(i);
                    if (0==i && 1==form.getCode()){
                        //success
                        TbCertifyObjVo certifyObjVo=new TbCertifyObjVo();
                        certifyObjVo.setPem_cert(pem);
                        certifyObjVo.setPrivate_key(key);
                        long notAfter= HashUtils.getCertEndTime(pem);
                        HashUtils.updateCertVoInfo(certifyObjVo);
                        String jsonObjectStr=DataTypeConversionUtil.entity2jonsStr(certifyObjVo);
                        certify.setObjInfo(jsonObjectStr);
                        certify.setNotAfter(notAfter);
                        certify.setRemark(CertSrcTypeEnums.getNameByType(certify.getSrcType()));
                        //0=待发行；1=成功；2=失败；3=自有
                        certify.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
                        tbcertifyDao.updateById(certify);
                    }else if (0==i && 1!=form.getCode()){
                        certify.setAcmeLog(DataTypeConversionUtil.entity2jonsStr(form));
                        certify.setStatus(TbCertifyStatusEnum.FAIL.getId());
                        tbcertifyDao.updateById(certify);
                    }else {
                        certify.setStatus(TbCertifyStatusEnum.RE_APPLY.getId());
                        tbcertifyDao.updateById(certify);
                    }
                }
            }

            if (1==form.getCode()){
                //推送保存SSL
                SaveSiteSslFormVo sVo=new SaveSiteSslFormVo(String.valueOf(form.getSiteId()),pem,key);
                R r1=applicationContext.getBean(CdnSiteController.class).saveAttr(DataTypeConversionUtil.entity2map(sVo));
                //logger.info(DataTypeConversionUtil.entity2jonsStr(sVo));
                //String saveSiteAttrUri=String.format("http://127.0.0.1:%s%s/sys/common/save/site/attr",serverProperties.getPort(), serverProperties.getServlet().getContextPath());
                //String result=HttpRequest.sendPostJsonStr(saveSiteAttrUri,DataTypeConversionUtil.entity2jonsStr(sVo));
                logger.info("push_save_ssl:"+r1.toJsonString());
                return R.ok().put("data",r1.toJsonString());
            }
            return R.ok();
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    @Override
    public R batSIteCertReissued(Long userId, BatReissuedForm form) {
        ValidatorUtils.validateEntity(form);
        String[] siteIds=form.getIds().split(",");
        if (1==siteIds.length){
            TbSiteEntity siteEntity= tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",siteIds[0]).eq(null!=userId,"user_id",userId).select("id,main_server_name").last("limit 1"));
            if (null==siteEntity){
                return R.error("siteId is null");
            }
           return   pushApplyCertificateBySiteId(Integer.parseInt(siteIds[0]),0,form.getUseMode(),0,form.getDnsConfigId());
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (String siteId:siteIds){
                        TbSiteEntity siteEntity= tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",siteId).eq(null!=userId,"user_id",userId).select("id,main_server_name").last("limit 1"));
                        if (null==siteEntity){
                            continue;
                        }
                        try {
                            pushApplyCertificateBySiteId(Integer.parseInt(siteId),0,form.getUseMode(),0,form.getDnsConfigId());
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        return R.ok();
    }


    public R deleteStream(Integer id,DelConfMode opMode){
        if (id==null){
            return R.error("id is null");
        }
        String fn=String.format("%d",id);
        String gid=makeFileFromResService.getGroupIdById(null,id.toString(),null);
        for (Integer cid:PushSetEnum.getSetIdsByGroup(PushSetEnum.STREAM_CONF.getGroup())){
            deleteLocalFile(cid,null,gid,fn);
        }
        if (opMode.equals(DelConfMode.DELETE)){
            tbStreamProxyDao.deleteById(id);
            tbStreamProxyAttrDao.delete(new QueryWrapper<TbStreamProxyAttrEntity>()
                    .eq("stream_id",id)
            );
        }else if (opMode.equals(DelConfMode.CLOSE)){
            tbStreamProxyDao.update(null,new UpdateWrapper<TbStreamProxyEntity>().eq("id",id).set("status",0));
        }else {
            logger.error("unknown "+opMode+"");
        }
        return pushUpStatus2Stream();
    }

    public R deleteRewrite(Integer id,DelConfMode opMode){
        if (id==null){
            return R.error("id is null");
        }
        String fn=String.format("%d",id);
        String gid=makeFileFromResService.getGroupIdById(null,null,id.toString());
        for (Integer cid:PushSetEnum.getSetIdsByGroup(PushSetEnum.REWRITE_CONF.getGroup())){
            deleteLocalFile(cid,null,gid,fn);
        }
        if (opMode.equals(DelConfMode.DELETE)){
            tbRewriteDao.deleteById(id);
        }else if (opMode.equals(DelConfMode.CLOSE)){
            tbRewriteDao.update(null,new UpdateWrapper<TbRewriteEntity>().eq("id",id).set("status",0));
        }else {
            logger.error("unknown "+opMode+"");
        }
        return pushUpStatus2Stream();
    }

    /**
     * 推送删除key,先删除redis 数据，推送删除stream,最后操作持久存储mysql
     * @param addKey
     * @param delKey
     */
    private void pushDeleteRdsKey(String type, Integer id, String addKey, String delKey,DelConfMode opMode){
        if (redisUtils.getSMembersSize(addKey+SET_SUFFIX)>0){
            //将xxx_ID_SET 集体复制到xxx_ID_DEL_SET
            long r=redisUtils.setUnionAndStore(addKey+SET_SUFFIX,delKey+SET_SUFFIX);
            if (r<0){
                logger.error("set Union And Store ["+delKey+SET_SUFFIX+"]    fail");
                return;
            }
            //删除SITE_ID_SET集合成员对应的文件路径@值键值对及当前集合体
            boolean b= redisUtils.setDeleteMember(addKey+SET_SUFFIX);
            if(!b){
                logger.error("Delete Member ["+delKey+SET_SUFFIX+"]  fail");
                return;
            }
            //设置xxx_ID_DEL_SET集合过期时间
            b= redisUtils.keyExpire(delKey+SET_SUFFIX, EXPIRE_TIME);
            if (!b){
                logger.error("key Expire ["+delKey+SET_SUFFIX+"]  fail");
                return;
            }
            String xid= rdsXAddConfigByGroupIds(RDS_DEL_MODE_DEL,"-1",delKey+SET_SUFFIX);
            if (StringUtils.isBlank(xid)){
                logger.error("rds XAdd Config By Group Ids  ["+delKey+SET_SUFFIX+"]  fail");
                return;
            }
        }
        switch (type){
            case "site":
                if (true){
                    if (opMode.equals(DelConfMode.DELETE)){
                        //2 删除site mysql attr
                        tbSiteAttrDao.delete(new QueryWrapper<TbSiteAttrEntity>().eq("site_id",id));
                        //3 删除site mysql mut attr
                        tbSiteMutAttrDao.delete(new QueryWrapper<TbSiteMutAttrEntity>().eq("site_id",id));
                        //4删除证书 cert file
                        //..
                        //5 删除site mysql
                        tbSiteDao.deleteById(id);
                        logger.info("deleted  site " + id + " complete");
                    }else if (opMode.equals(DelConfMode.CLOSE)){
                        tbSiteDao.update(null,new UpdateWrapper<TbSiteEntity>().eq("id",id).set("status",0));
                    }else {
                        logger.error("unknown "+opMode+"");
                    }
                }
                break;
            case "stream":
                if (true){
                    if (opMode.equals(DelConfMode.DELETE)){
                        tbStreamProxyDao.deleteById(id);
                        tbStreamProxyAttrDao.delete(new QueryWrapper<TbStreamProxyAttrEntity>()
                                .eq("stream_id",id)
                        );
                    }else if (opMode.equals(DelConfMode.CLOSE)){
                        tbStreamProxyDao.update(null,new UpdateWrapper<TbStreamProxyEntity>().eq("id",id).set("status",0));
                    }else {
                        logger.error("unknown "+opMode+"");
                    }
                }
                break;
            case "rewrite":
                if (true){
                    if (opMode.equals(DelConfMode.DELETE)){
                        tbRewriteDao.deleteById(id);
                    }else if (opMode.equals(DelConfMode.CLOSE)){
                        tbRewriteDao.update(null,new UpdateWrapper<TbRewriteEntity>().eq("id",id).set("status",0));
                    }else {
                        logger.error("unknown "+opMode+"");
                    }
                }
                break;
            default:
                logger.error("unknown type["+type+"] delete key set!");
                break;
        }

    }

    private void updateInitStatusMsg(){
        // String key=String.format("%s-feedback",client.getClientIp());
        Set<String> keys=redisUtils.scanAll("*-feedback");
        for(String key:keys){
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("init",System.currentTimeMillis());
            redisUtils.set(key,jsonObject.toJSONString());
        }
    }

    private Object opTaskV2Handle(Map<String, String> map) {
        List<Object > result=new ArrayList<>();
        for(Map.Entry entry:map.entrySet()){
            String key=entry.getKey().toString();
            String value=entry.getValue().toString();
            PushTypeEnum iType=PushTypeEnum.getEnumByKey(key);
            if (null==iType){
                logger.info("unknown type");
                continue;
            }
            logger.info(entry.toString());
            StaticVariableUtils.curPushTaskName=iType.getName();
            switch (iType){
                case ALL_TASK:
                    break;
                case INIT_NODE:
                case INIT_ALL_NODE:
                    //all_crate_and_push
                    if (true){
                        long startTm=System.currentTimeMillis();
                        this.updateInitStatusMsg();
                        this.initCleanRdsData(null);
                        //20241121 del-dir_close
                        //final String dirPath="/usr/ants/cdn-api/nginx-config/";
                        //R r1=FileUtils.deleteDirectory(dirPath);
                        //logger.info("delete directory: "+dirPath+  r1.toString());
                        makeFileFromResService.sentNginxConf("");
                        makeFileFromResService.sentCacheConf("");
                        logger.info("init_s13:"+(System.currentTimeMillis()-startTm));
                        makeFileFromResService.sentEtcHttpConf();
                        makeFileFromResService.sentEtcCertVerifyConf();
                        makeFileFromResService.sendEtcInjRegx();
                        makeFileFromResService.sendPubIndexWafTemplate();
                        makeFileFromResService.sentNginxDefaultIndexHtml();
                        logger.info("init_s14:"+(System.currentTimeMillis()-startTm));
                        makeFileFromResService.sentNginxDefaultErrPageHtml();
                        makeFileFromResService.sentHttpDefaultWafConf();
                        makeFileFromResService.sentEtcPubRegRuleMConf();
                        logger.info("init_s15:"+(System.currentTimeMillis()-startTm));
                        makeFileFromResService.sentEtcWhiteIpv4Conf();
                        makeFileFromResService.sentEtcBlackIpv4Conf();
                        makeFileFromResService.sentEtcWhiteIpv6Conf();
                        makeFileFromResService.sentEtcBlackIpv6Conf();
                        logger.info("init_s16:"+(System.currentTimeMillis()-startTm));
                        makeFileFromResService.sentConfForwardMConf("");
                        makeFileFromResService.sentConfRewriteMConf("");
                        makeFileFromResService.sentConfSiteMConf("");
                        logger.info("init_s17:"+(System.currentTimeMillis()-startTm));
                        logger.info("build file complete");
                        //startTm=System.currentTimeMillis();
                        this.initCleanNodeConf();
                        logger.info("init_s18:"+(System.currentTimeMillis()-startTm));
                        //makeFileFromResService.pushAllConfFile("","",PushSetEnum.ADD_NOCHECK_PUSH);
                        logger.info("init_s19:"+(System.currentTimeMillis()-startTm));
                        //makeFileFromResService.pushAllConfFile("ETC_WAF_IP_SET","","");
                        //logger.info("init_s20:"+(System.currentTimeMillis()-startTm));
                        logger.info("inti xadd complete");
                        R r=pushUpStatus2Stream();
                        result.add(r);
                        String  logMsg=String.format("初始化，耗时【%d】ms",(System.currentTimeMillis()-startTm));
                        InsertPushLog(logMsg,"init");
                    }
                    break;
                case ALL_FILE:
                    //all_push
                    //makeFileFromResService.pushAllConfFile("","","");
                    this.updateInitStatusMsg();
                    R r=pushUpStatus2Stream();
                    result.add(r);
                    logger.info("push complete");
                    break;
                case ALL_FILE_2_NODE:
                case NODE_CUSTOM:
                    makeFileFromResService.sentNginxConf("");
                    makeFileFromResService.sentCacheConf("");
                    //makeFileFromResService.pushAllConfFile("PUB_SINGLE_SET","","");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUBLIC_NGINX_CONF:
                    makeFileFromResService.sentNginxConf("");
                    //makeFileFromResService.pushAllConfFile("PUB_SINGLE_SET","","");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUBLIC_HTTP_CONF:
                    makeFileFromResService.sentEtcHttpConf();
                    makeFileFromResService.sentEtcCertVerifyConf();
                    makeFileFromResService.sendEtcInjRegx();
                    makeFileFromResService.sendPubIndexWafTemplate();
                    //makeFileFromResService.pushAllConfFile("PUB_CONF_SET","","");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUBLIC_CACHE_CONF:
                case PUBLIC_PUSH_ETC_VHOST_CONF:
                case PUBLIC_PUB_WAF_SELECT:
                case PUBLIC_HTTP_DEFAULT_WAF:
                case PUBLIC_CHUNK:
                    makeFileFromResService.sentNginxConf("");
                    makeFileFromResService.sentCacheConf("");
                    makeFileFromResService.sentEtcHttpConf();
                    makeFileFromResService.sentEtcCertVerifyConf();
                    makeFileFromResService.sendEtcInjRegx();
                    makeFileFromResService.sendPubIndexWafTemplate();
                    makeFileFromResService.sentNginxDefaultIndexHtml();
                    makeFileFromResService.sentNginxDefaultErrPageHtml();
                    makeFileFromResService.sentHttpDefaultWafConf();
                    makeFileFromResService.sentEtcPubRegRuleMConf();
                    makeFileFromResService.sentEtcWhiteIpv4Conf();
                    makeFileFromResService.sentEtcBlackIpv4Conf();
                    makeFileFromResService.sentEtcWhiteIpv6Conf();
                    makeFileFromResService.sentEtcBlackIpv6Conf();
                    //makeFileFromResService.pushAllConfFile("PUB_SINGLE_SET,PUB_CONF_SET,PUB_WAF_SET,ETC_WAF_IP_SET","","");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUBLIC_PUB_ERR_PAGE_CONF:
                    makeFileFromResService.sentNginxDefaultIndexHtml();
                    makeFileFromResService.sentNginxDefaultErrPageHtml();
                    makeFileFromResService.sentHttpDefaultWafConf();

                    makeFileFromResService.sendPubIndexWafTemplate();
                    //makeFileFromResService.pushAllConfFile("PUB_CONF_SET","","");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUBLIC_NFT_WAF:
                case IP_TRIE:
                case IP_TRIE_AND_NFT:
                    //this.saveEtcWafIpSetV2(true);
                    makeFileFromResService.sentEtcPubRegRuleMConf();
                    makeFileFromResService.sentEtcWhiteIpv4Conf();
                    makeFileFromResService.sentEtcBlackIpv4Conf();
                    makeFileFromResService.sentEtcWhiteIpv6Conf();
                    makeFileFromResService.sentEtcBlackIpv6Conf();
                    makeFileFromResService.sendEtcInjRegx();
                    makeFileFromResService.sendPubIndexWafTemplate();
                    //makeFileFromResService.pushAllConfFile("PUB_WAF_SET,ETC_WAF_IP_SET","",PushSetEnum.ADD_NOCHECK_PUSH);
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case SITE_CHUNK:
                case SITE_SSL:
                case SITE_WAF:
                case SITE_CONF:
                case SITE_SELECT_CHUNK:
                case SITE_HTML_FILE :
                    //ids==value
                    makeFileFromResService.sentConfSiteMConf(value);
                    //makeFileFromResService.pushAllConfFile("SITE_###id###_SET",value,"");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUSH_SUIT_SERVICE:
                    this.pushSuitServerHandler(value);
                    break;
                case STREAM_CONF:
                    //ids
                    makeFileFromResService.sentConfForwardMConf(value);
                    //makeFileFromResService.pushAllConfFile("STREAM_###id###_SET",value,"");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case PUSH_REWRITE_CONF:
                    //ids
                    makeFileFromResService.sentConfRewriteMConf(value);
                    //makeFileFromResService.pushAllConfFile("REWRITE_###id###_SET",value,"");
                    r=pushUpStatus2Stream();
                    result.add(r);
                    break;
                case COMMAND:
                    this.pushCommandToNodeHand(Integer.parseInt(value));
                    break;

                case APPLY_CERTIFICATE:
                    if (StringUtils.isNotBlank(value)){
                        for (String certId:value.split(",")){
                            pushApplyCertificate(null,Integer.parseInt(certId),-1,0,0);
                        }
                    }
                    break;
                case APPLY_CERTIFICATE_V2:
                    if (StringUtils.isNotBlank(value)){
                        for (String siteId:value.split(",")){
                            //重签
                            pushApplyCertificateBySiteId(Integer.parseInt(siteId),0,-1,0,0);
                        }
                    }
                    break;
                case APPLY_CERTIFICATE_REISSUED:
                    if (StringUtils.isNotBlank(value)){
                        for (String siteId:value.split(",")){
                            //强制重签
                            pushApplyCertificateBySiteId(Integer.parseInt(siteId),1,-1,0,0);
                        }
                    }
                    break;
                case COMMAND_CREATE_DIR:
                    if(StringUtils.isNotBlank(value)){
                        String cmd="mkdir -p "+value;
                        this.pusCmdUserMutThreadPurge2AllNode("normal",cmd);
                    }
                    break;
                case COMMAND_DELETE_DIR:
                    deleteNodeDir(value);
                    break;
                case CLEAN_DEL_SITE:
                    if(isIntegerOrIntegerList(value)){
                        //String addKey="SITE_"+value+"_SET";
                        //String delKey="SITE_"+value+"_DEL_SET";
                        //this.pushDeleteRdsKey("site",Integer.valueOf(value),addKey,delKey,DelConfMode.DELETE);
                        r= this.deleteSite(Integer.valueOf(value),DelConfMode.DELETE);
                        result.add(r);
                    }
                    break;
                case CLEAN_DEL_STREAM_CONF:
                    if(isIntegerOrIntegerList(value)){
                        //String addKey="STREAM_"+value+"_SET";
                        //String delKey="STREAM_"+value+"_DEL_SET";
                        //this.pushDeleteRdsKey("stream",Integer.valueOf(value),addKey,delKey,DelConfMode.DELETE);
                        r= this.deleteStream(Integer.valueOf(value),DelConfMode.DELETE);
                        result.add(r);
                    }
                    break;
                case CLEAN_DEL_REWRITE_CONF:
                    if(isIntegerOrIntegerList(value)){
                        //String addKey="REWRITE_"+value+"_SET";
                        //String delKey="REWRITE_"+value+"_DEL_SET";
                        //this.pushDeleteRdsKey("rewrite",Integer.valueOf(value),addKey,delKey,DelConfMode.DELETE);
                        r= this.deleteRewrite(Integer.valueOf(value),DelConfMode.DELETE);
                        result.add(r);
                    }
                    break;
                case CLEAN_STOP_SITE:
                    if(isIntegerOrIntegerList(value)){
                        //String addKey="SITE_"+value+"_SET";
                        //String delKey="SITE_"+value+"_DEL_SET";
                        //this.pushDeleteRdsKey("site",Integer.valueOf(value),addKey,delKey,DelConfMode.CLOSE);
                        r= this.deleteSite(Integer.valueOf(value),DelConfMode.CLOSE);
                        result.add(r);
                    }
                    break;
                case CLEAN_STOP_STREAM_CONF:
                    if(isIntegerOrIntegerList(value)){
                        //String addKey="STREAM_"+value+"_SET";
                        //String delKey="STREAM_"+value+"_DEL_SET";
                        //this.pushDeleteRdsKey("stream",Integer.valueOf(value),addKey,delKey,DelConfMode.CLOSE);
                        r= this.deleteStream(Integer.valueOf(value),DelConfMode.CLOSE);
                        result.add(r);
                    }
                    break;
                case CLEAN_STOP_REWRITE_CONF:
                    if(isIntegerOrIntegerList(value)){
                        //String addKey="REWRITE_"+value+"_SET";
                        //String delKey="REWRITE_"+value+"_DEL_SET";
                        //this.pushDeleteRdsKey("rewrite",Integer.valueOf(value),addKey,delKey,DelConfMode.CLOSE);
                        r= this.deleteRewrite(Integer.valueOf(value),DelConfMode.CLOSE);
                        result.add(r);
                    }
                    break;
                case CLEAN_CLOSE_SUIT_SERVICE:
                    closeSuitServiceHandle(value);
                    pushUpStatus2Stream();
                    break;
                case SHELL_ANTS_CMD_TO_MAIN:
                    if(StringUtils.isNotBlank(value)){
                        Object c1= JavaShellCmd(1,value);
                        result.add(c1);
                    }
                    break;
                case SHELL_ANTS_CMD_TO_NODE:
                    if(StringUtils.isNotBlank(value)){
                        Object c2= JavaShellCmd(2,value);
                        result.add(c2);
                    }
                    break;
                case PUSH_INTERCEPT_IP_TO_ALL_NODE:
                    break;
                case RELEASE_INTERCEPT_IP:
                    this.releaseInterceptIp(value);
                    break;
                case REBOOT_NODE:
                    if (StringUtils.isNotBlank(value)){
                        for (String clientIp:value.split(",")){
                            String cmd="current_ip=$(curl -s https://ipinfo.io/ip) && if [ \"$current_ip\" == \""+clientIp+"\" ]; then reboot ; else echo \"$current_ip\"; fi\n";
                            this.pushCmd2AllNode("normal",cmd);
                        }
                    }
                    break;
                case NODE_SYS_WS_STATUS_ON:
                    if (QuerySysAuth.WS_MODULE_FLAG){
                        this.pushNodeSysWsStatus(value,1);
                    }
                    break;
                case  NODE_SYS_WS_STATUS_OFF:
                    if (QuerySysAuth.WS_MODULE_FLAG){
                        this.pushNodeSysWsStatus(value,0);
                    }
                    break;
                case NODE_SYS_WS_SPECIAL_PORTS_RESET:
                    if (QuerySysAuth.WS_MODULE_FLAG){
                        this.pushWsSpecialPortsReset();
                    }
                    break;
                case AI_MODEL_PUSH:
                    //redisUtils.setAdd(PushSetEnum.AI_MODEL_CATBOOST_BIN.getGroup()+SET_SUFFIX,PushSetEnum.AI_MODEL_CATBOOST_BIN.getTemplatePath());
                    //makeFileFromResService.pushAllConfFile(PushSetEnum.AI_MODEL_CATBOOST_BIN.getGroup(),"",PushSetEnum.ADD_NOCHECK_PUSH);
                    pushUpStatus2Stream();
                    break;
                case NODE_RESTART_NGINX:
                    if (StringUtils.isNotBlank(value)){
                        this.push2nodeCmdV2(value,"systemctl restart nginx");
                    }
                    break;
                default:
                    logger.error(" unknown op ["+key+"]");
                    break;
            }
        }
        return  result;
    }

    private Object opTaskV2(Map<String, String> map){
       try{
           return opTaskV2Handle(map);
       } catch (RRException e2){
           logger.error("opTaskV2:RRException:"+e2.getMessage());
       } catch (Exception e){
           e.printStackTrace();
           logger.error("opTaskV2: Exception:"+e.getMessage());
       }
        return null;
    }

    @Override
    public Object pushByInputInfo(Map<String, String> map) {
        if(map.containsKey("shell_ants_cmd_1") ||
                map.containsKey("shell_ants_cmd_2") ||
                map.containsKey("apply_certificate") ||
                map.containsKey("apply_certificate_v2")){
            return opTaskV2Handle(map);
        }
        String taskKey=System.currentTimeMillis()+""+StaticVariableUtils.pushTaskStreamMap.size();
        for (String key:map.keySet()){
            if (key.equals(INIT_ALL_NODE.getName())){
                StaticVariableUtils.pushTaskStreamMap.clear();
                logger.info("clean--init--all");
            }
        }
        ConcurrentHashMap cMap=new ConcurrentHashMap();
        cMap.putAll(map);
        StaticVariableUtils.pushTaskStreamMap.put(taskKey,cMap);
        logger.info("TASK:"+taskKey+"--->"+DataTypeConversionUtil.map2json(map).toJSONString());
        if (!StaticVariableUtils.taskTheadFlag){
            operaLinkTaskV2();
        }
        return null;
    }

    @Override
    public void operaLinkTaskV2() {
             new Thread(new Runnable() {
               @Override
               public void run() {
                   while (true){
                       try{
                           StaticVariableUtils.taskTheadFlag=true;
                           if (StaticVariableUtils.pushTaskStreamMap.size()>0){
                               Iterator<Map.Entry<String, ConcurrentHashMap>> iterator = StaticVariableUtils.pushTaskStreamMap.entrySet().iterator();
                               while (iterator.hasNext()){
                                   ConcurrentHashMap.Entry<String,ConcurrentHashMap> entry = iterator.next();
                                   iterator.remove();
                                   opTaskV2(entry.getValue());
                               }
                           }else{
                               ////logger.debug("no task");
                               StaticVariableUtils.curPushTaskName="";
                           }
                           try{
                               Thread.sleep(2000);
                           }catch (Exception e2){
                               e2.printStackTrace();
                           }
                       }catch (Exception e){
                           e.printStackTrace();
                       }
                   }
               }
           }).start();
           //logger.debug(Thread.currentThread().getName());
           // RedisConnectionFactory redisConnectionFactory=redisUtils.getRedisConnectionFactory();

    }



}
