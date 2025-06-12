package io.ants.modules.utils.service.fuiou;

import io.ants.modules.utils.config.fuiou.FuiouConfig;
import io.ants.modules.utils.factory.fuiou.FuiouFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;


/**
 * @author Administrator
 */
public class Utils {

    public static Map<String, String> paraFilter(Map<String, String> map) {

        Map<String, String> result = new HashMap<>();

        if (map == null || map.size() <= 0) {
            return result;
        }

        for (String key : map.keySet()) {
            String value = map.get(key);
            if ("sign".equalsIgnoreCase(key) || (key.length() >= 8 && "reserved".equalsIgnoreCase(key.substring(0, 8)))) {
                continue;
            }
            result.put(key, value);
        }

        return result;
    }

    public static String createLinkString(Map<String, String> map) {

        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        StringBuilder prestr = new StringBuilder();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = map.get(key);

            if (i == keys.size() - 1) {
                //拼接时，不包括最后一个&字符
                prestr.append(key).append("=").append(value);
            } else {
                prestr.append(key).append("=").append(value).append("&");
            }
        }

        return prestr.toString();
    }

    public static String getSign(Map<String, String> map) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, IOException {

        FuiouService  fuiouService = FuiouFactory.build();
        FuiouConfig fuiouConfig = fuiouService.getFuiouConfig();

        Map<String, String> mapNew = paraFilter(map);

        String preSignStr = createLinkString(mapNew);

        //System.out.println("==============================待签名字符串==============================\r\n" + preSignStr);

        String sign = Sign.sign(preSignStr, fuiouConfig.ins_private_key);

        sign = sign.replace("\r\n", "");

        //System.out.println("==============================签名字符串==============================\r\n" + sign);

        return sign;
    }

    public static Boolean verifySign(Map<String, String> map, String sign) throws Exception {

        Map<String, String> mapNew = paraFilter(map);

        String preSignStr = createLinkString(mapNew);

        return Sign.verify(preSignStr.getBytes(FuiouConfig.charset), FuiouConfig.FY_PUBLIC_KEY, sign);
    }

    public static Map<String,String> xmlStr2Map(String xmlStr){
        Map<String,String> map = new HashMap<>();
        Document doc;
        try {
            doc = DocumentHelper.parseText(xmlStr);
            Element resroot = doc.getRootElement();
            //noinspection rawtypes
            List children = resroot.elements();
            if(children != null && children.size() > 0) {
                for (Object o : children) {
                    Element child = (Element) o;
                    //   map.put(child.getName(), child.getTextTrim());//会将换行符转换成空格
                    map.put(child.getName(), child.getStringValue().trim());
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void main(String[] args) {
        String xmlStr = "<?xml version=\"1.0\" encoding=\"GBK\" standalone=\"yes\"?>\n" +
                "<xml>\n" +
                "    <sign>1\n2 3   4:5</sign>\n" +
                "</xml>";
        xmlStr2Map(xmlStr);
    }

}
