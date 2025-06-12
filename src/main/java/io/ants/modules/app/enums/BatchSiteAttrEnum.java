package io.ants.modules.app.enums;

public enum BatchSiteAttrEnum {

    ACCESS_LOG_MODE("access_log_mode","access_log_mode","日志记录模式"),
    FORCED_SSL("forced_ssl","forced_ssl",""),
    FORCED_HSTS("forced_hsts","forced_hsts",""),
    SSL_HTTP2("ssl_http2","ssl_http2",""),
    SSL_PROTOCOLS("ssl_protocols","ssl_protocols",""),

    LISTEN_PORT("listen_port","source_base_info","监听端口"),
    SOURCE_IP("source_ip","source_base_info","回源ip"),
    SOURCE_PORT("source_port","source_base_info","回源端口"),
    //todo  源站设置其它属性
    SOURCE_HOST("source_host","source_host",""),
    SOURCE_SNI("source_sni","source_sni",""),
    SLICE("slice","slice",""),
    PROXY_CONNECT_TIMEOUT("proxy_connect_timeout","proxy_connect_timeout",""),
    KEEPALIVE("keepalive","keepalive",""),
    KEEPALIVE_TIMEOUT("keepalive_timeout","keepalive_timeout",""),
    PROXY_READ_TIMEOUT("proxy_read_timeout","proxy_read_timeout",""),
    PROXY_SEND_TIMEOUT("proxy_send_timeout","proxy_send_timeout",""),
    CLIENT_HEADER_TIMEOUT("client_header_timeout","client_header_timeout",""),
    CLIENT_BODY_TIMEOUT("client_body_timeout","client_body_timeout",""),
    SEND_TIMEOUT("send_timeout","send_timeout",""),
    PROXY_REDIRECT("proxy_redirect","proxy_redirect",""),


    CACHE_CONFIG("cache_config","cache_config","缓存设置"),
    UN_CACHE_CONFIG("un_cache_config","un_cache_config","不缓存设置"),
    DEFAULT_LOCATION_CACHE_RULE("default_location_cache_rule","default_location_cache_rule","强制缓存"),
    CUSTOM_CACHE_KEY_PREFIX("custom_cache_key_prefix","custom_cache_key_prefix",""),
    AW_U_LIMIT_MODE("aw_u_limit_mode","aw_u_limit_mode","回源过滤"),
    PROXY_BUFFERING("proxy_buffering","proxy_buffering",""),
    CLIENT_MAX_BODY_SIZE("client_max_body_size","client_max_body_size",""),
    LIMIT_RATE("limit_rate","limit_rate",""),
    MOBILE_JUMP("mobile_jump","mobile_jump",""),
    //todo 页面压缩

    OCSP("ocsp","ocsp",""),
    WEBSOCKET("websocket","websocket","WebSocket开关"),
    //todo 自定义HTTP回源请求头
    //todo 自定义HTTP响应头
    //todo 页面定制

    X_ROBOTS_TAG("x_robots_tag","x_robots_tag","搜索引擎爬虫限制"),
    ACCESS_CONTROL_ALLOW_ORIGIN("access_control_allow_origin","access_control_allow_origin",""),
    //todo 防盗链

    //todo 网站漏洞防御
    //todo IP白名单
    //todo IP黑名单


    PUB_WAF_SELECT("pub_waf_select","pri_precise_waf_selects","公共WAF选择"),

    ;
    private String key;

    private String siteKey;

    private String remark;



    BatchSiteAttrEnum(String key,String siteKey,String remark){
        this.key=key;
        this.siteKey=siteKey;
        this.remark=remark;
    }

    public String getKey() {
        return key;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public String getRemark() {
        return remark;
    }


    public static String getSitePkeyByKey(String key){
        for (BatchSiteAttrEnum item:BatchSiteAttrEnum.values()){
            if (item.getKey().equals(key)){
                return item.getSiteKey();
            }
        }
        return "";
    }
}
