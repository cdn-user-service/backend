package io.ants.modules.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.TbCertifyDao;
import io.ants.modules.app.dao.TbRewriteDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbCertifyEntity;
import io.ants.modules.app.entity.TbRewriteEntity;
import io.ants.modules.app.service.TbRewriteService;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.sys.enums.PushSetEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.enums.RedisStreamType;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.CdnSuitService;
import io.ants.modules.sys.service.InputAvailableService;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Administrator
 */
@Service("rewriteService")
public class TbRewriteServiceImpl extends ServiceImpl<TbRewriteDao, TbRewriteEntity> implements TbRewriteService {

    private final Integer[] rewriteMode={301,302,303,307};
    private final String[]  schemeMode={"http","https","$scheme"};
    private final String MODE_SAVE="save";
    private final String MODE_DELETE ="delete";
    private final int DELETE_MODE=2;

    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private CdnSuitService cdnSuitService;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private InputAvailableService inputAvailableService;
    @Autowired
    private TbUserService userService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private TbCertifyDao tbCertifyDao;

   private List<String> getAliasLs(String alias){
       List<String> fAlias=new ArrayList<>();
       String[] aliasLs=alias.split(",");
       for (String a:aliasLs){
           if (StringUtils.isNotBlank(a)){
               if(!fAlias.contains(a)){
                   fAlias.add(a);
               }
           }
       }
       return fAlias;
   }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        if ( !StaticVariableUtils.exclusive_modeList.contains("rewrite")){
            System.out.println("exclusive_modeList not include rewrite");
            return null;
        }

        int page=1;
        int limit=20;
        if (params.containsKey("page") && params.containsKey("limit")){
            params.put("page",params.get("page").toString());
            params.put("limit",params.get("limit").toString());
            page=Integer.parseInt(params.get("page").toString());
            limit=Integer.parseInt(params.get("limit").toString());
        }

        List<String> userIdLs=new ArrayList<>();
        if (params.containsKey("user") && null!=params.get("user") ){
            String user=params.get("user").toString();
            if (StringUtils.isNotBlank(user))
            {
                if (StringUtils.isNotBlank(user)){
                    String uids= userService.key2userIds(user);
                    userIdLs.addAll(Arrays.asList(uids.split(",")));
                }
            }
        }
        if(params.containsKey("userIds") && null!=params.get("userIds") ){
            String userIds=params.get("userIds").toString();
            if (StringUtils.isNotBlank(userIds))
            {
                userIdLs.addAll(Arrays.asList(params.get("userIds").toString().split(",")));
            }
        }

        String serverName="";
        //serverName
        if (params.containsKey("serverName")){
            serverName=params.get("serverName").toString();
        }

        final String finalServerName = serverName;
        IPage<TbRewriteEntity> iPage = this.page(
                new Page<>(page,limit),
                new QueryWrapper<TbRewriteEntity>()
                        .in(userIdLs.size()>0,"user_id",userIdLs.toArray() )
                        .and(StringUtils.isNotBlank(finalServerName),q->q.like("server_name", finalServerName).or().like("alias", finalServerName))
        );
        iPage.getRecords().forEach(item->{
            item.setSuitObj(cdnSuitService.getSuitDetailBySerial(item.getUserId(),item.getSerialNumber(),false,false));
            item.setAliasLs(getAliasLs(item.getAlias()));
            item.setUser(tbUserDao.getUserNamesByUserId(item.getUserId()));
        });
        return new PageUtils(iPage);
    }

//    boolean checkServeNameExistInConf(Integer rwId,String name){
//        if (StringUtils.isBlank(name)){
//            return false;
//        }
//        //先自己里找
//        Integer c1=this.count(new QueryWrapper<TbRewriteEntity>()
//                .ne("id",rwId)
//                .and(q->q.eq("server_name",name).or().like("alias",","+name+","))
//        );
//        if (c1>0){
//            throw new RRException("["+name+"]URL转发中已存在");
//        }
//        //alias
//
//
//        //site
//        int c2=tbSiteDao.selectCount(new QueryWrapper<TbSiteEntity>()
//                .eq("main_server_name",name)
//        );
//        if (c2>0){
//            throw new RRException("["+name+"]站中已存在[code=-1]");
//        }
//        //3 site alias
//        int c3=tbSiteAttrDao.selectCount(new QueryWrapper<TbSiteAttrEntity>()
//                .eq("pkey","alias")
//                .eq("pvalue",name)
//        );
//        if (c3>0){
//            throw new RRException("["+name+"]站中已存在[code=-2]");
//        }
//        return false;
//    }

    private String addBeforeEndStr(String alias){
        List<String> fAlias=new ArrayList<>();
        String[] aliasLs=alias.split(",");
        for (String a:aliasLs){
            if (StringUtils.isNotBlank(a)){
                if(!fAlias.contains(a)){
                    fAlias.add(a);
                }
            }
        }
        String n_alias=String.join(",",fAlias);
        alias=","+n_alias+",";
        return  alias;
    }

    private void checkAndUpdateCertInfo(TbRewriteEntity rewrite){
       if (StringUtils.isBlank(rewrite.getCertStr()) || StringUtils.isBlank(rewrite.getKeyStr())){
           rewrite.setNotAfter(null);
       }else {
           //检测证书对
           PemObject pemObject= SslUtil.getPrivateKeyObject(rewrite.getKeyStr());
           if (null==pemObject){
               throw new RRException("证书有误!");
           }
           if (!pemObject.getType().equals("EC PRIVATE KEY")){
               if(!SslUtil.verifySign(rewrite.getCertStr(),rewrite.getKeyStr())){
                   throw new RRException("证书与私钥不匹配!");
               }
           }
           long endTime= HashUtils.getCertEndTime(rewrite.getCertStr());
           rewrite.setNotAfter(new Date(endTime));
       }
    }

    @Override
    public TbRewriteEntity saveObj(Long userId,TbRewriteEntity rewrite) {
        if ( !StaticVariableUtils.exclusive_modeList.contains("rewrite")){
            return null;
        }
        if (null!=userId){
            if (!userId.equals(rewrite.getUserId())){
                return null;
            }
        }
        if (StringUtils.isBlank(rewrite.getServerName())){
            return null;
        }
        if (1==rewrite.getRewriteType()){
            //rewrite--方式
            if (!Arrays.asList(rewriteMode).contains(rewrite.getRewriteMode())){
                throw new RRException("转发方式可选值为【301】【302】【303】【307】中一个");
            }
            if (!Arrays.asList(schemeMode).contains(rewrite.getScheme())){
                throw new RRException("转发协议可选值为【http】【https】【$scheme】中一个");
            }
            if ("follow".equals(rewrite.getFollowMode())){
                if(rewrite.getTarget().contains("?")) {
                    throw new RRException("转发目标格式有误[?]！！");
                }
            }
            //检测转发目标格式
            String tUrl=rewrite.getTarget().trim();
            final String pattern = "^http(s?):.*";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(tUrl);
            if (m.matches()){
                throw new RRException("转发目标不能包含协议！");
            }
            final String patternTarget = "[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?";
            Pattern r2 = Pattern.compile(patternTarget);
            Matcher m2 = r2.matcher(tUrl);
            if(!m2.matches()){
                throw new RRException("转发目标格式有误！！");
            }
        }else if(2==rewrite.getRewriteType()){
            if (StringUtils.isBlank(rewrite.getJsContent())){
                throw new RRException("转发目标JS有误！！");
            }
        }

        if (null==rewrite.getId() || 0==rewrite.getId()){
            //INSERT
            rewrite.setId(0);
            if(!inputAvailableService.checkNginxServerNameAndAliasIsValid("rewrite",0,rewrite.getServerName())){
                throw new RRException("添加失败，存在的域名！");
            }
            if (StringUtils.isNotBlank(rewrite.getAlias())){
                String[] aliasLs=rewrite.getAlias().split(",");
                for (String alias:aliasLs){
                    if(!inputAvailableService.checkNginxServerNameAndAliasIsValid("rewrite",0,alias)){
                       throw new RRException("添加失败，存在的域名！");
                    }
                }
            }
            rewrite.setAlias(this.addBeforeEndStr(rewrite.getAlias()));
            rewrite.setCreateTmp(new Date());
            this.checkAndUpdateCertInfo(rewrite) ;
            this.save(rewrite);
            //            TbCertifyEntity certify=tbCertifyDao.selectOne(new QueryWrapper<TbCertifyEntity>().eq("common_name",rewrite.getServerName()).last("limit 1"));
            //            if (null==certify){
            //                certify=new TbCertifyEntity();
            //                certify.setStatus(-1);
            //                certify.setUserId(rewrite.getUserId());
            //                certify.setCommonName(rewrite.getServerName());
            //                tbCertifyDao.insert(certify);
            //            }
            pushSendRewriteConf(rewrite,MODE_SAVE);

        }else {
            //MODIFY
            if(!inputAvailableService.checkNginxServerNameAndAliasIsValid("rewrite",rewrite.getId(),rewrite.getServerName())){
                throw new RRException("修改失败，存在的域名！");
            }
            if (StringUtils.isNotBlank(rewrite.getAlias())){
                for (String alias:rewrite.getAlias().split(",")){
                    if (StringUtils.isNotBlank(alias)){
                        if(!inputAvailableService.checkNginxServerNameAndAliasIsValid("rewrite",rewrite.getId(),alias)){
                            throw new RRException("修改失败，存在的域名！");
                        }
                    }
                }
            }
            rewrite.setAlias(this.addBeforeEndStr(rewrite.getAlias()));
            rewrite.setCreateTmp(new Date());
            this.checkAndUpdateCertInfo(rewrite) ;
            this.updateById(rewrite);
            if (0==rewrite.getStatus()){
                String pathKey=PushSetEnum.REWRITE_CONF.getTemplatePath().replace("###rewrite_id_name###",rewrite.getId()+"_");
                String cmd="rm -rf "+pathKey;
                String xid=redisUtils.streamXAdd(RedisStreamType.STREAM_HEAD.getName()+RedisStreamType.COMMAND.getName(),RedisStreamType.STREAM_NORMAL_KEY.getName(),cmd);
                if (StringUtils.isNotBlank(xid) && redisUtils.delete(pathKey)){
                    this.updateById(rewrite);
                }
            }else {
                this.pushSendRewriteConf(rewrite,MODE_SAVE);
            }
        }
        return  rewrite;
    }

    private void pushSendRewriteConf(TbRewriteEntity rewrite,String mode){
       Map map=new HashMap();
       if (MODE_SAVE.equals(mode) && 1==rewrite.getStatus()){
           map.put("push_rewrite_conf",rewrite.getId());
           cdnMakeFileService.pushByInputInfo(map);
       }else if(MODE_DELETE.equals(mode)){
           if(1==DELETE_MODE){
               String vValue=String.format("%d_%s_",rewrite.getId(),rewrite.getServerName());
               String path= PushSetEnum.STREAM_CONF.getTemplatePath().replace("###sp_id###",vValue);
               map.put(PushTypeEnum.COMMAND_DELETE_DIR.getName(),path);
               cdnMakeFileService.pushByInputInfo(map);
           }else if(2==DELETE_MODE){
               map.put(PushTypeEnum.CLEAN_DEL_REWRITE_CONF.getName(),rewrite.getId().toString());
               cdnMakeFileService.pushByInputInfo(map);
           }
       }

    }

    @Override
    public Integer removeByIds(Long userId, String ids) {
        if ( !StaticVariableUtils.exclusive_modeList.contains("rewrite")){
            return null;
        }
        int i=0;
        for (String id:ids.split(",")){
            TbRewriteEntity rw=this.getById(id);
            if (null==rw){
                continue;
            }else if(null!=userId && !rw.getUserId().equals(userId)){
                continue;
            }
            this.pushSendRewriteConf(rw, MODE_DELETE);
            tbCertifyDao.delete(new QueryWrapper<TbCertifyEntity>().eq("common_name",rw.getServerName()));
            rw.setStatus(2);
            this.updateById(rw);
            i++;
        }
        return  i;
    }


}