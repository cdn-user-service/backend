package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.exception.RRException;
import io.ants.common.other.QueryAnts;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.TbCertifyEntity;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.CdnClientAreaEntity;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.entity.CdnClientGroupChildConfEntity;
import io.ants.modules.sys.enums.ClientStatusEnum;
import io.ants.modules.sys.enums.CommandEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.enums.RedisStreamType;
import io.ants.modules.sys.form.CdnClientForm;
import io.ants.modules.sys.form.CdnClientQueryForm;
import io.ants.modules.sys.form.ChangeMasterForm;
import io.ants.modules.sys.service.AntsAuthService;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.vo.AntsAuthInfoVo;
import io.ants.modules.sys.vo.NodePushStreamFeedBackInfoVo;
import io.ants.modules.sys.vo.NodeVersionVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AntsAuthServiceImpl implements AntsAuthService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Integer HttpMethodGet=0;
    private final Integer HttpMethodPost=1;
    static   Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private SysUserDao sysUserDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private CdnClientAreaDao cdnClientAreaDao ;
    @Autowired
    private TbCertifyDao tbCertifyDao;
    @Autowired
    private CdnClientGroupChildConfDao groupChildConfDao;

    private void restartScan(){
        ShellUtils.execShell("pkill port-scan");
        ShellUtils.execShell("pkill ants_port_scan");
    }

    private void updateNodeIpList(){
        StaticVariableUtils.synNodeIpTimeTemp=System.currentTimeMillis();
        if (StaticVariableUtils.NodeIpList.isEmpty()){
            StaticVariableUtils.NodeIpList.add("0.0.0.0");
        }
        List<CdnClientEntity>list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .select("client_ip")
                .notIn("client_ip",StaticVariableUtils.NodeIpList)
        );
        list.forEach(item->{
            if(StringUtils.isNotBlank(item.getClientIp())){
                //logger.info(item.getClientIp());
                if(!StaticVariableUtils.NodeIpList.contains(item.getClientIp())){
                    StaticVariableUtils.NodeIpList.add(item.getClientIp());
                }
            }
        });

    }

    /**
     * 获取授权
     * @return
     */
    @Override
    public String getAuthInfo() {
        if( StaticVariableUtils.authInfoMap.containsKey("time") ){
            if(Math.abs(System.currentTimeMillis()-(long)StaticVariableUtils.authInfoMap.get("time"))<60*1000){
                if(StaticVariableUtils.authInfoMap.containsKey("data")){
                    return StaticVariableUtils.authInfoMap.get("data").toString();
                }
            }
        }
        String url= QueryAnts.getAuthAddress();
        String param="func=QueryAccess";
        try{
            String resData=QueryAnts.antsCdnHttpRequest(HttpMethodGet,url,param);
            AntsAuthInfoVo antsAuthInfoVo=DataTypeConversionUtil.string2Entity(resData,AntsAuthInfoVo.class);
            if(StringUtils.isNotBlank(resData) && null!=antsAuthInfoVo){
                StaticVariableUtils.authInfoMap.put("time",System.currentTimeMillis());
                StaticVariableUtils.authInfoMap.put("data",resData);
                JSONObject jsonData=JSONObject.parseObject(resData);
                if(jsonData.containsKey("master_ip") && jsonData.containsKey("endtime") && jsonData.containsKey("goods_code")){
                    StaticVariableUtils.goods_code=antsAuthInfoVo.getGoods_code();
                    StaticVariableUtils.authMasterIp =antsAuthInfoVo.getMaster_ip();
                    StaticVariableUtils.authEndTime =Integer.parseInt(antsAuthInfoVo.getEndtime());
                    if (StringUtils.isNotBlank(QueryAnts.AUTH_GOODS_CODE_HEAD) && !StaticVariableUtils.goods_code.startsWith(QueryAnts.AUTH_GOODS_CODE_HEAD)){
                        throw new  RRException("JAR ERROR");
                    }
                    StaticVariableUtils.checkNodeInputToken= HashUtils.md5ofString("check_"+StaticVariableUtils.authMasterIp);
                    if (StringUtils.isNotBlank(antsAuthInfoVo.getExclusive_mode())){
                        String[] exclusiveModeStr=antsAuthInfoVo.getExclusive_mode().split(",");
                        StaticVariableUtils.exclusive_modeList.addAll(Arrays.asList(exclusiveModeStr));
                    }
                    redisUtils.set("public:master:api:dnsrecord",StaticVariableUtils.goods_code,-1);
                }
                String r_version=antsAuthInfoVo.getVersion();
                String l_version=StaticVariableUtils.JAR_VERSION;
                if(StringUtils.isNotBlank(l_version)){
                    if(!r_version.equals(l_version)){
                        this.updateRemoteVersion(antsAuthInfoVo.getMaster_ip(),l_version);
                    }
                }
                this.updateNodeIpList();
                return resData;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从ANTS 获取所有节点
     */
    private String getNodeListFromAnts() {
        if(StaticVariableUtils.authNodeListMap.containsKey("time") ){
            if(Math.abs(System.currentTimeMillis()-(long)StaticVariableUtils.authNodeListMap.get("time"))<60*1000){
                if(StaticVariableUtils.authNodeListMap.containsKey("data")){
                    return StaticVariableUtils.authNodeListMap.get("data").toString();
                }
            }
        }
        String url= QueryAnts.getAuthAddress();
        String param="func=QueryNodeList";
        try{
            String resData=QueryAnts.antsCdnHttpRequest(HttpMethodGet,url,param);
            if(StringUtils.isNotBlank(resData)){
                StaticVariableUtils.authNodeListMap.put("time",System.currentTimeMillis());
                StaticVariableUtils.authNodeListMap.put("data",resData);
                this.updateNodeIpList();
                return resData;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void updateRegNodeClientInfo(String ip){
        if (!QueryAnts.useMainRegInfo()){
            return;
        }
        List<CdnClientEntity>list =cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_ip",ip)
        );
        list.forEach(item->{
            //locClient.setStatus(ClientStatusEnum.ALREADY_REGISTER.getId());
            item.setStatus(ClientStatusEnum.ALREADY_REGISTER.getId());
            item.setEffectiveEndingTime(DateUtils.stamp2date(StaticVariableUtils.authEndTime) );
            item.setRegInfo(StaticVariableUtils.authInfoMap.get("data").toString());
            cdnClientDao.updateById(item);
        });
    }

    @Override
    public String regNode(String NodeIp) {
        String url= QueryAnts.getAuthAddress();
        String params="func=EnableNode&ip="+NodeIp;
        try {
            StaticVariableUtils.authNodeListMap.clear();
            String resData=QueryAnts.antsCdnHttpRequest(HttpMethodPost,url,params);
            //同步节点
            this.updateRegNodeClientInfo(NodeIp);
            this.SyncAntsNodeListThread();
            if(StringUtils.isNotBlank(resData)){
               //logger.debug("resData:"+resData);
                this.updateNodeIpList();
                return  resData;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  "";
    }

    @Override
    public String deleteNode(String NodeIp) {
        String url= QueryAnts.getAuthAddress();
        String params="func=DeleteNode&ip="+NodeIp;
        try {
            StaticVariableUtils.authNodeListMap.clear();
            String resData=QueryAnts.antsCdnHttpRequest(HttpMethodPost,url,params);
            if(StringUtils.isNotBlank(resData)){
                this.updateNodeIpList();
                this.updatePrometheusConf();
                return  resData;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }

    private void updateRemoteVersion(String NodeIp, String version){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= QueryAnts.getAuthAddress();
                    String params="func=UpdateVersionInfo&ip="+NodeIp+"&version="+version;
                    HttpRequest.sendPost(url,params);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    StaticVariableUtils.authNodeListMap.clear();
                }

            }
        }).start();
    }

    /**
     * 迁移主控
     * @param newMasterIp
     */
    private void migrateMaster(String newMasterIp){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= QueryAnts.getAuthAddress();
                    String params="func=changeMasterIp&new_master_ip="+newMasterIp;
                    String  res= HttpRequest.sendPost(url,params);
                    logger.info(res);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    StaticVariableUtils.authNodeListMap.clear();
                }
            }
        }).start();
    }

    private NodeVersionVo getVersionObj(String v){
        NodeVersionVo vo =new NodeVersionVo();
        if (StringUtils.isNotBlank(v)){
            //"1.27|{\"nginx_version\":\"1.19.9\",\"ants_waf\":\"2.30\"}"
            String[] angentVersionAndNgxAdnWafVersion= v.split("\\|");
            vo.setAgentVersion(angentVersionAndNgxAdnWafVersion[0]);
            if (angentVersionAndNgxAdnWafVersion.length>=2){
                JSONObject object=DataTypeConversionUtil.string2Json(angentVersionAndNgxAdnWafVersion[1]);
                if (object.containsKey("ants_waf")){
                    vo.setNginxWafVersion(object.getString("ants_waf"));
                }
                if (object.containsKey("nginx_version")){
                    vo.setNginxVersion(object.getString("nginx_version"));
                }
            }
        }
        return vo;
    }


    private Map<String,Object> getSynRemoteNodeList(){
        String info=this.getNodeListFromAnts();
       //logger.debug("getSynRemoteNodeList:"+info);
        Map<String,Object> ipsMap=new HashMap<>();
        if(StringUtils.isNotBlank(info)){
            JSONArray jsonArray=DataTypeConversionUtil.string2JsonArray(info);
            for (int i = 0; i <jsonArray.size() ; i++) {
                JSONObject jsonRemoteObject=jsonArray.getJSONObject(i);
                //ip,goods,version,addtime,endtime
                if(jsonRemoteObject.containsKey("ip")){
                    String ip=jsonRemoteObject.getString("ip");
                    ipsMap.put(ip,jsonRemoteObject);
                    CdnClientEntity locClient=cdnClientDao.selectOne(new QueryWrapper<CdnClientEntity>()
                            .eq("client_ip",ip)
                            .last("limit 1")
                    );
                    if(null==locClient){
                        //ANTS 有，local 无
                        //CdnClientEntity new_client=new CdnClientEntity();
                        //new_client.setClientIp(ip);
                        //new_client.setClientType(1);
                        //cdnClientDao.insert(new_client);
                        //locClient=new_client;
                    }
                    if(null!=locClient){
                        //ANTS 有，local 有
                        if(StringUtils.isBlank(locClient.getArea()) || StringUtils.isBlank(locClient.getLine())){
                            logger.error("--area or line is empty--");
                            continue;
                        }
                        locClient.setRegInfo(jsonRemoteObject.toJSONString());
                        if(jsonRemoteObject.containsKey("addtime")){
                            if(StringUtils.isNotBlank(jsonRemoteObject.getString("addtime"))){
                                String sDate= DateUtils.stampToDate(jsonRemoteObject.getString("addtime")+"000");
                                Date s=DateUtils.stringToDate(sDate,DateUtils.DATE_TIME_PATTERN);
                                locClient.setEffectiveStartTime(s);
                            }
                        }
                        if(jsonRemoteObject.containsKey("endtime")){
                            if(StringUtils.isNotBlank(jsonRemoteObject.getString("endtime"))){
                                String eDate= DateUtils.stampToDate(jsonRemoteObject.getString("endtime")+"000");
                                Date e=DateUtils.stringToDate(eDate,DateUtils.DATE_TIME_PATTERN);
                                if (null==locClient.getEffectiveEndingTime()){
                                    //重新启动
                                    restartScan();
                                    //2023 03 30 取消推送初始化
                                    //Map pushTaskMap=new HashMap();
                                    //pushTaskMap.put(PushTypeEnum.INIT_ALL_NODE.getName(),locClient.getId().toString());
                                    //cdnMakeFileService.pushByInputInfo(pushTaskMap);
                                }
                                if (e.after(new Date())){
                                    locClient.setEffectiveEndingTime(e);
                                    locClient.setStatus(ClientStatusEnum.ALREADY_REGISTER.getId());
                                }
                            }
                        }
                        cdnClientDao.updateById(locClient);
                    }

                }
            }
        }
        return ipsMap;
    }

    private void syncAntsNodeListHandle(){
            try{
                //1 远程获取后更新本地,输出到本地\
                Map<String,Object> ipsMap=new HashMap<>();
                if (true){
                    ipsMap.putAll(this.getSynRemoteNodeList());
                }
                //2 redis ->ants_agent vesrion更新到本地
                Set<String> listClient= redisUtils.scanAll("version_*");
                if (null!=listClient){
                    for (String key: listClient){
                        //1) "version_69.176.94.50"
                        String cip=key.replace("version_","");
                        if (IPUtils.isValidIPV4(cip)){
                            Integer count=cdnClientDao.selectCount(new QueryWrapper<CdnClientEntity>()
                                    .eq("client_ip",cip)
                                    .last("limit 1")
                            );
                            if (0==count){
                                //发现新节点
                                CdnClientEntity client=new CdnClientEntity();
                                client.setClientIp(cip);
                                client.setStatus(ClientStatusEnum.UNKNOWN.getId());
                                client.setClientType(1);
                                client.setStableScore(0);
                                client.setRegInfo(null);
                                String v=redisUtils.get(key);
                                if (StringUtils.isNotBlank(v)){
                                    NodeVersionVo vo= getVersionObj(v);
                                    client.setAgentVersion(vo.getAgentVersion());
                                    client.setNgxVersion(vo.getNginxVersion());
                                    client.setVersion(vo.getNginxWafVersion());
                                }
                                cdnClientDao.insert(client);
                                restartScan();
                            }
                        }
                    }
                }

                //3 本地与远程对比
                List<CdnClientEntity> list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                        .isNull("parent_id"));
                for (CdnClientEntity client:list){
                    if(!ipsMap.containsKey(client.getClientIp())){
                        logger.error("sign auth servers not fount client:"+client.getClientIp());
                        //授权服务器无此数据
                        //client.setStatus(ClientStatusEnum.REMOTE_EMPTY.getId());
                        //client.setEffectiveStartTime(null);
                        //client.setEffectiveEndingTime(null);
                        //client.setRegInfo(null);
                        //cdnClientDao.updateById(client);
                    }else{
                        //授权服务器存在 对比版本 更新本地版本到远程
                        JSONObject r_object=(JSONObject)ipsMap.get(client.getClientIp());
                        if(null!=r_object && r_object.containsKey("version")){
                            String r_version=r_object.getString("version");
                            String l_version=client.getVersion();
                            if(StringUtils.isNotBlank(l_version)){
                                if(!r_version.equals(l_version)){
                                    this.updateRemoteVersion(client.getClientIp(),l_version);
                                }
                            }
                        }
                    }
                }

            }
            catch (Exception e){
                e.printStackTrace();
            }

          StaticVariableUtils.syncNodeInfoThread=false;
    }

    private void SyncAntsNodeListThread(){
        if(StaticVariableUtils.syncNodeInfoThread){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                syncAntsNodeListHandle();
            }
        }).start();
        Map map=new HashMap();
        map.put(PushTypeEnum.ALL_TASK.getName(),"null");
        cdnMakeFileService.pushByInputInfo(map);
    }

    /**
     *  获取授权节点列表
     */
    @Override
    public R syncFlushAuthList() {
        if(StaticVariableUtils.syncNodeInfoThread){
            return R.error("同步中,请稍侯！");
        }
        //更新本地PrometheusConf
        //this.updatePrometheusConf();
        //同步节点
        this.SyncAntsNodeListThread();
        return R.ok();
    }


    /**
     * 更新本地 Prometheus 配置文件！
     */
    private void updatePrometheusConf(){
        final String confDir="/usr/ants/prome/config/";
        final String[] confList={"info","sts","vts"};
        List<CdnClientEntity> clientList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
                .select("client_ip")
        );
        List<String> clientIpList=clientList.stream().map(t->t.getClientIp()).collect(Collectors.toList());
        JSONArray clientArray=new JSONArray();
        for (String clientIp: clientIpList){
            clientArray.add(clientIp+":8181");
        }
        for (String file:confList){
            String path=confDir+file+".json";
            JSONObject labels_obj=new JSONObject();
            labels_obj.put("project_name",file);
            JSONArray fileArray=new JSONArray();
            JSONObject fileObj=new JSONObject();
            fileObj.put("targets",clientArray);
            fileObj.put("labels",labels_obj);
            fileArray.add(fileObj);
            try {
                File t_file=new File(path);
                if (t_file.exists()){
                    BufferedWriter out = new BufferedWriter(new FileWriter(path));
                    out.write(fileArray.toJSONString());
                    out.close();
                }else{
                    //pass
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }




    /**
     * 保存节点【主控添加节点】
     * @param form
     * @return
     */
    @Override
    public CdnClientEntity SaveByMainControl(CdnClientForm form) {
        if(null!=form.getId()){
            CdnClientEntity tClient= cdnClientDao.selectById(form.getId());
            if(null!=tClient){
                //update
                if (StringUtils.isNotBlank(tClient.getRegInfo())){
                    //已注册成功节点不可修改分组
                    if (!form.getAreaId().equals(tClient.getAreaId())){
                        throw new RuntimeException("已注册成功节点不可修改节点分组");
                    }
                }
                tClient.setAreaId(form.getAreaId());
                tClient.setClientIp(form.getClientIp());
                tClient.setClientType(form.getClientType());
                tClient.setArea(form.getArea());
                tClient.setLine(form.getLine());
                tClient.setRemark(form.getRemark());
                tClient.setClientInfo(form.getClientInfo());
                tClient.setStableScore(0);
                if (null!=form.getClientType() && 1==form.getClientType()){
                    redisUtils.set(form.getClientIp()+"_groupId",form.getAreaId());
                }
                cdnClientDao.updateById(tClient);
                this.updateNodeIpList();
                //同步节点
                this.SyncAntsNodeListThread();
                return  tClient;
            }
        }
        //其它情况添加 insert cdn22.cdn_client.client_ip
        String ip=form.getClientIp();
        CdnClientEntity t2_client=cdnClientDao.selectOne(new QueryWrapper<CdnClientEntity>()
                .eq("client_ip",ip)
                .last(" limit 1")
        );
        if(null!=t2_client){
            throw new RRException("ip["+ip+"]节点存在！");
        }
        CdnClientEntity client=new CdnClientEntity();
        client.setClientIp(form.getClientIp());
        //client.setClientIp2(form.getClientIp2());
        //client.setRedisPort(form.getRedisPort());
        //client.setRedisAuth(form.getRedisAuth());
        client.setClientInfo(form.getClientInfo());
        client.setStableScore(0);
        cdnClientDao.insert(client);
        restartScan();
        this.updateNodeIpList();

        return client;
    }

    private void nodePushInfosV2Thread(CdnClientEntity client){
        if (StaticVariableUtils.pushCallBackErrorThread.containsKey(client.getClientIp()) &&  StaticVariableUtils.pushCallBackErrorThread.get(client.getClientIp())){
            return ;
        }
        StaticVariableUtils.pushCallBackErrorThread.put(client.getClientIp(),true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    NodePushStreamFeedBackInfoVo rVo=new NodePushStreamFeedBackInfoVo();
                    rVo.setCode(200);
                    rVo.setMsg("推送完毕");
                    rVo.setError_info("");
                    //1 获取流长度
                    Long streamSize=0L;
                    List<String> pushTypes= RedisStreamType.getAllXadd();
                    for (String name:pushTypes){
                        String streamName=client.getClientIp()+name;
                        long ss=redisUtils.streamSize(streamName);
                        streamSize+=ss;
                        //streamSize+=redisUtils.StreamSizeV2(streamName);
                        if (ss>0l){
                            rVo.setSn( rVo.getSn()+"|"+streamName+"("+ss+")");
                        }
                        //map.put(name,redisUtils.streamRead(streamName));
                    }
                    if(0L!=streamSize){
                        rVo.setCode(100);
                        rVo.setMsg("推送中");
                        rVo.setXLen(streamSize);
                        rVo.setError_info("");
                    }
                    StaticVariableUtils.pushCallBackMap.put(client.getClientIp(),rVo);

                    //2 获取错误信息
                    List<MapRecord<String, Object, Object>> objList=getSteamFeedbackInfo(client);
                    //map.put("feedback",objList);

                    //3 ERROR
                    if(objList.size()>0){
                        for ( MapRecord<String, Object, Object> LastError:objList){
                            Map<Object,Object>valMap= LastError.getValue();
                            if(valMap.containsKey("module") && valMap.containsKey("config") && valMap.containsKey("info")){
                                String config=valMap.get("config").toString();
                                if ("error".equals(config) || "success".equals(config)){
                                    valMap.put("xid",LastError.getId());
                                    StaticVariableUtils.pushCallBackErrorMap.put(client.getClientIp(),valMap);
                                    break;
                                }
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    StaticVariableUtils.pushCallBackErrorThread.put(client.getClientIp(),false);
                }
            }
        }).start();

    }

    private  List<MapRecord<String, Object, Object>>  getSteamFeedbackInfo(CdnClientEntity client){
        List<MapRecord<String, Object, Object>>  list= redisUtils.streamReadLimitSize(client.getClientIp()+":feedback-stream",StaticVariableUtils.maxFeedBackInfoSize);
        return list;
    }


    /**
     * 分页 节点列表
     * @param param
     * @return
     */
    @Override
    public PageUtils nodePageList(CdnClientQueryForm param) {
        List<Integer> groupIds=new ArrayList<>(128);
        if (null!=param.getGroup_id() && 0!=param.getGroup_id()){
            List<CdnClientGroupChildConfEntity> gList= groupChildConfDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>().eq("group_id",param.getGroup_id()).select("client_id"));
            if (!gList.isEmpty()){
                groupIds.addAll(gList.stream().map(o->o.getClientId()).collect(Collectors.toList()));
            }else{
                groupIds.add(-1);
            }
        }
        List<Integer> childClientIds=new ArrayList<>();
        List<Integer>parentIds=new ArrayList<>();
        if (StringUtils.isNotBlank(param.getIp())){
            List<CdnClientEntity> clients=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                    .like("client_ip", param.getIp())
            );
            if (clients.isEmpty()){
                childClientIds.add(-1);
                parentIds.add(-1);
            }else{
                for (CdnClientEntity client:clients){
                    if (null!=client.getParentId()){
                        parentIds.add(client.getParentId());
                        childClientIds.add(client.getId());
                    }else{
                        parentIds.add(client.getId());
                    }
                }
            }
        }

        IPage<CdnClientEntity> ipage = cdnClientDao.selectPage(
                new Page<CdnClientEntity>(param.getPage(),param.getLimit()),
                new QueryWrapper<CdnClientEntity>()
                        .and(q->q.isNull(parentIds.isEmpty(),"parent_id").or().in(!parentIds.isEmpty(),"id",parentIds))
                        .in(!groupIds.isEmpty(),"id",groupIds)
                        .eq(null!=param.getClientType(),"client_type",param.getClientType())
                        .eq(null!=param.getAreaId(),"area_id",param.getAreaId())
        );
        ipage.getRecords().forEach(item->{
            List<CdnClientEntity> list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                    .eq("parent_id",item.getId())
                    .in(!childClientIds.isEmpty(),"id",childClientIds)
            );
            list.forEach(item2->{
                item2.setArea(item.getArea());
                item2.setLine(item.getLine());
                item2.setRemark(item.getRemark());
                item2.setVersion(null);
                item2.setPushInfo(null);
            });
            item.setChildBackupIpList(list);

            //node
            nodePushInfosV2Thread(item);

            if(StaticVariableUtils.pushCallBackMap.containsKey(item.getClientIp())){
                item.setPushInfo(StaticVariableUtils.pushCallBackMap.get(item.getClientIp()));
            }

            if(StaticVariableUtils.pushCallBackErrorMap.containsKey(item.getClientIp())){
                item.setPushResult(StaticVariableUtils.pushCallBackErrorMap.get(item.getClientIp()));
            }
            if (StaticVariableUtils.lastSendXAddTaskStreamIdMap.containsKey(item.getClientIp())){
                String tId=StaticVariableUtils.lastSendXAddTaskStreamIdMap.get(item.getClientIp());
                String[] ti=tId.split("-");
                String date=DateUtils.stampToDate(ti[0]);
                item.setLastPushStreamId(date);
            }
            //2023 03 16 groupInfo
            if (null==item.getAreaId() || 0==item.getAreaId()){
                 if (null==item.getAreaId()){
                     item.setAreaId(0);
                     cdnClientDao.updateById(item);
                 }
                 item.setAreaName("默认");
                 if (StringUtils.isNotBlank(item.getRegInfo())){
                     redisUtils.set(item.getClientIp()+"_groupId","0");
                 }
            }else{
                if (StringUtils.isNotBlank(item.getRegInfo())){
                    redisUtils.set(item.getClientIp()+"_groupId",item.getAreaId().toString());
                }
                CdnClientAreaEntity areaEntity=cdnClientAreaDao.selectById(item.getAreaId());
                if (null!=areaEntity){
                    item.setAreaName( areaEntity.getName());
                }else {
                    item.setAreaName("【缺失】");
                }

            }

        });
        return new PageUtils(ipage);
    }

    @Override
    public Object feedbackInfo(Integer clientId) {
        CdnClientEntity client=cdnClientDao.selectById(clientId);
        if(null==client){
            return null;
        }
        return this.getSteamFeedbackInfo(client);
    }

    @Override
    public void deleteAllFeedback(Integer clientId) {
        CdnClientEntity client=cdnClientDao.selectById(clientId);
        if(null!=client){
          redisUtils.delete(client.getClientIp()+":feedback-stream");
        }
    }


    /**
     * 对反馈信息进行处理
     */
    @Override
    public Object operaFeeds(Integer index) {
        if(null==index){return null;}
        Map<String,String> result=new LinkedHashMap<>();
        final String SitePathM="home/local/nginx/conf/conf/site/";
        final String StreamProxyPathM="/home/local/nginx/conf/conf/forward/";
        final String SSLPathM="/home/local/nginx/conf/conf/ssl/ssl_";
        final  String commandM="command:normal";
        final String errorPattern = "nginx.*\\stest\\sfailed.*";
        final String successPatter="nginx.*\\stest\\sis\\ssuccessful.*";
        Pattern r_error = Pattern.compile(errorPattern);
        Pattern r_success = Pattern.compile(successPatter);
        //"1652864515293-0": "119.97.137.47|/home/local/nginx/conf/nginx.conf|1",
        Iterator<Map.Entry<String, Object>> entries =StaticVariableUtils.alreadyPushTaskRecordMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            String key = entry.getKey();
            //将超过10分钟的清除
            String temp=key.replaceAll("-\\d+","");
            Long L_temp=Long.parseLong(temp);
            if(System.currentTimeMillis()-L_temp>10*60*1000){
                entries.remove();
                continue;
            }
            String mapValue = entry.getValue().toString();
            String[] value_s=mapValue.split("\\|");
            //logger.debug(mapValue);
            //数据格式错误的清除
            if(value_s.length<3){
                entries.remove();
                continue;
            }
            if(  isInteger(value_s[2])){
                Integer thisIndex=Integer.parseInt(value_s[2]) ;
                if(!index.equals(thisIndex)){
                    continue;
                }
            }else{
                continue;
            }
            String path=value_s[1];
            //logger.debug(path);
            Integer siteI=path.indexOf(SitePathM);
            Integer streamI=path.indexOf(StreamProxyPathM);
            Integer sslI=path.indexOf(SSLPathM);
            if(-1!=siteI || -1!=streamI || -1!=sslI) {
                //处理站点反馈
                String labelName="";
                if (-1!=siteI){
                    String idp= path.replace(SitePathM,"");
                    idp=idp.replace("_.conf","");
                    idp=idp.replace("/","");
                    if (StringUtils.isBlank(idp)){
                        continue;
                    }
                    labelName="site_"+idp;
                }else if(-1!=streamI){
                    String idp= path.replace(StreamProxyPathM,"");
                    idp=idp.replace("_.conf","");
                    idp=idp.replace("/","");
                    if (StringUtils.isBlank(idp)){
                        continue;
                    }
                    labelName="stream_"+idp;
                }else if(-1!=sslI){
                    String idp= path.replace(SSLPathM,"");
                    idp=idp.replace(".conf","");
                    idp=idp.replace("/","");
                    if (StringUtils.isBlank(idp)){
                        continue;
                    }
                    labelName="ssl_"+idp;
                }
                if(value_s.length==3){
                    if(!result.containsKey(labelName)){
                        result.put(labelName,"wait");
                    }
                }else if(value_s.length==4){
                    String feeds=value_s[3];
                    feeds=feeds.replaceAll("\n",";");
                   //logger.debug(feeds);
                    Matcher m_error = r_error.matcher(feeds);
                    Matcher m_success = r_success.matcher(feeds);
                    if(m_error.matches()){
                        logger.error("[error]"+feeds);
                        result.put(labelName,"danger");
                    }else if(m_success.matches()){
                       //logger.debug("[success]"+feeds);
                        if(!result.containsKey(labelName)){
                            result.put(labelName,"success");
                        }else {
                            String v=result.get(labelName) ;
                           //logger.debug(v);
                            if(!v.contains("[emerg]")){
                                result.put(labelName,"success");
                            }
                        }
                    }else{
                        logger.error("[unknown]"+feeds);
                    }
                }
            }else if(path.contains(commandM)){
                if(value_s.length>=3){
                    String NodeIp=value_s[0];
                    if(3==value_s.length){
                        result.put(NodeIp,"wait");
                    }else if(4==value_s.length){
                        if(StringUtils.isBlank(value_s[3])){
                            result.put(NodeIp,"-");
                        }else{
                            result.put(NodeIp,value_s[3]);
                        }
                    }
                }
            }


        }
        return result;
    }


    private  boolean isInteger(String str) {
        return pattern.matcher(str).matches();
    }

    @Override
    public Object operaDetail(Integer index) {
        LinkedHashMap map=new LinkedHashMap();
        Iterator<Map.Entry<String, Object>> entries =StaticVariableUtils.alreadyPushTaskRecordMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            String key = entry.getKey();
            String mapValue = entry.getValue().toString();
            String[] value_s=mapValue.split("\\|");
            if(value_s.length<3){
                entries.remove();
                continue;
            }
            if(  isInteger(value_s[2])){
                Integer thisIndex=Integer.parseInt(value_s[2]) ;
                if(!index.equals(thisIndex)){
                    continue;
                }else {
                    map.put(key,mapValue);
                }
            }
        }
        return map;
    }

    //获取静态变量
    private Object getIndexKeyValue(String key){
        Object ret="";
        switch (key){
            case "server_sum":
                ret=cdnClientDao.selectCount(new QueryWrapper<CdnClientEntity>().eq("client_type",1).eq("status",1));
                break;
            case "site_sum":
                ret=tbSiteDao.selectCount( null );
                break;
            case "streamProxy_sum":
                ret=tbStreamProxyDao.selectCount(null);
                break;
            case "cert_sum":
                ret=tbCertifyDao.selectCount(new QueryWrapper<TbCertifyEntity>().isNotNull("obj_info"));
                break;
            case "already_pay_sum":
                if (true){
                    Map<String,Object> already_pay_map=tbOrderDao.getAlreadyPaySum();
                    //map.put("staticVariable",StaticVariableUtils.get_static_staticVariable());
                    if(already_pay_map.containsKey("already_pay")){
                       ret=already_pay_map.get("already_pay");
                    }else {
                        ret=0;
                    }
                }
                break;
            case "already_recharge_sum":
                if (true){
                    Map<String,Object> already_recharge_map=tbOrderDao.getAlreadyRechargeSum();
                    if(already_recharge_map.containsKey("already_recharge")){
                        ret=already_recharge_map.get("already_recharge");
                    }else {
                        ret=0;
                    }
                }
                break;
            case "user_sum":
                ret=tbUserDao.selectCount(null);
                break;
            case "admin_sum":
                ret=sysUserDao.selectCount(null);
                break;
            case "app_user_reg_7":
                ret=tbUserDao.query7regdata();
                break;
            case "app_user_login_7":
                ret=sysUserDao.query7login();
                break;
            case "area_id_sum":
                ret=cdnClientDao.selectCount(new QueryWrapper<CdnClientEntity>().eq("client_type",1).groupBy("area_id"));
                break;
            case "node_sum":
                ret=cdnClientDao.selectCount(new QueryWrapper<CdnClientEntity>().eq("client_type",1));
                break;
            case "doc_list":
                ret=  HttpRequest.curlHttpGet(QueryAnts.getDocList("CDN技术文档"));
                break;
            case "update_log":
                String  update_str=HttpRequest.curlHttpGet(QueryAnts.getUpdateInfoList("3","1","10"));
                ret=DataTypeConversionUtil.string2Json(update_str);
                break;
            case "auth_info":
                ret=DataTypeConversionUtil.string2Json(this.getAuthInfo());
                break;
            case "cpu":
                ret=AntsSystemInfoUtils.cpu();
                break;
            case "cpuOshi":
                ret=AntsSystemInfoUtils.cpuOshi();
                break;
            case "disk":
                ret=AntsSystemInfoUtils.disk();
                break;
            case "mem":
                ret=AntsSystemInfoUtils.mem();
                break;
            case "jvm":
                ret=AntsSystemInfoUtils.jvm();
                break;
            case "env":
                ret=AntsSystemInfoUtils.env();
                break;
            case "master_env":
                ret=StaticVariableUtils.getStaticStaticVariable();
                break;
            default:
                break;
        }
        return ret;
    }

    /**
     * 首页图表数据源
     * @return
     */
    @Override
    public Map<String, Object> getIndexChartData(String keys)  {
        Map<String, Object> map=new HashMap<>();
        if (StringUtils.isBlank(keys)){
            return map;
        }
        for (String key:keys.split(",")){
            try{
                map.put(key,getIndexKeyValue(key));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return map;
    }

    @Override
    public R nodeAttrSave(Map param) {
       if (!param.containsKey("id")){
           return R.error("id is empty [1]");
       }
        CdnClientEntity client=cdnClientDao.selectOne(new QueryWrapper<CdnClientEntity>()
                .eq("id",param.get("id"))
                .last("limit 1")
        );
       if (null==client){
           return R.error("id is err 【2】");
       }
       param.remove("id");
       JSONObject clientJson=DataTypeConversionUtil.entity2jsonObj(client);
       if (null==clientJson){
           return R.error("id is err 【3】");
       }

       Map pushMap=new HashMap(8);

       for (Object key:param.keySet()){
           clientJson.put(key.toString(),param.get(key));
           //推送
           switch (key.toString()){
               case "sysWsStatus":
                   if ("1".equals(param.get(key).toString())){
                       pushMap.put(PushTypeEnum.NODE_SYS_WS_STATUS_ON.getName(),client.getClientIp());
                   } else{
                       pushMap.put(PushTypeEnum.NODE_SYS_WS_STATUS_OFF.getName(),client.getClientIp());
                   }
                   cdnMakeFileService.pushByInputInfo(pushMap);
                   break;
               default:
                   break;
           }
       }
       CdnClientEntity updateClint=DataTypeConversionUtil.json2entity(clientJson,CdnClientEntity.class);
       if (null!=updateClint){
           cdnClientDao.updateById(updateClint);
       }
       return R.ok();
    }

    @Override
    public R changeMaster(ChangeMasterForm form) {
        if (!HttpRequest.isPortOpen(form.getMasterIp(),form.getRedisPort())){
            return R.error("检测"+form.getMasterIp()+":"+form.getRedisPort()+"连接失败！");
        }
        redisUtils.longSet("migrate:redis",form.getMasterIp()+":"+form.getRedisPort());
        redisUtils.longSet("migrate:passwd",form.getRedisPwd());
        Map<String,String> pushInfo=new HashMap<>(8);
        pushInfo.put(PushTypeEnum.COMMAND.getName(), CommandEnum.MIGRATE.getId().toString());
        cdnMakeFileService.pushByInputInfo(pushInfo);
        //发送更新迁移主控
        this.migrateMaster(form.getMasterIp());
        return R.ok();
    }

}
