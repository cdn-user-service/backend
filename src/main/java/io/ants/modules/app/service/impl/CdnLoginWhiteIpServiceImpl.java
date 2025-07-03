package io.ants.modules.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.utils.IPUtils;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.dao.CdnLoginWhiteIpDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.CdnLoginWhiteIpEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.QueryLoginWhiteIpForm;
import io.ants.modules.app.service.CdnLoginWhiteIpService;
import io.ants.modules.sys.dao.SysUserDao;
import io.ants.modules.sys.entity.SysUserEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CdnLoginWhiteIpServiceImpl implements CdnLoginWhiteIpService {

    @Autowired
    private CdnLoginWhiteIpDao cdnLoginWhiteIpDao;

    @Autowired
    private TbUserDao tbUserDao;

    @Autowired
    private SysUserDao sysUserDao;

    @Override
    public R loginWhiteIPList(int type, long uid, QueryLoginWhiteIpForm form) {
        IPage<CdnLoginWhiteIpEntity> page = cdnLoginWhiteIpDao.selectPage(
                new Page<>(form.getPage(), form.getLimit()),
                new QueryWrapper<CdnLoginWhiteIpEntity>()
                        .eq("type", type)
                        .eq("uid", uid)
                        .like(StringUtils.isNotBlank(form.getIp()), "ip", form.getIp())
                        .orderByDesc("id")

        );

        return R.ok().put("data", new PageUtils(page));
    }

    @Override
    public R saveLoginWhiteIp(int type, long uid, CdnLoginWhiteIpEntity entity) {
        if (!IPUtils.isValidIPV4(entity.getIp())) {
            return R.error("IP 格式不正确");
        }
        entity.setType(type);
        entity.setUid(uid);
        if (null == entity.getId() || 0 == entity.getId()) {
            // insert
            Long count = cdnLoginWhiteIpDao.selectCount(new QueryWrapper<CdnLoginWhiteIpEntity>()
                    .eq("type", type)
                    .eq("uid", uid)
                    .eq("ip", entity.getIp()));
            if (count > 0) {
                return R.error("IP 已存在");
            }
            entity.setCreateTime(new Date());
            cdnLoginWhiteIpDao.insert(entity);
        } else {
            // update
            Long count = cdnLoginWhiteIpDao.selectCount(new QueryWrapper<CdnLoginWhiteIpEntity>()
                    .eq("type", type)
                    .eq("uid", uid)
                    .ne("id", entity.getId())
                    .eq("ip", entity.getIp()));
            if (count > 0) {
                return R.error("IP 已存在");
            }
            cdnLoginWhiteIpDao.updateById(entity);
        }
        return R.ok();
    }

    @Override
    public R deleteLoginWhiteIps(int type, long uid, String ids) {
        if (StringUtils.isBlank(ids)) {
            return R.error("请选择要删除的IP");
        }
        cdnLoginWhiteIpDao.delete(new QueryWrapper<CdnLoginWhiteIpEntity>()
                .eq("type", type)
                .eq("uid", uid)
                .in("id", StringUtils.split(ids, ",")));
        return R.ok();
    }

    @Override
    public R checkCanLoginInCurrentIp(int type, long uid, String ip) {
        if (type == UserTypeEnum.MANAGER_TYPE.getId()) {
            SysUserEntity sysUserEntity = sysUserDao.selectOne(new QueryWrapper<SysUserEntity>()
                    .eq("user_id", uid)
                    .select("white_ip_status"));
            if (null == sysUserEntity || 0 == sysUserEntity.getWhiteIpStatus()) {
                return R.ok();
            }
            Long count = cdnLoginWhiteIpDao.selectCount(new QueryWrapper<CdnLoginWhiteIpEntity>()
                    .eq("type", type)
                    .eq("uid", uid)
                    .eq("status", 1)
                    .eq("ip", ip));
            if (count > 0) {
                return R.ok();
            }
            return R.error("此 IP  限制");
        } else if (type == UserTypeEnum.USER_TYPE.getId()) {
            TbUserEntity userEntity = tbUserDao.selectOne(new QueryWrapper<TbUserEntity>()
                    .eq("user_id", uid)
                    .select("white_ip_status"));
            if (null == userEntity || 0 == userEntity.getWhiteIpStatus()) {
                return R.ok();
            }
            Long count = cdnLoginWhiteIpDao.selectCount(new QueryWrapper<CdnLoginWhiteIpEntity>()
                    .eq("type", type)
                    .eq("uid", uid)
                    .eq("status", 1)
                    .eq("ip", ip));
            if (count > 0) {
                return R.ok();
            }
            return R.error("此 IP  限制");
        }
        return R.error("ip or user error");
    }
}
