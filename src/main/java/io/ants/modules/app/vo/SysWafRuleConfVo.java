package io.ants.modules.app.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Data
public class SysWafRuleConfVo {

    // {"siteId":"4896","badRequest":1,"highLimit":1,"forbidSeal":1,"postCheck":0,"hotUrlCheck":0,"hotUrlCheckLow":0,"botCheck":0,"isReset":0}

    @NotNull
    private Integer siteId;

    private int badRequest = 0;

    private int highLimit = 0;

    private int forbidSeal = 0;

    private int postCheck = 0;

    private int hotUrlCheck = 0;

    private int hotUrlCheckLow = 0;

    private int botCheck = 0;

    private int isReset = 0;
    // 接口防护
    private int limitUrlRate = 0;

    private int randomCheck = 0;

    private int refererUrl = 0;

    // 随机请求
    private int randomReq = 0;

    // 低频请求
    private int lowLimit = 0;

    public static int getWafRuleTypeId(String key) {
        try {
            Class<?> clazz = SysWafRuleConfVo.class;
            int i = 0;
            for (Field field : clazz.getDeclaredFields()) {
                i++;
                // System.out.println(field.getName());
                if (field.getName().equals(key)) {
                    return i;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static List<String> getWafRuleName() {
        List<String> list = new ArrayList<String>();
        try {
            Class<?> clazz = SysWafRuleConfVo.class;
            for (Field field : clazz.getDeclaredFields()) {
                list.add(field.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void main(String[] args) {
        System.out.println(getWafRuleTypeId("badRequest"));
    }
}
