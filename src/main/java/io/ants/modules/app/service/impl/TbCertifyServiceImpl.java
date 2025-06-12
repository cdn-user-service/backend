package io.ants.modules.app.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.app.form.QueryCertPageForm;
import io.ants.modules.app.service.TbCertifyService;
import io.ants.modules.app.vo.ZeroSslAPiCreateCertForm;
import io.ants.modules.app.vo.ZeroSslApiCertInfoVo;
import io.ants.modules.sys.enums.CertSrcTypeEnums;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.enums.SiteAttrEnum;
import io.ants.modules.sys.enums.TbCertifyStatusEnum;
import io.ants.modules.sys.form.CertCallbackForm;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.TbSiteServer;
import io.ants.modules.sys.vo.*;
import io.ants.modules.utils.config.ZeroSslConfig;
import io.ants.modules.utils.factory.ZeroSslFactory;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("TbCertifyService")
public class TbCertifyServiceImpl extends ServiceImpl<TbCertifyDao, TbCertifyEntity> implements TbCertifyService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private TbCertifyDao certifyDao;
    @Autowired
    private TbSiteServer tbSiteServer;

    @Override
    public PageUtils certPageList(QueryCertPageForm form) {

        QueryWrapper qw=new QueryWrapper<TbCertifyEntity>();
        qw.in(null!=form.getUids(),"user_id",form.getUids());
        qw.eq(null!=form.getStatus(),"status",form.getStatus());
        if (StringUtils.isNotBlank(form.getKey())){
            qw.like("common_name",form.getKey());
        }

        //not_after
        if (null!= form.getNotAfters() && !form.getNotAfters().isEmpty() && 2==form.getNotAfters().size()){
            qw.between("not_after",form.getNotAfters().get(0),form.getNotAfters().get(1));
        }

        if (StringUtils.isNotBlank(form.getOrderBy())){
            //id not_after
            String[] orderInfos=form.getOrderBy().split(",");
            if (2==orderInfos.length){
                if ("asc".equals(orderInfos[1])){
                    qw.orderByAsc(orderInfos[0]);
                }else if ("desc".equals(orderInfos[1])){
                    qw.orderByDesc(orderInfos[0]);
                }
            }
        }else {
            qw.orderByDesc("id");
        }


        IPage<TbCertifyEntity> iPage=this.page(
                new Page<>(form.getPage(),form.getLimit()),
                qw
        );
        iPage.getRecords().forEach(item->{
             updateCertByAcmeSh(item);
             updateExcessCert(item);
        });

        return new PageUtils(iPage);
    }


    private void updateExcessCert(TbCertifyEntity certify){
        if (null==certify || 0l==certify.getNotAfter() || certify.getStatus()!=TbCertifyStatusEnum.SUCCESS.getId()){
            return;
        }
        List<TbCertifyEntity>  excessCEList=certifyDao.selectList(new QueryWrapper<TbCertifyEntity>()
                .and(q->q.eq("site_id",certify.getSiteId()).or().eq("common_name",certify.getCommonName()))
                .eq("src_type",certify.getSrcType())
                .eq("status",TbCertifyStatusEnum.SUCCESS.getId())
                .orderByDesc("not_after")
                .select("id")
        );
        for (int i = 0; i < excessCEList.size(); i++) {
            if (i>0){
                TbCertifyEntity item=excessCEList.get(i);
                certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>().set("status",TbCertifyStatusEnum.EXCESS_CERT.getId()).eq("id",item.getId()));
            }
        }

    }

    private String getFinalStr(String str){
        if (StringUtils.isBlank(str)){
            return "";
        }
        String regFileName = "-----[\\W\\D\\S]*-----";
        // 匹配当前正则表达式
        Matcher matcher = Pattern.compile(regFileName).matcher(str);
        if (matcher.find()) {
            // 将匹配当前正则表达式的字符串即文件名称进行赋值
            return matcher.group();
        }
        return "";

    }

    @Override
    public TbCertifyEntity updateCertByAcmeSh(TbCertifyEntity certify){
        if (null==certify){
            return null;
        }

        //更新userInfo
        if (null==certify.getUserId()){
            TbSiteEntity siteEntity=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("main_server_name",certify.getCommonName()).last("limit 1"));
            if(null!=siteEntity){
                certify.setUserId(siteEntity.getUserId());
                this.update(null,new UpdateWrapper<TbCertifyEntity>()
                        .eq("id",certify.getId())
                        .set("user_id",certify.getUserId())
                );
            }
        }
        if(null!=certify.getUserId()){
            TbUserEntity user=tbUserDao.selectOne(new QueryWrapper<TbUserEntity>().eq("user_id",certify.getUserId()).select("username,mobile,mail"));
            certify.setUser(user);
        }
        certify.setCanReApply(true);

        if (certify.getStatus()==TbCertifyStatusEnum.EXCESS_CERT.getId()){
            certify.setAcmeLog("");
            certify.setApplyLog("");
            certify.setApplyInfo(null);
            certify.setObjInfo(null);
            return certify;
        }
        //状态显示
        if ( certify.getStatus()==TbCertifyStatusEnum.APPLYING.getId()){
            certify.setCanReApply(false);
        }else if(TbCertifyStatusEnum.SUCCESS.getId()==(certify.getStatus()) ){
            certify.setAcmeLog("");
            certify.setApplyLog("");
            if (certify.getNotAfter()>0 &&  certify.getNotAfter()<System.currentTimeMillis()){
                certify.setStatus(TbCertifyStatusEnum.TIMEOUT.getId());
                this.update(null,new UpdateWrapper<TbCertifyEntity>()
                        .eq("id",certify.getId())
                        .set("status",certify.getStatus())
                );
                return certify;
            }

        }else if( TbCertifyStatusEnum.USER.getId()==(certify.getStatus()) || TbCertifyStatusEnum.TIMEOUT.getId()==(certify.getStatus())){
            //成功-||-用户自有证书--超出时间
            this.updateCertifyEntityCertInfo(certify);
            //pass
            certify.setAcmeLog("");
            certify.setApplyLog("");
            return certify;
        } else if (TbCertifyStatusEnum.NEED_AUTH.getId()==(certify.getStatus())) {
            //todo
            if (1==certify.getSrcType()){
                certify.setAcmeLog(certify.getApiOrderInfo());
            }
            return certify;
        }else if (TbCertifyStatusEnum.RE_APPLY.getId()==certify.getStatus()){
            return certify;
        }
        //读取证书--信息-- acme 证书
        if(0==certify.getSrcType() &&  StringUtils.isBlank(certify.getObjInfo())){
            // 从目录获取 证书文件
            TbSiteEntity tbSite=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                    .eq("main_server_name",certify.getCommonName())
                    .eq("status",1)
                    .last("limit 1")
            );
            if (null!=tbSite){
                R r= AcmeShUtils.getApplyCertInfos(tbSite.getId(),tbSite.getMainServerName());
                //临时日志
                certify.setApplyLog(r.toJsonString());
                if (1==r.getCode()){
                    //if success
                    if (r.containsKey("pem_cert") && r.containsKey("private_key")){
                        TbCertifyObjVo certifyObjVo=new TbCertifyObjVo();
                        String pem=getFinalStr(r.get("pem_cert").toString());
                        certifyObjVo.setPem_cert(pem);
                        certifyObjVo.setPrivate_key(getFinalStr(r.get("private_key").toString()));
                        HashUtils.updateCertVoInfo(certifyObjVo);
                        certify.setObjInfo(DataTypeConversionUtil.entity2jonsStr(certifyObjVo));
                        certify.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
                        certify.setNotAfter(certifyObjVo.getNotAfter());
                        certify.setApplyLog("");
                        certify.setRemark("Letsencrypt");
                        this.updateById(certify);
                        return certify;
                    }
                }
            }
        }

        //显示信息
        this.updateCertifyEntityCertInfo(certify);
        return certify;
    }

    private void updateCertifyEntityCertInfo(TbCertifyEntity certify){
        if(null==certify || StringUtils.isBlank(certify.getObjInfo())){
            logger.error("update_CertifyEntity_CertInfo:Invalid ObjInfo");
            return;
        }
        TbCertifyObjVo certifyObjVo= DataTypeConversionUtil.string2Entity(certify.getObjInfo(),TbCertifyObjVo.class);
        if (null==certifyObjVo || StringUtils.isBlank(certifyObjVo.getPem_cert())) {
            logger.error("Invalid pem");
            return;
        }

        certify.setCanReApply(HashUtils.isCanApplyCert(certifyObjVo.getPem_cert()));
        certify.setApplyLog("");
        HashUtils.updateCertVoInfo(certifyObjVo);

        //不显示证书
        certifyObjVo.setPem_cert("");
        certifyObjVo.setPrivate_key("");
        certify.setCert(certifyObjVo);
        certify.setObjInfo("");
        certify.setApiOrderInfo("");
        certify.setApplyInfo("");
        certify.setAcmeLog("");
        if (certify.getStatus()!=TbCertifyStatusEnum.SUCCESS.getId()){
            certify.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
            this.update(null,new UpdateWrapper<TbCertifyEntity>()
                    .eq("id",certify.getId())
                    .set("status",certify.getStatus())
                    .set("not_after",certifyObjVo.getNotAfter())
            );
        }
        if (0==certify.getNotAfter() || certify.getNotAfter()!=certifyObjVo.getNotAfter()){
            this.update(null,new UpdateWrapper<TbCertifyEntity>()
                    .set("not_after",certifyObjVo.getNotAfter())
                    .eq("id",certify.getId())

            );
            certify.setNotAfter(certifyObjVo.getNotAfter());
        }



    }

    @Override
    public R getCertDetailById(Long userId, Integer id) {
        TbCertifyEntity certify=this.getOne(new QueryWrapper<TbCertifyEntity>()
                .eq("id",id)
                .eq(null!=userId,"user_id",userId)
        );
        if(StringUtils.isNotBlank(certify.getObjInfo())){
            TbCertifyObjVo certifyObjVo= DataTypeConversionUtil.string2Entity(certify.getObjInfo(),TbCertifyObjVo.class);
            if (null!=certifyObjVo){
                certify.setCanReApply(HashUtils.isCanApplyCert(certifyObjVo.getPem_cert()));
                certify.setNotAfter(certifyObjVo.getNotAfter());
                certify.setApplyLog("");
                certify.setStatus(TbCertifyStatusEnum.SUCCESS.getId());
                HashUtils.updateCertVoInfo(certifyObjVo);
                certify.setCert(certifyObjVo);
                this.update(null,new UpdateWrapper<TbCertifyEntity>()
                        .eq("id",certify.getId())
                        .set("status",certify.getStatus())
                        .set("not_after",certifyObjVo.getNotAfter())
                );
            }
        }
        return R.ok().put("data",certify);
    }


    //api ssl apply
    private void apiApplyCert( TbCertifyEntity srcCertify, String orderString, ZeroSslApiCertInfoVo cInfo,int step){
        if (StaticVariableUtils.zeroSslThread.containsKey(cInfo.getId())){
            logger.info("zero ssl apply cert thread has been running,id:"+cInfo.getId());
            return;
        }
        StaticVariableUtils.zeroSslThread.put(cInfo.getId(),1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //wait for insert cert
                    Thread.sleep(5*1000);
                }catch (Exception e){
                }
                JSONObject orderObj=DataTypeConversionUtil.string2Json(orderString);
                if (!orderObj.containsKey("code") || 1!=orderObj.getInteger("code") || !orderObj.containsKey("data")){
                    StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                    return;
                }
                JSONObject dataObj=orderObj.getJSONObject("data");
                if (!dataObj.containsKey("id")){
                    StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                    return;
                }
                String id=dataObj.getString("id");
                certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                        .eq("record_id",id)
                        .set("status",TbCertifyStatusEnum.APPLYING.getId())
                );
                if (1==step){
                    //首次需要添加验证
                    boolean inThisSysFlag=false;
                    JSONObject validationObj=dataObj.getJSONObject("validation");
                    if (!validationObj.containsKey("other_methods")){
                        certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                .eq("record_id",id)
                                .set("acme_log",orderString)
                                .set("status",TbCertifyStatusEnum.FAIL.getId())
                        );
                        logger.info(dataObj.toJSONString());
                        StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                        return;
                    }
                    //推送数据
                    JSONObject otherMObj=validationObj.getJSONObject("other_methods");
                    for (String key:otherMObj.keySet()){
                        TbSiteEntity site=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>()
                                .eq("main_server_name",key)
                                .last("limit 1")
                        );
                        if (null==site){
                            logger.info(key+" not in current system ");
                            continue;
                        }
                        inThisSysFlag=true;
                        JSONObject siteConf=otherMObj.getJSONObject(key);
                        if (siteConf.containsKey("file_validation_url_http") && siteConf.containsKey("file_validation_content")){
                            String  fvuh=siteConf.getString("file_validation_url_http");
                            JSONArray fvc=siteConf.getJSONArray("file_validation_content");
                            List<String> cs=new ArrayList<>();
                            for (int i = 0; i <fvc.size() ; i++) {
                                cs.add(fvc.getString(i));
                            }
                            String value=String.join("\\n",cs);
                            int uri_start=fvuh.indexOf(key)+key.length();
                            String uri=fvuh.substring(uri_start);
                            //    if ( $uri ~ "/.well-known/pki-validation/9F6CCB156972B217A562D40E5AB38256.txt"){ return 200 '12326A981E59C21E7A237AD1AF001DC6C3E974AE1C9F2BE9785E4914DA4D4447\ncomodoca.com\nebf48527fe8c4f7'; }
                            //String content= String.format("if ( $uri ~ \"%s\"){ return 200 '%s'; }",uri,value);
                            ZeroSslConfMapVo zvo=new ZeroSslConfMapVo();
                            zvo.setSiteId(site.getId().toString());
                            zvo.setCert_verify_zero_ssl_uri(uri);
                            zvo.setCert_verify_zero_ssl_value(value);
                            R r4=tbSiteServer.saveSiteAttr(DataTypeConversionUtil.entity2map(zvo) );
                            logger.info("cert_save:"+r4.toJsonString());
                            //推送数据
                            StaticVariableUtils.cacheSiteIdConfFileMap.remove(site.getId().toString());
                            Map pushMap=new HashMap(4);
                            pushMap.put(PushTypeEnum.SITE_CONF.getName(),site.getId().toString());
                            cdnMakeFileService.pushByInputInfo(pushMap);
                            break;
                        }
                    }
                    if (!inThisSysFlag){
                        certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                .eq("record_id",id)
                                .set("status",TbCertifyStatusEnum.NEED_AUTH.getId())
                        );
                        logger.info(dataObj.toJSONString());
                        StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                        return;
                    }
                }

                //验证数据
                if (true){
                    R r2=R.error();
                    boolean t2Flag=false;
                    int t2=30;
                    while (t2>0){
                        t2--;
                        try{
                            Thread.sleep(1000*20);
                        }catch (Exception e){
                        }
                        r2=ZeroSslUtils.verifyDomains(id);
                        if (1==r2.getCode()){
                            t2Flag=true;
                            break;
                        }
                    }
                    if (!t2Flag){
                        certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                .eq("record_id",id)
                                .set("acme_log",r2.toJsonString())
                                .set("status",TbCertifyStatusEnum.FAIL.getId())
                        );
                        logger.info("record_id:"+id+" verify failed;"+r2.toJsonString());
                        StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                        return;
                    }
                }

                //下载证书
                if (true){
                    int t3=30;
                    R r3=R.error();
                    while (t3>0){
                        t3--;
                        try{
                            Thread.sleep(1000*20);
                        }catch (Exception e){
                        }
                        r3=ZeroSslUtils.downloadCert(id);
                        if (1==r3.getCode()){
                            break;
                        }
                    }
                    TbCertifyEntity certify=srcCertify;
                    if (null==srcCertify){
                        certify=new TbCertifyEntity();
                        certify.setRecordId(id);
                        certify.setAcmeLog(r3.toJsonString());
                        certify.setSrcType(1);
                        cInfo.setId(id);
                        certify.setApplyInfo(DataTypeConversionUtil.entity2jonsStr(cInfo));
                        logger.info("insert:"+DataTypeConversionUtil.entity2jonsStr(certify));
                        certifyDao.insert(certify);
                    }else{
                        cInfo.setId(id);
                        certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                .eq("id",certify.getId())
                                .set("acme_log",r3.toJsonString())
                                .set("record_id",id)
                                .set("src_type",1)
                                .set("apply_info",DataTypeConversionUtil.entity2jonsStr(cInfo))
                        );
                    }

                    if (1==r3.getCode() ){
                        //保证数据
                        logger.info("success:"+r3.toJsonString());
                        String pem=r3.get("data").toString();
                        String key=cInfo.getPrivateKey();
                        CertCallbackForm cForm=new CertCallbackForm();
                        cForm.setCode(1);
                        cForm.setCode(0);
                        cForm.setPem(pem);
                        cForm.setKey(key);
                        R r4=cdnMakeFileService.saveCert2Db(cForm);
                        logger.info("save_Cert_2_Db:"+r4.toJsonString());
                        StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                        return;
                    }else{
                        certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>()
                                .eq("record_id",id)
                                .set("status",TbCertifyStatusEnum.FAIL.getId())
                        );
                        logger.info("下载失败");
                        StaticVariableUtils.zeroSslThread.remove(cInfo.getId());
                        return;
                    }

                }

            }

        }).start();
    }


    @Override
    public R zeroSslApiCreateCert(ZeroSslAPiCreateCertForm form) {
        //检测是否重复提交
        TbCertifyEntity certify=new TbCertifyEntity();
        if (null!=form.getSiteId()){
            //-1 0:待申请 1:成功 2失败 3自有
            Integer[] statusList={-1,0,2};
            certify=certifyDao.selectOne(new QueryWrapper<TbCertifyEntity>()
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

        // {"commonName":"deurnecityclassic.com.deurnecityclassic.com","id":12495,"type":1}
        if(null!=form.getSiteId()) {
            TbCertifyEntity srcEntity=certifyDao.selectOne(new QueryWrapper<TbCertifyEntity>() .eq("site_id",form.getSiteId()) .orderByDesc("id").last("limit 1"));
            if (null!=srcEntity){
                certify= srcEntity;
            }
        }
        certify.setUserId(form.getUserId());
        certify.setCommonName(form.getDomains());
        if (null!=form.getSiteId()){
            certify.setSiteId(form.getSiteId());
        }
        if (form.getDomains().contains(",")){
            String[] dms= form.getDomains().split(",");
            certify.setCommonName(dms[0]);
        }
        certify.setApplyInfo(DataTypeConversionUtil.entity2jonsStr(cInfo));
        if (null!=form.getSiteId() && 0!=form.getSiteId()){
            certify.setSiteId(form.getSiteId());
            TbSiteEntity tbSite=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",form.getSiteId()).select("user_id").last("limit 1"));
            if (null!=tbSite){
                certify.setUserId(tbSite.getUserId());
            }
        }
        certify.setSrcType(1);
        certify.setRemark("zeroSslApi证书");

        TbCertifyObjVo tbCertifyObjVo=new TbCertifyObjVo();
        tbCertifyObjVo.setPrivate_key(cInfo.getPrivateKey());
        tbCertifyObjVo.setStatus(0);
        certify.setObjInfo(DataTypeConversionUtil.entity2jonsStr(tbCertifyObjVo));
        String apiOrderString=r.toJsonString();
        certify.setApiOrderInfo(apiOrderString);
        if (1==r.getCode() && r.containsKey("data")){
            //创建订单成功
            JSONObject dataObj=DataTypeConversionUtil.string2Json(r.get("data").toString());
            if (null!=dataObj && dataObj.containsKey("validation") && dataObj.containsKey("id") ){
                String id=dataObj.getString("id");
                cInfo.setId(id);
                certify.setRecordId(id);
                certify.setApplyInfo(DataTypeConversionUtil.entity2jonsStr(cInfo));
                certify.setStatus(TbCertifyStatusEnum.APPLYING.getId());
                this.saveOrInsert(certify);
                this.apiApplyCert(certify,apiOrderString,cInfo,1);
                r.put("src",certify);
                return r;
            }
        }
        certify.setAcmeLog(r.toJsonString());
        certify.setStatus(TbCertifyStatusEnum.FAIL.getId());
        this.saveOrInsert(certify);
        r.put("src",certify);
        return r;
    }

    private boolean saveOrInsert(TbCertifyEntity certify){
        if (null==certify){
            return false;
        }
        if (null==certify.getId() || 0==certify.getId()){
             certifyDao.insert(certify);
        }else{
            certifyDao.updateById(certify);
        }
        return true;
    }

    /**
     * 证书重签
     * @param userId
     * @param
     * @return
     */
    @Override
    public R reIssued(Long userId, CertReIssuedVo params) {
        List<String> errMsg=new ArrayList<String>();
        List<Integer> cIds=new ArrayList<Integer>();
        if (null!=params.getId()){
            cIds.add(params.getId());
        }else if (StringUtils.isNotBlank(params.getIds())){
            String[] ids=params.getIds().split(",");
            for (String id:ids){
                cIds.add(Integer.parseInt(id));
            }
        }

        List<TbCertifyEntity> certifyList=certifyDao.selectList(new QueryWrapper<TbCertifyEntity>()
                .eq(null!=userId,"user_id",userId)
                .select("id,site_id,src_type")
                .in("id",cIds)
        ) ;
        if (null==certifyList || certifyList.isEmpty()  ) {
            return R.error("ID 错误");
        }
        for ( TbCertifyEntity certify: certifyList){
            if (1==params.getType()){
                ZeroSslConfig zConf= ZeroSslFactory.build();
                if (null==zConf ||  StringUtils.isBlank(zConf.getApi_key()) ) {
                    if (1==certifyList.size()){
                        return R.error("ZeroSsl api_key is empty!");
                    }
                    continue;
                }
            }
            //acme sh  0=ACME.SH  1==ZEROSSL API
            // Map<String,String> taskMap=new HashMap<>(8);
            // APPLY_CERTIFICATE("apply_certificate","certifyIds","通过证书ID申请证书"),
            // taskMap.put(PushTypeEnum.APPLY_CERTIFICATE.getName(),params.getId().toString());
            if (1==certifyList.size()){
                cdnMakeFileService.pushApplyCertificateBySiteId(certify.getSiteId(),0,params.getType(),certify.getId(),params.getDnsConfigId());
            }else{
                try {
                    cdnMakeFileService.pushApplyCertificateBySiteId(certify.getSiteId(),0,params.getType(),certify.getId(),params.getDnsConfigId());
                }catch (Exception e){
                    errMsg.add(e.getMessage());
                }
            }
        }
        return R.ok().put("errMsg",errMsg);
    }

    @Override
    public R saveCertRemark(CertRemarkVo vo) {
        TbCertifyEntity certify=certifyDao.selectOne(new QueryWrapper<TbCertifyEntity>()
                .eq("id",vo.getId())
                .eq(null!=vo.getUserId(),"user_id",vo.getUserId())
                .last("limit 1")
        );
        if (null==certify){
            return R.error("保存失败");
        }
        certifyDao.update(null,new UpdateWrapper<TbCertifyEntity>().eq("id",vo.getId()).set("remark",vo.getRemark()));
        return R.ok();
    }

    @Override
    public R applyCertificate(Long userId, CertApplyVo vo) {
        //0==申请  1==重新申请
        if (0==vo.getMode()){
            return cdnMakeFileService.pushApplyCertificateBySiteId(vo.getSiteId(),0,vo.getUseMode(),0,vo.getDnsConfigId());
        }else if (1==vo.getMode()){
            AcmeShUtils.deleteCertFileBySiteId(vo.getSiteId(),null);
            return  cdnMakeFileService.pushApplyCertificateBySiteId(vo.getSiteId(),1,vo.getUseMode(),0,vo.getDnsConfigId());
        }
        return R.ok();
    }

    @Override
    public R getCertStatistics(Long userId) {
        @Data
        class SslStatisticsVo{
            private int totalCount=0;
            private int validCount=0;
            private int expiredCount=0;
            private int  e7Count=0;
            private int e30Count=0;
        }

        Date e7date=DateUtils.addDateDays(new Date(),7);
        Date e30date=DateUtils.addDateDays(new Date(),30);

        SslStatisticsVo freeInfo=new SslStatisticsVo();

        freeInfo.setTotalCount(this.count(new QueryWrapper<TbCertifyEntity>().eq(null!=userId,"user_id",userId)));
        freeInfo.setValidCount(this.count(new QueryWrapper<TbCertifyEntity>().eq(null!=userId,"user_id",userId).gt("not_after",new Date().getTime()).eq("status",TbCertifyStatusEnum.SUCCESS.getId())));
        freeInfo.setExpiredCount(this.count(new QueryWrapper<TbCertifyEntity>().eq(null!=userId,"user_id",userId).isNotNull("not_after").ne("not_after",0).lt("not_after",new Date().getTime()).eq("status",TbCertifyStatusEnum.SUCCESS.getId())));
        freeInfo.setE7Count(this.count(new QueryWrapper<TbCertifyEntity>().eq(null!=userId,"user_id",userId).gt("not_after",new Date().getTime()).lt("not_after",e7date.getTime()).eq("status",TbCertifyStatusEnum.SUCCESS.getId())));
        freeInfo.setE30Count(this.count(new QueryWrapper<TbCertifyEntity>().eq(null!=userId,"user_id",userId).gt("not_after",new Date().getTime()).lt("not_after",e30date.getTime()).eq("status",TbCertifyStatusEnum.SUCCESS.getId())));

        return R.ok().put("free",freeInfo);

    }


    @Override
    public TbCertifyEntity saveCert(TbCertifyEntity certify) {
        String serverName=certify.getCommonName();
        if(StringUtils.isBlank(serverName)){
            return null;
        }
        Integer count=tbSiteDao.selectCount(new QueryWrapper<TbSiteEntity>().eq(null!=certify.getUserId(),"user_id",certify.getUserId()).eq("main_server_name",serverName));
        if(0==count){
            throw  new RRException("申请站点不存在！");
        }
        int cert_count=this.count(new QueryWrapper<TbCertifyEntity>().eq("common_name",serverName).ne("id",certify.getId()));
        if (0!=cert_count){
            throw  new RRException("证书已存在！");
        }
        if(null!=certify.getId() && 0!=certify.getId()){
            //update
            TbCertifyEntity source_certify=this.getById(certify.getId());
            if(null!=source_certify){
                if(!source_certify.getUserId().equals(certify.getUserId())){
                    return null;
                }
                if(1==source_certify.getStatus() && !source_certify.getCommonName().equals(certify.getCommonName())){
                    return null;
                }
                certify.setObjInfo(source_certify.getObjInfo());
                this.updateById(certify);
            }
            return null;
        }
        //insert
        certify.setStatus(TbCertifyStatusEnum.NEED_APPLY.getId());
        certify.setStatusMsg(TbCertifyStatusEnum.NEED_APPLY.getName());
        this.save(certify);
        return certify;
    }



    private void deleteCertFile(Long certId){
        //
        TbCertifyEntity certify=this.getById(certId);
        if (null!=certify){
            if (StringUtils.isNotBlank(certify.getCommonName())){
                String certFilePath=String.format("%s/%s",AcmeShUtils.getAcmeRootDir(),certify.getCommonName());
                //logger.debug("[certFilePath]="+certFilePath);
                File file = new File(certFilePath);
                if (file.exists()) {
                    ShellUtils.runShell("rm -rf "+certFilePath,false);
                }
            }
            TbSiteEntity site=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("main_server_name",certify.getCommonName()).last("limit 1"));
            if (null!=site){
                String cer_site_path=String.format("%s/ants/%s.crt",AcmeShUtils.getAcmeRootDir(),site.getId());
                File file = new File(cer_site_path);
                if (file.exists()) {
                    ShellUtils.runShell("rm -rf "+cer_site_path,false);
                }
            }
        }

    }

    @Override
    public void batDeleteCert(Long userId, Long[] ids) {
        for (Long id:ids){
            TbCertifyEntity cert=this.getOne(new QueryWrapper<TbCertifyEntity>()
                    .eq("id",id)
                    .eq(null!=userId,"user_id",userId)
                    .last("limit 1")
                    .select("id,src_type")
            );
            if(null!=cert){
                if ( 0==cert.getSrcType()){
                    this.deleteCertFile(id);
                }else if (1==cert.getSrcType()) {
                    //todo
                }
                this.removeById(id);
            }
        }
        //baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    private int getAliastSslIsInCert(String host){
        TbSiteEntity siteEntity=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("main_server_name",host).last("limit 1"));
        if (null==siteEntity){
            return 0;
        }
        TbSiteMutAttrEntity ssl_pem=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                .eq("site_id",siteEntity.getId())
                .eq("pkey",SiteAttrEnum.SSL_OTHER_CERT_PEM.getName())
                .eq("status",1)
                .last("limit 1")
        );
        if (null==ssl_pem){
            return 0;
        }
        List<TbSiteAttrEntity> aliasList=tbSiteAttrDao.selectList(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey",SiteAttrEnum.ALIAS.getName())
                .eq("site_id",siteEntity.getId())
                .select("pvalue")
                .eq("status",1)
        );
        if (aliasList.isEmpty()){
            return 0;
        }

        JSONObject pemObj=DataTypeConversionUtil.string2Json(ssl_pem.getPvalue());
        if (!pemObj.containsKey("value")){
            return 0;
        }
        String pem= pemObj.getString("value").trim();
        TbCertifyObjVo certifyObjVo =new TbCertifyObjVo();
        certifyObjVo.setPem_cert(pem);
        HashUtils.updateCertVoInfo(certifyObjVo);
        if (StringUtils.isBlank(certifyObjVo.getSubjectAlternativeNames())){
            return 1;
        }
        String[] aCN=certifyObjVo.getSubjectAlternativeNames().split(",");
        for (TbSiteAttrEntity siteAttr:aliasList){
            String alias=siteAttr.getPvalue();
            boolean matchFlag=false;
            for (String cn:aCN){
                if (cn.contains("*")){
                    String  patternString = cn.replace(".", "\\.").replace("*", ".*");
                    Pattern pattern = Pattern.compile(patternString);
                    Matcher matcher = pattern.matcher(alias);
                    if (matcher.matches()) {
                        matchFlag=true;
                        break;
                    }
                }else{
                    if (cn.equals(alias)){
                        matchFlag=true;
                        break;
                    }
                }
            }
            if (false==matchFlag){
                return 1;
            }
        }




        return 0;
    }

    @Override
    public R getCertifyList(Long userId, String host) {
        List<TbCertifyEntity> type_all_ls=new ArrayList<>();
        List<TbCertifyEntity> type1_ls=this.list(new QueryWrapper<TbCertifyEntity>()
                .eq(null!=userId,"user_id",userId)
                .like("common_name",host)
                .isNotNull("obj_info")
        );
        List<TbCertifyEntity> type2_ls=this.list(new QueryWrapper<TbCertifyEntity>()
                .eq(null!=userId,"user_id",userId)
                .like("common_name","%*%")
                .isNotNull("obj_info")
        );
        if (type1_ls.size()>0){
            type_all_ls.addAll(type1_ls);
        }
        if (type2_ls.size()>0){
            type_all_ls.addAll(type2_ls);
        }
        Iterator<TbCertifyEntity> iterator=type_all_ls.iterator();
        while (iterator.hasNext()){
            TbCertifyEntity item=iterator.next();
            String pattern=item.getCommonName().replace(".","\\.");
            //pattern=pattern.replace("*.",".*");
            pattern=pattern.replace("*\\.",".*\\.?");
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(host);
            if (!m.matches()){
                iterator.remove();
                continue;
            }
            JSONObject ce_obj=DataTypeConversionUtil.string2Json(item.getObjInfo());
            if (ce_obj!=null && ce_obj.containsKey("pem_cert")){
                String ce_str=ce_obj.getString("pem_cert");
                if(!HashUtils.isValidCert(ce_str)){
                    iterator.remove();
                }
            }
        }
        TbCertifyEntity sysCertify=new TbCertifyEntity();
        if (0==type_all_ls.size()){
            TbSiteEntity siteEntity=tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("main_server_name",host).last("limit 1"));
            if (null!=siteEntity){
                sysCertify=this.getOne(new QueryWrapper<TbCertifyEntity>().eq("common_name",host).last("limit 1"));
                if (null==sysCertify){
                    //acme
                    sysCertify=new TbCertifyEntity();
                    sysCertify.setCommonName(host);
                    sysCertify.setUserId(siteEntity.getUserId());
                    sysCertify.setStatus(-1);
                    sysCertify.setSiteId(siteEntity.getId());
                    sysCertify.setSrcType(0);
                    sysCertify.setCanReApply(true);
                }else {
                    this.updateCertByAcmeSh(sysCertify);
                }
            }
            if (sysCertify.isCanReApply()){
                sysCertify.setCanReApplyMode(0);
            }
        }else{
            //site pem--alias 对比
            int mode= this.getAliastSslIsInCert(host);
            if (1==mode){
                sysCertify.setCanReApply(true);
                sysCertify.setCanReApplyMode(1);
            }
        }
//        Map map=new HashMap();
//        map.put("data",type_all_ls);
//        map.put("sys_certify",sysCertify);
        return R.ok().put("data",type_all_ls).put("sys_certify",sysCertify);
    }


}
