package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Administrator
 */
public enum PushTypeEnum {

    ALL_FILE("all_file","null","推送所有文件"),
    ALL_FILE_2_NODE("all_file_2_node","clientId","推送所有文件到节点"),
    INIT_ALL_NODE("initAllNode","null","始始化所有节点"),
    INIT_NODE("init_node","clientId","初始化节点"),

    NODE_CUSTOM("node_custom","clientIps","单节点自定义配置更新"),

    PUSH_FILE_BY_MODEL("push_file_by_model","fileAbsPathS","根据模板路径集推送"),
    PUSH_SUIT_SERVICE("push_suit_service","SerialNumber","根据套餐的服务项目推送"),

    ALL_TASK("all_task","null","执行完成任务链"),
    SHOW_FEEDBACK("show_feedback","null","显示反馈信息"),


    PUBLIC_CHUNK("public_chunk","null","推送所有公共文件块【nginx.conf http.conf cache.conf nft b_w_ip etc ..】"),
    PUBLIC_NGINX_CONF("nginx_conf","null","推送 nginx.conf"),
    PUBLIC_HTTP_CONF("http_conf","null","推送 http.conf"),
    PUBLIC_CACHE_CONF("cache_conf","null","推送 cache.conf"),
    PUBLIC_NFT_WAF("nft_waf","null","推送nft"),
    PUBLIC_HTTP_DEFAULT_WAF("http_default_waf","null","推送默认站IP访问WAF规"),
    PUBLIC_PUB_WAF_SELECT("pub_waf_select","null","推送公共WAF"),
    PUBLIC_PUSH_ETC_VHOST_CONF("push_etc_vhost_conf","null","推送流量统计配置"),
    PUBLIC_PUB_ERR_PAGE_CONF("public_pub_err_page_conf","null","推送默认（err_page）页"),


    IP_TRIE("ip_trie","null","推送 white_ipv4|ipv6 black_ipv4|ipv6 to nginx"),
    IP_TRIE_AND_NFT("ip_trie_and_nft","null","推送 white_ipv4|ipv6 black_ipv4|ipv6 to nginx 和3层数据"),

    SITE_SELECT_CHUNK("site_select_chunk","siteIds","批量推送站点块（ site_reg site_rule site_ssl site_html site_conf）"),
    SITE_CHUNK("site_chunk","siteId","推送站点块 site_reg site_rule site_ssl site_html site_conf"),

    SITE_SSL("site_ssl","siteId","推送站 site_ssl"),
    SITE_WAF("site_waf","siteId","推送站 site_reg site_rule.conf"),
    SITE_CONF("site_conf","siteId","推送站 site_.conf"),
    SITE_HTML_FILE("site_html_file","siteId","推送site_html"),

    STREAM_CONF("stream_conf","streamProxyIds","推送stream_.conf "),

    PUSH_REWRITE_CONF("push_rewrite_conf","rewriteIds","推送URL转发"),

    COMMAND("command","index","执行指令"),
    COMMAND_CREATE_DIR("create_dir","dirPath","创建目录"),
    COMMAND_DELETE_DIR("delete_file","dirPath","删除目录"),
    //COMMAND_PURGE_CACHE("purge_cache","urls","清理缓存"),
    //COMMAND_PURGE_CACHE_V2("purge_cache_v2","jsonStr","清理缓存v2"),
    COMMAND_INIT_DEL_DIR("init_del_dir","clientId","推送删除节点conf/conf/配置"),


    APPLY_CERTIFICATE("apply_certificate","certifyIds","通过证书ID申请证书"),
    APPLY_CERTIFICATE_V2("apply_certificate_v2","siteIds","通过站点ID申请证书"),
    APPLY_CERTIFICATE_REISSUED("apply_certificate_reissued","siteIds","重新申请站点IDS证书"),


    //clean
    //CLEAN_SHORT_CC("clean_short_cc","key","根据条件释放临时封禁数据[:NODEIP:HOSTNAME:ATTACKiP:OP_TYPE:]"),
    CLEAN_CLOSE_SUIT_SERVICE("close_suit_service","SerialNumber","关闭套餐的服务项目"),
    CLEAN_DEL_SITE("delete_site","siteId","删除站点块"),
    CLEAN_DEL_STREAM_CONF("del_stream_conf","streamProxyId","删除推送stream_.conf "),
    CLEAN_DEL_REWRITE_CONF("delete_rewrite_conf","id","关闭URL 转发"),


    //stop
    CLEAN_STOP_SITE("stop_site","siteId","暂停站点块"),
    CLEAN_STOP_STREAM_CONF("stop_stream_conf","streamProxyId","暂停推送stream_.conf "),
    CLEAN_STOP_REWRITE_CONF("stop_rewrite_conf","id","暂停URL 转发"),

    SHELL_ANTS_CMD_TO_MAIN("shell_ants_cmd_1","","向主控发cdm"),
    SHELL_ANTS_CMD_TO_NODE("shell_ants_cmd_2","","向节点发cdm"),

    PUSH_INTERCEPT_IP_TO_ALL_NODE("push_intercept_ip_to_all_node","",""),
    RELEASE_INTERCEPT_IP("release_intercept_ip","",""),

    REBOOT_NODE("reboot_node","client_ips","重启节点"),

    NODE_SYS_WS_STATUS_ON("ws_status_on","ip","节点线路优化"),
    NODE_SYS_WS_STATUS_OFF("ws_status_off","ip","节点线路优化"),
    NODE_SYS_WS_SPECIAL_PORTS_RESET("ws_special_ports_reset","null","特殊端口重新设定"),

    NODE_RESTART_NGINX("node_restart_nginx","ip","重启NGINX"),

    AI_MODEL_PUSH("ai_model_push","null","ai模型推送"),
    ;
    private final String name;
    private final String param;
    private final  String  remark;

    PushTypeEnum( String name,String param ,String  remark){
        this.name=name;
        this.param=param;
        this.remark=remark;
    }

    public String getName() {
        return name;
    }

    public String getParam() {
        return param;
    }

    public String getRemark() {
        return remark;
    }

    public static PushTypeEnum getEnumByKey(String key){
        for (PushTypeEnum item:PushTypeEnum.values()){
            if (item.getName().equals(key)){
                return item;
            }
        }
        return null;
    }

    public static Map<String,String> getAll(){
        Map<String,String> map=new HashMap();
        for (PushTypeEnum item:PushTypeEnum.values()){
            map.put(item.getName(),item.getParam());
        }
        return map;
    }
}
