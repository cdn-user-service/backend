package io.ants.modules.job.task;

import io.ants.common.utils.StaticVariableUtils;
import io.ants.modules.sys.service.CommonTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 节点调度
 */
@Component("dispatchdns")
public class DispatchDns implements ITask {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CommonTaskService commonTaskService;

    void dispatchHandle(){
        if(StaticVariableUtils.dispatchThread){
           return;
        }
        StaticVariableUtils.dispatchTemp=System.currentTimeMillis();
        commonTaskService.groupDnsRecordDispatch();
    }


    @Override
    public void run(String params) {
        if((System.currentTimeMillis()-StaticVariableUtils.dispatchTemp)<1*60*1000){
            logger.error("[task]DispatchDns task too fast! exit ,"+StaticVariableUtils.dispatchTemp);
            return;
        }
        dispatchHandle();
    }


}
