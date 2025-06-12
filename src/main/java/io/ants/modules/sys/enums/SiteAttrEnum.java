package io.ants.modules.sys.enums;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.util.*;



public enum SiteAttrEnum {
    //String pattern = "^[*a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?$";
    //base 别名
    ALIAS("base","alias","m_text","*","别名","别名","", "","{\"pvalue1\":\"areaId\"}"),
    //SITE_VHOST_FILTER_MODE("base","site_vhost_filter_mode","int","*","流量采集级别(1,2,3)","流量采集级别","2","","{}"),
    SITE_ACCESS_LOG_MODE("base","access_log_mode","int","*","日志记录模式(0-7)","日志记录模式（off,all,1,10,100,1000,10000,100000）","5","","{}"),
    SITE_FLOW_RATIO("base","flow_ratio","text","*","流量系数","流量系数","1.0","","{}"),
    SITE_ACME_EXP("base","acme_exp","int","*","","","0","","{}"),

    //source  回源
    SOURCE_BASE_INFO("source","source_base_info","mm_text","*","源站基本设置（ 协议类型,监听端口,取源协议,均衡方式,回源设置,线路列表）","","","","{\"pvalue1\":\"listen\",\"pvalue2\":\"areaId\"}"),
    SOURCE_HOST("source","source_host","text","*","回源域名(为空为自身,有值为访问域名)","","$host", "","{}"),
    SOURCE_SNI("source","source_sni","text","*","回源 SNI(为空表示 关闭)","","$host", "","{}"),
    SOURCE_RANGE("source","slice","bool","*","Range 分片 回源配置","","","","{}"),
    SOURCE_PROXY_CONNECT_TIMEOUT("source","proxy_connect_timeout","int","*","与upstream server的连接超时时间（没有单位，最大不可以超过75s）","","15","","{}"),
    SOURCE_KEEP_LIVE("source","keepalive","int","*","与源保存缓存链接数量","","1","","{}"),
    SOURCE_KEEP_LIVE_TIMEOUT("source","keepalive_timeout","int","*","客户端连接在服务器端保持多久后退出","","60","","{}"),
    SOURCE_PROXY_READ_TIMEOUT("source","proxy_read_timeout","text","*","nginx会等待多长时间来获得请求的响应15s）","","60","","{}"),
    SOURCE_PROXY_SEND_TIMEOUT("source","proxy_send_timeout","text","*","发送请求给upstream服务器的超时时间15s","","15","","{}"),
    SOURCE_CLIENT_HEAD_TIMEOUT("source","client_header_timeout","int","*","设置读取客户端请求头数据的超时时间，如果超过这个时间客户端还没有发送完整的数据，服务器端将返回408错误，放置客户端利用http协议进行***","","15","","{}"),
    SOURCE_CLIENT_BODY_TIMEOUT("source","client_body_timeout","int","*","设置读取客户端请求主体的超时间","","15","","{}"),
    SOURCE_SEND_TIMEOUT("source","send_timeout","int","*","服务端等待客户端两次请求的间隔时间","","30","","{}"),
    PROXY_REDIRECT("source","proxy_redirect","text","*","Location重定向","修改 Location 头域中的 URL 协议，以确保客户端收到正确的重定向 URL","http:// $scheme://","","{}"),
    FOLLOW_30X("source","follow_30x","l_text","*","回源301/302跟随","回源301/302跟随","","","{}"),

    //performance 性能优化
    AW_U_LIMIT_MODE("performance","aw_u_limit_mode","int","*","ul优化方式","回源过滤：0=关闭；1=重试;4=wait","0","","{}"),
    AW_U_MAX_COUNT("performance","aw_u_max_count","int","*","ul-value","回源过滤:ul优化上限值","500","","{}"),
    DEFAULT_LOCATION_CACHE_RULE("performance","default_location_cache_rule","int","*","强制缓存时间(秒)","强制缓存时间（0为分关闭）","0","","{}"),
    PERFORMANCE_CACHE_TYPE("performance","cache_config","mm_text","*","缓存类型（文件类型，匹配内容，缓存时间，weight）","","","","{}"),
    PERFORMANCE_UN_CACHE_TYPE("performance","un_cache_config","mm_text","*","不缓存类型（文件类型，匹配内容，缓存时间，weight）","","","","{}"),
    PERFORMANCE_SSL_ON_OFF("performance","cache_http_https","bool","*","HTTP / HTTPS 缓存共用","","","","{}"),
    PERFORMANCE_CACHE_MIN_USES("performance","proxy_cache_min_uses","int","*","设置客户端请求发送的次数，当客户端向被代理服务器发送相同请求达到该指令设定的次数后，Nginx服务器才对该请求的响应数据做缓存。合理设置该值可以有效地降低硬盘上缓存数据的数量，并提高缓存的命中率。","","","","{}"),
    PERFORMANCE_CACHE_IGNORE_CONTROL_PRAGMA("performance","cache_control_pragma","bool","*","忽略请求头中 Cache-Control 和 Pragma","","","","{}"),
    PERFORMANCE_CACHE_PROXY_BUFFERING("performance","proxy_buffering","bool","*","是否缓冲响应数据","是否缓冲响应数据","on","","{}"),
    PERFORMANCE_CACHE_IGNORE_URL_PARAM("performance","cache_url_param","bool","*","缓存时忽略 URL 中的参数","","$request_uri","","{}"),
    PERFORMANCE_CACHE_IGNORE_URL_PARAM_ORDER("performance","cache_url_param_order","bool","*","缓存时忽略 URL 中的参数顺序","","","","{}"),
    PERFORMANCE_CACHE_FOLLOW("performance","cache_follow","bool","*","遵循源站响应头部缓存规则","","","","{}"),
    PERFORMANCE_CACHE_NO_CACHE("performance","proxy_no_cache","text","*","可以是一个或者都多个变量。当string的值不为空或者不为0时，不启用cache功能","","","","{}"),
    //PERFORMANCE_PROXY_STORE("performance","proxy_store","bool","*","[local]本地磁盘缓存来自被代理服务器的响应数据"),
    PERFORMANCE_CUSTOM_CACHE_KEY_PREFIX("performance","custom_cache_key_prefix","text","*","自定义缓存KEY前缀,如无特殊需求，请勿修改","自定义缓存KEY前缀","$site_id","","{}"),
    PERFORMANCE_PC_H5_CACHE("performance","pc_h5_cache_flag","bool","*","禁用h5页面独立缓存","禁用h5页面独立缓存","0","","{}"),
    PERFORMANCE_GZIP("performance","gzip","bool","*","智能压缩开关[on|off]" ,"","","","{}"),
    PERFORMANCE_GZIP_MIN_LENGTH("performance","gzip_min_length","text","*","压缩的最小文件，小于设置值的文件将不会压缩","" ,"","","{}"),
    PERFORMANCE_GZIP_COMP_LEVEL("performance","gzip_comp_level","int","*","压缩级别，1-9，数字越大压缩的越好，也越占用CPU时间" ,"","","","{}"),
    PERFORMANCE_GZIP_TYPES("performance","gzip_types","l_text","*","进行压缩的文件类型","","","","{}"),
    PERFORMANCE_GZIP_VARY("performance","gzip_vary","bool","*","是否在http header中添加Vary: Accept-Encoding，建议开启 " ,"","","","{}"),
    //PERFORMANCE_GZIP_DISABLE("performance","gzip_disable","bool","*","禁用IE 6  gzip gzip_disable \"MSIE [1-6]\\.\"",""),
    //PERFORMANCE_GZIP_BUFFERS("performance","gzip_buffers","bool","*","gzip_buffers 32 4k;",""),
    //PERFORMANCE_GZIP_HTTP_VERSION("performance","gzip_http_version","text","*","gzip_http_version 1.0|1.1;","",""),
    PERFORMANCE_AUTO_WEBP("performance","web_p","bool","*","WebP自适应","","","","{}"),
    PERFORMANCE_AUTO_PAGE_OPTIMIZATION("performance","page_optimization","bool","*","页面优化","","","","{}"),
    PERFORMANCE_SYN_LOAD("performance","syn_load","bool","*","异步加载","","off","","{}"),
    PERFORMANCE_FULL_LINK_OPTIMIZATION("performance","full_link_optimization","bool","*","全链路优化","","off","","{}"),
    PERFORMANCE_BROWSER_CACHE_OPTIMIZATION("performance","browser_cache_optimization","bool","*","浏览器缓存优化","","off","","{}"),
    //NETWORK_NEXT_UPSTREAM_STATUS("network","proxy_next_upstream_status","bool","*","","","0",""  ,"{}"),
    NETWORK_NEXT_UPSTREAM("performance","proxy_next_upstream","text","*","","","error timeout  http_503 http_504 non_idempotent","","{}"),
    NETWORK_NEXT_UPSTREAM_TRIES("performance","proxy_next_upstream_tries","int","*","遇错重试次数,0表示不限制","遇错重试次数","1","","{}"),
    SOURCE_PROXY_CACHE_LOCK("performance","proxy_cache_lock","bool","*","同一资源请求合并","支持同一资源请求合并，仅需向源站请求一次即可，减少回源通信消耗，优化访问，默认开","on","","{}"),


    //advanced 高级
    ADVANCED_CONF_SPIDER("advanced","spider","text","*","搜索引擎优化[IP列表]","","","","{}"),
    ADVANCED_CONF_OCSP("advanced","ocsp","bool","*","OCSP Stapling","","off","","{}"),
    ADVANCED_CONF_WEBSOCKET("advanced","websocket","bool","*","WebSocket 开启状态（0）","","0","","{}"),
    //ADVANCED_CONF_HTTP_DNS_HOST("advanced","f_host","bool","*","ip请求HOST(针对httP IP 访问WEBVIEW，不能设置WEBVIEW host头的请求,在User-Agent：|f-host|1.1.1.1:www.cdn.com,|","","off","","{}"),
    ADVANCED_WS_PROXY_READ_TIMEOUT("advanced","ws_proxy_read_timeout","int","*","WebSocket  超时时间）","","3600","","{}"),
    ADVANCED_CONF_HTTP_RESPONSE_HEADER("advanced","add_header","mm_text","*","自定义HTTP响应头","","","","{}"),
    ADVANCED_CONF_HTTP_REQUEST_HEADER("advanced","proxy_set_header","mm_text","*","自定义 HTTP 回源请求头","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_500_JUMP("advanced","error_page_500","l_text","*","500 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_502_JUMP("advanced","error_page_502","l_text","*","502/504 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_503_JUMP("advanced","error_page_503","l_text","*","503 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_504_JUMP("advanced","error_page_504","l_text","*","504 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_410_JUMP("advanced","error_page_410","l_text","*","410 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_404_JUMP("advanced","error_page_404","l_text","*","404 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_403_JUMP("advanced","error_page_403","l_text","*","403 页面定制","","","","{}"),
    ADVANCED_CONF_ERROR_PAGE_400_JUMP("advanced","error_page_400","l_text","*","400 页面定制","","","","{}"),
    ADVANCED_CONF_MORE_CLEAR_HEADERS("advanced","more_clear_headers","mm_text","*","清除多余响应头","清除多余响应头","","","{}"),
    ADVANCED_CONF_SERVER_USER_CUSTOM_INFO("advanced","server_user_custom_info","l_text","*","自定义配置项","","","","{}"),
    ADVANCED_CONF_ERROR_CODE_REWRITE("advanced","error_code_rewrite","mm_text","*","状态码改写","状态码改写","","","{}"),

    //content 内容安全
    CONTENT_SECURITY_SNAPSHOT("content","snapshot","bool","*","网站快照","","off","","{}"),
    CONTENT_SECURITY_SUB_FILTER_STATUS("content","sub_filter_status","bool","*","内容替换总开关","","off","","{}"),
    CONTENT_SECURITY_SUB_FILTER("content","sub_filter","m_text","*","内容替换","","","","{}"),
    //CONTENT_SECURITY_SUB_FILTER_LAST_MODIFIED("content","sub_filter_last_modified","bool","*","网页内替换后是否修改;默认为ON","",""),
    CONTENT_SECURITY_SUB_FILTER_ONCE("content","sub_filter_once","bool","*","内容替换一次默认只替换第一次匹配到的到字 符，如果是off，那么所有匹配到的字符都会被替换；默认ON","","off","","{}"),
    CONTENT_SECURITY_SUB_FILTER_TYPES("content","sub_filter_types","text","*","用于指定需要被替换的MIME类型,默认为“text/html”","","","","{}"),
    CONTENT_SECURITY_REWRITE("content","site_uri_rewrite","mm_text","*","站内uri重定向","站内uri重定向","","","{}"),
    CONTENT_SECURITY_OPTIONS_RETURN_200("content","options_return_200","bool","*","options Return 200","options Return 200","0","","{}"),

    // business 业务安全
    BUSINESS_SECURITY_ANTI_THEFT_CHAIN("business","anti_theft_chain","l_text","*","防盗链","","","","{}"),
    BUSINESS_SECURITY_AREA_SHIELDING("business","area_shielding","l_text","*","区域屏蔽","","","","{}"),
    BUSINESS_SECURITY_X_ROBOTS_TAG("business","x_robots_tag","int","*","禁止搜索引擎抓取","noindex标签告诉搜索引擎不要将此页面编入索引。nofollow 标签告诉搜索引擎不要跟随此页面上的链接","0","","{}"),
    BUSINESS_SECURITY_CODE_REQUESTS("business","forbid_code_requests","bool","*","禁止爬虫抓取","禁止爬虫抓取，默认关","0","","{}"),
    BUSINESS_SECURITY_ACCESS_CONTROL_ALLOW_ORIGIN("business","access_control_allow_origin","bool","*","允许跨域请求","允许跨域请求","0","","{}"),
    BUSINESS_SECURITY_REFERER_CHECK("business","referer_check","l_text","*","来源检测","来源检测","","","{}"),

    //network 线路与网络优化
    NETWORK_SECURITY_DDOS("network","ddos","bool","*","ddos","","off","","{}"),
    NETWORK_SECURITY_DNS("network","dns","bool","*","dns","","off","","{}"),
    NETWORK_ROUTE_OPTIMIZATION("network","route_optimization","bool","*","对IP回源线路自动优化","线路优化","off","","{}"),
    SITE_ATTR_IPV6("network","ipv6","bool","*","启用ipv6","启用ipv6","0","","{}"),
    SEARCH_ENGINES_DNS_SOURCE("network","search_engines_dns_source","bool","*","","搜索引擎回源","0","","{}"),
    PERFORMANCE_LIMIT_UPLOAD_SIZE("network","client_max_body_size","int","*","上传文件大小限制","","200","","{}"),
    PERFORMANCE_LIMIT_SPEED("network","limit_rate","int","*","限速（0为不限速）","限速（0为不限速）","0","","{}"),
    PERFORMANCE_MOBILE_JUMP("network","mobile_jump","text","*","移动端跳转","","","","{}"),

    //自定义DNS组
    NETWORK_CUSTOM_DNS("custom_dns","custom_dns","l_text","*","自定义DNS记录","自定义DNS记录","","","{}"),

    //ssl
    SSL_FORCED_HTTPS("ssl","forced_ssl","int","*","强制HTTPS","","0","","{}"),
    SSL_FORCED_HSTS("ssl","forced_hsts","bool","*","HSTS功能开关","","0","","{}"),
    SSL_OTHER_CERT_KEY("ssl","other_ssl_key","mm_text","*","上传证书 key","","","","{}"),
    SSL_OTHER_CERT_PEM("ssl","other_ssl_pem","mm_text","*","上传证书 pem","","","","{\"pvalue1\":\"end_time\"}"),
    SSL_PROTOCOLS("ssl","ssl_protocols","text","*","加密协议版本TLSv1.0-1.3","加密协议版本 ","TLSv1 TLSv1.1 TLSv1.2","","{}"),
    SSL_HTTP2("ssl","ssl_http2","bool","*","HTTPS HTTP2","","0","","{}"),
    SSL_CDN_APPLY_CERT("ssl","ssl_cdn_apply_cert","bool","*","申请证书方式","0=cdn申请，1=回源验证申请","0","","{}"),

    //waf
    WAF_STATUS("waf","ants_waf","bool","*","waf 开关 ","","on","","{}"),
    //WAF_RULE_PASS("waf","waf_rule_pass","mm_text","*","waf 规则（pass , 正则）","","","","{}"),
    //WAF_RULE_FORBID("waf","waf_rule_forbid","mm_text","*","waf 规则（ ,forbid 正则）","","","","{}"),
    //WAF_RULE_SUSPICIOUS("waf","waf_rule_suspicious","mm_text","*","waf 规则（ suspicious 正则）","","","","{}"),
    PRI_PRECISE_WAF_DETAILS("waf","pri_precise_waf_details","mm_text","*","自定义精准waf策略","自定义精准WAF","","","{\"pvalue1\":\"rule_source\",\"pvalue2\":\"rule_type\"}"),
    WAF_ACCESS_LOG("waf","access_log","bool","*","access 日志开关","","off","","{}"),
    PRI_PRECISE_WAF_SELECTS("waf","pri_precise_waf_selects","int","*","公共精准waf策略选择（自定义为0）","精准waf策略选择","","","{}"),
    PRI_PRECISE_WAF_USER_SELECTS("waf","pri_precise_waf_user_selects","int","*","用户归属下所有精准waf策略选择(为空或0为站点自身) ）","用户归属下所有精准waf策略选择","0","","{}"),
    WAF_CHECK_TOKEN_CONFIG("waf","ants_waf_token_set","text","*","鉴权：[模式1=url;2=head][超时时间(秒)][鉴权key][鉴权密钥[6个字符]]","鉴权","0 3600 null null","","{}"),
    WAF_SYS_RULE_CONFIG("waf","waf_sys_rule_config","l_text","*","系统内置规则配置","系统内置规则配置","","","{}"),
    WAF_REWRITE_URL("waf","rewrite_url","text","*","重定向地址","重定向地址http(s)开头","","","{}"),
    WAF_REFERER_VERIFY_URL("waf","referer_verify_url","text","*","来源控制referer","来源控制referer,多个用|分割","","","{}"),
    WAF_BOOT_CHECK_HTTP_STATUS_CODE("waf","bchsc","int","*","人机验证页面返回码","人机验证页面返回码","200","","{}"),

    //pub_waf 业务安全-
    //PUB_WAF_PASS_SELECTS("pub_waf","pub_waf_pass_selects","m_text","*","x公共waf策略选择集-PASS","公共WAF-PASS","","","{}"),
    //PUB_WAF_FORBID_SELECTS("pub_waf","pub_waf_forbid_selects","m_text","*","公共waf策略选择集合-FORBID","公共WAF-FORBID","","","{}"),
    //PUB_WAF_SUSPICIOUS_SELECTS("pub_waf","pub_waf_suspicious_selects","m_text","*","公共waf策略选择集合-suspicious","公共WAF-suspicious","","","{}"),
    PUB_WAF_WHITE_IP("pub_waf","server_waf_white_ip","mm_text","*","ip白名单","ip白名单","","","{}"),
    PUB_WAF_BLACK_IP("pub_waf","server_waf_black_ip","mm_text","*","ip黑名单","ip黑名单","","","{}"),
    INJ_PEN_WAF_SELECTS("pub_waf","inj_pen_selects","l_text","*","防注入渗透","防注入渗透选择","","","{}"),


    //pub_precise_waf
    PUB_PRECISE_WAF_SELECTS("pub_precise_waf","pub_precise_waf_selects","m_text","*","公共精准waf策略选择集","公共精准WAF策略集","","","{}"),

    //pri_precise_waf
    PRI_USER_PRECISE_WAF_SET("pri_precise_waf","pri_user_precise_waf_set","m_text","*","用户归属下所有精准waf策略集","用户归属下所有精准waf策略集","","","{}"),


    PRI_WAF_URL_STRINGS("pri_waf_uri","pri_waf_url_strings","l_text","*","uri频率的URL列表 ","uri频率的URL列表，空格分割","","","{}"),
    PRI_WAF_URL_CYCLE("pri_waf_uri","pri_waf_url_cycle","int","*","uri频率的周期","uri频率的周期","0","","{}"),

    //REGX_CHAIN
    //REGX_CHAIN("regx_chain","regx_chain","m_text","*","处理链","处理链","","{}"),

    JOB_SSL_APPLY("job","job_ssl_apply","int","*","自动续签SSL","自动续签SSL","0","","{\"pvalue1\":\"check_time\",\"pvalue2\":\"crt_end_time\"}"),
    JOB_CHECK_SITE_CNAME("job","check_site_cname","int","*","","","0","","{\"pvalue1\":\"check_time\"}"),


    CMD_CLEAN_CACHE("cmd","clean_cache","cmd","*","","","","","{}"),


    //CERT_VERIFY_MODE("cert_verify","cert_verify_mode","int","*","","","2","","{}"),
    CERT_VERIFY_ZERO_SSL_URI("cert_verify","cert_verify_zero_ssl_uri","text","*","","","pki-validation/rdm.txt","","{}"),
    CERT_VERIFY_ZERO_SSL_VALUE("cert_verify","cert_verify_zero_ssl_value","text","*","","","rdm\\ncomodoca.com\\nrdm","","{}"),

    LABEL_1("label","ssl_not_insert","text","*","","","0","","{}"),
    LABEL_2("label","no_need_push","text","*","","","0","","{}"),
    Label_3("label","siteId","text","*","*","","0","","{}"),
    Label_4("label","site_id","text","*","*","","0","","{}"),

    ;
   private  final   String  group;
   private  final   String name;
   private  final   String type;
   private  final   String versionPattern;
   private  final   String notes;
   private  final   String cn_name;
   private  final   String defaultValue;
   private  final   String rule;
   private  final   String additionalAttrJsonString;

    SiteAttrEnum(String  group,String name,String type,String versionPattern,String notes,String cn_name,String defaultValue,String rule,String additionalAttrJsonString){
        this.group=group;
        this.name=name;
        this.type=type;
        this.versionPattern=versionPattern;
        this.notes=notes;
        this.cn_name=cn_name;
        this.defaultValue=defaultValue;
        this.rule= rule;
        this.additionalAttrJsonString=additionalAttrJsonString;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getVersionPattern() {
        return versionPattern;
    }

    public String getNotes() {
        return notes;
    }

    public String getCn_name() {
        return cn_name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getRule() {
        return rule;
    }

    public String getAdditionalAttrJsonString(){
        return  additionalAttrJsonString;
    }

    public JSONObject getAdditionalAttrJsonStringObj(){
        if (StringUtils.isEmpty(additionalAttrJsonString)){
            return null;
        }
        return JSONObject.parseObject(additionalAttrJsonString);
    }



    public static  SiteAttrEnum getObjByName(String name){
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if(item.getName().equals(name)){
                return item;
            }
        }
        return null;
    }

    public static List<String> allGroup(){
        List<String> list=new ArrayList<>();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
          if(!list.contains(item.getGroup()) ){
              list.add(item.getGroup());
          }
        }
        return  list;
    }



    public static List<String> getAllErrorPage(){
        List<String> list=new ArrayList<>();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if(item.getName().contains("error_page")){
                list.add(item.getName());
            }
        }
        return  list;
    }

    public static List<String> allName(){
        List<String> list=new ArrayList<>();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if(!list.contains(item.getGroup()) ){
                list.add(item.getName());
            }
        }
        return  list;
    }



    public static Map<String,String[]>  getAllByGroupName(String group){
        Map<String,String[]> map=new HashMap();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if(item.getGroup().equals(group)){
                String[] attr={item.getGroup(),item.getType(),item.getVersionPattern(),item.getNotes()};
                map.put(item.getName(),attr);
            }
        }
        return map;
    }


    public static JSONObject getAdditionalByKey(String name){
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if(item.getName().equals(name)){
                return item.getAdditionalAttrJsonStringObj();
            }
        }
        return null;
    }

    public static  Map getAll(){
        Map map=new HashMap();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            String[] attr={item.getGroup(),item.getType(),item.getVersionPattern(),item.getNotes()};
            map.put(item.getName(),attr);
        }
        return map;
    }

    public static String getTypeByName(String name){
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if (name.equals(item.getName())){
                return item.getType();
            }
        }
        return "";
    }

    public static List<String> getAllErrorCode(){
        List<String> ls=new ArrayList<>(64);
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if (item.getName().startsWith("error_page_")){
                String errCode=item.getName().replace("error_page_","");
                ls.add(errCode);
            }
        }
        return ls;
    }

    public static int  getDbType(SiteAttrEnum item){
        if (item.getType().equals("bool")){
            return 1;
        }else if(item.getType().equals("int")){
            return 1;
        } else if(item.getType().equals("text")){
            return 1;
        } else if(item.getType().equals("m_text")){
            return 1;
        } else if(item.getType().equals("l_text")){
            return 2;
        } else if(item.getType().equals("mm_text")){
            return 2;
        }
        return 1;
    }

    public static Map getAllKeyDefault(){
        Map map=new HashMap();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if (item.getGroup().equals("label")){
                continue;
            }
            map.put(item.getName(),item.getDefaultValue());
        }
        return map;
    }

    public static String getKeyDefaultValue(String key){

        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if (item.getName().equals(key)){
                return  item.getDefaultValue();
            }
        }
        return "";
    }

    public static  Map getAllTypeGroup(){
        final String[] t1={"bool","int","text","m_text"};
        final String[] t2={"l_text", "mm_text"};
        Map map=new HashMap();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            String type=item.getType();
            if (!map.containsKey(type)){
                if (Arrays.asList(t1).contains(type)){
                    map.put(type,1);
                }else if(Arrays.asList(t2).contains(type)){
                    map.put(type,2);
                }else {
                    map.put(type,0);
                }
            }
        }
        return map;
    }

    public static List<String> getAllKeyByType(String[] types){
        List<String>list=new ArrayList<>();
        for (SiteAttrEnum item:SiteAttrEnum.values()){
            if (Arrays.asList(types).contains(item.getType())){
                list.add(item.getName());
            }
        }
        return list;
    }

}
