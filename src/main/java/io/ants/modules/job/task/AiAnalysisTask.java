package io.ants.modules.job.task;


import io.ants.common.utils.StaticVariableUtils;
import io.ants.modules.sys.enums.CommandEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.service.CdnMakeFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("ai_analysis")
public class AiAnalysisTask implements ITask{

    @Autowired
    private CdnMakeFileService cdnMakeFileService;

    @Override
    public void run(String params) {
        if (!StaticVariableUtils.exclusive_modeList.contains("ai_waf")){
            return;
        }
        Map pushMap=new HashMap(8);
        pushMap.put(PushTypeEnum.COMMAND.getName(), CommandEnum.AI_ANALYSIS.getId().toString());
        cdnMakeFileService.pushByInputInfo(pushMap);
    }
}
