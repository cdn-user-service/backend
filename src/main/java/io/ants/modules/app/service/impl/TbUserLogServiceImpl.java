package io.ants.modules.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.ip2area.IPSeeker;
import io.ants.common.utils.HttpContextUtils;
import io.ants.common.utils.IPUtils;
import io.ants.common.utils.PageUtils;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.dao.TbUserLogDao;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.entity.TbUserLogEntity;
import io.ants.modules.app.service.TbUserLogService;
import io.ants.modules.sys.enums.LogTypeEnum;
import io.ants.modules.sys.enums.UserTypeEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;


@Service
public class TbUserLogServiceImpl extends ServiceImpl<TbUserLogDao, TbUserLogEntity>  implements TbUserLogService {

    @Autowired
    private TbUserDao tbUserDao;

    @Override
    public void frontUserWriteLog(Long userId, Integer logType, String method, String params) {
        TbUserLogEntity logEntity=new TbUserLogEntity();
        logEntity.setUserType(UserTypeEnum.USER_TYPE.getId());
        logEntity.setUserId(userId);
        logEntity.setLogType(logType);
        if(method.length()>128){
            logEntity.setMethod(method.substring(0,127));
        }else {
            logEntity.setMethod(method);
        }
        if(params.length()>4096){
            logEntity.setParams(params.substring(0,4096));
        }else {
            logEntity.setParams(params);
        }

        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        //设置IP地址
        logEntity.setIp(IPUtils.getIpAddr(request));
        this.save(logEntity);
    }

    @Override
    public PageUtils getLogPages(Long userId, Integer page, Integer limit, String key, Date s_date, Date e_date, Integer logType, String paramsKey) {
        IPage<TbUserLogEntity> logPage = this.page(
                new Page<>(page,limit),
                new QueryWrapper<TbUserLogEntity>()
                        .orderByDesc("id")
                        .isNotNull("log_type")
                        .isNotNull("user_id")
                        .eq(null!=userId , "user_id", userId)
                        .eq(null!=logType,"log_type",logType)
                        .like(StringUtils.isNotBlank(key), "method", key)
                        .like(StringUtils.isNotBlank(paramsKey),"params",paramsKey)
                        .eq("is_delete",0)
                        .between(null!=s_date && null!=e_date,"create_date",s_date,e_date)
        );
        try{
            logPage.getRecords().forEach(item->{
                item.setArea(IPSeeker.getIpAreaByNew(item.getIp()));
                String method=item.getMethod();
                method=method.substring(method.lastIndexOf("/")+1);
                item.setMethod(method);
                if(null==userId){
                    TbUserEntity userEntity=tbUserDao.selectOne(new QueryWrapper<TbUserEntity>().eq("user_id",item.getUserId()).select("user_id,username,mail,mobile").last("limit 1"));
                    item.setUserInfo(userEntity);
                }

            });

        }catch(Exception e){
            e.printStackTrace();
        }
        return new PageUtils(logPage);
    }

    @Override
    public TbUserLogEntity getLastLogin(Long userId) {
        TbUserLogEntity ll=this.getOne(new QueryWrapper<TbUserLogEntity>()
                .isNotNull("user_type")
                .isNotNull("log_type")
                .isNotNull("user_id")
                .eq("user_type", UserTypeEnum.USER_TYPE.getId())
                .eq(null!=userId,"user_id",userId)
                .eq("log_type", LogTypeEnum.LOGIN_LOG.getId()   )
                .orderByDesc("create_date")
                .last("limit 1")
        );
        if(null!=ll){
            return  ll;
        }
        return new TbUserLogEntity();
    }


}
