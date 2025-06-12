package io.ants.common.other;

import com.alibaba.fastjson.JSONObject;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.HttpRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class QuerySysAuth {

    private final static Logger logger = LoggerFactory.getLogger(QuerySysAuth.class);

    public static final String BASE_PATH="/api/";
    public static final String CHECK_NODE_INPUT_PATH =BASE_PATH+"sys/cdnsys/auth/check/import";
    public static final boolean USER_HAVE_ALL_PERMS=false;
    public static final boolean PLUS_1_FLAG=true;
    public static final boolean PLUS_2_FLAG=false;
    public static  String zeroSslAccessId="E4BsW17_eTDjldpoNASArw";
    public static  String zeroSslAccessPwd="4XhM742MlVhIed9fkohzuOiLKP437EDP7zNcjiEdM42AZ6dNo-aSkueWSr_xx4k3i-F9_pgNCCjym6jt4-fR0A";
    public static  int  defaultForceUrl=0;
    public static  final boolean SHOW_NODE_REMARK=true;
    public static final boolean SITE_HTTP2_FLAG=false;
    public static final boolean PPV2_FLAG=true;
    public static final boolean WS_MODULE_FLAG=true;
    public static final String AUTH_GOODS_CODE_HEAD="sd";
   //1=sys;2=source;3=site
    public static final int    NGX_DEFAULT_ERR_PAGE=2;
    public static final boolean FORBID_FREE_CONSUME_DEL_SITES_FLAG=true;
    public static final String  FORCED_SSL_MODE_3_STATUS_CODE="200";
    public static final Integer[] EXISTS_CONFIG_IDS={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};
    public static final boolean IS_OFFLINE_VERSION=false;
    public static final boolean IS_USE_CACHE_NODE=false;
    public static final boolean HtmlShowBotReLoad=false;

    public static boolean getShowOtherSysConf(){
        return true;
    }

    /**获取本地与远程的版本数据
     * @param goods
     * @param hash
     * @return
     */
    public static Map<String,Object> queryCdnModelNewestVersionData(String goods, String hash){
        Map<String,Object> map=new HashMap<>();
        String url=getNewestVersionInfoAddress("3",goods,hash);
        // logger.debug(url1);
        map.put("checktime",System.currentTimeMillis());
        map.put("local_version_date",null);
        map.put("remote",null);
        map.put("remote_version_date",null);
        String  data1=  HttpRequest.curlHttpGet(url);
        if (StringUtils.isNotBlank(data1)){
            JSONObject cdnModelInfoObj= DataTypeConversionUtil.string2Json(data1);
            if (cdnModelInfoObj.containsKey("query") && null!=cdnModelInfoObj.get("query")){
                JSONObject queryObj=cdnModelInfoObj.getJSONObject("query");
                if (queryObj.containsKey("version_date")){
                    map.put("local_version_date",queryObj.getString("version_date"));
                }
            }
            if(cdnModelInfoObj.containsKey("newest") && null!=cdnModelInfoObj.get("newest")){
                JSONObject newestObj=cdnModelInfoObj.getJSONObject("newest");
                if (newestObj.containsKey("hash") && newestObj.containsKey("version_date")){
                    map.put("remote",newestObj.getString("hash"));
                    map.put("remote_version_date",newestObj.getString("version_date"));
                }
            }
        }
        return  map;
    }

    public static void setZeroSslAccessId(String zeroSslAccessId) {
        QuerySysAuth.zeroSslAccessId = zeroSslAccessId;
    }

    public static void setZeroSslAccessPwd(String zeroSslAccessPwd) {
        QuerySysAuth.zeroSslAccessPwd = zeroSslAccessPwd;
    }


    public static String getNewestVersionInfoAddress(String product_type,String goods, String hash){
        //http://sign.cdn.com/api/update_log.php?p=/v2/public/index.php/product/docs/get_version_detail_by_hash&product_type=3&goods=db&hash=aa;
        //http://sign.cdn.com/api/update_log.php?p=newest&product_type=3&goods=db&hash=4b6d3af41ed73ff98afaa7f0c4ae0a64
        return QuerySysAuth.getUpdateLogAddress()+"?p=newest&product_type="+product_type+"&goods="+goods+"&hash="+hash;
    }

    public static String getDocList(String name){
        try{
            return  QuerySysAuth.getUpdateLogAddress()+"?p=cdn_doc";
            //return QuerySysAuth.getUpdateLogAddress()+"?p=/v2/public/index.php/product/docs/get_child_from_name&name="+ URLEncoder.encode(name, "UTF-8");
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";

    }

    public static String getUpdateInfoList(String product_type,String pagenum, String pagesize){
        // http://www.cdn.com/v2/public/index.php/product/docs/versionupdatelist?product_type=3");
        //http://sign.cdn.com/api/update_log.php?p=updateLog&product_type=3&pagenum=1&pagesize=10
        return QuerySysAuth.getUpdateLogAddress()+"?p=updateLog&product_type="+product_type+"&pagenum="+pagenum+"&pagesize="+pagesize;
    }

    public static  String getSyncIpAddress(String parentId){
        String url="http://sign.antsxdp.com/cdn22/update_log.php?p=/v2/public/index.php/product/docs/cloud_ip_remark";
        //String url=getUpdateLogAddress()+"?p=/v2/public/index.php/product/docs/cloud_ip_remark";
        if (StringUtils.isBlank(parentId)){
            return url;
        }
        return url+"&parentId"+parentId;
    }

    public static String getAuthAddress(){
        return "http://"+getSignAddress()+"/api/auth.php";
    }


    public static String getSignAddress(){
        return "updata.sudun.com";
    }

    public static String getUpdateLogAddress(){
        return "http://"+getSignAddress()+"/api/update_log.php";
    }

    public static String getTargetVersionInfo(String product_type, String goods, String hash){
        //http://sign.cdn.com/api/update_log.php?p=/v2/public/index.php/product/docs/get_release_version_data_for_hash&product_type=3&goods=db&hash=;
        //http://sign.cdn.com/api/update_log.php?p=target&product_type=3&goods=db&hash=4b6d3af41ed73ff98afaa7f0c4ae0a64
        return "http://"+getSignAddress()+"/api/update_log.php"+"?p=target&product_type="+product_type+"&goods="+goods+"&hash="+hash;
    }

    public static String getDownloadInstallNodeAddressCentos7(){
        return "http://"+getSignAddress()+"/cdn20/2.33/install.sh";
    }

    public static String getDownloadInstallNodeAddressWithUbuntu(){
        //http://updata.sudun.com/cdn20/3.4.2-ubuntu/install.sh
       return String.format("http://%s/cdn20/3.4.2-ubuntu/install.sh", getSignAddress());
    }

    public static String getDownloadInstallWithCentosBbr(){
        // http://updata.sudun.com/cdn20/bbr/install_centos_bbrp.sh
        return  String.format("wget -O install.sh http://%s/cdn20/bbr/install_centos_bbrp.sh && sh install.sh",getSignAddress());
    }
    public static String getDownloadInstallWithUbuntuBbr(){
        //  http://updata.sudun.com/cdn20/bbr/install_debian_bbrp.sh
        return String.format("wget -O install.sh http://%s/cdn20/bbr/install_debian_bbrp.sh && sh install.sh",getSignAddress());
    }

   public static String getInstallCertificateService(){
        return "wget -O install.sh http://download.antsxdp.com/install_certbot.sh && sh install.sh -m 123456@qq.com -i \"\" -k \"\"";
    }


    public static boolean useMainRegInfo() {
        return true;
    }


    public static String getSignAuthIp() {
        String domain = QuerySysAuth.getSignAddress();
        try {
            InetAddress address = InetAddress.getByName(domain);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    public static String getSysAuthInfoVo(){
        String url= QuerySysAuth.getAuthAddress();
        String param="func=QueryAccess";
        try{
            return QuerySysAuth.cdnSysAuthHttpRequest(0,url,param);

        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return "{}";
    }
    public  static String cdnSysAuthHttpRequest(Integer method, String url, String param){
        try{
            String res=null;
            if(0==method){
                logger.debug("------>"+url+"?"+param);
                res= HttpRequest.sendGet(url,param);
            }else if (1==method){
                res= HttpRequest.sendPost(url,param);
            }
            if (!StringUtils.isBlank(res)) {
                JSONObject jsonObject = DataTypeConversionUtil.string2Json(res);
                if (jsonObject.containsKey("code") && 1 == jsonObject.getInteger("code")) {
                    // logger.debug("--->"+jsonObject.getString("data"));
                    return jsonObject.getString("data");
                } else if (jsonObject.containsKey("code") && 0 == jsonObject.getInteger("code") && jsonObject.containsKey("message")) {
                    //return  R.error(jsonObject.getString("message"));
                    //logger.debug(jsonObject.getString("message"));
                     logger.debug(jsonObject.getString("message"));
                }
            } else {
                //logger.debug("res is empty");
                 logger.debug("res is empty");
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
