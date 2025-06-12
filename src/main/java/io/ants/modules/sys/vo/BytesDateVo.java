package io.ants.modules.sys.vo;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class BytesDateVo {
    private Integer mode;
    private Date start;
    private Date end;
    private Map<Integer,Long> data;
    private long data_total;
    private long data_top;
    private Integer data_3_available_day;
    private long data_4_average_max_value_bytes;
    private long data_4_95value_bytes;
    private long data_value;
    private int data_5_index;
    private int data_unit_price;
    private long data_top_speed;
    private int data_month_last_day;
    private float cur_paid_fee;
    private String errMsg;
}
