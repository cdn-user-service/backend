package io.ants.modules.sys.enums;

import org.apache.commons.lang.StringUtils;

import java.util.*;

public enum PublicEnum {
    //nginx
    WORKER_PROCESSES("nginx_conf","worker_processes","text","*","线程数","worker线程数量（默认auto）","auto",1),
    WORKER_CPU_AFFINITY("nginx_conf","worker_cpu_affinity","text","*","CPU绑定","线程与CPU绑定（默认auto）","auto",0),
    WORKER_RLIMIT_NOFILE("nginx_conf","worker_rlimit_nofile","int","*","打开文件数","worker进程最大打开文件数(默认65000)","65000",0),
    WORKER_SHUTDOWN_TIMEOUT("nginx_conf","worker_shutdown_timeout","text","*","","为正常关闭工作进程配置超时。当过期时，nginx将尝试关闭当前打开的所有连接，以便于关闭","60s",0),
    EVENT_WORKER_CONNECTIONS("nginx_conf","event_worker_connections","int","*","","单个进程能并发处理的最大连接数(默认65000)","65000",0),
    EVENT_WORKER_AIO_REQUESTS("nginx_conf","event_worker_aio_requests","int","*","","设置单个工作进程（worker process）的最大未完成异步 I/O 操作数（默认32）","32",0),
    ERROR_LOG_LEVEL("nginx_conf","error_log_level","text","*","","错误日志级别[info, notice, warn, error, crit]","error",1),
    HTTP_MODEL_FLAG("nginx_conf","http_model","bool","*","http模块","站点模块","1",1),
    STREAM_MODEL_FLAG("nginx_conf","stream_model","bool","*","stream模块","四层转发模块","1",1),

    //cache
    PROXY_BUFFER_SIZE("cache_conf","proxy_buffer_size","text","*","","代理请求缓存大小（默认64k）","64k",0),
    PROXY_BUFFERS("cache_conf","proxy_buffers","text","*","","代理的响应内容缓冲（默认32 32k）","32 32k",0),
    PROXY_BUSY_BUFFERS_SIZE("cache_conf","proxy_busy_buffers_size","text","*","","busy状态的缓冲（默认128k）","128k",0),
    //PROXY_BUFFERING("cache_conf","proxy_buffering","bool","*","是否缓冲响应数据","是否缓冲响应数据","0",0),
    PROXY_CACHE_METHODS("cache_conf","proxy_cache_methods","text","*","代理请求类型","指定请求类型可缓存，默认GET HEAD","GET HEAD",0),
    PROXY_HTTP_VERSION("cache_conf","proxy_http_version","text","*","","代理http版本（默认1.0）","1.0",0),
    PROXY_MAX_TEMP_FILE_SIZE("cache_conf","proxy_max_temp_file_size","text","*","","临时文件的的最大大小（默认1024m）,超过的文件不缓存","1024m",1),
    PROXY_CACHE_PATH_DIR("cache_conf","proxy_cache_path_dir","text","*","","临时文件存在目录(默认/data/cache)","/data/cache",1),
    PROXY_CACHE_DIR_LEVELS("cache_conf","proxy_cache_dir_levels","text","*","目录层级","目录层级，每级n个16进制","1:2",0),
    PROXY_CACHE_DIR_MAX_SIZE("cache_conf","proxy_cache_dir_max_size","text","*","缓存容量","缓存容量最大值","100G",1),
    PROXY_CACHE_PATH_ZONE("cache_conf","proxy_cache_path_zone","text","*","共享内存区容量","共享内存区容量","100m",1),
    PROXY_CACHE_INACTIVE("cache_conf","proxy_cache_inactive","text","*","缓存非活动时间限制","非活动对象会在达到指定的非活动时间后被删除，从而释放存储空间,(默认1d)","1d",0),

    //http
    CLIENT_MAX_BODY_SIZE("http_conf","client_max_body_size","text","*","","客户端请求服务器最大允许大小（默认200M）","200m",0),
    RESOLVER("http_conf","resolver","text","*","","域名解析地址（默认 223.5.5.5 8.8.8.8 valid=600 ipv6=off;）","114.114.114.114 8.8.8.8 valid=600 ipv6=off",0),
    RESOLVER_TIMEOUT("http_conf","resolver_timeout","text","*","","解析超时时间（默认5s）","5s",0),
    //_WAF_FORBID_DATA_BUFFER("http_conf","waf_forbid_data_buffer","text","*","","禁封数据缓存区(默认10m)","10m",0),
    //_WAF_EXPORT_MEMORY_BUFFER("http_conf","waf_export_memory_buffer","text","*","","导出数据缓存区(默认2m)","2m",0),
    KEEPALIVE_TIMEOUT("http_conf","keepalive_timeout","int","*","","空闲最大超时时间（默认60）","60",0),
    KEEPALIVE_REQUESTS("http_conf","keepalive_requests","int","*","","最大连接数（默认10000）","10000",0),
    SERVER_NAMES_HASH_MAX_SIZE("http_conf","server_names_hash_max_size","int","*","","存储server缓存区（默认512）","512",0),
    SERVER_NAMES_HASH_BUCKET_SIZE("http_conf","server_names_hash_bucket_size","int","*","","存储server bucket缓存区（默认512）","512",0),
    LARGE_CLIENT_HEADER_BUFFERS("http_conf","large_client_header_buffers","text","*","","请求行+请求头 缓冲大小（默认4 32k）","4 32k",0),
    CUSTOM_SERVER_HEAD("http_conf","custom_server_head","text","*","自定义server头","自定义server头","nginx",0),
    PROXY_HEADERS_HASH_BUCKET_SIZE("http_conf","proxy_headers_hash_bucket_size","int","*","代理头哈希表的大小","如果系统中有大量的代理头信息并且遇到了哈希冲突的问题，可尝试调整该值以改善性能","64",0),
    NGX_TM_OUT_SENSITIVITY("http_conf","tmout_sensitivity","int","*","握手攻击拉黑系数","握手攻击检测拉黑系数，0-100，默认80","80",0),
    NGX_TM_OUT_BLOCK_TIME("http_conf","tmout_time_m","int","*","握手攻击拉黑时长","握手攻击检测拉黑时长（分钟），默认60分钟","60",0),

    //http_head
    HTTP_RESPONSE_HEADER("http_head","add_header","m_object","*","自定义HTTP响应头","","",0),
    HTTP_REQUEST_HEADER("http_head","proxy_set_header","m_object","*","自定义 HTTP 回源请求头","","",0),


    //stream
    STREAM_PROXY_CONNECT_TIMEOUT("stream","proxy_connect_timeout","text","*","建立连接代理服务器时的超时时间","如果x秒内无法建立连接，则会返回“502 Bad Gateway”错误","60s",0),
    STREAM_PROXY_TIMEOUT("stream","proxy_timeout","text","*","设置与代理服务器之间的通信超时时间","proxy_timeout包括了与代理服务器建立连接的时间（即proxy_connect_timeout）以及请求、响应的时间","60s",0),

    //common
    COMMON_FORBID_PORT("common","forbid_port","text","*","禁用端口","禁用端口","22|5000|9001|6379|9200",0),
    COMMON_SPECIAL_PORTS("common","special_ports","text","*","特殊端口","特殊开放端口","80|443",0),
    //http_cdn
    //HTTP_cdn_BAD_REQUEST_400_FORBID("http_cdn_conf","cdn_bad_request_400_forbid","bool","*","畸形包过滤","畸形包过滤,对BAD REQUEST 400请求拦截3600秒","off",0),

    //http_waf_conf
    HTTP_DEFAULT_PASS_RULE("http_waf_conf","default_pass_rule","text","*","放行规则","默认站80放行处理规则","100 0",0),
    HTTP_DEFAULT_FORBID_RULE("http_waf_conf","default_forbid_rule","text","*","拦截规则","默认站80拦截处理规则","302 300",0),
    HTTP_DEFAULT_RULE("http_waf_conf","default_rule","m_text","*","拦截规则","默认站80规则【规则集】","",0),




    //error_page
    ERROR_400("error_page","error_400","text","*","","","",0),
    ERROR_403("error_page","error_403","text","*","","","",0),
    ERROR_404("error_page","error_404","text","*","","","",0),
    ERROR_410("error_page","error_410","text","*","","","",0),
    ERROR_500("error_page","error_500","text","*","","","",0),
    ERROR_502("error_page","error_502","text","*","","","",0),
    ERROR_503("error_page","error_503","text","*","","","",0),
    ERROR_504("error_page","error_504","text","*","","","",0),
    DEFAULT_INDEX_HTML("error_page","default_index_html","text","*","","默认页面","<!DOCTYPE html><html><head><meta content=\"text/html;charset=utf-8\" http-equiv=\"Content-Type\"><meta content=\"utf-8\" http-equiv=\"encoding\"><title>Welcome to OpenResty!</title><style>body{width:35em;margin:0 auto;font-family:Tahoma,Verdana,Arial,sans-serif}</style></head><body><h1>Welcome to OpenResty!</h1><p>If you see this page,the OpenResty web platform is successfully installed and working. Further configuration is required.</p><p>For online documentation and support please refer to our <a href=\"https://openresty.org/\">openresty.org</a>site<br/>Commercial support is available at <a href=\"https://openresty.com/\">openresty.com</a>.</p><p>We have articles on troubleshooting issues like <a href=\"https://blog.openresty.com/en/lua-cpu-flame-graph/?src=wb\">high CPU usage</a>and <a href=\"https://blog.openresty.com/en/how-or-alloc-mem/\">large memory usage</a>on <a href=\"https://blog.openresty.com/\">our official blog site</a>. <p><em>Thank you for flying <a href=\"https://openresty.org/\">OpenResty</a>.</em></p></body></html>",0),
    INDEX_SITE_SUIT_EXP_HTML("error_page","site_suit_exp_html","text","*","","套餐过期页面","<html><head><meta content=\"text/html;charset=utf-8\" http-equiv=\"Content-Type\"></head><body><h1 style=\"text-align: center;\">Service paused</h1></body></html>",0),


    //web_pub_waf[弃用]
    WEB_RULE_REGX("web_pub_waf","web_rule_regx","m_text","*","","","",0),
    WEB_RULE_REGX_DETAIL("web_pub_waf","web_rule_regx_detail","c_m_text","*","","","",0),

    //web_precise_waf 精准规则
    WEB_RULE_PRECISE("web_precise_waf","web_rule_precise","m_text","*","","","",0),
    WEB_RULE_PRECISE_DETAIL("web_precise_waf","web_rule_precise_detail","c_m_text","*","","","",0),

    //web_net_waf-->nft
    NET_WAF_PING("net_waf","net_waf_ping","bool","*","ping开关","ping开关","0",0),
    NET_WAF("net_waf","net_waf","m_text","*","网络防火墙","网络防火墙","",0),


    //ssl_server
    SSL_SERVER("ssl_server","ssl_server","text","*","证书来源地址","","8.8.8.8",0),

    //white-black-ip|host
    WHITE_IP("wb_ip_host","white_ip","m_text","*","全局IP白名单","支持IPV4和ipv6,支持点分十进制和冒号十六进制表示法和网段划分","121.62.18.146",0),
    BLACK_IP("wb_ip_host","black_ip","m_text","*","全局IP黑名单","支持IPV4和ipv6,支持点分十进制和冒号十六进制表示法和网段划分","",0),
    WHITE_HOST("wb_ip_host","white_host","m_text","*","全局host白名单","","www.cdn.com",0),
    BLACK_HOST("wb_ip_host","black_host","m_text","*","全局host黑名单","","",0),

    //elk
    ELK_CONFIG("elk","elk_config","text","*","elk配置","elk配置","",0),

    //dns_rewrite_conf
    DNS_REWRITE_CONF("dns_rewrite","dns_rewrite_conf","text","*","","","",0),

    NGX_WAF_VERIFY_TEMPLATE_AUTO_RELOAD_201("waf_verify_template","verify_201","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_CLICK_NUM_202("waf_verify_template","verify_202","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_CLICK_205("waf_verify_template","verify_205","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_SLIDE_206("waf_verify_template","verify_206","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_9GRID_207("waf_verify_template","verify_207","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_5S_208("waf_verify_template","verify_208","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_VERIFY_209("waf_verify_template","verify_209","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_CLICK_210("waf_verify_template","verify_210","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_CLICK_STYLE2_211("waf_verify_template","verify_211","text","*","","","",0),
    NGX_WAF_VERIFY_TEMPLATE_AUTO_RELOAD_212("waf_verify_template","verify_212","text","*","","","",0),


    CERT_APPLY_MODE("cert_conf","cert_apply_mode","int","*","证书申请模式","1=本地,2=证书服务器","2",0),
    CERT_APPLY_PROXY_PASS("cert_conf","cert_apply_proxy_pass","text","*","证书申请回源地址","","http://127.0.0.1:80",0),
    CERT_APPLY_CALLBACK_URL("cert_conf","cert_apply_callback_url","text","*","证书申请回调地址","","http://127.0.0.1:8080/sys/common/save/cert/callback",0),

    AUTO_PULL_CACHE("auto_pull_cache","auto_pull_cache","text","*","自动缓存预取","自动缓存预取","",0),
    ;
    private  final  String  group;
    private  final   String name;
    private  final   String type;
    private  final   String versionPattern;
    private  final   String cn_name;
    private  final   String notes;
    private  final   String defaultValue;
    private  final   Integer nodeConf;//节点可单独配置属性


    PublicEnum(String  group, String name, String type, String versionPattern, String cn_name, String notes, String defaultValue, Integer nodeConf){
        this.group=group;
        this.name=name;
        this.type=type;
        this.versionPattern=versionPattern;
        this.cn_name=cn_name;
        this.notes=notes;
        this.defaultValue = defaultValue;
        this.nodeConf=nodeConf;
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

    public String getCn_name() {
        return cn_name;
    }

    public String getNotes() {
        return notes;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Integer getNodeConf() {
        return nodeConf;
    }

    public static  PublicEnum getObjByName(String name){
        if (StringUtils.isBlank(name)){
            return null;
        }
        for (PublicEnum item:PublicEnum.values()){
            if(item.getName().equals(name)){
                return item;
            }
        }
        return null;
    }
    public static  String getObjDefByName(String name){
        for (PublicEnum item:PublicEnum.values()){
            if(item.getName().equals(name)){
                return item.getDefaultValue();
            }
        }
        return "";
    }

    public static  String getObjTypeByName(String name){
        for (PublicEnum item:PublicEnum.values()){
            if(item.getName().equals(name)){
                return item.getType();
            }
        }
        return null;
    }

    public static  String getObjGroupByName(String name){
        for (PublicEnum item:PublicEnum.values()){
            if(item.getName().equals(name)){
                return item.getGroup();
            }
        }
        return null;
    }


    public static List<String> allGroup(){
        List<String> list=new ArrayList<>();
        for (PublicEnum item:PublicEnum.values()){
            if(!list.contains(item.getGroup()) ){
                list.add(item.getGroup());
            }
        }
        return  list;
    }

    public static List<String> allName(){
        List<String> list=new ArrayList<>();
        for (PublicEnum item:PublicEnum.values()){
            if(!list.contains(item.getGroup()) ){
                list.add(item.getName());
            }
        }
        return  list;
    }

    public static Map getAllByGroupName(String group){
        Map map=new HashMap();
        for (PublicEnum item:PublicEnum.values()){
            if(item.getGroup().equals(group)){
                String[] attr={item.getGroup(),item.getType(),item.getVersionPattern(),item.getCn_name(), item.getNotes(),item.getDefaultValue()};
                map.put(item.getName(),attr);
            }
        }
        return map;
    }

    public static List<String> getAllNameByGroup(String group){
        List<String> list=new ArrayList<>();
        for (PublicEnum item:PublicEnum.values()){
            if(item.getGroup().equals(group)){
                list.add(item.getName());
            }
        }
        return list;
    }

    public static Map getNodeConfKeys(){
        Map map=new HashMap();
        for (PublicEnum item:PublicEnum.values()){
            if(1==item.getNodeConf()){
                String[] attr={item.getGroup(),item.getType(),item.getVersionPattern(),item.getNotes(),item.getDefaultValue()};
                map.put(item.getName(),attr);
            }
        }
        return map;
    }

    public static  Map getAll(){
        Map map=new HashMap();
        for (PublicEnum item:PublicEnum.values()){
            String[] attr={item.getGroup(),item.getType(),item.getVersionPattern(),item.getNotes()};
            map.put(item.getName(),attr);
        }
        return map;
    }

    public static Map<String,String> getAllKeyDefault(String groups){
        Map<String,String>res=new HashMap<>();
        List<String> groupList=Arrays.asList(groups.split(","));
        for (PublicEnum item:PublicEnum.values()){
            if (groupList.contains(item.getGroup())){
                res.put(item.getName(),item.getDefaultValue());
            }
        }

        return res;
    }
}
