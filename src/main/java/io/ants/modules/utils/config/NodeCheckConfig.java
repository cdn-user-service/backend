package io.ants.modules.utils.config;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

@Data
public class NodeCheckConfig implements Serializable {
    //    {
    //	"DnsDispatchMode": "online",
    //	"ModeTimingFrequency": 300,
    //	"checkNodePortList": "80",
    //	"checkNodeFrequency": 30,
    //	"mailNotice": "165668@qq.com",
    //	"mailNoticeStatus": 1,
    //	"smsNotice": "15907289088",
    //	"smsNoticeStatus": 1
    //}

    private static final long serialVersionUID = 1L;

    private String checkNodePortList;

    @Min(value = 10)
    private Integer checkNodeFrequency;

    @Min(value = 1)
    @Max(value = 100)
    private int failPercent=100;


    private Integer mailNoticeStatus=0;
    private String mailNotice="";

    private Integer smsNoticeStatus=0;
    private String smsNotice="";

    private String DnsDispatchMode;//online|timing|random
    private String ModeTimingFrequency;
}
