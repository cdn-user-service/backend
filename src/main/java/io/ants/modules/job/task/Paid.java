package io.ants.modules.job.task;

import io.ants.common.utils.StaticVariableUtils;
import io.ants.modules.sys.service.CommonTaskService;
import io.ants.modules.sys.service.PayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 生成 计费 任务| 关停 到期套餐 | 关闭超出游量套餐相关服务
 */
@Component("paid")
public class Paid implements ITask {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CommonTaskService commonTaskService;
    @Autowired
    private PayService payService;

    private void paidHandle(){

        if(StaticVariableUtils.pre_paidThread){
            return;
        }
        commonTaskService.prePaidTask();
        payService.operateRecord();
        StaticVariableUtils.pre_paidTimeTemp=System.currentTimeMillis();

    }

    @Override
    public void run(String params) {
        long tm=System.currentTimeMillis()-StaticVariableUtils.bytesTimeTemp;
        if(tm<1*60*60*1000){
            logger.error("[task]paid task too fast,min is 3600 sec! tm:"+tm);
            return;
        }
        //
        new Thread(new Runnable() {
            @Override
            public void run() {
                paidHandle();
            }
        }).start();
    }
}
