package io.ants.modules.sys.form;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SuitAverageMaxSpeedMode {
    Map<Integer,Long> AvailableMaxSpeedData;

    Map<Integer,Long> AllSpeedData;

    Map<Integer,Long> Near7DayAllSpeedData;

    int MonthAvailableDaySum;

    long  AvailableMaxSpeedSum;

    long FinalAvailableSpeedBytes;

    //总流量
    long totalFlow;

    //峰值带宽
    long topSpeed;

    public SuitAverageMaxSpeedMode(){
        AvailableMaxSpeedData=new HashMap<>();
        AllSpeedData=new HashMap<>();
        Near7DayAllSpeedData=new HashMap<>();
        MonthAvailableDaySum=0;
        AvailableMaxSpeedSum=0L;
        FinalAvailableSpeedBytes=0L;
        totalFlow=0L;
        topSpeed=0L;
    }
}
