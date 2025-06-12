/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.modules.sys.service;


import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.modules.sys.entity.SysLogEntity;
import io.ants.modules.sys.form.QueryLogForm;


/**
 * 系统日志
 *
 * @author Mark sunlightcs@gmail.com
 */
public interface SysLogService extends IService<SysLogEntity> {

    PageUtils querySysLogPage(QueryLogForm form);

    void deleteLog(Long userId,String ids);

}
