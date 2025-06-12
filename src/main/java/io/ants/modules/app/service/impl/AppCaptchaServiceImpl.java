/**
 * Copyright (c)  rights reserved.
 *
 * 
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.app.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.code.kaptcha.Producer;
import io.ants.common.utils.DateUtils;
import io.ants.modules.app.service.AppCaptchaService;
import io.ants.modules.sys.dao.SysCaptchaDao;
import io.ants.modules.sys.entity.SysCaptchaEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Date;

/**
 * 验证码
 *
 * @author Mark sunlightcs@gmail.com
 */
@Service("appCaptchaService")
public class AppCaptchaServiceImpl extends ServiceImpl<SysCaptchaDao, SysCaptchaEntity> implements AppCaptchaService {
    @Autowired
    private Producer producer;

    @Override
    public BufferedImage getCaptcha(String uuid) {

        //生成文字验证码
        String code = producer.createText();
        SysCaptchaEntity captchaEntity = new SysCaptchaEntity();
        captchaEntity.setUuid(uuid);
        captchaEntity.setCode(code);
        //5分钟后过期
        captchaEntity.setExpireTime(DateUtils.addDateMinutes(new Date(), 5));
        if(0!=this.count(new QueryWrapper<SysCaptchaEntity>().eq("uuid",uuid))){
            return producer.createImage(code);
        }
        this.save(captchaEntity);
        return producer.createImage(code);
    }

    @Override
    public boolean validate(String uuid, String code) {

        SysCaptchaEntity captchaEntity = this.getOne(new QueryWrapper<SysCaptchaEntity>().eq("uuid", uuid).last("limit 1"));
        if(captchaEntity == null){
            return false;
        }

        //删除验证码
        this.removeById(uuid);

        if(captchaEntity.getCode().equalsIgnoreCase(code) && captchaEntity.getExpireTime().getTime() >= System.currentTimeMillis()){
            return true;
        }

        return false;
    }
}
