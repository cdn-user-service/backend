package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */

public enum PushSetEnum {

    NGINX_CONF(1,"PUB_SINGLE_SET","/home/local/nginx/conf/nginx.conf","add-node","-1","nginx.conf","/usr/ants/cdn-api/nginx-config/ip/{ip}/"),
    CACHE_CONF(2,"PUB_SINGLE_SET","/home/local/nginx/conf/cache.conf","add-node","-1","cache.conf","/usr/ants/cdn-api/nginx-config/ip/{ip}/"),

    HTTP_CONF(10,"PUB_CONF_SET","/home/local/nginx/conf/etc/http.conf","add","-1","http.conf","/usr/ants/cdn-api/nginx-config/etc/"),

    //VHOST_MODE_CONF("PUB_CONF_SET","/home/local/nginx/conf/etc/vhost_mode.conf","add","-1"),
    //VHOST_MODE_1_CONF("PUB_CONF_SET","/home/local/nginx/conf/etc/vhost_mode_1.conf","add","-1"),
    //VHOST_MODE_2_CONF("PUB_CONF_SET","/home/local/nginx/conf/etc/vhost_mode_2.conf","add","-1"),
    //VHOST_MODE_3_CONF("PUB_CONF_SET","/home/local/nginx/conf/etc/vhost_mode_3.conf","add","-1"),
    INDEX_HTML(11,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/index.html","add","-1","index.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_400_HTML(12,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/400.html","add","-1","400.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_403_HTML(13,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/403.html","add","-1","403.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_404_HTML(14,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/404.html","add","-1","404.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_410_HTML(15,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/410.html","add","-1","410.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_500_HTML(16,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/500.html","add","-1","500.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_502_HTML(17,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/502.html","add","-1","502.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_503_HTML(18,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/503.html","add","-1","503.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    ERR_504_HTML(19,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/504.html","add","-1","504.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    INDEX_SITE_SUIT_EXP_HTML(20,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/site_suit_exp.html","add","-1","site_suit_exp.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    PUB_WAF_TEMPLATE(21,"PUB_CONF_SET","/home/local/nginx/conf/etc/html/###file_name###.html","add","-1","{fn}.html","/usr/ants/cdn-api/nginx-config/etc/html/"),
    REG_HTTP(22,"PUB_CONF_SET","/home/local/nginx/conf/etc/reg_http","add","-1","reg_http","/usr/ants/cdn-api/nginx-config/etc/"),
    RULE_HTTP(23,"PUB_CONF_SET","/home/local/nginx/conf/etc/rule_http","add","-1","rule_http","/usr/ants/cdn-api/nginx-config/etc/"),
    PUB_INJ_WAF_REGX(24,"PUB_CONF_SET","/home/local/nginx/conf/etc/inj_waf_reg","add","-1","inj_waf_reg","/usr/ants/cdn-api/nginx-config/etc/"),


    REG_PUB_WAF_SELECT_ID(25,"PUB_WAF_SET","/home/local/nginx/conf/etc/reg_###pub_waf_select_id###","add","-1","reg_{fn}","/usr/ants/cdn-api/nginx-config/etc/"),
    RULE_PUB_WAF_SELECT_ID(26,"PUB_WAF_SET","/home/local/nginx/conf/etc/rule_###pub_waf_select_id###","add","-1","rule_{fn}","/usr/ants/cdn-api/nginx-config/etc/"),


    //NFT_SET("NFT","/etc/nftables/nftables.conf","pub_nft_push"),
    //NFT_SHORT_CC("NFT","long_cc",""),
    //NFT_LONG_CC_V6("NFT","long_cc_v6",""),
    //NFT_INTERNET("NFT","internet",""),

    HTTP_BLACK_IPV4(27,"ETC_WAF_IP_SET","/home/local/nginx/conf/etc/http_black_ipv4","add","-1","http_black_ipv4","/usr/ants/cdn-api/nginx-config/etc/"),
    HTTP_WHITE_IPV4(28,"ETC_WAF_IP_SET","/home/local/nginx/conf/etc/http_white_ipv4","add","-1","http_white_ipv4","/usr/ants/cdn-api/nginx-config/etc/"),
    HTTP_BLACK_IPV6(29,"ETC_WAF_IP_SET","/home/local/nginx/conf/etc/http_black_ipv6","add","-1","http_black_ipv6","/usr/ants/cdn-api/nginx-config/etc/"),
    HTTP_WHITE_IPV6(30,"ETC_WAF_IP_SET","/home/local/nginx/conf/etc/http_white_ipv6","add","-1","http_white_ipv6","/usr/ants/cdn-api/nginx-config/etc/"),

    //ai_model_catboost_bin
    AI_MODEL_CATBOOST_BIN(31,"AI_WAF_SET","/home/local/nginx/logs/ai_waf_model.bin","add","-1","ai_waf_model.bin","/usr/ants/cdn-api/nginx-config/group/etc/"),

    CERT_VERIFY_CONF(32,"PUB_CONF_SET","/home/local/nginx/conf/etc/cert_verify.conf","add","-1","cert_verify.conf","/usr/ants/cdn-api/nginx-config/etc/"),

    SITE_SSL_CRT(101,"SITE_###id###_SET","/home/local/nginx/conf/conf/ssl/ssl_###ssl_ids###.crt","add","x","{fn}.crt","/usr/ants/cdn-api/nginx-config/group/{gid}/ssl/"),
    SITE_SSL_KEY(102,"SITE_###id###_SET","/home/local/nginx/conf/conf/ssl/ssl_###ssl_ids###.key","add","x","{fn}.key","/usr/ants/cdn-api/nginx-config/group/{gid}/ssl/"),
    SITE_HTMLS(103,"SITE_###id###_SET","/home/local/nginx/conf/conf/html/###site_id###/###error_code###.html","add","x","{fc}.html","/usr/ants/cdn-api/nginx-config/group/{gid}/html/{fn}/"),
    SITE_WAF_REG(104,"SITE_###id###_SET","/home/local/nginx/conf/conf/waf/reg_###site_id_name###","add","x","reg_{fn}","/usr/ants/cdn-api/nginx-config/group/{gid}/waf/"),
    SITE_WAF_RULE(105,"SITE_###id###_SET","/home/local/nginx/conf/conf/waf/rule_###site_id_name###","add","x","rule_{fn}","/usr/ants/cdn-api/nginx-config/group/{gid}/waf/"),
    SITE_WAF_WHITE_IP(106,"SITE_###id###_SET","/home/local/nginx/conf/conf/waf/white_ip_###site_id_name###","add","x","white_ip_{fn}","/usr/ants/cdn-api/nginx-config/group/{gid}/waf/"),
    SITE_WAF_BLACK_IP(107,"SITE_###id###_SET","/home/local/nginx/conf/conf/waf/black_ip_###site_id_name###","add","x","black_ip_{fn}","/usr/ants/cdn-api/nginx-config/group/{gid}/waf/"),
    SITE_CONF(108,"SITE_###id###_SET","/home/local/nginx/conf/conf/site/###site_id_name###.conf","add","x","{fn}.conf","/usr/ants/cdn-api/nginx-config/group/{gid}/site/"),

    STREAM_CONF(109,"STREAM_###id###_SET","/home/local/nginx/conf/conf/forward/###sp_id###.conf","add","x","{fn}.conf","/usr/ants/cdn-api/nginx-config/group/{gid}/forward/"),

    //REWRITE_SSL_CRT("REWRITE_###id###_SET","/home/local/nginx/conf/conf/rewrite/###rewrite_id_name###.crt","add"),
    //REWRITE_SSL_KEY("REWRITE_###id###_SET","/home/local/nginx/conf/conf/rewrite/###rewrite_id_name###.key","add"),
    REWRITE_CONF(110,"REWRITE_###id###_SET","/home/local/nginx/conf/conf/rewrite/###rewrite_id_name###.conf","add","x","{fn}.conf","/usr/ants/cdn-api/nginx-config/group/{gid}/rewrite/"),



    ;
    private final  int id;
    private final String group ;
    private final String templatePath ;
    private final String pushMode;
    private final String pushGroupIds;
    private final String fileName;
    private final String localParentDirectory;
    PushSetEnum(int id,String group,String templatePath,String pushMode, String pushGroupIds,String fileName,String localParentDirectory){
        this.id=id;
        this.group=group;
        this.templatePath = templatePath;
        this.pushMode = pushMode;
        this.pushGroupIds=  pushGroupIds;
        this.fileName=fileName;
        this.localParentDirectory=localParentDirectory;
    }


    public String getGroup() {
        return group;
    }

    public String getTemplatePath() {
        return templatePath;
    }
    public String getPushGroupIds(){
        return this.pushGroupIds;
    }

    public String getPushMode() {
        return pushMode;
    }

    public Integer getId() {
        return id;
    }

    public String getLocalParentDirectory(){
        return localParentDirectory;
    }

    public String getFileName(){
        return fileName;
    }

    public static final  String ADD_NODE_PUSH ="add-node";
    public static  final String ADD_PUSH ="add";
    public static final  String  ADD_NOCHECK_PUSH="add-nocheck";

    public static List<String> getAllGroups(){
        List<String> list=new ArrayList<>();
        for (PushSetEnum item : PushSetEnum.values()) {
            if (!list.contains(item.getGroup())){
                list.add(item.getGroup());
            }
        }
        return list;
    }

    public static Map<String,PushSetEnum> getAllGroupsItem(){
        Map<String,PushSetEnum> res=new HashMap<>();
        for (PushSetEnum item : PushSetEnum.values()) {
            res.put(item.getGroup(),item);
        }
        return res;
    }

    public static  List<String> getTemplatePathsByGroup(String group){
        List<String> list=new ArrayList<>();
        for (PushSetEnum item : PushSetEnum.values()) {
            if (item.getGroup().equals(group)){
                list.add(item.getTemplatePath());
            }
        }
        return list;
    }


    public static List<Integer> getSetIdsByGroup(String group){
        List<Integer> list=new ArrayList<>();
        for (PushSetEnum item : PushSetEnum.values()) {
            if (item.getGroup().equals(group)){
                list.add(item.getId());
            }
        }
        return list;
    }

    public static  String getPushModeByGroup(String group){
        for (PushSetEnum item : PushSetEnum.values()) {
            if (item.getGroup().equals(group)){
               return  item.getPushMode();
            }
        }
        return "";

    }

    public static  String getPushModeByTpath(String tpath){
        for (PushSetEnum item : PushSetEnum.values()) {
            if (item.getTemplatePath().equals(tpath)){
                return  item.getPushMode();
            }
        }
        return "";

    }

    public static PushSetEnum getItemByPath(String path){
        for (PushSetEnum item : PushSetEnum.values()) {
            String tmPath=item.getTemplatePath();
            Integer rIndex=tmPath.indexOf("#");
            String sPath=tmPath;
            if (-1!=rIndex){
                sPath=tmPath.substring(0,rIndex);
                //System.out.println(sPath);
            }
            if (path.contains(sPath)){
                return item;
            }
        }
        return null;
    }


    public static PushSetEnum getItemById(int cid){
        for (PushSetEnum item : PushSetEnum.values()) {
             if (item.getId()==cid){
                return item;
            }
        }
        return null;
    }


}
