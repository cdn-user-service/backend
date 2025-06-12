package io.ants.modules.sys.enums;


import java.util.LinkedHashMap;


public enum PreciseWafParamEnum {
        A00("ip","k00","text","ip"),
        A01("host","k01","text","host"),
        A02("uri","k02","text","uri"),
        A03("exten","k03","text","后缀"),
        A04("arg","k04","text","参数"),
        A05("user_agent","k05","text","user_agent"),
        A06("request_length","k06","text","请求字节数"),
        A07("geoip","k07","text","ip所在区域"),
        A08("referer","k08","text","referer"),
        A09("scheme","k09","text","请求协议"),
        A10("method","k10","text","请求方式"),

        c11("head_cookies_success_sum","k11","int","随机COOKIES验证成功数"),
        c12("verify_fail_sum","k12","int","人机验证失败数"),
        c13("ip_join_time","k13","int","ip首次请求距今时间"),
        c14("ip_total","k14","int","ip总请求"),
        c15("ip_1_sum","k15","int","ip近1分钟总请求"),
        c16_OLD_KEY("ip_5_sum","k16","int","ip近5分钟总请求"),
        c16("k_ip_hot_uri_1_sum","k16","int","ip近1分钟HOT URL总请求"),
        c17("ip_1m_404_sum","k17","int","ip返回404数量 "),
        c17_OLD_KEY("ip_404_sum","k17","int","ip返回404数量 "),
        c18("ip_cache_hit_sum","k18","int","ip缓存命中数量"),
        c19("ip_content_type_info","k19","bin","ip返回文件类型HASH信息"),
        c20("ip_method_type_info","k20","bin","ip请求类型HASH信息"),
        c21("server_total","k21","int","server请求总数"),
        c22("server_1_sum","k22","int","server近1分钟请求总数"),
        C23_OLD_KEY("server_5_sum","k23","int","server近5分钟请求总数"),
        c23("server_5_sum","k23","int","ip1分钟请求热U"),
        c24("ip_url_hash_info","k24","bin","ip Url种类HASH信息"),
        c25("ip_return_code","k25","int","ip 返回码"),
        c26("ip_out_size","k26","int","返回字节数"),
        c27("l_server_in_byte","k27","int","server_in_size"),
        c28("l_server_out_byte","k28","int","server_out_byte"),
        c29("ip_upstream_status","k29","int","回源状态"),
        c30("ip_1_m_400_sum","k30","int","IP1分钟返回400数量"),
        c30_OLD_KEY("ip_400_sum","k30","int","返回400数量"),
        c31("ip_timeout_sum","k31","int","IP超时次数"),
        c32("ip_uri_n_sum","k32","int","指定URI周期内次数"),
        k33("ip_cookie_names","k33","text","cookie_names"),
        k34("ip_referer_verify","k34","int","来源控制验证,1=通过"),
        k35("s_port","k35","int","请求端口"),
        k36("k_x_forwarded_for","k36","int","forwarded length"),
        k37("k_server_5s_sum","k37","int","server sum for 5 seconds"),
        k38("k_upstream_ip","k38","text","request upstream ip address"),
        k39("k_http_host","k39","text","request cname"),
        k40("k_cname_type_info","k40","bin","request cname hash sums"),
        k41("k_body_length","k41","int","request k_body_length"),
          ;
    private final String name;
    private final String antsWafKey;
    private final String valueType;
    private final String remark;




    PreciseWafParamEnum(String name, String antsWafKey, String valueType, String remark){
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

    public static String getValueTypeByName(String name){
        for (PreciseWafParamEnum item : values()) {
            if(item.getName().equals(name)){
                return item.getValueType();
            }
        }
        return "";
    }

    public static String getSearchKeyByName(String name){
        for (PreciseWafParamEnum item : values()) {
            if(item.getName().equals(name)){
                return item.getAntsWafKey();
            }
        }
        return "";
    }

    public static LinkedHashMap getTransLinkMap(){
        LinkedHashMap linkedHashMap=new LinkedHashMap();
        for (PreciseWafParamEnum item : values()) {
            linkedHashMap.put(item.getName(),item.getAntsWafKey());
        }
        return linkedHashMap;
    }

    public static LinkedHashMap getAllMap(){
        LinkedHashMap linkedHashMap=new LinkedHashMap();
        for (PreciseWafParamEnum item : values()) {
            String [] mapV={item.getValueType(),item.getRemark()};
            linkedHashMap.put(item.getName(),mapV);
        }
        return linkedHashMap;
    }
}
