package io.ants.modules.job.task;


import io.ants.common.utils.StaticVariableUtils;
import io.ants.modules.sys.service.CommonTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 流量记录
 */
@Component("bytes")
public class Bytes implements ITask{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CommonTaskService commonTaskService;


    void bytesHandle(){
        if(StaticVariableUtils.bytesThread){
            logger.info("bytes thread ing");
            return;
        }
        commonTaskService.requestBytesRecordHandle();
    }


    @Override
    public void run(String params) {
        long tm=System.currentTimeMillis()-StaticVariableUtils.bytesTimeTemp;
        if(tm<1*60*60*1000){
            logger.error("[task]bytes task too fast ,need less 3600 sec ! tm:"+tm);
            return;
        }
        bytesHandle();


    }

}
