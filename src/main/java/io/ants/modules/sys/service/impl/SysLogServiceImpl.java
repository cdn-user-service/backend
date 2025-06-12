/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有,侵权必究！
 */

package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.ip2area.IPSeeker;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.PageUtils;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.sys.dao.SysLogDao;
import io.ants.modules.sys.entity.SysLogEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.form.QueryLogForm;
import io.ants.modules.sys.service.SysLogService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Administrator
 */
@Service("sysLogService")
public class SysLogServiceImpl extends ServiceImpl<SysLogDao, SysLogEntity> implements SysLogService {

    @Autowired
    private TbUserDao tbUserDao;


    private void autoDeleteData(){
        // 删除30天前的数据
        java.util.Date date = DateUtils.addDateMonths(new  java.util.Date(),-1);
        this.remove(new QueryWrapper<SysLogEntity>()
                .eq("log_type",5)
                .lt("create_date",date)
        );
    }

    @Override
    public PageUtils querySysLogPage(QueryLogForm form) {
        autoDeleteData();
        ValidatorUtils.validateEntity(form);
        if (StringUtils.isNotBlank(form.getUser())){
            List<TbUserEntity> uidEntityList= tbUserDao.selectList(new QueryWrapper<TbUserEntity>().like("username",form.getUser()).select("user_id"));
            List<Long> uLIds=uidEntityList.stream().map(q->q.getUserId()).collect(Collectors.toList());
            form.setUIds(uLIds);
        }
        if (StringUtils.isBlank(form.getStart_date()) ||  StringUtils.isBlank(form.getEnd_date())){
            form.setStart_date(null);
            form.setEnd_date(null);
        }
        IPage<SysLogEntity> page = this.page(
            new Page<>(form.getPage(),form.getLimit()),
            new QueryWrapper<SysLogEntity>()
                    .eq(null!=form.getUserType(),"user_type",form.getUserType())
                    .eq(null!=form.getLogType(),"log_type",form.getLogType())
                    .orderByDesc("id")
                    .like(StringUtils.isNotBlank(form.getIp()),"ip",form.getIp())
                    .eq(null!=form.getUserId(),"user_id",form.getUserId())
                    .in(null!=form.getUIds() && !form.getUIds().isEmpty(),"user_id",form.getUIds())
                    .between(null!=form.getStart_date(),"create_date", DateUtils.StringToFullDate(form.getStart_date()) ,DateUtils.StringToFullDate(form.getEnd_date()))
                    .like(StringUtils.isNotBlank(form.getUsername()),"username", form.getUsername())
                    .like(StringUtils.isNotBlank(form.getKey()),"operation", form.getKey())
        );
        page.getRecords().forEach(item->{
            item.setArea(IPSeeker.getIpAreaByNew(item.getIp()));
            if (item.getUserType().equals(UserTypeEnum.USER_TYPE.getId()) && StringUtils.isBlank(item.getUsername())){
                if (null!=item.getUserId()){
                    TbUserEntity userEntity=tbUserDao.selectById(item.getUserId());
                    if (null!=userEntity){
                        item.setUsername(userEntity.getUsername());
                    }
                }
            }
            if(StringUtils.isNotBlank(item.getMethod())){
                String[] ms=item.getMethod().split("/");
                if (ms.length>0){
                    item.setMethod(ms[ms.length-1]);
                }
            }
        });
        return new PageUtils(page);
    }

    @Override
    public void deleteLog(Long userId, String ids) {
        String[] ss=ids.split(",");
        this.remove(new QueryWrapper<SysLogEntity>().eq(null!=userId,"user_id",userId).in("id", Arrays.asList(ss)));
    }
}
