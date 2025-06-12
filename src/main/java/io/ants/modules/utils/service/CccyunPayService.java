package io.ants.modules.utils.service;

import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.HashUtils;
import io.ants.common.utils.HttpRequest;
import io.ants.common.utils.R;
import io.ants.modules.utils.config.cccpay.CccYunNotifyForm;
import io.ants.modules.utils.config.cccpay.CccYunPayForm;
import io.ants.modules.utils.config.cccpay.CccyunPayConf;
import io.ants.modules.utils.config.cccpay.CccYunSubmitPayForm;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.TreeMap;

public class CccyunPayService {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static CccyunPayConf conf=new CccyunPayConf();

    public CccyunPayService(CccyunPayConf conf){
        CccyunPayService.conf=conf;
    }

    public R pay(CccYunSubmitPayForm form){
        String eMsg="";
        try{
            CccYunPayForm cForm=new CccYunPayForm();
            cForm.setPid(conf.getPId());
            cForm.setType(form.getType());
            cForm.setOut_trade_no(form.getOut_trade_no());
            cForm.setNotify_url(conf.getNotify_url());
            cForm.setReturn_url(conf.getReturn_url());
            cForm.setName(form.getName());
            double number = form.getAmount()/100.00;
            DecimalFormat df = new DecimalFormat("#.00");
            String formattedNumber = df.format(number);
            cForm.setMoney(formattedNumber);
            cForm.setClientip(form.getClientip());
            cForm.setDevice("pc");
            cForm.setSign_type("MD5");
            Map<String,Object> map= DataTypeConversionUtil.entity2map(cForm);
            if (null==map){
                eMsg="entity2map fail";
                return R.error(eMsg);
            }else{
                cForm.setSign(this.getSign(map));
                map.put("sign",cForm.getSign());
            }
            String rUrl=String.format("%s/mapi.php",conf.getInterfaceAddress());
            StringBuilder postData=new StringBuilder();
            for (String key:map.keySet()){
                if (null!=map.get(key)){
                    postData.append(key);
                    postData.append("=");
                    postData.append(map.get(key).toString());
                    postData.append("&");
                }
            }
            int pLen=postData.length();
            if (pLen>1){
                String postD=postData.toString().substring(0,pLen-1);
                return HttpRequest.sendFormPostRequest(rUrl,postD);
            }
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


    public  R parseCallBack(CccYunNotifyForm form){
        Map<String,Object> map= DataTypeConversionUtil.entity2map(form);
        if (null==map){
            return R.error("form is empty!");
        }
        String cSign=this.getSign(map);
        if (!cSign.equalsIgnoreCase(form.getSign())){
            return R.error("签名错误:"+DataTypeConversionUtil.entity2jonsStr(form));
        }
        return R.ok();
    }



    private static String getSign( Map<String, Object>  map){
        //1、将发送或接收到的所有参数按照参数名ASCII码从小到大排序（a-z），sign、sign_type、和空值不参与签名！
        //
        //2、将排序后的参数拼接成URL键值对的格式，例如 a=b&c=d&e=f，参数值不要进行url编码。
        //
        //3、再将拼接好的字符串与商户密钥KEY进行MD5加密得出sign签名参数，sign = md5 ( a=b&c=d&e=f + KEY ) （注意：+ 为各语言的拼接符，不是字符！），md5结果为小写。
        //
        //4、具体签名与发起支付的示例代码可下载SDK查看。
        // 将HashMap转换为TreeMap，并根据键的ASCII码从小到大排序
        Map<String, Object> sortedMap = new TreeMap<>(map);
        StringBuilder sb=new StringBuilder();

        // 遍历打印排序后的键值对
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (entry.getKey().equals("sign") || entry.getKey().equals("sign_type")){
                continue;
            }else if ( null== entry.getValue() || StringUtils.isBlank(entry.getValue().toString()) ){
                continue;
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            sb.append("&");
        }
        int sLen=sb.toString().length();
        if (sLen>1){
            String srcStr=sb.toString().substring(0,sLen-1);
            srcStr+=conf.getSecret();
            return  HashUtils.md5ofString(srcStr);
        }
        return "";
    }

    public static void main(String[] args) {


    }
}
