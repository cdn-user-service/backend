package io.ants.modules.job.task;

import io.ants.modules.sys.enums.CommandEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.service.CdnMakeFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 清理日志
 */
@Component("clean_log")
public class CleanNodeLogTask implements ITask{

    @Autowired
    private CdnMakeFileService cdnMakeFileService;

    @Override
    public void run(String params) {
        Map map=new HashMap();
        map.put(PushTypeEnum.COMMAND.getName(), CommandEnum.CLEAN_ERROR_LOG.getId());
        cdnMakeFileService.pushByInputInfo(map);
    }
}
