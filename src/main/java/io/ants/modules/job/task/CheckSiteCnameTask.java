package io.ants.modules.job.task;

import io.ants.modules.sys.service.CommonTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("check_site_cname")
public class CheckSiteCnameTask implements ITask{

    @Autowired
    private CommonTaskService commonTaskService;

    @Override
    public void run(String params) {
        commonTaskService.checkSiteCname();
    }
}
