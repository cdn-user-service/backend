package io.ants.modules.sys.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class TbOrderInitVo {
    //10{
    //	"price_obj": "{\"value\":20100,\"status\":1}",
    //	"buy_obj": "{\"type\":\"y\",\"sum\":4}",
    //	"product_obj": "{\"createtime\":1650535530000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"测试产品1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":20,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1}]\",\"serverGroupIds\":\"1\",\"weight\":2,\"id\":2,\"productType\":10,\"status\":1}"
    //}

    //11{
    //	"price_obj": "{\"value\":20100,\"status\":1}",
    //	"buy_obj": "{\"serialNumber\":\"1664822455107002\",\"sum\":1,\"type\":\"m\"}",
    //	"product_obj": "{\"createtime\":1662606012000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":20100,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":30100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":40100,\\\"status\\\":1}}\",\"name\":\"升级测试\",\"serverGroupIds\":\"1\",\"attrJson\":\"[{\\\"attr\\\":\\\"charging_mode\\\",\\\"name\\\":\\\"charging_mode\\\",\\\"value\\\":1,\\\"valueType\\\":\\\"select\\\"},{\\\"attr\\\":\\\"flow\\\",\\\"name\\\":\\\"flow\\\",\\\"valueType\\\":\\\"int\\\",\\\"unit\\\":\\\"G\\\",\\\"value\\\":220000},{\\\"attr\\\":\\\"bandwidth\\\",\\\"name\\\":\\\"bandwidth\\\",\\\"valueType\\\":\\\"price_int\\\",\\\"unit\\\":\\\"元/Mbps/月\\\",\\\"value\\\":2000},{\\\"attr\\\":\\\"ai_waf\\\",\\\"id\\\":27,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"AI WAF\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"defense\\\",\\\"id\\\":41,\\\"unit\\\":\\\"QPS\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"CC防御\\\",\\\"value\\\":100,\\\"hiddenUsed\\\":true},{\\\"attr\\\":\\\"public_waf\\\",\\\"id\\\":37,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"公共WAF\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"port_forwarding\\\",\\\"id\\\":36,\\\"unit\\\":\\\"个\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"端口转发\\\",\\\"value\\\":8},{\\\"attr\\\":\\\"site\\\",\\\"id\\\":35,\\\"unit\\\":\\\"个\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"站点\\\",\\\"value\\\":12},{\\\"attr\\\":\\\"sms\\\",\\\"id\\\":34,\\\"unit\\\":\\\"条\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"短信通知\\\",\\\"value\\\":102,\\\"hiddenUsed\\\":true},{\\\"attr\\\":\\\"monitor\\\",\\\"id\\\":33,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"流量监控\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"private_waf\\\",\\\"id\\\":32,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"专属WAF\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"live_data\\\",\\\"id\\\":30,\\\"unit\\\":\\\"\\\",\\\"superpositionMode\\\":2,\\\"valueType\\\":\\\"bool\\\",\\\"name\\\":\\\"实时数据\\\",\\\"value\\\":\\\"1\\\"},{\\\"attr\\\":\\\"dd_defense\\\",\\\"id\\\":42,\\\"unit\\\":\\\"GB\\\",\\\"superpositionMode\\\":1,\\\"valueType\\\":\\\"int\\\",\\\"name\\\":\\\"DDos防御\\\",\\\"value\\\":100}]\",\"weight\":2,\"id\":24,\"productType\":10,\"status\":1}",
    //	"type": "m"
    //}

    //12{
    //	"price_obj": "{\"value\":20100,\"status\":1}",
    //	"buy_obj": {
    //		"serialNumber": "1650954704735002",
    //		"startTime": 1650769152,
    //		"sum": 2,
    //		"type": "y"
    //	},
    //	"product_obj": "{\"createtime\":1650789712000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1,\\\"status\\\":0},\\\"s\\\":{\\\"value\\\":10100,\\\"status\\\":0},\\\"y\\\":{\\\"value\\\":20100,\\\"status\\\":1}}\",\"name\":\"加油包1\",\"attrJson\":\"[{\\\"id\\\":3,\\\"name\\\":\\\"defense\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":10,\\\"status\\\":1,\\\"weight\\\":2},{\\\"id\\\":2,\\\"name\\\":\\\"flow\\\",\\\"unit\\\":\\\"G\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":1001,\\\"status\\\":1,\\\"weight\\\":1},{\\\"id\\\":4,\\\"name\\\":\\\"public_waf\\\",\\\"unit\\\":\\\"\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"bool\\\",\\\"value\\\":\\\"1\\\",\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":1,\"id\":4,\"productType\":12,\"status\":1}"
    //}

    //13{
    //	"buy_obj": {
    //		"serialNumber": "1650954704735002",
    //		"sum": "1",
    //		"type": "y"
    //	},
    //	"price_obj": {
    //		"value": 20100,
    //		"status": 1
    //	},
    //	"end_date_time": 1777185114000,
    //	"product_obj": "{\"createtime\":1650873242000,\"productJson\":\"{\\\"m\\\":{\\\"value\\\":1000,\\\"status\\\":1},\\\"s\\\":{\\\"value\\\":20100,\\\"status\\\":1},\\\"y\\\":{\\\"value\\\":120100,\\\"status\\\":1}}\",\"name\":\"测试产品三\",\"attrJson\":\"[{\\\"id\\\":5,\\\"name\\\":\\\"live_data\\\",\\\"unit\\\":\\\"\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"bool\\\",\\\"value\\\":\\\"1\\\",\\\"status\\\":1,\\\"weight\\\":1},{\\\"id\\\":6,\\\"name\\\":\\\"site\\\",\\\"unit\\\":\\\"个\\\",\\\"transferMode\\\":null,\\\"valueType\\\":\\\"int\\\",\\\"value\\\":10,\\\"status\\\":1,\\\"weight\\\":1}]\",\"weight\":1,\"id\":5,\"productType\":10,\"status\":1}"
    //}



    //2{"ip":"171.44.123.135"}

    //30 {
    //	"date": "2022-8",
    //	"mode": 3,
    //	"name": "pre_month_95",
    //	"serial_number": "1662388058317002",
    //	"remark": "[2022-8]月95带宽系统月结扣费",
    //	"product_obj": {
    //		"createtime": 1658542736000,
    //		"productJson": "{\"m\":{\"value\":0,\"status\":1},\"s\":{\"value\":100,\"status\":1},\"y\":{\"value\":100,\"status\":1}}",
    //		"name": "月95带宽套餐",
    //		"serverGroupIds": "1",
    //		"attrJson": "[{\"attr\":\"charging_mode\",\"name\":\"charging_mode\",\"value\":3,\"valueType\":\"select\"},{\"attr\":\"flow\",\"name\":\"flow\",\"valueType\":\"int\",\"unit\":\"G\",\"value\":10000},{\"attr\":\"bandwidth\",\"name\":\"bandwidth\",\"valueType\":\"price_int\",\"unit\":\"元/Mbps/月\",\"value\":2000},{\"attr\":\"ai_waf\",\"id\":27,\"name\":\"AI WAF\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"defense\",\"id\":38,\"name\":\"防御\",\"valueType\":\"int\",\"unit\":\"G\",\"value\":100,\"hiddenUsed\":true},{\"attr\":\"public_waf\",\"id\":37,\"name\":\"WAF\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"port_forwarding\",\"id\":36,\"name\":\"端口转发\",\"valueType\":\"int\",\"unit\":\"个\",\"value\":5},{\"attr\":\"site\",\"id\":35,\"name\":\"站点\",\"valueType\":\"int\",\"unit\":\"个\",\"value\":10},{\"attr\":\"sms\",\"id\":34,\"name\":\"短信通知\",\"valueType\":\"int\",\"unit\":\"条/月\",\"value\":100,\"hiddenUsed\":true},{\"attr\":\"monitor\",\"id\":33,\"name\":\"流量监控\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"private_waf\",\"id\":32,\"name\":\"专属WAF\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"live_data\",\"id\":30,\"name\":\"实时数据\",\"valueType\":\"bool\",\"value\":\"1\"},{\"attr\":\"custom_dns\",\"id\":39,\"name\":\"自定义dns\",\"valueType\":\"bool\",\"value\":\"1\"}]",
    //		"weight": 1,
    //		"id": 16,
    //		"productType": 10,
    //		"status": 1
    //	}
    //}


    //1 {
    //	"mode": 2,
    //	"uid": "511002199507017210",
    //	"RequestId": "c64d0d89-5291-46e4-9bea-e2649fcee121",
    //	"bizcode": "{\"Url\":\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx1f7125112b74db52&redirect_uri=https%3A%2F%2Fopen.faceid.qq.com%2Fv1%2Fapi%2FgetCode%3FbizRedirect%3Dhttps%253A%252F%252Ffaceid.qq.com%252Fapi%252Fauth%252FgetOpenidAndSaveToken%253Ftoken%253D1C3E2027-6398-4A6F-99F0-DFDB3285A3E3&response_type=code&scope=snsapi_base&state=&component_appid=wx9802ee81e68d6dee#wechat_redirect\",\"BizToken\":\"1C3E2027-6398-4A6F-99F0-DFDB3285A3E3\",\"RequestId\":\"c64d0d89-5291-46e4-9bea-e2649fcee121\"}",
    //	"name": "易建",
    //	"BizToken": "1C3E2027-6398-4A6F-99F0-DFDB3285A3E3",
    //	"Url": "https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx1f7125112b74db52&redirect_uri=https%3A%2F%2Fopen.faceid.qq.com%2Fv1%2Fapi%2FgetCode%3FbizRedirect%3Dhttps%253A%252F%252Ffaceid.qq.com%252Fapi%252Fauth%252FgetOpenidAndSaveToken%253Ftoken%253D1C3E2027-6398-4A6F-99F0-DFDB3285A3E3&response_type=code&scope=snsapi_base&state=&component_appid=wx9802ee81e68d6dee#wechat_redirect"
    //}

    //1{
    //	"mode": 1,
    //	"certify_id": "e68a14d5fe17a1d7f424952b8bdf9c96",
    //	"uid": "511002199507017210",
    //	"certify_url": "https://openapi.alipay.com/gateway.do?alipay_sdk=alipay-sdk-java-dynamicVersionNo&app_id=2021002191694659&biz_content=%7B%22certify_id%22%3A%22e68a14d5fe17a1d7f424952b8bdf9c96%22%7D&charset=UTF-8&format=json&method=alipay.user.certify.open.certify&sign=JLdBOu%2Fq1dcbquPHUkNrditNjWsnSp1GEe9wPMtnhT5w9Yz%2Fwa7nIwuzL4tZ1wVYAJTxXN8KiEfddPvBA9OlHnSkz%2F47DP%2Fl%2FZU1lFLHmdwNYW2%2FAfsaiHryQCrsnG409qy1%2Ble5%2BE5ZGQ7BJFgH3rvMBB5tL6XCeIa4z06HpNGcQkqrpNvU0AUe8V3AB1lfBLFR9hEMLz%2ByTTS0XS9yJKeAP84kQ8%2FKlX2CfBD%2FSuLJ2n8TshdYMgJ8SfHdqKzvJEoQ1TwgvTwrXSrwVvvWo3eZG4tjui93su8314ArIMP7aDM2DryYN32XG%2Bgl2LU6VYFtWT3j7fvw3MKDH12Yhw%3D%3D&sign_type=RSA2&timestamp=2022-08-30+18%3A19%3A54&version=1.0",
    //	"name": "易建"
    //}

    private JSONObject price_obj;
    private JSONObject buy_obj;
    private JSONObject product_obj;
    private String type;
    private Long end_date_time;
    private String ip;
    private String date;
    private Integer mode;
    private String name;
    private String serial_number;
    private String remark;
    private String uid;
    private String RequestId;
    private String bizcode;
    private String BizToken;
    private String Url;
    private String certify_id;
    private String certify_url;
}
