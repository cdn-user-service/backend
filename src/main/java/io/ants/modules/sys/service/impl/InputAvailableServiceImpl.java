package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.exception.RRException;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.IPUtils;
import io.ants.common.utils.StaticVariableUtils;
import io.ants.modules.app.dao.*;
import io.ants.modules.app.entity.*;
import io.ants.modules.sys.dao.TbCdnPublicMutAttrDao;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.PublicEnum;
import io.ants.modules.sys.enums.SiteAttrEnum;
import io.ants.modules.sys.service.InputAvailableService;
import io.ants.modules.sys.vo.NgxSourceBaseInfoVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InputAvailableServiceImpl implements InputAvailableService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;

    @Autowired
    private TbStreamProxyAttrDao tbStreamProxyAttrDao;

    @Autowired
    private TbSiteMutAttrDao tbSiteMutAttrDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private TbRewriteDao tbRewriteDao;
    @Autowired
    private TbCdnPublicMutAttrDao tbCdnPublicMutAttrDao;


    /**
     * @return 四层转发端口冲突
     */
    private boolean checkProxyPortValidUsed(int port, Integer areaId, TbStreamProxyEntity proxy){

        if (false){
            //cdn proxy 80，443 -- 禁用 ，不禁用，如果有站点就自动不可用
            if(Arrays.asList(StaticVariableUtils.STREAM_FORBID_BIND_PORT).contains(port)){
                //logger.debug("insert fail,listen  is error");
                throw new RRException(port+" 端口不可使用[1]！");
            }
        }

        //其它proxy占用检测
        if (true){
            //  \"listen\":1456,
            //                String likeListenA="\"listen\":\""+port+"\",";
            //                String likeListenB="\"listen\":"+port+",";
            //                String[] likes={likeListenA,likeListenB};
            //                for (String likeStr:likes){
            //                }
            Integer count=tbStreamProxyAttrDao.selectCount(new QueryWrapper<TbStreamProxyAttrEntity>()
                    .ne("stream_id",proxy.getId())
                    .eq("pkey","port")
                    .eq("pvalue",String.valueOf(port))
                    .eq("area_id",areaId)
            );
            if (count>0){
               //logger.debug("insert fail,listen  is used");
                throw new RRException(port+" 端口已被[proxy]占用！");
            }

        }

        // SITE 占用检测
        if (true){
            //                String likeValueA=",\"port\":" + port + ",";
            //                String likeValueB=",\"port\":\"" + port + "\",";
            //                String[] likes={likeValueA,likeValueB};
            //                for (String likeStr:likes){
            //                    Integer count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
            //                            .eq("status", 1)
            //                            .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
            //                            .like("pvalue", likeStr)
            //                    );
            //                }
            //"{\"pvalue1\":\"listen\",\"pvalue2\":\"areaId\"}"
            TbSiteMutAttrEntity mutAttrEntity=tbSiteMutAttrDao.selectOne(new QueryWrapper<TbSiteMutAttrEntity>()
                    .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                    .eq("pvalue1",port)
                    .eq("pvalue2",areaId)
                    .select("id,site_id")
            );
            if (null!=mutAttrEntity){
               //logger.debug("insert fail,listen  is used");
                throw new RRException( String.format("[%d]端口已被站点[%d]占用！",port,mutAttrEntity.getSiteId()) );
            }

        }
         return true;
    }


    /**
     * 检测站点 端口 冲突
     * @param port
     * @param
     * @return
     */
    private boolean checkSitePortValidUser(Integer port,Integer areaId, TbSiteMutAttrEntity mutAttr){
        //site 禁用
        if(Arrays.asList(StaticVariableUtils.SITE_FORBID_LISTEN_PORT).contains(port)){
           //logger.debug("insert fail,listen  is error");
            throw new RRException(port+" 端口不可使用[2]！");
        }
        //proxy占用检测
        if (true){
            //  \"listen\":1456,
            //                String likeListenA="\"listen\":\""+port+"\",";
            //                String likeListenB="\"listen\":"+port+",";
            //                String[] likes={likeListenA,likeListenB};
            //                for (String likeStr:likes){
            //                    Integer count=tbStreamProxyDao.selectCount(new QueryWrapper<TbStreamProxyEntity>()
            //                            .like("conf_info",likeStr));
            //                }
            Integer count=tbStreamProxyAttrDao.selectCount(new QueryWrapper<TbStreamProxyAttrEntity>()
                    .eq("pkey","port")
                    .eq("pvalue",port.toString())
                    .eq("area_id",areaId)
            );
            if (count>0){
               //logger.debug("insert fail,listen  is used");
                throw new RRException(port+" 端口已被四层转发占用！");
            }
        }

        // 自身检测
        if (false){
            //{"SiteId":1,"id":51,"protocol":"https","port":443,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"::1","domain":"","port":"443","line":1,"weight":1}]}

        }

        //http 的端口不能在HTTPS 中出现;https的端口不能在HTTP 中出现
        if (true){
            //"{\"pvalue1\":\"listen\",\"pvalue2\":\"areaId\"}"
            //{"protocol":"https","port":443,"s_protocol":"https","upstream":"check","source_set":"ip","line":[{"ip":"121.62.18.146","domain":"","port":"443","line":1,"weight":1}]}
            NgxSourceBaseInfoVo vo= DataTypeConversionUtil.string2Entity(mutAttr.getPvalue(),NgxSourceBaseInfoVo.class);
            String protocol=vo.getProtocol();
            if ("http".equals(protocol)){
                //http 的端口不能在HTTPS 中出现
                String likeValue1=String.format("\"protocol\":\"https\",\"port\":\"%d\",",port);
                String likeValue2=String.format("\"protocol\":\"https\",\"port\":%d,",port);
                Integer count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                        .ne(0!=mutAttr.getId(),"id", mutAttr.getId())
                        .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                        .eq("pvalue2",areaId)
                        .and(q->q.like("pvalue",likeValue1 ).or().like("pvalue",likeValue2))

                );
                if (count > 0) {
                    throw new RRException("端口【" + port + "】重复！端口已被【https】占用");
                }
            }else if ("https".equals(protocol)){
                //https的端口不能在HTTP 中出现
                String likeValue1=String.format("\"protocol\":\"http\",\"port\":\"%d\",",port);
                String likeValue2=String.format("\"protocol\":\"http\",\"port\":%d,",port);
                Integer count = tbSiteMutAttrDao.selectCount(new QueryWrapper<TbSiteMutAttrEntity>()
                        .ne(0!=mutAttr.getId(),"id", mutAttr.getId())
                        .eq("pkey", SiteAttrEnum.SOURCE_BASE_INFO.getName())
                        .eq("pvalue2",areaId)
                        .and(q->q.like("pvalue",likeValue1 ).or().like("pvalue",likeValue2))
                );
                if (count > 0) {
                    throw new RRException("端口【" + port + "】重复！端口已被【http】占用");
                }
            }
        }
        return true;
    }

    @Override
    public boolean checkListenIsAvailable(Integer port,Integer areaId, String mode,TbStreamProxyEntity proxy,TbSiteMutAttrEntity mutAttr) {

        if (!IPUtils.isValidPortEx(port.toString())){
            throw new RRException(port+" 端口范围必须为 1-65535 ");
        }

        //cdn 系统 节点 需使用，不可使用
        if (Arrays.asList(StaticVariableUtils.PUBLIC_FORBID_LISTEN_PORT).contains(port)){
            logger.error("insert fail,listen  is error 1");
            throw new RRException(port+" 端口不可使用[0],系统使用中！");
        }

        //全局自定义的
        TbCdnPublicMutAttrEntity pubAttr=tbCdnPublicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq("pkey", PublicEnum.COMMON_FORBID_PORT.getName())
                .eq("status",1)
        );
        if (null!=pubAttr){
            if (StringUtils.isNotBlank(pubAttr.getPvalue())){
                String[] userForbid=pubAttr.getPvalue().split("\\|");
                if (Arrays.asList(userForbid).contains(port)){
                    logger.error("insert fail,listen  is error 2");
                    throw new RRException(port+" 端口不可使用[1],系统使用中！");
                }
            }
        }

        if ("proxy".equals(mode)){
           return this.checkProxyPortValidUsed(port,areaId,proxy);
        }else  if ("site".equals(mode)) {
            return checkSitePortValidUser(port,areaId,mutAttr);
        }
        return true;
    }

    @Override
    public boolean checkNginxServerNameAndAliasIsValid(String addMode, int id, String name) {
        //noinspection AlibabaUndefineMagicConstant

        final  String serverNamePattern = "^(\\*\\.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
        Pattern r = Pattern.compile(serverNamePattern);
        Matcher m = r.matcher(name);
        if(!m.matches()){
            throw new RRException("["+name+"]域名格式有误【1】！");
        }

        int countA1=tbSiteDao.selectCount(new QueryWrapper<TbSiteEntity>()
                .ne("site".equals(addMode) && id>0,"id",id)
                .eq("main_server_name",name)
        );
        if (countA1>0 ){
           //logger.debug("found site-like "+name);
            return  false;
        }

        int countB1=tbSiteAttrDao.selectCount(new QueryWrapper<TbSiteAttrEntity>()
                .ne("alias".equals(addMode) && id>0,"id",id)
                .eq("pkey",SiteAttrEnum.ALIAS.getName())
                .eq("pvalue",name)
        );
        if (countB1>0){
           //logger.debug("found site-alias-like "+name);
            return  false;
        }


        int countC1=tbRewriteDao.selectCount(new QueryWrapper<TbRewriteEntity>()
                .ne("rewrite".equals(addMode) && id>0 ,"id",id)
                .eq("server_name",name)
        );
        if (countC1>0){
           //logger.debug("found rewrite-like "+name);
            return  false;
        }


        int countD=tbRewriteDao.selectCount(new QueryWrapper<TbRewriteEntity>()
                .ne("rewrite".equals(addMode) && id>0 ,"id",id)
                .like("alias",","+name+",")
        );
        if (countD>0){
           //logger.debug("found rewrite-alias-like "+name);
            return  false;
        }
        return true;
    }
}
