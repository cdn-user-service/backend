package io.ants.modules.app.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.utils.RedisUtils;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.TbSiteAttrDao;
import io.ants.modules.app.dao.TbSiteCachePrefetchDao;
import io.ants.modules.app.dao.TbSiteDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbSiteAttrEntity;
import io.ants.modules.app.entity.TbSiteCachePrefetchEntity;
import io.ants.modules.app.entity.TbSiteEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.DeleteIdsForm;
import io.ants.modules.app.form.QuerySiteCachePrefetchPageForm;
import io.ants.modules.app.service.TbSiteCacheService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.modules.sys.enums.RedisStreamType;
import io.ants.modules.sys.enums.SiteAttrEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.*;


@Service
public class TbSiteCacheServiceImpl extends ServiceImpl<TbSiteCachePrefetchDao, TbSiteCachePrefetchEntity>implements TbSiteCacheService {

    @Autowired
    private TbSiteCachePrefetchDao tbSiteCachePrefetchDao;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private TbSiteDao tbSiteDao;
    @Autowired
    private TbSiteAttrDao tbSiteAttrDao;
    @Autowired
    private RedisUtils redisUtils;

    private String getUserName(long userId) {
        TbUserEntity user = tbUserDao.selectOne(new QueryWrapper<TbUserEntity>().eq("user_id", userId).select("username").last("limit 1"));
         if (null!=user){
             return user.getUsername();
         }
        return "";
    }

    private String getSiteServerName(long siteId) {
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id", siteId).select("main_server_name").last("limit 1"));
        if (null!=site){
            return site.getMainServerName();
        }
        return "";
    }

    private Integer getSiteIdByServerName(String serverName) {
        TbSiteEntity site = tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("main_server_name", serverName).select("id").last("limit 1"));
        if (null!=site){
            return site.getId();
        }
        return null;
    }
    @Override
    public R getCachePrePageList(long userId, QuerySiteCachePrefetchPageForm form) {
        ValidatorUtils.validateEntity(form);
        IPage<TbSiteCachePrefetchEntity> iPage=tbSiteCachePrefetchDao.selectPage(
             new Page<>(form.getPage(),form.getLimit()),
             new QueryWrapper<TbSiteCachePrefetchEntity>()
                     .eq(0!=userId,"user_id",userId)
                     .like(StringUtils.isNotBlank(form.getUserName()),"user_name",form.getUserName())
                     .like(StringUtils.isNotBlank(form.getServerName()),"site_server_name",form.getServerName())
                     .like(StringUtils.isNotBlank(form.getKey()),"pf_path",form.getKey())
        );
        return R.ok().put("data",new PageUtils(iPage));
    }

    private R xAddPreFetcher( ){
        Set<String> xIds = new HashSet<String>();
        List<TbSiteCachePrefetchEntity> serverList=tbSiteCachePrefetchDao.selectList(new QueryWrapper<TbSiteCachePrefetchEntity>()
                .eq("status",1)
                .groupBy("site_server_name,frequency")
                .orderByAsc("site_server_name")
                .select("site_server_name,frequency")
        );
        if (serverList.size()==0) {return R.ok().put("msg","0");};
        Map<String,String> taskMap=new HashMap<>();
        for (TbSiteCachePrefetchEntity svItem: serverList){
            try{
                List<TbSiteCachePrefetchEntity>ls=tbSiteCachePrefetchDao.selectList(new QueryWrapper<TbSiteCachePrefetchEntity>()
                        .eq("status",1)
                        .eq("site_server_name",svItem.getSiteServerName())
                        .eq("frequency",svItem.getFrequency())
                        .orderByAsc("id")
                        .select("pf_path")
                );
                if(0==ls.size()){
                    continue;
                }

                //xadd public:purge-stream  prefetch:165668.com:3600 /index.html \n/index.css \n
                //1) 1) "1736921869682-0
                //   2) 1) "prefetch:w7soft.com.cn:3600"
                //      2) "/check_cname_in_sys/ \n/ \n/favicon.ico \n/.well-known/acme-challenge/1 \n/index.js \n/index.png \n/.env \n/index.html \n/check_cname_in_sys \n/.git/config \n/.well-known/acme-challenge/L6heEcPwjHo5qfKtKyj5aW0mDAQihsdmz22pk7Wl6Jg \n/index.css \n/robots.txt \n/sitemap.xml \n/wp-admin/setup-config.php \n/index.htm \n/.aws/credentials \n/.env.production \n/.git/HEAD \n/.kube/config \n/.ssh/id_ecdsa \n/.ssh/id_ed25519 \n/.ssh/id_rsa \n/.svn/wc.db \n/.vscode/sftp.json \n/111111.com \n/2019/wp-includes/wlwmanifest.xml \n/2020/wp-includes/wlwmanifest.xml \n/2021/wp-includes/wlwmanifest.xml \n/_vti_pvt/administrators.pwd \n/_vti_pvt/authors.pwd \n/_vti_pvt/service.pwd \n/ads.txt \n/api/.env \n/backup.sql \n/backup.tar.gz \n/backup.zip \n/blog/wp-includes/wlwmanifest.xml \n/cloud-config.yml \n/cms/wp-includes/wlwmanifest.xml \n/config.json \n/config.php \n/config.xml \n/config.yaml \n/config.yml \n/config/database.php \n/config/production.json \n/database.sql \n/docker-compose.yml \n/dump.sql \n/etc/shadow \n/etc/ssl/private/server.key \n/feed \n/feed/ \n/phpinfo.php \n/secrets.json \n/server-status \n/server.key \n/shop/wp-includes/wlwmanifest.xml \n/site/wp-includes/wlwmanifest.xml \n/test/wp-includes/wlwmanifest.xml \n/user_secrets.yml \n/web.config \n/web/wp-includes/wlwmanifest.xml \n/wordpress/wp-admin/setup-config.php \n/wordpress/wp-includes/wlwmanifest.xml \n/wp-config.php \n/wp-includes/ID3/license.txt \n/wp/wp-includes/wlwmanifest.xml \n/wp1/wp-includes/wlwmanifest.xml \n/www \n/xmlrpc.php \n/.well-known/check_cname_in_sys/ \n/.well-known/check_cname_in_sys/^C \n/aaa \n/index.htms \n/index.htmsf \n/wp-content/ \n"

                String key=String.format("prefetch:%s:%d",svItem.getSiteServerName(),svItem.getFrequency());
                StringBuilder valuesSb=new StringBuilder();
                for (TbSiteCachePrefetchEntity pr:ls){
                    valuesSb.append(String.format("%s \n",pr.getPfPath()));
                }
                taskMap.put(key,valuesSb.toString());
            }catch (Exception e){
                e.printStackTrace();
                xIds.add(e.getMessage());
            }
        }
        String xaddKey="public"+ RedisStreamType.MULTI_PURGE_COMMAND.getName();
        String setKey="prefetch:domain:interval";
        redisUtils.delete(setKey);
        redisUtils.hashSetPushMap(setKey,taskMap);
        String xid= redisUtils.streamXAdd(xaddKey,setKey,"");
        xIds.add(xid);
        return R.ok().put("x_ids",xIds);
    }

    private R saveEntity(TbSiteCachePrefetchEntity entity, String mode){
        if (StringUtils.isBlank(entity.getSiteServerName()) || StringUtils.isBlank(entity.getPfPath())){
            return R.error("No site server");
        }
        TbSiteCachePrefetchEntity e0= tbSiteCachePrefetchDao.selectOne(new QueryWrapper<TbSiteCachePrefetchEntity>()
                .eq("site_server_name",entity.getSiteServerName())
                .eq("pf_path",entity.getPfPath())
                .eq("frequency",entity.getFrequency())
                .select("id")
                .last("limit 1")
        );
        if (e0 != null){
            //already exist
            return R.error("already exist");
        }
        TbSiteEntity e1=  tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("main_server_name",entity.getSiteServerName()).select("user_id,main_server_name").last("limit 1"));
        if (null!=e1 ){
            if (null!=e1.getUserId()){
                entity.setUserId( new Long(e1.getUserId()).intValue());
                entity.setUserName(getUserName(entity.getUserId()));
            }
        }
        TbSiteAttrEntity e2 = tbSiteAttrDao.selectOne(new QueryWrapper<TbSiteAttrEntity>()
                .eq("pkey", SiteAttrEnum.ALIAS.getName())
                .eq("pvalue",entity.getSiteServerName())
                .last("limit 1")
        );
        if (null!=e2 ){
            if (0!=e2.getSiteId()){
                TbSiteEntity site=  tbSiteDao.selectOne(new QueryWrapper<TbSiteEntity>().eq("id",e2.getSiteId()).select("user_id,main_server_name").last("limit 1"));
                if (null!=site.getUserId()){
                    entity.setUserId( new Long(site.getUserId()).intValue());
                    entity.setUserName(getUserName(entity.getUserId()));
                }

            }
        }
        if (e1==null && e2==null){
            return R.error("not implemented site");
        }
        if ("add".equals(mode)){
            tbSiteCachePrefetchDao.insert(entity);
        }else {
            tbSiteCachePrefetchDao.updateById(entity);
        }
        return R.ok("success");
    }

    @Override
    public R saveCachePre(long userId, TbSiteCachePrefetchEntity entity) {
        ValidatorUtils.validateEntity(entity);
        if (null==entity   ){
            return R.error("param is empty!");
        }
        if (StringUtils.isBlank(entity.getPfPath())){
            entity.setPfPath("");
        }
        if (null==entity.getId() || 0==entity.getId()){
            //insert
            entity.setCreateTime( new Long(System.currentTimeMillis()/1000).intValue());
            if (StringUtils.isNotBlank(entity.getPfPaths())){
                for(String pfPath : entity.getPfPaths().split(",")){
                    //http(s)://domain.com/path/file.suffix
                    //domain.com/path/file.suffix
                    String nPfPath = pfPath.replaceAll("https?://","");
                    String nDomain = nPfPath.replaceAll("/.*",""); //domain.com
                    if (StringUtils.isNotBlank(nDomain)){
                        entity.setSiteServerName(nDomain);
                    }
                    String pfPathSuf = nPfPath.replaceAll(nDomain,"");
                    entity.setSiteServerName(nDomain);
                    entity.setPfPath(pfPathSuf);
                    saveEntity(entity,"add");
                }
            }else if (StringUtils.isNotBlank(entity.getPfPath())){
                String nPfPath = entity.getPfPath().replaceAll("https?://","");
                String nDomain = nPfPath.replaceAll("/.*",""); //domain.com
                String pfPathSuf = nPfPath.replaceAll(nDomain,"");
                if (StringUtils.isNotBlank(nDomain)){
                    entity.setSiteServerName(nDomain);
                }
                entity.setPfPath(pfPathSuf);
                saveEntity(entity,"add");
            }
        }else{
            //update
            if(StringUtils.isNotBlank(entity.getPfPath())){
                String nPfPath = entity.getPfPath().replaceAll("https?://","");
                String nDomain = nPfPath.replaceAll("/.*",""); //domain.com
                String pfPathSuf = nPfPath.replaceAll(nDomain,"");
                if (StringUtils.isNotBlank(nDomain)){
                    entity.setSiteServerName(nDomain);
                }
                entity.setPfPath(pfPathSuf);
            }
            saveEntity(entity,"save");
        }
        //推送
        return xAddPreFetcher();
    }

    @Override
    public R delCachePre(long userId, DeleteIdsForm idsForm) {
        ValidatorUtils.validateEntity(idsForm);
         for(String id : idsForm.getIds().split(",")){
             tbSiteCachePrefetchDao.delete(new QueryWrapper<TbSiteCachePrefetchEntity>()
                     .eq("id",id)
                     .eq(0!=userId,"user_id",userId)
             );
         }
        return  xAddPreFetcher();
    }


}
