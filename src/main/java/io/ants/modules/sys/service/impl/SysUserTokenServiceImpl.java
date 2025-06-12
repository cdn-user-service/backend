/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有,侵权必究！
 */

package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.ants.common.utils.R;
import io.ants.modules.app.utils.JwtUtils;
import io.ants.modules.sys.dao.SysUserTokenDao;
import io.ants.modules.sys.entity.SysUserTokenEntity;
import io.ants.modules.sys.oauth2.TokenGenerator;
import io.ants.modules.sys.service.SysUserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


/**
 * @author Administrator
 */
@Service("sysUserTokenService")
public class SysUserTokenServiceImpl extends ServiceImpl<SysUserTokenDao, SysUserTokenEntity> implements SysUserTokenService {
	//1小时后过期
	//private final static int EXPIRE = 3600;

	@Autowired
	private JwtUtils jwtUtils;

	@Override
	public R createToken(long userId) {
		String token="";

		//当前时间
		Date now = new Date();
		//过期时间
		Date expireTime = new Date(now.getTime() + jwtUtils.getExpire() * 1000);

		//判断是否生成过token
		SysUserTokenEntity tokenEntity = this.getById(userId);
		if(tokenEntity == null){
			tokenEntity = new SysUserTokenEntity();
			tokenEntity.setUserId(userId);
			//生成一个token
			token = TokenGenerator.generateValue();
			tokenEntity.setToken(token);
			tokenEntity.setUpdateTime(now);
			tokenEntity.setExpireTime(expireTime);
			//保存token
			this.save(tokenEntity);
		}else{
			//tokenEntity.setToken(token);
			//存在token  取出旧token
			token=tokenEntity.getToken();
			tokenEntity.setUpdateTime(now);
			tokenEntity.setExpireTime(expireTime);

			//更新token
			this.updateById(tokenEntity);
		}

		R r = R.ok().put("token", token).put("expire", jwtUtils.getExpire());

		return r;
	}

	@Override
	public void logout(long userId) {
		//生成一个token
		String token = TokenGenerator.generateValue();

		//修改token
		SysUserTokenEntity tokenEntity = new SysUserTokenEntity();
		tokenEntity.setUserId(userId);
		tokenEntity.setToken(token);
		this.updateById(tokenEntity);
	}
}
