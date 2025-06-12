package io.ants.modules.sys.form;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SuitMonth95SpeedMode {

    Map<Integer, List<Long>>  DayKeySpeedMap;

    //范围内的带宽数据
    Map<Integer,Long> AllSpeedData;

    Map<Integer,Long> Near7DayAllSpeedData;

    List<Long> AvailableM95SpeedData;

    //当前有效天数
    int MonthAvailableDaySum;

    int  M95SpeedIndex;

    //峰值带宽
    long maxM95SpeedBytesData;

    //95计费的带宽
    long FinalM95SpeedBytesData;

    //总流量
    long totalFlow;



    public SuitMonth95SpeedMode(){
        DayKeySpeedMap=new HashMap<>();
        AllSpeedData=new HashMap<>();
        Near7DayAllSpeedData=new HashMap<>();
        AvailableM95SpeedData=new ArrayList<>();
        MonthAvailableDaySum=0;
        M95SpeedIndex=0;
        FinalM95SpeedBytesData=0L;
        totalFlow=0L;
        maxM95SpeedBytesData=0L;
    }
}
