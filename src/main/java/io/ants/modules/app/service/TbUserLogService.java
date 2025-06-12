package io.ants.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.modules.app.entity.TbUserLogEntity;

import java.util.Date;

public interface TbUserLogService extends IService<TbUserLogEntity> {

    void frontUserWriteLog(Long userId, Integer logType, String method, String params);

    PageUtils getLogPages(Long userId, Integer page, Integer limit, String key, Date s_date, Date e_date, Integer logType, String paramsKey);

    TbUserLogEntity getLastLogin(Long userId);
}
