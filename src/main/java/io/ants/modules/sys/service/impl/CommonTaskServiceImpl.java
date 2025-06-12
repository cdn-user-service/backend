package io.ants.modules.sys.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.ants.common.utils.*;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.app.vo.AcmeDnsVo;
import io.ants.modules.sys.dao.*;
import io.ants.modules.sys.entity.*;
import io.ants.modules.sys.enums.*;
import io.ants.modules.sys.form.*;
import io.ants.modules.sys.service.*;
import io.ants.modules.sys.vo.*;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.MailConfig;
import io.ants.modules.utils.config.NodeCheckConfig;
import io.ants.modules.utils.factory.MailFactory;
import io.ants.modules.utils.service.MailService;
import io.ants.modules.utils.vo.DnsRecordItemVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommonTaskServiceImpl implements CommonTaskService {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DnsCApiService dnsCApiService;
    @Autowired
    private CdnClientGroupChildConfDao groupClientDao;
    @Autowired
    private CdnClientGroupDao cdnClientGroupDao;
    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private SysConfigDao sysConfigDao;
    @Autowired
    private CdnSuitDao cdnSuitDao;
    @Autowired
    private CdnConsumeDao cdnConsumeDao;
    @Autowired
    private TbOrderDao tbOrderDao;
    @Autowired
    private TbUserDao userDao;
    @Autowired
    private TbCdnPublicMutAttrDao publicMutAttrDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private SysLogDao sysLogDao;
    @Autowired
    private TbDnsConfigDao tbDnsConfigDao;
    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private TbRewriteDao tbRewriteDao;
    @Autowired
    private CdnClientAreaDao cdnClientAreaDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private TbSiteServer tbSiteServer;
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private  TbCertifyDao tbcertifyDao;
    @Autowired
    private CdnConsumeSiteDao cdnConsumeSiteDao;

    //"title": "【cdn】套餐过期提醒",
    private final String MEAL_OUT="meal_out";
    //【cdn】套餐即将过期提醒
    //private final String MEAL_EXPIRE="meal_expire";
    //【cdn】流量已超限
    private final String FLOW_OUT="flow_out";
    // 【cdn】流量即将超限
    //private final String FLOW_EXPIRE="flow_expire";

    private final String[] POST_PAID_MODE ={"2","3"};
    private final int DELETE_MODE=2;
    private final double ONE_THOUSAND=1000.00;


    /**
     * 发送邮件和短信通知
     * @param logStr
     */
    private void sendMailAndEms(String logStr){
        SysConfigEntity syConfig=sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>()
                .eq("param_key", ConfigConstantEnum.CDN_NODECHECK_KEY.getConfKey())
                .last("limit 1")
        );
        if (null==syConfig){
            logger.error("get config error");
            return;
        }
        NodeCheckConfig nc= DataTypeConversionUtil.string2Entity(syConfig.getParamValue(),NodeCheckConfig.class);
        if (null==nc){
            logger.error("get config nc error");
            return;
        }
        if (1==nc.getMailNoticeStatus() &&  StringUtils.isNotBlank(nc.getMailNotice())){
            sendMail(nc.getMailNotice(), "节点调度通知",logStr);
        }

        //todo send_sms

    }


    /**
     * 发送邮件
     * @param
     */
    private void sendMail(String mailAddr,String title, String logStr){
        new Thread(new Runnable() {
            @Override
            public void run() {
                MailService mailService= MailFactory.build();
                mailService.sendEmail(mailAddr,title,logStr);
            }
        }).start();
    }


    /**
     * dns 移出 相关记录日志
     * recordInfoList
     * ipValueList
     */
    private void removeByValue(Integer dnsId, List<DnsRecordItemVo> recordInfoList, List<String> ipValueList, String line, String groupName ){
        if(null==recordInfoList){
            return;
        }
        for (DnsRecordItemVo obj:recordInfoList){
            if (ipValueList.contains(obj.getValue())   ){
                String ip=obj.getValue();
                if( line.equals(obj.getLine())){
                    String logStr=String.format("调度任务：下线分组[%s]的ip:[%s],记录ID[%s]",groupName,obj.getRecordId(),ip);
                    this.InsertTaskLog(logStr,"调度任务");
                    Object removeObj= dnsCApiService.removeRecordByRecordIdAndDnsId(dnsId,obj.getRecordId());
                    sendMailAndEms(logStr);
                    logger.info("removeObj："+removeObj+"[dnsId："+dnsId+"]+[value:"+obj.getValue()+"]");
                }else {
                    logger.error("remove fail!");
                }
            }
        }

    }

    //dns 创建解析
    private  void addByInfo(CdnClientGroupEntity group, CdnClientGroupChildConfEntity mainGroupclient, String ip){
        // ip=m_cdnClient.getClientIp()
        String logStr=String.format("调度任务：分组【%s】上线【%s】",group.getName(),ip);
        String rType="A";
        if (IPUtils.isValidIPV4(ip)){
            rType="A";
        }else if (IPUtils.isValidIPV6(ip)){
            rType="AAAA";
        }else{
            logger.error("ip is not valid ipv4 or ipv6!");
            return;
        }
        R r= dnsCApiService.addRecordByConfId(group.getDnsConfigId(),"*."+group.getHash(),rType,mainGroupclient.getLine(),ip,mainGroupclient.getTtl().toString());
        if (null==r){
            this.InsertTaskLog(logStr,"调度任务-fail");
        }
        if(1!=r.getCode()){
            this.InsertTaskLog(logStr,"调度任务-fail["+r.toString()+"]");
        }
        this.InsertTaskLog(logStr,"调度任务-success");

    }

    /**
     * @param group
     * @param recordId
     * @param main_groupClient
     * @param ip
     * @return
     */
    private void updateDnsInfo(CdnClientGroupEntity group, String recordId, CdnClientGroupChildConfEntity main_groupClient, String ip){
        String logStr=String.format("调度任务：切换上线【%s】上线【%s】",group.getName(),ip);
        this.InsertTaskLog(logStr,"调度任务");
        String rType="A";
        if (IPUtils.isValidIPV4(ip)){
            rType="A";
        }else if (IPUtils.isValidIPV6(ip)){
            rType="AAAA";
        }else{
            logger.error("ip is not valid ipv4 or ipv6!");
            return;
        }
        dnsCApiService.modifyRecordByConfigId(group.getDnsConfigId(),recordId,"*."+group.getHash(),rType,main_groupClient.getLine(),ip,main_groupClient.getTtl().toString());
    }

    //是否在解析中
    private boolean isInAnalysis(List<DnsRecordItemVo> recordInfoList,String ip,String line){
        if (null==recordInfoList){
            return false;
        }
        try{
            for (DnsRecordItemVo obj:recordInfoList){
                if(obj.getValue().equals(ip) && obj.getLine().equals(line) ){
                    return true;
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private String getRecordId(List<DnsRecordItemVo> recordInfoList,String ip,String line){
        if (null==recordInfoList){
            return null;
        }
        try{
            for (DnsRecordItemVo obj:recordInfoList){
                if (IPUtils.ipCompare(obj.getValue(),ip) && obj.getLine().equals(line)){
                    return obj.getRecordId();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
    /**
     * 节点离线切换
     */
    private void dispatchHandleOnlineHandle(){
        if(StaticVariableUtils.dispatchThread){
            logger.info("exit,dispatchThreading");
            return;
        }
        //logger.info();
        StaticVariableUtils.dispatchThread=true;
        List<CdnClientGroupEntity> groupEntityList=cdnClientGroupDao.selectList(new QueryWrapper<CdnClientGroupEntity>()
                .eq("status",1)
                .isNotNull("dns_config_id")   );
        for (CdnClientGroupEntity group:groupEntityList){
            if (1==group.getRecordMode()){
                // //0==A 记录（cdn调度） 1==自建DNS GTM 调度
                continue;
            }
            //logger.debug("检测groupId:"+group.getId()+",groupName:"+group.getName());

            //获取当前分组的DNS记录
            List<DnsRecordItemVo> recordInfoList=dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),"*."+group.getHash(),"A",null);
            List<DnsRecordItemVo> recordInfoListIpv6=dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),"*."+group.getHash(),"AAAA",null);
            recordInfoList.addAll(recordInfoListIpv6);
            //获取当前分组的主节点
            List<CdnClientGroupChildConfEntity> mainClientList=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                    .eq("group_id",group.getId())
                    .eq("status",1)
                    .eq("parent_id",0));
            //System.out.println(recordInfoList);
            for (CdnClientGroupChildConfEntity mainGroupClient:mainClientList){
                if(null==mainGroupClient.getClientId()){
                    continue;
                }
                CdnClientEntity m_cdnClient=cdnClientDao.selectById(mainGroupClient.getClientId());
                if(null==m_cdnClient){
                    continue;
                }
               //logger.debug("检测主节点ID:"+m_cdnClient.getId()+",clientIp:"+m_cdnClient.getClientIp());
                boolean mainHealth=false;
                boolean mainAnalysis=false;
                String mainRecordId=null;
                //获取main是否健康
                if (IPUtils.isValidIPV4(m_cdnClient.getClientIp())){
                    if(null!=m_cdnClient.getStableScore() && m_cdnClient.getStableScore()>0  ){
                        mainHealth=true;
                    }
                }else if (IPUtils.isValidIPV6(m_cdnClient.getClientIp())){
                    //不检查IPV6 20240813
                    mainHealth=true;
                }
                //获取main是否解析中
                if(null!=recordInfoList){
                    mainRecordId=this.getRecordId(recordInfoList,m_cdnClient.getClientIp(),mainGroupClient.getLine());
                    if (StringUtils.isNotBlank(mainRecordId)){
                        mainAnalysis=true;
                    }
                }
                String tips="group:"+group.getId()+"-"+group.getName()+"DnsConf:"+group.getDnsConfigId();
                if(mainHealth && mainAnalysis){
                    //1 【主ip正常 && cname=主IP  ->下线所有副IP】
                    //logger.info(tips+",【DNS操作类型=>1 主ip正常 && 主IP解析中  ->检测备IP,若在线下线】");
                    List<String> removeValueList=new ArrayList<>();
                    List<CdnClientGroupChildConfEntity> backList=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .eq("parent_id",mainGroupClient.getId()));
                    if(backList.size()>0){
                        for (CdnClientGroupChildConfEntity backClient:backList){
                            CdnClientEntity b_cdnClient=cdnClientDao.selectById(backClient.getClientId());
                            if(null==b_cdnClient){
                                continue;
                            }
                            if(this.isInAnalysis(recordInfoList,b_cdnClient.getClientIp(),mainGroupClient.getLine())){
                                removeValueList.add(b_cdnClient.getClientIp());
                            }

                        }
                        this.removeByValue(group.getDnsConfigId(),recordInfoList,removeValueList,mainGroupClient.getLine(),group.getName());
                    }
                }else if(mainHealth && !mainAnalysis){
                    //2 【主ip正常 && cname!=主IP  ->下线副 && 上线主】
                    //logger.info(tips+ ",【DNS操作类型=>2 主ip正常 && 主未解析  ->检测备IP,若在线下线 && 上线主】");

                    List<String> removeValueList=new ArrayList<>();
                    List<CdnClientGroupChildConfEntity> backList=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .eq("parent_id",mainGroupClient.getId()));
                    if(backList.size()>0){
                        for (CdnClientGroupChildConfEntity b_group_client:backList){
                            CdnClientEntity b_cdnClient=cdnClientDao.selectById(b_group_client.getClientId());
                            if(null==b_cdnClient){
                                continue;
                            }
                            if(this.isInAnalysis(recordInfoList,b_cdnClient.getClientIp(),mainGroupClient.getLine())){
                                removeValueList.add(b_cdnClient.getClientIp());
                            }
                        }
                        //下线所有在线备
                        this.removeByValue(group.getDnsConfigId(),recordInfoList,removeValueList,mainGroupClient.getLine(),group.getName());
                    }
                    this.addByInfo(group,mainGroupClient,m_cdnClient.getClientIp());
                }else if(!mainHealth && mainAnalysis){
                    //3 【主IP 不正常 && cname=主IP ==>找一个副上线】
                    //logger.info(tips+",【DNS操作类型=>3 主IP不正常 ->切换备ip上线】");
                    List<CdnClientGroupChildConfEntity> backList=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .eq("parent_id",mainGroupClient.getId()));
                    if(backList.size()>0){
                        List<Integer> backClientIds=backList.stream().map(t->t.getClientId()).collect(Collectors.toList());
                        CdnClientEntity backCdnClient=cdnClientDao.selectOne(new QueryWrapper<CdnClientEntity>()
                                .in("id",backClientIds.toArray())
                                .orderByDesc("stable_score")
                                .last("limit 1")
                        );
                        if(null!=backCdnClient && backCdnClient.getStableScore()>0){
                            //onlineBackLineObject= this.addByInfo(group,main_groupClient,b_cdnClient.getClientIp());
                            this.updateDnsInfo(group,mainRecordId,mainGroupClient,backCdnClient.getClientIp());
                            break;
                        }
                    }
                }else {
                    String eMsg=String.format("ERROR:[%s][%s][%s]",tips,mainHealth,mainAnalysis);
                    logger.error(eMsg);
                }
            }
        }
        StaticVariableUtils.dispatchThread=false;
    }

    /**
     * 随机切换
     */
    private void dispatchHandleRandomHandle(){
        if(StaticVariableUtils.dispatchThread){
            logger.info("exit,dispatchThreading ");
            return;
        }
        if((System.currentTimeMillis()-StaticVariableUtils.dispatchLastModifyTemp)< (5*60*1000)){
            return;
        }
        StaticVariableUtils.dispatchThread=true;
        StaticVariableUtils.dispatchLastModifyTemp=System.currentTimeMillis();
        List<CdnClientGroupEntity> groupEntityList=cdnClientGroupDao.selectList(new QueryWrapper<CdnClientGroupEntity>()
                .eq("status",1)
                .isNotNull("dns_config_id")   );
        for (CdnClientGroupEntity group:groupEntityList){
                if (1==group.getRecordMode()){
                    //0==A 记录（cdn调度） 1==自建DNS GTM 调度
                    continue;
                }
                //logger.debug("检测组：groupId:"+group.getId()+",name:"+group.getName());
                List<DnsRecordItemVo> recordInfoList=dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),"*."+group.getHash(),"A",null);
                List<DnsRecordItemVo> recordInfoListIpV6=dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),"*."+group.getHash(),"AAAA",null);
                recordInfoList.addAll(recordInfoListIpV6);
                List<CdnClientGroupChildConfEntity> main_list=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                        .eq("group_id",group.getId())
                        .eq("status",1)
                        .eq("parent_id",0));
                //遍历主节点
                for (CdnClientGroupChildConfEntity main_groupClient:main_list){
                    CdnClientEntity m_client= cdnClientDao.selectById(main_groupClient.getClientId());
                    if(null==m_client){
                        continue;
                    }
                    //logger.debug("检测主节点：client_id:"+m_client.getId()+",主IP:"+m_client.getClientIp());
                    //获取主节点的子节点m_b_list
                    List<CdnClientGroupChildConfEntity> main_and_back_client_list=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .eq("parent_id",main_groupClient.getId()));

                    if (0==main_and_back_client_list.size()){
                        //无备不切换
                        continue;
                    }
                    //将主节点加入待解析的DNS
                    main_and_back_client_list.add(main_groupClient);
                    CdnClientGroupChildConfEntity current_dns_obj =null;
                    CdnClientGroupChildConfEntity next_dns_obj=null;

                    //查找当前组内的解析的对象
                    for (CdnClientGroupChildConfEntity item:main_and_back_client_list){
                        CdnClientEntity client= cdnClientDao.selectById(item.getClientId());
                        if(null==client){
                            continue;
                        }
                        item.setClientEntity(client);
                        item.setClientIp(client.getClientIp());
                        //对比解析记录
                        for (DnsRecordItemVo obj:recordInfoList){
                            if(obj.getValue().equals(client.getClientIp()) && obj.getLine().equals(main_groupClient.getLine())){
                                if(null==current_dns_obj){
                                    current_dns_obj=item;
                                }
                            }
                        }
                    }

                    //随机获取
                    //当前DNS 解析存在-> 获取下一个待换的节点
                    List<CdnClientGroupChildConfEntity> next_list=new ArrayList<>();
                    if(null!= current_dns_obj){
                        for (CdnClientGroupChildConfEntity g_client_entity:main_and_back_client_list){
                            if(g_client_entity.getId().equals(main_groupClient.getId())){
                                //过滤自身
                                continue;
                            }
                            if(null!=g_client_entity.getClientEntity() && g_client_entity.getClientEntity().getStableScore()>0){
                                //next_dns_obj=g_client_entity;
                                next_list.add(g_client_entity);
                            }
                        }
                        //获取不到规则内待切对象==》获取 一个健康的对象
                        if(next_list.size()>0){
                            int ir=RandomUtil.randomInt(0,next_list.size()-1);
                            next_dns_obj=next_list.get(ir);
                            if(null==next_dns_obj){
                                next_dns_obj=next_list.get(0);
                            }
                        }
                    }else{
                        //当前找不到解析==；待切为主
                        next_dns_obj=main_groupClient;
                    }
                    if(null==next_dns_obj || StringUtils.isBlank(next_dns_obj.getClientIp())){
                        logger.error(main_groupClient.getId()+"---->"+main_groupClient.getClientId()+"获取待切换目标失败！");
                        continue;
                    }

                    //logger.info("TYPE=>下线所有,上线"+next_dns_obj.getClientIp()+"！");
                    List<String> removeValueList=new ArrayList<>();
                    List<CdnClientGroupChildConfEntity> all_List=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .and(q->q.eq("parent_id",0).or().eq("parent_id",main_groupClient.getId())));
                    if(all_List.size()>1){
                        for (CdnClientGroupChildConfEntity all_group_client:all_List){
                            CdnClientEntity c_cdnClient=cdnClientDao.selectById(all_group_client.getClientId());
                            if(null==c_cdnClient){
                                continue;
                            }
                            if(this.isInAnalysis(recordInfoList,c_cdnClient.getClientIp(),main_groupClient.getLine())){
                                removeValueList.add(c_cdnClient.getClientIp());
                            }

                        }
                        this.removeByValue(group.getDnsConfigId(),recordInfoList,removeValueList,main_groupClient.getLine(),group.getName());
                    }
                    this.addByInfo(group,main_groupClient,next_dns_obj.getClientIp());

                }
            }


    }

    //定时切换
    private void dispatchHandleTiming(String Frequency){
        if(StaticVariableUtils.dispatchThread){
            logger.info("exit,dispatchThreading ");
            return;
        }
        StaticVariableUtils.dispatchThread=true;
        long min_fre=Long.parseLong(Frequency);
        logger.info("dispatchHandleTiming:"+StaticVariableUtils.dispatchLastModifyTemp+","+min_fre);
        if( (System.currentTimeMillis()-StaticVariableUtils.dispatchLastModifyTemp)> (min_fre*1000) ){
            List<CdnClientGroupEntity> groupEntityList=cdnClientGroupDao.selectList(new QueryWrapper<CdnClientGroupEntity>()
                    .eq("status",1)
                    .isNotNull("dns_config_id")   );
            for (CdnClientGroupEntity group:groupEntityList){
                if (1==group.getRecordMode()){
                    //0==A 记录（cdn调度） 1==自建DNS GTM 调度
                    continue;
                }
               //logger.debug("检测组：groupId:"+group.getId()+",name:"+group.getName());
                List<DnsRecordItemVo> recordInfoList=dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),"*."+group.getHash(),"A",null);
                List<DnsRecordItemVo> recordInfoListIpV6=dnsCApiService.getRecordByInfoToList(group.getDnsConfigId(),"*."+group.getHash(),"AAAA",null);
                recordInfoList.addAll(recordInfoListIpV6);
                List<CdnClientGroupChildConfEntity> main_list=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                        .eq("group_id",group.getId())
                        .eq("status",1)
                        .eq("parent_id",0));
                //遍历主节点
                for (CdnClientGroupChildConfEntity main_groupClient:main_list){
                    CdnClientEntity m_client= cdnClientDao.selectById(main_groupClient.getClientId());
                    if(null==m_client){
                        continue;
                    }
                   //logger.debug("检测主节点：client_id:"+m_client.getId()+",主IP:"+m_client.getClientIp());
                    //获取主节点的子节点m_b_list
                    List<CdnClientGroupChildConfEntity> main_and_back_client_list=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .eq("parent_id",main_groupClient.getId()));

                    if (0==main_and_back_client_list.size()){
                        //无备不切换
                        continue;
                    }
                    //将主节点加入待解析的DNS
                    main_and_back_client_list.add(main_groupClient);
                    CdnClientGroupChildConfEntity current_dns_obj =null;
                    CdnClientGroupChildConfEntity next_dns_obj=null;

                    //查找当前组内的解析的对象
                    for (CdnClientGroupChildConfEntity item:main_and_back_client_list){
                        CdnClientEntity client= cdnClientDao.selectById(item.getClientId());
                        if(null==client){
                            continue;
                        }
                        item.setClientEntity(client);
                        item.setClientIp(client.getClientIp());
                        //对比解析记录
                        for (DnsRecordItemVo obj:recordInfoList){
                            if(obj.getValue().equals(client.getClientIp()) && obj.getLine().equals(main_groupClient.getLine())){
                                if(null==current_dns_obj){
                                    current_dns_obj=item;
                                }
                            }
                        }
                    }

                    //获取当前组内下一个ID大于当前 且 健康的对象
                    //当前DNS 解析存在-> 获取下一个待换的节点
                    List<CdnClientGroupChildConfEntity> next_list=new ArrayList<>();
                    if(null!= current_dns_obj){
                        for (CdnClientGroupChildConfEntity g_client_entity:main_and_back_client_list){
                            if(g_client_entity.getId().equals(main_groupClient.getId())){
                                //过滤自身
                                continue;
                            }
                            if(null!=g_client_entity.getClientEntity() && g_client_entity.getClientEntity().getStableScore()>0){
                                //next_dns_obj=g_client_entity;
                                next_list.add(g_client_entity);
                            }
                        }
                        //获取不到规则内待切对象==》获取 一个健康的对象
                        if(next_list.size()>0){
                            for (CdnClientGroupChildConfEntity next:next_list){
                                if (next.getId()>current_dns_obj.getId()){
                                    next_dns_obj=next;
                                }
                            }
                            if(null==next_dns_obj){
                                next_dns_obj=next_list.get(0);
                            }
                        }
                    }else{
                        //当前找不到解析==；待切为主
                        next_dns_obj=main_groupClient;
                    }
                    if(null==next_dns_obj || StringUtils.isBlank(next_dns_obj.getClientIp())){
                        logger.error(main_groupClient.getId()+"---->"+main_groupClient.getClientId()+"获取待切换目标失败！");
                        continue;
                    }

                    //logger.info("TYPE=>下线所有,上线"+next_dns_obj.getClientIp()+"！");
                    List<String> removeValueList=new ArrayList<>();
                    List<CdnClientGroupChildConfEntity> all_List=groupClientDao.selectList(new QueryWrapper<CdnClientGroupChildConfEntity>()
                            .eq("group_id",group.getId())
                            .eq("status",1)
                            .and(q->q.eq("parent_id",0).or().eq("parent_id",main_groupClient.getId())));
                    if(all_List.size()>1){
                        for (CdnClientGroupChildConfEntity all_group_client:all_List){
                            CdnClientEntity c_cdnClient=cdnClientDao.selectById(all_group_client.getClientId());
                            if(null==c_cdnClient){
                                continue;
                            }
                            if(this.isInAnalysis(recordInfoList,c_cdnClient.getClientIp(),main_groupClient.getLine())){
                                removeValueList.add(c_cdnClient.getClientIp());
                            }

                        }
                        this.removeByValue(group.getDnsConfigId(),recordInfoList,removeValueList,main_groupClient.getLine(),group.getName());
                    }
                    this.addByInfo(group,main_groupClient,next_dns_obj.getClientIp());

                }
            }

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
    }


    /**
     *
     * @param userId
     * @param mode ={MEAL_OUT,MEAL_EXPIRE,FLOW_OUT,FLOW_EXPIRE}
     * @param serialNumber
     */
    private void sendSmsMailNotice2User(Long userId, String mode, String serialNumber){
        if (null==userId ||  StringUtils.isBlank(serialNumber)){
            return;
        }
        TbUserEntity userEntity=userDao.selectById(userId);
        if (null==userEntity){
            return;
        }
        if (StringUtils.isBlank(userEntity.getMail()) ){
            return;
        }
        TbOrderEntity orderEntity=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",serialNumber).orderByDesc("id").last("limit 1"));
        if (null==orderEntity){
            return;
        }
        TbOrderInitVo vo=DataTypeConversionUtil.string2Entity(orderEntity.getInitJson(),TbOrderInitVo.class);
        JSONObject productObj=vo.getProduct_obj();
        CdnProductEntity productEntity=DataTypeConversionUtil.json2entity(productObj,CdnProductEntity.class);
        //域名 #domain#
        //套餐 #product#

        //mail
        MailConfig config = sysConfigService.getConfigObject(ConfigConstantEnum.MAIL_CONFIG_KEY.getConfKey(), MailConfig.class);
        for (MailConfig.tempVo item:config.getTemplates()){
            if (1==item.getStatus() && mode.equals(item.getName())){
                String msg=item.getContent();
                msg=msg.replace("#domain#","*");
                msg=msg.replace("#product#",productEntity.getName());
                sendMail(userEntity.getMail(),item.getTitle(),msg);
                break;
            }
        }
        //sms



    }

    /**
     * @param suitEntity
     * @param types
     * @param mode MEAL_OUT 超出，FLOW_OUT到期
     */
    //过期|超出停用套餐
    private  void timeOutDisableSuitProduct(CdnSuitEntity suitEntity, String[] types,String mode){
        logger.info("time-OutDisableSuitProduct:"+suitEntity.toString());
        for (String type:types){
            switch (type){
                case "suit":
                    if (true){
                        //  UpdateWrapper<TbSiteAttrEntity> updateWrapper=new UpdateWrapper<>();
                        //        updateWrapper.eq("site_id",siteId).eq("pkey",pKey).set("pvalue",pValue).set("updatetime",new Date());
                        //        Integer result=  tbSiteAttrDao.update(null,updateWrapper);
                        if (mode.equals(MEAL_OUT)){
                            Integer[] sList={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
                            UpdateWrapper<CdnSuitEntity> updateWrapper=new UpdateWrapper<>();
                            updateWrapper.eq("id",suitEntity.getId())
                                    .eq("status",CdnSuitStatusEnum.NORMAL.getId())
                                    .in("suit_type",sList)
                                    .set("status",CdnSuitStatusEnum.TIMEOUT.getId())
                            ;
                            cdnSuitDao.update(null,updateWrapper);
                        }
                        sendSmsMailNotice2User(suitEntity.getUserId(),mode,suitEntity.getSerialNumber());
                        if (MEAL_OUT.equals(mode)){
                            this.InsertTaskLog(suitEntity.getSerialNumber()+"，套餐到期！","套餐到期");
                        }else if (FLOW_OUT.equals(mode)){
                            this.InsertTaskLog(suitEntity.getSerialNumber()+"，套餐超出！","套餐超出");
                        }else{
                            logger.warn("unknown mode");
                        }

                    }
                    break;
                case "site":
                    if (true){
                        //当前套餐所有站点下线
                        List<TbSiteEntity> ls=tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
                                .eq("serial_number",suitEntity.getSerialNumber())
                                .select("id,serial_number,user_id,main_server_name")
                                .eq("status",1)
                        );
                        for (TbSiteEntity site:ls){
                            //site.setStatus(0);
                            // tbSiteDao.updateById(site);
                            tbSiteDao.update(null,new UpdateWrapper<TbSiteEntity>().eq("id",site.getId()).set("status",0));
                            if (1==DELETE_MODE){
                                Map<String, String> map=new HashMap<>();
                                String vValue=String.format("%d_%s_",site.getId(),site.getMainServerName());
                                String path=PushSetEnum.SITE_CONF.getTemplatePath().replace("###site_id_name###",vValue);
                                map.put(PushTypeEnum.COMMAND_DELETE_DIR.getName(),path);
                                cdnMakeFileService.pushByInputInfo(map);
                            }else  if(2==DELETE_MODE){
                                Map<String, String> map=new HashMap<>();
                                map.put(PushTypeEnum.CLEAN_STOP_SITE.getName(),site.getId().toString());
                                cdnMakeFileService.pushByInputInfo(map);
                            }
                            this.InsertTaskLog(suitEntity.getSerialNumber()+"，套餐到期,关停站点【"+site.getId()+"=","time_OutDisableSuitProduct");
                        }
                    }
                    break;
                case "proxy":
                    if (true){
                        //当前套餐所有转发下线
                        List<TbStreamProxyEntity> ls=tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
                                .eq("serial_number",suitEntity.getSerialNumber())
                                .select("id,serial_number,user_id")
                                .eq("status",1)
                        );
                        for (TbStreamProxyEntity proxy:ls){
                            //proxy.setStatus(0);
                            //tbStreamProxyDao.updateById(proxy);
                            tbStreamProxyDao.update(null,new UpdateWrapper<TbStreamProxyEntity>()
                                    .eq("id",proxy.getId())
                                    .set("status",0)
                            );
                            if (1==DELETE_MODE){
                                Map<String, String> map=new HashMap<>();
                                String vValue=String.format("%d_",proxy.getId());
                                String path=PushSetEnum.STREAM_CONF.getTemplatePath().replace("###sp_id###",vValue);
                                map.put(PushTypeEnum.COMMAND_DELETE_DIR.getName(),path);
                                cdnMakeFileService.pushByInputInfo(map);
                            }else if(2==DELETE_MODE){
                                Map<String, String> map=new HashMap<>();
                                map.put(PushTypeEnum.CLEAN_STOP_STREAM_CONF.getName(),proxy.getId().toString());
                                cdnMakeFileService.pushByInputInfo(map);
                            }
                            this.InsertTaskLog(suitEntity.getSerialNumber()+"，套餐到期,关停转发【"+proxy.getId()+"=","time_OutDisableSuitProduct");

                        }
                    }
                    break;
                case "rewrite":
                    if (true){
                        List<TbRewriteEntity> ls=tbRewriteDao.selectList(new QueryWrapper<TbRewriteEntity>()
                                .eq("serial_number",suitEntity.getSerialNumber())
                                .eq("status",1)
                        );
                        for (TbRewriteEntity rewrite:ls){
                            rewrite.setStatus(0);
                            tbRewriteDao.updateById(rewrite);
                            if (1==DELETE_MODE){
                                Map<String, String> map=new HashMap<>();
                                String vValue=String.format("%d_%s",rewrite.getId(),rewrite.getServerName());
                                String path=PushSetEnum.REWRITE_CONF.getTemplatePath().replace("###rewrite_id_name###",vValue);
                                map.put(PushTypeEnum.COMMAND_DELETE_DIR.getName(),path);
                                cdnMakeFileService.pushByInputInfo(map);
                            }else if(2==DELETE_MODE){
                                Map<String, String> map=new HashMap<>();
                                map.put(PushTypeEnum.CLEAN_STOP_REWRITE_CONF.getName(),rewrite.getId().toString());
                                cdnMakeFileService.pushByInputInfo(map);
                            }
                            this.InsertTaskLog(suitEntity.getSerialNumber()+"，套餐到期,关停转发【"+rewrite.getId()+"=","time_OutDisableSuitProduct");

                        }

                    }
                    break;
                default:
                    break;
            }
        }

    }

    //根据MODE LIST 获取套餐列表
    private List<CdnSuitEntity> getFeeModeSuitList(Integer[] mode){
        List<CdnSuitEntity> result=new ArrayList<>();
        List<String>sn=new ArrayList<>();
        if(StringUtils.isBlank(StaticVariableUtils.authMasterIp)){
            logger.error("AuthMasterIp is empty! getFeeModeSuitList return null " );
            return result;
        }
        Date now =new Date();

        //0 clean 3 months before suit
        if (true){

        }

        //1 对过期套餐 关闭站 禁用套餐
        if (true){
            Integer[] tTypes={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
            List<CdnSuitEntity> timeOutSuitList=cdnSuitDao.selectList(
                    new QueryWrapper<CdnSuitEntity>()
                            .orderByDesc("id")
                            .in("suit_type",tTypes)
                            .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                            .le("end_time",now)
            );
            Iterator<CdnSuitEntity> iterator=timeOutSuitList.iterator();
            while (iterator.hasNext()){
                CdnSuitEntity item=iterator.next();
                CdnSuitEntity itemCdnEntity=this.updateThisSuitDetailInfo(item,true);
                if (itemCdnEntity.getEndTime().after(now)){
                    iterator.remove();
                }else {
                    String[] types={"suit","site","proxy","rewrite"};
                    this.timeOutDisableSuitProduct(itemCdnEntity,types,MEAL_OUT);
                }
            }

        }

        // 2 对有效期内套餐处理流量
        Integer[] suitTypeLS={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        List<CdnSuitEntity> suitList=cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                .in("suit_type",suitTypeLS)
                .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                .le("start_time",now)
                .ge("end_time",now));
        for (CdnSuitEntity cdnSuitEntity:suitList){
            //logger.info(cdnSuitEntity.getSerialNumber());
            if(StringUtils.isBlank(cdnSuitEntity.getSerialNumber())){
                logger.error("套餐： "+cdnSuitEntity.getId()+" SerialNumber is null");
                continue;
            }

            //2 获取套餐类型
            if (StringUtils.isNotBlank(cdnSuitEntity.getAttrJson())){
                //{"live_data":1,"site":12,"charging_mode":1,"bandwidth":200000,"sms":100,"private_waf":1,"monitor":1,"flow":100,"port_forwarding":5,"ai_waf":1,"public_waf":1}
                ProductAttrVo attrVo=DataTypeConversionUtil.string2Entity(cdnSuitEntity.getAttrJson(),ProductAttrVo.class);
                Integer mValue=attrVo.getCharging_mode();
                if(!Arrays.asList(mode).contains(mValue)){
                   //logger.debug("["+mValue+","+ mode.toString()+"] not in mode");
                    continue;
                }
                //cdnSuitEntity.setProductAttrMap(attrJson);
            }else{
               //logger.debug("need query order");
                TbOrderEntity orderEntity=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                        .eq("serial_number",cdnSuitEntity.getPaySerialNumber())
                         .select("init_json")
                        .last("limit 1"));
                if(null==orderEntity){
                    logger.error("SerialNumber:"+cdnSuitEntity.getPaySerialNumber()+"订单不存在：");
                    continue;
                }
                String initJsonStr=orderEntity.getInitJson();
                JSONObject initJsonObj=DataTypeConversionUtil.string2Json(initJsonStr);
                if (!initJsonObj.containsKey("product_obj")){
                    logger.error("订单不无产品属性");
                    continue;
                }
                JSONObject productObj=initJsonObj.getJSONObject("product_obj");
                CdnProductEntity product=DataTypeConversionUtil.json2entity(productObj, CdnProductEntity.class);
                if(null==product){
                    logger.error("订单产品属性转换错误");
                    continue;
                }
                cdnSuitEntity.setProductEntity(product);
                int feeMode=1;
                Map productAttrMap=new HashMap();
                //[{"attr":"charging_mode","name":"charging_mode","value":2,"valueType":"select"},{"attr":"flow","name":"flow","valueType":"int","unit":"G","value":10000},{"attr":"bandwidth","name":"bandwidth","valueType":"price_int","unit":"元/Mbps/月","value":20000},{"attr":"ai_waf","id":27,"name":"AI WAF","valueType":"bool","value":"1"},{"attr":"port_forwarding","id":36,"name":"端口转发","valueType":"int","unit":"个","value":5},{"attr":"site","id":35,"name":"站点","valueType":"int","unit":"个","value":10},{"attr":"sms","id":34,"name":"短信通知","valueType":"int","unit":"条/月","value":100},{"attr":"monitor","id":33,"name":"流量监控","valueType":"bool","value":"1"},{"attr":"private_waf","id":32,"name":"专属WAF","valueType":"bool","value":"1"},{"attr":"live_data","id":30,"name":"实时数据","valueType":"bool","value":"1"},{"attr":"public_waf","id":37,"name":"WAF","valueType":"bool","value":"1"}]
                JSONArray attrInfoArr=DataTypeConversionUtil.string2JsonArray(product.getAttrJson());
                for (int i = 0; i < attrInfoArr.size(); i++) {
                    JSONObject attrJson=attrInfoArr.getJSONObject(i);
                    if(attrJson.containsKey("name") ){
                        productAttrMap.put(attrJson.getString("name"),attrJson.getString("value"));
                        if( attrJson.getString("name").equals(ProductAttrNameEnum.ATTR_CHARGING_MODE.getName())){
                            feeMode=Integer.parseInt(attrJson.getString("value"));
                        }else  if( attrJson.getString("name").equals(ProductAttrNameEnum.ATTR_CHARGING_MODE.getAttr())){
                            feeMode=Integer.parseInt(attrJson.getString("value"));
                        }
                    }
                }
                if(!Arrays.asList(mode).contains(feeMode)){
                    logger.info("计费方式不为"+Arrays.asList(mode) +",continue");
                    continue;
                }
                if (sn.contains(cdnSuitEntity.getSerialNumber())){
                    continue;
                }
                sn.add(cdnSuitEntity.getSerialNumber());
            }
            result.add(cdnSuitEntity);

        }
        return result;
    }


    private void insert2Consume(CdnSuitEntity cdnSuitEntity,CdnConsumeEntity lastConsume,long totalUsedFlow,Integer startTime,Integer endTime){
        CdnConsumeEntity consume=new CdnConsumeEntity();
        consume.setSerialNumber(cdnSuitEntity.getSerialNumber());
        consume.setAttrName("flow");
        consume.setSValue(totalUsedFlow);
        consume.setStartTime(startTime);
        consume.setEndTime(endTime);
        if (0l==totalUsedFlow){
            if (null==lastConsume){
                consume.setRType(0);
                cdnConsumeDao.insert(consume);
            }else {
                lastConsume.setEndTime(endTime);
                cdnConsumeDao.update(null,new UpdateWrapper<CdnConsumeEntity>().eq("id",lastConsume.getId()).set("end_time",endTime));
            }
            return;
        }
        BigDecimal rTotalGFlow =new BigDecimal(totalUsedFlow).divide(new BigDecimal(ONE_THOUSAND*ONE_THOUSAND*ONE_THOUSAND),10,BigDecimal.ROUND_HALF_UP);
        //logger.info("rTotalGFlow:["+rTotalGFlow+"]GB");

        //增值加油包里
        Date now=new Date();
        List<CdnSuitEntity>list=cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number",cdnSuitEntity.getSerialNumber())
                .eq("suit_type",OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())
                .eq("status", CdnSuitStatusEnum.NORMAL.getId())
                .le("start_time",now)
                .ge("end_time",now)
                .select("id,flow,used_flow")
                .orderByAsc("end_time")
        );
        for (CdnSuitEntity addSuitItem:list){
            //计算获取余量
            BigDecimal bal=(new BigDecimal(addSuitItem.getFlow()).subtract(addSuitItem.getUsedFlow())).setScale(10, RoundingMode.HALF_UP);
            if (bal.compareTo(rTotalGFlow)>0){
                //当前余量足
                BigDecimal used=(addSuitItem.getUsedFlow().add(rTotalGFlow)).setScale(10, RoundingMode.HALF_UP);
                addSuitItem.setUsedFlow(used);
                cdnSuitDao.update(null,new UpdateWrapper<CdnSuitEntity>().eq("id",addSuitItem.getId())
                        .set("used_flow",used)
                );
                rTotalGFlow=BigDecimal.ZERO;
                break;
            }else{
                //余量不足
                addSuitItem.setUsedFlow(new BigDecimal(addSuitItem.getFlow()));
                //addSuitItem.setStatus(CdnSuitStatusEnum.USED_RUN_OUT.getId());
                // cdnSuitDao.updateById(addSuitItem);
                cdnSuitDao.update(null,new UpdateWrapper<CdnSuitEntity>().eq("id",addSuitItem.getId())
                                .set("status",CdnSuitStatusEnum.USED_RUN_OUT.getId())
                );
                rTotalGFlow=rTotalGFlow.subtract(bal);
            }
        }

        if (0==rTotalGFlow.compareTo(BigDecimal.ZERO)){
            //消费增值业务流量
            consume.setRType(1);
        }else{
            //消费套餐流量
            consume.setRType(0);
        }
        if (null==lastConsume || !consume.getRType().equals(lastConsume.getRType()) ){
            cdnConsumeDao.insert(consume);
        }else{
            int  lDay= DateUtils.getDayOfMonth(DateUtils.stamp2date(lastConsume.getEndTime()));
            int  cDay=DateUtils.getDayOfMonth(DateUtils.stamp2date(endTime));
            if (cDay==lDay){
                cdnConsumeDao.update(null,new UpdateWrapper<CdnConsumeEntity>().eq("id",lastConsume.getId())
                        .set("end_time",endTime)
                        .set("s_value",lastConsume.getSValue()+totalUsedFlow)
                );
            }else{
                cdnConsumeDao.insert(consume);
            }
        }
    }

    /**
     *  流量入库--来之日志记录采集流量
     * @param cdnSuitEntity
     * @param elkServerVo
     * @param startTime
     * @param endTime
     */
//    private void recordIntoConsume(CdnSuitEntity cdnSuitEntity, ElkServerVo elkServerVo, Integer startTime, Integer endTime ){
//       long streamFlowValue=this.getStreamConsume(cdnSuitEntity.getSerialNumber(),elkServerVo);
//       try{
//            //logger.debug("query_url:"+url);
//            String url="http://"+elkServerVo.getHost()+":"+elkServerVo.getPort()+"/"+"filebeat-*/_search";
//            //String paramFormat="{\"track_total_hits\":10000,\"_source\":{},\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*ants_access*\"}}},{\"terms\":{\"k_host\":[\"www.vedns.com\",\"cdntest2.91hu.top\"]}},{\"range\":{\"@timestamp\":{\"gte\":\"2023-08-29 00:00:00\",\"lte\":\"2023-08-29 23:59:59\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":0,\"aggs\":{\"group_by_host\":{\"terms\":{\"field\":\"k_host\",\"size\":1000},\"aggs\":{\"latest_hit\":{\"top_hits\":{\"_source\":{\"includes\":[\"l_server_out_byte\",\"l_server_in_byte\",\"k_host\",\"log_time\"]},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":1}}}}}}";
//            //String paramFormat="{\"track_total_hits\":10000,\"_source\":{},\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*ants_access*\"}}},{\"terms\":{\"k_host\":[%s]}},{\"range\":{\"@timestamp\":{\"gte\":\"%s\",\"lte\":\"%s\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":0,\"aggs\":{\"group_by_host\":{\"terms\":{\"field\":\"k_host\",\"size\":1000},\"aggs\":{\"latest_hit\":{\"top_hits\":{\"_source\":{\"includes\":[\"l_server_out_byte\",\"l_server_in_byte\"]},\"sort\":[{\"log_time\":{\"order\":\"desc\"}}],\"size\":1}}}}}}";
//            List<TbSiteEntity> siteList=tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
//                    .eq("serial_number",cdnSuitEntity.getSerialNumber())
//                    .select("id,main_server_name")
//            );
//            Map<String,Integer>siteMap=new HashMap<>(siteList.size());
//            StringBuilder hostSb=new StringBuilder();
//            siteList.forEach(item->{
//                hostSb.append("\""+item.getMainServerName()+"\""+",");
//                siteMap.put(item.getMainServerName(),item.getId());
//            });
//            hostSb.append("\"\"");
//            String paramFormat="{\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*access*\"}}},{\"terms\":{\"k_host\":[%s]}},{\"range\":{\"@timestamp\":{\"gte\":\"now-5m\",\"lte\":\"now\"}}}]}},\"size\":0,\"aggs\":{\"flow\":{\"terms\":{\"field\":\"k_host\",\"size\":%d},\"aggs\":{\"1\":{\"sum\":{\"field\":\"k_request_length\"}},\"2\":{\"sum\":{\"field\":\"l_out_size\"}},\"total\":{\"bucket_script\":{\"buckets_path\":{\"in\":\"1\",\"out\":\"2\"},\"script\":\"params.in+params.out\"}}}}}}";
//            String param=String.format(paramFormat,hostSb,siteMap.size());
//            //logger.info(param);
//            String res=  HttpRequest.erkHttp("GET",url,elkServerVo.getPwd(),param);
//            //logger.info(res);
//            if(StringUtils.isBlank(res)){
//                // logger.error("url:["+url+"],request error");
//                this.insert2Consume(cdnSuitEntity,0l+streamFlowValue,startTime,endTime);
//                return;
//            }
//            //logger.info(res);
//            ElkFilterSiteIOFlowVo ioVo=DataTypeConversionUtil.string2Entity(res,ElkFilterSiteIOFlowVo.class);
//            Map<String,Long> siteFlowInfo=ioVo.getSiteFlowData();
//            //logger.info(DataTypeConversionUtil.map2json(siteFlowInfo).toJSONString());
//            if (siteFlowInfo.isEmpty()){
//                //logger.info("url:["+url+"],request get empty");
//                this.insert2Consume(cdnSuitEntity,0l+streamFlowValue,startTime,endTime);
//                return;
//            }
//            long mainConsumeFlowUsed=0l;
//            for (String hostKey:siteFlowInfo.keySet()){
//                if(siteMap.containsKey(hostKey)){
//                    Long curTotalFlow=siteFlowInfo.get(hostKey);
//                    mainConsumeFlowUsed+=curTotalFlow;
//                }
//            }
//            this.insert2Consume(cdnSuitEntity,mainConsumeFlowUsed+streamFlowValue,startTime,endTime);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }
//

    /**
     *  流量入库--来之server IO 记录采集流量
     */
//    private  void  recordFlowIntoConsumeBySIO(CdnSuitEntity cdnSuitEntity, ElkServerVo elkServerVo, Integer startTime, Integer endTime ){
//        long streamFlowValue=this.getStreamConsume(cdnSuitEntity.getSerialNumber(),elkServerVo);
//        try{
//            //logger.debug("query_url:"+url);
//            String url="http://"+elkServerVo.getHost()+":"+elkServerVo.getPort()+"/"+"server_io/_search";
//            //String paramFormat="{\"track_total_hits\":10000,\"_source\":{},\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*ants_access*\"}}},{\"terms\":{\"k_host\":[\"www.vedns.com\",\"cdntest2.91hu.top\"]}},{\"range\":{\"@timestamp\":{\"gte\":\"2023-08-29 00:00:00\",\"lte\":\"2023-08-29 23:59:59\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":0,\"aggs\":{\"group_by_host\":{\"terms\":{\"field\":\"k_host\",\"size\":1000},\"aggs\":{\"latest_hit\":{\"top_hits\":{\"_source\":{\"includes\":[\"l_server_out_byte\",\"l_server_in_byte\",\"k_host\",\"log_time\"]},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":1}}}}}}";
//            //String paramFormat="{\"track_total_hits\":10000,\"_source\":{},\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*ants_access*\"}}},{\"terms\":{\"k_host\":[%s]}},{\"range\":{\"@timestamp\":{\"gte\":\"%s\",\"lte\":\"%s\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":0,\"aggs\":{\"group_by_host\":{\"terms\":{\"field\":\"k_host\",\"size\":1000},\"aggs\":{\"latest_hit\":{\"top_hits\":{\"_source\":{\"includes\":[\"l_server_out_byte\",\"l_server_in_byte\"]},\"sort\":[{\"log_time\":{\"order\":\"desc\"}}],\"size\":1}}}}}}";
//            List<TbSiteEntity> siteList=tbSiteDao.selectList(new QueryWrapper<TbSiteEntity>()
//                    .eq("serial_number",cdnSuitEntity.getSerialNumber())
//                    .select("id,main_server_name")
//            );
//            Map<String,Integer>siteMap=new HashMap<>(siteList.size());
//            List<String> hostSb=new ArrayList<>();
//            siteList.forEach(item->{
//                hostSb.add(item.getMainServerName());
//                siteMap.put(item.getMainServerName(),item.getId());
//            });
//            if (0==hostSb.size()){
//                this.insert2Consume(cdnSuitEntity,0l+streamFlowValue,startTime,endTime);
//                return;
//            }
//            String paramFormat="{\"_source\":false,\"size\":0,\"query\":{\"bool\":{\"must\":[{\"range\":{\"timestamp\":{\"gte\":%d,\"lte\":%d}}},{\"nested\":{\"path\":\"v\",\"query\":{\"terms\":{\"v.s\":[\"%s\"]}},\"inner_hits\":{}}}]}},\"aggs\":{\"flow\":{\"nested\":{\"path\":\"v\"},\"aggs\":{\"flow_buckets\":{\"terms\":{\"field\":\"v.s\",\"size\":%d,\"include\":\"%s\"},\"aggs\":{\"flow_in\":{\"sum\":{\"field\":\"v.i\"}},\"flow_out\":{\"sum\":{\"field\":\"v.o\"}},\"flow_total\":{\"bucket_script\":{\"buckets_path\":{\"in\":\"flow_in\",\"out\":\"flow_out\"},\"script\":\"params.in + params.out\"}}}}}}}}";
//            long sTm=startTime*1000l;
//            long eTm=endTime*1000l;
//            String param=String.format(paramFormat,sTm,eTm,hostSb.get(0),hostSb.size(),String.join("|",hostSb));
//            //logger.info(param);
//            String res=  HttpRequest.erkHttp("GET",url,elkServerVo.getPwd(),param);
//            //logger.info(res);
//            if(StringUtils.isBlank(res)){
//                logger.info(param);
//                logger.info(res);
//                logger.error("url:["+url+"],request error");
//                this.insert2Consume(cdnSuitEntity,0l+streamFlowValue,startTime,endTime);
//                return;
//            }
//            //logger.info(res);
//            //ElkFilterSiteIOFlowVo ioVo=DataTypeConversionUtil.string2Entity(res,ElkFilterSiteIOFlowVo.class);
//            ElkFilterSiteIOFlowFilterVo ioVo=DataTypeConversionUtil.string2Entity(res,ElkFilterSiteIOFlowFilterVo.class);
//            Map<String,Long> siteFlowInfo=ioVo.getSiteFlowData();
//            //logger.info(DataTypeConversionUtil.map2json(siteFlowInfo).toJSONString());
//            if (siteFlowInfo.isEmpty()){
//                logger.info(param);
//                logger.info(res);
//                logger.error("url:["+url+"],request error");
//                this.insert2Consume(cdnSuitEntity,0l+streamFlowValue,startTime,endTime);
//                return;
//            }
//            long mainConsumeFlowUsed=0l;
//            for (String hostKey:siteFlowInfo.keySet()){
//                if(siteMap.containsKey(hostKey)){
//                    Long curTotalFlow=siteFlowInfo.get(hostKey);
//                    mainConsumeFlowUsed+=curTotalFlow;
//                }
//            }
//            this.insert2Consume(cdnSuitEntity,mainConsumeFlowUsed+streamFlowValue,startTime,endTime);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }


    /**
     * 流量入库--来之server IOp 记录采集流量
     * @param cdnSuitEntity
     * @param elkServerVo
     * @param startTime
     * @param endTime
     */
    private void recordFlowIntoConsumeBySp(CdnSuitEntity cdnSuitEntity,  CdnConsumeEntity lastConsume,ElkServerVo elkServerVo, Integer startTime, Integer endTime){
        long streamFlowValue=this.getStreamConsume(cdnSuitEntity.getSerialNumber(),elkServerVo);
        try{
            logger.info("recordFlowIntoConsumeBySp");
            String url="http://"+elkServerVo.getHost()+":"+elkServerVo.getPort()+"/"+"metricbeat-8.9.1/_search";
            //String paramFormat="{\"track_total_hits\":10000,\"_source\":{},\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*ants_access*\"}}},{\"terms\":{\"k_host\":[\"www.vedns.com\",\"cdntest2.91hu.top\"]}},{\"range\":{\"@timestamp\":{\"gte\":\"2023-08-29 00:00:00\",\"lte\":\"2023-08-29 23:59:59\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":0,\"aggs\":{\"group_by_host\":{\"terms\":{\"field\":\"k_host\",\"size\":1000},\"aggs\":{\"latest_hit\":{\"top_hits\":{\"_source\":{\"includes\":[\"l_server_out_byte\",\"l_server_in_byte\",\"k_host\",\"log_time\"]},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":1}}}}}}";
            //String paramFormat="{\"track_total_hits\":10000,\"_source\":{},\"query\":{\"bool\":{\"must\":[{\"wildcard\":{\"log.file.path\":{\"value\":\"*ants_access*\"}}},{\"terms\":{\"k_host\":[%s]}},{\"range\":{\"@timestamp\":{\"gte\":\"%s\",\"lte\":\"%s\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}}}]}},\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":0,\"aggs\":{\"group_by_host\":{\"terms\":{\"field\":\"k_host\",\"size\":1000},\"aggs\":{\"latest_hit\":{\"top_hits\":{\"_source\":{\"includes\":[\"l_server_out_byte\",\"l_server_in_byte\"]},\"sort\":[{\"log_time\":{\"order\":\"desc\"}}],\"size\":1}}}}}}";
            //{"size":0,"query":{"bool":{"must":[{"range":{"timestamp":{"gte":0,"lte":1}}},{"nested":{"path":"v","query":{"match":{"v.p":"%s"}},"inner_hits":{}}}]}},"aggs":{"flow":{"nested":{"path":"v"},"aggs":{"flow_buckets":{"filter":{"match":{"v.p":"%s"}},"aggs":{"flow_in":{"sum":{"field":"v.i"}},"flow_out":{"sum":{"field":"v.o"}}}}}}}}
            //String paramFormat="{\"size\":0,\"query\":{\"bool\":{\"must\":[{\"range\":{\"timestamp\":{\"gte\":%d,\"lte\":%d}}},{\"nested\":{\"path\":\"v\",\"query\":{\"match\":{\"v.p\":\"%s\"}},\"inner_hits\":{}}}]}},\"aggs\":{\"flow\":{\"nested\":{\"path\":\"v\"},\"aggs\":{\"flow_buckets\":{\"filter\":{\"match\":{\"v.p\":\"%s\"}},\"aggs\":{\"flow_in\":{\"sum\":{\"field\":\"v.i\"}},\"flow_out\":{\"sum\":{\"field\":\"v.o\"}}}}}}}}";
            //"{\"size\":0,\"query\":{\"bool\":{\"must\":[{\"range\":{\"timestamp\":{\"gte\":%d,\"lte\":%d}}},{\"nested\":{\"path\":\"v\",\"query\":{\"match\":{\"v.p\":\"%s\"}},\"inner_hits\":{}}}]}},\"aggs\":{\"flow\":{\"nested\":{\"path\":\"v\"},\"aggs\":{\"flow_buckets\":{\"filter\":{\"match\":{\"v.p\":\"%s\"}},\"aggs\":{\"flow_in\":{\"sum\":{\"field\":\"v.i\"}},\"flow_out\":{\"sum\":{\"field\":\"v.o\"}}}}}}}}";
            //GETmetricbeat-8.9.1/_search
            // {"size":0,"query":{"bool":{"must":[{"range":{"@timestamp":{"gte":"now-1h","lte":"now"}}},{"match":{"event.dataset":"http.server_io_p"}},{"nested":{"path":"http.server_io_p.v","query":{"match":{"http.server_io_p.v.p":"17060826690650010"}},"inner_hits":{}}}]}},"aggs":{"flow":{"nested":{"path":"http.server_io_p.v"},"aggs":{"flow_buckets":{"filter":{"match":{"http.server_io_p.v.p":"17060826690650010"}},"aggs":{"flow_in":{"sum":{"field":"http.server_io_p.v.i"}},"flow_out":{"sum":{"field":"http.server_io_p.v.o"}}}}}}}}
            String paramFormat="{\"size\":0,\"query\":{\"bool\":{\"must\":[{\"range\":{\"@timestamp\":{\"gte\":%d,\"lte\":%d}}},{\"match\":{\"event.dataset\":\"http.server_io_p\"}},{\"nested\":{\"path\":\"http.server_io_p.v\",\"query\":{\"match\":{\"http.server_io_p.v.p\":\"%s\"}},\"inner_hits\":{}}}]}},\"aggs\":{\"flow\":{\"nested\":{\"path\":\"http.server_io_p.v\"},\"aggs\":{\"flow_buckets\":{\"filter\":{\"match\":{\"http.server_io_p.v.p\":\"%s\"}},\"aggs\":{\"flow_in\":{\"sum\":{\"field\":\"http.server_io_p.v.i\"}},\"flow_out\":{\"sum\":{\"field\":\"http.server_io_p.v.o\"}}}}}}}}";
            long sTm=startTime*1000l;
            long eTm=endTime*1000l;
            String param=String.format(paramFormat,sTm,eTm,cdnSuitEntity.getSerialNumber(),cdnSuitEntity.getSerialNumber());
            //logger.info(param);
            String res=  HttpRequest.erkHttp("GET",url,elkServerVo.getPwd(),param);
            //logger.info(res);
            if(StringUtils.isBlank(res)){
                logger.info(param);
                logger.info(res);
                logger.error("url:["+url+"],request error");
                if(streamFlowValue>0l){
                    this.insert2Consume(cdnSuitEntity, lastConsume,0l+streamFlowValue,startTime,endTime);
                }
                return;
            }
            //logger.info(res);
            //ElkFilterSiteIOFlowVo ioVo=DataTypeConversionUtil.string2Entity(res,ElkFilterSiteIOFlowVo.class);
            long suitFlowValue=0;
            ElkFilterSiteIOPFlowFilterVo ioVo=DataTypeConversionUtil.string2Entity(res,ElkFilterSiteIOPFlowFilterVo.class);
            if (null==ioVo){
                logger.info(param);
                logger.info(res);
                logger.error("url:["+url+"],flow  is  fail");
            }else{
                 suitFlowValue=ioVo.getSiteFlowData();
            }
            //logger.info(DataTypeConversionUtil.map2json(siteFlowInfo).toJSONString());
            if (suitFlowValue+streamFlowValue>0l){
                this.insert2Consume(cdnSuitEntity,lastConsume,suitFlowValue+streamFlowValue,startTime,endTime);
            }else{
               //logger.debug(url);
                //logger.debug(elkServerVo.getPwd());
               //logger.debug(param);
               //logger.debug(res);
               //logger.debug(cdnSuitEntity.getSerialNumber()+":["+sTm+","+eTm+"]suitFlowValue+streamFlowValue is zero ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 4层转发的流量
     * @param sn
     * @param elkServerVo
     * @return
     */
    private long getStreamConsume(String sn, ElkServerVo elkServerVo ){
        long result=0l;
        try{
            String url="http://"+elkServerVo.getHost()+":"+elkServerVo.getPort()+"/"+"filebeat-*/_search";
            String param="{\"size\":0,\"query\":{\"bool\":{\"must\":[{\"terms\":{\"s_sn\":[\""+sn+"\"]}},{\"wildcard\":{\"log.file.path\":{\"value\":\"*stream*\"}}},{\"range\":{\"@timestamp\":{\"gte\":\"now-5m\",\"lte\":\"now\"}}}]}},\"aggs\":{\"flow\":{\"terms\":{\"field\":\"s_sn\",\"order\":{\"_key\":\"asc\"},\"size\":1},\"aggs\":{\"flow-in\":{\"sum\":{\"field\":\"bytes_received\"}},\"flow-out\":{\"sum\":{\"field\":\"bytes_sent\"}},\"flow-total\":{\"bucket_script\":{\"buckets_path\":{\"flowInValue\":\"flow-in\",\"flowOutValue\":\"flow-out\"},\"script\":\"params.flowInValue + params.flowOutValue\"}}}}}}";
            String res=  HttpRequest.erkHttp("GET",url,elkServerVo.getPwd(),param);
            if(StringUtils.isBlank(res)){
                return result;
            }
            ElkFilterStreamIOFlowVo flowVo=DataTypeConversionUtil.string2Entity(res,ElkFilterStreamIOFlowVo.class);
            if (null==flowVo || null==flowVo.getAggregations() || null==flowVo.getAggregations().getFlow() || null==flowVo.getAggregations().getFlow().getBuckets() || flowVo.getAggregations().getFlow().getBuckets().isEmpty()){
                return result;
            }
            if (null==flowVo.getAggregations().getFlow().getBuckets().get(0) || null==flowVo.getAggregations().getFlow().getBuckets().get(0).getFlow_total()){
                return result;
            }
            return flowVo.getAggregations().getFlow().getBuckets().get(0).getFlow_total().getValue();

        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除6个月前的流量记录
     */
    private void deleteOneMonthBeforeFlowRecord(){
        Date t=DateUtils.addDateMonths(new Date(),-6);
        int temT= new Long(t.getTime()/1000).intValue();
        cdnConsumeDao.delete(new QueryWrapper<CdnConsumeEntity>().le("end_time",temT));
        cdnConsumeSiteDao.delete(new QueryWrapper<CdnConsumeSiteEntity>().le("end_time",temT));
    }


    private ElkServerVo getElkConfigInfo(){
        TbCdnPublicMutAttrEntity publicMutAttr=publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey", PublicEnum.ELK_CONFIG.getName())
                .eq("status",1)
                .last("limit 1")
        );
        if(null==publicMutAttr || StringUtils.isBlank(publicMutAttr.getPvalue())){
            logger.info("ELK_CONFIG is null ");
            return null;
        }
        return DataTypeConversionUtil.string2Entity(publicMutAttr.getPvalue(),ElkServerVo.class);
    }



    //流量入库 handle
    private void requestBytesRecordHandleThread()  {
        try{
            StaticVariableUtils.bytesTimeTemp=System.currentTimeMillis();
            logger.info("bytesTimeTemp:"+StaticVariableUtils.bytesTimeTemp);
            //清理一个月前的数据
            this.deleteOneMonthBeforeFlowRecord();
            //定义MODE
            Integer[] mode={1,2,3};
            //  //获取流量采集的url
            ElkServerVo elkServerVo=getElkConfigInfo();
            if (null==elkServerVo){
                logger.info("ELK_CONFIG is null ");
                return ;
            }
            // 根据MODE LIST 获取 1 2 3 类型套餐列表
            List<CdnSuitEntity> suitList=this.getFeeModeSuitList(mode);
            if ( null==suitList|| suitList.isEmpty()){
                logger.error("suitList is empty");
                return;
            }
            for (CdnSuitEntity cdnSuitEntity:suitList){
               //logger.debug("request_Bytes_Record_Handle_Thread Serial_Number:"+cdnSuitEntity.getSerialNumber());
                //ShellUtils.runShell("echo  "+cdnSuitEntity.getSerialNumber()+" >> /usr/ants/cdn-api/1.tlog");
                CdnConsumeEntity lastConsume=cdnConsumeDao.selectOne(new QueryWrapper<CdnConsumeEntity>()
                        .eq("serial_number",cdnSuitEntity.getSerialNumber())
                        .eq("attr_name", CdnConsumeAttrEnum.ATTR_FLOW.getName())
                        .orderByDesc("id")
                        .last("limit 1"));
                if (null==lastConsume){
                    //首次记录
                    Integer startTime= new Long(System.currentTimeMillis()/1000-(3600*24*180)).intValue();
                    Integer endTime=new Long( System.currentTimeMillis()/1000).intValue()-600;
                    //this.recordIntoConsume(cdnSuitEntity,elkServerVo,startTime,endTime);
                    this.recordFlowIntoConsumeBySp(cdnSuitEntity,null,elkServerVo,startTime,endTime);
                }
                if(null!=lastConsume){
                    //存在记录
                    Integer curTime=new Long( System.currentTimeMillis()/1000).intValue();
                    if ( (curTime-lastConsume.getEndTime()) >600){
                        Integer startTime=lastConsume.getEndTime();
                        Integer endTime=curTime;
                       //logger.debug(cdnSuitEntity.getSerialNumber()+":"+  startTime+"--->"+endTime);
                        //this.recordIntoConsume(cdnSuitEntity,elkServerVo,startTime,endTime);
                        this.recordFlowIntoConsumeBySp(cdnSuitEntity,lastConsume,elkServerVo,startTime,endTime);
                    }else{
                       //logger.debug(String.format("离上次记录不足3600秒,套餐[%s],LASTTIME=[%d],CURTIME=[%d]",cdnSuitEntity.getSerialNumber(),lastConsume.getEndTime(),curTime));
                    }
                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //获取指定套餐周期内的流量数据【前付费】
    private SuitFlowMode preBytesData(String SerialNumber, Date startTime, Date endTime){
        SuitFlowMode result=new SuitFlowMode();
        String start_=DateUtils.format(startTime) +" 00:00:00";
        String end_=DateUtils.format(endTime) +" 23:59:59";
        Date s_d=DateUtils.stringToDate(start_,DateUtils.DATE_TIME_PATTERN);
        Date e_d=DateUtils.stringToDate(end_,DateUtils.DATE_TIME_PATTERN);

        Integer s_tm=new Long(s_d.getTime()/1000).intValue();
        Integer ie_tm=new Long(e_d.getTime()/1000).intValue();
        List<CdnConsumeEntity> consumeList=cdnConsumeDao.selectList(new QueryWrapper<CdnConsumeEntity>()
                .eq("serial_number",SerialNumber)
                .eq("status",1)
                .ge("start_time",s_tm)
                .gt("s_value",0l)
                .le("end_time",ie_tm));
        for (CdnConsumeEntity consume:consumeList){
            result.getFlowData().put(consume.getStartTime(),consume.getSValue());
            result.setTotalFlow(result.getTotalFlow()+consume.getSValue());
            if (result.getTopFlow()<consume.getSValue()){
                result.setTopFlow(consume.getSValue());
            }
        }
        return result;
    }

    //获取指定套餐周期内平均日峰带宽数据【后付费】
    private SuitAverageMaxSpeedMode preMonthDayData(String SerialNumber, Date startTime, Date endTime ){
        SuitAverageMaxSpeedMode resultObj=new SuitAverageMaxSpeedMode();
        Date Near7Day_start_date=DateUtils.addDateDays(new Date(),-7);
        String start_=DateUtils.format(startTime) +" 00:00:00";
        //String end_=DateUtils.format(endTime) +" 23:59:59";
        Date f_d=DateUtils.stringToDate(start_,DateUtils.DATE_TIME_PATTERN);
        Integer i=0;
        do{
            Date index_date=DateUtils.addDateDays(f_d,i);
            if (index_date.after(endTime)){
                break;
            }
            Date index_end_date=DateUtils.LongStamp2Date(index_date.getTime()+3600*24*1000) ;
            List<CdnConsumeEntity> consumeList=cdnConsumeDao.selectList(new QueryWrapper<CdnConsumeEntity>()
                    .eq("serial_number",SerialNumber)
                    .eq("status",1)
                    .ge("start_time",index_date.getTime()/1000)
                    .le("end_time",index_end_date.getTime()/1000)
                    .gt("s_value",0l)
                    .orderByAsc("id")
            );
            long index_day_max_speed=0L;
            for (CdnConsumeEntity consume:consumeList){
                long cur_point_speed=consume.getSValue()/(consume.getEndTime()-consume.getStartTime());
                cur_point_speed=cur_point_speed*8;//Byte/s==>byte/s
                index_day_max_speed=cur_point_speed>index_day_max_speed?cur_point_speed:index_day_max_speed;
                resultObj.setTotalFlow(resultObj.getTotalFlow()+consume.getSValue());
                resultObj.getAllSpeedData().put(consume.getStartTime(),cur_point_speed);
                if (Near7Day_start_date.before(index_date)){
                    resultObj.getNear7DayAllSpeedData().put(consume.getStartTime(),cur_point_speed);
                }
                if (resultObj.getTopSpeed()<cur_point_speed){
                    resultObj.setTopSpeed(cur_point_speed);
                }
            }
            if(index_day_max_speed>1024){
                //max_day_i.add(max_i);
                Integer t=Math.round(index_date.getTime()/1000);
                long v=index_day_max_speed;
                resultObj.getAvailableMaxSpeedData().put(t,v);
                resultObj.setMonthAvailableDaySum(resultObj.getMonthAvailableDaySum()+1); ;
            }else {
                //result1.put((int)(index_date.getTime()/1000),0);
                //logger.info(index_date + "max_i="+max_i+",不计算！");
            }
            i++;
        }while (true);
        if (true){
            long allSumMaxValue= 0L;
            for (Integer tKey:resultObj.getAvailableMaxSpeedData().keySet()){
                allSumMaxValue+=resultObj.getAvailableMaxSpeedData().get(tKey);
            }
            resultObj.setAvailableMaxSpeedSum(allSumMaxValue);
            if (allSumMaxValue>0){
                long final_bytes_value=allSumMaxValue/resultObj.getMonthAvailableDaySum();
                resultObj.setFinalAvailableSpeedBytes(final_bytes_value);
            }else {
                logger.error(SerialNumber+" SumMaxValue5 is zero");
            }

        }
        return  resultObj;
    }

    //获取指定套餐周期内月95带宽数据【后付费】
    private SuitMonth95SpeedMode preMonth95Data(String SerialNumber, Date startTime, Date endTime){
        SuitMonth95SpeedMode resultObj=new SuitMonth95SpeedMode();
        //在一个自然月内，将每个有效日的所有峰值带宽的统计点进行排序，去掉数值最高的5%的统计点，取剩下的数值最高统计点，该点就是95峰值的计费点。
        Date Near7Day_start_date=DateUtils.addDateDays(new Date(),-7);
        String start_=DateUtils.format(startTime) +" 00:00:00";
        Date f_start_date=DateUtils.stringToDate(start_,DateUtils.DATE_TIME_PATTERN);
        //Integer first_day_i=DateUtils.getCalendarByDate(startTime).get(Calendar.DAY_OF_MONTH);
        //Integer end_day_i=DateUtils.getCalendarByDate(endTime).get(Calendar.DAY_OF_MONTH);
        int i=0;
        do{
            List<Long>day_i=new ArrayList<>();
            Date index_start_date=DateUtils.addDateDays(f_start_date,i);
            if(index_start_date.after(endTime)){
                break;
            }
            Date index_end_date=DateUtils.LongStamp2Date(index_start_date.getTime()+3600*24*1000) ;
            float cur_index_day_max_speed=0;
            List<CdnConsumeEntity> consumeList=cdnConsumeDao.selectList(new QueryWrapper<CdnConsumeEntity>()
                    .eq("serial_number",SerialNumber)
                    .eq("status",1)
                    .ge("start_time",index_start_date.getTime()/1000)
                    .le("end_time",index_end_date.getTime()/1000)
                    .gt("s_value",0l)
                    .orderByAsc("id")
            );
            for (CdnConsumeEntity consume:consumeList){
                //当前点带宽
                long cur_point_speed=(consume.getSValue()/(consume.getEndTime()-consume.getStartTime()));
                cur_point_speed=cur_point_speed*8;//Byte/s==>byte/s
                cur_index_day_max_speed=cur_point_speed>cur_index_day_max_speed?cur_point_speed:cur_index_day_max_speed;
                day_i.add(cur_point_speed );
                resultObj.getAllSpeedData().put(consume.getStartTime(),cur_point_speed);
                resultObj.setTotalFlow(resultObj.getTotalFlow()+consume.getSValue());
                if (resultObj.getMaxM95SpeedBytesData()<cur_point_speed){
                    resultObj.setMaxM95SpeedBytesData(cur_point_speed);
                }
                if (cur_point_speed>1024){
                    resultObj.getAvailableM95SpeedData().add(cur_point_speed);
                }
                if (Near7Day_start_date.before(index_start_date)){
                    resultObj.getNear7DayAllSpeedData().put(consume.getStartTime(),cur_point_speed);
                }

            }
            if(cur_index_day_max_speed>1024){
                resultObj.getDayKeySpeedMap().put(i,day_i);
                resultObj.setMonthAvailableDaySum(resultObj.getMonthAvailableDaySum()+1);
            }else {
                //logger.debug(index_date + "max_i="+max_i+",当天不计算！");
            }
            i++;
        }while (true);
        int t95Index=0;
        long t95Value=0;
        if (resultObj.getAvailableM95SpeedData().size()>0){
            Collections.sort(resultObj.getAvailableM95SpeedData());
            for (Long v:resultObj.getAvailableM95SpeedData()){
                System.out.println(v+",");
            }
            t95Index= new Long(Math.round(resultObj.getAvailableM95SpeedData().size()*0.95)).intValue();
            if (t95Index>=resultObj.getAvailableM95SpeedData().size()){
                t95Index=resultObj.getAvailableM95SpeedData().size()-1;
            }
            t95Value=resultObj.getAvailableM95SpeedData().get(t95Index);

        }
        resultObj.setM95SpeedIndex(t95Index);
        resultObj.setFinalM95SpeedBytesData(t95Value);
        return resultObj;
    }




    /**
     * set updateUsedFlow setUsedTypeDetail
     * @param suitEntity
     */
    @Override
    public void updateUsedFlow(CdnSuitEntity suitEntity ){
        //logger.error("updateUsedFlow");
        //logger.info(suitEntity.toString());
        CdnConsumeUsedInfoVo res=new CdnConsumeUsedInfoVo();
        //获取主套餐
        Date now=new Date();
        //Integer[] types={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        //Integer[] statusS={CdnSuitStatusEnum.UNKNOWN.getId(),CdnSuitStatusEnum.NORMAL.getId()};
        if (null==suitEntity){
            logger.info("suit is null");
            return;
        }
        //Date buyStartTm=suitEntity.getStartTime();
        Date buyStartTm=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>().eq("serial_number",suitEntity.getSerialNumber()).select("start_time").orderByAsc("start_time").last("limit 1")).getStartTime();
        //Date buyEndTm=suitEntity.getEndTime();
        Date buyEndTm=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>().eq("serial_number",suitEntity.getSerialNumber()).select("end_time").orderByDesc("end_time").last("limit 1")).getEndTime();
        //当前时间处于套餐的阶段
        Date curDateLastStartDate=null;
        Date curDateLastEndDate=null;
        int i=0;
        while (true){
            Date next_month_date=DateUtils.addDateMonths(buyStartTm,i);
            if(next_month_date.after(now)){
                curDateLastStartDate=DateUtils.addDateMonths(next_month_date,-1);
                if(next_month_date.before(buyEndTm)){
                    curDateLastEndDate=next_month_date;
                }else{
                    curDateLastEndDate=buyEndTm;
                }
                break;
            }
            i++;
        }
        if(null==curDateLastStartDate || null==curDateLastEndDate){
            logger.error("获取套餐时间错误");
            return ;
        }
        //logger.info(curDateLastStartDate.toString());
        //logger.info(curDateLastEndDate.toString());
        List<CdnConsumeEntity> list=cdnConsumeDao.selectList(new QueryWrapper<CdnConsumeEntity>()
                .eq("serial_number",suitEntity.getSerialNumber())
                .eq("attr_name",ProductAttrNameEnum.ATTR_FLOW.getAttr())
                .ge("start_time",new Long(curDateLastStartDate.getTime()/1000).intValue())
                .le("end_time",new Long(curDateLastEndDate.getTime()/1000).intValue())
                .gt("s_value",0l)
                .select("s_value")
                .and(q->q.eq("r_type",0).or().isNull("r_type"))
        );
        //logger.info(list.toString());
        for (CdnConsumeEntity c:list){
            BigDecimal gbFlow=new BigDecimal(c.getSValue()).divide(new BigDecimal(ONE_THOUSAND*ONE_THOUSAND*ONE_THOUSAND));
            res.setMainFlowGb(res.getMainFlowGb().add(gbFlow));
        }
        List<CdnSuitEntity>addList=cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                .eq("serial_number",suitEntity.getSerialNumber())
                .eq("suit_type",OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())
                .le("start_time",now)
                .ge("end_time",now)
        );
        //logger.info(addList.toString());
        for (CdnSuitEntity s:addList ){
            if (null==s.getUsedFlow()){
                s.setUsedFlow(BigDecimal.ZERO);
            }
            res.setAddFlowGb(res.getAddFlowGb().add(s.getUsedFlow()));
        }
        res.setTotalFlowGb(res.getAddFlowGb().add(res.getMainFlowGb()));
        //logger.info("used flowF=["+flowF+"]GB");
        //suitEntity.getConsume().put(ProductAttrNameEnum.ATTR_FLOW.getAttr(),res.getTotalFlowGb().longValue());
        if (null!=res.getTotalFlowGb() && null!=suitEntity.getConsume()){
            suitEntity.getConsume().setFlow(res.getTotalFlowGb().longValue());
        }
        suitEntity.setUsedFlow(res.getTotalFlowGb());
        suitEntity.setUsedTypeDetail(res);
    }

    /*已使用信息*/
    private void updateAlreadyConsumeInfo(CdnSuitEntity targetSuit){
        ProductAttrVo vo=new ProductAttrVo();
        //targetSuit.setConsume(DataTypeConversionUtil.entity2jsonObj(vo));
        targetSuit.setConsume(vo);
        ProductAttrNameEnum[] objs= ProductAttrNameEnum.values();
        for (ProductAttrNameEnum item:objs){
            switch (item.getAttr()){
                case "defense":
                    //todo defense
                    break;
                case "flow":
                    if(true){
                      this.updateUsedFlow(targetSuit);
                    }
                    break;
                case "site":
                    if (true){
                        Integer sum=tbSiteDao.selectCount(new QueryWrapper<TbSiteEntity>().eq("serial_number",targetSuit.getSerialNumber()));
                        vo.setSite(sum);
                    }
                    break;
                case "sms":
                    //todo sms
                    break;
                case "port_forwarding":
                    if (true){
                        Integer sum=tbStreamProxyDao.selectCount(new QueryWrapper<TbStreamProxyEntity>().eq("serial_number",targetSuit.getSerialNumber()));
                        vo.setPort_forwarding(sum);
                    }
                    break;
                default:
                    break;
            }
        }
        //targetSuit.setConsume(DataTypeConversionUtil.entity2jsonObj(vo));
        targetSuit.setConsume(vo);
    }

    //通过serialNumber获取套餐
    @Override
    public CdnSuitEntity commGetSuitDetail(Long userId, String serialNumber, List<Integer> typeList,boolean usedInfo) {
        if(StringUtils.isBlank(serialNumber)){
            return null;
        }
        //Integer[] sType={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        CdnSuitEntity suit=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq(null!=userId,"user_id",userId)
                .eq("serial_number",serialNumber)
                .in("suit_type",typeList.toArray())
                //.eq("status",CdnSuitStatusEnum.NORMAL.getId())
                .orderByDesc("id")
                .last("limit 1"));
        if(null==suit){
           //logger.debug("order is null");
            return null;
        }
        return this.updateThisSuitDetailInfo(suit,usedInfo);
    }

    private Integer getSuitModeFromSuitAttrStr(String attrStr){
        Integer suitMode=1;
        if (StringUtils.isNotBlank(attrStr)){
            //{"live_data":1,"site":12,"charging_mode":1,"bandwidth":200000,"sms":100,"private_waf":1,"monitor":1,"flow":100,"port_forwarding":5,"ai_waf":1,"public_waf":1}
            JSONObject attrJson=DataTypeConversionUtil.string2Json(attrStr);
            if (null!=attrJson && attrJson.containsKey(ProductAttrNameEnum.ATTR_CHARGING_MODE.getAttr())){
                suitMode=attrJson.getIntValue(ProductAttrNameEnum.ATTR_CHARGING_MODE.getAttr());
            }
        }
        return suitMode;
    }

    private Integer getSuitUnitPriceFromSuitAttrStr(String attrStr){
        Integer suitMode=0;
        if (StringUtils.isNotBlank(attrStr)){
            //{"live_data":1,"site":12,"charging_mode":1,"bandwidth":200000,"sms":100,"private_waf":1,"monitor":1,"flow":100,"port_forwarding":5,"ai_waf":1,"public_waf":1}
            JSONObject attrJson=DataTypeConversionUtil.string2Json(attrStr);
            if (null!=attrJson && attrJson.containsKey(ProductAttrNameEnum.ATTR_BANDWIDTH_PRICE.getAttr())){
                suitMode=attrJson.getIntValue(ProductAttrNameEnum.ATTR_BANDWIDTH_PRICE.getAttr());
            }
        }
        return suitMode;
    }

    @Override
    public CdnSuitEntity stopUsePostpaidSuit(Long userId, String serialNumber) {
        Integer[] statusS={CdnSuitStatusEnum.NORMAL.getId(),CdnSuitStatusEnum.UNKNOWN.getId()};
        List<CdnSuitEntity> list=cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                .eq(null!=userId,"user_id",userId)
                .eq("serial_number",serialNumber)
                .eq("suit_type",OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                .in("status",statusS)
        );
        LocalDate date = LocalDate.now();
        LocalDate firstDay = date.with(TemporalAdjusters.firstDayOfMonth()); // 获取当前月的第一天
        LocalDate lastDay; // 获取当前月的的最后一天
        lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
        String f_date_str=firstDay.format(DateTimeFormatter.ISO_DATE)+" 00:00:00";
        Date first_d=DateUtils.stringToDate(f_date_str,DateUtils.DATE_TIME_PATTERN);
        String l_date_str=lastDay.format(DateTimeFormatter.ISO_DATE)+" 23:59:59";
        Date last_d=DateUtils.stringToDate(l_date_str,DateUtils.DATE_TIME_PATTERN);
        for (CdnSuitEntity itemSuitEntity:list ){
            //attr_json charging_mode
            Integer suitMode=this.getSuitModeFromSuitAttrStr(itemSuitEntity.getAttrJson());
            if (2==suitMode ){
                this.createMode2OrderAndBalancePay(itemSuitEntity,lastDay,first_d,last_d,"(清算)");
                itemSuitEntity.setStatus(CdnSuitStatusEnum.LIQUIDATION.getId());
                cdnSuitDao.updateById(itemSuitEntity);
                //stop services
                Map<String,String>map=new HashMap<>();
                map.put(PushTypeEnum.CLEAN_CLOSE_SUIT_SERVICE.getName(),itemSuitEntity.getSerialNumber());
                cdnMakeFileService.pushByInputInfo(map);
            }else if(3==suitMode){
                this.createMode3OrderAndBalancePay(itemSuitEntity,lastDay,first_d,last_d,"(清算)");
                itemSuitEntity.setStatus(CdnSuitStatusEnum.LIQUIDATION.getId());
                cdnSuitDao.updateById(itemSuitEntity);
                //stop services
                Map<String,String>map=new HashMap<>();
                map.put(PushTypeEnum.CLEAN_CLOSE_SUIT_SERVICE.getName(),itemSuitEntity.getSerialNumber());
                cdnMakeFileService.pushByInputInfo(map);
            }else {
                logger.error("this mode con't stop!");
            }
        }
        return null;
    }


    private R removeDnsRecordByApplyCertSuccess(int siteId,int dnsconfigid ){
        try{
            TbSiteEntity siteEntity= tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",siteId).select("id,main_server_name").last("limit 1"));
            if (null==siteEntity){
                return R.error("removeDns : site is not found");
            }
            R r1=cdnMakeFileService.getDomainAndAlias(siteEntity,false);
            if (0==r1.getCode()){
                return r1;
            }
            List<TbCertifyEntity>  excessCEList=tbcertifyDao.selectList(new QueryWrapper<TbCertifyEntity>()
                    .and(q->q.eq("site_id",siteId).or().eq("common_name",r1.get("data").toString()))
                    .in("src_type",CertSrcTypeEnums.getAllDnsTypes())
                    .orderByDesc("id")
            );
            String mDomain=DomainUtils.getMainTopDomain(siteEntity.getMainServerName());
            int delCount=0;
            for (TbCertifyEntity certify:excessCEList){
                if (CertSrcTypeEnums.CertServerLetDnsV2.getType()==certify.getSrcType()|| CertSrcTypeEnums.CertServerZeroDnsV2.getType()==certify.getSrcType()){
                    // let-dns-v2 || zero_ssl_dns
                    if (null==certify || StringUtils.isBlank(certify.getApplyInfo()) || StringUtils.isBlank(certify.getApiOrderInfo())){
                        logger.info(" cert info is null");
                        continue;
                    }
                    int dnsConfigIdValue=dnsconfigid;
                    if (0==dnsConfigIdValue){
                        CertServerApplyForm applyForm=DataTypeConversionUtil.string2Entity(certify.getApplyInfo(),CertServerApplyForm.class);
                        if (null==applyForm || 0==applyForm.getDnsconfigid()){
                            logger.info("01 apply dns_config is null");
                            continue;
                        }else{
                            dnsConfigIdValue= applyForm.getDnsconfigid();
                        }
                    }

                    CertCallbackForm dnsAddForm=DataTypeConversionUtil.string2Entity(certify.getApiOrderInfo(),CertCallbackForm.class);
                    if (null==dnsAddForm || null==dnsAddForm.getDnsRecordInfo()){
                        logger.info("02 apply info is null");
                        continue;
                    }
                    String mTop=dnsAddForm.getDnsRecordInfo().getTop();
                    if (StringUtils.isNotBlank(mTop)){
                        if (mTop.contains(".@")){
                            mTop= mTop.replace(".@","");
                        }else if (mTop.contains("@")){
                            mTop= mTop.replace("@","");
                        }
                    }
                    delCount++;
                    R r2=dnsCApiService.removeRecordByInfoWithMainDomain(dnsConfigIdValue,mDomain,mTop,dnsAddForm.getDnsRecordInfo().getType(),"","","");
                    logger.info(String.format("remove_dns_record ,confId=%d,type=%s,top=%s,domain=%s ;result:%s  ",dnsConfigIdValue,dnsAddForm.getDnsRecordInfo().getType(),mTop,mDomain,r2.toJsonString()));
                }else if (certify.getSrcType()==CertSrcTypeEnums.LetsencryptDns.getType()){
                    // let dns v1
                    if (null==certify || StringUtils.isBlank(certify.getApplyInfo())){
                        continue;
                    }
                    AcmeDnsVo vo=DataTypeConversionUtil.string2Entity(certify.getApplyInfo(),AcmeDnsVo.class);
                    if (null==vo) {
                        continue;
                    }
                    if (0==vo.getDnsConfigId() || null==vo.getTvMap() || 0==vo.getTvMap().size()){
                        continue;
                    }
                    for (String key:vo.getTvMap().keySet()){
                       if (StringUtils.isNotBlank(vo.getTvMap().get(key).getValue())){
                           delCount++;
                           R r2=dnsCApiService.removeRecordByInfoWithMainDomain(vo.getDnsConfigId(),mDomain,vo.getTvMap().get(key).getTop(),vo.getTvMap().get(key).getType(),"","","");
                           logger.info("remove_dns_record 2:"+r2.toJsonString());
                       }
                    }
                }
            }
            return R.ok().put("delCount",delCount);
        }catch (Exception e){
            e.printStackTrace();
        }
       return R.error();
    }

    @Override
    public R saveCertCallback(CertCallbackForm form) {
        ValidatorUtils.validateEntity(form);
        String eMsg="";
        try {
            logger.info("save_Cert_Call_back_form:"+DataTypeConversionUtil.entity2jonsStr(form));
            if ( 0==form.getType() ){
                //0 cert_callback
                if (1==form.getCode()){
                    byte[] decodedPemBytes = Base64.getDecoder().decode(form.getPem().getBytes());
                    String pem = new String(decodedPemBytes, "UTF-8");
                    form.setPem(pem);

                    byte[] decodedKeyBytes =Base64.getDecoder().decode(form.getKey().getBytes());
                    String key = new String(decodedKeyBytes, "UTF-8");
                    form.setKey(key);
                    //logger.info("form:"+DataTypeConversionUtil.entity2jonsStr(form));
                }
                // del dns-record
                R r2=this.removeDnsRecordByApplyCertSuccess(form.getSiteId(),form.getDnsconfigid());
                logger.info("remove_DnsRecordByApplyCert:"+r2.toJsonString());
                return cdnMakeFileService.saveCert2Db(form);
            }else if (1== form.getType() ){
                //1 add_dns_record
                if (null==form.getDnsRecordInfo()){
                    return R.error("dns record is required");
                }
                TbSiteEntity siteEntity= tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",form.getSiteId()).select("id,main_server_name").last("limit 1"));
                if (null==siteEntity){
                    return R.error( String.format("site [%d] is not found",form.getSiteId()) );
                }
                TbCertifyEntity certify= tbcertifyDao.selectOne(new QueryWrapper<TbCertifyEntity>()
                        .eq("site_id",form.getSiteId())
                        .in("src_type",CertSrcTypeEnums.getAllDnsTypes())
                        .orderByDesc("id")
                        .last("limit 1")
                );
                if (null==certify || StringUtils.isBlank(certify.getApplyInfo())){
                    return R.error("not found certificate in db");
                }
                certify.setApiOrderInfo(DataTypeConversionUtil.entity2jonsStr(form));
                tbcertifyDao.updateById(certify);
                CertServerApplyForm applyForm=DataTypeConversionUtil.string2Entity(certify.getApplyInfo(),CertServerApplyForm.class);
                if (null==applyForm || 0==applyForm.getDnsconfigid()){
                    return R.error("apply form is valid");
                }
                String mDomain=DomainUtils.getMainTopDomain(siteEntity.getMainServerName());
                //R r1=dnsCApiService.removeRecordByInfoWithMainDomain(applyForm.getDnsconfigid(),mDomain,form.getDnsRecordInfo().getTop(),form.getDnsRecordInfo().getType(),"","","");
                //logger.info("removed record:"+r1.toJsonString());
                String mTop=form.getDnsRecordInfo().getTop();
                if (StringUtils.isNotBlank(mTop)){
                    if (mTop.contains(".@")){
                        mTop= mTop.replace(".@","");
                    }else if (mTop.contains("@")){
                        mTop= mTop.replace("@","");
                    }
                }
                Object rAddObj=dnsCApiService.addRecordByConfIdWithMainDomain(applyForm.getDnsconfigid(),mDomain,mTop,form.getDnsRecordInfo().getType(),"",form.getDnsRecordInfo().getValue(),"600");
                logger.info("add_dns_record:"+rAddObj.toString());
                return R.ok().put("data",rAddObj);
            }else{
                eMsg="param is valid:"+DataTypeConversionUtil.entity2jonsStr(form);
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        logger.error(eMsg);
        return R.error(eMsg);
    }

    @Override
    public R saveSiteAttr(Map<String, Object> params) {
        return tbSiteServer.saveSiteAttr(params);
    }


    private void updateSiteCnameInSysAttr(TbSiteAttrEntity siteAttr, boolean flag){
        siteAttr.setPvalue1(System.currentTimeMillis());
        siteAttr.setPvalue(flag?"1":"0");
        siteAttr.setStatus(1);
        tbSiteAttrDao.updateById(siteAttr);
    }

    @Override
    public void checkSiteCname() {
        //----
        try{
            List<TbSiteEntity>list=tbSiteDao.selectList(null);
            List<Integer>allSiteIds=list.stream().map(o->o.getId()).collect(Collectors.toList());
            List<TbSiteAttrEntity>tbSiteAttrList= tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("pkey", SiteAttrEnum.JOB_CHECK_SITE_CNAME.getName())
                    .select("site_id")
                    .orderByAsc("pvalue1")
            );
            List<Integer>allSiteCheckAttrList=tbSiteAttrList.stream().map(o->o.getSiteId()).collect(Collectors.toList());
            allSiteIds.removeAll(allSiteCheckAttrList);
            for (Integer siteId:allSiteIds){
                TbSiteAttrEntity siteAttr=new TbSiteAttrEntity();
                siteAttr.setSiteId(siteId);
                siteAttr.setPkey(SiteAttrEnum.JOB_CHECK_SITE_CNAME.getName());
                tbSiteAttrDao.insert(siteAttr);
            }
            //----
            List<TbSiteAttrEntity>TbSiteAttrList= tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                    .eq("pkey",SiteAttrEnum.JOB_CHECK_SITE_CNAME.getName())
                    .orderByAsc("pvalue1")
                    .last("limit 20")
            );
            for(  TbSiteAttrEntity siteAttr:TbSiteAttrList){
                TbSiteEntity site=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",siteAttr.getSiteId()).select("main_server_name").last("limit 1"));
                if (null==site){
                    continue;
                }
                boolean CHECK_MODE_BY_CNAME_DNS=false;
                boolean CHECK_MODE_BY_ELK=false;
                boolean CHECK_MODE_BY_GET=true;
                //boolean CHECK_MODE_BY_IN_NODE=false;
                if (CHECK_MODE_BY_ELK){
                    String param="{\"query\":{\"bool\":{\"must\":[{\"range\":{\"@timestamp\":{\"gte\":\"now-1d\",\"lte\":\"now\"}}},{\"match\":{\"k_host\":\""+site.getMainServerName()+"\"}}]}},\"size\":0}";
                    R r=tbSiteServer.queryElk("GET","filebeat-*/_search",param);
                    if (1==r.getCode() && r.containsKey("data")){
                        String ret=r.get("data").toString();
                        ElkFilterCnameTotalVo cvo=DataTypeConversionUtil.string2Entity(ret,ElkFilterCnameTotalVo.class);
                        if (null!=cvo
                                && null!=cvo.getHits()
                                && null!=cvo.getHits().getTotal()
                                && null!=cvo.getHits().getTotal().getValue()
                                && 0!=cvo.getHits().getTotal().getValue())
                        {
                            updateSiteCnameInSysAttr(siteAttr,true);
                            continue;
                        }
                    }
                    //未获取到更新为0
                    HttpRequest.sendGet("http://"+site.getMainServerName(),"");
                }
                if (CHECK_MODE_BY_CNAME_DNS){
                    TbSiteEntity siteSuitEntity=tbSiteServer.getSiteSuitInfo(null,siteAttr.getSiteId());
                    if (null!=siteSuitEntity){
                        if (StringUtils.isNotBlank(siteSuitEntity.getCname())){
                            boolean flag= IPUtils.checkDomainInCname(siteSuitEntity.getMainServerName(),siteSuitEntity.getCname());
                            if (flag){
                                updateSiteCnameInSysAttr(siteAttr,true);
                                continue;
                            }
                        }
                    }
                }
                if (CHECK_MODE_BY_GET){
                    boolean flag=HttpRequest.isExistInAntsWaf("http://"+site.getMainServerName());
                    if (flag){
                        updateSiteCnameInSysAttr(siteAttr,true);
                        continue;
                    }
                }
                updateSiteCnameInSysAttr(siteAttr,false);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
        }


    }

    //检测前付费套餐流量|防御等属性超出情况 handle
    private  void paid1TaskHandle(){
        //Date now =new Date();
        Integer[] sType={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        Integer[] mode={1};
        List<CdnSuitEntity> suitList= this.getFeeModeSuitList(mode);
        for (CdnSuitEntity suitEntity:suitList){
            CdnSuitEntity suitObj=this.commGetSuitDetail(null,suitEntity.getSerialNumber(),Arrays.asList(sType),true);
            if(null==suitObj){
                logger.error("["+suitEntity.getSerialNumber()+"] 获取套餐失败！");
                continue;
            }
            if (false && new Date().after(suitObj.getEndTime())){
                //套餐过期  停用站与站点--正常不会走这里
                String[] types={"suit","site","proxy"};
                this.timeOutDisableSuitProduct(suitEntity,types,MEAL_OUT);
                continue;
            }
            if (null==suitObj.getAttr()){
                logger.error("[paid_1_TaskHandle]获取当前套餐失败[0]！");
                continue;
            }
            //ProductAttrVo maxAttrVo=DataTypeConversionUtil.json2entity(suitObj.getAttr(),ProductAttrVo.class) ;
            ProductAttrVo maxAttrVo=suitObj.getAttr();
            if (null!= maxAttrVo && null!=suitObj.getConsume()){
                // //当前套餐已用量
                //ProductAttrVo usedConsumeObj= DataTypeConversionUtil.json2entity(suitObj.getConsume(),ProductAttrVo.class);
                ProductAttrVo usedConsumeObj=suitObj.getConsume();
                if (null==usedConsumeObj){
                    continue;
                }
                if (Arrays.asList(POST_PAID_MODE).contains(maxAttrVo.getCharging_mode())){
                    //不处理后付费
                    continue;
                }
                //
                if (maxAttrVo.getFlow()<usedConsumeObj.getFlow()){
                    String[] types={"suit","site","proxy"};
                    this.timeOutDisableSuitProduct(suitEntity,types,FLOW_OUT);
                    String warnStr="套餐["+suitObj.getSerialNumber()+"],【流量】已超出！";
                    logger.warn("[paid_1_TaskHandle]"+warnStr);
                }
            }
        }
    }



    //创建日均带宽费用
    private  void createMode2OrderAndBalancePay(CdnSuitEntity cdnSuitEntity, LocalDate lastDay, Date first_d, Date last_d, String order_tips_info){
        JSONObject orderInitJsonObject=new JSONObject();
        TbOrderEntity order=new TbOrderEntity();
        if (true){
            //获取源订单产品属性 //从老订单中获取产品属性更新当前订单
            TbOrderEntity source_order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("user_id",cdnSuitEntity.getUserId())
                    .eq("order_type",OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                    .eq("serial_number",cdnSuitEntity.getSerialNumber())
                    .last("limit 1"));
            if (null!=source_order){
                order.setTargetId(source_order.getTargetId());
                if (StringUtils.isNotBlank(source_order.getInitJson())){
                    JSONObject init_json=DataTypeConversionUtil.string2Json(source_order.getInitJson());
                    if ( null!=init_json && init_json.containsKey("product_obj")){
                        JSONObject product_obj=init_json.getJSONObject("product_obj");
                        if (product_obj.containsKey("id")){
                            order.setTargetId(product_obj.getInteger("id"));
                        }
                        orderInitJsonObject.put("product_obj",product_obj);
                    }
                }

            }
        }
        orderInitJsonObject.put("serial_number",cdnSuitEntity.getSerialNumber());
        String dateBuffer=lastDay.getYear()+"-"+lastDay.getMonthValue();
        orderInitJsonObject.put("date",dateBuffer);
        orderInitJsonObject.put("remark","["+dateBuffer+"]平均日峰带宽系统月结扣费"+order_tips_info);
        orderInitJsonObject.put("name","pre_month_day");
        orderInitJsonObject.put("mode",2);
        if (true){
            //检测是否扣费
            // {"date":"2022-8","mode":2,"name":"pre_month_day","serial_number":"1662409121577004","remark":"平均日峰带宽系统月结扣费","product_obj":{"createtime":1658542776000,"productJson":"{\"m\":{\"value\":0,\"status\":1},\"s\":{\"value\":100,\"status\":1},\"y\":{\"value\":100,\"status\":1}}","name":"平均日峰带宽套餐","serverGroupIds":"1","attrJson":"[{\"attr\":\"charging_mode\",\"name\":\"charging_mode\",\"value\":2,\"valueType\":\"select\"},{\"attr\":\"flow\",\"name\":\"flow\",\"valueType\":\"int\",\"unit\":\"G\",\"value\":10000},{\"attr\":\"bandwidth\",\"name\":\"bandwidth\",\"valueType\":\"price_int\",\"unit\":\"元/Mbps/月\",\"value\":20000},{\"attr\":\"ai_waf\",\"id\":27,\"name\":\"AI WAF\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"defense\",\"id\":38,\"name\":\"防御\",\"valueType\":\"int\",\"unit\":\"G\",\"value\":100,\"hiddenUsed\":true},{\"attr\":\"public_waf\",\"id\":37,\"name\":\"WAF\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"port_forwarding\",\"id\":36,\"name\":\"端口转发\",\"valueType\":\"int\",\"unit\":\"个\",\"value\":5},{\"attr\":\"site\",\"id\":35,\"name\":\"站点\",\"valueType\":\"int\",\"unit\":\"个\",\"value\":10},{\"attr\":\"sms\",\"id\":34,\"name\":\"短信通知\",\"valueType\":\"int\",\"unit\":\"条/月\",\"value\":100,\"hiddenUsed\":true},{\"attr\":\"monitor\",\"id\":33,\"name\":\"流量监控\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"private_waf\",\"id\":32,\"name\":\"专属WAF\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"live_data\",\"id\":30,\"name\":\"实时数据\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"custom_dns\",\"id\":39,\"name\":\"自定义dns\",\"valueType\":\"bool\",\"value\":\"1\"}]","weight":1,"id":17,"productType":10,"status":1}}
            String likeDate1=lastDay.getYear()+"-"+lastDay.getMonth();
            String likeDate2=lastDay.getYear()+"-"+lastDay.getMonthValue();
            String l1=String.format("\"date\":\"%s\"",likeDate1);
            String l2=String.format("\"date\":\"%s\"",likeDate2);
            String e1=String.format("\"serial_number\":\"%s\"",cdnSuitEntity.getSerialNumber());
            String e2=String.format("\"mode\":%d",2);
            TbOrderEntity exist_order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("order_type",OrderTypeEnum.ORDER_SYS_PAID.getTypeId())
                    .eq("user_id",cdnSuitEntity.getUserId())
                    .and(q->q.like("init_json",l1).or().like("init_json",l2))
                    .like("init_json",e1)
                    .like("init_json",e2)
                    .last("limit 1"));
            if(null!=exist_order){
               //logger.debug("【paid_2_TaskHandle】order is exist ");
                return;
            }
        }
        SuitAverageMaxSpeedMode av_speed_obj=this.preMonthDayData(cdnSuitEntity.getSerialNumber(),first_d,last_d);
        //获取单价
        int unit_price=this.getSuitUnitPriceFromSuitAttrStr(cdnSuitEntity.getAttrJson());
        if (0==unit_price){
            logger.error("【paid_2_TaskHandle】price value is 0");
        }
        //<ul>
        //<li>1. 例如客户从1月1日正式开始计费，签订的合同价格为：P 元/Mbps/月。</li>
        //<li>2. 有效天：产生的消耗 ＞ 0，则记为有效天。</li>
        //<li>3. 假设客户1月份有14天有效天，这14天有效天每一天的288个统计点最大值为：Max_1、Max_2、Max_3... Max_14，
        // 计费带宽为 Average(Max_1, Max_2, ..., Max_14)，1月的费用为：Average(Max_1, Max_2, ..., Max_14) * P * 14 / 31。</li>
        //</ul>
        long need_paid=0;//需支付金额
        JSONObject payJsonObj=new JSONObject();
        if ( null==av_speed_obj){
            logger.error("max_day_map_obj is lost param");
            return;
        }  else{
            int available_day_sum=av_speed_obj.getMonthAvailableDaySum();
            long  SumMaxValue5= av_speed_obj.getAvailableMaxSpeedSum();
            long  Average_spend_bytes=av_speed_obj.getFinalAvailableSpeedBytes();
            double  Average_spend_M_bytes=Average_spend_bytes/(ONE_THOUSAND*ONE_THOUSAND);
            int  thisMonthSumDay=lastDay.getDayOfMonth();
            //String calFunc=String.format("(%d*%d%d)/(1024*1024*%d)",av_speed_obj.getFinalAvailableSpeedBytes(),unit_price,data_3_available_day,lastDay.getDayOfMonth());
            long p=Average_spend_bytes*unit_price*available_day_sum* 1L;
            double c= ONE_THOUSAND * ONE_THOUSAND * thisMonthSumDay;
            double v=p/c;
            need_paid= Math.round(v);
            payJsonObj.put("Average_spend_bytes",Average_spend_bytes);
            payJsonObj.put("Average_spend_M_bytes",Average_spend_M_bytes);
            payJsonObj.put("unit_price",unit_price);
            payJsonObj.put("availableDay_day",available_day_sum);
            payJsonObj.put("SumMaxValue",SumMaxValue5);
            payJsonObj.put("d/m",available_day_sum+"/"+thisMonthSumDay);
            payJsonObj.put("p/c",p+"/"+c);
           // payJsonObj.put("cal_function",calFunc);
        }
        //创建一个后付费扣费订单--
        order=new TbOrderEntity();
        String new_serialNumber=System.currentTimeMillis()+"000"+StaticVariableUtils.createOrderIndex;
        order.setSerialNumber(new_serialNumber);
        order.setUserId(cdnSuitEntity.getUserId());
        order.setOrderType(OrderTypeEnum.ORDER_SYS_PAID.getTypeId());
        order.setPayJson(payJsonObj.toJSONString());
        order.setPayable(new Long( 0l-need_paid).intValue());
        order.setInitJson(orderInitJsonObject.toJSONString());
        order.setStatus(PayStatusEnum.PAY_SYS_PAID.getId());
        order.setCreateTime(new Date());
        tbOrderDao.insert(order);
        //余额扣款
        TbUserEntity user=userDao.selectById(cdnSuitEntity.getUserId());
        if(null==user){
            logger.error("【paid_2_TaskHandle】user "+cdnSuitEntity.getUserId()+" is null");
        }else {
            Integer balance=user.getPropertyBalance()-new Long(need_paid).intValue() ;
            user.setPropertyBalance(balance);
            userDao.updateById(user);
        }
    }

    //平均日峰带宽生成订单 handle
    private void paid2TaskHandle()  {
        //LocalDate date = LocalDate.of(2022,8,1);
        LocalDate date;
        date = LocalDate.now();
        LocalDate lastMonth; // 当前月份减1-->上月
        lastMonth = date.minusMonths(1);
        LocalDate firstDay = lastMonth.with(TemporalAdjusters.firstDayOfMonth()); // 获取当前月的上月的第一天
        LocalDate lastDay = lastMonth.with(TemporalAdjusters.lastDayOfMonth()); // 获取当前月的上月的最后一天

        String f_date_str=firstDay.format(DateTimeFormatter.ISO_DATE)+" 00:00:00";
        Date first_d=DateUtils.stringToDate(f_date_str,DateUtils.DATE_TIME_PATTERN);
        String l_date_str=lastDay.format(DateTimeFormatter.ISO_DATE)+" 23:59:59";
        Date last_d=DateUtils.stringToDate(l_date_str,DateUtils.DATE_TIME_PATTERN);

        Integer[] mode={2};
        List<CdnSuitEntity> suit_list=this.getFeeModeSuitList(mode);//   //根据MODE LIST 获取套餐列表
        for (CdnSuitEntity suitEntityBuffer:suit_list){
           this.createMode2OrderAndBalancePay(suitEntityBuffer,lastDay,first_d,last_d,"");
        }
    }

    //创建95计费带宽费用
    private void createMode3OrderAndBalancePay(CdnSuitEntity cdnSuitEntity, LocalDate lastDay, Date first_d, Date last_d, String order_tips_info){
        TbOrderEntity order=new TbOrderEntity();
        JSONObject orderInitJsonObject=new JSONObject();
        if (true){
            //获取源订单产品属性
            //从老订单中获取产品属性更新当前订单
            TbOrderEntity source_order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("user_id",cdnSuitEntity.getUserId())
                    .eq("order_type",OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())
                    .eq("serial_number",cdnSuitEntity.getSerialNumber())
                    .last("limit 1"));
            if (null!=source_order){
                order.setTargetId(source_order.getTargetId());
                if (StringUtils.isNotBlank(source_order.getInitJson())){
                    JSONObject init_json=DataTypeConversionUtil.string2Json(source_order.getInitJson());
                    if (null!=init_json &&  init_json.containsKey("product_obj")){
                        JSONObject product_obj=init_json.getJSONObject("product_obj");
                        orderInitJsonObject.put("product_obj",product_obj);
                        if (product_obj.containsKey("id")){
                            order.setTargetId(product_obj.getInteger("id"));
                        }
                    }
                }
            }
        }
        orderInitJsonObject.put("serial_number",cdnSuitEntity.getSerialNumber());
        String dateBuffer=lastDay.getYear()+"-"+lastDay.getMonthValue();
        orderInitJsonObject.put("date",dateBuffer);
        orderInitJsonObject.put("remark","["+dateBuffer+"]月95带宽系统月结扣费"+order_tips_info);
        orderInitJsonObject.put("name","pre_month_95");
        orderInitJsonObject.put("mode",3);
        if (true){
            //检测是否扣费
            String likeDate1=lastDay.getYear()+"-"+lastDay.getMonth();
            String likeDate2=lastDay.getYear()+"-"+lastDay.getMonthValue();
            String l1=String.format("\"date\":\"%s\"",likeDate1);
            String l2=String.format("\"date\":\"%s\"",likeDate2);
            String e1=String.format("\"serial_number\":\"%s\"",cdnSuitEntity.getSerialNumber());
            String e2=String.format("\"mode\":%d",3);
            TbOrderEntity exist_order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                    .eq("order_type",OrderTypeEnum.ORDER_SYS_PAID.getTypeId())
                    .eq("user_id",cdnSuitEntity.getUserId())
                    .and(q->q.like("init_json",l1).or().like("init_json",l2))
                    .like("init_json",e1)
                    .like("init_json",e2)
                    .last("limit 1"));
            if(null!=exist_order){
               //logger.debug("【paid_2_TaskHandle】order is exist ");
                return;
            }
        }
        //List<Integer>month_i_list=new ArrayList<>();
        SuitMonth95SpeedMode m95_obj= this.preMonth95Data(cdnSuitEntity.getSerialNumber(),first_d,last_d);
        int unit_price=this.getSuitUnitPriceFromSuitAttrStr(cdnSuitEntity.getAttrJson());
        if (0==unit_price){
            logger.error("price value is 0");
        }
        //<ul>
        //<li>1. 例如客户从1月1日正式开始计费，签订的合同价格为：P 元/Mbps/月。</li>
        //<li>2. 有效天：产生的消耗 ＞ 0，则记为有效天。</li>
        //<li>3. 假设客户1月份有14天有效天，则计费带宽为这14天有效天的所有统计点14 * 288个，去掉最高的5%的点，剩余统计点中最高的为 Max95，Max95
        // 即为计费带宽，1月的费用为：Max95 * P * 14 / 31。</li>
        //</ul>
        if ( null==m95_obj){
            logger.error("m95_obj is lost param");
            return;
        }
        long p95_bytes=m95_obj.getFinalM95SpeedBytesData();
        double p95_m_bytes= p95_bytes/(ONE_THOUSAND*ONE_THOUSAND);
        int availableDaySum=m95_obj.getMonthAvailableDaySum();
        int thisMonthDaySum=lastDay.getDayOfMonth();
        //String calFunc=String.format("(%d*%d*%d)/(1024*1024*%d)",m95_obj.getFinalM95SpeedBytesData(),unit_price,availableDaySum,lastDay.getDayOfMonth());
        long p=p95_bytes*unit_price*availableDaySum*1L;
        double c=ONE_THOUSAND*ONE_THOUSAND*thisMonthDaySum;
        double v=p/c;
        Integer need_paid=new Long( Math.round(v)).intValue();
        JSONObject payJsonObj =new JSONObject();
        payJsonObj.put("p95_bytes",p95_bytes);
        payJsonObj.put("p95_m_bytes",p95_m_bytes);
        payJsonObj.put("unit_price",unit_price);
        payJsonObj.put("need_paid",need_paid);
        payJsonObj.put("d/m",availableDaySum+"/"+thisMonthDaySum);
        payJsonObj.put("p/c",p +"/"+c);
        //payJsonObj.put("cal_function",calFunc);
        //logger.info("===>"+need_paid);
        //创建一个后付费扣费订单--

        String new_serialNumber=System.currentTimeMillis()+"000"+StaticVariableUtils.createOrderIndex;
        order.setSerialNumber(new_serialNumber);
        order.setUserId(cdnSuitEntity.getUserId());
        order.setOrderType(OrderTypeEnum.ORDER_SYS_PAID.getTypeId());
        order.setPayable(0-need_paid );
        order.setInitJson(orderInitJsonObject.toJSONString());
        order.setPayJson(payJsonObj.toJSONString());
        order.setStatus(PayStatusEnum.PAY_SYS_PAID.getId());
        order.setCreateTime(new Date());
        tbOrderDao.insert(order);
        //余额扣款
        TbUserEntity user=userDao.selectById(cdnSuitEntity.getUserId());
        if(null==user){
            logger.error("user "+cdnSuitEntity.getUserId()+" is null");
            return;
        }
        Integer balance=user.getPropertyBalance()-need_paid;
        user.setPropertyBalance(balance);
        userDao.updateById(user);
    }

    //月95 带宽生成订单 handle
    private void paid3TaskHandle(){
        //LocalDate date = LocalDate.of(2022,8,1);
        LocalDate date = LocalDate.now();
        // 当前月份减1
        LocalDate lastMonth = date.minusMonths(1);
        // 获取当前月的第一天
        LocalDate firstDay = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
        // 获取当前月的最后一天
        LocalDate lastDay = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
        String f_date_str=firstDay.format(DateTimeFormatter.ISO_DATE)+" 00:00:00";
        Date first_d=DateUtils.stringToDate(f_date_str,DateUtils.DATE_TIME_PATTERN);
        String l_date_str=lastDay.format(DateTimeFormatter.ISO_DATE)+" 23:59:59";
        Date last_d=DateUtils.stringToDate(l_date_str,DateUtils.DATE_TIME_PATTERN);

        Integer[] mode={3};
        //根据MODE LIST 获取套餐列表
        List<CdnSuitEntity> suit_list=this.getFeeModeSuitList(mode);
        for (CdnSuitEntity itemSuitEntity:suit_list){
              this.createMode3OrderAndBalancePay(itemSuitEntity,lastDay,first_d,last_d,"");
        }
    }


    private String getProductNameByPaySerialNumber(String paySerialNumber){
        TbOrderEntity sourceOrder=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",paySerialNumber).last("limit 1"));
        if (null==sourceOrder){
            return null;
        }
        //{"price_obj":"{\"value\":100,\"status\":1}",
        // "buy_obj":{"serialNumber":"1663913563415002","sum":1,"startTime":1664248074364,"type":"m"},"
        // product_obj":"{\"createtime\":1659499295000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":100,\\\"status\\\":1}}\",\"name\":\"流量月包\",\"serverGroupIds\":\"\",\"attrJson\":\"[{\\\"attr\\\":\\\"flow\\\",\\\"id\\\":31,\\\"name\\\":\\\"流量\\\",\\\"valueType\\\":\\\"int\\\",\\\"unit\\\":\\\"G\\\",\\\"value\\\":200000}]\",\"weight\":1,\"id\":18,\"productType\":12,\"status\":1}"}
        String initJsonStr=sourceOrder.getInitJson();
        JSONObject initJson=DataTypeConversionUtil.string2Json(initJsonStr);
        if (!initJson.containsKey("product_obj")){
            return null;
        }
        CdnProductEntity product=DataTypeConversionUtil.json2entity(initJson.getJSONObject("product_obj"), CdnProductEntity.class);
        return  product.getName();
    }



    //套餐量与使用量详情
    @Override
    public CdnSuitEntity   updateThisSuitDetailInfo(CdnSuitEntity suit,boolean usedInfo){
        if(false){
            //更正 将中文属性更新为英文属性
            boolean isNeedUpdate=false;
            JSONObject new_en_attrJson=new JSONObject();
            JSONObject curAttrJson=DataTypeConversionUtil.string2Json(suit.getAttrJson());
            for(String key : curAttrJson.keySet()){
                ProductAttrNameEnum attrEnum=ProductAttrNameEnum.getEnum(key);
                if(null==attrEnum){
                    continue;
                }
                new_en_attrJson.put(attrEnum.getAttr(),curAttrJson.getString(key));
                if(key.equals(attrEnum.getName())){
                    isNeedUpdate=true;
                }
            }
            if(isNeedUpdate){
                suit.setAttrJson(new_en_attrJson.toJSONString());
                cdnSuitDao.updateById(suit);
            }
        }

        //1更新结束时间
        Integer[] sInList={OrderTypeEnum.ORDER_CDN_SUIT.getTypeId(),OrderTypeEnum.ORDER_CDN_RENEW.getTypeId()};
        if (Arrays.asList(sInList).contains(suit.getSuitType())){
            Integer[] sInStatusList={CdnSuitStatusEnum.NORMAL.getId()};
            CdnSuitEntity end_suit=cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                    .eq("serial_number",suit.getSerialNumber())
                    .in("suit_type",sInList)
                    .in("status", sInStatusList)
                    .orderByDesc("end_time")
                    .last("limit 1")
            );
            if(null!=end_suit){
                suit.setEndTime(end_suit.getEndTime());
            }
        }

        // 2 获取增值业务 更新 所有属性 ATTR
        if (Arrays.asList(sInList).contains(suit.getSuitType())){
            JSONObject finalAttrJson=DataTypeConversionUtil.string2Json(suit.getAttrJson());
            Integer[] addStatusArray={CdnSuitStatusEnum.NORMAL.getId(),CdnSuitStatusEnum.USED_RUN_OUT.getId()};
            List<CdnSuitEntity> list=cdnSuitDao.selectList(new QueryWrapper<CdnSuitEntity>()
                    .eq("serial_number",suit.getSerialNumber())
                    .eq("suit_type",OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())
                    .le("start_time",new Date())
                    .ge("end_time",new Date())
                    .in("status",addStatusArray));
            suit.setAddedServices(list);
            list.forEach(item->{
                //ProductAttrVo productAttrVo=new ProductAttrVo();
                item.setSuitName(getProductNameByPaySerialNumber(item.getPaySerialNumber()));
                JSONObject sAttrJson=DataTypeConversionUtil.string2Json(item.getAttrJson());
                for(String key : sAttrJson.keySet()){
                    ProductAttrNameEnum attrEnum=ProductAttrNameEnum.getEnum(key);
                    if(null==attrEnum){
                        continue;
                    }
                    if(1 ==attrEnum.getCanAddMode() ){
                        //int
                        Integer v=sAttrJson.getInteger(key);
                        if(finalAttrJson.containsKey(key)){
                            Integer new_v=finalAttrJson.getInteger(key)+v;
                            finalAttrJson.put(key,new_v);
                        }else{
                            finalAttrJson.put(key,v);
                        }
                    }else if(2==attrEnum.getCanAddMode()){
                        //bool
                        Integer v=sAttrJson.getInteger(key);
                        if(finalAttrJson.containsKey(key)){
                            Integer new_v=(finalAttrJson.getInteger(key)+v)>0?1:0;
                            finalAttrJson.put(key,new_v);
                        }else{
                            finalAttrJson.put(key,v);
                        }
                    }else if(3==attrEnum.getCanAddMode()){
                        //TXT
                        String v=sAttrJson.getString(key);
                        if(finalAttrJson.containsKey(key)){
                            String new_v=finalAttrJson.getString(key);
                            finalAttrJson.put(key,new_v);
                        }else{
                            finalAttrJson.put(key,v);
                        }
                    }else if(0==attrEnum.getCanAddMode()){
                        //其它类型
                        String v=sAttrJson.getString(key);
                        if(finalAttrJson.containsKey(key)){
                            String new_v=finalAttrJson.getString(key);
                            finalAttrJson.put(key,new_v);
                        }else{
                            finalAttrJson.put(key,v);
                        }
                    }else {
                        logger.error(key +" type is unknown! ");
                    }
                }
            });
            JSONObject attrJson=ProductAttrNameEnum.getAllAttrMaxValueJson(finalAttrJson);
            //suit.setAttr(attrJson);
            suit.setAttr(DataTypeConversionUtil.json2entity(attrJson,ProductAttrVo.class)  );
        }else if(suit.getSuitType().equals(OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())){
            //如果增值业务
            JSONObject sAttrJson=DataTypeConversionUtil.string2Json(suit.getAttrJson());
            JSONObject attrJson=ProductAttrNameEnum.getAllAttrMaxValueJson(sAttrJson);
            //suit.setAttr(attrJson);
            suit.setAttr( DataTypeConversionUtil.json2entity(attrJson,ProductAttrVo.class));
        }else{
            logger.error("[updateThisSuitDetailInfo]getSuitType unknown");
        }


        //3 套餐已用量
        if (usedInfo && Arrays.asList(sInList).contains(suit.getSuitType())){
          //  suit.setConsume();
            this.updateAlreadyConsumeInfo(suit);
        }

        //4 套餐名 相关信息
        String pay_orderNumber="";
        if (suit.getSuitType().equals(OrderTypeEnum.ORDER_CDN_SUIT.getTypeId())){
            //新购套餐
            pay_orderNumber=suit.getPaySerialNumber();
        }
        else if (suit.getSuitType().equals(OrderTypeEnum.ORDER_CDN_RENEW.getTypeId())){
            //cdn 续费套餐
            pay_orderNumber=suit.getSerialNumber();
        }else if (suit.getSuitType().equals(OrderTypeEnum.ORDER_CDN_ADDED.getTypeId())){
            //cdn 增值业务
            pay_orderNumber=suit.getPaySerialNumber();
        }else {
            logger.error("[updateThisSuitDetailInfo]unknown pay_orderNumber");
        }
        List<CdnClientGroupEntity> groupList=new ArrayList<>();
        TbOrderEntity order=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",pay_orderNumber).last("limit 1"));
        if(null!=order){
            JSONObject OrderInitJson=DataTypeConversionUtil.string2Json(order.getInitJson());
            if( OrderInitJson.containsKey("product_obj")){
                JSONObject product_obj_json=OrderInitJson.getJSONObject("product_obj");
                CdnProductEntity product=DataTypeConversionUtil.json2entity(product_obj_json, CdnProductEntity.class);
                if (StringUtils.isNotBlank(product.getServerGroupIds())){
                    String[] ids=product.getServerGroupIds().split(",");
                    for (String id:ids){
                        CdnClientGroupEntity group=cdnClientGroupDao.selectById(id);
                        if (null!=group){
                            if (null!=group && null!=group.getAreaId()){
                                if ( null==group.getAreaId() || 0==group.getAreaId() ){
                                    group.setAreaName("默认");
                                }else{
                                    CdnClientAreaEntity areaEntity=cdnClientAreaDao.selectById(group.getAreaId());
                                    if (null!=areaEntity){
                                        group.setAreaName(areaEntity.getName());
                                    }
                                }
                            }
                            groupList.add(group);
                        }
                    }
                    product.setClient_group_list(groupList);
                }
                String SerialNumberHash= HashUtils.getCRC32(suit.getSerialNumber());
                if (null!=product){
                    //product.setName(String.format("[%s]%s",SerialNumberHash,product.getName()));
                    TbOrderEntity order_main=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>().eq("serial_number",suit.getSerialNumber()).last("limit 1"));
                    if (null!=order_main){
                        product.setName(String.format("[%d]%s",order_main.getId(),product.getName()));
                        suit.setSuitName(product.getName());
                    }
                    suit.setProductEntity(product);
                    suit.setProduct(product);
                }else {
                    product=new CdnProductEntity();
                    product.setName("["+SerialNumberHash+"]");
                    suit.setProductEntity(product);
                    suit.setProduct(product);
                }

            }
        }

        //5  cname 与 hashList
        if (Arrays.asList(sInList).contains(suit.getSuitType())){
            List<String >t_came=new ArrayList<>();
            List<String >t_hash=new ArrayList<>();
            for (CdnClientGroupEntity cg:groupList){
                String  hash=cg.getHash();
                t_hash.add(hash);
                TbDnsConfigEntity dnsConfigEntity=tbDnsConfigDao.selectById(cg.getDnsConfigId());
                if (null!=dnsConfigEntity){
                    t_came.add(String.format("*.%s.%s",hash,dnsConfigEntity.getAppDomain()));
                }
                if (!t_hash.contains(hash)){
                    t_hash.add(hash);
                }
            }
            suit.setCname(String.join(",",t_came));
            suit.setHashList(String.join(",",t_hash));
        }
        return  suit;
    }



    /**
     * DNS 记录调度
     */
    @Override
    public void groupDnsRecordDispatch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    String varDnsDispatchMode="online";
                    String varModeTimingFrequency="300";
                    SysConfigEntity config=sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().eq("param_key", ConfigConstantEnum.CDN_NODECHECK_KEY.getConfKey()).last("limit 1"));
                    if(null!=config){
                        NodeCheckConfig nodeCheckConfig= DataTypeConversionUtil.string2Entity(config.getParamValue(), NodeCheckConfig.class);
                        if (null!=nodeCheckConfig){
                            varDnsDispatchMode=nodeCheckConfig.getDnsDispatchMode();
                            varModeTimingFrequency=nodeCheckConfig.getModeTimingFrequency();
                        }
                    }
                    if("online".equals(varDnsDispatchMode)){
                        dispatchHandleOnlineHandle();
                    }else if("timing".equals(varDnsDispatchMode)){
                        dispatchHandleTiming(varModeTimingFrequency);
                    }else if ("random".equals(varDnsDispatchMode)){
                        //随机切换
                        dispatchHandleRandomHandle();
                    }else{
                        logger.error("unknown mode ");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    StaticVariableUtils.dispatchThread=false;
                }

            }
        }).start();
    }

    /**
     * 检测节点状态【停用】
     */
    @Override
    public void checkNodeNormalHandle() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    StaticVariableUtils.checkNodeThread=true;
                    List<CdnClientEntity>list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>());
                    for (CdnClientEntity client:list){
                        if (StringUtils.isNotBlank(client.getClientIp())){
                            // TODO: 2022/7/7
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    StaticVariableUtils.checkNodeThread=false;
                }

            }
        }).start();

    }


    /**
     *  记录流量
     */
    @Override
    public void requestBytesRecordHandle() {
        try{
            requestBytesRecordHandleThread();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            StaticVariableUtils.bytesThread=false;
        }

    }


    /**
     * 生成 计费 任务| 关停 到期套餐 | 关闭超出游量套餐相关服务
     */
    @Override
    public void prePaidTask() {
        StaticVariableUtils.pre_paidThread=true;
        try{
           //logger.debug("[pre_paidTask]start");
            this.paid1TaskHandle();//检测前付费套餐流量|防御等属性超出情况 handle
            this.paid2TaskHandle();//平均日峰带宽生成订单 handle【----后付费----】
            this.paid3TaskHandle();// 月95 带宽生成订单 handle【----后付费----】
           //logger.debug("[pre_paidTask]end");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            StaticVariableUtils.pre_paidThread=false;
            StaticVariableUtils.pre_paidTimeTemp=System.currentTimeMillis();
        }
    }


    private void recordSslJobTime(Integer siteId){
        //"{\"pvalue1\":\"check_time\",\"pvalue2\":\"crt_end_time\"}"
        tbSiteAttrDao.update(null,new UpdateWrapper<TbSiteAttrEntity>()
                .eq("pkey",SiteAttrEnum.JOB_SSL_APPLY.getName())
                .eq("site_id",siteId)
                .set("pvalue1",System.currentTimeMillis())
        );
    }

    /**
     * acme 申请 证书
     *
     */
    @Override
    public void applySslTaskHandle() {
        //继续执行申请证书
        cdnMakeFileService.applyCertThread();

        //"{\"pvalue1\":\"check_time\",\"pvalue2\":\"crt_end_time\"}"
        //获取所有开启自动申请证书的attr
        TbSiteAttrEntity siteApplyAttr=tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey",SiteAttrEnum.JOB_SSL_APPLY.getName())
                .eq("pvalue","1")
                .orderByAsc("pvalue1")
                .eq("status",1)
                .last("limit 1")
        );
        if (null==siteApplyAttr){
            logger.error("applySslTaskHandle,获取配置为空，next");
            return;
        }
        recordSslJobTime(siteApplyAttr.getSiteId());
        //无此站 | 站点暂停->下一轮
        TbSiteEntity site=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                .eq("id",siteApplyAttr.getSiteId())
                .ne("status",0)
                .select("id,user_id,serial_number,main_server_name")
        );
        if(null==site){
            logger.info( siteApplyAttr.getSiteId()+",site is close or not found，next");
            return;
        }
        int siteId=site.getId();
        //取出站点--ssl
        TbSiteMutAttrEntity certMutAttr=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                .eq("status",1)
                .eq("site_id",siteId)
                .orderByDesc("id")
                .last("limit 1")
        );
        if (null==certMutAttr){
            //未配置证书
            // 1查询是否有证书文件
            R r=AcmeShUtils.getApplyCertInfos(site.getId(),"");
            if (1==r.getCode()){
                TbCertifyObjVo vo=DataTypeConversionUtil.map2entity(r,TbCertifyObjVo.class);
                if (null!=vo){
                    if (0!=vo.getNotAfter() && DateUtils.addDateMonths(DateUtils.LongStamp2Date(vo.getNotAfter()),-1).after(new Date())){
                        logger.info("find crt by acme ,"+site.getMainServerName()+",The certificate is valid for more than one month and cannot be re signed");
                        viewLocalCertFileUpdateSslCrt(site.getId());
                        return;
                    }
                }
            }
            //String key_path=String.format("/root/.acme.sh/ants/%d.key",siteAttr.getSiteId());
            //String key= FileUtils.getStringByPath(key_path).trim();
            // 2查询列表有可用
        }
        else{
            //已配置证书
            //到期时间
            if (  0l!= certMutAttr.getPvalue1()){
                Date nfDate=DateUtils.LongStamp2Date(certMutAttr.getPvalue1());
                if (DateUtils.addDateMonths(nfDate,-1).after(new Date())){
                    String msg=String.format("site:%s,pv1:%d,CTM:%d (If the expiration date is greater than 1 month, cancel the current site application task [1])",site.getMainServerName(),certMutAttr.getPvalue1(),System.currentTimeMillis());
                    logger.info(msg);
                    return;
                }
            }
            //解析证书--
            JSONObject pemObj=DataTypeConversionUtil.string2Json(certMutAttr.getPvalue());
            if (pemObj.containsKey("value")){
                Map crtRes=HashUtils.readCerSubjectToMap(pemObj.getString("value"));
                if (crtRes.containsKey("NotAfter")){
                    //Wed Jun 07 07:59:59 CST 2023
                    Date aDate= (Date)crtRes.get("NotAfter");
                    Long afterTime=aDate.getTime();
                    //DateUtils.stringToDate(crtRes.get("NotAfter").toString(),DateUtils.DATE_TIME_PATTERN).getTime();
                    certMutAttr.setPvalue1(afterTime);
                    tbSiteMutAttrDao.updateById(certMutAttr);
                    if (null!=afterTime && DateUtils.addDateMonths(DateUtils.LongStamp2Date(afterTime),-1).after(new Date())){
                        //证书有效期大于1个月，不重签
                        logger.info( "["+site.getId()+"]"+site.getMainServerName()+",If the expiration date is greater than 1 month, cancel the current site application task [1] ");
                        return;
                    }
                }
            }
        }
        logger.info(String.format("--start acme_job ==>id:%d,servername:%s",siteId,site.getMainServerName()) );
        //获取cert
        TbCertifyEntity srcEntity=tbcertifyDao.selectOne(new QueryWrapper<TbCertifyEntity>() .eq("site_id",siteId) .orderByDesc("id").last("limit 1"));
        if (null==srcEntity ||  -1==srcEntity.getSrcType()){
            R r1=cdnMakeFileService.pushApplyCertificateBySiteId(site.getId(),0,-1,0,0);
            logger.info("ssl_task_apply_result[-1]: " + r1.toJsonString());
        }else{
            if (!CertSrcTypeEnums.getIsNeedVerifyDnsByType(srcEntity.getSrcType())){
                R r1=cdnMakeFileService.pushApplyCertificateBySiteId(site.getId(),0,srcEntity.getSrcType(),0,0);
                logger.info("ssl_task_apply_result[0]: " + r1.toJsonString());
            }else {
                int dnsconfigid=0;
                JSONObject jsonObject=DataTypeConversionUtil.string2Json(srcEntity.getApplyInfo());
                if (jsonObject.containsKey("dnsconfigid")){
                    dnsconfigid=jsonObject.getInteger("dnsconfigid");
                }else if (jsonObject.containsKey("dnsConfigId")){
                    dnsconfigid=jsonObject.getInteger("dnsConfigId");
                }
                R r1=cdnMakeFileService.pushApplyCertificateBySiteId(site.getId(),0,srcEntity.getSrcType(),0,dnsconfigid);
                logger.info("ssl_task_apply_result[1]: " + r1.toJsonString());
            }
        }
    }





    /**
     * 证书回调
     * @param siteId
     */
    @Override
    public void viewLocalCertFileUpdateSslCrt(int siteId) {
        logger.info("auto_acme_job_callback-siteId="+siteId);
        String pem_path=String.format("%s/ants/%d.crt",AcmeShUtils.getAcmeRootDir(),siteId) ;
        String key_path=String.format("%s/ants/%d.key",AcmeShUtils.getAcmeRootDir(),siteId);
        //{"siteId":"3126","other_ssl_pem":[{"value":""}],"other_ssl_key":[{"value":"------"}]}
        String pem=FileUtils.getStringByPath(pem_path);
        String key= FileUtils.getStringByPath(key_path);
        if (StringUtils.isBlank(pem) || StringUtils.isBlank(key)){
            logger.error("auto_apply_cert_found is empty");
            return;
        }
        CertCallbackForm cForm=new CertCallbackForm();
        cForm.setType(0);
        cForm.setCode(1);
        cForm.setPem(pem);
        cForm.setKey(key);
        R r1= this.removeDnsRecordByApplyCertSuccess(siteId,0);
        logger.info("remove_dns_record:"+r1.toJsonString());
        R r2=cdnMakeFileService.saveCert2Db(cForm);
        logger.info("save_Cert_2_Db:"+r2.toJsonString());
    }

    /**
     * 获取 流量使用详情数据
     * @param userId
     * @param SerialNumber
     * @param startTime
     * @param endTime
     * @return
     */
    @Override
    public BytesDateVo getBytesDataListBySuitSerialNumber(Long userId, String SerialNumber, Date startTime, Date endTime) {
        //Map result=new HashMap();
        logger.info("##### getBytesDataListBySuitSerialNumber ######");
        BytesDateVo bytesDateVo=new BytesDateVo();
        TbOrderEntity orderEntity=tbOrderDao.selectOne(new QueryWrapper<TbOrderEntity>()
                .eq(null!=userId,"user_id",userId)
                .eq("serial_number",SerialNumber)
                .last("limit 1"));
        if(null==orderEntity){
            bytesDateVo.setErrMsg("SerialNumber:"+SerialNumber+"订单不存在：");
            logger.error("SerialNumber:"+SerialNumber+"订单不存在：");
            return bytesDateVo;
        }
        String initJsonStr=orderEntity.getInitJson();
        JSONObject initJsonObj=DataTypeConversionUtil.string2Json(initJsonStr);
        if (!initJsonObj.containsKey("product_obj")){
            bytesDateVo.setErrMsg("订单不无产品属性");
            logger.error("订单不无产品属性");
            return bytesDateVo;
        }
        JSONObject productObj=initJsonObj.getJSONObject("product_obj");
        CdnProductEntity product=DataTypeConversionUtil.json2entity(productObj, CdnProductEntity.class);
        if(null==product){
            bytesDateVo.setErrMsg("订单产品属性转换错误");
            logger.error("订单产品属性转换错误");
            return bytesDateVo;
        }
        //[{"attr":"charging_mode","name":"charging_mode","value":1,"valueType":"select"},{"attr":"flow","name":"flow","valueType":"int","unit":"G","value":220000},{"attr":"bandwidth","name":"bandwidth","valueType":"price_int","unit":"元/Mbps/月","value":2000},{"attr":"ai_waf","id":27,"unit":"","superpositionMode":2,"valueType":"bool","name":"AI WAF","value":"1"},{"attr":"defense","id":38,"unit":"QPS","superpositionMode":1,"valueType":"int","name":"CC防御","value":100,"hiddenUsed":true},{"attr":"public_waf","id":37,"unit":"","superpositionMode":2,"valueType":"bool","name":"公共WAF","value":"1"},{"attr":"port_forwarding","id":36,"unit":"个","superpositionMode":1,"valueType":"int","name":"端口转发","value":8},{"attr":"site","id":35,"unit":"个","superpositionMode":1,"valueType":"int","name":"站点","value":12},{"attr":"sms","id":34,"unit":"条","superpositionMode":1,"valueType":"int","name":"短信通知","value":102,"hiddenUsed":true},{"attr":"monitor","id":33,"unit":"","superpositionMode":2,"valueType":"bool","name":"流量监控","value":"1"},{"attr":"private_waf","id":32,"unit":"","superpositionMode":2,"valueType":"bool","name":"专属WAF","value":"1"},{"attr":"live_data","id":30,"unit":"","superpositionMode":2,"valueType":"bool","name":"实时数据","value":"1"},{"attr":"dd_defense","id":40,"unit":"GB","superpositionMode":1,"valueType":"int","name":"DDos防御","value":100}]
        int feeMode=this.getSuitModeFromSuitAttrStr(ProductAttrNameEnum.getFinalAttrJsonByAttrJsonArrayStr(product.getAttrJson()).toJSONString() );
        int unit_price=this.getSuitUnitPriceFromSuitAttrStr(ProductAttrNameEnum.getFinalAttrJsonByAttrJsonArrayStr(product.getAttrJson()).toJSONString());
        switch (feeMode){
            case 1:
                //result.put("mode",feeMode);
                bytesDateVo.setMode(feeMode);
                //result.put("start",startTime);
                bytesDateVo.setStart(startTime);
                //result.put("end",endTime);
                bytesDateVo.setEnd(endTime);
                SuitFlowMode m1Obj=this.preBytesData(SerialNumber,startTime,endTime);
                //result.put("data",m1Obj.getFlowData());
                bytesDateVo.setData(m1Obj.getFlowData());
                //result.put("data_total",m1Obj.getTotalFlow());
                bytesDateVo.setData_total(m1Obj.getTotalFlow());
                //result.put("data_top",m1Obj.getTopFlow());
                bytesDateVo.setData_top(m1Obj.getTopFlow());
                break;
            case 2:
                if (true){
                    //result.put("mode",feeMode);
                    bytesDateVo.setMode(feeMode);
                    //result.put("start",startTime);
                    bytesDateVo.setStart(startTime);
                    //result.put("end",endTime);
                    bytesDateVo.setEnd(endTime);
                    SuitAverageMaxSpeedMode av_speed_obj=this.preMonthDayData(SerialNumber,startTime,endTime);
                    //result.put("data_1", p_m_data_map.get("data_1"));
                    long av_value_bytes= av_speed_obj.getFinalAvailableSpeedBytes();
                    int  available_day=av_speed_obj.getMonthAvailableDaySum();
                    //result.put("data", av_speed_obj.getNear7DayAllSpeedData());
                    bytesDateVo.setData(av_speed_obj.getNear7DayAllSpeedData());
                    //result.put("data_3_available_day", available_day);
                    bytesDateVo.setData_3_available_day(available_day);
                    //result.put("data_4_average_max_value_bytes",av_value_bytes );
                    bytesDateVo.setData_4_average_max_value_bytes(av_value_bytes);
                    //result.put("data_value",av_value_bytes);
                    bytesDateVo.setData_value(av_value_bytes);
                    //result.put("data_unit_price",unit_price);
                    bytesDateVo.setData_unit_price(unit_price);
                    //result.put("data_total",av_speed_obj.getTotalFlow());
                    bytesDateVo.setData_total(av_speed_obj.getTotalFlow());
                    //result.put("data_top_speed",av_speed_obj.getTopSpeed());
                    bytesDateVo.setData_top_speed(av_speed_obj.getTopSpeed());
                    int ThisMonthDaySum=DateUtils.getDateLastDayNum(endTime);
                    //result.put("data_month_last_day",ThisMonthDaySum);
                    bytesDateVo.setData_month_last_day(ThisMonthDaySum);
                    long p=av_value_bytes*available_day*unit_price*1L;
                    double c=ONE_THOUSAND*ONE_THOUSAND*ThisMonthDaySum;
                    double v=p/c;
                    //result.put("cur_paid_fee",v);
                    bytesDateVo.setCur_paid_fee(Math.round(v));
                    //String calFunc=String.format("(%d*%d*%d)/(1024*1024*%d)",av_speed_obj.getFinalAvailableSpeedBytes(),unit_price,av_speed_obj.getMonthAvailableDaySum(),ThisMonthDaySum);
                    //result.put("cal",calFunc);
                    //mode2_need_pay=data_4_average_max_value_bytes/(1024*1024)*data_unit_price*data_3_available_day/data_month_last_day
                }
                break;
            case 3:
                if (true){
                    //result.put("mode",feeMode);
                    bytesDateVo.setMode(feeMode);
                    //result.put("start",startTime);
                    bytesDateVo.setStart(startTime);
                    //result.put("end",endTime);
                    bytesDateVo.setEnd(endTime);
                    SuitMonth95SpeedMode m95_ob=this.preMonth95Data(SerialNumber,startTime,endTime);
                    //result.put("data_1", pm95_data_map.get("data_1"));
                    long m95_value_bytes=m95_ob.getFinalM95SpeedBytesData();
                    int  availableDay=m95_ob.getMonthAvailableDaySum();
                    //result.put("data",m95_ob.getNear7DayAllSpeedData());
                    bytesDateVo.setData(m95_ob.getNear7DayAllSpeedData());
                    //result.put("data_3_available_day", availableDay);
                    bytesDateVo.setData_3_available_day(availableDay);
                    //result.put("data_4_95value_bytes",m95_value_bytes);
                    bytesDateVo.setData_4_95value_bytes(m95_value_bytes);
                    //result.put("data_value",m95_value_bytes);
                    bytesDateVo.setData_value(m95_value_bytes);
                    //result.put("data_5_index", m95_ob.getM95SpeedIndex());
                    bytesDateVo.setData_5_index(m95_ob.getM95SpeedIndex());
                    //result.put("data_unit_price",unit_price);
                    bytesDateVo.setData_unit_price(unit_price);
                    //result.put("data_total",m95_ob.getTotalFlow());
                    bytesDateVo.setData_total(m95_ob.getTotalFlow());
                    //result.put("data_top_speed",m95_ob.getMaxM95SpeedBytesData());
                    bytesDateVo.setData_top_speed(m95_ob.getMaxM95SpeedBytesData());
                    int ThisMonthDaySum=DateUtils.getDateLastDayNum(endTime);
                    //result.put("data_month_last_day",ThisMonthDaySum);
                    bytesDateVo.setData_month_last_day(ThisMonthDaySum);
                    long  p=m95_value_bytes*availableDay*unit_price*1L;
                    double  c=ONE_THOUSAND*ONE_THOUSAND*ThisMonthDaySum;
                    double v=p/c;
                    //result.put("cur_paid_fee",p/c);
                    bytesDateVo.setCur_paid_fee(new Double(v).floatValue());
                    //String calFunc=String.format("(%d*%d*%d)/(1024*1024*%d)",m95_ob.getFinalM95SpeedBytesData(),unit_price,m95_ob.getMonthAvailableDaySum(),ThisMonthDaySum);
                    //result.put("cal",calFunc);
                    //mode3_need_pay=data_4_95value_bytes/(1024*1024)*data_unit_price*data_3_available_day/data_month_last_day
                }
                break;
            default:
                bytesDateVo.setErrMsg("["+feeMode+"]unknown type");
                logger.error("[get-Bytes-Data-List-BySuit-SerialNumber]unknown type "+feeMode);
                break;
        }
        logger.info("bytesDateVo: {}", bytesDateVo);
        return bytesDateVo;
    }
}
