package io.ants.modules.app.vo;

import lombok.Data;

@Data
public class TokenPayCallBackBodyVo {

    //Id	string	TokenPay内部订单号
    //BlockTransactionId	string	区块哈希
    //OutOrderId	string	外部订单号，调用 创建订单 接口时传递的外部订单号
    //OrderUserKey	string	支付用户标识，调用 创建订单 接口时传递的支付用户标识
    //PayTime	string	支付时间，示例：2022-09-15 16:00:00
    //BlockchainName	string	区块链名称
    //Currency	string	币种，USDT_TRC20、TRX等，如配置了EVMChains.json,原生币格式为EVM_[ChainNameEN]_[BaseCoin],ERC20代币格式为：EVM_[ChainNameEN]_[Erc20.Name]_[ERC20Name]，如BSC的原生币为EVM_BSC_BNB，BSC的USDT代币为EVM_BSC_USDT_BEP20
    //CurrencyName	string	币种名称
    //BaseCurrency	string	法币币种，支持CNY、USD、EUR、GBP、AUD、HKD、TWD、SGD
    //Amount	string	订单金额，此金额为法币BaseCurrency转换为Currency币种后的金额
    //ActualAmount	string	订单金额，此金额为法币金额
    //FromAddress	string	付款地址
    //ToAddress	string	收款地址
    //Status	int	状态 0 等待支付 1 已支付 2 订单过期
    //PassThroughInfo	string	创建订单如提供了此字段，在回调通知或订单信息中会原样返回
    //Signature	string	签名，接口请务必验证此参数！！！将除Signature字段外的所有字段，按照字母升序排序。按顺序拼接为key1=value1&key2=value2形式，然后在末尾拼接上异步通知密钥，将此字符串计算MD5，即为签名。
    private String   Id;
    private String   BlockTransactionId;
    private String   OutOrderId;
    private String   OrderUserKey;
    private String   PayTime;
    private String   BlockchainName;
    private String   Currency;
    private String   CurrencyName;
    private String   BaseCurrency;
    private String   Amount;
    private String   ActualAmount;
    private String   FromAddress;
    private String   ToAddress;
    private Integer  Status;
    private String   PassThroughInfo;
    private String   Signature;

}
