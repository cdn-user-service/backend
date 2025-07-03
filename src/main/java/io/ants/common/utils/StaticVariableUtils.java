package io.ants.common.utils;

import io.ants.common.other.QuerySysAuth;
import io.ants.modules.sys.enums.SiteAttrEnum;
import io.ants.modules.sys.vo.SiteGroupMiniVo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StaticVariableUtils {

    public final static String JAR_VERSION = "3.1.0";

    public final static String demoIp = "121.62.17.154";
    public final static String[] ANTS_USER = { "zh2021", "byijian", "zh2022", "byuejie", "admin" };

    // 授权信息
    public static String authMasterIp = "127.0.0.1";
    public static Integer authEndTime = 0;
    public static String goods_code = "";
    // rewrite f_host ai_waf
    public static List<String> exclusive_modeList = new ArrayList<>();

    // 同步节点授权信息线程状态
    public static boolean syncNodeInfoThread = false;
    // 授权信息
    public static ConcurrentHashMap<String, Object> authInfoMap = new ConcurrentHashMap<>();
    // 授权的节点信息
    public static ConcurrentHashMap<String, Object> authNodeListMap = new ConcurrentHashMap<>();
    public static Set<String> NodeIpList = new HashSet<>();
    public static long synNodeIpTimeTemp = 0l;

    // redis 存master 公共KEY
    public static String masterWebSeverName = "";
    public static Integer MasterWebPort = 80;
    public static String MasterProtocol = "";
    public static final String ADD_NODE_PATH = "/sys/cdnsys/auth/node/addByNodeRequest";
    public static final String FEEDBACKS = "/sys/cdnsys/auth/nginx/conf/feedbacks";

    // check node input token
    public static String checkNodeInputToken = "";
    public static Map<String, Map<String, String>> nodeCheckMap = new HashMap<>();

    public static final Integer[] PUBLIC_FORBID_LISTEN_PORT = { 20, 21, 22, 23, 25, 53, 53, 69, 110, 8181 };
    public static final Integer[] STREAM_FORBID_BIND_PORT = { 80, 443 };
    public static final Integer[] SITE_FORBID_LISTEN_PORT = {};

    // mm_text 中只有一项配置的类型
    public static String[] onlyOneAttrGroup = {
            SiteAttrEnum.SSL_OTHER_CERT_PEM.getName(),
            SiteAttrEnum.SSL_OTHER_CERT_KEY.getName(),
    };
    public static ConcurrentHashMap<String, Object> zeroSslThread = new ConcurrentHashMap<>();

    // 订单后缀
    public static Integer createOrderIndex = 1;
    // 订单支付处理线程状态
    public static boolean inOperatePayRecord = false;

    // 推送配置线程状态
    public static boolean makeFileThread = false;
    // 推送序号
    public static Integer makeFileThreadIndex = 0;

    // 更新版本的全局变量
    public static boolean is_in_update = false;
    public static Long up_time = 0L;
    public static String up_name = "";
    public static boolean db_sync_status = false;

    // dns type
    @SuppressWarnings("AlibabaCommentsMustBeJavadocFormat")
    public static final String[] NGX_ERR_LOG_TYPES = { "debug", "info", "notice", "warn", "error", "crit" };

    // 待推送任务
    public static ConcurrentHashMap<String, Object> taskMap = new ConcurrentHashMap<>(1024);
    public static String curPushTaskName = "";

    // 推送失败
    public static ConcurrentHashMap<String, Object> pushFailTask = new ConcurrentHashMap<>();

    // 已推送任务记录 recordId ->clientIp | filePath | time | feedback
    // 最后一次推送的任务ID
    public static ConcurrentHashMap<String, String> lastSendXAddTaskStreamIdMap = new ConcurrentHashMap<>();;
    public static ConcurrentHashMap<String, Object> alreadyPushTaskRecordMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Object> pushCallBackMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Object> pushCallBackErrorMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Object> pushCallBackUserErrorMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Boolean> pushCallBackErrorThread = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ConcurrentHashMap> pushTaskStreamMap = new ConcurrentHashMap<String, ConcurrentHashMap>();
    public static ConcurrentHashMap<String, String> siteIdSerNumMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> streamIdSerNumMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> rewriteIdSerNumMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> serialNumberGroupsMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> clientAreaIdNameMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> clientIpIdGroupMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SiteGroupMiniVo> siteIdGroupNameMap = new ConcurrentHashMap<>();
    public static boolean taskTheadFlag = false;
    public static ConcurrentHashMap<String, String> cacheSiteIdWafWhiteIpv4FileMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> cacheSiteIdWafBlockIpv4FileMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> cacheSiteIdWafRegFileMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> cacheSiteIdWafRuleFileMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> cacheSiteIdConfFileMap = new ConcurrentHashMap<>();
    /*
     * 定时任务状态
     */
    public static boolean dispatchThread = false;
    public static Long dispatchTemp = 0L;
    public static Long dispatchLastModifyTemp = 0L;
    public static boolean checkNodeThread = false;
    // 使用流量计算
    public static boolean bytesThread = false;
    public static Long bytesTimeTemp = 0L;
    // 使用预付费任务
    public static boolean pre_paidThread = false;
    public static Long pre_paidTimeTemp = 0L;
    // public static boolean check_site_cname_flag=false;

    // 使用SSL_APPLY
    public static boolean sslThread = false;
    public static Long sslTimeTemp = 0L;
    // 已解封
    public static ConcurrentHashMap<String, Long> deleteNftMap = new ConcurrentHashMap();
    // ssl_apply_thread
    public static boolean apply_ssl_thread1_flag = false;
    public static boolean apply_ssl_thread2_flag = false;

    // 同步IP数据线程
    public static boolean sync_ip_data_handle = false;

    public final static int maxFeedBackInfoSize = 20;

    public static String minNodeVersion = "9999.9999";

    public static Map getStaticStaticVariable() {
        Map staticMap = new HashMap();
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        if (null != request) {
            if ("localhost".equals(request.getServerName())) {

            } else if ("127.0.0.1".equals(request.getServerName())) {

            }
        }

        staticMap.put("AuthMasterIp", StaticVariableUtils.authMasterIp);
        staticMap.put("MasterProtocol", StaticVariableUtils.MasterProtocol);
        staticMap.put("MasterWebPort", StaticVariableUtils.MasterWebPort);
        staticMap.put("MasterWebSeverName", StaticVariableUtils.masterWebSeverName);
        staticMap.put("jarVersion", StaticVariableUtils.JAR_VERSION);
        return staticMap;
    }

}
