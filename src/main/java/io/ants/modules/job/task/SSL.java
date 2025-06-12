package io.ants.modules.job.task;

import io.ants.common.utils.StaticVariableUtils;
import io.ants.modules.sys.service.CommonTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 续签证书&申请证书
 */
@Component("ssl")
public class SSL implements ITask{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CommonTaskService commonTaskService;


    void applySslHandle(){
        if(StaticVariableUtils.sslThread){
            logger.info("ssl thread ing");
            return;
        }
        if ((System.currentTimeMillis()- StaticVariableUtils.sslTimeTemp)<(1*60*1000)){
            logger.info("ssl thread too fast!");
            return;
        }
        StaticVariableUtils.sslTimeTemp=System.currentTimeMillis();
        commonTaskService.applySslTaskHandle();
        StaticVariableUtils.sslThread=false;
    }


    @Override
    public void run(String params) {
        if((System.currentTimeMillis()-StaticVariableUtils.sslTimeTemp)<120*1000){
            //logger.debug("[task]ssl task too fast! exit");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                applySslHandle();
            }
        }).start();



    }

}
