package io.ants.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tb_pay_recory")
public class TbPayRecordEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String serialNumber;

    private Integer payType;

    private String payId;

    private Integer payPaid;

    private  String payMsg;

    private Integer status;

    private Integer operateStatus;

//        `id` int(11) NOT NULL,
//        `order_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单ID',
//            `pay_type` tinyint(1) DEFAULT NULL COMMENT '支付类型1人工 2支付宝 3微信 4富友 5银行卡',
//            `pay_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方支付流水号',
//            `pay_paid` int(11) DEFAULT NULL COMMENT '收到金额_分',
//            `pay_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '收到的第三方返回值',
//            `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支付状态',
//            `createtime` timestamp(0) DEFAULT CURRENT_TIMESTAMP,

    private Date createtime;
}
