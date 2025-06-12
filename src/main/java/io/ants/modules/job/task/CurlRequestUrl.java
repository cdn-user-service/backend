package io.ants.modules.job.task;

import io.ants.common.utils.HttpRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("curlrequesturl")
public class CurlRequestUrl implements ITask {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private void taskHandle(String params){
        if(StringUtils.isNotBlank(params)){
            for (String url:params.split(",")){
                if(url.startsWith("http")){
                    String response= HttpRequest.curlHttpGet(url);
                    //logger.debug(response);
                }
            }
         }
    }

    @Override
    public void run(String params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                taskHandle(params);
            }
        }).start();
    }
}
