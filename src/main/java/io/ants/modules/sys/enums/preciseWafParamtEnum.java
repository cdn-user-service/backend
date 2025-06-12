package io.ants.modules.sys.enums;


import java.util.LinkedHashMap;


public enum preciseWafParamtEnum {
        A00("time","b1","text","(当天)请求时间（24h制）"),
        A01("ip","b2","text",""),
        A02("url","b3","text",""),
        A03("area","b4","text","GEOIP 国家区域码， 大写"),
        A04("suffix","b5","text","后缀"),
        A05("method","b6","text","请求方式，大写"),
        B00("cookies","cookies","text",""),
        B01("user_agent","user_agent","text",""),
        B02("referer","referer","text",""),
        B03("args","args","text","参数"),
        B04("x_forwarded_for","X-Forwarded-For","text","代理头"),

        C01("http_version","k1","text","http版本"),
        C03("header_list_size","k2","int","请求头列数"),
        C04("header_size","k3","int","请求头大小"),
        C05("request_length","k4","int","请求长度"),
        C06("referer_length","k5","int",""),
        C07("cookies_sum","k6","int","cookies 个数"),
        C08("args_sum","k7","int","参数数量"),
        C09("host_length","k8","int",""),
        C10("label_exist_check_cookies","k10","int","存在cookies"),

        D00("week","k16","int","星期（0-6）"),

        E00("server_request_total","k18","int","当前站请求总数"),
        E01("server_request_1m_sum","k19","int","当前站近1分钟请求总数"),
        E02("server_request_1h_sum","k20","int","当前站近1小时请求总数"),
        E03("server_request_1d_sum","k21","int","当前站近1天请求总数"),
        E04("server_request_per_1d_sum","k22","int",""),

        E05("uri_request_total","k23","int","当前url请求总数"),
        E06("uri_request_1m_sum","k24","int","当前url近1分钟请求总数"),
        E07("uri_request_1h_sum","k25","int","当前url近1小时请求总数"),
        E08("uri_request_1d_sum","k26","int","当前url近1天请求总数"),
        E09("uri_request_per_1d_sum","k27","int",""),

        E10("ip_request_total","k28","int","当前IP请求总数"),
        E11("ip_request_1m_sum","k29","int","当前IP近1分钟请求总数"),
        E12("ip_request_1h_sum","k30","int","当前IP近1小时请求总数"),
        E13("ip_request_1d_sum","k31","int","当前IP近1天请求总数"),
        E14("ip_request_per_1d_sum","k32","int",""),
        E15("ip_history_long","k33","int","首次访问时间距今时间"),
        E16("ip_get_percentage","k34","int","get 万分比"),
        E17("ip_post_percentage","k35","int","post 万分比"),
        GROUP_EM("method_weight","get|post|head","object","GET|POST|head 万分比"),
        E18("ip_verification_fail_sum","k36","int","验证失败次数"),

        E19("type_0_percentage","k37","int","type0 万分比"),
        E20("type_1_percentage","k38","int","type1 万分比"),
        E21("type_2_percentage","k39","int","type2 万分比"),
        E22("type_3_percentage","k40","int","type3 万分比"),
        E23("type_4_percentage","k41","int","type4 万分比"),
        E24("type_5_percentage","k42","int","type5 万分比"),
        E25("type_6_percentage","k43","int","type6 万分比"),
        E26("type_7_percentage","k44","int","type7 万分比"),
        E27("type_8_percentage","k45","int","type8 万分比"),
        E28("type_9_percentage","k46","int","type9 万分比"),
        GROUP_E0("ants_weight","AS0|AS1|AS2|AS3|AS4|AS5|AS6|AS7|AS8|AS9","object","type0-type9 万分比"),

        E29("code_0_percentage_2xx","k47","int","2xx 万分比"),
        E30("code_1_percentage_3xx","k48","int","3xx 万分比"),
        E31("code_2_percentage_4xx","k49","int","4xx 万分比"),
        E32("code_3_percentage_5xx","k50","int","5xx 万分比"),
        GROUP_E1("return_code","2xx|3xx|4xx|5xx","object","返回码2XX|3XX|4XX|5XX万分比"),

        F00("all_record_request_use_times","k52","int","近x次请求用时总计"),
        F01("all_record_request_ip_sum","k53","int","近x次请求IP数"),
        F02("all_record_request_url_sum","k54","int","近x次请求URL数"),

        G00("ip_suspicious_pass_time_long","k55","int","验证通过距今时间（秒）"),
        G01("ip_ban_sum","k56","int","ip被禁次数"),
        G02("ip_head_percentage","k57","int","head万分比"),
        G03("last_request_to_current_time","k58","int","最后一次请求距今时间"),
    ;
    private final String name;
    private final String antsWafKey;
    private final String valueType;
    private final String remark;

    /*
//F01("ip_request_power","k54","int","当前ip在近x次请求占比"),
//F02("uri_request_power","k55","int","当前url在近x次请求占比"),
//F03("ip_uri_sum","k56","int","当前IP在近x次请求中的uri数量"),
//F04("uri_ip_sum","k57","int","当前uri在近x次请求中的ip的数量"),
//linkedHashMap.put("dayOfTime","k34","",""),
//linkedHashMap.put("ai_http_method","[k0-k15]","",""),
//linkedHashMap.put("ai_http_version","[k16-k20]","",""),

//       F05("history_return_2xx_code","k58","bool","当前url 2xx返回0|1"),
//       F06("history_return_3xx_code","k59","bool","当前url 3xx返回0|1"),
//       F07("history_return_4xx_code","k60","bool","当前url 4xx返回0|1"),
//       F08("history_return_5xx_code","k61","bool","当前url 5xx返回0|1"),
//        E29("code_0_percentage_2xx","K47","int","2xx 万分比"),
//        E30("code_1_percentage_3xx","K48","int","3xx 万分比"),
//        E31("code_2_percentage_4xx","K49","int","4xx 万分比"),
//        E32("code_3_percentage_5xx","K50","int","5xx 万分比"),
//       F09("history_ip_return_2xx_code_sum","k62","int","当前ip 2xx返回数量"),
//       F10("history_ip_return_3xx_code_sum","k63","int","当前ip 3xx返回数量"),
//       F11("history_ip_return_4xx_code_sum","k64","int","当前ip 4xx返回数量"),
//       F12("history_ip_return_5xx_code_sum","k65","int","当前ip 5xx返回数量"),
//       F13("history_ip_return_2xx_code_power","k66","int","当前ip 2xx返回比重"),
//       F14("history_ip_return_3xx_code_power","k67","int","当前ip 3xx返回比重"),
//       F15("history_ip_return_4xx_code_power","k68","int","当前ip 4xx返回比重"),
//       F16("history_ip_return_5xx_code_power","k69","int","当前ip 5xx返回比重"),
    */



    preciseWafParamtEnum(String name, String antsWafKey, String valueType, String remark){
        this.name=name;
        this.antsWafKey=antsWafKey;
        this.valueType=valueType;
        this.remark=remark;
    }

    public String getName() {
        return name;
    }

    public String getAntsWafKey() {
        return antsWafKey;
    }

    public String getValueType() {
        return valueType;
    }

    public String getRemark() {
        return remark;
    }

    public static String GetValueTypeByName(String name){
        for (preciseWafParamtEnum item : values()) {
            if(item.getName().equals(name)){
                return item.getValueType();
            }
        }
        return null;
    }

    public static LinkedHashMap getTransLinkMap(){
        LinkedHashMap linkedHashMap=new LinkedHashMap();
        for (preciseWafParamtEnum item : values()) {
            linkedHashMap.put(item.getName(),item.getAntsWafKey());
        }
        return linkedHashMap;
    }

    public static LinkedHashMap getAllMap(){
        LinkedHashMap linkedHashMap=new LinkedHashMap();
        for (preciseWafParamtEnum item : values()) {
            String [] mapV={item.getValueType(),item.getRemark()};
            linkedHashMap.put(item.getName(),mapV);
        }
        return linkedHashMap;
    }
}
