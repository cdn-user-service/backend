package io.ants.modules.utils.config;

import lombok.Data;

@Data
public class AllinpayConfig {

    private  String SYB_ORGID = "";
    private  String SYB_CUSID = "990581007426001";
    private  String SYB_APPID = "00000051";
    private  String SYB_MD5_APPKEY = "allinpay888";
    private  double EXCHANGE_RMB=7.2;
    //private  final String SYB_APIURL = "https://vsp.allinpay.com/apiweb/unitorder";
    private  String NOTIFY_URL="https://demo.antsxdp.com/antsxdp/app/pay/allinpay/callback";

    //private  String SIGN_TYPE = "SM2";
    /**商户RSA私钥,用于向通联发起请求前进行签名**/
    private  String SYB_RSACUSPRIKEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAK5aIo+E1eyWwEIgMB8ZEZRAaWjSAglmfKVhzy8N1eLjAlqPjJgOCqXGEYt/r61AyIjCCJiYVDTHzcqstmbBU7HKpYjTsquCLjRWcL/fhMwMGBSg2bP5mqw5locSOz1gtRujmd3kZo9OIJuWtfG2+wgPPdKUdGZS+5K8WtWCF4z1AgMBAAECgYAPvvqvkPzb9tpqrmsCJ/qvM6kBazP9Ytjfe8ehFYQLT1qrUJsPMXdzNMHpYhD82eSyeymZFGrIcIIMq4/2lD+pYOMQTMGGjoVb2wnQhZFqPdgjXgOQ90E43X69jD3p5F8CuKVNa13I4l3iyfzlVIL780JPdJdug7yKEFdSeOQZUQJBAONlFpIqz87pbnwzfgO5kRTbbI7DcyObb8OEeCK3VlGB3r9P4NoMEDaXm+HnIdv53gnFq+xgbREWUt2nFq9dSUUCQQDESOIdSvIBc3KQTYR+cnlQTH0SOvm0Tlx4KekBCLxTFAFyBqnOBLdVyQb6Z1wxGz855AjnNbHy1rFhUYQ6hPfxAkAIRZUcnBITJMqwGe9rk0SDzbeVOebmVLEsG5WDLcgmDuNbcjxrsiSk178D6LSCnARHtrkaUCenh3hcN8fLeUlBAkABNP2G9pYEYkRbFM7yxBtw3feK7Cfq7uxspL1VD0uxKxdTLy1OIgNKmMDdO1N6zdMWtQtE+LSObLmMgqbQgU7RAkBFX5kl4+B3k+/aCYB/ndqd1nQIr4SNAtLFJDtlW2xah9W2lQL/7KQDT4o4dUMY51m7Bu61SAmKtralv7Hf25yf";
    /**通联平台RSA公钥，用于请求返回或者通联通知的验签**/
    private  String SYB_RSATLPUBKEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYXfu4b7xgDSmEGQpQ8Sn3RzFgl5CE4gL4TbYrND4FtCYOrvbgLijkdFgIrVVWi2hUW4K0PwBsmlYhXcbR+JSmqv9zviVXZiym0lK3glJGVCN86r9EPvNTusZZPm40TOEKMVENSYaUjCxZ7JzeZDfQ4WCeQQr2xirqn6LdJjpZ5wIDAQAB";

    /**商户sm2私钥,用于向通联发起请求前进行签名**/
    private  String SYB_SM2PPRIVATEKEY = "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgjj4Rk+b0YjwO+UwXofnHf4bK+kaaY5Btkd8nMP2VimmgCgYIKoEcz1UBgi2hRANCAAQqlALW4qGC3bP1x3wo5QsKxaCMEZJ2ODTTwOQ+d8UGU7GoK/y/WMBQWf5upMnFU06p5FxGooXYYoBtldgm03hq";
    /**通联平台sm2公钥，用于请求返回或者通联通知的验签**/
    private  String SYB_SM2TLPUBKEY = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAE/BnA8BawehBtH0ksPyayo4pmzL/u1FQ2sZcqwOp6bjVqQX4tjo930QAvHZPJ2eez8sCz/RYghcqv4LvMq+kloQ==";
}
