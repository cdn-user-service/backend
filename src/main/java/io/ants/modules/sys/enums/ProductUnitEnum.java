package io.ants.modules.sys.enums;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public enum ProductUnitEnum {
    UNIT_1YEAR("y","年"),
    UNIT_1SEASON ("s","季"),
    UNIT_1Month("m","月"),
    UNIT_1DAY("d","天"),
    UNIT_1PER("p","次"),
    ;
    private final String name;
    private final  String remarks;
    ProductUnitEnum(String name, String remarks){
        this.name=name;
        this.remarks=remarks;
    }

    public String getName() {
        return name;
    }

    public String getRemarks() {
        return remarks;
    }

    public static Map<String,String> getAllType() {
        Map<String,String> map =new HashMap<>();
        for (ProductUnitEnum item : ProductUnitEnum.values()) {
            map.put(item.getName(),item.getRemarks());
        }
        return map;
    }


    /**
     * 计算套餐到期时间
     * @param unit
     * @param StartDate
     * @param sum
     * @return
     */
    public static Date getEndDate(String unit, Date StartDate, Integer sum){
        try{

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(StartDate);
            if(unit.equals(ProductUnitEnum.UNIT_1YEAR.getName())){
                //1y
                calendar.add(Calendar.YEAR,sum);
                Date end_time = calendar.getTime();
                return end_time;
            }else if(unit.equals(ProductUnitEnum.UNIT_1SEASON.getName())){
                //1s
                calendar.add(Calendar.MONTH,sum*3);
                Date end_time = calendar.getTime();
                return end_time;
            }else if(unit.equals(ProductUnitEnum.UNIT_1Month.getName())){
                //1m
                calendar.add(Calendar.MONTH,sum);
                Date end_time = calendar.getTime();
                return end_time;
            }else if(unit.equals(ProductUnitEnum.UNIT_1DAY.getName())){
                //1d
                calendar.add(Calendar.HOUR,sum*24);
                Date end_time = calendar.getTime();
                return end_time;
            }
        }catch (Exception e){
            //System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return StartDate;

    }

}
