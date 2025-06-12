package io.ants.modules.job.task;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("pushtask")
public class PushTask implements ITask{

    private Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public void run(String params) {
        //logger.debug("x");
    }
}
