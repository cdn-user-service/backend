package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum WafOpEnum {
     RESULT_IGNORE("ignore",1,"0","忽略（指向下一个规则）"),
     RESULT_PASS("pass",100,"0","放行"),


     RESULT_SUSPICIOUS_PASS("suspicious_pass_log",200,"0","可疑，记录日志后放行|定参0"),
     RESULT_SUSPICIOUS_VERIFY_AUTO_RELOAD("verify_auto_reload",201,"0","可疑，无感验证|定参0"),
     RESULT_SUSPICIOUS_VERIFY_CLICK_NUM("verify_click_num",202,"0","可疑，点击数字验证|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_DIRECTIONAL("direct",203,"0","307重试|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_CHECK_TOKEN("verify_check_token",204,"0","可疑，API鉴权|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_CLICK("verify_click",205,"0","可疑，点击验证|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_SLIDE("verify_slide",206,"0","可疑，滑动验证|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_9GRID("verify_9grid",207,"0","可疑，9宫格验证|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_WAIT5S("verify_wait5s",208,"0","可疑，5秒验证|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_CODE("verify_code",209,"0","可疑，字母验证|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_CLICK_V2("verify_click_v2",210,"0","可疑，点击验证V2|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_CLICK_V2_STYLE2("verify_click_v2_style2",211,"0","可疑，点击验证V2_style2|定参0") ,
     RESULT_SUSPICIOUS_VERIFY_AUTO_RELOAD_V2("verify_auto_reload_v2",212,"0","可疑，无感验证V2|定参0") ,

     RESULT_FORBID_PASS("forbid_pass_log",300,"0","拦截，记录日志后放行|定参0") ,
     RESULT_FORBID_REWRITE_URL("rewrite_url",301,"302","rewrite_url"),
     RESULT_FORBID_RETURN_CODE("forbid_return_code",301,"400|403|404|500|502|503|504","拦截，返回自定义错误码|参数为错误返回码"),
     RESULT_FORBID_FORBID_REQUEST("forbid_forbid_request",302,"60|180|300|600","拦截，限制访问|参数为限制时长(分钟)"),
     RESULT_FORBID_NFX_BLOCK_ONE("forbid_nfx_block_one",303,"60","拦截，单节点拉黑|60分钟") ,
     RESULT_FORBID_NFX_BLOCK_ALL("forbid_nfx_block_all",304,"60","拦截，全节点拉黑|60分钟") ,


    ;
   final   private String key;
   final   private Integer id;
   final   private String param;
   final   private String remark;
   WafOpEnum(String key,Integer id,String param,String remark){
        this.key=key;
        this.id=id;
        this. param=param;
        this.remark=remark;
    }

    public String getKey() {
        return key;
    }

    public Integer getId() {
        return id;
    }

    public String getParam() {
        return param;
    }

    public String getRemark() {
        return remark;
    }

    public static Map getDetail(WafOpEnum item){
        Map v=new HashMap();
        v.put("key",item.getKey());
        v.put("mode",item.getId());
        v.put("param",item.getParam());
        v.put("remark",item.getRemark());
        return v;
    }

    public static String getKeyById(Integer id){
        for (WafOpEnum item:WafOpEnum.values()){
            if(item.getId().equals(id)){
                return  item.getKey();
            }
        }
        return "";
    }

    public static Integer getWafModeValueByKey(String key){
        for (WafOpEnum item:WafOpEnum.values()){
            if(item.getKey().equals(key)){
                return  item.getId();
            }
        }
        return WafOpEnum.RESULT_IGNORE.getId();
    }

    public static Map getAll(){
        Map map=new HashMap();
        for (WafOpEnum item:WafOpEnum.values()){
           map.put(item.getKey(),getDetail(item));
        }
        return map;
    }
}
